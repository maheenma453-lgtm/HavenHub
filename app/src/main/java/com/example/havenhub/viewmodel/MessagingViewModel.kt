package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Message
import com.example.havenhub.repository.MessagingRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagingUiState(
    val isLoading     : Boolean                = false,
    val messages      : List<Message>          = emptyList(),
    val conversations : List<Map<String, Any>> = emptyList(),
    val unreadCount   : Int                    = 0,
    val errorMessage  : String?                = null,
    val sendSuccess   : Boolean                = false
)

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    private var currentUserId : String = ""
    private var messageJob    : Job?   = null
    private var convoJob      : Job?   = null

    fun initUserId(userId: String) {
        currentUserId = userId
    }

    fun loadConversations(userId: String) {
        if (userId.isEmpty()) return
        currentUserId = userId
        convoJob?.cancel()
        convoJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            messagingRepository.getConversationsRealtime(userId).collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading     = false,
                            conversations = result.data ?: emptyList()
                        )
                    }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    // ✅ FIX: otherUserId se chatId generate karo aur listen start karo
    fun loadChat(otherUserId: String) {
        if (currentUserId.isEmpty() || otherUserId.isEmpty()) return
        val chatId = messagingRepository.generateChatId(currentUserId, otherUserId)
        listenToMessages(chatId)
    }

    private fun listenToMessages(chatId: String) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            messagingRepository.getMessagesRealtime(chatId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading    = false,
                                messages     = result.data ?: emptyList(),
                                errorMessage = null
                            )
                        }
                        markAsRead(chatId, currentUserId)
                    }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun markAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            messagingRepository.markMessagesAsRead(chatId, userId)
        }
    }

    fun sendMessage(
        receiverId  : String,
        content     : String,
        // ✅ FIX: "TEXT" uppercase tha — Message.TYPE_TEXT = "text" lowercase use karo
        messageType : String  = Message.TYPE_TEXT,
        mediaUrl    : String? = null
    ) {
        if (content.isBlank() && mediaUrl == null) return
        // ✅ FIX: currentUserId empty check — agar empty hai to message send mat karo
        if (currentUserId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "User not logged in") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(sendSuccess = false) }
            val chatId = messagingRepository.generateChatId(currentUserId, receiverId)

            // ✅ FIX: pehle conversation banao, phir message bhejo
            messagingRepository.createOrGetConversation(currentUserId, receiverId)

            val result = messagingRepository.sendMessage(
                conversationId = chatId,
                senderId       = currentUserId,
                receiverId     = receiverId,
                content        = content,
                messageType    = messageType,
                mediaUrl       = mediaUrl
            )

            when (result) {
                is Resource.Success -> _uiState.update { it.copy(sendSuccess = true) }
                is Resource.Error   -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSendSuccess() {
        _uiState.update { it.copy(sendSuccess = false) }
    }

    override fun onCleared() {
        super.onCleared()
        messageJob?.cancel()
        convoJob?.cancel()
    }
}