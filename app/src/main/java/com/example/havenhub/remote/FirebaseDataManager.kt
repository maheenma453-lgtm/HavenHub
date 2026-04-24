package com.example.havenhub.remote

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

    // ── Helper — updatedAt ko safely Long mein convert karo ──────────────────

    private fun extractUpdatedAt(doc: com.google.firebase.firestore.DocumentSnapshot): Long? {
        return when (val raw = doc.get("updatedAt")) {
            is Long      -> raw
            is Timestamp -> raw.toDate().time
            else         -> null
        }
    }

    // ── Helper — DocumentSnapshot se manually Property banao ─────────────────
    // toObject() fail hota hai jab koi bhi field missing/mismatch ho
    // Yeh function har field ko safely handle karta hai

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
                imageUrls         = (doc.get("imageUrls") as? List<*>)
                    ?.filterIsInstance<String>()   ?: emptyList(),
                pt1DocumentUrl    = doc.getString("pt1DocumentUrl")    ?: "",
                amenities         = (doc.get("amenities") as? List<*>)
                    ?.filterIsInstance<String>()   ?: emptyList(),
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
                // ✅ FIX: Constructor mein 'available' aur 'featured' hain
                //         'isAvailable' aur 'isFeatured' sirf @Exclude getters hain
                available         = doc.getBoolean("isAvailable")      ?: true,
                featured          = doc.getBoolean("isFeatured")       ?: false,
                createdAt         = doc.getTimestamp("createdAt"),
                updatedAt         = extractUpdatedAt(doc)
            )
        } catch (e: Exception) {
            null
        }
    }

    // ── User ─────────────────────────────────────────────────────────────────

    suspend fun saveUser(user: User): Resource<Unit> {
        return try {
            usersCollection.document(user.userId).set(user).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save user")
        }
    }

    suspend fun getUser(uid: String): Resource<User> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val user     = snapshot.toObject(User::class.java)
                ?: return Resource.Error("User not found")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user")
        }
    }

    suspend fun updateUserFields(uid: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            usersCollection.document(uid).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update user")
        }
    }

    suspend fun deleteUser(uid: String): Resource<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete user")
        }
    }

    // ── Property ─────────────────────────────────────────────────────────────

    suspend fun addProperty(property: Property): Resource<String> {
        return try {
            val docRef      = propertiesCollection.document()
            val newProperty = property.copy(propertyId = docRef.id)
            docRef.set(newProperty).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to add property")
        }
    }

    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot   = propertiesCollection.get().await()
            val properties = snapshot.documents
                .mapNotNull { doc -> parseProperty(doc) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Resource.Success(properties)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch properties")
        }
    }

    suspend fun getPropertiesByOwner(ownerId: String): Resource<List<Property>> {
        return try {
            val snapshot   = propertiesCollection
                .whereEqualTo("ownerId", ownerId)
                .get()
                .await()
            val properties = snapshot.documents
                .mapNotNull { doc -> parseProperty(doc) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Resource.Success(properties)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch owner properties")
        }
    }

    suspend fun getPropertyById(propertyId: String): Resource<Property> {
        return try {
            val doc      = propertiesCollection.document(propertyId).get().await()
            val property = parseProperty(doc)
                ?: return Resource.Error("Property not found")
            Resource.Success(property)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch property")
        }
    }

    suspend fun updateProperty(propertyId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update property")
        }
    }

    suspend fun deleteProperty(propertyId: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete property")
        }
    }

    // ── Booking ──────────────────────────────────────────────────────────────

    suspend fun createBooking(booking: Booking): Resource<String> {
        return try {
            val docRef     = bookingsCollection.document()
            val newBooking = booking.copy(bookingId = docRef.id)
            docRef.set(newBooking).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create booking")
        }
    }

    suspend fun getBookingById(bookingId: String): Resource<Booking> {
        return try {
            val snapshot = bookingsCollection.document(bookingId).get().await()
            val booking  = snapshot.toObject(Booking::class.java)
                ?: return Resource.Error("Booking not found")
            Resource.Success(booking)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch booking")
        }
    }

    suspend fun getAllBookings(): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.get().await()
            val bookings = snapshot.toObjects(Booking::class.java)
                .sortedByDescending { it.createdAt }
            Resource.Success(bookings)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch all bookings")
        }
    }

    suspend fun getBookingsByTenantId(tenantId: String): List<Booking> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
            snapshot.toObjects(Booking::class.java)
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBookingsByLandlordId(landlordId: String): List<Booking> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            snapshot.toObjects(Booking::class.java)
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId)
                .update("status", status)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update booking status")
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    suspend fun sendNotification(notificationData: Map<String, Any>): Resource<Unit> {
        return try {
            val dataWithTime = notificationData.toMutableMap()
            dataWithTime["createdAt"] = FieldValue.serverTimestamp()
            notificationsCollection.add(dataWithTime).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send notification")
        }
    }

    // ── Review ────────────────────────────────────────────────────────────────

    suspend fun addReview(review: Review): Resource<String> {
        return try {
            val docRef    = reviewsCollection.document()
            val newReview = review.copy(reviewId = docRef.id)
            docRef.set(newReview).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to add review")
        }
    }

    suspend fun getReviewsByProperty(propertyId: String): Resource<List<Review>> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("propertyId", propertyId)
                .get()
                .await()
            val reviews = snapshot.toObjects(Review::class.java)
                .sortedByDescending { it.createdAt }
            Resource.Success(reviews)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch reviews")
        }
    }
}