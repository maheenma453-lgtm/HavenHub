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
        private const val TAG               = "MESSAGING_REPO"
        private const val CONVERSATIONS_COL = "conversations"
        private const val MESSAGES_COL      = "messages"
    }

    fun getConversationsRealtime(userId: String): Flow<Resource<List<Map<String, Any>>>> = callbackFlow {
        if (userId.isEmpty()) { trySend(Resource.Success(emptyList())); awaitClose(); return@callbackFlow }

        val listener = firestore
            .collection(CONVERSATIONS_COL)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load conversations"))
                    return@addSnapshotListener
                }
                val convos = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.also { it["id"] = doc.id }
                } ?: emptyList()
                trySend(Resource.Success(convos))
            }

        awaitClose { listener.remove() }
    }

    fun getMessagesRealtime(chatId: String): Flow<Resource<List<Message>>> = callbackFlow {
        if (chatId.isEmpty()) { trySend(Resource.Success(emptyList())); awaitClose(); return@callbackFlow }

        val listener = firestore
            .collection(CONVERSATIONS_COL)
            .document(chatId)
            .collection(MESSAGES_COL)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen")); return@addSnapshotListener
                }
                trySend(Resource.Success(snapshot?.toObjects(Message::class.java) ?: emptyList()))
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId      : String,
        receiverId    : String,
        content       : String,
        messageType   : String  = Message.TYPE_TEXT,
        mediaUrl      : String? = null,
        mediaFileName : String? = null
    ): Resource<Message> {
        return try {
            ensureConversation(conversationId, senderId, receiverId)

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
            // Increment recipient unread counter in conversation doc
            updateLastMessage(conversationId, message, senderId, receiverId)
            Resource.Success(message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    // Batch delete selected messages (sender-only, checked in ChatScreen)
    suspend fun deleteMessages(
        chatId    : String,
        messageIds: List<String>
    ): Resource<Unit> {
        return try {
            if (messageIds.isEmpty()) return Resource.Success(Unit)

            val batch = firestore.batch()
            messageIds.forEach { msgId ->
                val ref = firestore
                    .collection(CONVERSATIONS_COL)
                    .document(chatId)
                    .collection(MESSAGES_COL)
                    .document(msgId)
                batch.delete(ref)
            }
            batch.commit().await()
            Log.d(TAG, "deleteMessages SUCCESS: chatId=$chatId deleted=${messageIds.size} msgs")

            // Refresh lastMessage after delete
            refreshLastMessageAfterDelete(chatId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteMessages FAIL: ${e.message}")
            Resource.Error(e.message ?: "Messages could not be deleted")
        }
    }

    // Refresh conversation doc lastMessage after deletion
    private suspend fun refreshLastMessageAfterDelete(chatId: String) {
        try {
            val remaining = firestore
                .collection(CONVERSATIONS_COL)
                .document(chatId)
                .collection(MESSAGES_COL)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (remaining.isEmpty) {
                firestore.collection(CONVERSATIONS_COL).document(chatId)
                    .update(mapOf(
                        "lastMessage"          to "",
                        "lastMessageTimestamp" to 0L,
                        "lastMessageSenderId"  to ""
                    )).await()
            } else {
                val lastMsg = remaining.documents.first().toObject(Message::class.java)
                if (lastMsg != null) {
                    firestore.collection(CONVERSATIONS_COL).document(chatId)
                        .update(mapOf(
                            "lastMessage"          to lastMsg.preview,
                            "lastMessageTimestamp" to lastMsg.timestamp,
                            "lastMessageSenderId"  to lastMsg.senderId
                        )).await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshLastMessageAfterDelete FAIL: ${e.message}")
        }
    }

    private suspend fun ensureConversation(conversationId: String, userId1: String, userId2: String) {
        firestore.collection(CONVERSATIONS_COL).document(conversationId)
            .set(mapOf(
                "id"           to conversationId,
                "participants" to listOf(userId1, userId2),
                "createdAt"    to FieldValue.serverTimestamp()
            ), SetOptions.merge())
            .await()
    }

    // Increment "unreadCount_<receiverId>" so badge count is accurate
    private suspend fun updateLastMessage(
        conversationId: String,
        message       : Message,
        senderId      : String,
        receiverId    : String
    ) {
        firestore.collection(CONVERSATIONS_COL).document(conversationId)
            .set(mapOf(
                "id"                      to conversationId,
                "participants"            to listOf(senderId, receiverId),
                "lastMessage"             to message.preview,
                "lastMessageTimestamp"    to message.timestamp,
                "lastMessageSenderId"     to message.senderId,
                // Per-recipient unread counter — increments on every send
                "unreadCount_$receiverId" to FieldValue.increment(1)
            ), SetOptions.merge())
            .await()
    }

    // Reset unread counter when user opens the chat
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String) {
        try {
            val unread = firestore
                .collection(CONVERSATIONS_COL).document(chatId)
                .collection(MESSAGES_COL)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get().await()

            if (!unread.isEmpty) {
                val batch = firestore.batch()
                unread.documents.forEach { batch.update(it.reference, "isRead", true) }
                batch.commit().await()
            }

            // Reset per-user unread counter on the conversation doc
            firestore.collection(CONVERSATIONS_COL).document(chatId)
                .update("unreadCount_$currentUserId", 0).await()

        } catch (e: Exception) {
            Log.e(TAG, "markMessagesAsRead error: ${e.message}")
        }
    }

    suspend fun deleteConversation(chatId: String) {
        try {
            val messages = firestore.collection(CONVERSATIONS_COL).document(chatId)
                .collection(MESSAGES_COL).get().await()
            val batch = firestore.batch()
            messages.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            firestore.collection(CONVERSATIONS_COL).document(chatId).delete().await()
        } catch (e: Exception) {
            throw Exception("Failed to delete chat: ${e.message}")
        }
    }

    suspend fun getUnreadMessagesCount(userId: String): Int = 0

    fun generateChatId(userId1: String, userId2: String): String =
        listOf(userId1, userId2).sorted().joinToString("_")

    suspend fun createOrGetConversation(userId1: String, userId2: String): String {
        val conversationId = generateChatId(userId1, userId2)
        ensureConversation(conversationId, userId1, userId2)
        return conversationId
    }
}