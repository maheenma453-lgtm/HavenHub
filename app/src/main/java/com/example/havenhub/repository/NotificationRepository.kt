package com.example.havenhub.repository

import com.example.havenhub.data.Notification
import com.example.havenhub.data.NotificationType
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore        : FirebaseFirestore,
    private val realtimeListener : FirebaseRealtimeListener
) {

    private val col = firestore.collection("notifications")

    // ── OBSERVE (Real-time Flow) ───────────────────────────────────────────────
    // Listens to live notification updates for a given user via Firestore snapshot listener.
    fun observeNotifications(userId: String): Flow<List<Notification>> =
        realtimeListener.listenToNotifications(userId)

    // ── FETCH ─────────────────────────────────────────────────────────────────
    // One-time fetch of all notifications for a user, ordered newest first.
    suspend fun getUserNotifications(userId: String): Resource<List<Notification>> {
        return try {
            val snapshot = col
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Notification::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch notifications")
        }
    }

    // ── MARK AS READ ──────────────────────────────────────────────────────────
    // Marks a single notification as read by its ID.
    suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            col.document(notificationId).update("isRead", true).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark as read")
        }
    }

    // ── MARK ALL AS READ ──────────────────────────────────────────────────────
    // Batch-updates all unread notifications for a user to isRead = true.
    suspend fun markAllAsRead(userId: String): Resource<Unit> {
        return try {
            val unread = col
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("isRead", false)
                .get().await()
            val batch = firestore.batch()
            unread.documents.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark all as read")
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    // Permanently deletes a single notification document from Firestore.
    suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            col.document(notificationId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete notification")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CORE SENDER
    //
    // All public send* functions below delegate to this single method.
    // createdAt is set manually with Timestamp.now() — @ServerTimestamp is
    // intentionally NOT used because it caused null values in Firestore,
    // which broke orderBy("createdAt") queries.
    // ══════════════════════════════════════════════════════════════════════════
    suspend fun sendNotification(
        recipientId : String,
        type        : NotificationType,
        title       : String,
        body        : String,
        referenceId : String = "",
        adminNote   : String = "",
        targetRole  : String = "tenant"
    ): Resource<Unit> {
        return try {
            val docRef = col.document()
            val notification = Notification(
                notificationId = docRef.id,
                recipientId    = recipientId,
                targetRole     = targetRole,
                title          = title,
                body           = body,
                type           = type.name,
                referenceId    = referenceId,
                isRead         = false,
                isActive       = true,
                adminNote      = adminNote,
                createdAt      = Timestamp.now()
            )
            docRef.set(notification).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send notification")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROPERTY NOTIFICATIONS
    // Sent to landlords when admin approves/rejects their property,
    // and to admin when a new property is submitted for review.
    // ══════════════════════════════════════════════════════════════════════════

    // Notifies the property owner that their listing was approved.
    suspend fun sendPropertyApprovedNotification(
        ownerId      : String,
        propertyId   : String,
        propertyTitle: String,
        adminNote    : String = ""
    ): Resource<Unit> = sendNotification(
        recipientId = ownerId,
        type        = NotificationType.PROPERTY_APPROVED,
        title       = "Property Approved ✓",
        body        = if (adminNote.isNotEmpty())
            "Your property \"$propertyTitle\" has been approved! Admin note: $adminNote"
        else
            "Congratulations! Your property \"$propertyTitle\" has been approved.",
        referenceId = propertyId,
        adminNote   = adminNote,
        targetRole  = "landlord"
    )

    // Notifies the property owner that their listing was rejected, with optional reason.
    suspend fun sendPropertyRejectedNotification(
        ownerId      : String,
        propertyId   : String,
        propertyTitle: String,
        adminNote    : String = ""
    ): Resource<Unit> = sendNotification(
        recipientId = ownerId,
        type        = NotificationType.PROPERTY_REJECTED,
        title       = "Property Rejected ✗",
        body        = if (adminNote.isNotEmpty())
            "Your property \"$propertyTitle\" was not approved. Reason: $adminNote"
        else
            "Your property \"$propertyTitle\" was not approved. Please contact support.",
        referenceId = propertyId,
        adminNote   = adminNote,
        targetRole  = "landlord"
    )

    // Alerts admin that a new property has been submitted and needs review.
    suspend fun sendNewPropertyPendingNotification(
        adminId      : String,
        propertyId   : String,
        propertyTitle: String,
        landlordName : String
    ): Resource<Unit> = sendNotification(
        recipientId = adminId,
        type        = NotificationType.PROPERTY_PENDING,
        title       = "New Property Pending Review",
        body        = "$landlordName has submitted \"$propertyTitle\" — please review it.",
        referenceId = propertyId,
        targetRole  = "admin"
    )

    // ══════════════════════════════════════════════════════════════════════════
    // BOOKING NOTIFICATIONS
    // Cover the full booking lifecycle:
    //   REQUESTED → CONFIRMED → COMPLETED / CANCELLED
    // ══════════════════════════════════════════════════════════════════════════

    // Tells the landlord that a tenant has sent a new booking request.
    suspend fun sendBookingRequestToLandlord(
        landlordId   : String,
        bookingId    : String,
        propertyTitle: String,
        tenantName   : String
    ): Resource<Unit> = sendNotification(
        recipientId = landlordId,
        type        = NotificationType.BOOKING_REQUESTED,
        title       = "New Booking Request 📋",
        body        = "$tenantName has requested to book \"$propertyTitle\".",
        referenceId = bookingId,
        targetRole  = "landlord"
    )

    // Sends admin a copy of every new booking for monitoring purposes.
    suspend fun sendBookingNotificationToAdmin(
        adminId      : String,
        bookingId    : String,
        propertyTitle: String,
        tenantName   : String
    ): Resource<Unit> = sendNotification(
        recipientId = adminId,
        type        = NotificationType.BOOKING_REQUESTED,
        title       = "New Booking (Admin)",
        body        = "$tenantName has booked \"$propertyTitle\".",
        referenceId = bookingId,
        targetRole  = "admin"
    )

    // Tells the tenant their booking has been confirmed by the landlord.
    suspend fun sendBookingConfirmedToTenant(
        tenantId     : String,
        bookingId    : String,
        propertyTitle: String
    ): Resource<Unit> = sendNotification(
        recipientId = tenantId,
        type        = NotificationType.BOOKING_CONFIRMED,
        title       = "Booking Confirmed ✓",
        body        = "Your booking for \"$propertyTitle\" has been confirmed.",
        referenceId = bookingId,
        targetRole  = "tenant"
    )

    // Tells the tenant their booking has been cancelled.
    suspend fun sendBookingCancelledToTenant(
        tenantId     : String,
        bookingId    : String,
        propertyTitle: String
    ): Resource<Unit> = sendNotification(
        recipientId = tenantId,
        type        = NotificationType.BOOKING_CANCELLED,
        title       = "Booking Cancelled",
        body        = "Your booking for \"$propertyTitle\" has been cancelled.",
        referenceId = bookingId,
        targetRole  = "tenant"
    )

    // ── FIX: This function was missing — caused the 2 build errors ────────────
    // Called when a booking's checkOutDate passes (auto-complete) OR when
    // the landlord/admin manually marks a booking as COMPLETED.
    // Prompts the tenant to leave a review for the property.
    suspend fun sendBookingCompletedToTenant(
        tenantId     : String,
        bookingId    : String,
        propertyTitle: String
    ): Resource<Unit> = sendNotification(
        recipientId = tenantId,
        type        = NotificationType.BOOKING_COMPLETED,
        title       = "Stay Completed 🏠",
        body        = "Your stay at \"$propertyTitle\" is complete. We'd love to hear your feedback — please leave a review!",
        referenceId = bookingId,
        targetRole  = "tenant"
    )

    // ══════════════════════════════════════════════════════════════════════════
    // USER VERIFICATION NOTIFICATIONS
    // Sent when a user submits verification documents, and when admin
    // approves or rejects the verification request.
    // ══════════════════════════════════════════════════════════════════════════

    // Alerts admin that a new user has submitted a verification request.
    suspend fun sendNewUserPendingToAdmin(
        adminId : String,
        userId  : String,
        userName: String
    ): Resource<Unit> = sendNotification(
        recipientId = adminId,
        type        = NotificationType.USER_VERIFICATION_PENDING,
        title       = "New User Verification Request",
        body        = "$userName has applied for account verification.",
        referenceId = userId,
        targetRole  = "admin"
    )

    // Tells the user their account has been successfully verified.
    suspend fun sendUserVerifiedNotification(
        userId  : String,
        userName: String
    ): Resource<Unit> = sendNotification(
        recipientId = userId,
        type        = NotificationType.USER_VERIFIED,
        title       = "Account Verified ✓",
        body        = "Congratulations $userName! Your account has been verified. You can now access all features.",
        targetRole  = "all"
    )

    // Tells the user their verification was rejected, with optional reason.
    suspend fun sendUserRejectedNotification(
        userId : String,
        reason : String = ""
    ): Resource<Unit> = sendNotification(
        recipientId = userId,
        type        = NotificationType.USER_REJECTED,
        title       = "Verification Rejected",
        body        = if (reason.isNotEmpty())
            "Your account verification was rejected. Reason: $reason"
        else
            "Your account verification was rejected. Please contact support.",
        targetRole  = "all"
    )

    // ══════════════════════════════════════════════════════════════════════════
    // MESSAGE NOTIFICATIONS
    // Sent to notify a user that they have received a new chat message.
    // recipientRole can be "tenant", "landlord", or "admin".
    // ══════════════════════════════════════════════════════════════════════════

    // Notifies the recipient that a new message has arrived in a conversation.
    suspend fun sendNewMessageNotification(
        recipientId    : String,
        senderName     : String,
        messagePreview : String,
        conversationId : String,
        recipientRole  : String = "tenant"
    ): Resource<Unit> = sendNotification(
        recipientId = recipientId,
        type        = NotificationType.NEW_MESSAGE,
        title       = "New Message from $senderName 💬",
        body        = messagePreview,
        referenceId = conversationId,
        targetRole  = recipientRole
    )
}