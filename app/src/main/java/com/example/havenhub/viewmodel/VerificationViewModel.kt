package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.repository.NotificationRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _pendingProperties = MutableStateFlow<Resource<List<Property>>?>(null)
    val pendingProperties: StateFlow<Resource<List<Property>>?> = _pendingProperties.asStateFlow()

    private val _pendingUsers = MutableStateFlow<Resource<List<User>>?>(null)
    val pendingUsers: StateFlow<Resource<List<User>>?> = _pendingUsers.asStateFlow()

    private val _verifyPropertyState = MutableStateFlow<Resource<Boolean>?>(null)
    val verifyPropertyState: StateFlow<Resource<Boolean>?> = _verifyPropertyState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPendingVerifications()
    }

    // ✅ Load Pending Properties
    fun loadPendingVerifications() {
        viewModelScope.launch {
            _pendingProperties.value = Resource.Loading()
            _isLoading.value = true

            try {
                val result = adminRepository.getPendingProperties()
                _pendingProperties.value = result
            } catch (e: Exception) {
                _pendingProperties.value = Resource.Error(e.message ?: "Failed to load")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Load Pending Users
    fun loadPendingUsers() {
        viewModelScope.launch {
            _pendingUsers.value = Resource.Loading()
            _isLoading.value = true

            try {
                val result = adminRepository.getPendingUsers()
                _pendingUsers.value = result
            } catch (e: Exception) {
                _pendingUsers.value = Resource.Error(e.message ?: "Failed to load")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Approve Property
    fun approveProperty(propertyId: String, ownerId: String) {
        viewModelScope.launch {
            _verifyPropertyState.value = Resource.Loading()
            _isLoading.value = true

            try {
                val result = adminRepository.updatePropertyVerification(propertyId, "APPROVED")

                when (result) {
                    is Resource.Success -> {
                        _verifyPropertyState.value = Resource.Success(true)

                        // Send notification to owner
                        notificationRepository.sendVerificationNotification(
                            userId = ownerId,
                            propertyId = propertyId,
                            status = "APPROVED",
                            message = "Your property has been approved"
                        )

                        // Reload pending list
                        loadPendingVerifications()
                    }
                    is Resource.Error -> {
                        _verifyPropertyState.value = Resource.Error(result.message ?: "Approval failed")
                        _errorMessage.value = result.message
                    }
                    else -> {}
                }

            } catch (e: Exception) {
                _verifyPropertyState.value = Resource.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Reject Property
    fun rejectProperty(propertyId: String, ownerId: String, reason: String) {
        viewModelScope.launch {
            _verifyPropertyState.value = Resource.Loading()
            _isLoading.value = true

            try {
                val result = adminRepository.updatePropertyVerification(propertyId, "REJECTED")

                when (result) {
                    is Resource.Success -> {
                        _verifyPropertyState.value = Resource.Success(true)

                        // Send notification to owner with reason
                        notificationRepository.sendVerificationNotification(
                            userId = ownerId,
                            propertyId = propertyId,
                            status = "REJECTED",
                            message = "Property rejected: $reason"
                        )

                        // Reload pending list
                        loadPendingVerifications()
                    }
                    is Resource.Error -> {
                        _verifyPropertyState.value = Resource.Error(result.message ?: "Rejection failed")
                        _errorMessage.value = result.message
                    }
                    else -> {}
                }

            } catch (e: Exception) {
                _verifyPropertyState.value = Resource.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Verify User
    fun verifyUser(userId: String, isApproved: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val status = if (isApproved) "VERIFIED" else "REJECTED"
                adminRepository.updateUserVerification(userId, status)

                loadPendingUsers()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetVerifyPropertyState() {
        _verifyPropertyState.value = null
    }
}