package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Message
import com.example.havenhub.data.User
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.repository.MessagingRepository
import com.example.havenhub.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class MessagingUiState(
    val isLoading    : Boolean                = false,
    val messages     : List<Message>          = emptyList(),
    val conversations: List<Map<String, Any>> = emptyList(),
    val unreadCount  : Int                    = 0,
    val errorMessage : String?                = null,
    val sendSuccess  : Boolean                = false,

    // Selected messages delete state
    val isSelectionMode   : Boolean      = false,
    val selectedMessageIds: Set<String>  = emptySet(),
    val isDeleting        : Boolean      = false,
    val deleteSuccess     : Boolean      = false,

    // Full chat delete state
    val isChatDeleting    : Boolean      = false,
    val chatDeleteSuccess : Boolean      = false,

    // ✦ NEW — Other user profile (header mein profile pic + role ke liye)
    val otherUserProfile  : User?        = null,

    // ✦ NEW — Online / Last seen presence
    val isOtherUserOnline : Boolean      = false,
    val otherUserLastSeen : Long         = 0L
)

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messagingRepository   : MessagingRepository,
    private val firebaseRealtimeListener: FirebaseRealtimeListener,
    private val firestore             : FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    private var currentUserId : String = ""
    private var currentChatId : String = ""
    private var messageJob    : Job?   = null
    private var convoJob      : Job?   = null
    private var presenceJob   : Job?   = null  // ✦ NEW

    fun initUserId(userId: String) {
        currentUserId = userId
        // ✦ NEW — Apni presence online set karo
        firebaseRealtimeListener.updateMyPresence(userId, isOnline = true)
    }

    // ✦ NEW — App background/close hone pe call karo (MainActivity ya ChatScreen onStop mein)
    fun setOffline() {
        if (currentUserId.isNotEmpty()) {
            firebaseRealtimeListener.updateMyPresence(currentUserId, isOnline = false)
        }
    }

    // ✦ NEW — Other user ka profile Firestore se fetch karo
    fun loadOtherUserProfile(otherUserId: String) {
        if (otherUserId.isEmpty()) return
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(otherUserId).get().await()
                val user = doc.toObject(User::class.java)
                _uiState.update { it.copy(otherUserProfile = user) }
            } catch (e: Exception) {
                // Profile load fail hone pe silently ignore karo — naam already ChatScreen mein hai
            }
        }
    }

    // ✦ NEW — Other user ki real-time presence listen karo
    private fun listenToOtherUserPresence(otherUserId: String) {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            firebaseRealtimeListener.listenToUserPresence(otherUserId).collect { presence ->
                _uiState.update {
                    it.copy(
                        isOtherUserOnline = presence.isOnline,
                        otherUserLastSeen = presence.lastSeen
                    )
                }
            }
        }
    }

    fun loadConversations(userId: String) {
        if (userId.isEmpty()) return
        currentUserId = userId
        convoJob?.cancel()
        convoJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            messagingRepository.getConversationsRealtime(userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val convos = result.data ?: emptyList()
                        val unreadConversationCount = convos.count { convo ->
                            val perUserUnread = (convo["unreadCount_$userId"] as? Long)?.toInt() ?: 0
                            perUserUnread > 0
                        }
                        _uiState.update {
                            it.copy(
                                isLoading     = false,
                                conversations = convos,
                                unreadCount   = unreadConversationCount
                            )
                        }
                    }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun loadChat(otherUserId: String, propertyId: String = "") {
        if (currentUserId.isEmpty() || otherUserId.isEmpty()) return
        val chatId = messagingRepository.generateChatId(currentUserId, otherUserId)
        currentChatId = chatId
        listenToMessages(chatId)
        // ✦ NEW — Other user ka profile + presence load karo
        loadOtherUserProfile(otherUserId)
        listenToOtherUserPresence(otherUserId)
    }

    private fun listenToMessages(chatId: String) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            messagingRepository.getMessagesRealtime(chatId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val incoming = result.data ?: emptyList()
                        val selected = _uiState.value.selectedMessageIds
                        val merged   = incoming.map { msg ->
                            msg.copy(isSelected = msg.id in selected)
                        }
                        _uiState.update {
                            it.copy(isLoading = false, messages = merged, errorMessage = null)
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

    // ── Selection Mode ────────────────────────────────────────────────────────

    fun onMessageLongPress(messageId: String) {
        _uiState.update { state ->
            val newSelected = state.selectedMessageIds + messageId
            state.copy(
                isSelectionMode    = true,
                selectedMessageIds = newSelected,
                messages           = state.messages.map { msg ->
                    msg.copy(isSelected = msg.id in newSelected)
                }
            )
        }
    }

    fun onMessageTap(messageId: String) {
        if (!_uiState.value.isSelectionMode) return
        _uiState.update { state ->
            val newSelected = if (messageId in state.selectedMessageIds)
                state.selectedMessageIds - messageId
            else
                state.selectedMessageIds + messageId
            state.copy(
                isSelectionMode    = newSelected.isNotEmpty(),
                selectedMessageIds = newSelected,
                messages           = state.messages.map { msg ->
                    msg.copy(isSelected = msg.id in newSelected)
                }
            )
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            state.copy(
                isSelectionMode    = false,
                selectedMessageIds = emptySet(),
                messages           = state.messages.map { it.copy(isSelected = false) }
            )
        }
    }

    fun selectAllMessages() {
        _uiState.update { state ->
            val allIds = state.messages.map { it.id }.toSet()
            state.copy(
                isSelectionMode    = true,
                selectedMessageIds = allIds,
                messages           = state.messages.map { it.copy(isSelected = true) }
            )
        }
    }

    fun deleteSelectedMessages() {
        val chatId   = currentChatId
        val myMsgIds = _uiState.value.messages
            .filter { it.id in _uiState.value.selectedMessageIds && it.senderId == currentUserId }
            .map    { it.id }

        if (myMsgIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "You can only delete your own messages") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val result = messagingRepository.deleteMessages(chatId = chatId, messageIds = myMsgIds)
            when (result) {
                is Resource.Success -> _uiState.update { state ->
                    state.copy(
                        isDeleting         = false,
                        isSelectionMode    = false,
                        selectedMessageIds = emptySet(),
                        deleteSuccess      = true,
                        messages           = state.messages
                            .filter { it.id !in myMsgIds }
                            .map    { it.copy(isSelected = false) }
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isDeleting = false, errorMessage = result.message)
                }
                else -> {}
            }
        }
    }

    // ── Full Chat Delete ───────────────────────────────────────────────────────

    fun deleteEntireChat(otherUserId: String) {
        val chatId = if (currentChatId.isNotEmpty()) currentChatId
        else messagingRepository.generateChatId(currentUserId, otherUserId)

        viewModelScope.launch {
            _uiState.update { it.copy(isChatDeleting = true) }
            try {
                messagingRepository.deleteConversation(chatId)
                messageJob?.cancel()
                _uiState.update { it.copy(isChatDeleting = false, chatDeleteSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isChatDeleting = false, errorMessage = e.message ?: "Chat could not be deleted")
                }
            }
        }
    }

    fun deleteConversation(conversationId: String, currentUid: String) {
        viewModelScope.launch {
            try {
                messagingRepository.deleteConversation(conversationId)
                _uiState.update { state ->
                    state.copy(
                        conversations = state.conversations.filter { convo ->
                            val cid = (convo["conversationId"] as? String)
                                ?: (convo["id"] as? String) ?: ""
                            cid != conversationId
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Could not delete: ${e.message}")
                }
            }
        }
    }

    // ── Mark as Read ─────────────────────────────────────────────────────────

    fun markAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            messagingRepository.markMessagesAsRead(chatId, userId)

            val updatedConvos = _uiState.value.conversations.map { convo ->
                val cid = (convo["conversationId"] as? String)
                    ?: (convo["id"] as? String) ?: ""
                if (cid == chatId)
                    convo.toMutableMap().also { it["unreadCount_$userId"] = 0L }
                else
                    convo
            }

            val newUnread = updatedConvos.count { convo ->
                ((convo["unreadCount_$userId"] as? Long)?.toInt() ?: 0) > 0
            }

            _uiState.update { it.copy(conversations = updatedConvos, unreadCount = newUnread) }
        }
    }

    fun sendMessage(
        receiverId : String,
        content    : String,
        propertyId : String  = "",
        messageType: String  = Message.TYPE_TEXT,
        mediaUrl   : String? = null
    ) {
        if (content.isBlank() && mediaUrl == null) return
        if (currentUserId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "User not logged in") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(sendSuccess = false) }
            val chatId = messagingRepository.generateChatId(currentUserId, receiverId)
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

    fun clearError()             { _uiState.update { it.copy(errorMessage = null) } }
    fun resetSendSuccess()       { _uiState.update { it.copy(sendSuccess = false) } }
    fun resetDeleteSuccess()     { _uiState.update { it.copy(deleteSuccess = false) } }
    fun resetChatDeleteSuccess() { _uiState.update { it.copy(chatDeleteSuccess = false) } }

    override fun onCleared() {
        super.onCleared()
        messageJob?.cancel()
        convoJob?.cancel()
        presenceJob?.cancel()  // ✦ NEW
    }
}