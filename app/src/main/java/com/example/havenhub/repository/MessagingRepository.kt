package com.example.havenhub.repository
import com.example.havenhub.remote.FirebaseMessagingManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.havenhub.data.model.Message
import com.havenhub.data.remote.FirebaseMessagingManager
import com.havenhub.data.remote.FirebaseRealtimeListener
import com.havenhub.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MessagingRepository
 *
 * Manages in-app real-time chat between tenants and landlords.
 * Combines [FirebaseRealtimeListener] for live message streams
 * with [FirebaseMessagingManager] for sending messages and managing
 * read receipts.
 *
 * Conversation ID Convention:
 *  Sort both user UIDs lexicographically and join with "_"
 *  e.g., min(uid1, uid2) + "_" + max(uid1, uid2)
 *  This ensures the same document is referenced regardless of who initiates.
 *
 * Responsibilities:
 *  - Observe live message flow for a conversation
 *  - Send messages
 *  - Mark messages as read
 *  - Build conversation IDs
 */
@Singleton
class MessagingRepository @Inject constructor(
    private val messagingManager: FirebaseMessagingManager,
    private val realtimeListener: FirebaseRealtimeListener
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Conversation ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the canonical conversation ID for two users.
     * Delegates to [FirebaseMessagingManager] to ensure consistent formatting.
     *
     * @param uid1 First participant's UID.
     * @param uid2 Second participant's UID.
     * @return A deterministic, unique conversation ID string.
     */
    fun getConversationId(uid1: String, uid2: String): String {
        return messagingManager.buildConversationId(uid1, uid2)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Real-time Message Stream
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a [Flow] that emits the message list for a conversation
     * in real-time, updating whenever a new message is added or changed.
     *
     * @param conversationId The unique conversation document ID.
     * @return [Flow] of [List<Message>] ordered by ascending timestamp.
     */
    fun observeMessages(conversationId: String): Flow<List<Message>> {
        return realtimeListener.listenToMessages(conversationId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send Message
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends a chat message and updates the conversation's last-message metadata.
     *
     * @param message The [Message] object to send.
     * @return [Resource.Success] with the new message ID, or [Resource.Error].
     */
    suspend fun sendMessage(message: Message): Resource<String> {
        return messagingManager.sendMessage(message)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read Receipts
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Marks all unread messages in a conversation as read for the given user.
     * Should be called when the user opens a chat screen.
     *
     * @param conversationId The unique conversation document ID.
     * @param userId         The UID of the user viewing the messages.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun markMessagesAsRead(conversationId: String, userId: String): Resource<Unit> {
        return messagingManager.markMessagesAsRead(conversationId, userId)
    }
}


