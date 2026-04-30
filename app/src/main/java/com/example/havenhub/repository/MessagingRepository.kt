package com.example.havenhub.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.example.havenhub.data.Message
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val firestore          : FirebaseFirestore,
    private val firebaseDataManager: FirebaseDataManager
) {

    companion object {
        private const val TAG                   = "MESSAGING_REPO"
        private const val CONVERSATIONS_COL     = "conversations"
        private const val MESSAGES_COL          = "messages"
    }

    // Returns a real-time flow of all conversations the user is part of.
    // Firestore query: participants array contains userId, ordered by last message time.
    // Index required: conversations — participants (Array) + lastMessageTimestamp DESC
    fun getConversationsRealtime(userId: String): Flow<Resource<List<Map<String, Any>>>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(Resource.Success(emptyList()))
            awaitClose()
            return@callbackFlow
        }

        Log.d(TAG, "Starting conversations listener for userId=$userId")

        val listener = firestore
            .collection(CONVERSATIONS_COL)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getConversationsRealtime error: ${error.localizedMessage}")
                    trySend(Resource.Error(error.message ?: "Failed to load conversations"))
                    return@addSnapshotListener
                }

                val convos = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.also { it["id"] = doc.id }
                } ?: emptyList()

                Log.d(TAG, "Conversations loaded: ${convos.size} for userId=$userId")
                trySend(Resource.Success(convos))
            }

        awaitClose { listener.remove() }
    }

    // Returns a real-time flow of messages in a conversation, oldest first.
    fun getMessagesRealtime(chatId: String): Flow<Resource<List<Message>>> = callbackFlow {
        if (chatId.isEmpty()) {
            trySend(Resource.Success(emptyList()))
            awaitClose()
            return@callbackFlow
        }

        val listener = firestore
            .collection(CONVERSATIONS_COL)
            .document(chatId)
            .collection(MESSAGES_COL)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getMessagesRealtime error: ${error.localizedMessage}")
                    trySend(Resource.Error(error.message ?: "Failed to listen"))
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                Log.d(TAG, "Messages loaded: ${messages.size} in chatId=$chatId")
                trySend(Resource.Success(messages))
            }

        awaitClose { listener.remove() }
    }

    // Send a message between two users.
    // Steps: ensure conversation doc exists → save message → update last message metadata.
    suspend fun sendMessage(
        conversationId : String,
        senderId       : String,
        receiverId     : String,
        content        : String,
        messageType    : String  = Message.TYPE_TEXT,
        mediaUrl       : String? = null,
        mediaFileName  : String? = null
    ): Resource<Message> {
        return try {
            // Step 1: Make sure conversation document exists with both participants
            ensureConversation(conversationId, senderId, receiverId)

            // Step 2: Save the message
            val messageRef = firestore
                .collection(CONVERSATIONS_COL)
                .document(conversationId)
                .collection(MESSAGES_COL)
                .document()

            val message = Message(
                id             = messageRef.id,
                conversationId = conversationId,
                senderId       = senderId,
                receiverId     = receiverId,
                content        = content,
                timestamp      = System.currentTimeMillis(),
                isRead         = false,
                messageType    = messageType,
                mediaUrl       = mediaUrl,
                mediaFileName  = mediaFileName
            )

            messageRef.set(message).await()
            Log.d(TAG, "Message saved: ${messageRef.id} in conversation=$conversationId")

            // Step 3: Update conversation metadata so MessageListScreen shows latest message
            updateLastMessage(conversationId, message, senderId, receiverId)

            Resource.Success(message)
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage error: ${e.localizedMessage}")
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    // Create or update the conversation document with both participants.
    // Uses SetOptions.merge() so existing fields are not overwritten.
    // participants field is always written — this is the field Firestore queries on.
    // Without this field being correctly set, getConversationsRealtime returns nothing.
    private suspend fun ensureConversation(
        conversationId : String,
        userId1        : String,
        userId2        : String
    ) {
        val data = mapOf(
            "id"           to conversationId,
            "participants" to listOf(userId1, userId2),   // CRITICAL: both users always present
            "createdAt"    to FieldValue.serverTimestamp()
        )
        firestore
            .collection(CONVERSATIONS_COL)
            .document(conversationId)
            .set(data, SetOptions.merge())
            .await()

        Log.d(TAG, "Conversation ensured: $conversationId participants=[$userId1, $userId2]")
    }

    // Update conversation doc with last message preview and timestamp.
    // participants is re-written here too as a safety net.
    private suspend fun updateLastMessage(
        conversationId : String,
        message        : Message,
        senderId       : String,
        receiverId     : String
    ) {
        val data = mapOf(
            "id"                   to conversationId,
            "participants"         to listOf(senderId, receiverId),  // Safety: keep both users
            "lastMessage"          to message.preview,
            "lastMessageTimestamp" to message.timestamp,
            "lastMessageSenderId"  to message.senderId
        )
        firestore
            .collection(CONVERSATIONS_COL)
            .document(conversationId)
            .set(data, SetOptions.merge())
            .await()

        Log.d(TAG, "Last message updated for conversation=$conversationId")
    }

    // Mark all messages sent to currentUserId in this chat as read
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String) {
        try {
            val unread = firestore
                .collection(CONVERSATIONS_COL)
                .document(chatId)
                .collection(MESSAGES_COL)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (unread.isEmpty) return

            val batch = firestore.batch()
            unread.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            Log.d(TAG, "Marked ${unread.size()} messages as read in chatId=$chatId")
        } catch (e: Exception) {
            Log.e(TAG, "markMessagesAsRead error: ${e.localizedMessage}")
        }
    }

    // Delete all messages and the conversation document itself
    suspend fun deleteConversation(chatId: String) {
        try {
            val messages = firestore
                .collection(CONVERSATIONS_COL)
                .document(chatId)
                .collection(MESSAGES_COL)
                .get()
                .await()

            val batch = firestore.batch()
            messages.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()

            firestore.collection(CONVERSATIONS_COL).document(chatId).delete().await()
            Log.d(TAG, "Conversation deleted: $chatId")
        } catch (e: Exception) {
            throw Exception("Failed to delete chat: ${e.message}")
        }
    }

    suspend fun getUnreadMessagesCount(userId: String): Int = 0

    // Generate a consistent chatId for any two users.
    // Sorted alphabetically so userId1_userId2 == userId2_userId1.
    fun generateChatId(userId1: String, userId2: String): String =
        listOf(userId1, userId2).sorted().joinToString("_")

    // Public entry point called from ViewModel before opening a chat
    suspend fun createOrGetConversation(userId1: String, userId2: String): String {
        val conversationId = generateChatId(userId1, userId2)
        ensureConversation(conversationId, userId1, userId2)
        return conversationId
    }
}