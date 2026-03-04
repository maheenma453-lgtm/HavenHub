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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Unified UI State ─────────────────────────────────────────────
data class MessagingUiState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null,
    val sendSuccess: Boolean = false
)

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    // Current User ID (Aap isse AuthRepository se bhi le sakte hain)
    private var currentUserId: String = ""

    fun initUserId(userId: String) {
        currentUserId = userId
    }

    // ✅ Load Chat & Listen Real-time (image_0413e4.png fix)
    fun loadChat(otherUserId: String) {
        val chatId = messagingRepository.generateChatId(currentUserId, otherUserId)
        listenToMessages(chatId, currentUserId)
    }

    // ✅ Listen to Messages
    fun listenToMessages(chatId: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            messagingRepository.getMessagesRealtime(chatId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                messages = result.data ?: emptyList()
                            )
                        }
                        // Mark as read automatically
                        markAsRead(chatId, userId)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    // ✅ Mark Messages as Read (image_0413e4.png fix)
    fun markAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            messagingRepository.markMessagesAsRead(chatId, userId)
        }
    }

    // ✅ Send Message (Text & Image combined)
    fun sendMessage(
        receiverId: String,
        content: String,
        messageType: String = Message.TYPE_TEXT,
        mediaUrl: String? = null
    ) {
        if (content.isBlank() && mediaUrl == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(sendSuccess = false) }
            val chatId = messagingRepository.generateChatId(currentUserId, receiverId)

            // Conversation ensure karein
            messagingRepository.createOrGetConversation(currentUserId, receiverId)

            val result = messagingRepository.sendMessage(
                conversationId = chatId,
                senderId = currentUserId,
                receiverId = receiverId,
                content = content,
                messageType = messageType,
                mediaUrl = mediaUrl
            )

            if (result is Resource.Success) {
                _uiState.update { it.copy(sendSuccess = true) }
            } else if (result is Resource.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    // ✅ Clear States
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSendSuccess() {
        _uiState.update { it.copy(sendSuccess = false) }
    }
}