package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Message
import com.example.havenhub.repository.MessagingRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<Resource<List<Message>>?>(null)
    val messages: StateFlow<Resource<List<Message>>?> = _messages.asStateFlow()

    private val _sendMessageState = MutableStateFlow<Resource<Message>?>(null)
    val sendMessageState: StateFlow<Resource<Message>?> = _sendMessageState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ✅ Listen to Messages (Real-time)
    fun listenToMessages(chatId: String, currentUserId: String) {
        viewModelScope.launch {
            _messages.value = Resource.Loading()

            try {
                messagingRepository.getMessagesRealtime(chatId).collect { result ->
                    _messages.value = result

                    // Mark messages as read
                    if (result is Resource.Success) {
                        messagingRepository.markMessagesAsRead(chatId, currentUserId)
                    }
                }
            } catch (e: Exception) {
                _messages.value = Resource.Error(e.message ?: "Failed to load messages")
                _errorMessage.value = e.message
            }
        }
    }

    // ✅ Load Unread Count
    fun loadUnreadCount(userId: String) {
        viewModelScope.launch {
            try {
                _unreadCount.value = messagingRepository.getUnreadMessagesCount(userId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // ✅ Send Text Message
    fun sendMessage(
        senderId: String,
        receiverId: String,
        content: String
    ) {
        if (content.isBlank()) {
            _errorMessage.value = "Message cannot be empty"
            return
        }

        viewModelScope.launch {
            _sendMessageState.value = Resource.Loading()

            try {
                // Generate conversation ID
                val conversationId = messagingRepository.generateChatId(senderId, receiverId)

                // Ensure conversation exists
                messagingRepository.createOrGetConversation(senderId, receiverId)

                // Send message
                val result = messagingRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = content,
                    messageType = Message.TYPE_TEXT
                )

                when (result) {
                    is Resource.Success -> {
                        _sendMessageState.value = Resource.Success(result.data!!)
                    }
                    is Resource.Error -> {
                        _sendMessageState.value = Resource.Error(result.message ?: "Failed to send")
                        _errorMessage.value = result.message
                    }
                    else -> {}
                }

            } catch (e: Exception) {
                _sendMessageState.value = Resource.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message
            }
        }
    }

    // ✅ Send Image Message
    fun sendImageMessage(
        senderId: String,
        receiverId: String,
        imageUrl: String,
        caption: String = ""
    ) {
        viewModelScope.launch {
            _sendMessageState.value = Resource.Loading()

            try {
                val conversationId = messagingRepository.generateChatId(senderId, receiverId)
                messagingRepository.createOrGetConversation(senderId, receiverId)

                val result = messagingRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = caption,
                    messageType = Message.TYPE_IMAGE,
                    mediaUrl = imageUrl
                )

                when (result) {
                    is Resource.Success -> {
                        _sendMessageState.value = Resource.Success(result.data!!)
                    }
                    is Resource.Error -> {
                        _sendMessageState.value = Resource.Error(result.message ?: "Failed to send")
                        _errorMessage.value = result.message
                    }
                    else -> {}
                }

            } catch (e: Exception) {
                _sendMessageState.value = Resource.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message
            }
        }
    }

    // ✅ Delete Chat
    fun deleteConversation(chatId: String) {
        viewModelScope.launch {
            try {
                messagingRepository.deleteConversation(chatId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // ✅ Clear States
    fun clearError() {
        _errorMessage.value = null
    }

    fun resetSendMessageState() {
        _sendMessageState.value = null
    }
}
