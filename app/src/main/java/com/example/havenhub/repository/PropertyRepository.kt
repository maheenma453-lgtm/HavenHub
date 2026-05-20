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
    private val packagesCol   = firestore.collection("rental_packages")

    // ══════════════════════════════════════════════════════════════════════════
    // COORDINATE PARSE HELPER
    //
    // Two property types exist in Firestore:
    //
    //   1. App-added (auto-id, landlord submissions via AddPropertyScreen):
    //      - Top-level latitude  = 31.4504   ← non-zero, use this
    //      - Top-level longitude = 73.1350
    //      - location.latitude  = 0          ← PropertyViewModel doesn't fill nested obj
    //      - location.longitude = 0
    //
    //   2. Manually seeded (prop_001 … prop_012):
    //      - Top-level latitude  = missing / 0
    //      - location.latitude  = 31.5204    ← real coords here
    //      - location.longitude = 74.3587
    //
    // Fix: try top-level first → fallback to nested location → else 0.0
    // Property.resolvedLatitude will then try city-name lookup if still 0.0.
    // ══════════════════════════════════════════════════════════════════════════
    @Suppress("UNCHECKED_CAST")
    private fun resolveCoords(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): Pair<Double, Double> {
        val topLat = doc.getDouble("latitude")
        val topLng = doc.getDouble("longitude")

        val locationMap = doc.get("location") as? Map<String, Any>
        val nestedLat   = (locationMap?.get("latitude")  as? Number)?.toDouble()
        val nestedLng   = (locationMap?.get("longitude") as? Number)?.toDouble()

        val lat = when {
            topLat    != null && topLat    != 0.0 -> topLat
            nestedLat != null && nestedLat != 0.0 -> nestedLat
            else -> 0.0
        }
        val lng = when {
            topLng    != null && topLng    != 0.0 -> topLng
            nestedLng != null && nestedLng != 0.0 -> nestedLng
            else -> 0.0
        }
        return Pair(lat, lng)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // fetchApproved() — direct Firestore query, APPROVED only
    //
    // Uses resolveCoords() so both app-added AND manually seeded properties
    // get correct lat/lng — fixes missing pins on ExploreMapScreen.
    // ══════════════════════════════════════════════════════════════════════════
    private suspend fun fetchApproved(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCol
                .whereEqualTo("status", PropertyStatus.APPROVED.name)
                .get()
                .await()

            val properties = snapshot.documents.mapNotNull { doc ->
                try {
                    val (lat, lng) = resolveCoords(doc)   // ← FIXED

                    Property(
                        propertyId        = doc.id,
                        ownerId           = doc.getString("ownerId")           ?: "",
                        ownerName         = doc.getString("ownerName")         ?: "",
                        title             = doc.getString("title")             ?: "",
                        description       = doc.getString("description")       ?: "",
                        propertyType      = doc.getString("propertyType")      ?: "APARTMENT",
                        status            = doc.getString("status")            ?: "PENDING",
                        address           = doc.getString("address")           ?: "",
                        city              = doc.getString("city")              ?: "",
                        latitude          = lat,           // ← FIXED (was: doc.getDouble("latitude") ?: 0.0)
                        longitude         = lng,           // ← FIXED (was: doc.getDouble("longitude") ?: 0.0)
                        pricePerNight     = doc.getDouble("pricePerNight")     ?: 0.0,
                        pricePerMonth     = doc.getDouble("pricePerMonth"),
                        pricePerWeek      = doc.getDouble("pricePerWeek"),
                        securityDeposit   = doc.getDouble("securityDeposit")   ?: 0.0,
                        bedrooms          = (doc.getLong("bedrooms")           ?: 1L).toInt(),
                        bathrooms         = (doc.getLong("bathrooms")          ?: 1L).toInt(),
                        maxGuests         = (doc.getLong("maxGuests")          ?: 2L).toInt(),
                        areaSqFt          = doc.getDouble("areaSqFt"),
                        floor             = doc.getLong("floor")?.toInt(),
                        imageUrls         = (doc.get("imageUrls") as? List<*>)
                            ?.filterIsInstance<String>()                       ?: emptyList(),
                        pt1DocumentUrl    = doc.getString("pt1DocumentUrl")    ?: "",
                        amenities         = (doc.get("amenities") as? List<*>)
                            ?.filterIsInstance<String>()                       ?: emptyList(),
                        drawableImageName = doc.getString("drawableImageName") ?: "",
                        petsAllowed       = doc.getBoolean("petsAllowed")      ?: false,
                        smokingAllowed    = doc.getBoolean("smokingAllowed")   ?: false,
                        partiesAllowed    = doc.getBoolean("partiesAllowed")   ?: false,
                        checkInTime       = doc.getString("checkInTime")       ?: "14:00",
                        checkOutTime      = doc.getString("checkOutTime")      ?: "11:00",
                        minNights         = (doc.getLong("minNights")          ?: 1L).toInt(),
                        averageRating     = (doc.getDouble("averageRating")    ?: 0.0).toFloat(),
                        reviewCount       = (doc.getLong("reviewCount")        ?: 0L).toInt(),
                        adminNote         = doc.getString("adminNote")         ?: "",
                        available         = doc.getBoolean("isAvailable")      ?: true,
                        featured          = doc.getBoolean("isFeatured")       ?: false,
                        createdAt         = doc.getTimestamp("createdAt"),
                        updatedAt         = doc.getTimestamp("updatedAt")
                    )
                } catch (e: Exception) {
                    Log.e("PROP_REPO", "fetchApproved parse fail ${doc.id}: ${e.localizedMessage}")
                    null
                }
            }.sortedByDescending { it.createdAt?.seconds ?: 0L }

            Log.d("PROP_REPO", "fetchApproved: ${properties.size} APPROVED properties fetched")
            Resource.Success(properties)

        } catch (e: Exception) {
            Log.e("PROP_REPO", "fetchApproved FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch approved properties")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROPERTY CRUD — CREATE
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun addProperty(
        property  : Property,
        imageUris : List<Uri> = emptyList(),
        pt1Uri    : Uri?      = null
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
            status         = PropertyStatus.PENDING.name   // Always PENDING on create
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

    // ══════════════════════════════════════════════════════════════════════════
    // READ — Tenant facing (APPROVED only)
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun getApprovedProperties(): Resource<List<Property>> = fetchApproved()

    suspend fun getAllProperties(): Resource<List<Property>> = fetchApproved()

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

    // Landlord ke apne properties — SAARI statuses (PENDING, APPROVED, REJECTED)
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

    // ══════════════════════════════════════════════════════════════════════════
    // READ — Admin facing (ALL statuses)
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun getAllPropertiesForAdmin(): Resource<List<Property>> =
        dataManager.getAllProperties()

    suspend fun getPendingProperties(): Resource<List<Property>> {
        return when (val result = dataManager.getAllProperties()) {
            is Resource.Success -> Resource.Success(
                result.data.filter {
                    it.status.equals(PropertyStatus.PENDING.name, ignoreCase = true)
                }
            )
            is Resource.Error   -> Resource.Error(result.message)
            Resource.Loading    -> Resource.Error("Unexpected loading state")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun updateProperty(
        propertyId : String,
        fields     : Map<String, Any>
    ): Resource<Unit> = dataManager.updateProperty(propertyId, fields)

    suspend fun approveProperty(
        propertyId : String,
        adminNote  : String = ""
    ): Resource<Unit> {
        val result = dataManager.updateProperty(
            propertyId,
            mapOf(
                "status"    to PropertyStatus.APPROVED.name,
                "adminNote" to adminNote,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        )

        if (result is Resource.Success) {
            try {
                val propDoc       = propertiesCol.document(propertyId).get().await()
                val ownerId       = propDoc.getString("ownerId")  ?: ""
                val propertyTitle = propDoc.getString("title")    ?: "Property"

                if (ownerId.isNotBlank()) {
                    notificationRepository.sendPropertyApprovedNotification(
                        ownerId       = ownerId,
                        propertyId    = propertyId,
                        propertyTitle = propertyTitle,
                        adminNote     = adminNote
                    )
                    Log.d("PROP_REPO", "✅ Property approved — notification sent to landlord $ownerId")
                }

                sendApprovalAlertToInterestedTenants(propertyId, propertyTitle)

            } catch (e: Exception) {
                Log.e("PROP_REPO", "approveProperty notification error: ${e.localizedMessage}")
            }
        }
        return result
    }

    suspend fun rejectProperty(
        propertyId : String,
        adminNote  : String
    ): Resource<Unit> {
        val result = dataManager.updateProperty(
            propertyId,
            mapOf(
                "status"    to PropertyStatus.REJECTED.name,
                "adminNote" to adminNote,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        )

        if (result is Resource.Success) {
            try {
                val propDoc       = propertiesCol.document(propertyId).get().await()
                val ownerId       = propDoc.getString("ownerId") ?: ""
                val propertyTitle = propDoc.getString("title")   ?: "Property"

                if (ownerId.isNotBlank()) {
                    notificationRepository.sendPropertyRejectedNotification(
                        ownerId       = ownerId,
                        propertyId    = propertyId,
                        propertyTitle = propertyTitle,
                        adminNote     = adminNote
                    )
                    Log.d("PROP_REPO", "✅ Property rejected — notification sent to landlord $ownerId")
                }
            } catch (e: Exception) {
                Log.e("PROP_REPO", "rejectProperty notification error: ${e.localizedMessage}")
            }
        }
        return result
    }

    suspend fun addPropertyImages(
        propertyId    : String,
        newImageUris  : List<Uri>
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

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun deleteProperty(propertyId: String): Resource<Unit> =
        dataManager.deleteProperty(propertyId)

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private suspend fun sendPendingPropertyNotificationToAdmins(
        propertyId    : String,
        propertyTitle : String,
        landlordName  : String
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
            Log.d("PROP_REPO", "Pending property notification sent to all admins")
        } catch (e: Exception) {
            Log.e("PROP_REPO", "sendPendingPropertyNotificationToAdmins error: ${e.localizedMessage}")
        }
    }

    private suspend fun sendApprovalAlertToInterestedTenants(
        propertyId    : String,
        propertyTitle : String
    ) {
        try {
            Log.d("PROP_REPO", "Approval alert — propertyId: $propertyId, title: $propertyTitle")
            // TODO: query favourites collection and notify tenants
        } catch (e: Exception) {
            Log.e("PROP_REPO", "sendApprovalAlertToInterestedTenants error: ${e.localizedMessage}")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RENTAL PACKAGES
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun addRentalPackage(pkg: RentalPackage): Resource<String> {
        return try {
            val docRef = packagesCol.add(pkg).await()
            Log.d("PROP_REPO", "Rental package created — id: ${docRef.id}")
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Log.e("PROP_REPO", "addRentalPackage error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to create package")
        }
    }

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
            Log.e("PROP_REPO", "getPackagesByProperty error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch packages")
        }
    }

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
            Log.e("PROP_REPO", "getPackagesByLandlord error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch packages")
        }
    }

    suspend fun deleteRentalPackage(packageId: String): Resource<Unit> {
        return try {
            packagesCol.document(packageId).delete().await()
            Log.d("PROP_REPO", "Rental package deleted — id: $packageId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PROP_REPO", "deleteRentalPackage error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete package")
        }
    }
}