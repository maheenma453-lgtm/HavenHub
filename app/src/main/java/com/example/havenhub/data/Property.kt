package com.example.havenhub.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a rental property stored in Firestore → `properties/{propertyId}`.
 *
 * A property is created by a [UserRole.LANDLORD] and goes through
 * an admin verification flow before becoming publicly visible.
 *
 * Property types: [PropertyType]
 * Listing status: [PropertyStatus]
 */
data class Property(

    @DocumentId
    val propertyId: String = "",

    /** UID of the landlord who owns this property. */
    val ownerId: String = "",

    /** Display name of the owner (denormalized for list performance). */
    val ownerName: String = "",

    val title: String = "",

    val description: String = "",

    val propertyType: PropertyType = PropertyType.APARTMENT,

    /** Admin review status of this listing. */
    val status: PropertyStatus = PropertyStatus.PENDING,

    // ── Location ──────────────────────────────────────────────────────────────

    val location: Location = Location(),

    /** Full human-readable address. */
    val address: String = "",

    /** City name for search filtering. */
    val city: String = "",

    // ── Pricing ───────────────────────────────────────────────────────────────

    /** Base nightly rate in PKR. */
    val pricePerNight: Double = 0.0,

    /** Optional weekly rate (if lower than 7 × nightly). */
    val pricePerWeek: Double? = null,

    /** Optional monthly rate. */
    val pricePerMonth: Double? = null,

    /** Refundable security deposit in PKR. */
    val securityDeposit: Double = 0.0,

    // ── Details ───────────────────────────────────────────────────────────────

    val bedrooms: Int = 1,

    val bathrooms: Int = 1,

    /** Maximum number of guests allowed. */
    val maxGuests: Int = 2,

    /** Total area in square feet. */
    val areaSqFt: Double? = null,

    /** Floor number (for apartments). */
    val floor: Int? = null,

    // ── Media ─────────────────────────────────────────────────────────────────

    /** Ordered list of Firebase Storage URLs; first item is the cover photo. */
    val imageUrls: List<String> = emptyList(),

    // ── Amenities ─────────────────────────────────────────────────────────────

    /** Free-form list of amenity labels (e.g. "WiFi", "Parking", "AC"). */
    val amenities: List<String> = emptyList(),

    // ── Rules ─────────────────────────────────────────────────────────────────

    val petsAllowed: Boolean = false,
    val smokingAllowed: Boolean = false,
    val partiesAllowed: Boolean = false,

    /** Earliest check-in time (e.g. "14:00"). */
    val checkInTime: String = "14:00",

    /** Latest check-out time (e.g. "11:00"). */
    val checkOutTime: String = "11:00",

    /** Minimum booking duration in nights. */
    val minNights: Int = 1,

    // ── Ratings ───────────────────────────────────────────────────────────────

    val averageRating: Float = 0f,

    val reviewCount: Int = 0,

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** Admin notes added during the verification process. */
    val adminNote: String = "",

    val isAvailable: Boolean = true,

    val isFeatured: Boolean = false,

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    val updatedAt: Timestamp? = null

) {
    constructor() : this(propertyId = "")

    /** Convenience getter for the cover / thumbnail image URL. */
    val coverImageUrl: String get() = imageUrls.firstOrNull() ?: ""

    /** Formatted price string for UI display. */
    val formattedPrice: String get() = "PKR ${"%,.0f".format(pricePerNight)}"

    /** True when this listing is live and bookable. */
    val isLive: Boolean get() = status == PropertyStatus.APPROVED && isAvailable
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class PropertyType {
    APARTMENT,
    HOUSE,
    VILLA,
    STUDIO,
    ROOM,
    HOSTEL,
    PENTHOUSE,
    FARMHOUSE;

    fun displayName(): String = when (this) {
        APARTMENT  -> "Apartment"
        HOUSE      -> "House"
        VILLA      -> "Villa"
        STUDIO     -> "Studio"
        ROOM       -> "Room"
        HOSTEL     -> "Hostel"
        PENTHOUSE  -> "Penthouse"
        FARMHOUSE  -> "Farmhouse"
    }
}

enum class PropertyStatus {
    /** Newly submitted; awaiting admin review. */
    PENDING,

    /** Currently under admin review. */
    UNDER_REVIEW,

    /** Approved and visible to tenants. */
    APPROVED,

    /** Rejected by admin; landlord can edit and resubmit. */
    REJECTED,

    /** Landlord has temporarily hidden the listing. */
    INACTIVE;

    fun displayName(): String = when (this) {
        PENDING      -> "Pending"
        UNDER_REVIEW -> "Under Review"
        APPROVED     -> "Approved"
        REJECTED     -> "Rejected"
        INACTIVE     -> "Inactive"
    }
}

