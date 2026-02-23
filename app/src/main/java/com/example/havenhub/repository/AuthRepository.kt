package com.example.havenhub.repository
import  com.google.firebase.auth.FirebaseUser
import com.havenhub.data.model.User
import com.havenhub.data.remote.FirebaseAuthManager
import com.havenhub.data.remote.FirebaseDataManager
import com.havenhub.data.remote.FirebaseMessagingManager
import com.havenhub.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository
 *
 * Orchestrates all authentication-related business logic for HavenHub.
 * Acts as the single source of truth for auth state between ViewModels
 * and the Firebase remote layer.
 *
 * Responsibilities:
 *  - Register new users and persist their profile to Firestore
 *  - Sign in (email/password and Google)
 *  - Sign out and clean up FCM tokens
 *  - Password reset
 *  - Exposing current user session state
 *
 * ViewModels should inject this repository rather than accessing
 * Firebase managers directly.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val dataManager: FirebaseDataManager,
    private val messagingManager: FirebaseMessagingManager
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Session State
    // ─────────────────────────────────────────────────────────────────────────

    /** The currently signed-in FirebaseUser, or null if not authenticated. */
    val currentUser: FirebaseUser? get() = authManager.currentUser

    /** The UID of the currently signed-in user, or null. */
    val currentUserId: String? get() = authManager.currentUserId

    /** Returns true if a user session is active. */
    fun isUserSignedIn(): Boolean = authManager.isUserSignedIn()

    // ─────────────────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a new user with email/password and saves their profile to Firestore.
     *
     * Flow:
     *  1. Create Firebase Auth account
     *  2. Build a [User] data model with role and display name
     *  3. Save user profile to Firestore `users` collection
     *  4. Save FCM token for push notification delivery
     *
     * @param email       The user's email address.
     * @param password    The chosen password.
     * @param fullName    The user's display name.
     * @param role        User role: "tenant", "landlord", or "admin".
     * @return [Resource.Success] with [FirebaseUser], or [Resource.Error].
     */
    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String,
        role: String
    ): Resource<FirebaseUser> {
        // Step 1: Create Firebase Auth account
        val authResult = authManager.registerWithEmail(email, password)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data

        // Step 2: Build user model
        val user = User(
            uid         = firebaseUser.uid,
            email       = email,
            fullName    = fullName,
            role        = role,
            isVerified  = false,
            createdAt   = System.currentTimeMillis()
        )

        // Step 3: Save profile to Firestore
        val saveResult = dataManager.saveUser(user)
        if (saveResult is Resource.Error) return Resource.Error(saveResult.message)

        // Step 4: Save FCM token (non-blocking, failure is non-critical)
        val tokenResult = messagingManager.getDeviceToken()
        if (tokenResult is Resource.Success) {
            messagingManager.saveDeviceToken(firebaseUser.uid, tokenResult.data)
        }

        return Resource.Success(firebaseUser)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sign In
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Signs in an existing user with email and password.
     * Also refreshes and stores the FCM token after successful login.
     *
     * @param email    The user's registered email.
     * @param password The user's password.
     * @return [Resource.Success] with [FirebaseUser], or [Resource.Error].
     */
    suspend fun signIn(email: String, password: String): Resource<FirebaseUser> {
        val authResult = authManager.signInWithEmail(email, password)
        if (authResult is Resource.Success) {
            // Refresh FCM token on each login to handle token rotation
            val tokenResult = messagingManager.getDeviceToken()
            if (tokenResult is Resource.Success) {
                messagingManager.saveDeviceToken(authResult.data.uid, tokenResult.data)
            }
        }
        return authResult
    }

    /**
     * Signs in using a Google ID token.
     * Creates a Firestore user profile if the account is new.
     *
     * @param idToken The Google Sign-In ID token from Google client.
     * @return [Resource.Success] with [FirebaseUser], or [Resource.Error].
     */
    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser> {
        val authResult = authManager.signInWithGoogle(idToken)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data

        // Check if user profile already exists in Firestore
        val existingUser = dataManager.getUser(firebaseUser.uid)
        if (existingUser is Resource.Error) {
            // New Google user — create default profile
            val user = User(
                uid      = firebaseUser.uid,
                email    = firebaseUser.email ?: "",
                fullName = firebaseUser.displayName ?: "",
                role     = "tenant",  // Default role; can be changed in onboarding
                photoUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
            dataManager.saveUser(user)
        }

        // Save FCM token for notifications
        val tokenResult = messagingManager.getDeviceToken()
        if (tokenResult is Resource.Success) {
            messagingManager.saveDeviceToken(firebaseUser.uid, tokenResult.data)
        }

        return Resource.Success(firebaseUser)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sign Out
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Signs out the current user and clears their FCM token.
     * Clearing the token prevents push notifications after sign-out.
     */
    suspend fun signOut() {
        authManager.currentUserId?.let { uid ->
            messagingManager.clearDeviceToken(uid)
        }
        authManager.signOut()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password Reset
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends a password reset email to the given address.
     *
     * @param email The email address to send the reset link to.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        return authManager.sendPasswordResetEmail(email)
    }
}


