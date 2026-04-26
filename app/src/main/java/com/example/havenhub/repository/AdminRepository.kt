package com.example.havenhub.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Payment
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.data.UserPreferences
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersCollection      = firestore.collection("users")
    private val propertiesCollection = firestore.collection("properties")
    private val bookingsCollection   = firestore.collection("bookings")
    private val paymentsCollection   = firestore.collection("payments")

    // -------------------------------------------------------------------------
    // Helper: Convert Long / Date / Timestamp safely to Firestore Timestamp.
    // Firestore sometimes stores timestamps as Long (epoch ms) instead of
    // Timestamp objects, so we handle all three cases here.
    // -------------------------------------------------------------------------
    private fun Any?.toTimestampOrNull(): Timestamp? = when (this) {
        is Timestamp -> this
        is Long      -> Timestamp(Date(this))
        is Date      -> Timestamp(this)
        else         -> null
    }

    // -------------------------------------------------------------------------
    // Helper: Safely parse a Firestore document map into a User object.
    //
    // KEY FIX: Firestore stores the field as "banned" (not "isBanned").
    // We also read "active" instead of "isActive" to match Firestore schema.
    // Using manual parsing instead of toObject() avoids crashes when nested
    // fields like preferences.updatedAt are stored as Long instead of Timestamp.
    // -------------------------------------------------------------------------
    private fun parseUserSafely(
        data: Map<String, Any?>,
        fallbackUserId: String = ""
    ): User {

        // Parse nested preferences map safely
        @Suppress("UNCHECKED_CAST")
        val prefsMap = data["preferences"] as? Map<String, Any?> ?: emptyMap()

        val prefsUpdatedAt = prefsMap["updatedAt"].toTimestampOrNull()

        val preferences = UserPreferences(
            userId               = prefsMap["userId"] as? String ?: fallbackUserId,
            notifyBookingUpdates = prefsMap["notifyBookingUpdates"] as? Boolean ?: true,
            notifyMessages       = prefsMap["notifyMessages"] as? Boolean ?: true,
            notifyPayments       = prefsMap["notifyPayments"] as? Boolean ?: true,
            notifyPromotions     = prefsMap["notifyPromotions"] as? Boolean ?: false,
            notifyAdminAlerts    = prefsMap["notifyAdminAlerts"] as? Boolean ?: true,
            isProfilePublic      = prefsMap["isProfilePublic"] as? Boolean ?: true,
            showPhoneNumber      = prefsMap["showPhoneNumber"] as? Boolean ?: false,
            showEmail            = prefsMap["showEmail"] as? Boolean ?: false,
            preferredLanguage    = prefsMap["preferredLanguage"] as? String ?: "en",
            isDarkMode           = prefsMap["isDarkMode"] as? Boolean ?: false,
            updatedAt            = prefsUpdatedAt
        )

        return User(
            userId              = data["userId"] as? String ?: fallbackUserId,
            fullName            = data["fullName"] as? String ?: "",
            email               = data["email"] as? String ?: "",
            phoneNumber         = data["phoneNumber"] as? String ?: "",
            profileImageUrl     = data["profileImageUrl"] as? String ?: "",
            role                = data["role"] as? String ?: "TENANT",
            verificationStatus  = data["verificationStatus"] as? String ?: "PENDING",
            isVerified          = data["isVerified"] as? Boolean ?: false,

            // FIX: Firestore field is "active", not "isActive"
            isActive            = data["active"] as? Boolean ?: true,

            // FIX: Firestore field is "banned", not "isBanned"
            // Previously this was reading "isBanned" which never matched the
            // actual Firestore field, so isBanned was always false in the app.
            isBanned            = data["banned"] as? Boolean ?: false,

            nationalId          = data["nationalId"] as? String ?: "",
            idFrontUrl          = data["idFrontUrl"] as? String ?: "",
            idBackUrl           = data["idBackUrl"] as? String ?: "",
            fcmToken            = data["fcmToken"] as? String ?: "",
            landlordRating      = (data["landlordRating"] as? Number)?.toFloat() ?: 0f,
            landlordReviewCount = (data["landlordReviewCount"] as? Number)?.toInt() ?: 0,
            preferences         = preferences,
            createdAt           = data["createdAt"].toTimestampOrNull(),
            updatedAt           = data["updatedAt"].toTimestampOrNull()
        )
    }

    // -------------------------------------------------------------------------
    // Helper: Safely parse a Firestore document map into a Property object.
    // Uses manual parsing to fix updatedAt Long → Timestamp crash that occurs
    // when toObject() is used directly on documents with mixed timestamp types.
    // -------------------------------------------------------------------------
    private fun parsePropertySafely(
        data: Map<String, Any?>,
        fallbackPropertyId: String = ""
    ): Property {
        return Property(
            propertyId       = data["propertyId"] as? String ?: fallbackPropertyId,
            ownerId          = data["ownerId"] as? String ?: "",
            ownerName        = data["ownerName"] as? String ?: "",
            title            = data["title"] as? String ?: "",
            description      = data["description"] as? String ?: "",
            propertyType     = data["propertyType"] as? String ?: "APARTMENT",
            status           = data["status"] as? String ?: "PENDING",
            address          = data["address"] as? String ?: "",
            city             = data["city"] as? String ?: "",
            pricePerNight    = (data["pricePerNight"] as? Number)?.toDouble() ?: 0.0,
            pricePerWeek     = (data["pricePerWeek"] as? Number)?.toDouble(),
            pricePerMonth    = (data["pricePerMonth"] as? Number)?.toDouble(),
            securityDeposit  = (data["securityDeposit"] as? Number)?.toDouble() ?: 0.0,
            bedrooms         = (data["bedrooms"] as? Number)?.toInt() ?: 1,
            bathrooms        = (data["bathrooms"] as? Number)?.toInt() ?: 1,
            maxGuests        = (data["maxGuests"] as? Number)?.toInt() ?: 2,
            areaSqFt         = (data["areaSqFt"] as? Number)?.toDouble(),
            floor            = (data["floor"] as? Number)?.toInt(),
            imageUrls        = (data["imageUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            pt1DocumentUrl   = data["pt1DocumentUrl"] as? String ?: "",
            amenities        = (data["amenities"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            drawableImageName = data["drawableImageName"] as? String ?: "",
            petsAllowed      = data["petsAllowed"] as? Boolean ?: false,
            smokingAllowed   = data["smokingAllowed"] as? Boolean ?: false,
            partiesAllowed   = data["partiesAllowed"] as? Boolean ?: false,
            checkInTime      = data["checkInTime"] as? String ?: "14:00",
            checkOutTime     = data["checkOutTime"] as? String ?: "11:00",
            minNights        = (data["minNights"] as? Number)?.toInt() ?: 1,
            averageRating    = (data["averageRating"] as? Number)?.toFloat() ?: 0f,
            reviewCount      = (data["reviewCount"] as? Number)?.toInt() ?: 0,
            adminNote        = data["adminNote"] as? String ?: "",
            available        = data["isAvailable"] as? Boolean ?: true,
            featured         = data["isFeatured"] as? Boolean ?: false,
            createdAt        = data["createdAt"].toTimestampOrNull(),
            updatedAt        = data["updatedAt"].toTimestampOrNull()
        )
    }

    // =========================================================================
    // USERS
    // =========================================================================

    // Fetch all users from Firestore.
    // Uses manual parseUserSafely() instead of toObjects() to avoid crashes
    // caused by nested timestamp fields stored as Long.
    suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()

            val users = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    parseUserSafely(data, fallbackUserId = doc.id)
                } catch (e: Exception) {
                    // Skip corrupt documents instead of crashing the whole list
                    null
                }
            }

            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch users")
        }
    }

    // Ban a user by setting the "banned" field to true in Firestore.
    // FIX: Field name corrected from "isBanned" → "banned" to match
    // the actual Firestore schema visible in Firebase Console.
    suspend fun banUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("banned", true)   // FIX: was "isBanned", Firestore field is "banned"
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to ban user")
        }
    }

    // Unban a user by setting the "banned" field back to false in Firestore.
    // FIX: Field name corrected from "isBanned" → "banned" to match
    // the actual Firestore schema visible in Firebase Console.
    suspend fun unbanUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("banned", false)  // FIX: was "isBanned", Firestore field is "banned"
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unban user")
        }
    }

    // =========================================================================
    // PROPERTIES
    // =========================================================================

    // Fetch all properties from Firestore.
    // Uses manual parsePropertySafely() to handle Long timestamps safely.
    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection.get().await()

            val properties = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    parsePropertySafely(data, fallbackPropertyId = doc.id)
                } catch (e: Exception) {
                    // Skip corrupt documents instead of crashing the whole list
                    null
                }
            }

            Resource.Success(properties)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch properties")
        }
    }

    // Approve a property listing — sets status to "APPROVED" with optional admin note.
    suspend fun approveProperty(propertyId: String, adminNote: String = ""): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId)
                .update(
                    mapOf(
                        "status"    to "APPROVED",
                        "adminNote" to adminNote,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to approve property")
        }
    }

    // Reject a property listing — sets status to "REJECTED" with a rejection reason.
    suspend fun rejectProperty(propertyId: String, reason: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId)
                .update(
                    mapOf(
                        "status"    to "REJECTED",
                        "adminNote" to reason,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to reject property")
        }
    }

    // Permanently delete a property document from Firestore.
    suspend fun deleteProperty(propertyId: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete property")
        }
    }

    // =========================================================================
    // BOOKINGS
    // =========================================================================

    // Fetch all bookings from Firestore.
    suspend fun getAllBookings(): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.get().await()
            Resource.Success(snapshot.toObjects(Booking::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch bookings")
        }
    }

    // Confirm a pending booking — sets status to "CONFIRMED".
    suspend fun confirmBooking(bookingId: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId)
                .update(mapOf("status" to "CONFIRMED"))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to confirm booking")
        }
    }

    // Cancel a booking — sets status to "CANCELLED" and records cancellation metadata.
    suspend fun cancelBooking(bookingId: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId)
                .update(
                    mapOf(
                        "status"      to "CANCELLED",
                        "cancelledAt" to Timestamp.now(),
                        "cancelledBy" to "ADMIN"
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to cancel booking")
        }
    }

    // =========================================================================
    // PAYMENTS
    // =========================================================================

    // Fetch all payment records from Firestore.
    suspend fun getAllPayments(): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection.get().await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch payments")
        }
    }
}