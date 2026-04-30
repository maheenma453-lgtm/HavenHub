package com.example.havenhub.remote

import android.util.Log
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Message
import com.example.havenhub.data.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRealtimeListener @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ══════════════════════════════════════════════════════════════════════════
    // MESSAGING
    // ══════════════════════════════════════════════════════════════════════════

    fun listenToMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val registration: ListenerRegistration = firestore
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToMessages error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Message::class.java) ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATIONS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * ✅ FIX: orderBy HATA diya — composite index nahi tha isliye
     * query silently fail hoti thi aur emptyList() return hota tha.
     * Ab client-side sort kiya sortedByDescending se — same result.
     */
    fun listenToNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }

        val registration: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("recipientId", userId)
            // ✅ orderBy REMOVED — no composite index needed
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToNotifications error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Notification::class.java)
                    ?.sortedByDescending { it.createdAt?.seconds } // ✅ client-side sort
                    ?: emptyList()
                Log.d("REALTIME", "✅ ${list.size} notifications for user $userId")
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Admin ke liye role-based notifications.
     * targetRole == "admin" wali saari notifications aayengi.
     */
    fun listenToAdminNotifications(): Flow<List<Notification>> = callbackFlow {
        val registration: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("targetRole", "admin")
            // ✅ orderBy REMOVED — same fix
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToAdminNotifications error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Notification::class.java)
                    ?.sortedByDescending { it.createdAt?.seconds } // ✅ client-side sort
                    ?: emptyList()
                Log.d("REALTIME", "✅ ${list.size} admin notifications")
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Admin Dashboard ke liye recent activities (last 15).
     */
    fun listenToRecentActivities(): Flow<List<Notification>> = callbackFlow {
        val registration: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("targetRole", "admin")
            // ✅ orderBy REMOVED — same fix
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToRecentActivities error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Notification::class.java)
                    ?.sortedByDescending { it.createdAt?.seconds } // ✅ client-side sort
                    ?.take(15)                                      // ✅ limit 15
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BOOKINGS
    // ══════════════════════════════════════════════════════════════════════════

    fun getBookingsFlow(userId: String): Flow<List<Booking>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }

        val registration: ListenerRegistration = firestore
            .collection("bookings")
            .whereEqualTo("tenantId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "getBookingsFlow error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
                Log.d("REALTIME", "✅ ${bookings.size} bookings for tenant $userId")
                trySend(bookings)
            }
        awaitClose { registration.remove() }
    }

    fun listenToPropertyBookings(propertyId: String): Flow<List<Booking>> = callbackFlow {
        val registration: ListenerRegistration = firestore
            .collection("bookings")
            .whereEqualTo("propertyId", propertyId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToPropertyBookings error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
                trySend(bookings)
            }
        awaitClose { registration.remove() }
    }

    fun listenToAllBookings(): Flow<List<Booking>> = callbackFlow {
        val registration: ListenerRegistration = firestore
            .collection("bookings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToAllBookings error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val bookings = try {
                    snapshot?.toObjects(Booking::class.java) ?: emptyList()
                } catch (e: Exception) {
                    Log.e("REALTIME", "Booking parse error: ${e.localizedMessage}")
                    emptyList()
                }
                Log.d("REALTIME", "✅ ${bookings.size} total bookings (admin)")
                trySend(bookings)
            }
        awaitClose { registration.remove() }
    }

    fun listenToLandlordBookings(landlordId: String): Flow<List<Booking>> = callbackFlow {
        if (landlordId.isEmpty()) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }

        val registration: ListenerRegistration = firestore
            .collection("bookings")
            .whereEqualTo("landlordId", landlordId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToLandlordBookings error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
                Log.d("REALTIME", "✅ ${bookings.size} bookings for landlord $landlordId")
                trySend(bookings)
            }
        awaitClose { registration.remove() }
    }
}