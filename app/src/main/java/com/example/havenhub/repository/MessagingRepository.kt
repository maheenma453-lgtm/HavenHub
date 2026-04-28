package com.example.havenhub.repository

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
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION      = "messages"
    }

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
                val convos = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.also { it["id"] = doc.id }
                } ?: emptyList()
                trySend(Resource.Success(convos))
            }
        awaitClose { listener.remove() }
    }

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
            // ✅ Step 1: Message se PEHLE conversation ensure karo
            ensureConversation(conversationId, senderId, receiverId)

            // ✅ Step 2: Message save karo
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

            // ✅ Step 3: Last message update karo
            updateLastMessage(conversationId, message, senderId, receiverId)

            Resource.Success(message)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    // ✅ KEY FIX: set() + merge() — atomic operation
    // Na race condition, na duplicate doc
    private suspend fun ensureConversation(
        conversationId : String,
        userId1        : String,
        userId2        : String
    ) {
        firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .set(
                mapOf(
                    "id"           to conversationId,
                    "participants" to listOf(userId1, userId2), // ✅ DONO hamesha
                    "createdAt"    to FieldValue.serverTimestamp()
                ),
                SetOptions.merge() // ✅ Exist kare toh sirf missing fields add karo
            ).await()
    }

    // ✅ Last message update — participants hamesha overwrite hoti hain
    private suspend fun updateLastMessage(
        conversationId : String,
        message        : Message,
        senderId       : String,
        receiverId     : String
    ) {
        firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .set(
                mapOf(
                    "id"                   to conversationId,
                    "participants"         to listOf(senderId, receiverId), // ✅ CRITICAL
                    "lastMessage"          to message.preview,
                    "lastMessageTimestamp" to message.timestamp,
                    "lastMessageSenderId"  to message.senderId
                ),
                SetOptions.merge()
            ).await()
    }

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
        } catch (_: Exception) { }
    }

    suspend fun deleteConversation(chatId: String) {
        try {
            val messages = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .get()
                .await()

            val batch = firestore.batch()
            messages.documents.forEach { doc -> batch.delete(doc.reference) }
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

    suspend fun getUnreadMessagesCount(userId: String): Int = 0

    // ✅ Simple 2-user chatId — consistent everywhere
    fun generateChatId(userId1: String, userId2: String): String {
        return listOf(userId1, userId2).sorted().joinToString("_")
    }

    // ✅ Public wrapper — ViewModel se call hoti hai
    suspend fun createOrGetConversation(userId1: String, userId2: String): String {
        val conversationId = generateChatId(userId1, userId2)
        ensureConversation(conversationId, userId1, userId2)
        return conversationId
    }
}