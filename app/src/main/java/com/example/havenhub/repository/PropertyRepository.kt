package com.example.havenhub.repository

import android.net.Uri
import com.example.havenhub.data.Property
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseStorageManager
import com.example.havenhub.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

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
            if (uploadResult is Resource.Error) return Resource.Error(uploadResult.message ?: "Upload Failed")
            imageUrls = (uploadResult as Resource.Success).data ?: emptyList()
        }

        val propertyWithImages = property.copy(imageUrls = imageUrls)
        return dataManager.addProperty(propertyWithImages)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read & Home Screen Filters (Missing Functions Added Here)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAllProperties(): Resource<List<Property>> =
        dataManager.getAllProperties()

    // ✅ ViewModel requires this
    suspend fun getFeaturedProperties(): Resource<List<Property>> =
        dataManager.getAllProperties() // Future: Filter by 'isFeatured' field

    // ✅ ViewModel requires this
    suspend fun getNearbyProperties(): Resource<List<Property>> =
        dataManager.getAllProperties() // Future: Filter by Location/Distance

    // ✅ ViewModel requires this
    suspend fun getRecentProperties(): Resource<List<Property>> =
        dataManager.getAllProperties() // Future: Sort by Timestamp

    suspend fun getPropertyById(propertyId: String): Resource<Property> =
        dataManager.getPropertyById(propertyId)

    suspend fun getMyProperties(ownerId: String): Resource<List<Property>> =
        dataManager.getPropertiesByOwner(ownerId)

    // ✅ Search and City Filters added for ViewModel
    suspend fun searchPropertiesByName(query: String): Resource<List<Property>> =
        dataManager.getAllProperties() // Future: Implement query in DataManager

    suspend fun getPropertiesByCity(city: String): Resource<List<Property>> =
        dataManager.getAllProperties() // Future: Implement city filter

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun updateProperty(propertyId: String, fields: Map<String, Any>): Resource<Unit> =
        dataManager.updateProperty(propertyId, fields)

    suspend fun addPropertyImages(propertyId: String, newImageUris: List<Uri>): Resource<List<String>> {
        val uploadResult = storageManager.uploadPropertyImages(propertyId, newImageUris)
        if (uploadResult is Resource.Error) return Resource.Error(uploadResult.message ?: "Upload Failed")

        val newUrls = (uploadResult as Resource.Success).data ?: emptyList()

        val getResult = dataManager.getPropertyById(propertyId)
        if (getResult is Resource.Error) return Resource.Error(getResult.message ?: "Property not found")

        val existingUrls = (getResult as Resource.Success).data?.imageUrls ?: emptyList()
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