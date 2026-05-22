package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

// Granular permission flags stored in Firestore under each admin user document.
// Super Admin always has all permissions = true (enforced in code, not Firestore).
// Sub-Admins get only the permissions that Super Admin explicitly grants them.
data class AdminPermissions(
    val canManageUsers      : Boolean = false,
    val canVerifyUsers      : Boolean = false,
    val canVerifyProperties : Boolean = false,
    val canManageProperties : Boolean = false,
    val canManageBookings   : Boolean = false,
    val canViewReports      : Boolean = false
)

data class User(
    val userId             : String            = "",
    val fullName           : String            = "",
    val email              : String            = "",
    val phoneNumber        : String            = "",
    val profileImageUrl    : String            = "",
    val role               : String            = "tenant",
    val verificationStatus : String            = "PENDING",
    val isVerified         : Boolean           = false,
    val isActive           : Boolean           = true,
    val isBanned           : Boolean           = false,

    // CNIC fields
    val cnicNumber         : String            = "",
    val cnicImageUrl       : String            = "",

    val nationalId         : String            = "",
    val idFrontUrl         : String            = "",
    val idBackUrl          : String            = "",
    val fcmToken           : String            = "",
    val landlordRating     : Float             = 0f,
    val landlordReviewCount: Int               = 0,
    val location           : Location?         = null,
    val preferences        : UserPreferences   = UserPreferences(),

    // Admin permission map — null for non-admin users
    val adminPermissions   : AdminPermissions? = null,

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
    val isSubAdmin     : Boolean get() = normalizedRole == "sub_admin"   // ← ADDED
    val isTenant       : Boolean get() = normalizedRole == "tenant"
    val isAnyAdmin     : Boolean get() = isAdmin || isSubAdmin           // ← ADDED

    val userRole: UserRole
        get() = try { UserRole.valueOf(role.uppercase().trim()) }
        catch (e: Exception) { UserRole.TENANT }

    val verificationStatusEnum: VerificationStatus
        get() = try { VerificationStatus.valueOf(verificationStatus.uppercase().trim()) }
        catch (e: Exception) { VerificationStatus.PENDING }
}

// ✅ FIXED: SUB_ADMIN added, duplicate "ADMIN" case removed
enum class UserRole {
    TENANT, LANDLORD, ADMIN, SUB_ADMIN;

    fun displayName() = when (this) {
        TENANT    -> "Tenant"
        LANDLORD  -> "Landlord"
        ADMIN     -> "Super Admin"
        SUB_ADMIN -> "Sub Admin"   // ← ab yeh properly kaam karega
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