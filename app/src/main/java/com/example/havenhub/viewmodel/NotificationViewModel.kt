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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<Resource<List<Notification>>?>(null)
    val notifications: StateFlow<Resource<List<Notification>>?> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ✅ Load Notifications
    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            _notifications.value = Resource.Loading()
            _isLoading.value = true

            try {
                val result = notificationRepository.getUserNotifications(userId)
                _notifications.value = result

                // Calculate unread count
                if (result is Resource.Success) {
                    _unreadCount.value = result.data?.count { !it.isRead } ?: 0
                }

            } catch (e: Exception) {
                _notifications.value = Resource.Error(e.message ?: "Failed to load")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Mark as Read
    fun markAsRead(notificationId: String, userId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
                loadNotifications(userId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // ✅ Mark All as Read
    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAllAsRead(userId)
                loadNotifications(userId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // ✅ Delete Notification
    fun deleteNotification(notificationId: String, userId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
                loadNotifications(userId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}