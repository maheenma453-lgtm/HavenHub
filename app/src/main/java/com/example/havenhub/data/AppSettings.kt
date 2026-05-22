package com.example.havenhub.data

import com.google.firebase.Timestamp

// ═══════════════════════════════════════════════════════════════════════════════
// AppSettings.kt
// Model: Global, admin-controlled platform configuration settings.
// Fetched from Firestore at app startup and cached for the session.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * # AppSettings
 *
 * Stores platform-wide configuration settings for HavenHub, managed exclusively
 * by administrators. These settings are fetched from Firestore at app startup.
 *
 * ## Firestore Path
 * ```
 * app_settings/global
 * ```
 * A single fixed document with ID "global".
 *
 * ## How Featured Properties Work
 * ─────────────────────────────────────────────────────────────────────────────
 * [featuredPropertyIds] is the admin-curated list of property Firestore document
 * IDs that should appear in the "Featured" section on HomeScreen.
 *
 * This works for BOTH property types:
 *   • Auto-ID properties  (e.g. "K9mXpQ2vRtUwYzAbCdEf") — landlord submissions
 *   • Manual-seed IDs     (e.g. "prop_001" … "prop_012") — seeded by admin
 *
 * The HomeViewModel compares each property's [Property.propertyId] against this
 * list to decide which properties are "featured", regardless of whether
 * [Property.isFeatured] is true in Firestore.
 *
 * Admin flow:
 *   1. Open Firebase Console → app_settings/global
 *   2. Add any property's document ID to the [featuredPropertyIds] array
 *   3. HomeViewModel picks it up on next data load — no app update needed
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * @property isMaintenanceMode       If true, display maintenance banner and disable bookings.
 * @property maintenanceMessage      Message shown during maintenance. Null if not in maintenance.
 * @property minimumAppVersion       Oldest allowed version string (e.g. "1.2.0").
 * @property latestAppVersion        Latest Play Store version (e.g. "2.1.0").
 * @property forceUpdate             If true, users below [minimumAppVersion] are hard-blocked.
 * @property platformFeePercent      HavenHub commission percentage per booking.
 * @property maxPropertyImages       Max images allowed per property listing.
 * @property maxBookingDaysAdvance   How many days ahead a tenant can book.
 * @property featuredPropertyIds     Admin-curated list of property IDs for the Featured section.
 *                                   Supports both auto-generated IDs and manual seed IDs.
 * @property announcementBanner      Optional banner text for HomeScreen. Null if none.
 * @property supportEmail            Contact email shown in Help & Support screen.
 * @property termsOfServiceUrl       URL to Terms of Service page.
 * @property privacyPolicyUrl        URL to Privacy Policy page.
 * @property updatedAt               Timestamp of last admin update.
 */
data class AppSettings(

    // ── Platform Health ──────────────────────────────────────────────────────

    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String? = null,

    // ── Version Control ──────────────────────────────────────────────────────

    val minimumAppVersion: String = "1.0.0",
    val latestAppVersion: String = "1.0.0",
    val forceUpdate: Boolean = false,

    // ── Business Rules ───────────────────────────────────────────────────────

    val platformFeePercent: Double = 5.0,
    val maxPropertyImages: Int = 10,
    val maxBookingDaysAdvance: Int = 90,

    // ── Content ──────────────────────────────────────────────────────────────

    /**
     * Admin-curated list of property Firestore document IDs shown in
     * the "Featured" section on HomeScreen.
     *
     * Works with any property ID format:
     *   - Auto-generated IDs: "K9mXpQ2vRtUwYzAbCdEf"
     *   - Manual seed IDs:    "prop_001", "prop_012"
     *
     * HomeViewModel uses this list to override the [Property.isFeatured] field,
     * so admin can feature any property without touching the property document.
     */
    val featuredPropertyIds: List<String> = emptyList(),

    val announcementBanner: String? = null,

    // ── Legal & Support Links ────────────────────────────────────────────────

    val supportEmail: String = "support@havenhub.co.za",
    val termsOfServiceUrl: String = "https://havenhub.co.za/terms",
    val privacyPolicyUrl: String = "https://havenhub.co.za/privacy",

    /**
     * ✅ FIX: Changed from Long to Timestamp? to prevent Firebase
     * deserialization crash: "Failed to convert Timestamp to long"
     */
    val updatedAt: Timestamp? = null

) {

    // ─────────────────────────────────────────────────────────────────────────
    // Computed Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if a soft "update available" prompt should be shown. */
    fun isUpdateAvailable(currentVersion: String): Boolean =
        compareVersions(currentVersion, latestAppVersion) < 0

    /** Returns true if the user must update before they can proceed. */
    fun shouldForceUpdate(currentVersion: String): Boolean =
        forceUpdate && compareVersions(currentVersion, minimumAppVersion) < 0

    /** Calculates the platform fee amount for a given booking total. */
    fun calculatePlatformFee(bookingTotal: Double): Double =
        bookingTotal * (platformFeePercent / 100.0)

    /** Calculates the net payout to the landlord after the platform fee is deducted. */
    fun calculateLandlordPayout(bookingTotal: Double): Double =
        bookingTotal - calculatePlatformFee(bookingTotal)

    /** Returns true if there are any admin-featured property IDs to display. */
    val hasFeaturedProperties: Boolean
        get() = featuredPropertyIds.isNotEmpty()

    /** Returns true if an announcement banner should be shown on HomeScreen. */
    val hasAnnouncementBanner: Boolean
        get() = !announcementBanner.isNullOrBlank()

    /**
     * Returns true if the given property ID is in the admin-curated featured list.
     * Use this in HomeViewModel to determine featured status from AppSettings.
     */
    fun isPropertyFeatured(propertyId: String): Boolean =
        featuredPropertyIds.contains(propertyId)

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compares two semantic version strings.
     * Returns negative if v1 < v2, zero if equal, positive if v1 > v2.
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