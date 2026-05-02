package com.example.havenhub.remote

import android.util.Log
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Property
import com.example.havenhub.data.Review
import com.example.havenhub.data.User
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersCollection         = firestore.collection("users")
    private val propertiesCollection    = firestore.collection("properties")
    private val bookingsCollection      = firestore.collection("bookings")
    private val reviewsCollection       = firestore.collection("reviews")
    private val notificationsCollection = firestore.collection("notifications")

    private fun extractUpdatedAt(doc: com.google.firebase.firestore.DocumentSnapshot): Long? {
        return when (val raw = doc.get("updatedAt")) {
            is Long      -> raw
            is Timestamp -> raw.toDate().time
            else         -> null
        }
    }

    private fun parseProperty(doc: com.google.firebase.firestore.DocumentSnapshot): Property? {
        return try {
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
                pricePerNight     = doc.getDouble("pricePerNight")     ?: 0.0,
                pricePerMonth     = doc.getDouble("pricePerMonth"),
                pricePerWeek      = doc.getDouble("pricePerWeek"),
                securityDeposit   = doc.getDouble("securityDeposit")   ?: 0.0,
                bedrooms          = (doc.getLong("bedrooms")           ?: 1L).toInt(),
                bathrooms         = (doc.getLong("bathrooms")          ?: 1L).toInt(),
                maxGuests         = (doc.getLong("maxGuests")          ?: 2L).toInt(),
                areaSqFt          = doc.getDouble("areaSqFt"),
                floor             = doc.getLong("floor")?.toInt(),
                imageUrls         = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                pt1DocumentUrl    = doc.getString("pt1DocumentUrl")    ?: "",
                amenities         = (doc.get("amenities") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
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
        } catch (e: Exception) { null }
    }

    // ── User ─────────────────────────────────────────────────────────────────

    suspend fun saveUser(user: User): Resource<Unit> {
        return try {
            usersCollection.document(user.userId).set(user).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to save user") }
    }

    suspend fun getUser(uid: String): Resource<User> {
        return try {
            val directSnapshot = usersCollection.document(uid).get().await()
            if (directSnapshot.exists()) {
                val user = directSnapshot.toObject(User::class.java)
                if (user != null) return Resource.Success(user)
            }
            val querySnapshot = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
            if (!querySnapshot.isEmpty) {
                val user = querySnapshot.documents.first().toObject(User::class.java)
                if (user != null) return Resource.Success(user)
            }
            Resource.Error("User not found")
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch user") }
    }

    suspend fun updateUserFields(uid: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            val directSnapshot = usersCollection.document(uid).get().await()
            val docId = if (directSnapshot.exists()) uid
            else {
                val q = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
                q.documents.firstOrNull()?.id ?: return Resource.Error("User document not found")
            }
            usersCollection.document(docId).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to update user") }
    }

    suspend fun deleteUser(uid: String): Resource<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to delete user") }
    }

    // ── Property ─────────────────────────────────────────────────────────────

    suspend fun addProperty(property: Property): Resource<String> {
        return try {
            val docRef = propertiesCollection.document()
            docRef.set(property.copy(propertyId = docRef.id)).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to add property") }
    }

    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection.get().await()
            Resource.Success(snapshot.documents.mapNotNull { parseProperty(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch properties") }
    }

    suspend fun getPropertiesByOwner(ownerId: String): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection.whereEqualTo("ownerId", ownerId).get().await()
            Resource.Success(snapshot.documents.mapNotNull { parseProperty(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch owner properties") }
    }

    suspend fun getPropertyById(propertyId: String): Resource<Property> {
        return try {
            val doc = propertiesCollection.document(propertyId).get().await()
            Resource.Success(parseProperty(doc) ?: return Resource.Error("Property not found"))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch property") }
    }

    suspend fun updateProperty(propertyId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to update property") }
    }

    suspend fun deleteProperty(propertyId: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to delete property") }
    }

    // ── Favourites ✦ ─────────────────────────────────────────────────────────

    suspend fun addFavourite(userId: String, propertyId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).collection("favourites").document(propertyId)
                .set(mapOf("propertyId" to propertyId, "addedAt" to FieldValue.serverTimestamp()))
                .await()
            Log.d("HAVEN_FAV", "addFavourite SUCCESS: userId=$userId propertyId=$propertyId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("HAVEN_FAV", "addFavourite FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to add favourite")
        }
    }

    suspend fun removeFavourite(userId: String, propertyId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).collection("favourites").document(propertyId)
                .delete().await()
            Log.d("HAVEN_FAV", "removeFavourite SUCCESS: userId=$userId propertyId=$propertyId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("HAVEN_FAV", "removeFavourite FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to remove favourite")
        }
    }

    suspend fun getFavouriteIds(userId: String): Resource<List<String>> {
        return try {
            val snapshot = usersCollection.document(userId).collection("favourites").get().await()
            val ids = snapshot.documents.map { it.id }
            Log.d("HAVEN_FAV", "getFavouriteIds: userId=$userId ids=$ids")
            Resource.Success(ids)
        } catch (e: Exception) {
            Log.e("HAVEN_FAV", "getFavouriteIds FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch favourite IDs")
        }
    }

    // ✅ FIX: whereIn("__name__") kaam nahi karta — direct .document(id).get() use karo
    suspend fun getFavouriteProperties(userId: String): Resource<List<Property>> {
        return try {
            val idsResult = getFavouriteIds(userId)
            if (idsResult is Resource.Error) return Resource.Error(idsResult.message)
            val ids = (idsResult as Resource.Success).data
            if (ids.isEmpty()) return Resource.Success(emptyList())

            val allProperties = mutableListOf<Property>()
            for (propertyId in ids) {
                try {
                    val doc = propertiesCollection.document(propertyId).get().await()
                    if (doc.exists()) {
                        parseProperty(doc)?.let { allProperties.add(it) }
                    } else {
                        Log.w("HAVEN_FAV", "Property $propertyId exist nahi karti — orphan")
                    }
                } catch (e: Exception) {
                    Log.e("HAVEN_FAV", "Property $propertyId fetch fail: ${e.localizedMessage}")
                }
            }
            Log.d("HAVEN_FAV", "getFavouriteProperties result: ${allProperties.size} properties")
            Resource.Success(allProperties)
        } catch (e: Exception) {
            Log.e("HAVEN_FAV", "getFavouriteProperties FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch favourite properties")
        }
    }

    suspend fun isFavourite(userId: String, propertyId: String): Boolean {
        return try {
            usersCollection.document(userId).collection("favourites").document(propertyId)
                .get().await().exists()
        } catch (e: Exception) { false }
    }

    // ── Booking ──────────────────────────────────────────────────────────────

    suspend fun createBooking(booking: Booking): Resource<String> {
        return try {
            val docRef = bookingsCollection.document()
            docRef.set(booking.copy(bookingId = docRef.id)).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to create booking") }
    }

    suspend fun getBookingById(bookingId: String): Resource<Booking> {
        return try {
            val snapshot = bookingsCollection.document(bookingId).get().await()
            Resource.Success(snapshot.toObject(Booking::class.java) ?: return Resource.Error("Booking not found"))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch booking") }
    }

    suspend fun getAllBookings(): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.get().await()
            Resource.Success(snapshot.toObjects(Booking::class.java).sortedByDescending { it.createdAt })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch all bookings") }
    }

    suspend fun getBookingsByTenantId(tenantId: String): List<Booking> {
        return try {
            bookingsCollection.whereEqualTo("tenantId", tenantId).get().await()
                .toObjects(Booking::class.java).sortedByDescending { it.createdAt }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getBookingsByLandlordId(landlordId: String): List<Booking> {
        return try {
            bookingsCollection.whereEqualTo("landlordId", landlordId).get().await()
                .toObjects(Booking::class.java).sortedByDescending { it.createdAt }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId).update("status", status).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to update booking status") }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    suspend fun sendNotification(notificationData: Map<String, Any>): Resource<Unit> {
        return try {
            notificationsCollection.add(notificationData.toMutableMap().apply {
                put("createdAt", FieldValue.serverTimestamp())
            }).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to send notification") }
    }

    // ── Review ────────────────────────────────────────────────────────────────

    suspend fun addReview(review: Review): Resource<String> {
        return try {
            val docRef = reviewsCollection.document()
            docRef.set(review.copy(reviewId = docRef.id)).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to add review") }
    }

    suspend fun getReviewsByProperty(propertyId: String): Resource<List<Review>> {
        return try {
            val snapshot = reviewsCollection.whereEqualTo("propertyId", propertyId).get().await()
            Resource.Success(snapshot.toObjects(Review::class.java).sortedByDescending { it.createdAt })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch reviews") }
    }
}
