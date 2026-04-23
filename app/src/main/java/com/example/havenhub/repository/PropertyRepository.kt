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
    private val dataManager : FirebaseDataManager,
    private val imgBBManager: ImgBBUploadManager
) {

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER — Firestore se saari properties lao + APPROVED filter
    // ✅ FIX: ignoreCase = true — "APPROVED" aur "approved" dono match honge
    // ─────────────────────────────────────────────────────────────
    private suspend fun fetchApproved(): Resource<List<Property>> {
        return when (val result = dataManager.getAllProperties()) {
            is Resource.Success -> Resource.Success(
                result.data.filter {
                    it.status.equals(PropertyStatus.APPROVED.name, ignoreCase = true)
                }
            )
            is Resource.Error   -> Resource.Error(result.message)
            Resource.Loading    -> Resource.Error("Unexpected loading state")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────

    suspend fun addProperty(
        property : Property,
        imageUris: List<Uri> = emptyList(),
        pt1Uri   : Uri?      = null
    ): Resource<String> {

        // Step 1: Property images upload
        var imageUrls = property.imageUrls
        if (imageUris.isNotEmpty()) {
            val uploadResult = imgBBManager.uploadImages(imageUris)
            if (uploadResult is Resource.Error)
                return Resource.Error(uploadResult.message)
            imageUrls = (uploadResult as Resource.Success).data
        }

        // Step 2: PT-1 document upload
        var pt1Url = ""
        if (pt1Uri != null) {
            val pt1Result = imgBBManager.uploadImages(listOf(pt1Uri))
            if (pt1Result is Resource.Error)
                return Resource.Error(pt1Result.message)
            pt1Url = (pt1Result as Resource.Success).data.firstOrNull() ?: ""
        }

        // Step 3: Firestore mein save karo — status PENDING se start
        val propertyToSave = property.copy(
            imageUrls      = imageUrls,
            pt1DocumentUrl = pt1Url,
            status         = PropertyStatus.PENDING.name
        )
        return dataManager.addProperty(propertyToSave)
    }

    // ─────────────────────────────────────────────────────────────
    // READ — Tenant ke liye (sirf APPROVED)
    // ─────────────────────────────────────────────────────────────

    suspend fun getApprovedProperties(): Resource<List<Property>> = fetchApproved()

    suspend fun getAllProperties(): Resource<List<Property>> = fetchApproved()

    suspend fun getFeaturedProperties(): Resource<List<Property>> {
        val result = fetchApproved()
        if (result is Resource.Error) return result
        val featured = (result as Resource.Success).data.filter { it.isFeatured }
        return Resource.Success(featured)
    }

    suspend fun getNearbyProperties(): Resource<List<Property>> = fetchApproved()

    suspend fun getRecentProperties(): Resource<List<Property>> {
        val result = fetchApproved()
        if (result is Resource.Error) return result
        val sorted = (result as Resource.Success).data
            .sortedByDescending { it.createdAt }
        return Resource.Success(sorted)
    }

    suspend fun getPropertyById(propertyId: String): Resource<Property> =
        dataManager.getPropertyById(propertyId)

    suspend fun getMyProperties(ownerId: String): Resource<List<Property>> =
        dataManager.getPropertiesByOwner(ownerId)

    suspend fun searchPropertiesByName(query: String): Resource<List<Property>> {
        val result = fetchApproved()
        if (result is Resource.Error) return result
        val filtered = (result as Resource.Success).data
            .filter { it.title.contains(query, ignoreCase = true) }
        return Resource.Success(filtered)
    }

    suspend fun getPropertiesByCity(city: String): Resource<List<Property>> {
        val result = fetchApproved()
        if (result is Resource.Error) return result
        val filtered = (result as Resource.Success).data
            .filter { it.city.contains(city, ignoreCase = true) }
        return Resource.Success(filtered)
    }

    // ─────────────────────────────────────────────────────────────
    // READ — Admin ke liye (sab properties, no status filter)
    // ─────────────────────────────────────────────────────────────

    suspend fun getAllPropertiesForAdmin(): Resource<List<Property>> =
        dataManager.getAllProperties()

    suspend fun getPendingProperties(): Resource<List<Property>> {
        return when (val result = dataManager.getAllProperties()) {
            is Resource.Success -> Resource.Success(
                result.data.filter {
                    // ✅ FIX: ignoreCase = true
                    it.status.equals(PropertyStatus.PENDING.name, ignoreCase = true)
                }
            )
            is Resource.Error -> Resource.Error(result.message)
            Resource.Loading  -> Resource.Error("Unexpected loading state")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────

    suspend fun updateProperty(
        propertyId: String,
        fields    : Map<String, Any>
    ): Resource<Unit> =
        dataManager.updateProperty(propertyId, fields)

    suspend fun approveProperty(
        propertyId: String,
        adminNote : String = ""
    ): Resource<Unit> =
        dataManager.updateProperty(
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
    ): Resource<Unit> =
        dataManager.updateProperty(
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
        if (uploadResult is Resource.Error)
            return Resource.Error(uploadResult.message)

        val newUrls = (uploadResult as Resource.Success).data

        val getResult = dataManager.getPropertyById(propertyId)
        if (getResult is Resource.Error)
            return Resource.Error(getResult.message)

        val existingUrls = (getResult as Resource.Success).data.imageUrls
        val allUrls      = existingUrls + newUrls

        dataManager.updateProperty(propertyId, mapOf("imageUrls" to allUrls))
        return Resource.Success(newUrls)
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────

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
