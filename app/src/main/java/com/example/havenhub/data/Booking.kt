package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

// ─────────────────────────────────────────────────────────────────────────────
// BOOKING STATUS
// All possible states a booking can be in throughout its lifecycle.
// ─────────────────────────────────────────────────────────────────────────────
enum class BookingStatus {
    PENDING,
    PENDING_APPROVAL,
    CONFIRMED,
    DEPOSIT_PAID,           // Pre-booking: tenant paid 20% deposit
    CHECKED_IN,
    AWAITING_FINAL_PAYMENT, // Pre-booking: tenant needs to pay remaining 80% on arrival
    COMPLETED,
    CANCELLED;

    fun displayName(): String = when (this) {
        PENDING                -> "Pending"
        PENDING_APPROVAL       -> "Awaiting Approval"
        CONFIRMED              -> "Confirmed"
        DEPOSIT_PAID           -> "Deposit Paid"
        CHECKED_IN             -> "Checked In"
        AWAITING_FINAL_PAYMENT -> "Final Payment Pending"
        COMPLETED              -> "Completed"
        CANCELLED              -> "Cancelled"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOOKING DATA CLASS
//
// Represents a single booking document in Firestore.
//
// Key pre-booking fields:
//   isPreBooking    — true when tenant books in advance with deposit only
//   depositAmount   — 20% of total, paid upfront
//   remainingAmount — 80% of total, paid on arrival
//   checkInDate     — NOW set by tenant from date picker (not landlord)
//   checkOutDate    — NOW set by tenant from date picker (not landlord)
// ─────────────────────────────────────────────────────────────────────────────
data class Booking(
    @DocumentId
    val bookingId          : String     = "",
    val tenantId           : String     = "",
    val tenantName         : String     = "",
    val tenantEmail        : String     = "",
    val landlordId         : String     = "",
    val landlordName       : String     = "",
    val propertyId         : String     = "",
    val propertyTitle      : String     = "",
    val propertyCoverUrl   : String     = "",
    val propertyAddress    : String     = "",

    // Tenant-selected check-in and check-out dates
    // These are set directly by the tenant in PreBookingScreen.
    // Landlord no longer needs to confirm dates separately.
    val checkInDate        : Timestamp? = null,
    val checkOutDate       : Timestamp? = null,

    val totalNights        : Int        = 0,
    val guestCount         : Int        = 1,
    val pricePerNight      : Double     = 0.0,
    val subtotal           : Double     = 0.0,
    val serviceFee         : Double     = 0.0,
    val securityDeposit    : Double     = 0.0,
    val totalAmount        : Double     = 0.0,

    // Pre-booking payment split
    val depositAmount      : Double     = 0.0, // 20% paid now
    val remainingAmount    : Double     = 0.0, // 80% paid on arrival

    // Whether this booking was made via pre-booking (advance deposit) flow
    val isPreBooking       : Boolean    = false,

    val status             : String     = BookingStatus.PENDING.name,
    val hasReview          : Boolean    = false,
    val paymentId          : String     = "",
    val paymentMethod      : String     = "",
    val paymentStatus      : String     = PaymentStatus.PENDING.name,
    val cancellationReason : String     = "",
    val cancelledBy        : String     = "",
    val cancelledAt        : Timestamp? = null,

    @ServerTimestamp
    val createdAt          : Timestamp? = null,
    val updatedAt          : Timestamp? = null
) {
    // No-argument constructor required by Firestore deserialization
    constructor() : this(bookingId = "")

    // ── Computed properties (excluded from Firestore serialization) ───────────

    @get:Exclude
    val bookingStatus: BookingStatus
        get() = try {
            BookingStatus.valueOf(status)
        } catch (_: Exception) {
            BookingStatus.PENDING
        }

    @get:Exclude
    val paymentStatusEnum: PaymentStatus
        get() = try {
            PaymentStatus.valueOf(paymentStatus)
        } catch (_: Exception) {
            PaymentStatus.PENDING
        }

    @get:Exclude
    val formattedTotal: String
        get() = "PKR ${"%,.0f".format(totalAmount)}"

    @get:Exclude
    val formattedDeposit: String
        get() = "PKR ${"%,.0f".format(depositAmount)}"

    @get:Exclude
    val formattedRemaining: String
        get() = "PKR ${"%,.0f".format(remainingAmount)}"

    // Booking can only be cancelled while it is still pending approval
    @get:Exclude
    val isCancellable: Boolean
        get() = bookingStatus == BookingStatus.PENDING ||
                bookingStatus == BookingStatus.PENDING_APPROVAL
}
