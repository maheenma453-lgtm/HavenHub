package com.example.havenhub.repository

import android.net.Uri
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.ImgBBUploadManager
import com.example.havenhub.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    private val dataManager: FirebaseDataManager,
    private val imgBBManager: ImgBBUploadManager
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun addProperty(
        property: Property,
        imageUris: List<Uri> = emptyList(),
        pt1Uri: Uri? = null               // ✅ NEW: PT-1 document URI
    ): Resource<String> {

        // ✅ Step 1: Property images upload karo imgbb pe
        var imageUrls = property.imageUrls
        if (imageUris.isNotEmpty()) {
            val uploadResult = imgBBManager.uploadImages(imageUris)
            if (uploadResult is Resource.Error)
                return Resource.Error(uploadResult.message ?: "Image upload failed")
            imageUrls = (uploadResult as Resource.Success).data ?: emptyList()
        }

        // ✅ Step 2: PT-1 document upload karo imgbb pe
        var pt1Url: String? = null
        if (pt1Uri != null) {
            val pt1Result = imgBBManager.uploadImages(listOf(pt1Uri))
            if (pt1Result is Resource.Error)
                return Resource.Error(pt1Result.message ?: "PT-1 upload failed")
            pt1Url = (pt1Result as Resource.Success).data?.firstOrNull()
        }

        // ✅ Step 3: Property object mein dono URLs save karo
        val propertyToSave = property.copy(
            imageUrls = imageUrls,
            pt1DocumentUrl = pt1Url ?: "",   // Property.kt mein yeh field add karni hai
            status = PropertyStatus.PENDING.name
        )
        return dataManager.addProperty(propertyToSave)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read — Tenant ke liye (sirf APPROVED)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getApprovedProperties(): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val approved = (result as Resource.Success).data
            ?.filter { it.status == PropertyStatus.APPROVED.name }
            ?: emptyList()
        return Resource.Success(approved)
    }

    suspend fun getAllProperties(): Resource<List<Property>> =
        getApprovedProperties()

    suspend fun getFeaturedProperties(): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val allApproved = (result as Resource.Success).data
            ?.filter { it.status == PropertyStatus.APPROVED.name }
            ?: emptyList()
        val featured = allApproved.filter { it.isFeatured }
        val toReturn = if (featured.isEmpty()) allApproved.take(5) else featured
        return Resource.Success(toReturn)
    }

    suspend fun getRecentProperties(): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val recent = (result as Resource.Success).data
            ?.filter { it.status == PropertyStatus.APPROVED.name }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
        return Resource.Success(recent)
    }

    suspend fun getNearbyProperties(): Resource<List<Property>> =
        getApprovedProperties()

    suspend fun getPropertyById(propertyId: String): Resource<Property> =
        dataManager.getPropertyById(propertyId)

    suspend fun getMyProperties(ownerId: String): Resource<List<Property>> =
        dataManager.getPropertiesByOwner(ownerId)

    suspend fun searchPropertiesByName(query: String): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val filtered = (result as Resource.Success).data
            ?.filter {
                it.status == PropertyStatus.APPROVED.name &&
                        it.title.contains(query, ignoreCase = true)
            } ?: emptyList()
        return Resource.Success(filtered)
    }

    suspend fun getPropertiesByCity(city: String): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val filtered = (result as Resource.Success).data
            ?.filter {
                it.status == PropertyStatus.APPROVED.name &&
                        it.city.contains(city, ignoreCase = true)
            } ?: emptyList()
        return Resource.Success(filtered)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read — Admin ke liye (sab properties)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAllPropertiesForAdmin(): Resource<List<Property>> =
        dataManager.getAllProperties()

    suspend fun getPendingProperties(): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val pending = (result as Resource.Success).data
            ?.filter { it.status == PropertyStatus.PENDING.name }
            ?: emptyList()
        return Resource.Success(pending)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun updateProperty(
        propertyId: String,
        fields: Map<String, Any>
    ): Resource<Unit> =
        dataManager.updateProperty(propertyId, fields)

    suspend fun approveProperty(
        propertyId: String,
        adminNote: String = ""
    ): Resource<Unit> =
        dataManager.updateProperty(
            propertyId,
            mapOf(
                "status" to PropertyStatus.APPROVED.name,
                "adminNote" to adminNote,
                "updatedAt" to System.currentTimeMillis()
            )
        )

    suspend fun rejectProperty(
        propertyId: String,
        adminNote: String
    ): Resource<Unit> =
        dataManager.updateProperty(
            propertyId,
            mapOf(
                "status" to PropertyStatus.REJECTED.name,
                "adminNote" to adminNote,
                "updatedAt" to System.currentTimeMillis()
            )
        )

    suspend fun addPropertyImages(
        propertyId: String,
        newImageUris: List<Uri>
    ): Resource<List<String>> {
        val uploadResult = imgBBManager.uploadImages(newImageUris)
        if (uploadResult is Resource.Error)
            return Resource.Error(uploadResult.message ?: "Upload failed")

        val newUrls = (uploadResult as Resource.Success).data ?: emptyList()

        val getResult = dataManager.getPropertyById(propertyId)
        if (getResult is Resource.Error)
            return Resource.Error(getResult.message ?: "Property not found")

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

    suspend fun submitForVerification(propertyId: String): Resource<Unit> =
        dataManager.updateProperty(
            propertyId,
            mapOf(
                "status" to PropertyStatus.PENDING.name,
                "updatedAt" to System.currentTimeMillis()
            )
        )
}