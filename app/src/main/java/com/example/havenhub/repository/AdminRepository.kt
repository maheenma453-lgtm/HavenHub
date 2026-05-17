package com.example.havenhub.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.havenhub.data.Booking
import com.example.havenhub.data.NotificationType
import com.example.havenhub.data.Payment
import com.example.havenhub.data.Property
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.data.User
import com.example.havenhub.data.UserPreferences
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val firestore            : FirebaseFirestore,
    private val notificationRepository: NotificationRepository   // ✅ ADDED
) {

    private val usersCollection      = firestore.collection("users")
    private val propertiesCollection = firestore.collection("properties")
    private val bookingsCollection   = firestore.collection("bookings")
    private val paymentsCollection   = firestore.collection("payments")
    private val packagesCollection   = firestore.collection("rentalPackages")  // ✅ ADDED

    // ══════════════════════════════════════════════════════════════════════════
    // PARSE HELPERS (unchanged)
    // ══════════════════════════════════════════════════════════════════════════

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
        @Suppress("UNCHECKED_CAST")
        val prefsMap = data["preferences"] as? Map<String, Any?> ?: emptyMap()

        val preferences = UserPreferences(
            userId = prefsMap["userId"] as? String
                ?: data["userId"] as? String
                ?: fallbackUserId,
            notifyBookingUpdates = prefsMap["notificationsEnabled"] as? Boolean
                ?: prefsMap["notifyBookingUpdates"] as? Boolean
                ?: true,
            notifyMessages       = prefsMap["smsNotifications"] as? Boolean
                ?: prefsMap["notifyMessages"] as? Boolean
                ?: true,
            notifyPayments       = prefsMap["notifyPayments"] as? Boolean ?: true,
            notifyPromotions     = prefsMap["notifyPromotions"] as? Boolean ?: false,
            notifyAdminAlerts    = prefsMap["notifyAdminAlerts"] as? Boolean ?: true,
            isProfilePublic      = prefsMap["isProfilePublic"] as? Boolean ?: true,
            showPhoneNumber      = prefsMap["showPhoneNumber"] as? Boolean ?: false,
            showEmail            = prefsMap["showEmail"] as? Boolean ?: false,
            preferredLanguage    = prefsMap["language"] as? String
                ?: prefsMap["preferredLanguage"] as? String
                ?: "en",
            isDarkMode           = prefsMap["isDarkMode"] as? Boolean ?: false,
            updatedAt            = prefsMap["updatedAt"].toTimestampOrNull()
        )

        return User(
            userId             = data["userId"] as? String ?: fallbackUserId,
            fullName           = data["fullName"] as? String ?: "",
            email              = data["email"] as? String ?: "",
            phoneNumber        = (data["phoneNumber"] as? String)
                ?: (data["phone"] as? String)
                ?: "",
            profileImageUrl    = data["profileImageUrl"] as? String ?: "",
            role               = data["role"] as? String ?: "tenant",
            verificationStatus = data["verificationStatus"] as? String ?: "PENDING",
            isVerified         = (data["isVerified"] as? Boolean)
                ?: (data["verificationStatus"] as? String)
                    ?.uppercase()?.let { it == "VERIFIED" || it == "APPROVED" }
                ?: false,
            isActive           = (data["isActive"] as? Boolean)
                ?: (data["active"] as? Boolean) ?: true,
            isBanned           = (data["isBanned"] as? Boolean)
                ?: (data["banned"] as? Boolean) ?: false,
            cnicNumber         = data["cnicNumber"] as? String ?: "",
            cnicImageUrl       = data["cnicImageUrl"] as? String ?: "",
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

    // ══════════════════════════════════════════════════════════════════════════
    // USERS
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try { parseUserSafely(data, fallbackUserId = doc.id) }
                catch (e: Exception) { null }
            }
            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch users")
        }
    }

    /**
     * ✅ FIXED: User approve hone ke baad USER_VERIFIED notification bhejo.
     * Pehle sirf Firestore update hota tha.
     */
    suspend fun approveUser(userId: String): Resource<Unit> {
        return try {
            // Pehle user ka naam fetch karo
            val userDoc  = usersCollection.document(userId).get().await()
            val userName = userDoc.getString("fullName") ?: "User"

            val fields = mapOf(
                "verificationStatus" to "VERIFIED",
                "isVerified"         to true,
                "updatedAt"          to FieldValue.serverTimestamp()
            )
            usersCollection.document(userId).update(fields).await()

            // ✅ Notification bhejo
            notificationRepository.sendUserVerifiedNotification(
                userId   = userId,
                userName = userName
            )
            Log.d("ADMIN_REPO", "✅ User verified notification sent to $userId")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to approve user")
        }
    }

    /**
     * ✅ FIXED: User unban ke baad USER_VERIFIED notification bhejo.
     */
    suspend fun unbanUser(userId: String): Resource<Unit> {
        return try {
            val userDoc  = usersCollection.document(userId).get().await()
            val userName = userDoc.getString("fullName") ?: "User"

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
                ).await()

            notificationRepository.sendNotification(
                recipientId = userId,
                type        = NotificationType.ACCOUNT_VERIFIED,
                title       = "Account Restored ✅",
                body        = "Mubarak $userName! Aapka account restore ho gaya hai. Ab aap sab features use kar sakte hain.",
                targetRole  = "all"
            )
            Log.d("ADMIN_REPO", "✅ Account restored notification sent to $userId")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unban user")
        }
    }

    /**
     * ✅ FIXED: User ban ke baad ACCOUNT_SUSPENDED notification bhejo.
     */
    suspend fun banUser(userId: String): Resource<Unit> {
        return try {
            val userDoc  = usersCollection.document(userId).get().await()
            val userName = userDoc.getString("fullName") ?: "User"

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
                ).await()

            notificationRepository.sendNotification(
                recipientId = userId,
                type        = NotificationType.ACCOUNT_SUSPENDED,
                title       = "Account Suspended ⚠️",
                body        = "Aapka account suspend kar diya gaya hai. Details ke liye support se contact karen.",
                targetRole  = "all"
            )
            Log.d("ADMIN_REPO", "✅ Account suspended notification sent to $userId")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to ban user")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROPERTIES
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection.get().await()
            val properties = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try { parsePropertySafely(data, fallbackPropertyId = doc.id) }
                catch (e: Exception) { null }
            }
            Resource.Success(properties)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch properties")
        }
    }

    /**
     * ✅ FIXED: approve ke baad landlord ko notification bhejo.
     * AdminRepository.approveProperty() → PropertyRepository se alag hai —
     * yahan bhi notification chahiye (admin panel se direct approve).
     */
    suspend fun approveProperty(propertyId: String, adminNote: String = ""): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId)
                .update(
                    mapOf(
                        "status"    to "APPROVED",
                        "adminNote" to adminNote,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()

            // Landlord ko notify karo
            val propDoc       = propertiesCollection.document(propertyId).get().await()
            val ownerId       = propDoc.getString("ownerId") ?: ""
            val propertyTitle = propDoc.getString("title")   ?: "Property"

            if (ownerId.isNotBlank()) {
                notificationRepository.sendPropertyApprovedNotification(
                    ownerId       = ownerId,
                    propertyId    = propertyId,
                    propertyTitle = propertyTitle,
                    adminNote     = adminNote
                )
                Log.d("ADMIN_REPO", "✅ Property approved notification sent to $ownerId")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to approve property")
        }
    }

    /**
     * ✅ FIXED: reject ke baad landlord ko reason ke saath notification bhejo.
     */
    suspend fun rejectProperty(propertyId: String, reason: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId)
                .update(
                    mapOf(
                        "status"    to "REJECTED",
                        "adminNote" to reason,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()

            val propDoc       = propertiesCollection.document(propertyId).get().await()
            val ownerId       = propDoc.getString("ownerId") ?: ""
            val propertyTitle = propDoc.getString("title")   ?: "Property"

            if (ownerId.isNotBlank()) {
                notificationRepository.sendPropertyRejectedNotification(
                    ownerId       = ownerId,
                    propertyId    = propertyId,
                    propertyTitle = propertyTitle,
                    adminNote     = reason
                )
                Log.d("ADMIN_REPO", "✅ Property rejected notification sent to $ownerId")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to reject property")
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

    // ══════════════════════════════════════════════════════════════════════════
    // BOOKINGS
    // ══════════════════════════════════════════════════════════════════════════

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
                ).await()
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
                ).await()
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
                ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to cancel booking")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PAYMENTS
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun getAllPayments(): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection.get().await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch payments")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ✅ NEW: SEASONAL AVAILABILITY ALERTS
    // RentalPackage mein availableFrom/availableTo fields hain.
    // Admin/system yeh call kare jab package activate karna ho ya
    // background job se check karo — tenants ko alert bhejo.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ek specific package activate karo aur interested tenants ko alert bhejo.
     * Landlord ya admin ke admin panel se call karo jab package ACTIVE karo.
     */
    suspend fun activatePackageAndNotifyTenants(packageId: String): Resource<Unit> {
        return try {
            // Package ACTIVE mark karo
            packagesCollection.document(packageId)
                .update(
                    mapOf(
                        "status"    to "ACTIVE",
                        "updatedAt" to Timestamp.now()
                    )
                ).await()

            // Package details fetch karo
            val packageDoc    = packagesCollection.document(packageId).get().await()
            val propertyId    = packageDoc.getString("propertyId")    ?: ""
            val propertyTitle = packageDoc.getString("propertyTitle") ?: "Property"
            val packageName   = packageDoc.getString("packageName")   ?: "Special Deal"
            val discounted    = (packageDoc.getDouble("discountedPricePerNight") ?: 0.0).toInt()
            val availableFrom = packageDoc.getTimestamp("availableFrom")
            val availableTo   = packageDoc.getTimestamp("availableTo")

            // Date range text
            val dateText = buildDateRangeText(availableFrom, availableTo)

            // Saare TENANT users ko notify karo
            sendSeasonalAlertToAllTenants(
                packageId     = packageId,
                propertyId    = propertyId,
                propertyTitle = propertyTitle,
                packageName   = packageName,
                discounted    = discounted,
                dateText      = dateText
            )

            Log.d("ADMIN_REPO", "✅ Package $packageId activated and tenants notified")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ADMIN_REPO", "activatePackageAndNotifyTenants error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to activate package")
        }
    }

    /**
     * Saare active packages check karo — jo expire ho gaye unhe EXPIRED mark karo.
     * Aur jo abhi shuru hone wale hain (agle 3 din mein) unke liye tenants ko alert bhejo.
     * Yeh background job ki tarah kisi CoroutineWorker/periodic call se invoke karo.
     */
    suspend fun checkAndSendSeasonalAvailabilityAlerts(): Resource<Unit> {
        return try {
            val now    = Timestamp.now()
            val snap   = packagesCollection.whereEqualTo("status", "ACTIVE").get().await()
            val packages = snap.toObjects(RentalPackage::class.java)

            packages.forEach { pkg ->
                val availFrom = pkg.availableFrom
                val availTo   = pkg.availableTo

                // 1. Expired packages EXPIRED mark karo
                if (availTo != null && availTo.toDate().before(now.toDate())) {
                    packagesCollection.document(pkg.packageId)
                        .update("status", "EXPIRED").await()
                    Log.d("ADMIN_REPO", "Package ${pkg.packageId} marked EXPIRED")
                    return@forEach
                }

                // 2. Agle 3 din mein shuru hone wale packages ke liye tenants ko alert karo
                if (availFrom != null) {
                    val diffMs    = availFrom.toDate().time - now.toDate().time
                    val diffDays  = diffMs / (1000 * 60 * 60 * 24)
                    if (diffDays in 0..3) {
                        val dateText = buildDateRangeText(availFrom, availTo)
                        sendSeasonalAlertToAllTenants(
                            packageId     = pkg.packageId,
                            propertyId    = pkg.propertyId,
                            propertyTitle = pkg.propertyTitle.ifBlank { "Property" },
                            packageName   = pkg.packageName.ifBlank { "Special Deal" },
                            discounted    = pkg.discountedPricePerNight.toInt(),
                            dateText      = dateText
                        )
                    }
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ADMIN_REPO", "checkAndSendSeasonalAvailabilityAlerts error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to send seasonal alerts")
        }
    }

    // ── Private seasonal helpers ──────────────────────────────────────────────

    private suspend fun sendSeasonalAlertToAllTenants(
        packageId    : String,
        propertyId   : String,
        propertyTitle: String,
        packageName  : String,
        discounted   : Int,
        dateText     : String
    ) {
        try {
            val tenantQuery = usersCollection.whereEqualTo("role", "TENANT").get().await()
            if (tenantQuery.isEmpty) {
                // lowercase fallback
                val fallback = usersCollection.whereEqualTo("role", "tenant").get().await()
                fallback.documents.forEach { doc ->
                    sendOneSeasonalAlert(doc.id, packageId, propertyId, propertyTitle, packageName, discounted, dateText)
                }
                return
            }
            tenantQuery.documents.forEach { doc ->
                sendOneSeasonalAlert(doc.id, packageId, propertyId, propertyTitle, packageName, discounted, dateText)
            }
        } catch (e: Exception) {
            Log.e("ADMIN_REPO", "sendSeasonalAlertToAllTenants error: ${e.localizedMessage}")
        }
    }

    private suspend fun sendOneSeasonalAlert(
        tenantId     : String,
        packageId    : String,
        propertyId   : String,
        propertyTitle: String,
        packageName  : String,
        discounted   : Int,
        dateText     : String
    ) {
        notificationRepository.sendNotification(
            recipientId = tenantId,
            type        = NotificationType.BOOKING_REMINDER,
            title       = "🌟 $packageName Available!",
            body        = "\"$propertyTitle\" pe limited deal: Rs. $discounted/night$dateText. Jaldi book karo!",
            referenceId = propertyId,
            targetRole  = "tenant"
        )
    }

    private fun buildDateRangeText(from: Timestamp?, to: Timestamp?): String {
        if (from == null && to == null) return ""
        val fmt = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        return when {
            from != null && to != null -> " (${fmt.format(from.toDate())} - ${fmt.format(to.toDate())})"
            from != null               -> " (from ${fmt.format(from.toDate())})"
            to   != null               -> " (until ${fmt.format(to.toDate())})"
            else                       -> ""
        }
    }
}