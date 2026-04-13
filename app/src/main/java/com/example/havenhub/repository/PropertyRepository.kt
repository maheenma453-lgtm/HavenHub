package com.example.havenhub.repository

import android.net.Uri
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseStorageManager
import com.example.havenhub.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    private val dataManager    : FirebaseDataManager,
    private val storageManager : FirebaseStorageManager
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun addProperty(property: Property, imageUris: List<Uri> = emptyList()): Resource<String> {
        var imageUrls: List<String> = property.imageUrls

        if (imageUris.isNotEmpty()) {
            val tempId       = System.currentTimeMillis().toString()
            val uploadResult = storageManager.uploadPropertyImages(tempId, imageUris)
            if (uploadResult is Resource.Error)
                return Resource.Error(uploadResult.message ?: "Upload failed")
            imageUrls = (uploadResult as Resource.Success).data ?: emptyList()
        }

        val propertyToSave = property.copy(
            imageUrls = imageUrls,
            status    = PropertyStatus.PENDING.name
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

    // ✅ FIX: getAllProperties → getApprovedProperties
    suspend fun getAllProperties(): Resource<List<Property>> =
        getApprovedProperties()

    // ✅ FIX: isFeatured check hata diya — sirf APPROVED filter
    //    Agar isFeatured feature chahiye to Firestore mein manually set karo
    //    aur neeche wala commented block use karo
    suspend fun getFeaturedProperties(): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val allApproved = (result as Resource.Success).data
            ?.filter { it.status == PropertyStatus.APPROVED.name }
            ?: emptyList()

        // ✅ Pehle isFeatured=true wali try karo, agar koi nahi mili
        //    to top 5 APPROVED properties featured ki jagah dikhao
        val featured = allApproved.filter { it.isFeatured }
        val toReturn = if (featured.isEmpty()) allApproved.take(5) else featured

        return Resource.Success(toReturn)
    }

    // ✅ Recent — sirf APPROVED, createdAt ke hisaab se sort
    suspend fun getRecentProperties(): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val recent = (result as Resource.Success).data
            ?.filter { it.status == PropertyStatus.APPROVED.name }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
        return Resource.Success(recent)
    }

    // ✅ Nearby — sirf APPROVED
    suspend fun getNearbyProperties(): Resource<List<Property>> =
        getApprovedProperties()

    suspend fun getPropertyById(propertyId: String): Resource<Property> =
        dataManager.getPropertyById(propertyId)

    // ✅ Landlord ki apni saari properties — PENDING bhi dikhti hain
    suspend fun getMyProperties(ownerId: String): Resource<List<Property>> =
        dataManager.getPropertiesByOwner(ownerId)

    // ✅ Search — sirf APPROVED properties mein search
    suspend fun searchPropertiesByName(query: String): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val filtered = (result as Resource.Success).data
            ?.filter {
                it.status == PropertyStatus.APPROVED.name &&
                        it.title.contains(query, ignoreCase = true)
            }
            ?: emptyList()
        return Resource.Success(filtered)
    }

    // ✅ City filter — sirf APPROVED properties
    suspend fun getPropertiesByCity(city: String): Resource<List<Property>> {
        val result = dataManager.getAllProperties()
        if (result is Resource.Error) return result
        val filtered = (result as Resource.Success).data
            ?.filter {
                it.status == PropertyStatus.APPROVED.name &&
                        it.city.contains(city, ignoreCase = true)
            }
            ?: emptyList()
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

    suspend fun updateProperty(propertyId: String, fields: Map<String, Any>): Resource<Unit> =
        dataManager.updateProperty(propertyId, fields)

    suspend fun approveProperty(propertyId: String, adminNote: String = ""): Resource<Unit> =
        dataManager.updateProperty(
            propertyId,
            mapOf(
                "status"    to PropertyStatus.APPROVED.name,
                "adminNote" to adminNote,
                "updatedAt" to System.currentTimeMillis()
            )
        )

    suspend fun rejectProperty(propertyId: String, adminNote: String): Resource<Unit> =
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
        val uploadResult = storageManager.uploadPropertyImages(propertyId, newImageUris)
        if (uploadResult is Resource.Error)
            return Resource.Error(uploadResult.message ?: "Upload failed")

        val newUrls = (uploadResult as Resource.Success).data ?: emptyList()

        val getResult = dataManager.getPropertyById(propertyId)
        if (getResult is Resource.Error)
            return Resource.Error(getResult.message ?: "Property not found")

        val existingUrls = (getResult as Resource.Success).data?.imageUrls ?: emptyList()
        val allUrls      = existingUrls + newUrls

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
                "status"    to PropertyStatus.PENDING.name,
                "updatedAt" to System.currentTimeMillis()
            )
        )
}





