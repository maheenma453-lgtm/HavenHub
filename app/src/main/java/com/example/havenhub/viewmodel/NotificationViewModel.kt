package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Notification
import com.example.havenhub.repository.NotificationRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading    : Boolean            = false,
    val notifications: List<Notification> = emptyList(),
    val unreadCount  : Int                = 0,
    val errorMessage : String?            = null,
    val actionSuccess: Boolean            = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // ── FIX: Tracks if we are already listening — prevents duplicate flows ──
    // Without this guard, every time a dashboard recomposed it started a NEW
    // listener, so unreadCount jumped erratically and badge showed wrong count.
    private var listeningUserId: String? = null

    /**
     * Call this from ANY dashboard (Admin, Landlord, Tenant) after login.
     * Safe to call multiple times — starts real-time listener only once per userId.
     *
     * Usage in every dashboard TopAppBar / init block:
     *   notificationViewModel.startListening(currentUserId)
     */
    fun startListening(userId: String) {
        if (listeningUserId == userId) return   // already listening, skip
        listeningUserId = userId
        observeNotifications(userId)
    }

    /**
     * Real-time Firestore Flow observer.
     * Firestore change hote hi unreadCount + badge instantly update hoga.
     */
    private fun observeNotifications(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            notificationRepository.observeNotifications(userId)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message)
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            isLoading     = false,
                            notifications = list,
                            unreadCount   = list.count { n -> !n.isRead }
                        )
                    }
                }
        }
    }

    /**
     * One-time fetch — use only when real-time is not needed
     * (e.g. a static report screen).
     */
    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = notificationRepository.getUserNotifications(userId)) {
                is Resource.Success -> {
                    val list = result.data
                    _uiState.update {
                        it.copy(
                            isLoading     = false,
                            notifications = list,
                            unreadCount   = list.count { n -> !n.isRead }
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                Resource.Loading  -> Unit
            }
        }
    }

    fun markAsRead(notificationId: String, userId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
                _uiState.update { state ->
                    val updated = state.notifications.map { n ->
                        if (n.notificationId == notificationId) n.copy(isRead = true) else n
                    }
                    state.copy(
                        notifications = updated,
                        unreadCount   = updated.count { !it.isRead }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAllAsRead(userId)
                _uiState.update { state ->
                    state.copy(
                        notifications = state.notifications.map { it.copy(isRead = true) },
                        unreadCount   = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteNotification(notificationId: String, userId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
                _uiState.update { state ->
                    val updated = state.notifications.filter {
                        it.notificationId != notificationId
                    }
                    state.copy(
                        notifications = updated,
                        unreadCount   = updated.count { !it.isRead }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}
