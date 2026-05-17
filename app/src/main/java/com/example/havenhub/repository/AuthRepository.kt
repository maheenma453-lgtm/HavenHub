package com.example.havenhub.repository

import android.util.Log
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
            User(
                userId              = doc.getString("userId")             ?: doc.id,
                fullName            = doc.getString("fullName")           ?: "",
                email               = doc.getString("email")              ?: "",
                phoneNumber         = doc.getString("phoneNumber")        ?: "",
                profileImageUrl     = doc.getString("profileImageUrl")    ?: "",
                role                = doc.getString("role")               ?: "tenant",
                verificationStatus  = doc.getString("verificationStatus") ?: "PENDING",
                isVerified          = doc.getBoolean("isVerified")        ?: false,
                isActive            = doc.getBoolean("isActive")          ?: true,
                isBanned            = doc.getBoolean("isBanned")          ?: false,
                cnicNumber          = doc.getString("cnicNumber")         ?: "",
                cnicImageUrl        = doc.getString("cnicImageUrl")       ?: "",
                nationalId          = doc.getString("nationalId")         ?: "",
                idFrontUrl          = doc.getString("idFrontUrl")         ?: "",
                idBackUrl           = doc.getString("idBackUrl")          ?: "",
                fcmToken            = doc.getString("fcmToken")           ?: "",
                landlordRating      = doc.getDouble("landlordRating")?.toFloat()  ?: 0f,
                landlordReviewCount = doc.getLong("landlordReviewCount")?.toInt() ?: 0,
                location = doc.get("location")?.let { raw ->
                    try {
                        val map = raw as? Map<*, *>
                        if (map != null) Location(
                            address   = map["address"]   as? String ?: "",
                            city      = map["city"]      as? String ?: "",
                            country   = map["country"]   as? String ?: "",
                            latitude  = (map["latitude"]  as? Double) ?: 0.0,
                            longitude = (map["longitude"] as? Double) ?: 0.0
                        ) else null
                    } catch (e: Exception) { null }
                },
                preferences = UserPreferences(),
                createdAt   = doc.getTimestamp("createdAt"),
                updatedAt   = doc.getTimestamp("updatedAt")
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

    // ── Helper: new social user document banao ────────────────────
    private suspend fun createSocialUserIfNew(
        user    : FirebaseUser,
        fcmToken: String
    ) {
        val doc = usersCollection.document(user.uid).get().await()
        if (!doc.exists()) {
            val userDoc = mapOf(
                "userId"              to user.uid,
                "fullName"            to (user.displayName ?: ""),
                "email"               to (user.email ?: ""),
                "role"                to "TENANT",
                "profileImageUrl"     to (user.photoUrl?.toString() ?: ""),
                "cnicNumber"          to "",
                "cnicImageUrl"        to "",
                "verificationStatus"  to "PENDING",
                "isVerified"          to false,
                "isActive"            to true,
                "isBanned"            to false,
                "phoneNumber"         to "",
                "fcmToken"            to fcmToken,
                "landlordRating"      to 0.0,
                "landlordReviewCount" to 0,
                "createdAt"           to FieldValue.serverTimestamp(),
                "updatedAt"           to FieldValue.serverTimestamp()
            )
            usersCollection.document(user.uid).set(userDoc).await()
        } else {
            usersCollection.document(user.uid)
                .update("fcmToken", fcmToken).await()
        }
    }

    suspend fun signIn(email: String, password: String): Resource<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user   = result.user ?: return Resource.Error("Sign in failed")
            saveFcmTokenToFirestore(user.uid)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sign in failed")
        }
    }

    suspend fun registerUser(
        email          : String,
        password       : String,
        fullName       : String,
        role           : String,
        profileImageUrl: String = "",
        cnicNumber     : String = "",
        cnicImageUrl   : String = ""
    ): Resource<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user   = result.user ?: return Resource.Error("Registration failed")

            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) { "" }

            val userDoc = mapOf(
                "userId"              to user.uid,
                "fullName"            to fullName,
                "email"               to email,
                "role"                to role.uppercase(),
                "profileImageUrl"     to profileImageUrl,
                "cnicNumber"          to cnicNumber,
                "cnicImageUrl"        to cnicImageUrl,
                "verificationStatus"  to "PENDING",
                "isVerified"          to false,
                "isActive"            to true,
                "isBanned"            to false,
                "phoneNumber"         to "",
                "fcmToken"            to fcmToken,
                "landlordRating"      to 0.0,
                "landlordReviewCount" to 0,
                "createdAt"           to FieldValue.serverTimestamp(),
                "updatedAt"           to FieldValue.serverTimestamp()
            )
            usersCollection.document(user.uid).set(userDoc).await()
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    // ── Google Sign In ────────────────────────────────────────────
    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser?> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result     = auth.signInWithCredential(credential).await()
            val user       = result.user ?: return Resource.Error("Google sign in failed")

            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) { "" }

            createSocialUserIfNew(user, fcmToken)
            Resource.Success(user)
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "signInWithGoogle error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Google sign in failed")
        }
    }

    suspend fun signOut() {
        try { auth.signOut() }
        catch (e: Exception) { Log.e("HAVEN_AUTH", "signOut error: ${e.localizedMessage}") }
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

    suspend fun getUserRole(uid: String): String {
        return try {
            val directDoc = usersCollection.document(uid).get().await()
            if (directDoc.exists()) {
                return directDoc.getString("role")?.lowercase()?.trim() ?: "tenant"
            }
            val query = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
            if (!query.isEmpty) {
                query.documents.first().getString("role")?.lowercase()?.trim() ?: "tenant"
            } else { "tenant" }
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "getUserRole error: ${e.localizedMessage}")
            "tenant"
        }
    }

    suspend fun getUserVerified(uid: String): Boolean {
        return try {
            val directDoc = usersCollection.document(uid).get().await()
            if (directDoc.exists()) {
                val status = directDoc.getString("verificationStatus")?.uppercase() ?: ""
                return status == "VERIFIED" || status == "APPROVED" ||
                        directDoc.getBoolean("isVerified") == true
            }
            val query = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
            if (!query.isEmpty) {
                val doc    = query.documents.first()
                val status = doc.getString("verificationStatus")?.uppercase() ?: ""
                status == "VERIFIED" || status == "APPROVED" ||
                        doc.getBoolean("isVerified") == true
            } else { false }
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "getUserVerified error: ${e.localizedMessage}")
            false
        }
    }

    suspend fun updateUserFields(uid: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            val directDoc = usersCollection.document(uid).get().await()
            val docId = if (directDoc.exists()) uid
            else {
                val query = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
                query.documents.firstOrNull()?.id ?: return Resource.Error("User not found")
            }
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
            val directDoc = usersCollection.document(uid).get().await()
            if (directDoc.exists()) {
                val user = parseUser(directDoc)
                if (user != null) return Resource.Success(user)
            }
            val query = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
            if (!query.isEmpty) {
                val user = parseUser(query.documents.first())
                if (user != null) return Resource.Success(user)
            }
            Resource.Error("User not found")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user")
        }
    }
}
