package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Message
import com.havenhub.data.repository.AuthRepository
import com.havenhub.data.repository.MessagingRepository
import com.havenhub.utils.Resource
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

    private val _conversations = MutableStateFlow<Resource<List<Message>>>(Resource.Idle())
    val conversations: StateFlow<Resource<List<Message>>> = _conversations.asStateFlow()

    private val _chatMessages = MutableStateFlow<Resource<List<Message>>>(Resource.Idle())
    val chatMessages: StateFlow<Resource<List<Message>>> = _chatMessages.asStateFlow()

    private val _sendMessageState = MutableStateFlow<Resource<Message>>(Resource.Idle())
    val sendMessageState: StateFlow<Resource<Message>> = _sendMessageState.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)

    fun loadConversations() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            _conversations.value = Resource.Loading()
            _conversations.value = messagingRepository.getConversations(userId)
        }
    }

    fun loadChatMessages(chatId: String) {
        _currentChatId.value = chatId
        viewModelScope.launch {
            _chatMessages.value = Resource.Loading()
            messagingRepository.listenToMessages(chatId).collect { messages ->
                _chatMessages.value = Resource.Success(messages)
            }
        }
    }

    fun sendMessage(
        receiverId: String,
        content: String,
        propertyId: String? = null
    ) {
        viewModelScope.launch {
            val senderId = authRepository.getCurrentUser()?.uid ?: return@launch
            _sendMessageState.value = Resource.Loading()
            _sendMessageState.value = messagingRepository.sendMessage(
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                propertyId = propertyId
            )
        }
    }

    fun markMessagesAsRead(chatId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            messagingRepository.markMessagesAsRead(chatId, userId)
        }
    }

    fun resetState() {
        _sendMessageState.value = Resource.Idle()
    }
}
