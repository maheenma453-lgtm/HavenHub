package com.example.havenhub.repository

import android.net.Uri
import android.util.Log
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
    private val notificationRepository: NotificationRepository   // NEW: inject for notifications
) {

    // Filter helper — returns only APPROVED properties
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

    // CREATE
    // After saving property, notify every admin so they can review it.
    suspend fun addProperty(
        property : Property,
        imageUris: List<Uri> = emptyList(),
        pt1Uri   : Uri?      = null
    ): Resource<String> {

        // Step 1: Upload property images
        var imageUrls = property.imageUrls
        if (imageUris.isNotEmpty()) {
            val uploadResult = imgBBManager.uploadImages(imageUris)
            if (uploadResult is Resource.Error) return Resource.Error(uploadResult.message)
            imageUrls = (uploadResult as Resource.Success).data
        }

        // Step 2: Upload PT-1 document
        var pt1Url = ""
        if (pt1Uri != null) {
            val pt1Result = imgBBManager.uploadImages(listOf(pt1Uri))
            if (pt1Result is Resource.Error) return Resource.Error(pt1Result.message)
            pt1Url = (pt1Result as Resource.Success).data.firstOrNull() ?: ""
        }

        // Step 3: Save to Firestore with PENDING status
        val propertyToSave = property.copy(
            imageUrls      = imageUrls,
            pt1DocumentUrl = pt1Url,
            status         = PropertyStatus.PENDING.name
        )
        val result = dataManager.addProperty(propertyToSave)

        // Step 4: Notify all admins about new pending property
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

    // Find all admin users and send each one a notification
    private suspend fun sendPendingPropertyNotificationToAdmins(
        propertyId   : String,
        propertyTitle: String,
        landlordName : String
    ) {
        try {
            // Try ADMIN (uppercase) first, then admin (lowercase) as fallback
            var adminQuery = firestore.collection("users")
                .whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = firestore.collection("users")
                    .whereEqualTo("role", "admin").get().await()
            }

            if (adminQuery.isEmpty) {
                Log.w("PROPERTY_REPO", "No admin users found — property pending notification skipped")
                return
            }

            adminQuery.documents.forEach { doc ->
                notificationRepository.sendNewPropertyPendingNotification(
                    adminId       = doc.id,
                    propertyId    = propertyId,
                    propertyTitle = propertyTitle,
                    landlordName  = landlordName
                )
                Log.d("PROPERTY_REPO", "Property pending notification sent to admin: ${doc.id}")
            }
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "sendPendingPropertyNotificationToAdmins error: ${e.localizedMessage}")
        }
    }

    // READ — Tenant (APPROVED only)
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

    // READ — Admin (all statuses, no filter)
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

    // UPDATE
    suspend fun updateProperty(
        propertyId: String,
        fields    : Map<String, Any>
    ): Resource<Unit> = dataManager.updateProperty(propertyId, fields)

    suspend fun approveProperty(
        propertyId: String,
        adminNote : String = ""
    ): Resource<Unit> = dataManager.updateProperty(
        propertyId,
        mapOf(
            "status"    to PropertyStatus.APPROVED.name,
            "adminNote" to adminNote,
            "updatedAt" to System.currentTimeMillis()
        )
    )

    suspend fun rejectProperty(
        propertyId: String,
        adminNote : String
    ): Resource<Unit> = dataManager.updateProperty(
        propertyId,
        mapOf(
            "status"    to PropertyStatus.REJECTED.name,
            "adminNote" to adminNote,
            "updatedAt" to System.currentTimeMillis()
        )
    )

    suspend fun addPropertyImages(
        propertyId  : String,
        newImageUris: List<Uri>
    ): Resource<List<String>> {
        val uploadResult = imgBBManager.uploadImages(newImageUris)
        if (uploadResult is Resource.Error) return Resource.Error(uploadResult.message)
        val newUrls = (uploadResult as Resource.Success).data
        val getResult = dataManager.getPropertyById(propertyId)
        if (getResult is Resource.Error) return Resource.Error(getResult.message)
        val allUrls = (getResult as Resource.Success).data.imageUrls + newUrls
        dataManager.updateProperty(propertyId, mapOf("imageUrls" to allUrls))
        return Resource.Success(newUrls)
    }

    // DELETE
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
}