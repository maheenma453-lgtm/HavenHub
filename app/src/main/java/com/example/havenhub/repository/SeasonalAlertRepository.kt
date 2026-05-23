package com.example.havenhub.repository

import android.util.Log
import com.example.havenhub.data.SeasonalAlert
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// SeasonalAlertRepository.kt
//
// Handles all read/write operations for seasonal_alerts Firestore collection.
//
// Admin creates alerts → stored in Firestore → this repo fetches them →
// SeasonalAlertViewModel filters by role + active status → UI shows them.
//
// Firestore structure:
//   seasonal_alerts/
//     {alertId}/
//       title        : "Eid ul Adha Special!"
//       message      : "Eid holidays aa rahi hain..."
//       season       : "Eid"
//       iconEmoji    : "🎉"
//       targetRole   : "both"        ← "landlord" | "tenant" | "both"
//       isActive     : true
//       startDate    : Timestamp
//       endDate      : Timestamp
//       createdAt    : Timestamp
//       updatedAt    : Timestamp
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class SeasonalAlertRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG        = "HAVEN_SEASONAL"
        private const val COLLECTION = "seasonal_alerts"
    }

    private val alertsCollection = firestore.collection(COLLECTION)

    // ─────────────────────────────────────────────────────────────────────────
    // parseAlert — safely converts a Firestore document to SeasonalAlert
    // Returns null if parsing fails (bad data won't crash the app)
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseAlert(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): SeasonalAlert? {
        return try {
            SeasonalAlert(
                alertId    = doc.id,
                title      = doc.getString("title")      ?: "",
                message    = doc.getString("message")    ?: "",
                season     = doc.getString("season")     ?: "",
                iconEmoji  = doc.getString("iconEmoji")  ?: "🎉",
                targetRole = doc.getString("targetRole") ?: "both",
                isActive   = doc.getBoolean("isActive")  ?: true,
                startDate  = doc.getTimestamp("startDate"),
                endDate    = doc.getTimestamp("endDate"),
                createdAt  = doc.getTimestamp("createdAt"),
                updatedAt  = doc.getTimestamp("updatedAt")
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseAlert FAIL for ${doc.id}: ${e.localizedMessage}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getActiveAlertsForRole
    //
    // Fetches all currently active seasonal alerts that apply to the given role.
    // Filters:
    //   1. isActive == true
    //   2. targetRole == userRole OR targetRole == "both"
    //   3. Current time is between startDate and endDate (if set)
    //
    // Called by SeasonalAlertViewModel on app launch / notification screen open.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getActiveAlertsForRole(userRole: String): Resource<List<SeasonalAlert>> {
        return try {
            val snapshot = alertsCollection
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val now = Timestamp.now()

            val alerts = snapshot.documents
                .mapNotNull { parseAlert(it) }
                .filter { alert ->
                    // Role check — show if meant for this role OR for everyone
                    val roleMatches = alert.targetRole == userRole || alert.targetRole == "both"

                    // Date range check — if no dates set, always show
                    val afterStart = alert.startDate?.let { it <= now } ?: true
                    val beforeEnd  = alert.endDate?.let  { it >= now } ?: true

                    roleMatches && afterStart && beforeEnd
                }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }

            Log.d(TAG, "getActiveAlertsForRole[$userRole]: ${alerts.size} alerts found")
            Resource.Success(alerts)

        } catch (e: Exception) {
            Log.e(TAG, "getActiveAlertsForRole FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch seasonal alerts")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // observeActiveAlerts — real-time Flow listener
    //
    // Uses Firestore snapshot listener so UI auto-updates when admin
    // adds/removes/changes alerts without needing a manual refresh.
    //
    // Usage in ViewModel:
    //   repo.observeActiveAlerts(userRole).collect { alerts -> ... }
    // ─────────────────────────────────────────────────────────────────────────
    fun observeActiveAlerts(userRole: String): Flow<List<SeasonalAlert>> = callbackFlow {
        val now = Timestamp.now()

        val listener = alertsCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeActiveAlerts listener error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val alerts = snapshot?.documents
                    ?.mapNotNull { parseAlert(it) }
                    ?.filter { alert ->
                        val roleMatches = alert.targetRole == userRole || alert.targetRole == "both"
                        val afterStart  = alert.startDate?.let { it <= now } ?: true
                        val beforeEnd   = alert.endDate?.let  { it >= now } ?: true
                        roleMatches && afterStart && beforeEnd
                    }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()

                Log.d(TAG, "observeActiveAlerts[$userRole] update: ${alerts.size} alerts")
                trySend(alerts)
            }

        // Cancel the Firestore listener when the Flow is no longer collected
        awaitClose { listener.remove() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllAlerts — Admin use only
    //
    // Fetches ALL alerts (active + inactive) for the Admin management screen.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getAllAlerts(): Resource<List<SeasonalAlert>> {
        return try {
            val snapshot = alertsCollection.get().await()
            val alerts   = snapshot.documents
                .mapNotNull { parseAlert(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }

            Log.d(TAG, "getAllAlerts (admin): ${alerts.size} total alerts")
            Resource.Success(alerts)
        } catch (e: Exception) {
            Log.e(TAG, "getAllAlerts FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch all alerts")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createAlert — Admin creates a new seasonal alert
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun createAlert(alert: SeasonalAlert): Resource<String> {
        return try {
            val docRef = alertsCollection.document()
            val data   = mapOf(
                "title"      to alert.title,
                "message"    to alert.message,
                "season"     to alert.season,
                "iconEmoji"  to alert.iconEmoji,
                "targetRole" to alert.targetRole,
                "isActive"   to alert.isActive,
                "startDate"  to alert.startDate,
                "endDate"    to alert.endDate,
                "createdAt"  to FieldValue.serverTimestamp(),
                "updatedAt"  to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Log.d(TAG, "createAlert SUCCESS: ${docRef.id} — ${alert.title}")
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "createAlert FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to create seasonal alert")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateAlert — Admin edits an existing alert
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun updateAlert(alertId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            val updatedFields = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            alertsCollection.document(alertId).update(updatedFields).await()
            Log.d(TAG, "updateAlert SUCCESS: $alertId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateAlert FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to update alert")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toggleAlertActive — Admin can quickly enable/disable an alert
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun toggleAlertActive(alertId: String, isActive: Boolean): Resource<Unit> {
        return updateAlert(alertId, mapOf("isActive" to isActive))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteAlert — Admin permanently removes an alert
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun deleteAlert(alertId: String): Resource<Unit> {
        return try {
            alertsCollection.document(alertId).delete().await()
            Log.d(TAG, "deleteAlert SUCCESS: $alertId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAlert FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete alert")
        }
    }
}