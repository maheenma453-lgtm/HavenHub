package com.example.havenhub.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    private val firestore: FirebaseFirestore,
    private val firebaseDataManager: FirebaseDataManager
) {

    companion object {
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
    }

    // ✅ Get Messages Between Two Users (Real-time)
    fun getMessagesRealtime(chatId: String): Flow<Resource<List<Message>>> = callbackFlow {
        val listenerRegistration = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    trySend(Resource.Success(messages))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    // ✅ Send Message
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: String,
        messageType: String = Message.TYPE_TEXT,
        mediaUrl: String? = null,
        mediaFileName: String? = null
    ): Resource<Message> {
        return try {
            // Reference to messages sub-collection
            val messageRef = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_COLLECTION)
                .document()

            // Create message object
            val message = Message(
                id = messageRef.id,
                conversationId = conversationId,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                messageType = messageType,
                mediaUrl = mediaUrl,
                mediaFileName = mediaFileName
            )

            // Save to Firestore
            messageRef.set(message).await()

            // Update conversation's last message
            updateConversationLastMessage(conversationId, message)

            Resource.Success(message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    // ✅ Mark Messages as Read
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String) {
        try {
            // Get all unread messages where current user is receiver
            val unreadMessages = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            // Batch update
            val batch = firestore.batch()
            unreadMessages.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

        } catch (e: Exception) {
            // Silent fail
        }
    }

    // ✅ Delete Conversation
    suspend fun deleteConversation(chatId: String) {
        try {
            // Delete all messages in sub-collection
            val messages = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .get()
                .await()

            val batch = firestore.batch()
            messages.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            // Delete conversation document
            firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(chatId)
                .delete()
                .await()

        } catch (e: Exception) {
            throw Exception("Failed to delete chat: ${e.message}")
        }
    }

    // ✅ Get Unread Messages Count
    suspend fun getUnreadMessagesCount(userId: String): Int {
        return try {
            // This will need to check across all conversations
            // For now, returning 0 as we need conversation list first
            0
        } catch (e: Exception) {
            0
        }
    }

    // ✅ Generate Conversation ID (sorted UIDs)
    fun generateChatId(userId1: String, userId2: String): String {
        return listOf(userId1, userId2).sorted().joinToString("_")
    }

    // ✅ Create or Get Conversation Document
    suspend fun createOrGetConversation(userId1: String, userId2: String): String {
        val conversationId = generateChatId(userId1, userId2)

        try {
            val conversationRef = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)

            val snapshot = conversationRef.get().await()

            if (!snapshot.exists()) {
                // Create new conversation document
                val conversationData = hashMapOf(
                    "id" to conversationId,
                    "participants" to listOf(userId1, userId2),
                    "lastMessage" to "",
                    "lastMessageTimestamp" to System.currentTimeMillis(),
                    "createdAt" to System.currentTimeMillis()
                )
                conversationRef.set(conversationData).await()
            }

            return conversationId
        } catch (e: Exception) {
            throw Exception("Failed to create conversation: ${e.message}")
        }
    }

    // Helper: Update conversation's last message
    private suspend fun updateConversationLastMessage(conversationId: String, message: Message) {
        try {
            firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .update(
                    mapOf(
                        "lastMessage" to message.preview,
                        "lastMessageTimestamp" to message.timestamp,
                        "lastMessageSenderId" to message.senderId
                    )
                ).await()
        } catch (e: Exception) {
            // If conversation doesn't exist, create it
            val conversationData = hashMapOf(
                "id" to conversationId,
                "participants" to listOf(message.senderId, message.receiverId),
                "lastMessage" to message.preview,
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId" to message.senderId,
                "createdAt" to System.currentTimeMillis()
            )
            firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .set(conversationData)
                .await()
        }
    }
}