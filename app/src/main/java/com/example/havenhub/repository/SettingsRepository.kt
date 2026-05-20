package com.example.havenhub.repository

import com.example.havenhub.data.AppSettings
import com.example.havenhub.data.UserPreferences
import com.example.havenhub.utils.PreferenceManager
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val firestore        : FirebaseFirestore
) {

    private val userPreferencesCollection = firestore.collection("user_preferences")
    private val appSettingsCollection     = firestore.collection("app_settings")

    // ─────────────────────────────────────────────────────────────────────────
    // LOCAL PREFERENCES
    // ─────────────────────────────────────────────────────────────────────────

    fun setDarkMode(isDarkMode: Boolean)        = preferenceManager.setDarkMode(isDarkMode)
    fun isDarkMode(): Boolean                   = preferenceManager.isDarkMode()
    fun setLanguage(languageCode: String)       = preferenceManager.setLanguage(languageCode)
    fun getLanguage(): String                   = preferenceManager.getLanguage()
    fun setNotificationsEnabled(enabled: Boolean) = preferenceManager.setPushEnabled(enabled)
    fun areNotificationsEnabled(): Boolean      = preferenceManager.isPushEnabled()

    // ─────────────────────────────────────────────────────────────────────────
    // REMOTE USER PREFERENCES
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getUserPreferences(userId: String): Resource<UserPreferences> {
        if (userId.isBlank()) {
            return Resource.Error("Cannot fetch preferences: user ID is missing.")
        }

        return try {
            val snapshot = userPreferencesCollection.document(userId).get().await()

            if (!snapshot.exists()) {
                return Resource.Success(UserPreferences(userId = userId))
            }

            val data = snapshot.data
                ?: return Resource.Success(UserPreferences(userId = userId))

            // ✅ Manual parsing — missing fields automatically get Kotlin default values
            // toObject() was failing silently when fields like locationAccess/dataSharing
            // were absent from Firestore, returning null instead of the default value.
            val prefs = UserPreferences(
                userId               = data["userId"]               as? String  ?: userId,
                notifyBookingUpdates = data["notifyBookingUpdates"] as? Boolean ?: true,
                notifyMessages       = data["notifyMessages"]       as? Boolean ?: true,
                notifyPayments       = data["notifyPayments"]       as? Boolean ?: true,
                notifyPromotions     = data["notifyPromotions"]     as? Boolean ?: false,
                notifyAdminAlerts    = data["notifyAdminAlerts"]    as? Boolean ?: true,
                isProfilePublic      = data["isProfilePublic"]      as? Boolean ?: true,
                showPhoneNumber      = data["showPhoneNumber"]      as? Boolean ?: false,
                showEmail            = data["showEmail"]            as? Boolean ?: false,
                locationAccess       = data["locationAccess"]       as? Boolean ?: true,
                dataSharing          = data["dataSharing"]          as? Boolean ?: false,
                preferredLanguage    = data["preferredLanguage"]    as? String  ?: "en",
                isDarkMode           = data["isDarkMode"]           as? Boolean ?: false,
                updatedAt            = parseTimestamp(data["updatedAt"])
            )

            Resource.Success(prefs)

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user preferences")
        }
    }

    suspend fun saveUserPreferences(preferences: UserPreferences): Resource<Unit> {
        if (preferences.userId.isBlank()) {
            return Resource.Error("Cannot save preferences: user ID is missing.")
        }

        return try {
            val updatedPrefs = preferences.copy(updatedAt = Timestamp.now())

            userPreferencesCollection
                .document(updatedPrefs.userId)
                .set(updatedPrefs)
                .await()

            Resource.Success(Unit)

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save user preferences")
        }
    }

    suspend fun updateUserPreferences(userId: String, fields: Map<String, Any>): Resource<Unit> {
        if (userId.isBlank()) {
            return Resource.Error("Cannot update preferences: user ID is missing.")
        }

        return try {
            val updatedFields = fields.toMutableMap().apply {
                put("updatedAt", Timestamp.now())
            }

            userPreferencesCollection
                .document(userId)
                .set(updatedFields, SetOptions.merge())
                .await()

            Resource.Success(Unit)

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update user preferences")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GLOBAL APP SETTINGS
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getAppSettings(): Resource<AppSettings> {
        return try {
            val snapshot = appSettingsCollection.document("global").get().await()

            if (!snapshot.exists()) {
                return Resource.Success(AppSettings())
            }

            val data = snapshot.data ?: return Resource.Success(AppSettings())

            val updatedAt = parseTimestamp(data["updatedAt"]) ?: Timestamp.now()

            val settings = AppSettings(
                isMaintenanceMode     = data["isMaintenanceMode"]      as? Boolean ?: false,
                maintenanceMessage    = data["maintenanceMessage"]      as? String,
                minimumAppVersion     = data["minimumAppVersion"]       as? String  ?: "1.0.0",
                latestAppVersion      = data["latestAppVersion"]        as? String  ?: "1.0.0",
                forceUpdate           = data["forceUpdate"]             as? Boolean ?: false,
                platformFeePercent    = (data["platformFeePercent"]     as? Number)?.toDouble() ?: 5.0,
                maxPropertyImages     = (data["maxPropertyImages"]      as? Number)?.toInt()    ?: 10,
                maxBookingDaysAdvance = (data["maxBookingDaysAdvance"]  as? Number)?.toInt()    ?: 90,
                featuredPropertyIds   = (data["featuredPropertyIds"]    as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                announcementBanner    = data["announcementBanner"]      as? String,
                supportEmail          = data["supportEmail"]            as? String  ?: "support@havenhub.co.za",
                termsOfServiceUrl     = data["termsOfServiceUrl"]       as? String  ?: "https://havenhub.co.za/terms",
                privacyPolicyUrl      = data["privacyPolicyUrl"]        as? String  ?: "https://havenhub.co.za/privacy",
                updatedAt             = updatedAt
            )

            Resource.Success(settings)

        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch app settings")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseTimestamp(raw: Any?): Timestamp? = when (raw) {
        is Timestamp -> raw
        is Long      -> Timestamp(Date(raw))
        is Date      -> Timestamp(raw)
        else         -> null
    }
}