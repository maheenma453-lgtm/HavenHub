package com.example.havenhub.repository

import com.example.havenhub.data.AppSettings
import com.example.havenhub.data.UserPreferences
import com.example.havenhub.utils.PreferenceManager
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

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

    // ✅ FIXED: update() ki jagah set() with merge use kiya
    // Pehle agar document exist nahi hota tha to crash ho jata tha
    // Ab document na ho to khud bana leta hai, ho to sirf update karta hai
    suspend fun updateUserPreferences(userId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            userPreferencesCollection
                .document(userId)
                .set(fields, SetOptions.merge()) // ✅ Yeh fix hai
                .await()
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

            if (!snapshot.exists()) {
                return Resource.Success(AppSettings())
            }

            val data = snapshot.data ?: return Resource.Success(AppSettings())

            val updatedAt = when (val raw = data["updatedAt"]) {
                is Timestamp -> raw
                is Date -> Timestamp(raw)
                is Long -> Timestamp(Date(raw))
                else -> Timestamp.now()
            }

            val settings = AppSettings(
                isMaintenanceMode = data["isMaintenanceMode"] as? Boolean ?: false,
                maintenanceMessage = data["maintenanceMessage"] as? String,
                minimumAppVersion = data["minimumAppVersion"] as? String ?: "1.0.0",
                latestAppVersion = data["latestAppVersion"] as? String ?: "1.0.0",
                forceUpdate = data["forceUpdate"] as? Boolean ?: false,
                platformFeePercent = (data["platformFeePercent"] as? Number)?.toDouble() ?: 5.0,
                maxPropertyImages = (data["maxPropertyImages"] as? Number)?.toInt() ?: 10,
                maxBookingDaysAdvance = (data["maxBookingDaysAdvance"] as? Number)?.toInt() ?: 90,
                featuredPropertyIds = (data["featuredPropertyIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                announcementBanner = data["announcementBanner"] as? String,
                supportEmail = data["supportEmail"] as? String ?: "support@havenhub.co.za",
                termsOfServiceUrl = data["termsOfServiceUrl"] as? String ?: "https://havenhub.co.za/terms",
                privacyPolicyUrl = data["privacyPolicyUrl"] as? String ?: "https://havenhub.co.za/privacy",
                updatedAt = updatedAt
            )

            Resource.Success(settings)

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch app settings")
        }
    }
}