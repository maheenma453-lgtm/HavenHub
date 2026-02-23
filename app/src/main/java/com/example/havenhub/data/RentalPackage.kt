package com.example.havenhub.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a rental deal or package stored in Firestore →
 * `rentalPackages/{packageId}`.
 *
 * Landlords can create time-limited packages (e.g. "Summer Deal",
 * "Weekend Getaway") that offer a discount over the base nightly rate.
 * Packages are linked to one property and optionally restricted to
 * a specific date range or minimum stay.
 *
 * Package status: [PackageStatus]
 * Duration type: [PackageDuration]
 */
data class RentalPackage(

    @DocumentId
    val packageId: String = "",

    // ── Ownership ─────────────────────────────────────────────────────────────

    val propertyId: String = "",

    /** Property title (denormalized for list display). */
    val propertyTitle: String = "",

    /** UID of the landlord who created this package. */
    val landlordId: String = "",

    // ── Content ───────────────────────────────────────────────────────────────

    val packageName: String = "",

    val description: String = "",

    /** Tag displayed on the property card (e.g. "🔥 Summer Deal"). */
    val badgeLabel: String = "",

    // ── Duration ──────────────────────────────────────────────────────────────

    val durationType: PackageDuration = PackageDuration.FLEXIBLE,

    /** Fixed number of nights for FIXED_NIGHTS type packages. */
    val fixedNights: Int? = null,

    /** Minimum nights required to use this package. */
    val minNights: Int = 1,

    /** Maximum nights allowed under this package. */
    val maxNights: Int? = null,

    // ── Pricing ───────────────────────────────────────────────────────────────

    /** Discounted nightly rate offered by this package. */
    val discountedPricePerNight: Double = 0.0,

    /** Original base nightly rate (for showing the strikethrough price). */
    val originalPricePerNight: Double = 0.0,

    /** Flat discount amount in PKR (alternative to percentage). */
    val flatDiscount: Double? = null,

    /** Percentage discount (0–100). */
    val discountPercentage: Float? = null,

    /** Any additional included extras (e.g. "Free breakfast", "Airport pickup"). */
    val inclusions: List<String> = emptyList(),

    // ── Availability Window ───────────────────────────────────────────────────

    /** Package is only valid for check-ins after this date. */
    val availableFrom: Timestamp? = null,

    /** Package expires after this date. */
    val availableTo: Timestamp? = null,

    /** Specific blackout dates when the package cannot be used. */
    val blackoutDates: List<Timestamp> = emptyList(),

    // ── Limits ────────────────────────────────────────────────────────────────

    /** Maximum number of times this package can be booked in total. */
    val totalSlots: Int? = null,

    /** Number of bookings already made using this package. */
    val bookedSlots: Int = 0,

    // ── Status ────────────────────────────────────────────────────────────────

    val status: PackageStatus = PackageStatus.ACTIVE,

    // ── Timestamps ────────────────────────────────────────────────────────────

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    val updatedAt: Timestamp? = null

) {
    constructor() : this(packageId = "")

    /** True when this package can still be applied to a new booking. */
    val isAvailable: Boolean
        get() = status == PackageStatus.ACTIVE &&
                (totalSlots == null || bookedSlots < (totalSlots ?: 0))

    /** Remaining slots; null means unlimited. */
    val remainingSlots: Int?
        get() = totalSlots?.let { it - bookedSlots }

    /** Formatted discounted price for display. */
    val formattedDiscountedPrice: String
        get() = "PKR ${"%,.0f".format(discountedPricePerNight)}"

    /** Formatted original price for strikethrough display. */
    val formattedOriginalPrice: String
        get() = "PKR ${"%,.0f".format(originalPricePerNight)}"

    /** Human-readable savings label (e.g. "Save 20%"). */
    val savingsLabel: String
        get() = when {
            discountPercentage != null -> "Save ${discountPercentage.toInt()}%"
            flatDiscount != null       -> "Save PKR ${"%,.0f".format(flatDiscount)}"
            originalPricePerNight > 0  -> {
                val pct = ((originalPricePerNight - discountedPricePerNight) / originalPricePerNight * 100).toInt()
                "Save $pct%"
            }
            else -> ""
        }
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class PackageDuration {
    /** Tenant can choose any number of nights within [minNights]..[maxNights]. */
    FLEXIBLE,

    /** Package is exactly [fixedNights] nights. */
    FIXED_NIGHTS,

    /** Always covers a full calendar week (7 nights). */
    WEEKLY,

    /** Always covers a full calendar month (~30 nights). */
    MONTHLY;

    fun displayName(): String = when (this) {
        FLEXIBLE     -> "Flexible"
        FIXED_NIGHTS -> "Fixed Nights"
        WEEKLY       -> "Weekly"
        MONTHLY      -> "Monthly"
    }
}

enum class PackageStatus {
    /** Package is live and available to tenants. */
    ACTIVE,

    /** Landlord has temporarily paused the package. */
    PAUSED,

    /** All [totalSlots] have been used. */
    SOLD_OUT,

    /** Package date window has passed. */
    EXPIRED;

    fun displayName(): String = when (this) {
        ACTIVE   -> "Active"
        PAUSED   -> "Paused"
        SOLD_OUT -> "Sold Out"
        EXPIRED  -> "Expired"
    }
}
