package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Notification(
    @DocumentId
    val notificationId: String = "",
    val recipientId: String = "",
    val targetRole: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = NotificationType.GENERAL.name,
    val referenceId: String = "",
    val isRead: Boolean = false,
    val isActive: Boolean = true,
    val adminNote: String = "",          // ✅ NEW: Admin ka note landlord ko dikhane ke liye
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val readAt: Timestamp? = null
) {
    constructor() : this(notificationId = "")

    val notificationType: NotificationType
        get() = try {
            NotificationType.valueOf(type)
        } catch (e: Exception) {
            NotificationType.GENERAL
        }
}

enum class NotificationType {
    BOOKING_REQUESTED, BOOKING_CONFIRMED, BOOKING_CANCELLED,
    BOOKING_COMPLETED, BOOKING_REMINDER, PAYMENT_RECEIVED,
    PAYMENT_FAILED, REFUND_ISSUED, NEW_REVIEW, REVIEW_REPLY,
    NEW_MESSAGE, PROPERTY_APPROVED, PROPERTY_REJECTED,
    ACCOUNT_VERIFIED, ACCOUNT_SUSPENDED, GENERAL;

    fun displayName(): String = name.replace("_", " ").lowercase().capitalize()
}