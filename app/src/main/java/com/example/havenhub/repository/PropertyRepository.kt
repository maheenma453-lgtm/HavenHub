package com.example.havenhub.repository

import android.net.Uri
import android.util.Log
import com.example.havenhub.data.NotificationType
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.ImgBBUploadManager
import com.example.havenhub.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    private val dataManager           : FirebaseDataManager,
    private val imgBBManager          : ImgBBUploadManager,
    private val firestore             : FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) {

    private val propertiesCol = firestore.collection("properties")
    private val usersCol      = firestore.collection("users")

    // ── Filter helper — returns only APPROVED properties ──────────────────────
    private suspend fun fetchApproved(): Resource<List<Property>> {
        return when (val result = dataManager.getAllProperties()) {
            is Resource.Success -> Resource.Success(
                result.data.filter {
                    it.status.equals(PropertyStatus.APPROVED.name, ignoreCase = true)
                }
            )
            is Resource.Error -> Resource.Error(result.message)
            Resource.Loading  -> Resource.Error("Unexpected loading state")
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    suspend fun addProperty(
        property : Property,
        imageUris: List<Uri> = emptyList(),
        pt1Uri   : Uri?      = null
    ): Resource<String> {

        var imageUrls = property.imageUrls
        if (imageUris.isNotEmpty()) {
            val uploadResult = imgBBManager.uploadImages(imageUris)
            if (uploadResult is Resource.Error) return Resource.Error(uploadResult.message)
            imageUrls = (uploadResult as Resource.Success).data
        }

        var pt1Url = ""
        if (pt1Uri != null) {
            val pt1Result = imgBBManager.uploadImages(listOf(pt1Uri))
            if (pt1Result is Resource.Error) return Resource.Error(pt1Result.message)
            pt1Url = (pt1Result as Resource.Success).data.firstOrNull() ?: ""
        }

        val propertyToSave = property.copy(
            imageUrls      = imageUrls,
            pt1DocumentUrl = pt1Url,
            status         = PropertyStatus.PENDING.name
        )
        val result = dataManager.addProperty(propertyToSave)

        if (result is Resource.Success) {
            val savedPropertyId = result.data ?: ""
            sendPendingPropertyNotificationToAdmins(
                propertyId    = savedPropertyId,
                propertyTitle = property.title,
                landlordName  = property.ownerName.ifBlank { "Landlord" }
            )
        }

        return result
    }

    // ── READ — Tenant (APPROVED only) ─────────────────────────────────────────
    suspend fun getApprovedProperties(): Resource<List<Property>> = fetchApproved()
    suspend fun getAllProperties(): Resource<List<Property>>       = fetchApproved()

    suspend fun getFeaturedProperties(): Resource<List<Property>> {
        val result = fetchApproved()
        if (result is Resource.Error) return result
        return Resource.Success((result as Resource.Success).data.filter { it.isFeatured })
    }

    suspend fun getNearbyProperties(): Resource<List<Property>> = fetchApproved()

    suspend fun getRecentProperties(): Resource<List<Property>> {
        val result = fetchApproved()
        if (result is Resource.Error) return result
        return Resource.Success(
            (result as Resource.Success).data.sortedByDescending { it.createdAt }
        )
    }

    suspend fun getPropertyById(propertyId: String): Resource<Property> =
        dataManager.getPropertyById(propertyId)

    suspend fun getMyProperties(ownerId: String): Resource<List<Property>> =
        dataManager.getPropertiesByOwner(ownerId)

    suspend fun searchPropertiesByName(query: String): Resource<List<Property>> {
        val result = fetchApproved()
        if (result is Resource.Error) return result
        return Resource.Success(
            (result as Resource.Success).data.filter {
                it.title.contains(query, ignoreCase = true)
            }
        )
    }

    suspend fun getPropertiesByCity(city: String): Resource<List<Property>> {
        val result = fetchApproved()
        if (result is Resource.Error) return result
        return Resource.Success(
            (result as Resource.Success).data.filter {
                it.city.contains(city, ignoreCase = true)
            }
        )
    }

    suspend fun searchProperties(query: String): Resource<List<Property>> =
        dataManager.searchProperties(query)

    // ── READ — Admin (all statuses) ───────────────────────────────────────────
    suspend fun getAllPropertiesForAdmin(): Resource<List<Property>> =
        dataManager.getAllProperties()

    suspend fun getPendingProperties(): Resource<List<Property>> {
        return when (val result = dataManager.getAllProperties()) {
            is Resource.Success -> Resource.Success(
                result.data.filter {
                    it.status.equals(PropertyStatus.PENDING.name, ignoreCase = true)
                }
            )
            is Resource.Error -> Resource.Error(result.message)
            Resource.Loading  -> Resource.Error("Unexpected loading state")
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    suspend fun updateProperty(
        propertyId: String,
        fields    : Map<String, Any>
    ): Resource<Unit> = dataManager.updateProperty(propertyId, fields)

    /**
     * ✅ FIXED: Approve ke baad landlord + tenant ko notification bhejo.
     * Pehle sirf Firestore update hota tha — notification missing thi.
     */
    suspend fun approveProperty(
        propertyId: String,
        adminNote : String = ""
    ): Resource<Unit> {
        val result = dataManager.updateProperty(
            propertyId,
            mapOf(
                "status"    to PropertyStatus.APPROVED.name,
                "adminNote" to adminNote,
                "updatedAt" to System.currentTimeMillis()
            )
        )

        if (result is Resource.Success) {
            try {
                val propDoc = propertiesCol.document(propertyId).get().await()
                val ownerId       = propDoc.getString("ownerId") ?: ""
                val propertyTitle = propDoc.getString("title")   ?: "Property"

                if (ownerId.isNotBlank()) {
                    notificationRepository.sendPropertyApprovedNotification(
                        ownerId       = ownerId,
                        propertyId    = propertyId,
                        propertyTitle = propertyTitle,
                        adminNote     = adminNote
                    )
                    Log.d("PROPERTY_REPO", "✅ Property approved notification sent to landlord $ownerId")
                }

                // Tenants jo is property mein interested hain unhe bhi batao (optional)
                sendApprovalAlertToInterestedTenants(propertyId, propertyTitle)

            } catch (e: Exception) {
                Log.e("PROPERTY_REPO", "approveProperty notification error: ${e.localizedMessage}")
            }
        }
        return result
    }

    /**
     * ✅ FIXED: Reject ke baad landlord ko reason ke saath notification bhejo.
     * Pehle sirf Firestore update hota tha — notification missing thi.
     */
    suspend fun rejectProperty(
        propertyId: String,
        adminNote : String
    ): Resource<Unit> {
        val result = dataManager.updateProperty(
            propertyId,
            mapOf(
                "status"    to PropertyStatus.REJECTED.name,
                "adminNote" to adminNote,
                "updatedAt" to System.currentTimeMillis()
            )
        )

        if (result is Resource.Success) {
            try {
                val propDoc = propertiesCol.document(propertyId).get().await()
                val ownerId       = propDoc.getString("ownerId") ?: ""
                val propertyTitle = propDoc.getString("title")   ?: "Property"

                if (ownerId.isNotBlank()) {
                    notificationRepository.sendPropertyRejectedNotification(
                        ownerId       = ownerId,
                        propertyId    = propertyId,
                        propertyTitle = propertyTitle,
                        adminNote     = adminNote
                    )
                    Log.d("PROPERTY_REPO", "✅ Property rejected notification sent to landlord $ownerId")
                }
            } catch (e: Exception) {
                Log.e("PROPERTY_REPO", "rejectProperty notification error: ${e.localizedMessage}")
            }
        }
        return result
    }

    suspend fun addPropertyImages(
        propertyId  : String,
        newImageUris: List<Uri>
    ): Resource<List<String>> {
        val uploadResult = imgBBManager.uploadImages(newImageUris)
        if (uploadResult is Resource.Error) return Resource.Error(uploadResult.message)
        val newUrls   = (uploadResult as Resource.Success).data
        val getResult = dataManager.getPropertyById(propertyId)
        if (getResult is Resource.Error) return Resource.Error(getResult.message)
        val allUrls   = (getResult as Resource.Success).data.imageUrls + newUrls
        dataManager.updateProperty(propertyId, mapOf("imageUrls" to allUrls))
        return Resource.Success(newUrls)
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    suspend fun deleteProperty(propertyId: String): Resource<Unit> =
        dataManager.deleteProperty(propertyId)

    suspend fun submitForVerification(propertyId: String): Resource<Unit> =
        dataManager.updateProperty(
            propertyId,
            mapOf(
                "status"    to PropertyStatus.PENDING.name,
                "updatedAt" to System.currentTimeMillis()
            )
        )

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private suspend fun sendPendingPropertyNotificationToAdmins(
        propertyId   : String,
        propertyTitle: String,
        landlordName : String
    ) {
        try {
            var adminQuery = usersCol.whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = usersCol.whereEqualTo("role", "admin").get().await()
            }
            if (adminQuery.isEmpty) {
                Log.w("PROPERTY_REPO", "No admin found — pending notification skipped")
                return
            }
            adminQuery.documents.forEach { doc ->
                notificationRepository.sendNewPropertyPendingNotification(
                    adminId       = doc.id,
                    propertyId    = propertyId,
                    propertyTitle = propertyTitle,
                    landlordName  = landlordName
                )
                Log.d("PROPERTY_REPO", "✅ Property pending notification sent to admin: ${doc.id}")
            }
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "sendPendingPropertyNotificationToAdmins error: ${e.localizedMessage}")
        }
    }

    /**
     * ✅ NEW: Seasonal availability alert —
     * Jab property approve ho, un tenants ko notify karo jinki
     * watchlist/bookings is property pe thi ya city match karti ho.
     * Abhi simple implementation: saare TENANT users ko city-match pe alert.
     * Zyada precise karna ho toh watchlist collection add karo.
     */
    private suspend fun sendApprovalAlertToInterestedTenants(
        propertyId   : String,
        propertyTitle: String
    ) {
        try {
            val propDoc = propertiesCol.document(propertyId).get().await()
            val city    = propDoc.getString("city") ?: return

            // City match karne wale tenants ko notify karo
            val tenantQuery = usersCol.whereEqualTo("role", "TENANT").get().await()
            tenantQuery.documents.forEach { doc ->
                val tenantId = doc.id
                notificationRepository.sendNotification(
                    recipientId = tenantId,
                    type        = NotificationType.PROPERTY_APPROVED,
                    title       = "New Property Available in $city! 🏠",
                    body        = "\"$propertyTitle\" ab $city mein available hai — check karo!",
                    referenceId = propertyId,
                    targetRole  = "tenant"
                )
            }
            Log.d("PROPERTY_REPO", "✅ Tenant availability alerts sent for $propertyTitle in $city")
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "sendApprovalAlertToInterestedTenants error: ${e.localizedMessage}")
        }
    }
}