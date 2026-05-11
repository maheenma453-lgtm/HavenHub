package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

// ── Booking Status ────────────────────────────────────────────
enum class BookingStatus {
    PENDING,
    PENDING_APPROVAL,  // Payment ho gayi, landlord ne approve nahi kiya abhi
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED;

    fun displayName(): String = when (this) {
        PENDING          -> "Pending"
        PENDING_APPROVAL -> "Awaiting Approval"
        CONFIRMED        -> "Confirmed"
        CHECKED_IN       -> "Checked In"
        COMPLETED        -> "Completed"
        CANCELLED        -> "Cancelled"
    }
}

// ── Booking Data Class ────────────────────────────────────────
data class Booking(
    @DocumentId
    val bookingId          : String     = "",
    val tenantId           : String     = "",
    val tenantName         : String     = "",
    val tenantEmail        : String     = "", // ✅ FIX: Tenant ki actual email store karne ke liye
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
    // Firestore empty constructor
    constructor() : this(bookingId = "")

    // ── Computed getters (@Exclude = Firestore save nahi karega) ──

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

    // SRS BR-3: Tenant sirf PENDING booking cancel kar sakta hai
    // PENDING_APPROVAL aur CONFIRMED booking cancel nahi ho sakti
    @get:Exclude
    val isCancellable: Boolean
        get() = bookingStatus == BookingStatus.PENDING
}