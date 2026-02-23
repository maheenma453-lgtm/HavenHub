package com.example.havenhub.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a payment transaction stored in Firestore → `payments/{paymentId}`.
 *
 * Each [Booking] has exactly one associated Payment document.
 * Refunds create a new [PaymentType.REFUND] document referencing the
 * original via [originalPaymentId].
 *
 * Payment flow:
 *  PENDING → PROCESSING → COMPLETED
 *                       ↘ FAILED
 *  COMPLETED → REFUNDED (via separate refund document)
 */
data class Payment(

    @DocumentId
    val paymentId: String = "",

    // ── References ────────────────────────────────────────────────────────────

    val bookingId: String = "",

    /** UID of the tenant being charged. */
    val payerId: String = "",

    /** Payer display name (denormalized). */
    val payerName: String = "",

    /** UID of the landlord receiving the payout. */
    val payeeId: String = "",

    /** Payee display name (denormalized). */
    val payeeName: String = "",

    // ── Amounts ───────────────────────────────────────────────────────────────

    /** Total amount charged to the tenant in PKR. */
    val amount: Double = 0.0,

    /** HavenHub platform service fee deducted before landlord payout. */
    val platformFee: Double = 0.0,

    /** Net amount paid out to the landlord after [platformFee]. */
    val landlordPayout: Double = 0.0,

    /** Currency code (default: PKR). */
    val currency: String = "PKR",

    // ── Method & Gateway ──────────────────────────────────────────────────────

    val paymentMethod: PaymentMethod = PaymentMethod.JAZZCASH,

    /** Transaction ID returned by the payment gateway. */
    val gatewayTransactionId: String = "",

    /** Raw response / receipt reference from the gateway. */
    val gatewayReference: String = "",

    // ── Type & Status ─────────────────────────────────────────────────────────

    val type: PaymentType = PaymentType.BOOKING,

    val status: PaymentStatus = PaymentStatus.PENDING,

    // ── Refund ────────────────────────────────────────────────────────────────

    /** Set when this is a refund document; points to the original payment. */
    val originalPaymentId: String = "",

    val refundReason: String = "",

    val refundedAt: Timestamp? = null,

    // ── Timestamps ────────────────────────────────────────────────────────────

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    val updatedAt: Timestamp? = null

) {
    constructor() : this(paymentId = "")

    /** Formatted amount string for UI display. */
    val formattedAmount: String get() = "$currency ${"%,.0f".format(amount)}"

    /** Formatted payout string for landlord dashboard. */
    val formattedPayout: String get() = "$currency ${"%,.0f".format(landlordPayout)}"

    val isSuccessful: Boolean get() = status == PaymentStatus.COMPLETED
    val isRefund: Boolean get() = type == PaymentType.REFUND
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class PaymentMethod {
    JAZZCASH,
    EASYPAISA,
    BANK_TRANSFER,
    CREDIT_CARD,
    DEBIT_CARD,
    CASH;           // Cash on arrival; marked completed manually by landlord

    fun displayName(): String = when (this) {
        JAZZCASH       -> "JazzCash"
        EASYPAISA      -> "EasyPaisa"
        BANK_TRANSFER  -> "Bank Transfer"
        CREDIT_CARD    -> "Credit Card"
        DEBIT_CARD     -> "Debit Card"
        CASH           -> "Cash"
    }
}

enum class PaymentType {
    /** Normal booking payment from tenant. */
    BOOKING,

    /** Full or partial refund to tenant. */
    REFUND,

    /** Payout disbursement to landlord. */
    PAYOUT
}

enum class PaymentStatus {
    /** Awaiting gateway confirmation. */
    PENDING,

    /** Gateway call in progress. */
    PROCESSING,

    /** Transaction successful. */
    COMPLETED,

    /** Gateway returned an error. */
    FAILED,

    /** Full amount refunded to tenant. */
    REFUNDED,

    /** Partial amount refunded to tenant. */
    PARTIALLY_REFUNDED;

    fun displayName(): String = when (this) {
        PENDING             -> "Pending"
        PROCESSING          -> "Processing"
        COMPLETED           -> "Completed"
        FAILED              -> "Failed"
        REFUNDED            -> "Refunded"
        PARTIALLY_REFUNDED  -> "Partially Refunded"
    }
}