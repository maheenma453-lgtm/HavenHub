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

    // ── Current session helpers ───────────────────────────────────────────────
    val currentUser   : FirebaseUser? get() = authManager.currentUser
    val currentUserId : String?       get() = authManager.currentUserId
    fun isUserSignedIn(): Boolean = authManager.isUserSignedIn()

    // ─────────────────────────────────────────────────────────────────────────
    // getUserRole — fetches the role string of a user from Firestore.
    // Returns "tenant" as safe default if fetch fails.
    // Called by AuthViewModel after login and on app start.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getUserRole(uid: String): String {
        return try {
            val result = dataManager.getUser(uid)
            if (result is Resource.Success) result.data.role.lowercase()
            else "tenant"
        } catch (e: Exception) { "tenant" }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserVerified — fetches the isVerified boolean of a user from Firestore.
    //
    // NEW: Called by AuthViewModel after every login and on app start.
    // The result is stored in AuthUiState.isVerified which NavGraph reads to
    // decide whether to allow or block:
    //   - Unverified Tenant   → cannot open BookingScreen
    //   - Unverified Landlord → cannot open AddPropertyScreen
    //
    // Returns false as safe default — unverified is always the safer assumption.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getUserVerified(uid: String): Boolean {
        return try {
            val result = dataManager.getUser(uid)
            if (result is Resource.Success) result.data.isVerified
            else false   // Safe default — treat as unverified if fetch fails
        } catch (e: Exception) {
            false         // Safe default — never assume verified on error
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // registerUser — creates Firebase Auth account then saves user profile.
    // New users always start as unverified (admin must verify them).
    // Steps:
    //   1. Create Firebase Auth account
    //   2. Save user profile to Firestore (isVerified = false)
    //   3. Save FCM device token for push notifications
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun registerUser(
        email    : String,
        password : String,
        fullName : String,
        role     : String
    ): Resource<FirebaseUser> {

        // Step 1: Create Firebase Auth account
        val authResult = authManager.registerWithEmail(email, password)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data

        // Step 2: Save user profile to Firestore — always unverified on registration
        val user = User(
            userId     = firebaseUser.uid,
            email      = email,
            fullName   = fullName,
            role       = role.uppercase(),
            isVerified = false   // Admin must verify before user gets full access
        )

        val saveResult = dataManager.saveUser(user)
        if (saveResult is Resource.Error) return Resource.Error(saveResult.message)

        // Step 3: Save FCM token so this device receives push notifications
        val tokenResult = messagingManager.getDeviceToken()
        if (tokenResult is Resource.Success) {
            messagingManager.saveDeviceToken(firebaseUser.uid, tokenResult.data)
        }

        return Resource.Success(firebaseUser)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // signIn — authenticates with email + password.
    //
    // FIX: Added banned user check after successful Firebase Auth login.
    // Previously banned users could still sign in because there was no check
    // against the Firestore "banned" field after authentication succeeded.
    //
    // Flow:
    //   1. Authenticate with Firebase Auth
    //   2. Fetch user profile from Firestore
    //   3. If banned → sign out immediately + return error
    //   4. Otherwise → save FCM token and return success
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun signIn(email: String, password: String): Resource<FirebaseUser> {

        // Step 1: Authenticate with Firebase
        val authResult = authManager.signInWithEmail(email, password)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data

        // Step 2: Fetch user profile from Firestore to check ban status
        val userResult = dataManager.getUser(firebaseUser.uid)

        if (userResult is Resource.Success) {
            // Step 3: Block banned users — sign out immediately
            if (userResult.data.isBanned) {
                authManager.signOut()
                return Resource.Error(
                    "Your account has been suspended. Please contact support for assistance."
                )
            }
        }

        // Step 4: Not banned — save FCM token and allow login
        val tokenResult = messagingManager.getDeviceToken()
        if (tokenResult is Resource.Success) {
            messagingManager.saveDeviceToken(firebaseUser.uid, tokenResult.data)
        }

        return Resource.Success(firebaseUser)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // signInWithGoogle — authenticates with Google OAuth token.
    //
    // FIX: Added banned user check — same logic as email sign in.
    // Also creates a new Firestore profile for first-time Google users.
    //
    // Flow:
    //   1. Authenticate with Google via Firebase
    //   2. New user → create Firestore profile (unverified by default)
    //   3. Existing user → check if banned, block if so
    //   4. Save FCM token and return success
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser> {

        // Step 1: Authenticate with Google via Firebase
        val authResult = authManager.signInWithGoogle(idToken)
        if (authResult is Resource.Error) return authResult

        val firebaseUser = (authResult as Resource.Success).data

        // Step 2: Check if user already exists in Firestore
        val existingUser = dataManager.getUser(firebaseUser.uid)

        if (existingUser is Resource.Error) {
            // New Google user — create Firestore profile (unverified by default)
            val user = User(
                userId          = firebaseUser.uid,
                email           = firebaseUser.email ?: "",
                fullName        = firebaseUser.displayName ?: "",
                role            = "TENANT",
                isVerified      = false,
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
            dataManager.saveUser(user)

        } else if (existingUser is Resource.Success) {
            // Step 3: Existing user — block if banned
            if (existingUser.data.isBanned) {
                authManager.signOut()
                return Resource.Error(
                    "Your account has been suspended. Please contact support for assistance."
                )
            }
        }

        // Step 4: Not banned — save FCM token and allow login
        val tokenResult = messagingManager.getDeviceToken()
        if (tokenResult is Resource.Success) {
            messagingManager.saveDeviceToken(firebaseUser.uid, tokenResult.data)
        }

        return Resource.Success(firebaseUser)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // signOut — clears FCM token first, then signs out from Firebase Auth.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun signOut() {
        authManager.currentUserId?.let { uid ->
            messagingManager.clearDeviceToken(uid)
        }
        authManager.signOut()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // sendPasswordResetEmail — sends reset link to the given email.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> =
        authManager.sendPasswordResetEmail(email)

    // ─────────────────────────────────────────────────────────────────────────
    // deleteAccount — completely removes the user from the app.
    // Steps:
    //   1. Clear FCM device token (stop push notifications)
    //   2. Delete Firestore profile document
    //   3. Delete Firebase Auth account
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun deleteAccount(): Resource<Unit> {
        return try {
            val uid = authManager.currentUserId
                ?: return Resource.Error("No user logged in")

            // Step 1: Stop push notifications on this device
            messagingManager.clearDeviceToken(uid)

            // Step 2: Delete Firestore profile document
            dataManager.deleteUser(uid)

            // Step 3: Delete Firebase Auth account
            authManager.deleteAccount()

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete account")
        }
    }
}
