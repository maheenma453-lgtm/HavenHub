package com.example.havenhub.repository

import android.net.Uri
import android.util.Log
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.data.RentalPackage
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

    // Firestore collection reference for rental packages
    private val packagesCol   = firestore.collection("rentalPackages")

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

    // ══════════════════════════════════════════════════════════════════════════
    // ── PROPERTY CRUD ─────────────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════════

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
     * Approve a property and notify the landlord.
     * Also notifies interested tenants optionally.
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
                    Log.d("PROPERTY_REPO", "Property approved — notification sent to landlord $ownerId")
                }

                // Notify tenants who were interested in this property (optional)
                sendApprovalAlertToInterestedTenants(propertyId, propertyTitle)

            } catch (e: Exception) {
                Log.e("PROPERTY_REPO", "approveProperty notification error: ${e.localizedMessage}")
            }
        }
        return result
    }

    /**
     * Reject a property and notify the landlord with the reason.
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
                    Log.d("PROPERTY_REPO", "Property rejected — notification sent to landlord $ownerId")
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

    // ── Notification helpers ──────────────────────────────────────────────────

    /**
     * Notify all admins that a new property is pending review.
     */
    private suspend fun sendPendingPropertyNotificationToAdmins(
        propertyId   : String,
        propertyTitle: String,
        landlordName : String
    ) {
        try {
            val adminsSnapshot = usersCol.whereEqualTo("role", "admin").get().await()
            for (adminDoc in adminsSnapshot.documents) {
                val adminId = adminDoc.id
                if (adminId.isNotBlank()) {
                    notificationRepository.sendNewPropertyPendingNotification(
                        adminId       = adminId,
                        propertyId    = propertyId,
                        propertyTitle = propertyTitle,
                        landlordName  = landlordName
                    )
                }
            }
            Log.d("PROPERTY_REPO", "Pending property notification sent to all admins")
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "sendPendingPropertyNotificationToAdmins error: ${e.localizedMessage}")
        }
    }

    /**
     * Optionally notify tenants who bookmarked or viewed this property.
     * Currently logs only — extend as needed.
     */
    private suspend fun sendApprovalAlertToInterestedTenants(
        propertyId   : String,
        propertyTitle: String
    ) {
        try {
            Log.d("PROPERTY_REPO", "Approval alert for interested tenants — propertyId: $propertyId, title: $propertyTitle")
            // TODO: query user_preferences or favourites collection and notify tenants
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "sendApprovalAlertToInterestedTenants error: ${e.localizedMessage}")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ── RENTAL PACKAGES ───────────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Create a new rental package in Firestore under `rentalPackages` collection.
     * Only the landlord who owns the property should call this.
     * Returns the auto-generated packageId on success.
     */
    suspend fun addRentalPackage(pkg: RentalPackage): Resource<String> {
        return try {
            val docRef = packagesCol.add(pkg).await()
            Log.d("PROPERTY_REPO", "Rental package created — id: ${docRef.id}")
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "addRentalPackage error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to create package")
        }
    }

    /**
     * Fetch all rental packages linked to a specific property.
     * Used in AddRentalPackageScreen to show existing packages.
     */
    suspend fun getPackagesByProperty(propertyId: String): Resource<List<RentalPackage>> {
        return try {
            val snapshot = packagesCol
                .whereEqualTo("propertyId", propertyId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull {
                it.toObject(RentalPackage::class.java)
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "getPackagesByProperty error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch packages")
        }
    }

    /**
     * Fetch all rental packages created by a specific landlord.
     * Useful for landlord dashboard to list all their packages.
     */
    suspend fun getPackagesByLandlord(landlordId: String): Resource<List<RentalPackage>> {
        return try {
            val snapshot = packagesCol
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull {
                it.toObject(RentalPackage::class.java)
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "getPackagesByLandlord error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch packages")
        }
    }

    /**
     * Delete a rental package by its Firestore document ID.
     */
    suspend fun deleteRentalPackage(packageId: String): Resource<Unit> {
        return try {
            packagesCol.document(packageId).delete().await()
            Log.d("PROPERTY_REPO", "Rental package deleted — id: $packageId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PROPERTY_REPO", "deleteRentalPackage error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete package")
        }
    }
}