package com.example.havenhub.remote

import android.util.Log
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Property
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.data.PackageDuration
import com.example.havenhub.data.PackageStatus
import com.example.havenhub.data.Review
import com.example.havenhub.data.User
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersCollection          = firestore.collection("users")
    private val propertiesCollection     = firestore.collection("properties")
    private val bookingsCollection       = firestore.collection("bookings")
    private val reviewsCollection        = firestore.collection("reviews")
    private val notificationsCollection  = firestore.collection("notifications")
    private val rentalPackagesCollection = firestore.collection("rental_packages")

    // ── Helper: Booking parse karo safely ────────────────────────────────────
    // ✅ FIX: toObject use karo per-document — crash avoid hoga
    private fun parseBooking(doc: com.google.firebase.firestore.DocumentSnapshot): Booking? {
        return try {
            val booking = doc.toObject(Booking::class.java) ?: return null
            // ✅ bookingId set karo agar blank hai (manual documents mein @DocumentId kaam nahi karta)
            if (booking.bookingId.isBlank()) {
                booking.copy(bookingId = doc.id)
            } else {
                booking
            }
        } catch (e: Exception) {
            Log.e("HAVEN_BOOKING", "parseBooking FAIL ${doc.id}: ${e.localizedMessage}")
            null
        }
    }

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

    private fun parseReview(doc: com.google.firebase.firestore.DocumentSnapshot): Review? {
        return try {
            Review(
                reviewId            = doc.id,
                bookingId           = doc.getString("bookingId")           ?: "",
                propertyId          = doc.getString("propertyId")          ?: "",
                reviewerId          = doc.getString("reviewerId")          ?: "",
                reviewerName        = doc.getString("reviewerName")        ?: "",
                reviewerAvatarUrl   = doc.getString("reviewerAvatarUrl")   ?: "",
                landlordId          = doc.getString("landlordId")          ?: "",
                overallRating       = doc.getDouble("overallRating")?.toFloat()       ?: 0f,
                cleanlinessRating   = doc.getDouble("cleanlinessRating")?.toFloat()   ?: 0f,
                accuracyRating      = doc.getDouble("accuracyRating")?.toFloat()      ?: 0f,
                communicationRating = doc.getDouble("communicationRating")?.toFloat() ?: 0f,
                checkInRating       = doc.getDouble("checkInRating")?.toFloat()       ?: 0f,
                valueRating         = doc.getDouble("valueRating")?.toFloat()         ?: 0f,
                locationRating      = doc.getDouble("locationRating")?.toFloat()      ?: 0f,
                comment             = doc.getString("comment")             ?: "",
                photoUrls           = (doc.get("photoUrls") as? List<*>)
                    ?.filterIsInstance<String>()                           ?: emptyList(),
                landlordReply       = doc.getString("landlordReply")       ?: "",
                landlordRepliedAt   = doc.getTimestamp("landlordRepliedAt"),
                isVisible           = doc.getBoolean("isVisible")          ?: true,
                moderationNote      = doc.getString("moderationNote")      ?: "",
                createdAt           = doc.getTimestamp("createdAt"),
                updatedAt           = doc.getTimestamp("updatedAt")
            )
        } catch (e: Exception) {
            Log.e("HAVEN_REVIEW", "parseReview FAIL ${doc.id}: ${e.localizedMessage}")
            null
        }
    }

    private fun parseRentalPackage(doc: com.google.firebase.firestore.DocumentSnapshot): RentalPackage? {
        return try {
            val statusStr  = doc.getString("status")       ?: "ACTIVE"
            val status     = try { PackageStatus.valueOf(statusStr)   } catch (e: Exception) { PackageStatus.ACTIVE }

            val durationStr = doc.getString("durationType") ?: "FLEXIBLE"
            val duration    = try { PackageDuration.valueOf(durationStr) } catch (e: Exception) { PackageDuration.FLEXIBLE }

            RentalPackage(
                packageId               = doc.id,
                propertyId              = doc.getString("propertyId")              ?: "",
                propertyTitle           = doc.getString("propertyTitle")           ?: "",
                landlordId              = doc.getString("landlordId")              ?: "",
                packageName             = doc.getString("packageName")             ?: "",
                description             = doc.getString("description")             ?: "",
                badgeLabel              = doc.getString("badgeLabel")              ?: "",
                durationType            = duration,
                fixedNights             = doc.getLong("fixedNights")?.toInt(),
                minNights               = (doc.getLong("minNights")                ?: 1L).toInt(),
                maxNights               = doc.getLong("maxNights")?.toInt(),
                discountedPricePerNight = doc.getDouble("discountedPricePerNight") ?: 0.0,
                originalPricePerNight   = doc.getDouble("originalPricePerNight")   ?: 0.0,
                flatDiscount            = doc.getDouble("flatDiscount"),
                discountPercentage      = doc.getDouble("discountPercentage")?.toFloat(),
                inclusions              = (doc.get("inclusions") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                availableFrom           = doc.getTimestamp("availableFrom"),
                availableTo             = doc.getTimestamp("availableTo"),
                blackoutDates           = (doc.get("blackoutDates") as? List<*>)
                    ?.filterIsInstance<com.google.firebase.Timestamp>() ?: emptyList(),
                totalSlots              = doc.getLong("totalSlots")?.toInt(),
                bookedSlots             = (doc.getLong("bookedSlots")              ?: 0L).toInt(),
                status                  = status,
                createdAt               = doc.getTimestamp("createdAt"),
                updatedAt               = doc.getTimestamp("updatedAt")
            )
        } catch (e: Exception) {
            Log.e("HAVEN_PKG", "parseRentalPackage FAILED for ${doc.id}: ${e.localizedMessage}")
            null
        }
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

    suspend fun searchProperties(query: String): Resource<List<Property>> {
        return try {
            val q = query.trim().lowercase()
            val snapshot = propertiesCollection
                .whereEqualTo("status", "APPROVED")
                .get().await()
            val results = snapshot.documents
                .mapNotNull { parseProperty(it) }
                .filter { property ->
                    property.title.lowercase().contains(q) ||
                            property.city.lowercase().contains(q)
                }
                .sortedBy { it.title }
            Log.d("HAVEN_SEARCH", "searchProperties[$query]: ${results.size} results")
            Resource.Success(results)
        } catch (e: Exception) {
            Log.e("HAVEN_SEARCH", "searchProperties FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Search failed")
        }
    }

    // ── Favourites ────────────────────────────────────────────────────────────

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

    // ── Booking ───────────────────────────────────────────────────────────────
    // ✅ KEY FIX: Saari booking functions ab parseBooking() use karti hain
    // parseBooking() manually set karta hai bookingId = doc.id agar blank ho
    // Isse manual documents (booking_001) aur auto documents dono fetch honge

    suspend fun createBooking(booking: Booking): Resource<String> {
        return try {
            val docRef = bookingsCollection.document()
            docRef.set(booking.copy(bookingId = docRef.id)).await()
            Log.d("HAVEN_BOOKING", "createBooking SUCCESS: ${docRef.id}")
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Log.e("HAVEN_BOOKING", "createBooking FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to create booking")
        }
    }

    suspend fun getBookingById(bookingId: String): Resource<Booking> {
        return try {
            val snapshot = bookingsCollection.document(bookingId).get().await()
            val booking  = parseBooking(snapshot)
                ?: return Resource.Error("Booking not found")
            Log.d("HAVEN_BOOKING", "getBookingById: $bookingId found")
            Resource.Success(booking)
        } catch (e: Exception) {
            Log.e("HAVEN_BOOKING", "getBookingById FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch booking")
        }
    }

    // ✅ FIX: toObjects() ki bajaye parseBooking() use karo
    // toObjects() @DocumentId set nahi karta manual documents mein
    suspend fun getAllBookings(): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.get().await()
            val bookings = snapshot.documents
                .mapNotNull { parseBooking(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Log.d("HAVEN_BOOKING", "getAllBookings: ${bookings.size} total")
            Resource.Success(bookings)
        } catch (e: Exception) {
            Log.e("HAVEN_BOOKING", "getAllBookings FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch all bookings")
        }
    }

    // ✅ FIX: tenantId se query — parseBooking() se bookingId bhi sahi milega
    suspend fun getBookingsByTenantId(tenantId: String): List<Booking> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
            val bookings = snapshot.documents
                .mapNotNull { parseBooking(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Log.d("HAVEN_BOOKING", "getBookingsByTenantId[$tenantId]: ${bookings.size} bookings")
            bookings
        } catch (e: Exception) {
            Log.e("HAVEN_BOOKING", "getBookingsByTenantId FAIL: ${e.localizedMessage}")
            emptyList()
        }
    }

    // ✅ FIX: landlordId se query — parseBooking() se bookingId bhi sahi milega
    suspend fun getBookingsByLandlordId(landlordId: String): List<Booking> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            val bookings = snapshot.documents
                .mapNotNull { parseBooking(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Log.d("HAVEN_BOOKING", "getBookingsByLandlordId[$landlordId]: ${bookings.size} bookings")
            bookings
        } catch (e: Exception) {
            Log.e("HAVEN_BOOKING", "getBookingsByLandlordId FAIL: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId).update("status", status).await()
            Log.d("HAVEN_BOOKING", "updateBookingStatus: $bookingId -> $status")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("HAVEN_BOOKING", "updateBookingStatus FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to update booking status")
        }
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
            val reviewData = mapOf(
                "bookingId"           to review.bookingId,
                "propertyId"          to review.propertyId,
                "reviewerId"          to review.reviewerId,
                "reviewerName"        to review.reviewerName,
                "reviewerAvatarUrl"   to review.reviewerAvatarUrl,
                "landlordId"          to review.landlordId,
                "overallRating"       to review.overallRating,
                "cleanlinessRating"   to review.cleanlinessRating,
                "accuracyRating"      to review.accuracyRating,
                "communicationRating" to review.communicationRating,
                "checkInRating"       to review.checkInRating,
                "valueRating"         to review.valueRating,
                "locationRating"      to review.locationRating,
                "comment"             to review.comment,
                "photoUrls"           to review.photoUrls,
                "landlordReply"       to review.landlordReply,
                "landlordRepliedAt"   to review.landlordRepliedAt,
                "isVisible"           to review.isVisible,
                "moderationNote"      to review.moderationNote,
                "updatedAt"           to review.updatedAt
            )
            docRef.set(reviewData).await()
            Log.d("HAVEN_REVIEW", "addReview SUCCESS: ${docRef.id}")
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Log.e("HAVEN_REVIEW", "addReview FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to add review")
        }
    }

    suspend fun getReviewsByProperty(propertyId: String): Resource<List<Review>> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("propertyId", propertyId)
                .get().await()

            val reviews = snapshot.documents
                .mapNotNull { parseReview(it) }
                .sortedByDescending { it.createdAt }

            Log.d("HAVEN_REVIEW", "getReviewsByProperty[$propertyId]: ${reviews.size} reviews")
            Resource.Success(reviews)
        } catch (e: Exception) {
            Log.e("HAVEN_REVIEW", "getReviewsByProperty FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch reviews")
        }
    }

    suspend fun getAllReviews(): Resource<List<Review>> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("isVisible", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get().await()

            val reviews = snapshot.documents.mapNotNull { parseReview(it) }
            Log.d("HAVEN_REVIEW", "getAllReviews: ${reviews.size} reviews fetched")
            Resource.Success(reviews)
        } catch (e: Exception) {
            Log.e("HAVEN_REVIEW", "getAllReviews FAIL: ${e.localizedMessage}")
            try {
                val fallbackSnapshot = reviewsCollection
                    .whereEqualTo("isVisible", true)
                    .get().await()
                val reviews = fallbackSnapshot.documents
                    .mapNotNull { parseReview(it) }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                Log.d("HAVEN_REVIEW", "getAllReviews fallback: ${reviews.size} reviews")
                Resource.Success(reviews)
            } catch (fallbackEx: Exception) {
                Log.e("HAVEN_REVIEW", "getAllReviews fallback FAIL: ${fallbackEx.localizedMessage}")
                Resource.Error(fallbackEx.localizedMessage ?: "Failed to fetch all reviews")
            }
        }
    }

    // ── Rental Packages ───────────────────────────────────────────────────────

    suspend fun getActiveRentalPackages(): Resource<List<RentalPackage>> {
        return try {
            val snapshot = rentalPackagesCollection
                .whereEqualTo("status", PackageStatus.ACTIVE.name)
                .get()
                .await()
            val packages = snapshot.documents.mapNotNull { parseRentalPackage(it) }
            Log.d("HAVEN_PKG", "getActiveRentalPackages: ${packages.size} ACTIVE packages fetched")
            Resource.Success(packages)
        } catch (e: Exception) {
            Log.e("HAVEN_PKG", "getActiveRentalPackages FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch rental packages")
        }
    }

    suspend fun getPackagesByProperty(propertyId: String): Resource<List<RentalPackage>> {
        return try {
            val snapshot = rentalPackagesCollection
                .whereEqualTo("propertyId", propertyId)
                .whereEqualTo("status", PackageStatus.ACTIVE.name)
                .get()
                .await()
            val packages = snapshot.documents.mapNotNull { parseRentalPackage(it) }
            Log.d("HAVEN_PKG", "getPackagesByProperty[$propertyId]: ${packages.size} ACTIVE packages")
            Resource.Success(packages)
        } catch (e: Exception) {
            Log.e("HAVEN_PKG", "getPackagesByProperty FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch packages for property")
        }
    }

    suspend fun getPackagesByLandlord(landlordId: String): Resource<List<RentalPackage>> {
        return try {
            val snapshot = rentalPackagesCollection
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            val packages = snapshot.documents.mapNotNull { parseRentalPackage(it) }
            Log.d("HAVEN_PKG", "getPackagesByLandlord[$landlordId]: ${packages.size} packages")
            Resource.Success(packages)
        } catch (e: Exception) {
            Log.e("HAVEN_PKG", "getPackagesByLandlord FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch landlord packages")
        }
    }

    suspend fun getRentalPackageById(packageId: String): Resource<RentalPackage> {
        return try {
            val doc = rentalPackagesCollection.document(packageId).get().await()
            val pkg = parseRentalPackage(doc) ?: return Resource.Error("Package not found")
            Resource.Success(pkg)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch package")
        }
    }

    suspend fun createRentalPackage(pkg: RentalPackage): Resource<String> {
        return try {
            val docRef = rentalPackagesCollection.document()
            docRef.set(pkg.copy(packageId = docRef.id)).await()
            Log.d("HAVEN_PKG", "createRentalPackage SUCCESS: ${docRef.id}")
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Log.e("HAVEN_PKG", "createRentalPackage FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to create rental package")
        }
    }

    suspend fun updateRentalPackage(packageId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            val updatedFields = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            rentalPackagesCollection.document(packageId).update(updatedFields).await()
            Log.d("HAVEN_PKG", "updateRentalPackage SUCCESS: $packageId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("HAVEN_PKG", "updateRentalPackage FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to update rental package")
        }
    }

    suspend fun deleteRentalPackage(packageId: String): Resource<Unit> {
        return try {
            rentalPackagesCollection.document(packageId).delete().await()
            Log.d("HAVEN_PKG", "deleteRentalPackage SUCCESS: $packageId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("HAVEN_PKG", "deleteRentalPackage FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete rental package")
        }
    }

    suspend fun incrementPackageBookedSlots(packageId: String): Resource<Unit> {
        return try {
            val docRef = rentalPackagesCollection.document(packageId)
            val doc = docRef.get().await()
            val pkg = parseRentalPackage(doc) ?: return Resource.Error("Package not found")

            val newBookedSlots = pkg.bookedSlots + 1
            val newStatus = if (pkg.totalSlots != null && newBookedSlots >= pkg.totalSlots) {
                PackageStatus.SOLD_OUT.name
            } else {
                pkg.status.name
            }

            docRef.update(
                mapOf(
                    "bookedSlots" to newBookedSlots,
                    "status"      to newStatus,
                    "updatedAt"   to FieldValue.serverTimestamp()
                )
            ).await()

            Log.d("HAVEN_PKG", "incrementBookedSlots: $packageId bookedSlots=$newBookedSlots status=$newStatus")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("HAVEN_PKG", "incrementBookedSlots FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to update booked slots")
        }
    }
}

































