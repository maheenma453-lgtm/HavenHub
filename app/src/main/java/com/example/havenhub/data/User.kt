package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class User(

    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",

    // ✅ FIX: String rakho — Firestore enum deserialize nahi kar sakta
    val role: String = "TENANT",

    // ✅ FIX: yeh bhi String
    val verificationStatus: String = "PENDING",

    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val isBanned: Boolean = false,

    val nationalId: String = "",
    val idFrontUrl: String = "",
    val idBackUrl: String = "",
    val fcmToken: String = "",

    val landlordRating: Float = 0f,
    val landlordReviewCount: Int = 0,

    val location: Location? = null,
    val preferences: UserPreferences = UserPreferences(),

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null

) {
    /** Firebase requires a no-arg constructor for deserialization. */
    constructor() : this(userId = "")

    /** Display-friendly initials derived from fullName. */
    val initials: String
        get() = fullName
            .trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

    // ✅ String se compare karo directly
    val isLandlord: Boolean get() = role == "LANDLORD"
    val isAdmin: Boolean    get() = role == "ADMIN"
    val isTenant: Boolean   get() = role == "TENANT"

    // ✅ Jahan enum zaruri ho wahan yeh use karo
    val userRole: UserRole
        get() = try {
            UserRole.valueOf(role)
        } catch (e: Exception) {
            UserRole.TENANT
        }

    val verificationStatusEnum: VerificationStatus
        get() = try {
            VerificationStatus.valueOf(verificationStatus)
        } catch (e: Exception) {
            VerificationStatus.PENDING
        }
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class UserRole {
    TENANT,
    LANDLORD,
    ADMIN;

    fun displayName(): String = when (this) {
        TENANT   -> "Tenant"
        LANDLORD -> "Landlord"
        ADMIN    -> "Admin"
    }
}

enum class VerificationStatus {
    PENDING,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED;

    fun displayName(): String = when (this) {
        PENDING      -> "Pending"
        UNDER_REVIEW -> "Under Review"
        VERIFIED     -> "Verified"
        REJECTED     -> "Rejected"
    }
}