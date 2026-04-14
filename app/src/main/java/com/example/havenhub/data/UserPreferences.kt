package com.example.havenhub.data

import com.google.firebase.Timestamp

// UserPreferences.kt
// Model: Per-user notification, privacy, and display preferences.
// Synced across devices via Firestore.

/**
 * # UserPreferences
 *
 * Stores personalisation and notification preferences for an individual HavenHub user.
 * These preferences are synced to Firestore so they persist across multiple devices
 * (e.g., phone and tablet) and survive app reinstalls.
 *
 * Fast-access preferences (like dark mode) are also cached locally via
 * [PreferenceManager] / DataStore to be readable without a network call.
 *
 * ## Firestore Path
 * ```
 * user_preferences/{userId}
 * ```
 * One document per user, using their Firebase Auth UID as the document ID.
 *
 * ## Preference Categories
 * | Category       | Fields                                                    |
 * |----------------|-----------------------------------------------------------|
 * | Notifications  | bookings, messages, payments, promotions, adminAlerts     |
 * | Privacy        | isProfilePublic, showPhoneNumber, showEmail               |
 * | Display        | preferredLanguage, isDarkMode                             |
 *
 * ## Usage Example
 * ```kotlin
 * // Create with defaults (all notifications on, light mode, English)
 * val prefs = UserPreferences(userId = "uid_abc123")
 *
 * // Disable promotional notifications
 * val updated = prefs.copy(notifyPromotions = false, updatedAt = Timestamp.now())
 *
 * // Check if user wants booking alerts
 * if (prefs.notifyBookingUpdates) { /* send FCM */ }
 * ```
 *
 * @property userId               Firebase Auth UID. Used as the Firestore document ID.
 * @property notifyBookingUpdates Notify when a booking status changes (confirmed/cancelled).
 * @property notifyMessages       Notify when a new in-app chat message is received.
 * @property notifyPayments       Notify when a payment is made or received.
 * @property notifyPromotions     Notify about HavenHub promotions and offers.
 * @property notifyAdminAlerts    Notify about platform-wide admin announcements.
 * @property isProfilePublic      Whether the user's profile is visible to other users.
 * @property showPhoneNumber      Whether the phone number is displayed on listings/profile.
 * @property showEmail            Whether the email is displayed on public profile.
 * @property preferredLanguage    Preferred display language as an ISO 639-1 code.
 * @property isDarkMode           Whether the app should use dark theme for this user.
 * @property updatedAt            Firebase Timestamp of last update, used for sync conflict resolution.
 */
data class UserPreferences(

    /**
     * Firebase Auth UID of the user these preferences belong to.
     * Also used as the Firestore document ID for this record.
     */
    val userId: String = "",

    // ── Notification Preferences ─────────────────────────────────────────────

    /**
     * If true, send push notifications when a booking's status changes.
     * Covers: pending → confirmed, confirmed → cancelled, etc.
     */
    val notifyBookingUpdates: Boolean = true,

    /**
     * If true, send push notifications when a new chat message is received.
     * Disabling this prevents the message notification badge from appearing.
     */
    val notifyMessages: Boolean = true,

    /**
     * If true, send push notifications for payment events.
     * Covers: payment received, payment failed, refund processed.
     */
    val notifyPayments: Boolean = true,

    /**
     * If true, allow HavenHub to send promotional push notifications.
     * Defaults to false to respect user privacy and reduce notification fatigue.
     */
    val notifyPromotions: Boolean = false,

    /**
     * If true, send push notifications for platform-wide admin announcements.
     * Examples: maintenance windows, new feature releases, policy updates.
     */
    val notifyAdminAlerts: Boolean = true,

    // ── Privacy Preferences ──────────────────────────────────────────────────

    /**
     * If true, the user's profile (name, photo, bio) is visible to other users.
     */
    val isProfilePublic: Boolean = true,

    /**
     * If true, the user's phone number is visible on their profile and listings.
     */
    val showPhoneNumber: Boolean = false,

    /**
     * If true, the user's email address is visible on their public profile.
     * Defaults to false to protect against spam.
     */
    val showEmail: Boolean = false,

    // ── Display Preferences ──────────────────────────────────────────────────

    /**
     * User's preferred display language as an ISO 639-1 two-letter code.
     * Examples: "en" (English), "af" (Afrikaans), "zu" (Zulu), "xh" (Xhosa).
     */
    val preferredLanguage: String = "en",

    /**
     * If true, the app uses dark theme for this user.
     * Cached locally for immediate startup.
     */
    val isDarkMode: Boolean = false,

    /**
     * Firebase Timestamp of the last time these preferences were updated.
     * Null when the document is first created (Firestore sets it on write).
     * Use updatedAt?.toDate()?.time to get epoch millis if needed.
     *
     * ✅ FIX: Changed from Long to Timestamp? to prevent Firebase
     * deserialization crash: "Failed to convert Timestamp to long"
     */
    val updatedAt: Timestamp? = null

) {

    // ─────────────────────────────────────────────────────────────────────────
    // Computed Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the user has enabled at least one type of push notification.
     * Used to determine whether to register/refresh an FCM token for this user.
     */
    val hasAnyNotificationsEnabled: Boolean
        get() = notifyBookingUpdates || notifyMessages ||
                notifyPayments       || notifyPromotions || notifyAdminAlerts

    /**
     * Returns a map of notification channel IDs to their enabled state.
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
     * Returns a copy of these preferences with all notifications disabled.
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
     * Returns a copy of these preferences with all recommended notifications enabled.
     */
    fun withDefaultNotifications(): UserPreferences = copy(
        notifyBookingUpdates = true,
        notifyMessages       = true,
        notifyPayments       = true,
        notifyPromotions     = false,
        notifyAdminAlerts    = true,
        updatedAt            = Timestamp.now()
    )
}