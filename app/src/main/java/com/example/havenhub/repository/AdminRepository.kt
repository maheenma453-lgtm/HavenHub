package com.example.havenhub.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.havenhub.data.AdminPermissions
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
    private val firestore             : FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) {

    private val usersCollection = firestore.collection("users")
    private val propertiesCollection = firestore.collection("properties")
    private val bookingsCollection = firestore.collection("bookings")
    private val paymentsCollection = firestore.collection("payments")
    private val packagesCollection = firestore.collection("rentalPackages")

    // ══════════════════════════════════════════════════════════════════════════
    // PARSE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private fun Any?.toTimestampOrNull(): Timestamp? = when (this) {
        is Timestamp -> this
        is Long -> Timestamp(Date(this))
        is Date -> Timestamp(this)
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAdminPermissions(data: Map<String, Any?>): AdminPermissions? {
        val permsMap = data["adminPermissions"] as? Map<String, Any?> ?: return null
        return AdminPermissions(
            canManageUsers = permsMap["canManageUsers"] as? Boolean ?: false,
            canVerifyUsers = permsMap["canVerifyUsers"] as? Boolean ?: false,
            canVerifyProperties = permsMap["canVerifyProperties"] as? Boolean ?: false,
            canManageProperties = permsMap["canManageProperties"] as? Boolean ?: false,
            canManageBookings = permsMap["canManageBookings"] as? Boolean ?: false,
            canViewReports = permsMap["canViewReports"] as? Boolean ?: false
        )
    }

    private fun parseUserSafely(
        data: Map<String, Any?>,
        fallbackUserId: String = ""
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
            notifyMessages = prefsMap["smsNotifications"] as? Boolean
                ?: prefsMap["notifyMessages"] as? Boolean
                ?: true,
            notifyPayments = prefsMap["notifyPayments"] as? Boolean ?: true,
            notifyPromotions = prefsMap["notifyPromotions"] as? Boolean ?: false,
            notifyAdminAlerts = prefsMap["notifyAdminAlerts"] as? Boolean ?: true,
            isProfilePublic = prefsMap["isProfilePublic"] as? Boolean ?: true,
            showPhoneNumber = prefsMap["showPhoneNumber"] as? Boolean ?: false,
            showEmail = prefsMap["showEmail"] as? Boolean ?: false,
            preferredLanguage = prefsMap["language"] as? String
                ?: prefsMap["preferredLanguage"] as? String
                ?: "en",
            isDarkMode = prefsMap["isDarkMode"] as? Boolean ?: false,
            updatedAt = prefsMap["updatedAt"].toTimestampOrNull()
        )

        return User(
            userId = data["userId"] as? String ?: fallbackUserId,
            fullName = data["fullName"] as? String ?: "",
            email = data["email"] as? String ?: "",
            phoneNumber = (data["phoneNumber"] as? String)
                ?: (data["phone"] as? String)
                ?: "",
            profileImageUrl = data["profileImageUrl"] as? String ?: "",
            // ══════════════════════════════════════════════════════════════
            // ROLE FIX: role field as-is store karo, lowercase mat karo
            // DashboardViewModel mein lowercase() call hota hai
            // Firestore mein jo value hai wahi aayegi: "admin", "sub_admin",
            // "ADMIN", "SUB_ADMIN", "tenant", "landlord" etc.
            // ══════════════════════════════════════════════════════════════
            role = data["role"] as? String ?: "tenant",
            verificationStatus = data["verificationStatus"] as? String ?: "PENDING",
            isVerified = (data["isVerified"] as? Boolean)
                ?: (data["verificationStatus"] as? String)
                    ?.uppercase()?.let { it == "VERIFIED" || it == "APPROVED" }
                ?: false,
            isActive = (data["isActive"] as? Boolean)
                ?: (data["active"] as? Boolean) ?: true,
            isBanned = (data["isBanned"] as? Boolean)
                ?: (data["banned"] as? Boolean) ?: false,
            cnicNumber = data["cnicNumber"] as? String ?: "",
            cnicImageUrl = data["cnicImageUrl"] as? String ?: "",
            nationalId = data["nationalId"] as? String ?: "",
            idFrontUrl = data["idFrontUrl"] as? String ?: "",
            idBackUrl = data["idBackUrl"] as? String ?: "",
            fcmToken = data["fcmToken"] as? String ?: "",
            landlordRating = (data["landlordRating"] as? Number)?.toFloat() ?: 0f,
            landlordReviewCount = (data["landlordReviewCount"] as? Number)?.toInt() ?: 0,
            // ══════════════════════════════════════════════════════════════
            // CRITICAL: adminPermissions parse karo — yahi decide karta hai
            // ke user sub_admin hai ya super_admin
            // Super Admin ke doc mein yeh field exist nahi karti → null
            // Sub Admin ke doc mein yeh field ZAROOR hoti hai → non-null
            // ══════════════════════════════════════════════════════════════
            adminPermissions = parseAdminPermissions(data),
            preferences = preferences,
            createdAt = data["createdAt"].toTimestampOrNull(),
            updatedAt = data["updatedAt"].toTimestampOrNull()
        )
    }

    private fun parsePropertySafely(
        data: Map<String, Any?>,
        fallbackPropertyId: String = ""
    ): Property {
        return Property(
            propertyId = data["propertyId"] as? String ?: fallbackPropertyId,
            ownerId = data["ownerId"] as? String ?: "",
            ownerName = data["ownerName"] as? String ?: "",
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            propertyType = data["propertyType"] as? String ?: "APARTMENT",
            status = data["status"] as? String ?: "PENDING",
            address = data["address"] as? String ?: "",
            city = data["city"] as? String ?: "",
            pricePerNight = (data["pricePerNight"] as? Number)?.toDouble() ?: 0.0,
            pricePerWeek = (data["pricePerWeek"] as? Number)?.toDouble(),
            pricePerMonth = (data["pricePerMonth"] as? Number)?.toDouble(),
            securityDeposit = (data["securityDeposit"] as? Number)?.toDouble() ?: 0.0,
            bedrooms = (data["bedrooms"] as? Number)?.toInt() ?: 1,
            bathrooms = (data["bathrooms"] as? Number)?.toInt() ?: 1,
            maxGuests = (data["maxGuests"] as? Number)?.toInt() ?: 2,
            areaSqFt = (data["areaSqFt"] as? Number)?.toDouble(),
            floor = (data["floor"] as? Number)?.toInt(),
            imageUrls = (data["imageUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            pt1DocumentUrl = data["pt1DocumentUrl"] as? String ?: "",
            amenities = (data["amenities"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            drawableImageName = data["drawableImageName"] as? String ?: "",
            petsAllowed = data["petsAllowed"] as? Boolean ?: false,
            smokingAllowed = data["smokingAllowed"] as? Boolean ?: false,
            partiesAllowed = data["partiesAllowed"] as? Boolean ?: false,
            checkInTime = data["checkInTime"] as? String ?: "14:00",
            checkOutTime = data["checkOutTime"] as? String ?: "11:00",
            minNights = (data["minNights"] as? Number)?.toInt() ?: 1,
            averageRating = (data["averageRating"] as? Number)?.toFloat() ?: 0f,
            reviewCount = (data["reviewCount"] as? Number)?.toInt() ?: 0,
            adminNote = data["adminNote"] as? String ?: "",
            available = data["isAvailable"] as? Boolean ?: true,
            featured = data["isFeatured"] as? Boolean ?: false,
            createdAt = data["createdAt"].toTimestampOrNull(),
            updatedAt = data["updatedAt"].toTimestampOrNull()
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
                try {
                    parseUserSafely(data, fallbackUserId = doc.id)
                } catch (e: Exception) {
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
            val userDoc = usersCollection.document(userId).get().await()
            val userName = userDoc.getString("fullName") ?: "User"

            usersCollection.document(userId).update(
                mapOf(
                    "verificationStatus" to "VERIFIED",
                    "isVerified" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            notificationRepository.sendUserVerifiedNotification(
                userId = userId,
                userName = userName
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to approve user")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAKE SUB-ADMIN
    //
    // CRITICAL FIX: role = "sub_admin" save karo, "admin" nahi!
    //
    // Pehle "admin" save ho raha tha — isliye DashboardViewModel mein
    // "admin" ko "super_admin" map hota tha aur sub_admin bhi Super Admin
    // dikhta tha.
    //
    // Ab:
    //   Sub Admin  → role = "sub_admin" + adminPermissions map
    //   Super Admin → role = "admin"    + NO adminPermissions field
    //
    // Firestore Security Rules mein isSubAdmin() function bhi update
    // karo (rules mein 'SUB_ADMIN' → 'sub_admin' ya 'SUB_ADMIN' dono
    // handle karo):
    //
    //   function isSubAdmin() {
    //     return isAuth() && (
    //       get(...).data.role == 'sub_admin' ||
    //       get(...).data.role == 'SUB_ADMIN'
    //     );
    //   }
    // ══════════════════════════════════════════════════════════════════════════
    suspend fun makeSubAdmin(
        userId: String,
        permissions: AdminPermissions
    ): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("User ID is required")
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val userName = userDoc.getString("fullName") ?: "User"

            // Purana role save karo taake removeSubAdmin() wapas restore kar sake
            val currentRole = userDoc.getString("role") ?: "tenant"

            usersCollection.document(userId).update(
                mapOf(
                    // ✦ FIX: "sub_admin" save karo, "admin" nahi
                    "role" to "sub_admin",
                    "preAdminRole" to currentRole,
                    "adminPermissions" to mapOf(
                        "canManageUsers" to permissions.canManageUsers,
                        "canVerifyUsers" to permissions.canVerifyUsers,
                        "canVerifyProperties" to permissions.canVerifyProperties,
                        "canManageProperties" to permissions.canManageProperties,
                        "canManageBookings" to permissions.canManageBookings,
                        "canViewReports" to permissions.canViewReports
                    ),
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            notificationRepository.sendNotification(
                recipientId = userId,
                type = NotificationType.GENERAL,
                title = "Admin Access Granted",
                body = "Hi $userName, you have been granted sub-admin access to HavenHub. Please check your dashboard.",
                targetRole = "sub_admin"
            )

            Log.d("ADMIN_REPO", "User $userId promoted to sub_admin with permissions: $permissions")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ADMIN_REPO", "makeSubAdmin failed for $userId: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to make user an admin")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REMOVE SUB-ADMIN
    // ══════════════════════════════════════════════════════════════════════════
    suspend fun removeSubAdmin(userId: String): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("User ID is required")
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val userName = userDoc.getString("fullName") ?: "User"
            val previousRole = userDoc.getString("preAdminRole") ?: "tenant"

            usersCollection.document(userId).update(
                mapOf(
                    "role" to previousRole,
                    "adminPermissions" to FieldValue.delete(),
                    "preAdminRole" to FieldValue.delete(),
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            notificationRepository.sendNotification(
                recipientId = userId,
                type = NotificationType.GENERAL,
                title = "Admin Access Revoked",
                body = "Hi $userName, your admin access has been revoked. You now have ${previousRole.replaceFirstChar { it.uppercaseChar() }} access.",
                targetRole = "all"
            )

            Log.d("ADMIN_REPO", "Admin access revoked for $userId, restored to $previousRole")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ADMIN_REPO", "removeSubAdmin failed for $userId: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to remove admin access")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE USER
    // ══════════════════════════════════════════════════════════════════════════
    suspend fun deleteUser(userId: String): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("User ID is required to delete a user")
        return try {
            Log.d("ADMIN_REPO", "Deleting user document: $userId")
            usersCollection.document(userId).delete().await()
            Log.d("ADMIN_REPO", "User document deleted successfully: $userId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ADMIN_REPO", "Failed to delete user $userId: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete user")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BAN USER
    // ══════════════════════════════════════════════════════════════════════════
    suspend fun banUser(userId: String): Resource<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val userName = userDoc.getString("fullName") ?: "User"
            val currentVerificationStatus = userDoc.getString("verificationStatus") ?: "PENDING"

            usersCollection.document(userId).update(
                mapOf(
                    "banned" to true,
                    "isBanned" to true,
                    "isVerified" to false,
                    "verificationStatus" to "REJECTED",
                    "prebanVerificationStatus" to currentVerificationStatus,
                    "active" to false,
                    "isActive" to false,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            notificationRepository.sendNotification(
                recipientId = userId,
                type = NotificationType.ACCOUNT_SUSPENDED,
                title = "Account Suspended",
                body = "Your account has been suspended. Please contact support for details.",
                targetRole = "all"
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to ban user")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UNBAN USER
    // ══════════════════════════════════════════════════════════════════════════
    suspend fun unbanUser(userId: String): Resource<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val userName = userDoc.getString("fullName") ?: "User"
            val restoredStatus = userDoc.getString("prebanVerificationStatus") ?: "PENDING"
            val restoredIsVerified = restoredStatus.uppercase() in listOf("VERIFIED", "APPROVED")

            usersCollection.document(userId).update(
                mapOf(
                    "banned" to false,
                    "isBanned" to false,
                    "verificationStatus" to restoredStatus,
                    "isVerified" to restoredIsVerified,
                    "active" to true,
                    "isActive" to true,
                    "prebanVerificationStatus" to FieldValue.delete(),
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            if (restoredIsVerified) {
                notificationRepository.sendNotification(
                    recipientId = userId,
                    type = NotificationType.ACCOUNT_VERIFIED,
                    title = "Account Restored",
                    body = "Your account has been restored, $userName. You can now access all features.",
                    targetRole = "all"
                )
            } else {
                notificationRepository.sendNotification(
                    recipientId = userId,
                    type = NotificationType.GENERAL,
                    title = "Account Restored",
                    body = "Your account has been restored, $userName. Please complete identity verification to access all features.",
                    targetRole = "all"
                )
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unban user")
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
            propertiesCollection.document(propertyId).update(
                mapOf(
                    "status" to "APPROVED",
                    "adminNote" to adminNote,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            val propDoc = propertiesCollection.document(propertyId).get().await()
            val ownerId = propDoc.getString("ownerId") ?: ""
            val propertyTitle = propDoc.getString("title") ?: "Property"

            if (ownerId.isNotBlank()) {
                notificationRepository.sendPropertyApprovedNotification(
                    ownerId = ownerId,
                    propertyId = propertyId,
                    propertyTitle = propertyTitle,
                    adminNote = adminNote
                )
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to approve property")
        }
    }

    suspend fun rejectProperty(propertyId: String, reason: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(
                mapOf(
                    "status" to "REJECTED",
                    "adminNote" to reason,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            val propDoc = propertiesCollection.document(propertyId).get().await()
            val ownerId = propDoc.getString("ownerId") ?: ""
            val propertyTitle = propDoc.getString("title") ?: "Property"

            if (ownerId.isNotBlank()) {
                notificationRepository.sendPropertyRejectedNotification(
                    ownerId = ownerId,
                    propertyId = propertyId,
                    propertyTitle = propertyTitle,
                    adminNote = reason
                )
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
            bookingsCollection.document(bookingId).update(
                mapOf(
                    "status" to status,
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
            bookingsCollection.document(bookingId).update(
                mapOf(
                    "status" to "CONFIRMED",
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
            bookingsCollection.document(bookingId).update(
                mapOf(
                    "status" to "CANCELLED",
                    "cancelledAt" to Timestamp.now(),
                    "cancelledBy" to "ADMIN",
                    "updatedAt" to FieldValue.serverTimestamp()
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
    // SEASONAL AVAILABILITY ALERTS
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun activatePackageAndNotifyTenants(packageId: String): Resource<Unit> {
        return try {
            packagesCollection.document(packageId).update(
                mapOf(
                    "status" to "ACTIVE",
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            val packageDoc = packagesCollection.document(packageId).get().await()
            val propertyId = packageDoc.getString("propertyId") ?: ""
            val propertyTitle = packageDoc.getString("propertyTitle") ?: "Property"
            val packageName = packageDoc.getString("packageName") ?: "Special Deal"
            val discounted = (packageDoc.getDouble("discountedPricePerNight") ?: 0.0).toInt()
            val availableFrom = packageDoc.getTimestamp("availableFrom")
            val availableTo = packageDoc.getTimestamp("availableTo")
            val dateText = buildDateRangeText(availableFrom, availableTo)

            sendSeasonalAlertToAllTenants(
                packageId = packageId,
                propertyId = propertyId,
                propertyTitle = propertyTitle,
                packageName = packageName,
                discounted = discounted,
                dateText = dateText
            )

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to activate package")
        }
    }

    suspend fun checkAndSendSeasonalAvailabilityAlerts(): Resource<Unit> {
        return try {
            val now = Timestamp.now()
            val snap = packagesCollection.whereEqualTo("status", "ACTIVE").get().await()
            val packages = snap.toObjects(RentalPackage::class.java)

            packages.forEach { pkg ->
                val availFrom = pkg.availableFrom
                val availTo = pkg.availableTo

                if (availTo != null && availTo.toDate().before(now.toDate())) {
                    packagesCollection.document(pkg.packageId)
                        .update("status", "EXPIRED").await()
                    return@forEach
                }

                if (availFrom != null) {
                    val diffMs = availFrom.toDate().time - now.toDate().time
                    val diffDays = diffMs / (1000 * 60 * 60 * 24)
                    if (diffDays in 0..3) {
                        val dateText = buildDateRangeText(availFrom, availTo)
                        sendSeasonalAlertToAllTenants(
                            packageId = pkg.packageId,
                            propertyId = pkg.propertyId,
                            propertyTitle = pkg.propertyTitle.ifBlank { "Property" },
                            packageName = pkg.packageName.ifBlank { "Special Deal" },
                            discounted = pkg.discountedPricePerNight.toInt(),
                            dateText = dateText
                        )
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send seasonal alerts")
        }
    }

    private suspend fun sendSeasonalAlertToAllTenants(
        packageId: String,
        propertyId: String,
        propertyTitle: String,
        packageName: String,
        discounted: Int,
        dateText: String
    ) {
        try {
            val tenantQuery = usersCollection.whereEqualTo("role", "TENANT").get().await()
            if (tenantQuery.isEmpty) {
                val fallback = usersCollection.whereEqualTo("role", "tenant").get().await()
                fallback.documents.forEach { doc ->
                    sendOneSeasonalAlert(
                        doc.id,
                        packageId,
                        propertyId,
                        propertyTitle,
                        packageName,
                        discounted,
                        dateText
                    )
                }
                return
            }
            tenantQuery.documents.forEach { doc ->
                sendOneSeasonalAlert(
                    doc.id,
                    packageId,
                    propertyId,
                    propertyTitle,
                    packageName,
                    discounted,
                    dateText
                )
            }
        } catch (e: Exception) {
            Log.e("ADMIN_REPO", "sendSeasonalAlertToAllTenants error: ${e.localizedMessage}")
        }
    }

    private suspend fun sendOneSeasonalAlert(
        tenantId: String,
        packageId: String,
        propertyId: String,
        propertyTitle: String,
        packageName: String,
        discounted: Int,
        dateText: String
    ) {
        notificationRepository.sendNotification(
            recipientId = tenantId,
            type = NotificationType.BOOKING_REMINDER,
            title = "$packageName Available!",
            body = "\"$propertyTitle\" has a limited deal: Rs. $discounted/night$dateText. Book now before it's gone!",
            referenceId = propertyId,
            targetRole = "tenant"
        )
    }

    private fun buildDateRangeText(from: Timestamp?, to: Timestamp?): String {
        if (from == null && to == null) return ""
        val fmt = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        return when {
            from != null && to != null -> " (${fmt.format(from.toDate())} - ${fmt.format(to.toDate())})"
            from != null -> " (from ${fmt.format(from.toDate())})"
            to != null -> " (until ${fmt.format(to.toDate())})"
            else -> ""
        }
    }
}