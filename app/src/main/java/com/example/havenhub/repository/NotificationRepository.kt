package com.example.havenhub.repository
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.google.firebase.firestore.FirebaseFirestore
import com.havenhub.data.model.Notification
import com.havenhub.data.remote.FirebaseRealtimeListener
import com.havenhub.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationRepository
 *
 * Manages in-app notification records stored in Firestore.
 * Works alongside FCM (push) notifications — this repository handles
 * the persistent notification history visible in the app's notification center.
 *
 * Responsibilities:
 *  - Observe live notification updates via a real-time Flow
 *  - Fetch all notifications for a user (one-time)
 *  - Mark individual or all notifications as read
 *  - Delete old notifications
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeListener: FirebaseRealtimeListener
) {

    private val notificationsCollection = firestore.collection("notifications")

    // ─────────────────────────────────────────────────────────────────────────
    // Real-time Observation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Provides a live [Flow] of notifications for the given user.
     * The Flow emits an updated list every time a notification is
     * added, modified, or deleted for this user.
     *
     * @param userId The Firebase Auth UID of the target user.
     * @return [Flow] of [List<Notification>] that updates in real time.
     */
    fun observeNotifications(userId: String): Flow<List<Notification>> {
        return realtimeListener.listenToNotifications(userId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * One-time fetch of all notifications for a user.
     *
     * @param userId The user's Firebase Auth UID.
     * @return [Resource.Success] with a list of [Notification], or [Resource.Error].
     */
    suspend fun getUserNotifications(userId: String): Resource<List<Notification>> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Notification::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch notifications")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Marks a single notification as read.
     *
     * @param notificationId The document ID of the notification.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark notification as read")
        }
    }

    /**
     * Marks all unread notifications for a user as read in a single batch.
     * Called when the user opens the notifications screen.
     *
     * @param userId The user's Firebase Auth UID.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun markAllAsRead(userId: String): Resource<Unit> {
        return try {
            val unread = notificationsCollection
                .whereEqualTo("userId", userId)
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
            Resource.Error(e.localizedMessage ?: "Failed to mark all notifications as read")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes a specific notification permanently.
     *
     * @param notificationId The document ID of the notification to delete.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete notification")
        }
    }
}


