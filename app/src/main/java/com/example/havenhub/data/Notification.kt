package com.example.havenhub.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents an in-app notification stored in Firestore →
 * `users/{userId}/notifications/{notificationId}`.
 *
 * Notifications are created by Cloud Functions in response to platform
 * events (new booking, payment received, review posted, etc.) and
 * delivered via FCM push + stored here for the in-app inbox.
 *
 * Use [NotificationType] to determine the deep-link destination when
 * the user taps the notification.
 */
data class Notification(

    @DocumentId
    val notificationId: String = "",

    /** UID of the user who should receive this notification. */
    val recipientId: String = "",

    // ── Content ───────────────────────────────────────────────────────────────

    val title: String = "",

    val body: String = "",

    /** Category that drives routing and icon selection. */
    val type: NotificationType = NotificationType.GENERAL,

    // ── Deep Link ─────────────────────────────────────────────────────────────

    /**
     * Optional ID used for navigation when the notification is tapped.
     * Interpretation depends on [type]:
     *  - BOOKING_*   → bookingId
     *  - PAYMENT_*   → paymentId
     *  - NEW_REVIEW  → reviewId
     *  - MESSAGE     → conversationId
     *  - PROPERTY_*  → propertyId
     */
    val referenceId: String = "",

    // ── State ─────────────────────────────────────────────────────────────────

    /** False = unread (bold / highlighted in the inbox). */
    val isRead: Boolean = false,

    /** False = archived / soft-deleted by the user. */
    val isActive: Boolean = true,

    // ── Timestamps ────────────────────────────────────────────────────────────

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    val readAt: Timestamp? = null

) {
    constructor() : this(notificationId = "")
}

// ── Enum ──────────────────────────────────────────────────────────────────────

enum class NotificationType {
    // Booking events
    BOOKING_REQUESTED,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_COMPLETED,
    BOOKING_REMINDER,

    // Payment events
    PAYMENT_RECEIVED,
    PAYMENT_FAILED,
    REFUND_ISSUED,

    // Review events
    NEW_REVIEW,
    REVIEW_REPLY,

    // Messaging
    NEW_MESSAGE,

    // Property events
    PROPERTY_APPROVED,
    PROPERTY_REJECTED,

    // Account / admin
    ACCOUNT_VERIFIED,
    ACCOUNT_SUSPENDED,
    GENERAL;

    fun displayName(): String = when (this) {
        BOOKING_REQUESTED  -> "Booking Request"
        BOOKING_CONFIRMED  -> "Booking Confirmed"
        BOOKING_CANCELLED  -> "Booking Cancelled"
        BOOKING_COMPLETED  -> "Stay Completed"
        BOOKING_REMINDER   -> "Check-in Reminder"
        PAYMENT_RECEIVED   -> "Payment Received"
        PAYMENT_FAILED     -> "Payment Failed"
        REFUND_ISSUED      -> "Refund Issued"
        NEW_REVIEW         -> "New Review"
        REVIEW_REPLY       -> "Review Reply"
        NEW_MESSAGE        -> "New Message"
        PROPERTY_APPROVED  -> "Property Approved"
        PROPERTY_REJECTED  -> "Property Rejected"
        ACCOUNT_VERIFIED   -> "Account Verified"
        ACCOUNT_SUSPENDED  -> "Account Suspended"
        GENERAL            -> "Notification"
    }
}

