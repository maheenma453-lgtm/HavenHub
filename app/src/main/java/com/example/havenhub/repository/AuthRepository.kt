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

    suspend fun getUserRole(uid: String): String {
        return try {
            val result = dataManager.getUser(uid)
            if (result is Resource.Success) result.data.role.lowercase()
            else "tenant"
        } catch (e: Exception) { "tenant" }
    }

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
            role       = role.uppercase(),
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
        if (authResult is Resource.Success) {
            val tokenResult = messagingManager.getDeviceToken()
            if (tokenResult is Resource.Success) {
                messagingManager.saveDeviceToken(authResult.data.uid, tokenResult.data)
            }
        }
        return authResult
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
                role            = "TENANT",
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
            dataManager.saveUser(user)
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

    // ✅ NEW: Delete account — Firestore data + FCM token + Firebase Auth
    suspend fun deleteAccount(): Resource<Unit> {
        return try {
            val uid = authManager.currentUserId
                ?: return Resource.Error("No user logged in")

            // 1. FCM token clear karo
            messagingManager.clearDeviceToken(uid)

            // 2. Firestore user document delete karo
            dataManager.deleteUser(uid)

            // 3. Firebase Auth account delete karo
            authManager.deleteAccount()

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete account")
        }
    }
}