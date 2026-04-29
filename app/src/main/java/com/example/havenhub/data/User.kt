package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class User(
    val userId             : String          = "",
    val fullName           : String          = "",
    val email              : String          = "",
    val phoneNumber        : String          = "",
    val profileImageUrl    : String          = "",
    val role               : String          = "tenant",
    val verificationStatus : String          = "PENDING",
    val isVerified         : Boolean         = false,
    val isActive           : Boolean         = true,
    val isBanned           : Boolean         = false,

    // ✅ CNIC fields — tenant verification
    val cnicNumber         : String          = "",
    val cnicImageUrl       : String          = "",

    val nationalId         : String          = "",
    val idFrontUrl         : String          = "",
    val idBackUrl          : String          = "",
    val fcmToken           : String          = "",
    val landlordRating     : Float           = 0f,
    val landlordReviewCount: Int             = 0,
    val location           : Location?       = null,
    val preferences        : UserPreferences = UserPreferences(),

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    constructor() : this(userId = "")

    val initials: String
        get() = fullName.trim().split(" ")
            .filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

    val normalizedRole : String  get() = role.lowercase().trim()
    val isLandlord     : Boolean get() = normalizedRole == "landlord"
    val isAdmin        : Boolean get() = normalizedRole == "admin"
    val isTenant       : Boolean get() = normalizedRole == "tenant"

    val userRole: UserRole
        get() = try { UserRole.valueOf(role.uppercase().trim()) }
        catch (e: Exception) { UserRole.TENANT }

    val verificationStatusEnum: VerificationStatus
        get() = try { VerificationStatus.valueOf(verificationStatus.uppercase().trim()) }
        catch (e: Exception) { VerificationStatus.PENDING }
}

enum class UserRole {
    TENANT, LANDLORD, ADMIN;
    fun displayName() = when (this) {
        TENANT   -> "Tenant"
        LANDLORD -> "Landlord"
        ADMIN    -> "Admin"
    }
}

enum class VerificationStatus {
    PENDING, UNDER_REVIEW, VERIFIED, REJECTED;
    fun displayName() = when (this) {
        PENDING      -> "Pending"
        UNDER_REVIEW -> "Under Review"
        VERIFIED     -> "Verified"
        REJECTED     -> "Rejected"
    }
}