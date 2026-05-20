package com.example.havenhub.data

import com.google.firebase.Timestamp

// ─────────────────────────────────────────────────────────────────────────────
// UserPreferences.kt
//
// Per-user personalisation, notification, and privacy preferences.
// Synced across devices via Firestore. Fast-access fields (dark mode) are also
// cached locally in SharedPreferences via PreferenceManager.
//
// Firestore path: user_preferences/{userId}
// One document per user — Firebase Auth UID is used as the document ID.
//
// Preference categories:
//   Notifications  — bookings, messages, payments, promotions, adminAlerts
//   Privacy        — isProfilePublic, showPhoneNumber, showEmail
//   Data           — locationAccess, dataSharing          ← NEW fields
//   Display        — preferredLanguage, isDarkMode
//
// CHANGE LOG:
//   Added locationAccess (default true)  — persists the Location Access toggle.
//   Added dataSharing    (default false) — persists the Data Sharing toggle.
//   Previously these two fields were NOT in this model, so the toggles in
//   PrivacySettingsScreen had nowhere to save their value — they always reset.
// ─────────────────────────────────────────────────────────────────────────────
data class UserPreferences(

    // ── Identity ──────────────────────────────────────────────────────────────
    /** Firebase Auth UID. Also used as the Firestore document ID for this record. */
    val userId: String = "",

    // ── Notification Preferences ──────────────────────────────────────────────

    /** Notify when a booking status changes (pending → confirmed, cancelled, etc.). */
    val notifyBookingUpdates: Boolean = true,

    /** Notify when a new in-app chat message is received. */
    val notifyMessages: Boolean = true,

    /** Notify for payment events: received, failed, refund processed. */
    val notifyPayments: Boolean = true,

    /**
     * Allow HavenHub to send promotional push notifications.
     * Default false — opt-in only, to reduce notification fatigue.
     */
    val notifyPromotions: Boolean = false,

    /** Notify about platform-wide admin announcements and policy updates. */
    val notifyAdminAlerts: Boolean = true,

    // ── Privacy Preferences ───────────────────────────────────────────────────

    /** If true, the user's profile (name, photo, bio) is visible to other users. */
    val isProfilePublic: Boolean = true,

    /**
     * If true, the user's phone number is visible on their profile and listings.
     * Default false — protect phone number unless explicitly shared.
     */
    val showPhoneNumber: Boolean = false,

    /**
     * If true, the user's email address is visible on their public profile.
     * Default false — protect email against spam by default.
     */
    val showEmail: Boolean = false,

    // ── Data & Permissions ────────────────────────────────────────────────────

    /**
     * If true, the app is allowed to use the device's GPS location.
     * Used for nearby property search and map features.
     * Default true — location is core to the HavenHub experience.
     *
     * ✅ NEW FIELD: Added so the Location Access toggle in PrivacySettingsScreen
     * has a real field to read from and write to. Previously this toggle had an
     * empty lambda '{}' and never saved anything.
     */
    val locationAccess: Boolean = true,

    /**
     * If true, the user consents to anonymous usage analytics being shared
     * with the HavenHub team to improve the app.
     * Default false — analytics sharing is opt-in only.
     *
     * ✅ NEW FIELD: Added so the Data Sharing toggle in PrivacySettingsScreen
     * has a real field to read from and write to. Previously this toggle had an
     * empty lambda '{}' and never saved anything.
     */
    val dataSharing: Boolean = false,

    // ── Display Preferences ───────────────────────────────────────────────────

    /**
     * User's preferred display language as an ISO 639-1 two-letter code.
     * Examples: "en" (English), "ur" (Urdu), "af" (Afrikaans).
     */
    val preferredLanguage: String = "en",

    /**
     * If true, the app uses dark theme for this user.
     * Also cached locally in SharedPreferences for instant startup.
     */
    val isDarkMode: Boolean = false,

    /**
     * Firebase Timestamp of the last time these preferences were updated.
     * Null when the document has never been written (brand-new user).
     * SettingsViewModel uses updatedAt == null to detect new users and
     * auto-create the Firestore document with default values.
     *
     * Type is Timestamp? (not Long) to prevent the Firebase deserialization
     * crash: "Failed to convert Timestamp to long".
     */
    val updatedAt: Timestamp? = null

) {

    // ─────────────────────────────────────────────────────────────────────────
    // Computed Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the user has at least one notification channel enabled.
     * Used to decide whether to register or refresh an FCM token.
     */
    val hasAnyNotificationsEnabled: Boolean
        get() = notifyBookingUpdates || notifyMessages ||
                notifyPayments       || notifyPromotions || notifyAdminAlerts

    /**
     * Returns a map of notification channel IDs to their enabled state.
     * Useful for batch-updating FCM topic subscriptions.
     */
    val notificationChannelStates: Map<String, Boolean>
        get() = mapOf(
            "booking_updates" to notifyBookingUpdates,
            "messages"        to notifyMessages,
            "payments"        to notifyPayments,
            "promotions"      to notifyPromotions,
            "admin_alerts"    to notifyAdminAlerts
        )

    /**
     * Returns a copy with all notification channels turned off.
     * Called by the master notifications toggle when set to false.
     */
    fun withAllNotificationsDisabled(): UserPreferences = copy(
        notifyBookingUpdates = false,
        notifyMessages       = false,
        notifyPayments       = false,
        notifyPromotions     = false,
        notifyAdminAlerts    = false,
        updatedAt            = Timestamp.now()
    )

    /**
     * Returns a copy with the recommended default notification channels on.
     * Called by the master notifications toggle when set to true.
     */
    fun withDefaultNotifications(): UserPreferences = copy(
        notifyBookingUpdates = true,
        notifyMessages       = true,
        notifyPayments       = true,
        notifyPromotions     = false,   // promotions stay opt-in even on master-enable
        notifyAdminAlerts    = true,
        updatedAt            = Timestamp.now()
    )
}