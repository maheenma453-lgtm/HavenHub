package com.example.havenhub.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.example.havenhub.data.Message          // ✅ fixed
import com.example.havenhub.utils.Resource         // ✅ fixed
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMessagingManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val fcm: FirebaseMessaging
) {

    private val usersCollection         = firestore.collection("users")
    private val conversationsCollection = firestore.collection("conversations")

    suspend fun getDeviceToken(): Resource<String> {
        return try {
            val token = fcm.token.await()
            Resource.Success(token)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to get FCM token")
        }
    }

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

    suspend fun subscribeToTopic(topic: String): Resource<Unit> {
        return try {
            fcm.subscribeToTopic(topic).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to subscribe to topic: $topic")
        }
    }

    suspend fun unsubscribeFromTopic(topic: String): Resource<Unit> {
        return try {
            fcm.unsubscribeFromTopic(topic).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unsubscribe from topic: $topic")
        }
    }

    fun buildConversationId(uid1: String, uid2: String): String {
        val sorted = listOf(uid1, uid2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    suspend fun sendMessage(message: Message): Resource<String> {
        return try {
            val convId = buildConversationId(message.senderId, message.receiverId)

            val msgRef = conversationsCollection
                .document(convId)
                .collection("messages")
                .document()
            val newMessage = message.copy(id = msgRef.id)
            msgRef.set(newMessage).await()

            conversationsCollection.document(convId).set(
                mapOf(
                    "lastMessage"   to message.content,
                    "lastTimestamp" to message.timestamp,
                    "participants"  to listOf(message.senderId, message.receiverId)
                ),
                SetOptions.merge()  // ✅ cleaned up import
            ).await()

            Resource.Success(msgRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send message")
        }
    }

    suspend fun markMessagesAsRead(conversationId: String, userId: String): Resource<Unit> {
        return try {
            val unreadMessages = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()

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