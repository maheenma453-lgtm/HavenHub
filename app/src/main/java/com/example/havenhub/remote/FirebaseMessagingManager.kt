package com.example.havenhub.remote

import android.util.Log
import com.example.havenhub.data.Message
import com.example.havenhub.utils.Constants
import com.example.havenhub.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMessagingManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val fcm      : FirebaseMessaging
) {
    private val usersCol         = firestore.collection("users")
    private val conversationsCol = firestore.collection("conversations")

    // ── FCM Token ─────────────────────────────────────────────────────────────
    suspend fun getDeviceToken(): Resource<String> {
        return try {
            val token = fcm.token.await()
            Resource.Success(token)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to get FCM token")
        }
    }

    /**
     * ✅ FIX: Token save karo + role ke hisaab se topic subscribe karo.
     * Pehle sirf token save hota tha, topic subscription missing thi —
     * isliye FCM push notifications deliver nahi hoti thin.
     */
    suspend fun saveDeviceTokenAndSubscribe(
        userId: String,
        token : String,
        role  : String   // "ADMIN" | "LANDLORD" | "TENANT"
    ): Resource<Unit> {
        return try {
            // Step 1: Firestore mein token save karo
            usersCol.document(userId)
                .update("fcmToken", token)
                .await()

            // Step 2: Role topic subscribe karo
            val roleTopic = when (role.uppercase()) {
                Constants.ROLE_ADMIN    -> Constants.TOPIC_ADMIN
                Constants.ROLE_LANDLORD -> Constants.TOPIC_LANDLORD
                else                    -> Constants.TOPIC_TENANT
            }
            fcm.subscribeToTopic(roleTopic).await()
            fcm.subscribeToTopic(Constants.TOPIC_ALL).await()   // sabko

            Log.d("FCM_MGR", "✅ Token saved & subscribed to $roleTopic + ${Constants.TOPIC_ALL}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("FCM_MGR", "saveDeviceTokenAndSubscribe error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to save FCM token")
        }
    }

    // Legacy — backward compat ke liye rakhha
    suspend fun saveDeviceToken(userId: String, token: String): Resource<Unit> {
        return try {
            usersCol.document(userId).update("fcmToken", token).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save FCM token")
        }
    }

    suspend fun clearDeviceToken(userId: String): Resource<Unit> {
        return try {
            // Token clear + topics unsubscribe
            usersCol.document(userId).update("fcmToken", null).await()
            fcm.unsubscribeFromTopic(Constants.TOPIC_ADMIN).await()
            fcm.unsubscribeFromTopic(Constants.TOPIC_LANDLORD).await()
            fcm.unsubscribeFromTopic(Constants.TOPIC_TENANT).await()
            fcm.unsubscribeFromTopic(Constants.TOPIC_ALL).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to clear FCM token")
        }
    }

    // ── Topics ────────────────────────────────────────────────────────────────
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

    // ── Messaging ─────────────────────────────────────────────────────────────
    fun buildConversationId(uid1: String, uid2: String): String {
        val sorted = listOf(uid1, uid2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    suspend fun sendMessage(message: Message): Resource<String> {
        return try {
            val convId = buildConversationId(message.senderId, message.receiverId)
            val msgRef = conversationsCol
                .document(convId)
                .collection("messages")
                .document()
            val newMessage = message.copy(id = msgRef.id)
            msgRef.set(newMessage).await()

            conversationsCol.document(convId).set(
                mapOf(
                    "lastMessage"   to message.content,
                    "lastTimestamp" to message.timestamp,
                    "participants"  to listOf(message.senderId, message.receiverId)
                ),
                SetOptions.merge()
            ).await()

            Resource.Success(msgRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send message")
        }
    }

    suspend fun markMessagesAsRead(conversationId: String, userId: String): Resource<Unit> {
        return try {
            val unread = conversationsCol
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", userId)
                .get().await()

            val batch = firestore.batch()
            unread.documents.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark messages as read")
        }
    }
}