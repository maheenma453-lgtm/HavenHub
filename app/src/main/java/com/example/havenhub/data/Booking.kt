package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Booking(

    @DocumentId
    val bookingId: String = "",

    val tenantId: String = "",
    val tenantName: String = "",
    val landlordId: String = "",
    val landlordName: String = "",
    val propertyId: String = "",
    val propertyTitle: String = "",
    val propertyCoverUrl: String = "",
    val propertyAddress: String = "",
    val checkInDate: Timestamp? = null,
    val checkOutDate: Timestamp? = null,
    val totalNights: Int = 0,
    val guestCount: Int = 1,
    val pricePerNight: Double = 0.0,
    val subtotal: Double = 0.0,
    val serviceFee: Double = 0.0,
    val securityDeposit: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: BookingStatus = BookingStatus.PENDING,
    val hasReview: Boolean = false,
    val paymentId: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val cancellationReason: String = "",
    val cancelledBy: String = "",
    val cancelledAt: Timestamp? = null,

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null

) {
    constructor() : this(bookingId = "")

    val formattedTotal: String get() = "PKR ${"%,.0f".format(totalAmount)}"

    val isCancellable: Boolean
        get() = status in listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)

    val canReview: Boolean
        get() = status == BookingStatus.COMPLETED && !hasReview
}

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED;

    fun displayName(): String = when (this) {
        PENDING    -> "Pending"
        CONFIRMED  -> "Confirmed"
        CHECKED_IN -> "Checked In"
        COMPLETED  -> "Completed"
        CANCELLED  -> "Cancelled"
    }
}

// ✅ PaymentStatus hata di — already doosri file mein exist karti hai