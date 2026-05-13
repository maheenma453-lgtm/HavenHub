package com.example.havenhub.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
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

    private fun Any?.toTimestampOrNull(): Timestamp? = when (this) {
        is Timestamp -> this
        is Long      -> Timestamp(Date(this))
        is Date      -> Timestamp(this)
        else         -> null
    }

    private fun parseUserSafely(
        data           : Map<String, Any?>,
        fallbackUserId : String = ""
    ): User {

        // ✅ FIX: Firebase mein preferences map ke actual field names use karo
        // (emailNotifications, language, notificationsEnabled, smsNotifications)
        @Suppress("UNCHECKED_CAST")
        val prefsMap = data["preferences"] as? Map<String, Any?> ?: emptyMap()

        val preferences = UserPreferences(
            userId = prefsMap["userId"] as? String
                ?: data["userId"] as? String
                ?: fallbackUserId,

            // ✅ Firebase field: "notificationsEnabled" — booking updates ke liye use karo
            notifyBookingUpdates = prefsMap["notificationsEnabled"] as? Boolean
                ?: prefsMap["notifyBookingUpdates"] as? Boolean
                ?: true,

            // ✅ Firebase field: "smsNotifications" ya fallback
            notifyMessages = prefsMap["smsNotifications"] as? Boolean
                ?: prefsMap["notifyMessages"] as? Boolean
                ?: true,

            // ✅ Firebase mein payment notification ka specific field nahi — default true
            notifyPayments = prefsMap["notifyPayments"] as? Boolean ?: true,

            notifyPromotions  = prefsMap["notifyPromotions"] as? Boolean ?: false,
            notifyAdminAlerts = prefsMap["notifyAdminAlerts"] as? Boolean ?: true,
            isProfilePublic   = prefsMap["isProfilePublic"] as? Boolean ?: true,
            showPhoneNumber   = prefsMap["showPhoneNumber"] as? Boolean ?: false,
            showEmail         = prefsMap["showEmail"] as? Boolean ?: false,

            // ✅ Firebase field: "language"
            preferredLanguage = prefsMap["language"] as? String
                ?: prefsMap["preferredLanguage"] as? String
                ?: "en",

            isDarkMode = prefsMap["isDarkMode"] as? Boolean ?: false,
            updatedAt  = prefsMap["updatedAt"].toTimestampOrNull()
        )

        return User(
            userId          = data["userId"] as? String ?: fallbackUserId,
            fullName        = data["fullName"] as? String ?: "",
            email           = data["email"] as? String ?: "",
            phoneNumber     = (data["phoneNumber"] as? String)
                ?: (data["phone"] as? String)       // ✅ Firebase mein "phone" field bhi ho sakta hai
                ?: "",
            profileImageUrl = data["profileImageUrl"] as? String ?: "",

            // ✅ Firebase mein role "TENANT"/"LANDLORD" all-caps — as-is store karo
            role = data["role"] as? String ?: "tenant",

            verificationStatus = data["verificationStatus"] as? String ?: "PENDING",

            // ✅ isVerified — verificationStatus se bhi derive karo
            isVerified = (data["isVerified"] as? Boolean)
                ?: (data["verificationStatus"] as? String)
                    ?.uppercase()
                    ?.let { it == "VERIFIED" || it == "APPROVED" }
                ?: false,

            isActive = (data["isActive"] as? Boolean)
                ?: (data["active"] as? Boolean)
                ?: true,

            isBanned = (data["isBanned"] as? Boolean)
                ?: (data["banned"] as? Boolean)
                ?: false,

            cnicNumber   = data["cnicNumber"] as? String ?: "",
            cnicImageUrl = data["cnicImageUrl"] as? String ?: "",
            nationalId   = data["nationalId"] as? String ?: "",
            idFrontUrl   = data["idFrontUrl"] as? String ?: "",
            idBackUrl    = data["idBackUrl"] as? String ?: "",
            fcmToken     = data["fcmToken"] as? String ?: "",

            landlordRating      = (data["landlordRating"] as? Number)?.toFloat() ?: 0f,
            landlordReviewCount = (data["landlordReviewCount"] as? Number)?.toInt() ?: 0,

            preferences = preferences,
            createdAt   = data["createdAt"].toTimestampOrNull(),
            updatedAt   = data["updatedAt"].toTimestampOrNull()
        )
    }

    private fun parsePropertySafely(
        data               : Map<String, Any?>,
        fallbackPropertyId : String = ""
    ): Property {
        return Property(
            propertyId        = data["propertyId"] as? String ?: fallbackPropertyId,
            ownerId           = data["ownerId"] as? String ?: "",
            ownerName         = data["ownerName"] as? String ?: "",
            title             = data["title"] as? String ?: "",
            description       = data["description"] as? String ?: "",
            propertyType      = data["propertyType"] as? String ?: "APARTMENT",
            status            = data["status"] as? String ?: "PENDING",
            address           = data["address"] as? String ?: "",
            city              = data["city"] as? String ?: "",
            pricePerNight     = (data["pricePerNight"] as? Number)?.toDouble() ?: 0.0,
            pricePerWeek      = (data["pricePerWeek"] as? Number)?.toDouble(),
            pricePerMonth     = (data["pricePerMonth"] as? Number)?.toDouble(),
            securityDeposit   = (data["securityDeposit"] as? Number)?.toDouble() ?: 0.0,
            bedrooms          = (data["bedrooms"] as? Number)?.toInt() ?: 1,
            bathrooms         = (data["bathrooms"] as? Number)?.toInt() ?: 1,
            maxGuests         = (data["maxGuests"] as? Number)?.toInt() ?: 2,
            areaSqFt          = (data["areaSqFt"] as? Number)?.toDouble(),
            floor             = (data["floor"] as? Number)?.toInt(),
            imageUrls         = (data["imageUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            pt1DocumentUrl    = data["pt1DocumentUrl"] as? String ?: "",
            amenities         = (data["amenities"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            drawableImageName = data["drawableImageName"] as? String ?: "",
            petsAllowed       = data["petsAllowed"] as? Boolean ?: false,
            smokingAllowed    = data["smokingAllowed"] as? Boolean ?: false,
            partiesAllowed    = data["partiesAllowed"] as? Boolean ?: false,
            checkInTime       = data["checkInTime"] as? String ?: "14:00",
            checkOutTime      = data["checkOutTime"] as? String ?: "11:00",
            minNights         = (data["minNights"] as? Number)?.toInt() ?: 1,
            averageRating     = (data["averageRating"] as? Number)?.toFloat() ?: 0f,
            reviewCount       = (data["reviewCount"] as? Number)?.toInt() ?: 0,
            adminNote         = data["adminNote"] as? String ?: "",
            available         = data["isAvailable"] as? Boolean ?: true,
            featured          = data["isFeatured"] as? Boolean ?: false,
            createdAt         = data["createdAt"].toTimestampOrNull(),
            updatedAt         = data["updatedAt"].toTimestampOrNull()
        )
    }

    // =========================================================================
    // USERS
    // =========================================================================

    suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    parseUserSafely(data, fallbackUserId = doc.id)
                } catch (e: Exception) {
                    // ✅ Ek user fail ho toh poori list crash nahi hogi
                    null
                }
            }
            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch users")
        }
    }

    suspend fun approveUser(userId: String): Resource<Unit> {
        return try {
            val fields = mapOf(
                "verificationStatus" to "VERIFIED",
                "isVerified"         to true,
                "updatedAt"          to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(userId).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to approve user")
        }
    }

    suspend fun unbanUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update(
                    mapOf(
                        "banned"             to false,
                        "isBanned"           to false,
                        "isVerified"         to true,
                        "verificationStatus" to "APPROVED",
                        "active"             to true,
                        "isActive"           to true,
                        "updatedAt"          to Timestamp.now()
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unban user")
        }
    }

    suspend fun banUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update(
                    mapOf(
                        "banned"             to true,
                        "isBanned"           to true,
                        "isVerified"         to false,
                        "verificationStatus" to "REJECTED",
                        "active"             to false,
                        "isActive"           to false,
                        "updatedAt"          to Timestamp.now()
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to ban user")
        }
    }

    // =========================================================================
    // PROPERTIES
    // =========================================================================

    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection.get().await()
            val properties = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    parsePropertySafely(data, fallbackPropertyId = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            Resource.Success(properties)
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

    // =========================================================================
    // BOOKINGS
    // =========================================================================

    suspend fun getAllBookings(): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.get().await()
            Resource.Success(snapshot.toObjects(Booking::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch bookings")
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId)
                .update(
                    mapOf(
                        "status"    to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update booking status")
        }
    }

    suspend fun confirmBooking(bookingId: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId)
                .update(
                    mapOf(
                        "status"    to "CONFIRMED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
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
                        "cancelledBy" to "ADMIN",
                        "updatedAt"   to FieldValue.serverTimestamp()
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

    suspend fun getAllPayments(): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection.get().await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch payments")
        }
    }
}
