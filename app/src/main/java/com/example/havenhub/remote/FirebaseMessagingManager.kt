package com.example.havenhub.remote
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.havenhub.data.model.Message
import com.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseMessagingManager
 *
 * Manages two distinct messaging concerns in HavenHub:
 *
 * 1. FCM Push Notifications (Firebase Cloud Messaging)
 *    - Retrieves and refreshes device FCM tokens
 *    - Saves tokens to Firestore for server-side targeting
 *    - Subscribes/unsubscribes to broadcast topics
 *
 * 2. In-App Chat Messaging (Firestore)
 *    - Sends chat messages between users (tenant ↔ host)
 *    - Manages conversation metadata (last message, unread count)
 *
 * Conversation ID convention:
 *   Sort both UIDs lexicographically and join with "_"
 *   e.g., min(uid1, uid2) + "_" + max(uid1, uid2)
 *   This guarantees the same document is used regardless of who initiates.
 */
@Singleton
class FirebaseMessagingManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val fcm: FirebaseMessaging
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Firestore References
    // ─────────────────────────────────────────────────────────────────────────

    private val usersCollection         = firestore.collection("users")
    private val conversationsCollection = firestore.collection("conversations")

    // ─────────────────────────────────────────────────────────────────────────
    // FCM Token Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves the current FCM device token for this installation.
     * The token is required by the backend to send targeted push notifications.
     *
     * @return [Resource.Success] with the FCM token string,
     *         or [Resource.Error] if retrieval fails.
     */
    suspend fun getDeviceToken(): Resource<String> {
        return try {
            val token = fcm.token.await()
            Resource.Success(token)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to get FCM token")
        }
    }

    /**
     * Saves the FCM device token to the user's Firestore document.
     * Called after login and when the token is refreshed (onNewToken in FCM service).
     *
     * @param userId The Firebase Auth UID of the user.
     * @param token  The FCM token to store.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun saveDeviceToken(userId: String, token: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("fcmToken", token)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save FCM token")
        }
    }

    /**
     * Removes the FCM token from the user's Firestore document on sign-out.
     * Prevents push notifications from being sent to a signed-out device.
     *
     * @param userId The Firebase Auth UID of the user.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun clearDeviceToken(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("fcmToken", null)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to clear FCM token")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FCM Topic Subscriptions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Subscribes this device to a broadcast FCM topic.
     * Useful for admin announcements or role-based notifications.
     *
     * Example topics: "all_users", "landlords", "admin_alerts"
     *
     * @param topic The FCM topic name (no spaces, lowercase recommended).
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun subscribeToTopic(topic: String): Resource<Unit> {
        return try {
            fcm.subscribeToTopic(topic).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to subscribe to topic: $topic")
        }
    }

    /**
     * Unsubscribes this device from a broadcast FCM topic.
     *
     * @param topic The FCM topic name to unsubscribe from.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun unsubscribeFromTopic(topic: String): Resource<Unit> {
        return try {
            fcm.unsubscribeFromTopic(topic).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unsubscribe from topic: $topic")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // In-App Chat Messaging (Firestore)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the unique conversation ID from two user UIDs.
     * Sorting ensures the same ID is produced regardless of who is "sender" or "receiver".
     *
     * @param uid1 First user's UID.
     * @param uid2 Second user's UID.
     * @return Deterministic conversation ID string.
     */
    fun buildConversationId(uid1: String, uid2: String): String {
        val sorted = listOf(uid1, uid2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    /**
     * Sends a chat message in a conversation thread.
     * Also updates the parent conversation document with last message metadata.
     *
     * @param message The [Message] object to persist.
     * @return [Resource.Success] with the generated message ID,
     *         or [Resource.Error] on failure.
     */
    suspend fun sendMessage(message: Message): Resource<String> {
        return try {
            val convId = buildConversationId(message.senderId, message.receiverId)

            // Step 1: Add message to the messages sub-collection
            val msgRef = conversationsCollection
                .document(convId)
                .collection("messages")
                .document()
            val newMessage = message.copy(id = msgRef.id)
            msgRef.set(newMessage).await()

            // Step 2: Update conversation metadata (last message preview)
            conversationsCollection.document(convId).set(
                mapOf(
                    "lastMessage"    to message.content,
                    "lastTimestamp"  to message.timestamp,
                    "participants"   to listOf(message.senderId, message.receiverId)
                ),
                com.google.firebase.firestore.SetOptions.merge()   // merge to avoid overwriting
            ).await()

            Resource.Success(msgRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send message")
        }
    }

    /**
     * Marks all messages in a conversation as read for the given user.
     * Called when the user opens a chat screen.
     *
     * @param conversationId The conversation document ID.
     * @param userId         The UID of the user reading the messages.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun markMessagesAsRead(conversationId: String, userId: String): Resource<Unit> {
        return try {
            // Fetch all unread messages not sent by the current user
            val unreadMessages = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()

            // Batch update all unread messages to isRead = true
            val batch = firestore.batch()
            unreadMessages.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark messages as read")
        }
    }
}


