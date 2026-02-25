package com.example.havenhub.repository

import com.example.havenhub.data.AppSettings
import com.example.havenhub.data.UserPreferences
import com.example.havenhub.utils.PreferenceManager
import com.example.havenhub.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val firestore: FirebaseFirestore
) {

    private val userPreferencesCollection = firestore.collection("user_preferences")
    private val appSettingsCollection = firestore.collection("app_settings")

    // ─────────────────────────────────────────────────────────────────────────
    // Local Preferences (SharedPreferences)
    // ─────────────────────────────────────────────────────────────────────────

    fun setDarkMode(isDarkMode: Boolean) = preferenceManager.setDarkMode(isDarkMode)
    fun isDarkMode(): Boolean = preferenceManager.isDarkMode()

    fun setLanguage(languageCode: String) = preferenceManager.setLanguage(languageCode)
    fun getLanguage(): String = preferenceManager.getLanguage()

    fun setNotificationsEnabled(enabled: Boolean) = preferenceManager.setPushEnabled(enabled)
    fun areNotificationsEnabled(): Boolean = preferenceManager.isPushEnabled()

    // ─────────────────────────────────────────────────────────────────────────
    // Remote User Preferences (Firestore)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getUserPreferences(userId: String): Resource<UserPreferences> {
        return try {
            val snapshot = userPreferencesCollection.document(userId).get().await()
            val prefs = snapshot.toObject(UserPreferences::class.java)
                ?: UserPreferences(userId = userId)
            Resource.Success(prefs)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user preferences")
        }
    }

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

    suspend fun updateUserPreferences(userId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            userPreferencesCollection.document(userId).update(fields).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update user preferences")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Global App Settings (Firestore)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAppSettings(): Resource<AppSettings> {
        return try {
            val snapshot = appSettingsCollection.document("global").get().await()
            val settings = snapshot.toObject(AppSettings::class.java) ?: AppSettings()
            Resource.Success(settings)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch app settings")
        }
    }
}