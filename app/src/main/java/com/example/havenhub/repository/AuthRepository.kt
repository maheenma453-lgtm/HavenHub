package com.example.havenhub.repository

import com.example.havenhub.data.User
import com.example.havenhub.data.UserRole
import com.example.havenhub.remote.FirebaseAuthManager
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseMessagingManager
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val dataManager: FirebaseDataManager,
    private val messagingManager: FirebaseMessagingManager
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Session State
    // ─────────────────────────────────────────────────────────────────────────

    val currentUser: FirebaseUser? get() = authManager.currentUser
    val currentUserId: String? get() = authManager.currentUserId
    fun isUserSignedIn(): Boolean = authManager.isUserSignedIn()

    // ─────────────────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String,
        role: String
    ): Resource<FirebaseUser> {
        val authResult = authManager.registerWithEmail(email, password)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data

        val user = User(
            userId     = firebaseUser.uid,
            email      = email,
            fullName   = fullName,
            role       = UserRole.valueOf(role.uppercase()),
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

    // ─────────────────────────────────────────────────────────────────────────
    // Sign In
    // ─────────────────────────────────────────────────────────────────────────

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
                role            = UserRole.TENANT,
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

    // ─────────────────────────────────────────────────────────────────────────
    // Sign Out
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun signOut() {
        authManager.currentUserId?.let { uid ->
            messagingManager.clearDeviceToken(uid)
        }
        authManager.signOut()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password Reset
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> =
        authManager.sendPasswordResetEmail(email)
}