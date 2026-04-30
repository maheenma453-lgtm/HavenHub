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

    fun observeNotifications(userId: String): Flow<List<Notification>> =
        realtimeListener.listenToNotifications(userId)

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

    suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            col.document(notificationId).update("isRead", true).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark as read")
        }
    }

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

    suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            col.document(notificationId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete notification")
        }
    }

    // CORE SENDER
    // createdAt = Timestamp.now() — hamesha manually set karo.
    // @ServerTimestamp Notification.kt se hata diya — dono ek saath conflict karte the
    // aur Firestore kabhi null store kar leta tha jis se orderBy("createdAt") fail hoti thi.
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

    suspend fun sendPropertyApprovedNotification(
        ownerId      : String,
        propertyId   : String,
        propertyTitle: String,
        adminNote    : String = ""
    ): Resource<Unit> = sendNotification(
        recipientId = ownerId,
        type        = NotificationType.PROPERTY_APPROVED,
        title       = "Property Approved",
        body        = if (adminNote.isNotEmpty())
            "Mubarak! \"$propertyTitle\" approve ho gayi. Admin note: $adminNote"
        else
            "Mubarak! Aapki property \"$propertyTitle\" approve ho gayi hai.",
        referenceId = propertyId,
        adminNote   = adminNote,
        targetRole  = "landlord"
    )

    suspend fun sendPropertyRejectedNotification(
        ownerId      : String,
        propertyId   : String,
        propertyTitle: String,
        adminNote    : String = ""
    ): Resource<Unit> = sendNotification(
        recipientId = ownerId,
        type        = NotificationType.PROPERTY_REJECTED,
        title       = "Property Rejected",
        body        = if (adminNote.isNotEmpty())
            "Aapki property \"$propertyTitle\" approve nahi hui. Reason: $adminNote"
        else
            "Aapki property \"$propertyTitle\" approve nahi hui.",
        referenceId = propertyId,
        adminNote   = adminNote,
        targetRole  = "landlord"
    )

    suspend fun sendNewPropertyPendingNotification(
        adminId      : String,
        propertyId   : String,
        propertyTitle: String,
        landlordName : String
    ): Resource<Unit> = sendNotification(
        recipientId = adminId,
        type        = NotificationType.PROPERTY_PENDING,
        title       = "New Property Pending Review",
        body        = "$landlordName ne \"$propertyTitle\" submit ki hai — please review karen.",
        referenceId = propertyId,
        targetRole  = "admin"
    )

    suspend fun sendBookingRequestToLandlord(
        landlordId   : String,
        bookingId    : String,
        propertyTitle: String,
        tenantName   : String
    ): Resource<Unit> = sendNotification(
        recipientId = landlordId,
        type        = NotificationType.BOOKING_REQUESTED,
        title       = "New Booking Request",
        body        = "$tenantName ne \"$propertyTitle\" ke liye booking request ki hai.",
        referenceId = bookingId,
        targetRole  = "landlord"
    )

    suspend fun sendBookingNotificationToAdmin(
        adminId      : String,
        bookingId    : String,
        propertyTitle: String,
        tenantName   : String
    ): Resource<Unit> = sendNotification(
        recipientId = adminId,
        type        = NotificationType.BOOKING_REQUESTED,
        title       = "New Booking Request (Admin)",
        body        = "$tenantName ne \"$propertyTitle\" book kiya.",
        referenceId = bookingId,
        targetRole  = "admin"
    )

    suspend fun sendBookingConfirmedToTenant(
        tenantId     : String,
        bookingId    : String,
        propertyTitle: String
    ): Resource<Unit> = sendNotification(
        recipientId = tenantId,
        type        = NotificationType.BOOKING_CONFIRMED,
        title       = "Booking Confirmed!",
        body        = "Aapki booking \"$propertyTitle\" ke liye confirm ho gayi hai.",
        referenceId = bookingId,
        targetRole  = "tenant"
    )

    suspend fun sendBookingCancelledToTenant(
        tenantId     : String,
        bookingId    : String,
        propertyTitle: String
    ): Resource<Unit> = sendNotification(
        recipientId = tenantId,
        type        = NotificationType.BOOKING_CANCELLED,
        title       = "Booking Cancelled",
        body        = "Aapki booking \"$propertyTitle\" ke liye cancel ho gayi hai.",
        referenceId = bookingId,
        targetRole  = "tenant"
    )

    suspend fun sendNewUserPendingToAdmin(
        adminId : String,
        userId  : String,
        userName: String
    ): Resource<Unit> = sendNotification(
        recipientId = adminId,
        type        = NotificationType.USER_VERIFICATION_PENDING,
        title       = "New User Verification Pending",
        body        = "$userName ne verification ke liye apply kiya hai.",
        referenceId = userId,
        targetRole  = "admin"
    )

    suspend fun sendUserVerifiedNotification(
        userId  : String,
        userName: String
    ): Resource<Unit> = sendNotification(
        recipientId = userId,
        type        = NotificationType.USER_VERIFIED,
        title       = "Account Verified!",
        body        = "Mubarak $userName! Aapka account verify ho gaya hai. Ab aap sab features use kar sakte hain.",
        targetRole  = "all"
    )

    suspend fun sendUserRejectedNotification(
        userId : String,
        reason : String = ""
    ): Resource<Unit> = sendNotification(
        recipientId = userId,
        type        = NotificationType.USER_REJECTED,
        title       = "Verification Rejected",
        body        = if (reason.isNotEmpty())
            "Aapka account verify nahi hua. Reason: $reason"
        else
            "Aapka account verification reject ho gaya. Support se contact karen.",
        targetRole  = "all"
    )

    suspend fun sendNewMessageNotification(
        recipientId    : String,
        senderName     : String,
        messagePreview : String,
        conversationId : String,
        recipientRole  : String = "tenant"
    ): Resource<Unit> = sendNotification(
        recipientId = recipientId,
        type        = NotificationType.NEW_MESSAGE,
        title       = "New Message from $senderName",
        body        = messagePreview,
        referenceId = conversationId,
        targetRole  = recipientRole
    )
}
