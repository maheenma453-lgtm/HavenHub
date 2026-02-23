package com.example.havenhub.remote
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.havenhub.data.model.Booking
import com.havenhub.data.model.Property
import com.havenhub.data.model.Review
import com.havenhub.data.model.User
import com.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseDataManager
 *
 * Central Firestore data access layer for HavenHub.
 * Handles CRUD operations for all top-level Firestore collections.
 *
 * Firestore Collection Structure:
 * ├── users/          → User profiles
 * ├── properties/     → Property listings
 * ├── bookings/       → Booking records
 * └── reviews/        → Property reviews
 *
 * All suspend functions are safe to call from a coroutine scope.
 * Results are wrapped in [Resource] for unified error handling.
 */
@Singleton
class FirebaseDataManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Collection References
    // ─────────────────────────────────────────────────────────────────────────

    private val usersCollection       = firestore.collection("users")
    private val propertiesCollection  = firestore.collection("properties")
    private val bookingsCollection    = firestore.collection("bookings")
    private val reviewsCollection     = firestore.collection("reviews")

    // ─────────────────────────────────────────────────────────────────────────
    // User Operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates or updates a user document in Firestore.
     * Uses [User.uid] as the document ID.
     *
     * @param user The [User] object to save.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun saveUser(user: User): Resource<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save user")
        }
    }

    /**
     * Fetches a single user document by UID.
     *
     * @param uid Firebase Auth UID.
     * @return [Resource.Success] with [User], or [Resource.Error] if not found.
     */
    suspend fun getUser(uid: String): Resource<User> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
                ?: return Resource.Error("User not found")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user")
        }
    }

    /**
     * Updates specific fields of a user document.
     * Only the provided fields are modified (no full overwrite).
     *
     * @param uid    The user's UID.
     * @param fields Map of field names to new values.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun updateUserFields(uid: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            usersCollection.document(uid).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update user")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property Operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves a new property listing to Firestore.
     * A new document ID is auto-generated and set on the [Property] object.
     *
     * @param property The [Property] object to create.
     * @return [Resource.Success] with the generated property ID, or [Resource.Error].
     */
    suspend fun addProperty(property: Property): Resource<String> {
        return try {
            val docRef = propertiesCollection.document()
            val newProperty = property.copy(id = docRef.id)
            docRef.set(newProperty).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to add property")
        }
    }

    /**
     * Fetches all approved/active property listings.
     * Results are ordered by creation date (newest first).
     *
     * @return [Resource.Success] with a list of [Property], or [Resource.Error].
     */
    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection
                .whereEqualTo("isApproved", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val properties = snapshot.toObjects(Property::class.java)
            Resource.Success(properties)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch properties")
        }
    }

    /**
     * Fetches all properties listed by a specific landlord/host.
     *
     * @param ownerId UID of the property owner.
     * @return [Resource.Success] with a list of [Property], or [Resource.Error].
     */
    suspend fun getPropertiesByOwner(ownerId: String): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection
                .whereEqualTo("ownerId", ownerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Property::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch owner properties")
        }
    }

    /**
     * Fetches a single property document by its ID.
     *
     * @param propertyId The Firestore document ID of the property.
     * @return [Resource.Success] with [Property], or [Resource.Error].
     */
    suspend fun getPropertyById(propertyId: String): Resource<Property> {
        return try {
            val snapshot = propertiesCollection.document(propertyId).get().await()
            val property = snapshot.toObject(Property::class.java)
                ?: return Resource.Error("Property not found")
            Resource.Success(property)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch property")
        }
    }

    /**
     * Updates specific fields of an existing property document.
     *
     * @param propertyId The property's document ID.
     * @param fields     Map of field names to updated values.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun updateProperty(propertyId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update property")
        }
    }

    /**
     * Deletes a property document permanently.
     *
     * @param propertyId The property's document ID.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun deleteProperty(propertyId: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete property")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Booking Operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new booking document in Firestore.
     *
     * @param booking The [Booking] object to persist.
     * @return [Resource.Success] with the booking ID, or [Resource.Error].
     */
    suspend fun createBooking(booking: Booking): Resource<String> {
        return try {
            val docRef = bookingsCollection.document()
            val newBooking = booking.copy(id = docRef.id)
            docRef.set(newBooking).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create booking")
        }
    }

    /**
     * Fetches all bookings made by a specific tenant/user.
     *
     * @param userId UID of the tenant.
     * @return [Resource.Success] with a list of [Booking], or [Resource.Error].
     */
    suspend fun getBookingsByUser(userId: String): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("tenantId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Booking::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch bookings")
        }
    }

    /**
     * Updates the status of a booking (e.g., confirmed, cancelled).
     *
     * @param bookingId The booking document ID.
     * @param status    New status string (e.g., "confirmed", "cancelled").
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun updateBookingStatus(bookingId: String, status: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId)
                .update("status", status).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update booking status")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Review Operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves a new review for a property.
     *
     * @param review The [Review] object to persist.
     * @return [Resource.Success] with the review ID, or [Resource.Error].
     */
    suspend fun addReview(review: Review): Resource<String> {
        return try {
            val docRef = reviewsCollection.document()
            val newReview = review.copy(id = docRef.id)
            docRef.set(newReview).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to add review")
        }
    }

    /**
     * Fetches all reviews for a specific property.
     *
     * @param propertyId The Firestore document ID of the property.
     * @return [Resource.Success] with a list of [Review], or [Resource.Error].
     */
    suspend fun getReviewsByProperty(propertyId: String): Resource<List<Review>> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("propertyId", propertyId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Review::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch reviews")
        }
    }
}

