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

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: Long/Date/Timestamp — teeno ko safely Timestamp mein convert karo
    // ─────────────────────────────────────────────────────────────────────────

    private fun Any?.toTimestampOrNull(): Timestamp? = when (this) {
        is Timestamp -> this
        is Long      -> Timestamp(Date(this))
        is Date      -> Timestamp(this)
        else         -> null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: Firestore document se User safely parse karo
    // nested preferences.updatedAt ka Long → Timestamp crash yahan fix hota hai
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseUserSafely(
        data: Map<String, Any?>,
        fallbackUserId: String = ""
    ): User {
        // preferences map nikaalo
        @Suppress("UNCHECKED_CAST")
        val prefsMap = data["preferences"] as? Map<String, Any?> ?: emptyMap()

        // ✅ updatedAt manually convert karo — yahi crash ka asli point hai
        val prefsUpdatedAt = prefsMap["updatedAt"].toTimestampOrNull()

        val preferences = UserPreferences(
            userId                = prefsMap["userId"] as? String ?: fallbackUserId,
            notifyBookingUpdates  = prefsMap["notifyBookingUpdates"] as? Boolean ?: true,
            notifyMessages        = prefsMap["notifyMessages"] as? Boolean ?: true,
            notifyPayments        = prefsMap["notifyPayments"] as? Boolean ?: true,
            notifyPromotions      = prefsMap["notifyPromotions"] as? Boolean ?: false,
            notifyAdminAlerts     = prefsMap["notifyAdminAlerts"] as? Boolean ?: true,
            isProfilePublic       = prefsMap["isProfilePublic"] as? Boolean ?: true,
            showPhoneNumber       = prefsMap["showPhoneNumber"] as? Boolean ?: false,
            showEmail             = prefsMap["showEmail"] as? Boolean ?: false,
            preferredLanguage     = prefsMap["preferredLanguage"] as? String ?: "en",
            isDarkMode            = prefsMap["isDarkMode"] as? Boolean ?: false,
            updatedAt             = prefsUpdatedAt   // ✅ safely set
        )

        return User(
            userId             = data["userId"] as? String ?: fallbackUserId,
            fullName           = data["fullName"] as? String ?: "",
            email              = data["email"] as? String ?: "",
            phoneNumber        = data["phoneNumber"] as? String ?: "",
            profileImageUrl    = data["profileImageUrl"] as? String ?: "",
            role               = data["role"] as? String ?: "TENANT",
            verificationStatus = data["verificationStatus"] as? String ?: "PENDING",
            isVerified         = data["isVerified"] as? Boolean ?: false,
            isActive           = data["isActive"] as? Boolean ?: true,
            isBanned           = data["isBanned"] as? Boolean ?: false,
            nationalId         = data["nationalId"] as? String ?: "",
            idFrontUrl         = data["idFrontUrl"] as? String ?: "",
            idBackUrl          = data["idBackUrl"] as? String ?: "",
            fcmToken           = data["fcmToken"] as? String ?: "",
            landlordRating     = (data["landlordRating"] as? Number)?.toFloat() ?: 0f,
            landlordReviewCount = (data["landlordReviewCount"] as? Number)?.toInt() ?: 0,
            preferences        = preferences,
            createdAt          = data["createdAt"].toTimestampOrNull(),
            updatedAt          = data["updatedAt"].toTimestampOrNull()
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Users
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()

            // ✅ toObjects() ki jagah manual parse — nested updatedAt crash fix
            val users = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    parseUserSafely(data, fallbackUserId = doc.id)
                } catch (e: Exception) {
                    null // ek corrupt document poori list crash na kare
                }
            }

            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch users")
        }
    }

    suspend fun banUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("isBanned", true)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to ban user")
        }
    }

    suspend fun unbanUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("isBanned", false)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unban user")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Properties
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection.get().await()
            Resource.Success(snapshot.toObjects(Property::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch properties")
        }
    }

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

    // ─────────────────────────────────────────────────────────────────────────
    // Bookings
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAllBookings(): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.get().await()
            Resource.Success(snapshot.toObjects(Booking::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch bookings")
        }
    }

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

    // ─────────────────────────────────────────────────────────────────────────
    // Payments
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAllPayments(): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection.get().await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch payments")
        }
    }
}