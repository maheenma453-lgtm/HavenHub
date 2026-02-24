package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Notification
import com.havenhub.data.repository.AuthRepository
import com.havenhub.data.repository.NotificationRepository
import com.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<Resource<List<Notification>>>(Resource.Idle())
    val notifications: StateFlow<Resource<List<Notification>>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            _notifications.value = Resource.Loading()
            val result = notificationRepository.getNotifications(userId)
            _notifications.value = result
            if (result is Resource.Success) {
                _unreadCount.value = result.data?.count { !it.isRead } ?: 0
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
            loadNotifications()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            notificationRepository.markAllAsRead(userId)
            _unreadCount.value = 0
            loadNotifications()
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
            loadNotifications()
        }
    }
}
