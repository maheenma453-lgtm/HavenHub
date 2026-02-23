package com.example.havenhub.data
// ═══════════════════════════════════════════════════════════════════════════════
// AppSettings.kt
// Model: Global, admin-controlled platform configuration settings.
// Fetched from Firestore at app startup and cached for the session.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * # AppSettings
 *
 * Stores platform-wide configuration settings for HavenHub, managed exclusively
 * by administrators. These settings apply globally to all users and are fetched
 * from Firestore at app startup to enforce platform-wide rules in real time —
 * without requiring an app update.
 *
 * ## Firestore Path
 * ```
 * app_settings/global
 * ```
 * A **single fixed document** with the ID `"global"`. There is only ever one
 * AppSettings document — all users read from this same source.
 *
 * ## Access Control
 * - **Read**: All authenticated users (needed at startup).
 * - **Write**: Admin users only (enforced by Firestore Security Rules).
 *
 * ## Settings Categories
 * | Category        | Fields                                                         |
 * |-----------------|----------------------------------------------------------------|
 * | Platform Health | isMaintenanceMode, maintenanceMessage                          |
 * | Version Control | minimumAppVersion, latestAppVersion, forceUpdate               |
 * | Business Rules  | platformFeePercent, maxPropertyImages, maxBookingDaysAdvance   |
 * | Content         | featuredPropertyIds, announcementBanner                        |
 * | Legal Links     | termsOfServiceUrl, privacyPolicyUrl, supportEmail              |
 *
 * ## Usage Example
 * ```kotlin
 * // In SplashViewModel — fetched once and cached
 * val settings = settingsRepository.getAppSettings()
 *
 * // Check before allowing actions
 * if (settings.isMaintenanceMode) showMaintenanceBanner()
 * if (settings.shouldForceUpdate(currentVersion)) showForceUpdateDialog()
 *
 * // Use business rule values
 * val fee = booking.totalAmount * (settings.platformFeePercent / 100)
 * ```
 *
 * @property isMaintenanceMode       If true, display a maintenance banner and disable bookings.
 * @property maintenanceMessage      Message shown to users during maintenance. Null if not in maintenance.
 * @property minimumAppVersion       Oldest version string allowed to use the app (e.g., "1.2.0").
 * @property latestAppVersion        Most current version available on the Play Store (e.g., "2.1.0").
 * @property forceUpdate             If true, users on older versions than [minimumAppVersion] are blocked.
 * @property platformFeePercent      HavenHub's commission percentage deducted from each booking payment.
 * @property maxPropertyImages       Maximum number of images allowed per property listing.
 * @property maxBookingDaysAdvance   How far in advance (days) a tenant may book a property.
 * @property featuredPropertyIds     Admin-curated list of property IDs shown in the HomeScreen featured section.
 * @property announcementBanner      Optional banner text shown at the top of the HomeScreen. Null if none.
 * @property supportEmail            Contact email displayed in Help & Support screen.
 * @property termsOfServiceUrl       URL to the Terms of Service web page.
 * @property privacyPolicyUrl        URL to the Privacy Policy web page.
 * @property updatedAt               Epoch millis of the last admin update to this document.
 */
data class AppSettings(

    // ── Platform Health ──────────────────────────────────────────────────────

    /**
     * Global maintenance mode flag.
     * When true, the app displays a maintenance banner and disables
     * booking creation and payment flows. Read-only and search features
     * may remain accessible at the app's discretion.
     */
    val isMaintenanceMode: Boolean = false,

    /**
     * Message displayed to users during a maintenance window.
     * Example: "HavenHub is undergoing scheduled maintenance. We'll be back at 10:00 AM."
     * Null when maintenance mode is off.
     */
    val maintenanceMessage: String? = null,

    // ── Version Control ──────────────────────────────────────────────────────

    /**
     * The oldest version of the app permitted to use HavenHub's services.
     * Format: semantic versioning string "MAJOR.MINOR.PATCH" (e.g., "1.2.0").
     * Users on versions below this will see an update prompt.
     */
    val minimumAppVersion: String = "1.0.0",

    /**
     * The latest version of the app published on the Google Play Store.
     * Used to show a soft "update available" prompt to users
     * even when [forceUpdate] is false.
     */
    val latestAppVersion: String = "1.0.0",

    /**
     * If true, users running a version below [minimumAppVersion] are
     * hard-blocked and cannot use the app until they update.
     * If false, users see a dismissible suggestion to update.
     */
    val forceUpdate: Boolean = false,

    // ── Business Rules ───────────────────────────────────────────────────────

    /**
     * HavenHub's platform commission as a percentage of the booking total.
     * Example: 5.0 means 5% is deducted from each booking payment.
     * Used by [PaymentRepository] when calculating transaction amounts.
     */
    val platformFeePercent: Double = 5.0,

    /**
     * Maximum number of images a landlord may upload per property listing.
     * Enforced in [AddPropertyScreen] and [EditPropertyScreen].
     */
    val maxPropertyImages: Int = 10,

    /**
     * Maximum number of days in the future a tenant can book a property.
     * Example: 90 means bookings can be made up to 3 months ahead.
     * Used to restrict the date picker in [BookingScreen].
     */
    val maxBookingDaysAdvance: Int = 90,

    // ── Content ──────────────────────────────────────────────────────────────

    /**
     * Admin-curated list of property Firestore document IDs.
     * These properties are displayed in the "Featured" section on [HomeScreen].
     * Admins update this list to promote specific high-quality listings.
     */
    val featuredPropertyIds: List<String> = emptyList(),

    /**
     * Optional short announcement shown in a banner at the top of [HomeScreen].
     * Example: "🎉 New: Vacation rentals are now available!"
     * Null when no announcement is active.
     */
    val announcementBanner: String? = null,

    // ── Legal & Support Links ────────────────────────────────────────────────

    /**
     * Support email address displayed in [HelpAndSupportScreen].
     * Tapping this opens the device's email client.
     */
    val supportEmail: String = "support@havenhub.co.za",

    /**
     * Full URL to HavenHub's Terms of Service page.
     * Opened in a WebView or external browser from [AboutScreen].
     */
    val termsOfServiceUrl: String = "https://havenhub.co.za/terms",

    /**
     * Full URL to HavenHub's Privacy Policy page.
     * Opened in a WebView or external browser from [PrivacySettingsScreen].
     */
    val privacyPolicyUrl: String = "https://havenhub.co.za/privacy",

    /**
     * Epoch millis of the last time an admin modified this document.
     * Used for informational logging and cache invalidation.
     */
    val updatedAt: Long = System.currentTimeMillis()

) {

    // ─────────────────────────────────────────────────────────────────────────
    // Computed Helpers
    // Business logic derived from the raw settings values.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if a soft "update available" prompt should be shown.
     * Compares the current installed version against [latestAppVersion].
     *
     * @param currentVersion The version string of the currently installed app build.
     * @return true if a newer version is available on the Play Store.
     */
    fun isUpdateAvailable(currentVersion: String): Boolean {
        return compareVersions(currentVersion, latestAppVersion) < 0
    }

    /**
     * Returns true if the installed version is below [minimumAppVersion]
     * AND [forceUpdate] is enabled — meaning the user must update before continuing.
     *
     * @param currentVersion The version string of the currently installed app build.
     * @return true if the user must update before they can proceed.
     */
    fun shouldForceUpdate(currentVersion: String): Boolean {
        return forceUpdate && compareVersions(currentVersion, minimumAppVersion) < 0
    }

    /**
     * Calculates the platform fee amount for a given booking total.
     *
     * @param bookingTotal The gross booking amount in the local currency (ZAR).
     * @return The HavenHub commission amount to be deducted.
     */
    fun calculatePlatformFee(bookingTotal: Double): Double {
        return bookingTotal * (platformFeePercent / 100.0)
    }

    /**
     * Calculates the net payout to the landlord after the platform fee is deducted.
     *
     * @param bookingTotal The gross booking amount in the local currency (ZAR).
     * @return The net amount the landlord receives.
     */
    fun calculateLandlordPayout(bookingTotal: Double): Double {
        return bookingTotal - calculatePlatformFee(bookingTotal)
    }

    /** Returns true if there are any featured properties to display on HomeScreen. */
    val hasFeaturedProperties: Boolean
        get() = featuredPropertyIds.isNotEmpty()

    /** Returns true if an announcement banner should be shown on HomeScreen. */
    val hasAnnouncementBanner: Boolean
        get() = !announcementBanner.isNullOrBlank()

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compares two semantic version strings.
     * Returns negative if [v1] < [v2], zero if equal, positive if [v1] > [v2].
     *
     * Supports standard "MAJOR.MINOR.PATCH" format.
     *
     * @param v1 First version string (e.g., "1.2.3").
     * @param v2 Second version string (e.g., "2.0.0").
     * @return Negative, zero, or positive integer.
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}

