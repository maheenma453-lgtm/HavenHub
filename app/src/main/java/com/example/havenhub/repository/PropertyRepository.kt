package com.example.havenhub.repository

import android.net.Uri
import com.example.havenhub.data.Property
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseStorageManager
import com.example.havenhub.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PropertyRepository
 *
 * Manages all property listing operations for HavenHub.
 * Combines Firestore data operations (via [FirebaseDataManager]) with
 * Firebase Storage image/document handling (via [FirebaseStorageManager]).
 */
@Singleton
class PropertyRepository @Inject constructor(
    private val dataManager: FirebaseDataManager,
    private val storageManager: FirebaseStorageManager
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun addProperty(property: Property, imageUris: List<Uri> = emptyList()): Resource<String> {
        var imageUrls: List<String> = property.imageUrls

        if (imageUris.isNotEmpty()) {
            val tempId = System.currentTimeMillis().toString()
            val uploadResult = storageManager.uploadPropertyImages(tempId, imageUris)
            if (uploadResult is Resource.Error) return Resource.Error(uploadResult.message)
            imageUrls = (uploadResult as Resource.Success).data
        }

        val propertyWithImages = property.copy(imageUrls = imageUrls)
        return dataManager.addProperty(propertyWithImages)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAllProperties(): Resource<List<Property>> =
        dataManager.getAllProperties()

    suspend fun getPropertyById(propertyId: String): Resource<Property> =
        dataManager.getPropertyById(propertyId)

    suspend fun getMyProperties(ownerId: String): Resource<List<Property>> =
        dataManager.getPropertiesByOwner(ownerId)

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun updateProperty(propertyId: String, fields: Map<String, Any>): Resource<Unit> =
        dataManager.updateProperty(propertyId, fields)

    suspend fun addPropertyImages(propertyId: String, newImageUris: List<Uri>): Resource<List<String>> {
        val uploadResult = storageManager.uploadPropertyImages(propertyId, newImageUris)
        if (uploadResult is Resource.Error) return uploadResult

        val newUrls = (uploadResult as Resource.Success).data

        val getResult = dataManager.getPropertyById(propertyId)
        if (getResult is Resource.Error) return Resource.Error(getResult.message)

        val existingUrls = (getResult as Resource.Success).data.imageUrls
        val allUrls = existingUrls + newUrls

        dataManager.updateProperty(propertyId, mapOf("imageUrls" to allUrls))
        return Resource.Success(newUrls)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun deleteProperty(propertyId: String): Resource<Unit> =
        dataManager.deleteProperty(propertyId)

    // ─────────────────────────────────────────────────────────────────────────
    // Verification
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun submitForVerification(propertyId: String): Resource<Unit> =
        dataManager.updateProperty(
            propertyId,
            mapOf(
                "verificationStatus" to "pending",
                "submittedForVerificationAt" to System.currentTimeMillis()
            )
        )
}