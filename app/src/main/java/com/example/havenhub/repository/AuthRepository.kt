package com.example.havenhub.repository

import com.example.havenhub.data.User
import com.example.havenhub.remote.FirebaseAuthManager
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseMessagingManager
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authManager      : FirebaseAuthManager,
    private val dataManager      : FirebaseDataManager,
    private val messagingManager : FirebaseMessagingManager
) {

    val currentUser   : FirebaseUser? get() = authManager.currentUser
    val currentUserId : String?       get() = authManager.currentUserId
    fun isUserSignedIn(): Boolean = authManager.isUserSignedIn()

    // ✅ FIX: UPPERCASE return karo — Firestore rules 'LANDLORD' expect karti hain
    suspend fun getUserRole(uid: String): String {
        return try {
            val result = dataManager.getUser(uid)
            if (result is Resource.Success)
                result.data.role.uppercase().trim().ifEmpty { "TENANT" }
            else "TENANT"
        } catch (e: Exception) { "TENANT" }
    }

    suspend fun getUserVerified(uid: String): Boolean {
        return try {
            val result = dataManager.getUser(uid)
            if (result is Resource.Success) {
                val user = result.data
                user.isVerified ||
                        user.verificationStatus.uppercase().trim() == "VERIFIED" ||
                        user.verificationStatus.uppercase().trim() == "APPROVED"
            } else false
        } catch (e: Exception) { false }
    }

    // ✅ FIX: role ab ALWAYS UPPERCASE save hoga Firestore mein
    suspend fun registerUser(
        email    : String,
        password : String,
        fullName : String,
        role     : String
    ): Resource<FirebaseUser> {

        val authResult = authManager.registerWithEmail(email, password)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data

        val user = User(
            userId     = firebaseUser.uid,
            email      = email,
            fullName   = fullName,
            role       = role.uppercase().trim(),   // ✅ UPPERCASE save
            isVerified = false
        )

        val saveResult = dataManager.saveUser(user)
        if (saveResult is Resource.Error) return Resource.Error(saveResult.message)

        val tokenResult = messagingManager.getDeviceToken()
        if (tokenResult is Resource.Success) {
            messagingManager.saveDeviceToken(firebaseUser.uid, tokenResult.data)
        }

        return Resource.Success(firebaseUser)
    }

    suspend fun signIn(email: String, password: String): Resource<FirebaseUser> {

        val authResult = authManager.signInWithEmail(email, password)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data

        val userResult = dataManager.getUser(firebaseUser.uid)

        if (userResult is Resource.Success) {
            if (userResult.data.isBanned) {
                authManager.signOut()
                return Resource.Error(
                    "Your account has been suspended. Please contact support for assistance."
                )
            }
        }

        val tokenResult = messagingManager.getDeviceToken()
        if (tokenResult is Resource.Success) {
            messagingManager.saveDeviceToken(firebaseUser.uid, tokenResult.data)
        }

        return Resource.Success(firebaseUser)
    }

    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser> {

        val authResult = authManager.signInWithGoogle(idToken)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data
        val existingUser = dataManager.getUser(firebaseUser.uid)

        if (existingUser is Resource.Error) {
            val user = User(
                userId          = firebaseUser.uid,
                email           = firebaseUser.email ?: "",
                fullName        = firebaseUser.displayName ?: "",
                role            = "TENANT",         // ✅ UPPERCASE
                isVerified      = false,
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
            dataManager.saveUser(user)

        } else if (existingUser is Resource.Success) {
            if (existingUser.data.isBanned) {
                authManager.signOut()
                return Resource.Error(
                    "Your account has been suspended. Please contact support for assistance."
                )
            }
        }

        val tokenResult = messagingManager.getDeviceToken()
        if (tokenResult is Resource.Success) {
            messagingManager.saveDeviceToken(firebaseUser.uid, tokenResult.data)
        }

        return Resource.Success(firebaseUser)
    }

    suspend fun signOut() {
        authManager.currentUserId?.let { uid ->
            messagingManager.clearDeviceToken(uid)
        }
        authManager.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> =
        authManager.sendPasswordResetEmail(email)

    suspend fun deleteAccount(): Resource<Unit> {
        return try {
            val uid = authManager.currentUserId
                ?: return Resource.Error("No user logged in")

            messagingManager.clearDeviceToken(uid)
            dataManager.deleteUser(uid)
            authManager.deleteAccount()

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete account")
        }
    }
}