package com.example.havenhub.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a HavenHub user stored in Firestore → `users/{userId}`.
 *
 * Roles:
 *  - [UserRole.TENANT]   — can search and book properties
 *  - [UserRole.LANDLORD] — can list and manage properties
 *  - [UserRole.ADMIN]    — full platform access
 */
data class User(

    @DocumentId
    val userId: String = "",

    val fullName: String = "",

    val email: String = "",

    val phoneNumber: String = "",

    /** Firebase Storage URL for the user's profile photo. */
    val profileImageUrl: String = "",

    /** One of: TENANT, LANDLORD, ADMIN */
    val role: UserRole = UserRole.TENANT,

    /** Account verification status. */
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,

    /** True after admin manually approves KYC documents. */
    val isVerified: Boolean = false,

    /** True when the account is in good standing. */
    val isActive: Boolean = true,

    /** National ID or passport number submitted for KYC. */
    val nationalId: String = "",

    /** URL of the uploaded government-issued ID (front). */
    val idFrontUrl: String = "",

    /** URL of the uploaded government-issued ID (back). */
    val idBackUrl: String = "",

    /** FCM token for push notifications; refreshed on each login. */
    val fcmToken: String = "",

    /** Average rating as a landlord (calculated field). */
    val landlordRating: Float = 0f,

    /** Total number of reviews received as a landlord. */
    val landlordReviewCount: Int = 0,

    /** User's home / search location preference. */
    val location: Location? = null,

    /** User-level app preferences (notifications, language, etc.). */
    val preferences: UserPreferences = UserPreferences(),

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    val updatedAt: Timestamp? = null
) {
    /** Firebase requires a no-arg constructor for deserialization. */
    constructor() : this(userId = "")

    /** Display-friendly initials derived from [fullName]. */
    val initials: String
        get() = fullName
            .trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

    /** True if the user can list properties. */
    val isLandlord: Boolean get() = role == UserRole.LANDLORD

    /** True if the user has admin privileges. */
    val isAdmin: Boolean get() = role == UserRole.ADMIN
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
    /** User has not submitted any documents yet. */
    PENDING,

    /** Documents submitted; awaiting admin review. */
    UNDER_REVIEW,

    /** Admin has approved the user. */
    VERIFIED,

    /** Admin rejected the documents; user can resubmit. */
    REJECTED;

    fun displayName(): String = when (this) {
        PENDING      -> "Pending"
        UNDER_REVIEW -> "Under Review"
        VERIFIED     -> "Verified"
        REJECTED     -> "Rejected"
    }
}


