package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

// ── Booking Status ────────────────────────────────────────────
enum class BookingStatus {
    PENDING,
    PENDING_APPROVAL,
    CONFIRMED,
    DEPOSIT_PAID,           // ✅ ADD: Pre-booking deposit paid
    CHECKED_IN,
    AWAITING_FINAL_PAYMENT, // ✅ ADD: Pre-booking final payment pending
    COMPLETED,
    CANCELLED;

    fun displayName(): String = when (this) {
        PENDING                -> "Pending"
        PENDING_APPROVAL       -> "Awaiting Approval"
        CONFIRMED              -> "Confirmed"
        DEPOSIT_PAID           -> "Deposit Paid"          // ✅ ADD
        CHECKED_IN             -> "Checked In"
        AWAITING_FINAL_PAYMENT -> "Final Payment Pending" // ✅ ADD
        COMPLETED              -> "Completed"
        CANCELLED              -> "Cancelled"
    }
}

// ── Booking Data Class ────────────────────────────────────────
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
    val checkInDate        : Timestamp? = null,
    val checkOutDate       : Timestamp? = null,
    val totalNights        : Int        = 0,
    val guestCount         : Int        = 1,
    val pricePerNight      : Double     = 0.0,
    val subtotal           : Double     = 0.0,
    val serviceFee         : Double     = 0.0,
    val securityDeposit    : Double     = 0.0,
    val totalAmount        : Double     = 0.0,
    val depositAmount      : Double     = 0.0,  // ✅ ADD
    val remainingAmount    : Double     = 0.0,  // ✅ ADD
    val isPreBooking       : Boolean    = false, // ✅ ADD
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
    constructor() : this(bookingId = "")

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
    val formattedDeposit: String  // ✅ ADD
        get() = "PKR ${"%,.0f".format(depositAmount)}"

    @get:Exclude
    val formattedRemaining: String  // ✅ ADD
        get() = "PKR ${"%,.0f".format(remainingAmount)}"

    @get:Exclude
    val isCancellable: Boolean
        get() = bookingStatus == BookingStatus.PENDING
}