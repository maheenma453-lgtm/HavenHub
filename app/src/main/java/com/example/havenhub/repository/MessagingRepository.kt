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
    private val firestore          : FirebaseFirestore,
    private val firebaseDataManager: FirebaseDataManager
) {

    companion object {
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION      = "messages"
    }

    // ✅ Conversations real-time listen
    fun getConversationsRealtime(userId: String): Flow<Resource<List<Map<String, Any>>>> = callbackFlow {
        val listener = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load conversations"))
                    return@addSnapshotListener
                }
                val convos = snapshot?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                trySend(Resource.Success(convos))
            }
        awaitClose { listener.remove() }
    }

    // ✅ Messages real-time listen
    fun getMessagesRealtime(chatId: String): Flow<Resource<List<Message>>> = callbackFlow {
        val listener = firestore
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
        awaitClose { listener.remove() }
    }

    // ✅ Message bhejo
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
            val messageRef = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_COLLECTION)
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
            updateConversationLastMessage(conversationId, message)
            Resource.Success(message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    // ✅ Messages read mark karo
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String) {
        try {
            val unreadMessages = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            unreadMessages.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Silent fail
        }
    }

    // ✅ Conversation delete karo
    suspend fun deleteConversation(chatId: String) {
        try {
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

            firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(chatId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to delete chat: ${e.message}")
        }
    }

    // ✅ Unread count
    suspend fun getUnreadMessagesCount(userId: String): Int = 0

    // ✅ Chat ID generate karo
    fun generateChatId(userId1: String, userId2: String): String {
        return listOf(userId1, userId2).sorted().joinToString("_")
    }

    // ✅ Conversation banao ya fetch karo
    suspend fun createOrGetConversation(userId1: String, userId2: String): String {
        val conversationId = generateChatId(userId1, userId2)
        return try {
            val conversationRef = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)

            val snapshot = conversationRef.get().await()

            if (!snapshot.exists()) {
                val conversationData = hashMapOf(
                    "id"                   to conversationId,
                    "participants"         to listOf(userId1, userId2),
                    "lastMessage"          to "",
                    "lastMessageTimestamp" to System.currentTimeMillis(),
                    "createdAt"            to System.currentTimeMillis()
                )
                conversationRef.set(conversationData).await()
            }
            conversationId
        } catch (e: Exception) {
            throw Exception("Failed to create conversation: ${e.message}")
        }
    }

    // Helper: Last message update karo
    private suspend fun updateConversationLastMessage(conversationId: String, message: Message) {
        try {
            firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .update(
                    mapOf(
                        "lastMessage"          to message.preview,
                        "lastMessageTimestamp" to message.timestamp,
                        "lastMessageSenderId"  to message.senderId
                    )
                ).await()
        } catch (e: Exception) {
            val conversationData = hashMapOf(
                "id"                   to conversationId,
                "participants"         to listOf(message.senderId, message.receiverId),
                "lastMessage"          to message.preview,
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId"  to message.senderId,
                "createdAt"            to System.currentTimeMillis()
            )
            firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .set(conversationData)
                .await()
        }
    }
}