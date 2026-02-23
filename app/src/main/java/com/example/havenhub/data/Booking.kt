package com.example.havenhub.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a property booking stored in Firestore → `bookings/{bookingId}`.
 *
 * Lifecycle:
 *  PENDING → CONFIRMED → CHECKED_IN → COMPLETED
 *                      ↘ CANCELLED
 *
 * Key denormalized fields (e.g. [propertyTitle], [tenantName]) are stored
 * for efficient list rendering without extra queries.
 */
data class Booking(

    @DocumentId
    val bookingId: String = "",

    // ── Participants ──────────────────────────────────────────────────────────

    /** UID of the tenant who made the booking. */
    val tenantId: String = "",

    /** Tenant display name (denormalized). */
    val tenantName: String = "",

    /** UID of the property's landlord. */
    val landlordId: String = "",

    /** Landlord display name (denormalized). */
    val landlordName: String = "",

    // ── Property ──────────────────────────────────────────────────────────────

    val propertyId: String = "",

    /** Property title (denormalized). */
    val propertyTitle: String = "",

    /** Cover image URL (denormalized for list display). */
    val propertyCoverUrl: String = "",

    /** Property address (denormalized). */
    val propertyAddress: String = "",

    // ── Dates ─────────────────────────────────────────────────────────────────

    /** Inclusive check-in date stored as Firestore Timestamp. */
    val checkInDate: Timestamp? = null,

    /** Inclusive check-out date stored as Firestore Timestamp. */
    val checkOutDate: Timestamp? = null,

    /** Computed number of nights = checkOut − checkIn in days. */
    val totalNights: Int = 0,

    // ── Guests ───────────────────────────────────────────────────────────────

    val guestCount: Int = 1,

    // ── Pricing ───────────────────────────────────────────────────────────────

    /** Nightly rate at time of booking (snapshot, not live). */
    val pricePerNight: Double = 0.0,

    /** Base cost: [pricePerNight] × [totalNights]. */
    val subtotal: Double = 0.0,

    /** Platform service fee. */
    val serviceFee: Double = 0.0,

    /** Refundable security deposit. */
    val securityDeposit: Double = 0.0,

    /** Grand total charged to the tenant. */
    val totalAmount: Double = 0.0,

    // ── Status ────────────────────────────────────────────────────────────────

    val status: BookingStatus = BookingStatus.PENDING,

    /** Whether the tenant has left a review for this booking. */
    val hasReview: Boolean = false,

    // ── Payment ───────────────────────────────────────────────────────────────

    /** Reference to the associated [Payment] document. */
    val paymentId: String = "",

    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    // ── Cancellation ─────────────────────────────────────────────────────────

    val cancellationReason: String = "",

    val cancelledBy: String = "",   // userId who cancelled

    val cancelledAt: Timestamp? = null,

    // ── Timestamps ────────────────────────────────────────────────────────────

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    val updatedAt: Timestamp? = null

) {
    constructor() : this(bookingId = "")

    /** Formatted total for UI display. */
    val formattedTotal: String get() = "PKR ${"%,.0f".format(totalAmount)}"

    /** True when the booking can still be cancelled by the tenant. */
    val isCancellable: Boolean
        get() = status in listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)

    /** True when the tenant can submit a review. */
    val canReview: Boolean
        get() = status == BookingStatus.COMPLETED && !hasReview
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class BookingStatus {
    /** Tenant submitted; awaiting landlord confirmation. */
    PENDING,

    /** Landlord accepted the booking. */
    CONFIRMED,

    /** Tenant has checked in. */
    CHECKED_IN,

    /** Stay completed; eligible for review. */
    COMPLETED,

    /** Cancelled by tenant, landlord, or admin. */
    CANCELLED;

    fun displayName(): String = when (this) {
        PENDING    -> "Pending"
        CONFIRMED  -> "Confirmed"
        CHECKED_IN -> "Checked In"
        COMPLETED  -> "Completed"
        CANCELLED  -> "Cancelled"
    }
}

