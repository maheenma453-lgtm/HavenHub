package com.example.havenhub.remote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton



/**
 * FirebaseAuthManager
 *
 * Handles all Firebase Authentication operations for HavenHub.
 * Supports Email/Password and Google Sign-In flows.
 *
 * Responsibilities:
 * - User registration & login (email/password)
 * - Google Sign-In integration
 * - Password reset via email
 * - Session management (current user, sign-out)
 *
 * All functions return [Resource] wrappers to safely propagate
 * success, loading, and error states to the Repository layer.
 */
@Singleton
class FirebaseAuthManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Properties
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the currently authenticated Firebase user, or null if signed out. */
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /** Returns the UID of the currently authenticated user. */
    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    // ─────────────────────────────────────────────────────────────────────────
    // Email / Password Auth
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a new user with the given email and password.
     *
     * @param email    The user's email address.
     * @param password The user's chosen password (min 6 characters).
     * @return [Resource.Success] with [FirebaseUser] on success,
     *         [Resource.Error] with a message on failure.
     */
    suspend fun registerWithEmail(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("Registration failed: user is null")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    /**
     * Signs in an existing user with email and password.
     *
     * @param email    The user's registered email.
     * @param password The user's password.
     * @return [Resource.Success] with [FirebaseUser] on success,
     *         [Resource.Error] with a message on failure.
     */
    suspend fun signInWithEmail(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("Sign-in failed: user is null")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sign-in failed")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Google Sign-In
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Signs in using a Google ID token obtained from Google Sign-In client.
     *
     * Usage:
     *  1. Trigger Google Sign-In UI from the screen/ViewModel.
     *  2. Pass the resulting idToken here.
     *
     * @param idToken The Google Sign-In ID token.
     * @return [Resource.Success] with [FirebaseUser] on success,
     *         [Resource.Error] with a message on failure.
     */
    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return Resource.Error("Google sign-in failed")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password Reset
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends a password reset email to the given address.
     *
     * @param email The email address of the account to reset.
     * @return [Resource.Success] with Unit on success,
     *         [Resource.Error] with a message on failure.
     */
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send reset email")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Session
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Signs out the currently authenticated user.
     * Clears the local Firebase session.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Checks whether a user is currently signed in.
     *
     * @return true if a user session is active, false otherwise.
     */
    fun isUserSignedIn(): Boolean = firebaseAuth.currentUser != null
}


