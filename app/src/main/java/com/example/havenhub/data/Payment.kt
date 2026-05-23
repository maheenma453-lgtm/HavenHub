package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Payment(
    @DocumentId
    val paymentId : String = "",

    val bookingId : String = "",
    val payerId   : String = "",
    val payerName : String = "",
    val payeeId   : String = "",
    val payeeName : String = "",

    val amount         : String = "0",
    val platformFee    : String = "0",
    val landlordPayout : String = "0",
    val currency       : String = "PKR",

    val paymentMethod        : String = PaymentMethod.JAZZCASH.name,
    val gatewayTransactionId : String = "",
    val gatewayReference     : String = "",

    val type   : String = PaymentType.BOOKING.name,
    val status : String = PaymentStatus.PENDING.name,

    val originalPaymentId : String     = "",
    val refundReason      : String     = "",
    val refundedAt        : Timestamp? = null,

    @ServerTimestamp
    val createdAt : Timestamp? = null,
    val updatedAt : Timestamp? = null

) {
    constructor() : this(paymentId = "")

    val formattedAmount : String get() = "$currency $amount"
    val formattedPayout : String get() = "$currency $landlordPayout"

    val amountDouble         : Double get() = amount.toDoubleOrNull()         ?: 0.0
    val platformFeeDouble    : Double get() = platformFee.toDoubleOrNull()    ?: 0.0
    val landlordPayoutDouble : Double get() = landlordPayout.toDoubleOrNull() ?: 0.0

    val isSuccessful : Boolean get() = status == PaymentStatus.COMPLETED.name
    val isRefund     : Boolean get() = type   == PaymentType.REFUND.name

    val paymentStatusEnum : PaymentStatus
        get() = try { PaymentStatus.valueOf(status) }
        catch (e: Exception) { PaymentStatus.PENDING }

    val paymentMethodEnum : PaymentMethod
        get() = try { PaymentMethod.valueOf(paymentMethod) }
        catch (e: Exception) { PaymentMethod.JAZZCASH }

    val paymentTypeEnum : PaymentType
        get() = try { PaymentType.valueOf(type) }
        catch (e: Exception) { PaymentType.BOOKING }
}

enum class PaymentMethod {
    JAZZCASH, EASYPAISA, BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, CASH;

    fun displayName(): String = when (this) {
        JAZZCASH      -> "JazzCash"
        EASYPAISA     -> "EasyPaisa"
        BANK_TRANSFER -> "Bank Transfer"
        CREDIT_CARD   -> "Credit Card"
        DEBIT_CARD    -> "Debit Card"
        CASH          -> "Cash"
    }
}

enum class PaymentType {
    BOOKING, REFUND, PAYOUT
}

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    PAID,            // ✅ ADD: BookingDetailScreen ke liye
    DEPOSIT_PAID,    // ✅ ADD: Pre-booking deposit paid
    PARTIALLY_PAID;  // ✅ ADD: BookingDetailScreen ke liye

    fun displayName(): String = when (this) {
        PENDING            -> "Pending"
        PROCESSING         -> "Processing"
        COMPLETED          -> "Completed"
        FAILED             -> "Failed"
        REFUNDED           -> "Refunded"
        PARTIALLY_REFUNDED -> "Partially Refunded"
        PAID               -> "Paid"           // ✅ ADD
        DEPOSIT_PAID       -> "Deposit Paid"   // ✅ ADD
        PARTIALLY_PAID     -> "Partially Paid" // ✅ ADD
    }
}