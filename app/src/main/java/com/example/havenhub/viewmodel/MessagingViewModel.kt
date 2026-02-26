package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Message
import com.example.havenhub.repository.AuthRepository
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
    private val messagingRepository: MessagingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _chatMessages = MutableStateFlow<Resource<List<Message>>>(Resource.Loading)
    val chatMessages: StateFlow<Resource<List<Message>>> = _chatMessages.asStateFlow()

    // FIX: sendMessage() returns Resource<String> not Resource<Message>
    private val _sendMessageState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val sendMessageState: StateFlow<Resource<String>> = _sendMessageState.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)

    // FIX: getConversations() doesn't exist — use getConversationId() + observeMessages()
    fun loadChatMessages(uid1: String, uid2: String) {
        val conversationId = messagingRepository.getConversationId(uid1, uid2)
        _currentChatId.value = conversationId
        viewModelScope.launch {
            _chatMessages.value = Resource.Loading
            messagingRepository.observeMessages(conversationId).collect { messages ->
                _chatMessages.value = Resource.Success(messages)
            }
        }
    }

    // FIX: sendMessage() takes a Message object, not separate parameters
    fun sendMessage(message: Message) {
        viewModelScope.launch {
            _sendMessageState.value = Resource.Loading
            _sendMessageState.value = messagingRepository.sendMessage(message)
        }
    }

    fun markMessagesAsRead(conversationId: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            messagingRepository.markMessagesAsRead(conversationId, userId)
        }
    }

    fun resetState() {
        _sendMessageState.value = Resource.Loading
    }
}