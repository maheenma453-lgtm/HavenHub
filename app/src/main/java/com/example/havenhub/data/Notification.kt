package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// ══════════════════════════════════════════════════════════════════════════════
// Notification — Firestore Data Model
//
// Represents a single in-app notification stored in the "notifications" collection.
// Each notification is tied to one recipient (recipientId) and one event type.
//
// IMPORTANT: createdAt uses NO @ServerTimestamp annotation — it is set manually
// with Timestamp.now() in NotificationRepository.sendNotification().
// Reason: @ServerTimestamp caused null values during object mapping, which broke
// orderBy("createdAt") queries in Firestore.
// ══════════════════════════════════════════════════════════════════════════════
data class Notification(
    @DocumentId
    val notificationId: String     = "",       // Firestore document ID (auto-set by @DocumentId)
    val recipientId   : String     = "",       // UID of the user who receives this notification
    val targetRole    : String     = "",       // "tenant", "landlord", "admin", or "all"
    val title         : String     = "",       // Short headline shown in notification list
    val body          : String     = "",       // Full message text
    val type          : String     = NotificationType.GENERAL.name, // NotificationType enum as string
    val referenceId   : String     = "",       // Related document ID (bookingId, propertyId, etc.)
    val isRead        : Boolean    = false,    // Whether the user has opened/read this notification
    val isActive      : Boolean    = true,     // Soft-delete flag — false means hidden from UI
    val adminNote     : String     = "",       // Optional note from admin (used in verification flows)
    val createdAt     : Timestamp? = null,     // Manually set — do NOT use @ServerTimestamp here
    val readAt        : Timestamp? = null      // Timestamp when the user marked it as read
) {
    // No-arg constructor required by Firestore's toObject() deserialization
    constructor() : this(notificationId = "")

    // Convenience getter — converts the raw type string back to the enum safely.
    // Falls back to GENERAL if the stored string doesn't match any enum value.
    val notificationType: NotificationType
        get() = try {
            NotificationType.valueOf(type)
        } catch (e: Exception) {
            NotificationType.GENERAL
        }
}

// ══════════════════════════════════════════════════════════════════════════════
// NotificationType — Enum
//
// All possible notification event types across the app.
// Stored as strings in Firestore (using .name) to keep it human-readable.
// ══════════════════════════════════════════════════════════════════════════════
enum class NotificationType {

    // ── Booking lifecycle ─────────────────────────────────────────────────────
    BOOKING_REQUESTED,      // Tenant sent a booking request
    BOOKING_CONFIRMED,      // Landlord confirmed the booking
    BOOKING_CANCELLED,      // Booking was cancelled (by tenant, landlord, or admin)
    BOOKING_COMPLETED,      // Stay ended — tenant prompted to leave a review
    BOOKING_REMINDER,       // Upcoming check-in reminder (future use)

    // ── Payments ──────────────────────────────────────────────────────────────
    PAYMENT_RECEIVED,       // Payment successfully processed
    PAYMENT_FAILED,         // Payment attempt failed
    REFUND_ISSUED,          // Refund has been issued to the tenant

    // ── Reviews ───────────────────────────────────────────────────────────────
    NEW_REVIEW,             // Tenant left a review on a property
    REVIEW_REPLY,           // Landlord replied to a review

    // ── Messaging ─────────────────────────────────────────────────────────────
    NEW_MESSAGE,            // New chat message received

    // ── Property verification ─────────────────────────────────────────────────
    PROPERTY_APPROVED,      // Admin approved a property listing
    PROPERTY_REJECTED,      // Admin rejected a property listing
    PROPERTY_PENDING,       // New property submitted — admin needs to review

    // ── User verification ─────────────────────────────────────────────────────
    USER_VERIFIED,                  // Admin approved user's identity verification
    USER_REJECTED,                  // Admin rejected user's verification request
    USER_VERIFICATION_PENDING,      // User submitted docs — admin needs to review

    // ── Account status ────────────────────────────────────────────────────────
    ACCOUNT_VERIFIED,       // Account fully activated (post-email or admin verify)
    ACCOUNT_SUSPENDED,      // Account has been suspended by admin

    // ── General / Fallback ────────────────────────────────────────────────────
    GENERAL;                // Default type for misc or unclassified notifications

    // Converts enum name to a readable display string.
    // Example: BOOKING_CONFIRMED → "Booking confirmed"
    fun displayName(): String =
        name.replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
}