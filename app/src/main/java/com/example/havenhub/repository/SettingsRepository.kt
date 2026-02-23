package com.example.havenhub.repository
import com.google.firebase.firestore.FirebaseFirestore
import com.havenhub.data.model.AppSettings
import com.havenhub.data.model.UserPreferences
import com.havenhub.utils.PreferenceManager
import com.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SettingsRepository
 *
 * Manages user preferences and app-wide settings for HavenHub.
 * Uses a two-layer approach:
 *
 *  1. Local layer ([PreferenceManager]): DataStore-backed preferences for
 *     fast, offline-accessible settings (theme, language, notifications toggle).
 *
 *  2. Remote layer ([FirebaseFirestore]): Firestore-backed settings for
 *     preferences that must sync across devices (notification preferences,
 *     privacy settings, account settings).
 *
 * Responsibilities:
 *  - Save and retrieve user notification preferences
 *  - Save and retrieve app theme/language preferences locally
 *  - Fetch and update remote user preferences from Firestore
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val firestore: FirebaseFirestore
) {

    private val userPreferencesCollection = firestore.collection("user_preferences")
    private val appSettingsCollection     = firestore.collection("app_settings")

    // ─────────────────────────────────────────────────────────────────────────
    // Local Preferences (DataStore)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves the user's dark mode preference locally via DataStore.
     * Takes effect immediately without a network round-trip.
     *
     * @param isDarkMode true to enable dark mode, false for light mode.
     */
    suspend fun setDarkMode(isDarkMode: Boolean) {
        preferenceManager.setDarkMode(isDarkMode)
    }

    /**
     * Retrieves the user's locally stored dark mode preference.
     *
     * @return true if dark mode is enabled, false otherwise (default: false).
     */
    suspend fun isDarkMode(): Boolean {
        return preferenceManager.isDarkMode()
    }

    /**
     * Saves the user's preferred language code locally (e.g., "en", "af", "zu").
     *
     * @param languageCode ISO 639-1 language code.
     */
    suspend fun setLanguage(languageCode: String) {
        preferenceManager.setLanguage(languageCode)
    }

    /**
     * Retrieves the locally stored language preference.
     *
     * @return ISO 639-1 language code (default: "en").
     */
    suspend fun getLanguage(): String {
        return preferenceManager.getLanguage()
    }

    /**
     * Saves the user's push notification toggle preference locally.
     *
     * @param enabled true to enable notifications, false to disable.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferenceManager.setNotificationsEnabled(enabled)
    }

    /**
     * Retrieves whether push notifications are enabled locally.
     *
     * @return true if enabled (default), false if disabled.
     */
    suspend fun areNotificationsEnabled(): Boolean {
        return preferenceManager.areNotificationsEnabled()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Remote User Preferences (Firestore)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches the [UserPreferences] document for a specific user from Firestore.
     * Used to sync preferences across multiple devices.
     *
     * @param userId The Firebase Auth UID of the user.
     * @return [Resource.Success] with [UserPreferences], or [Resource.Error].
     */
    suspend fun getUserPreferences(userId: String): Resource<UserPreferences> {
        return try {
            val snapshot = userPreferencesCollection.document(userId).get().await()
            val prefs = snapshot.toObject(UserPreferences::class.java)
                ?: UserPreferences(userId = userId) // Return defaults if no document exists
            Resource.Success(prefs)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user preferences")
        }
    }

    /**
     * Saves or updates the [UserPreferences] document for a user.
     * Creates the document if it doesn't exist.
     *
     * @param preferences The [UserPreferences] object to persist.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun saveUserPreferences(preferences: UserPreferences): Resource<Unit> {
        return try {
            userPreferencesCollection
                .document(preferences.userId)
                .set(preferences)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save user preferences")
        }
    }

    /**
     * Updates specific preference fields without overwriting the entire document.
     *
     * @param userId The user's Firebase Auth UID.
     * @param fields Map of preference field names to new values.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun updateUserPreferences(userId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            userPreferencesCollection.document(userId).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update user preferences")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Global App Settings (Firestore — Admin-Controlled)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches the global [AppSettings] document from Firestore.
     * Used to retrieve admin-controlled settings such as:
     *  - Maintenance mode flag
     *  - Minimum app version requirement
     *  - Feature flags
     *
     * @return [Resource.Success] with [AppSettings], or [Resource.Error].
     */
    suspend fun getAppSettings(): Resource<AppSettings> {
        return try {
            // "global" is the fixed document ID for app-wide settings
            val snapshot = appSettingsCollection.document("global").get().await()
            val settings = snapshot.toObject(AppSettings::class.java)
                ?: AppSettings() // Return defaults if settings doc missing
            Resource.Success(settings)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch app settings")
        }
    }
}


