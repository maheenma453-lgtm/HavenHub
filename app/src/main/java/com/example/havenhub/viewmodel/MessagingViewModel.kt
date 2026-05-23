package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Message
import com.example.havenhub.data.User
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.repository.MessagingRepository
import com.example.havenhub.repository.NotificationRepository  // ✦ NEW — for message notifications
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

// ══════════════════════════════════════════════════════════════════════════════
// MessagingViewModel.kt
//
// Manages all chat state: conversations list, messages in a chat,
// selection mode for delete, full chat delete, and read receipts.
//
// ✦ FIX — Messages notification was not being sent.
// Root cause: sendMessage() only saved the message to Firestore but never
// called NotificationRepository.sendNewMessageNotification() for the receiver.
// Fix: inject NotificationRepository and call it inside sendMessage() after
// a successful message save.
// ══════════════════════════════════════════════════════════════════════════════

data class MessagingUiState(
    val isLoading    : Boolean                = false,
    val messages     : List<Message>          = emptyList(),
    val conversations: List<Map<String, Any>> = emptyList(),
    val unreadCount  : Int                    = 0,
    val errorMessage : String?                = null,
    val sendSuccess  : Boolean                = false,

    // Selected messages delete state
    val isSelectionMode   : Boolean     = false,
    val selectedMessageIds: Set<String> = emptySet(),
    val isDeleting        : Boolean     = false,
    val deleteSuccess     : Boolean     = false,

    // Full chat delete state
    val isChatDeleting   : Boolean = false,
    val chatDeleteSuccess: Boolean = false,

    // Other user profile (for chat header — profile pic + role)
    val otherUserProfile  : User?   = null,

    // Online / Last seen presence
    val isOtherUserOnline : Boolean = false,
    val otherUserLastSeen : Long    = 0L
)

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messagingRepository     : MessagingRepository,
    private val notificationRepository  : NotificationRepository,   // ✦ NEW injection
    private val firebaseRealtimeListener: FirebaseRealtimeListener,
    private val firestore               : FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    private var currentUserId  : String = ""
    private var currentUserName: String = ""   // ✦ NEW — needed for notification title
    private var currentUserRole: String = ""   // ✦ NEW — needed for targetRole in notification
    private var currentChatId  : String = ""
    private var messageJob     : Job?   = null
    private var convoJob       : Job?   = null
    private var presenceJob    : Job?   = null

    // ─────────────────────────────────────────────────────────────────────────
    // initUserId
    //
    // Call this on ChatScreen launch. Sets the current user's ID and marks
    // them as online in Realtime Database for presence tracking.
    // ✦ UPDATED — now also accepts name and role for notification sending
    // ─────────────────────────────────────────────────────────────────────────
    fun initUserId(
        userId  : String,
        userName: String = "",   // ✦ NEW — shown in notification title "New Message from X"
        userRole: String = ""    // ✦ NEW — used as targetRole in Firestore notification
    ) {
        currentUserId   = userId
        currentUserName = userName
        currentUserRole = userRole
        // Mark this user as online in Realtime Database
        firebaseRealtimeListener.updateMyPresence(userId, isOnline = true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // setOffline — call from MainActivity/ChatScreen onStop
    // ─────────────────────────────────────────────────────────────────────────
    fun setOffline() {
        if (currentUserId.isNotEmpty()) {
            firebaseRealtimeListener.updateMyPresence(currentUserId, isOnline = false)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadOtherUserProfile
    //
    // Fetches the other user's Firestore profile for the chat header
    // (profile picture, role badge, etc.)
    // ─────────────────────────────────────────────────────────────────────────
    fun loadOtherUserProfile(otherUserId: String) {
        if (otherUserId.isEmpty()) return
        viewModelScope.launch {
            try {
                val doc  = firestore.collection("users").document(otherUserId).get().await()
                val user = doc.toObject(User::class.java)
                _uiState.update { it.copy(otherUserProfile = user) }
            } catch (e: Exception) {
                // Profile load failure is non-critical — name is already shown in header
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listenToOtherUserPresence — real-time online/offline status
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // loadConversations — real-time conversations list with unread badge count
    // ─────────────────────────────────────────────────────────────────────────
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
                        // Count conversations where this user has unread messages
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

    // ─────────────────────────────────────────────────────────────────────────
    // loadChat — called when opening a specific chat
    // Starts message listener + loads other user profile + presence
    // ─────────────────────────────────────────────────────────────────────────
    fun loadChat(otherUserId: String, propertyId: String = "") {
        if (currentUserId.isEmpty() || otherUserId.isEmpty()) return
        val chatId = messagingRepository.generateChatId(currentUserId, otherUserId)
        currentChatId = chatId
        listenToMessages(chatId)
        loadOtherUserProfile(otherUserId)
        listenToOtherUserPresence(otherUserId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listenToMessages — real-time message stream for open chat
    // ─────────────────────────────────────────────────────────────────────────
    private fun listenToMessages(chatId: String) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            messagingRepository.getMessagesRealtime(chatId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val incoming = result.data ?: emptyList()
                        val selected = _uiState.value.selectedMessageIds
                        // Re-apply selection state on new messages
                        val merged = incoming.map { msg ->
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

    // ─────────────────────────────────────────────────────────────────────────
    // deleteSelectedMessages — only sender can delete their own messages
    // ─────────────────────────────────────────────────────────────────────────
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
                is Resource.Error   -> _uiState.update {
                    it.copy(isDeleting = false, errorMessage = result.message)
                }
                else -> {}
            }
        }
    }

    // ── Full Chat Delete ──────────────────────────────────────────────────────

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
                    it.copy(
                        isChatDeleting = false,
                        errorMessage   = e.message ?: "Chat could not be deleted"
                    )
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
                _uiState.update { it.copy(errorMessage = "Could not delete: ${e.message}") }
            }
        }
    }

    // ── Mark as Read ──────────────────────────────────────────────────────────

    fun markAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            messagingRepository.markMessagesAsRead(chatId, userId)

            // Update local unread counter so badge clears immediately
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

    // ─────────────────────────────────────────────────────────────────────────
    // sendMessage
    //
    // ✦ FIX — Message notification was missing.
    //
    // Before this fix:
    //   sendMessage() saved the message to Firestore but never called
    //   NotificationRepository. So the receiver never got a NEW_MESSAGE
    //   notification in their notifications list.
    //
    // After this fix:
    //   On successful message save → call sendNewMessageNotification() for
    //   the receiver. The notification appears in their NotificationsScreen
    //   under "New Message from <senderName>".
    //
    // receiverRole is needed by NotificationRepository to set targetRole
    // correctly. It is passed in from ChatScreen where both users' roles
    // are known. Defaults to "tenant" if not provided.
    // ─────────────────────────────────────────────────────────────────────────
    fun sendMessage(
        receiverId  : String,
        content     : String,
        propertyId  : String  = "",
        messageType : String  = Message.TYPE_TEXT,
        mediaUrl    : String? = null,
        receiverRole: String  = "tenant"    // ✦ NEW — receiver's role for notification targetRole
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
                is Resource.Success -> {
                    _uiState.update { it.copy(sendSuccess = true) }

                    // ✦ FIX — Send in-app notification to the message receiver.
                    // This saves a NEW_MESSAGE notification document in Firestore
                    // under the receiver's notifications collection so it appears
                    // in their NotificationsScreen with the sender's name + preview.
                    //
                    // messagePreview: show first 60 chars of content so notification
                    // body is not too long. For media messages use a placeholder.
                    val senderDisplayName = currentUserName.ifEmpty { "Someone" }
                    val messagePreview    = when {
                        mediaUrl != null  -> "📎 Sent an attachment"
                        content.length > 60 -> content.take(60) + "..."
                        else              -> content
                    }

                    notificationRepository.sendNewMessageNotification(
                        recipientId    = receiverId,
                        senderName     = senderDisplayName,
                        messagePreview = messagePreview,
                        conversationId = chatId,
                        recipientRole  = receiverRole
                    )
                    // Note: notification send failure is intentionally ignored here —
                    // message was already saved successfully. A failed notification
                    // should not block or error the sender's UI.
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> {}
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun clearError()             { _uiState.update { it.copy(errorMessage = null) } }
    fun resetSendSuccess()       { _uiState.update { it.copy(sendSuccess = false) } }
    fun resetDeleteSuccess()     { _uiState.update { it.copy(deleteSuccess = false) } }
    fun resetChatDeleteSuccess() { _uiState.update { it.copy(chatDeleteSuccess = false) } }

    override fun onCleared() {
        super.onCleared()
        messageJob?.cancel()
        convoJob?.cancel()
        presenceJob?.cancel()
    }
}