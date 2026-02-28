package com.example.havenhub.repository

import com.example.havenhub.remote.FirebaseMessagingManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.data.Message
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val messagingManager: FirebaseMessagingManager,
    private val realtimeListener: FirebaseRealtimeListener
) {

    fun getConversationId(uid1: String, uid2: String): String {
        return messagingManager.buildConversationId(uid1, uid2)
    }

    fun observeMessages(conversationId: String): Flow<List<Message>> {
        return realtimeListener.listenToMessages(conversationId)
    }

    suspend fun sendMessage(message: Message): Resource<String> {
        return messagingManager.sendMessage(message)
    }

    suspend fun markMessagesAsRead(conversationId: String, userId: String): Resource<Unit> {
        return messagingManager.markMessagesAsRead(conversationId, userId)
    }
}