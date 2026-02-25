package com.example.havenhub.repository
import android.net.Uri
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseStorageManager
import com.havenhub.data.model.Property
import com.havenhub.data.remote.FirebaseDataManager
import com.havenhub.data.remote.FirebaseStorageManager
import com.havenhub.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PropertyRepository
 *
 * Manages all property listing operations for HavenHub.
 * Combines Firestore data operations (via [FirebaseDataManager]) with
 * Firebase Storage image/document handling (via [FirebaseStorageManager]).
 *
 * Responsibilities:
 *  - CRUD operations for property listings
 *  - Uploading property images and returning download URLs
 *  - Fetching listings by filter criteria (owner, search)
 *  - Submitting properties for admin verification
 */
@Singleton
class PropertyRepository @Inject constructor(
    private val dataManager: FirebaseDataManager,
    private val storageManager: FirebaseStorageManager
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new property listing.
     * If image URIs are provided, they are uploaded first and the resulting
     * download URLs are embedded into the [Property] before saving to Firestore.
     *
     * @param property  The [Property] data to save (without images).
     * @param imageUris Optional list of local image [Uri]s to upload.
     * @return [Resource.Success] with the new property ID, or [Resource.Error].
     */
    suspend fun addProperty(property: Property, imageUris: List<Uri> = emptyList()): Resource<String> {
        // Step 1: Upload images if provided and get their download URLs
        var imageUrls: List<String> = property.imageUrls
        if (imageUris.isNotEmpty()) {
            // Use a temporary ID for the storage path; will be replaced after Firestore save
            val tempId = System.currentTimeMillis().toString()
            val uploadResult = storageManager.uploadPropertyImages(tempId, imageUris)
            if (uploadResult is Resource.Error) return Resource.Error(uploadResult.message)
            imageUrls = (uploadResult as Resource.Success).data
        }

        // Step 2: Save property with image URLs to Firestore
        val propertyWithImages = property.copy(imageUrls = imageUrls)
        return dataManager.addProperty(propertyWithImages)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all approved/active property listings.
     *
     * @return [Resource.Success] with a list of [Property], or [Resource.Error].
     */
    suspend fun getAllProperties(): Resource<List<Property>> {
        return dataManager.getAllProperties()
    }

    /**
     * Fetches a single property by its Firestore document ID.
     *
     * @param propertyId The property's document ID.
     * @return [Resource.Success] with [Property], or [Resource.Error].
     */
    suspend fun getPropertyById(propertyId: String): Resource<Property> {
        return dataManager.getPropertyById(propertyId)
    }

    /**
     * Fetches all properties listed by a specific owner/landlord.
     *
     * @param ownerId The Firebase Auth UID of the property owner.
     * @return [Resource.Success] with a list of [Property], or [Resource.Error].
     */
    suspend fun getMyProperties(ownerId: String): Resource<List<Property>> {
        return dataManager.getPropertiesByOwner(ownerId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates specific fields of a property document.
     *
     * @param propertyId The property's document ID.
     * @param fields     Map of field names to new values.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun updateProperty(propertyId: String, fields: Map<String, Any>): Resource<Unit> {
        return dataManager.updateProperty(propertyId, fields)
    }

    /**
     * Uploads new images for an existing property and appends the download URLs.
     *
     * @param propertyId  The property's document ID (used as storage path prefix).
     * @param newImageUris List of local image [Uri]s to upload.
     * @return [Resource.Success] with a list of new download URLs, or [Resource.Error].
     */
    suspend fun addPropertyImages(propertyId: String, newImageUris: List<Uri>): Resource<List<String>> {
        val uploadResult = storageManager.uploadPropertyImages(propertyId, newImageUris)
        if (uploadResult is Resource.Error) return uploadResult

        val newUrls = (uploadResult as Resource.Success).data

        // Append new URLs to existing imageUrls array in Firestore
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

    /**
     * Deletes a property listing from Firestore.
     * Note: Associated images in Storage are not deleted here to avoid data loss;
     * a Cloud Function should handle Storage cleanup on property deletion.
     *
     * @param propertyId The property's document ID.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun deleteProperty(propertyId: String): Resource<Unit> {
        return dataManager.deleteProperty(propertyId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verification
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Submits a property for admin verification by updating its status field.
     *
     * @param propertyId The property's document ID.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun submitForVerification(propertyId: String): Resource<Unit> {
        return dataManager.updateProperty(
            propertyId,
            mapOf(
                "verificationStatus" to "pending",
                "submittedForVerificationAt" to System.currentTimeMillis()
            )
        )
    }
}

