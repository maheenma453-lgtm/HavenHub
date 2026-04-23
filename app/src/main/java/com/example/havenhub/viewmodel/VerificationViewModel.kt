package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.data.User
import com.example.havenhub.data.VerificationStatus
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.repository.NotificationRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerificationUiState(
    val isLoading         : Boolean        = false,
    val pendingUsers      : List<User>     = emptyList(),
    val pendingProperties : List<Property> = emptyList(),
    val actionSuccess     : Boolean        = false,
    val errorMessage      : String?        = null
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val adminRepository        : AdminRepository,
    private val notificationRepository : NotificationRepository  // ✅ NEW: Inject karo
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    init { loadAllPending() }

    fun loadAllPending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val usersResult      = adminRepository.getAllUsers()
            val propertiesResult = adminRepository.getAllProperties()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    pendingUsers = when (usersResult) {
                        is Resource.Success -> usersResult.data.filter {
                            it.verificationStatus == VerificationStatus.PENDING.name ||
                                    it.verificationStatus == VerificationStatus.UNDER_REVIEW.name
                        }
                        else -> emptyList()
                    },
                    pendingProperties = when (propertiesResult) {
                        is Resource.Success -> propertiesResult.data.filter {
                            it.status == PropertyStatus.PENDING.name ||
                                    it.status == PropertyStatus.UNDER_REVIEW.name
                        }
                        else -> emptyList()
                    }
                )
            }
        }
    }

    // ✅ UPDATED: Approve ke baad landlord ko notification bhejo adminNote ke saath
    fun approveProperty(propertyId: String, adminNote: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = adminRepository.approveProperty(propertyId, adminNote)

            if (result is Resource.Success) {
                // ✅ Property ka owner aur title dhundo — notification ke liye
                val property = _uiState.value.pendingProperties
                    .find { it.propertyId == propertyId }

                if (property != null) {
                    notificationRepository.sendPropertyApprovedNotification(
                        ownerId       = property.ownerId,
                        propertyId    = propertyId,
                        propertyTitle = property.title,
                        adminNote     = adminNote      // ✅ Admin ka note landlord ko jayega
                    )
                }
            }

            handleResult(result)
        }
    }

    // ✅ UPDATED: Reject ke baad landlord ko notification bhejo reason ke saath
    fun rejectProperty(propertyId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = adminRepository.rejectProperty(propertyId, reason)

            if (result is Resource.Success) {
                // ✅ Property ka owner aur title dhundo — notification ke liye
                val property = _uiState.value.pendingProperties
                    .find { it.propertyId == propertyId }

                if (property != null) {
                    notificationRepository.sendPropertyRejectedNotification(
                        ownerId       = property.ownerId,
                        propertyId    = propertyId,
                        propertyTitle = property.title,
                        adminNote     = reason         // ✅ Reject reason landlord ko jayega
                    )
                }
            }

            handleResult(result)
        }
    }

    fun approveUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            handleResult(adminRepository.unbanUser(userId))
        }
    }

    fun rejectUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            handleResult(adminRepository.banUser(userId))
        }
    }

    private fun handleResult(result: Resource<Unit>) {
        when (result) {
            is Resource.Success -> {
                _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                loadAllPending()
            }
            is Resource.Error -> {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
            is Resource.Loading -> Unit
        }
    }

    fun resetActionState() {
        _uiState.update { it.copy(actionSuccess = false, errorMessage = null) }
    }
}