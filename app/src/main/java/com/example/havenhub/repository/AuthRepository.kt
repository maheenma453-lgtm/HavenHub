package com.example.havenhub.repository

import android.util.Log
import com.example.havenhub.data.AdminPermissions
import com.example.havenhub.data.Location
import com.example.havenhub.data.User
import com.example.havenhub.data.UserPreferences
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth     : FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    private fun parseUser(doc: DocumentSnapshot): User? {
        return try {
            val adminPermissions: AdminPermissions? = doc.get("adminPermissions")?.let { raw ->
                try {
                    val map = raw as? Map<*, *>
                    if (map != null) AdminPermissions(
                        canManageUsers = map["canManageUsers"] as? Boolean ?: false,
                        canVerifyUsers = map["canVerifyUsers"] as? Boolean ?: false,
                        canVerifyProperties = map["canVerifyProperties"] as? Boolean ?: false,
                        canManageProperties = map["canManageProperties"] as? Boolean ?: false,
                        canManageBookings = map["canManageBookings"] as? Boolean ?: false,
                        canViewReports = map["canViewReports"] as? Boolean ?: false
                    ) else null
                } catch (e: Exception) {
                    null
                }
            }

            User(
                userId = doc.getString("userId") ?: doc.id,
                fullName = doc.getString("fullName") ?: "",
                email = doc.getString("email") ?: "",
                phoneNumber = doc.getString("phoneNumber") ?: "",
                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                role = doc.getString("role") ?: "tenant",
                verificationStatus = doc.getString("verificationStatus") ?: "PENDING",
                isVerified = doc.getBoolean("isVerified") ?: false,
                isActive = doc.getBoolean("isActive") ?: true,
                isBanned = doc.getBoolean("isBanned") ?: false,
                cnicNumber = doc.getString("cnicNumber") ?: "",
                cnicImageUrl = doc.getString("cnicImageUrl") ?: "",
                nationalId = doc.getString("nationalId") ?: "",
                idFrontUrl = doc.getString("idFrontUrl") ?: "",
                idBackUrl = doc.getString("idBackUrl") ?: "",
                fcmToken = doc.getString("fcmToken") ?: "",
                landlordRating = doc.getDouble("landlordRating")?.toFloat() ?: 0f,
                landlordReviewCount = doc.getLong("landlordReviewCount")?.toInt() ?: 0,
                location = doc.get("location")?.let { raw ->
                    try {
                        val map = raw as? Map<*, *>
                        if (map != null) Location(
                            address = map["address"] as? String ?: "",
                            city = map["city"] as? String ?: "",
                            country = map["country"] as? String ?: "",
                            latitude = (map["latitude"] as? Double) ?: 0.0,
                            longitude = (map["longitude"] as? Double) ?: 0.0
                        ) else null
                    } catch (e: Exception) {
                        null
                    }
                },
                adminPermissions = adminPermissions,
                preferences = UserPreferences(),
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt")
            )
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "parseUser FAIL for ${doc.id}: ${e.localizedMessage}")
            null
        }
    }

    private suspend fun saveFcmTokenToFirestore(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotEmpty()) {
                usersCollection.document(userId).update("fcmToken", token).await()
            }
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "saveFcmToken error: ${e.localizedMessage}")
        }
    }

    private suspend fun createSocialUserIfNew(user: FirebaseUser, fcmToken: String) {
        val doc = usersCollection.document(user.uid).get().await()
        if (!doc.exists()) {
            val userDoc = mapOf(
                "userId" to user.uid,
                "fullName" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "role" to "TENANT",
                "profileImageUrl" to (user.photoUrl?.toString() ?: ""),
                "cnicNumber" to "",
                "cnicImageUrl" to "",
                "verificationStatus" to "PENDING",
                "isVerified" to false,
                "isActive" to true,
                "isBanned" to false,
                "phoneNumber" to "",
                "fcmToken" to fcmToken,
                "landlordRating" to 0.0,
                "landlordReviewCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(user.uid).set(userDoc).await()
        } else {
            usersCollection.document(user.uid).update("fcmToken", fcmToken).await()
        }
    }

    suspend fun signIn(email: String, password: String): Resource<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("Sign in failed")
            saveFcmTokenToFirestore(user.uid)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sign in failed")
        }
    }

    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String,
        role: String,
        profileImageUrl: String = "",
        cnicNumber: String = "",
        cnicImageUrl: String = ""
    ): Resource<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("Registration failed")

            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                ""
            }

            val userDoc = mapOf(
                "userId" to user.uid,
                "fullName" to fullName,
                "email" to email,
                "role" to role.uppercase(),
                "profileImageUrl" to profileImageUrl,
                "cnicNumber" to cnicNumber,
                "cnicImageUrl" to cnicImageUrl,
                "verificationStatus" to "PENDING",
                "isVerified" to false,
                "isActive" to true,
                "isBanned" to false,
                "phoneNumber" to "",
                "fcmToken" to fcmToken,
                "landlordRating" to 0.0,
                "landlordReviewCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(user.uid).set(userDoc).await()
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser?> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return Resource.Error("Google sign in failed")

            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                ""
            }

            createSocialUserIfNew(user, fcmToken)
            Resource.Success(user)
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "signInWithGoogle error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Google sign in failed")
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "signOut error: ${e.localizedMessage}")
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send reset email")
        }
    }

    suspend fun deleteAccount(): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: return Resource.Error("No user logged in")
            usersCollection.document(user.uid).delete().await()
            user.delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete account")
        }
    }

    // =========================================================================
    // GET USER ROLE — FIXED
    //
    // Problem: Pehle sirf raw "role" field return hota tha. Agar Firestore mein
    // role = "admin" tha aur adminPermissions map bhi tha (legacy sub_admin
    // record), toh woh bhi "admin" return karta tha — AuthViewModel usse
    // "admin" samajhta tha (super_admin) aur "Sub Admin" ki jagah
    // "Super Admin" show hota tha.
    //
    // Fix: getUserRole() ab role + adminPermissions dono check karta hai:
    //   - role = "sub_admin"              → return "sub_admin"
    //   - role = "admin" + has permissions → return "sub_admin" (legacy)
    //   - role = "admin" + no permissions  → return "admin" (super admin)
    //   - role = "super_admin"             → return "admin" (normalized)
    //   - anything else                    → return as-is (tenant/landlord)
    // =========================================================================
    suspend fun getUserRole(uid: String): String {
        return try {
            val doc = fetchUserDoc(uid) ?: return "tenant"

            val rawRole = doc.getString("role")?.lowercase()?.trim() ?: "tenant"

            // Check adminPermissions map presence
            val hasPermissionsMap = doc.contains("adminPermissions") &&
                    doc.get("adminPermissions") != null

            val resolvedRole = when {
                rawRole == "sub_admin" -> "sub_admin"
                rawRole == "admin" && hasPermissionsMap -> "sub_admin"  // legacy record fix
                rawRole == "admin" -> "admin"
                rawRole == "super_admin" -> "admin"       // normalize to "admin"
                else -> rawRole
            }

            Log.d(
                "HAVEN_AUTH",
                "getUserRole uid=$uid rawRole=$rawRole hasPerms=$hasPermissionsMap resolved=$resolvedRole"
            )

            resolvedRole
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "getUserRole error: ${e.localizedMessage}")
            "tenant"
        }
    }

    // Helper — fetch user doc by uid (direct) or by userId field (fallback)
    private suspend fun fetchUserDoc(uid: String): DocumentSnapshot? {
        val directDoc = usersCollection.document(uid).get().await()
        if (directDoc.exists()) return directDoc

        val query = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
        return if (!query.isEmpty) query.documents.first() else null
    }

    suspend fun getUserVerified(uid: String): Boolean {
        return try {
            val doc = fetchUserDoc(uid) ?: return false
            val status = doc.getString("verificationStatus")?.uppercase() ?: ""
            status == "VERIFIED" || status == "APPROVED" ||
                    doc.getBoolean("isVerified") == true
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "getUserVerified error: ${e.localizedMessage}")
            false
        }
    }

    suspend fun updateUserFields(uid: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            val doc = fetchUserDoc(uid)
            val docId = doc?.id ?: return Resource.Error("User not found")

            val updatedFields = fields.toMutableMap()
            updatedFields["updatedAt"] = FieldValue.serverTimestamp()
            usersCollection.document(docId).update(updatedFields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update user")
        }
    }

    suspend fun getUser(uid: String): Resource<User> {
        return try {
            val doc = fetchUserDoc(uid)
            if (doc != null) {
                val user = parseUser(doc)
                if (user != null) return Resource.Success(user)
            }
            Resource.Error("User not found")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user")
        }
    }

    // =========================================================================
    // GET SUB-ADMIN PERMISSIONS — returns list of permission keys
    //
    // AuthViewModel mein yeh call hoti hai jab role == "sub_admin" hota hai.
    // Firestore se adminPermissions map read karke true wali keys return karo.
    // Navigation graph mein in keys se canManageUsers etc. decide hota hai.
    // =========================================================================
    suspend fun getSubAdminPermissions(uid: String): List<String> {
        return try {
            val doc = fetchUserDoc(uid) ?: return emptyList()

            // First check "adminPermissions" map (new format saved by AdminRepository)
            val permMap = doc.get("adminPermissions") as? Map<*, *>
            if (permMap != null) {
                val permissions = mutableListOf<String>()
                if (permMap["canManageUsers"] == true) permissions.add("manage_users")
                if (permMap["canVerifyUsers"] == true) permissions.add("verify_users")
                if (permMap["canVerifyProperties"] == true) permissions.add("verify_properties")
                if (permMap["canManageProperties"] == true) permissions.add("manage_properties")
                if (permMap["canManageBookings"] == true) permissions.add("manage_bookings")
                if (permMap["canViewReports"] == true) {
                    permissions.add("view_reports")
                    permissions.add("view_payment_reports")
                }
                Log.d("HAVEN_AUTH", "getSubAdminPermissions uid=$uid perms=$permissions")
                return permissions
            }

            // Fallback: old "permissions" string list format
            @Suppress("UNCHECKED_CAST")
            val legacyPerms = doc.get("permissions") as? List<String> ?: emptyList()
            Log.d("HAVEN_AUTH", "getSubAdminPermissions uid=$uid legacyPerms=$legacyPerms")
            legacyPerms
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "getSubAdminPermissions error: ${e.localizedMessage}")
            emptyList()
        }
    }
}