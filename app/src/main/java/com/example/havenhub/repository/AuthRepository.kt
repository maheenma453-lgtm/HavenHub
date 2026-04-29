package com.example.havenhub.repository

import android.util.Log
import com.example.havenhub.data.User
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

    // ── Sign In ───────────────────────────────────────────────────────────────
    suspend fun signIn(email: String, password: String): Resource<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Resource.Success(result.user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sign in failed")
        }
    }

    // ── Register — CNIC + profile image bhi save hoga ────────────────────────
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

            val userDoc = mapOf(
                "userId"             to user.uid,
                "fullName"           to fullName,
                "email"              to email,
                "role"               to role.uppercase(),
                "profileImageUrl"    to profileImageUrl,
                "cnicNumber"         to cnicNumber,
                "cnicImageUrl"       to cnicImageUrl,
                "verificationStatus" to "PENDING",
                "isVerified"         to false,
                "isActive"           to true,
                "isBanned"           to false,
                "phoneNumber"        to "",
                "fcmToken"           to "",
                "landlordRating"     to 0.0,
                "landlordReviewCount" to 0,
                "createdAt"          to FieldValue.serverTimestamp(),
                "updatedAt"          to FieldValue.serverTimestamp()
            )

            usersCollection.document(user.uid).set(userDoc).await()
            Log.d("HAVEN_AUTH", "User registered: ${user.uid} role=$role")
            Resource.Success(user)
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "registerUser error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    // ── Google Sign In ────────────────────────────────────────────────────────
    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser?> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result     = auth.signInWithCredential(credential).await()
            val user       = result.user ?: return Resource.Error("Google sign in failed")

            // Agar pehli baar login ho toh Firestore mein save karo
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
                    "fcmToken"            to "",
                    "landlordRating"      to 0.0,
                    "landlordReviewCount" to 0,
                    "createdAt"           to FieldValue.serverTimestamp(),
                    "updatedAt"           to FieldValue.serverTimestamp()
                )
                usersCollection.document(user.uid).set(userDoc).await()
            }

            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Google sign in failed")
        }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────
    suspend fun signOut() {
        try { auth.signOut() }
        catch (e: Exception) { Log.e("HAVEN_AUTH", "signOut error: ${e.localizedMessage}") }
    }

    // ── Password Reset ────────────────────────────────────────────────────────
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send reset email")
        }
    }

    // ── Delete Account ────────────────────────────────────────────────────────
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

    // ── Get User Role ─────────────────────────────────────────────────────────
    suspend fun getUserRole(uid: String): String {
        return try {
            // Direct UID se pehle try karo
            val directDoc = usersCollection.document(uid).get().await()
            if (directDoc.exists()) {
                return directDoc.getString("role")?.lowercase()?.trim() ?: "tenant"
            }
            // Fallback: userId field se query
            val query = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
            if (!query.isEmpty) {
                query.documents.first().getString("role")?.lowercase()?.trim() ?: "tenant"
            } else {
                "tenant"
            }
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "getUserRole error: ${e.localizedMessage}")
            "tenant"
        }
    }

    // ── Get User Verified Status ──────────────────────────────────────────────
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
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("HAVEN_AUTH", "getUserVerified error: ${e.localizedMessage}")
            false
        }
    }

    // ── Update User Fields ────────────────────────────────────────────────────
    suspend fun updateUserFields(uid: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            val directDoc = usersCollection.document(uid).get().await()
            val docId = if (directDoc.exists()) {
                uid
            } else {
                val query = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
                query.documents.firstOrNull()?.id
                    ?: return Resource.Error("User not found")
            }
            val updatedFields = fields.toMutableMap()
            updatedFields["updatedAt"] = FieldValue.serverTimestamp()
            usersCollection.document(docId).update(updatedFields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update user")
        }
    }

    // ── Get Full User Object ──────────────────────────────────────────────────
    suspend fun getUser(uid: String): Resource<User> {
        return try {
            val directDoc = usersCollection.document(uid).get().await()
            if (directDoc.exists()) {
                val user = directDoc.toObject(User::class.java)
                if (user != null) return Resource.Success(user)
            }
            val query = usersCollection.whereEqualTo("userId", uid).limit(1).get().await()
            if (!query.isEmpty) {
                val user = query.documents.first().toObject(User::class.java)
                if (user != null) return Resource.Success(user)
            }
            Resource.Error("User not found")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user")
        }
    }
}