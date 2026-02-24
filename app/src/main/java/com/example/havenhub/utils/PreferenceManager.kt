package com.example.havenhub.utils
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("havenhub_prefs", Context.MODE_PRIVATE)

    // ── User Session ──────────────────────────────────────────────────
    fun saveUserId(userId: String) = prefs.edit().putString(Constants.PREF_USER_ID, userId).apply()
    fun getUserId(): String = prefs.getString(Constants.PREF_USER_ID, "") ?: ""

    fun saveUserRole(role: String) = prefs.edit().putString(Constants.PREF_USER_ROLE, role).apply()
    fun getUserRole(): String = prefs.getString(Constants.PREF_USER_ROLE, "") ?: ""

    fun saveUserName(name: String) = prefs.edit().putString(Constants.PREF_NAME, name).apply()
    fun getUserName(): String = prefs.getString(Constants.PREF_NAME, "") ?: ""

    fun setLoggedIn(isLoggedIn: Boolean) = prefs.edit().putBoolean(Constants.PREF_IS_LOGGED_IN, isLoggedIn).apply()
    fun isLoggedIn(): Boolean = prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false)

    // ── Onboarding ────────────────────────────────────────────────────
    fun setOnboardingDone(done: Boolean) = prefs.edit().putBoolean(Constants.PREF_ONBOARDING_DONE, done).apply()
    fun isOnboardingDone(): Boolean = prefs.getBoolean(Constants.PREF_ONBOARDING_DONE, false)

    // ── Theme ─────────────────────────────────────────────────────────
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean(Constants.PREF_DARK_MODE, enabled).apply()
    fun isDarkMode(): Boolean = prefs.getBoolean(Constants.PREF_DARK_MODE, false)

    // ── Notifications ─────────────────────────────────────────────────
    fun setPushEnabled(enabled: Boolean) = prefs.edit().putBoolean(Constants.PREF_PUSH_ENABLED, enabled).apply()
    fun isPushEnabled(): Boolean = prefs.getBoolean(Constants.PREF_PUSH_ENABLED, true)

    // ── Language ──────────────────────────────────────────────────────
    fun setLanguage(language: String) = prefs.edit().putString(Constants.PREF_LANGUAGE, language).apply()
    fun getLanguage(): String = prefs.getString(Constants.PREF_LANGUAGE, "en") ?: "en"

    // ── Clear session (Logout) ────────────────────────────────────────
    fun clearSession() {
        prefs.edit()
            .remove(Constants.PREF_USER_ID)
            .remove(Constants.PREF_USER_ROLE)
            .remove(Constants.PREF_NAME)
            .remove(Constants.PREF_IS_LOGGED_IN)
            .apply()
    }

    fun clearAll() = prefs.edit().clear().apply()
}