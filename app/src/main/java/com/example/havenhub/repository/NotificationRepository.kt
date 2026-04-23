package com.example.havenhub.repository

import com.example.havenhub.data.Notification
import com.example.havenhub.data.NotificationType
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.utils.Resource
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

    private val notificationsCollection = firestore.collection("notifications")

    fun observeNotifications(userId: String): Flow<List<Notification>> =
        realtimeListener.listenToNotifications(userId)

    suspend fun getUserNotifications(userId: String): Resource<List<Notification>> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("recipientId", userId)   // ✅ FIX: "userId" → "recipientId"
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
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark as read")
        }
    }

    suspend fun markAllAsRead(userId: String): Resource<Unit> {
        return try {
            val unread = notificationsCollection
                .whereEqualTo("recipientId", userId)   // ✅ FIX: "userId" → "recipientId"
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            unread.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark all as read")
        }
    }

    suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete notification")
        }
    }

    // ✅ NEW: Generic notification sender — har jagah se use ho sakta hai
    suspend fun sendNotification(
        recipientId   : String,
        type          : NotificationType,
        title         : String,
        body          : String,
        referenceId   : String = "",
        adminNote     : String = "",     // ✅ Admin ka note yahan aayega
        targetRole    : String = "landlord"
    ): Resource<Unit> {
        return try {
            val docRef = notificationsCollection.document()
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
                adminNote      = adminNote   // ✅ Firestore mein save hoga
            )
            docRef.set(notification).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send notification")
        }
    }

    // ✅ NEW: Property approve pe call karo
    suspend fun sendPropertyApprovedNotification(
        ownerId      : String,
        propertyId   : String,
        propertyTitle: String,
        adminNote    : String = ""
    ): Resource<Unit> = sendNotification(
        recipientId = ownerId,
        type        = NotificationType.PROPERTY_APPROVED,
        title       = "Property Approved ✓",
        body        = "Mubarak! Aapki property '$propertyTitle' approve ho gayi hai.",
        referenceId = propertyId,
        adminNote   = adminNote,
        targetRole  = "landlord"
    )

    // ✅ NEW: Property reject pe call karo
    suspend fun sendPropertyRejectedNotification(
        ownerId      : String,
        propertyId   : String,
        propertyTitle: String,
        adminNote    : String = ""
    ): Resource<Unit> = sendNotification(
        recipientId = ownerId,
        type        = NotificationType.PROPERTY_REJECTED,
        title       = "Property Rejected",
        body        = "Aapki property '$propertyTitle' approve nahi hui.",
        referenceId = propertyId,
        adminNote   = adminNote,
        targetRole  = "landlord"
    )
}