package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.data.User
import com.example.havenhub.data.VerificationStatus
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.repository.AuthRepository
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
    private val adminRepository       : AdminRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository        : AuthRepository          // ✅ CNIC fetch ke liye
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    init { loadAllPending() }

    fun loadAllPending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val usersResult      = adminRepository.getAllUsers()
            val propertiesResult = adminRepository.getAllProperties()

            // ✅ FIX: Pending users ki full detail (cnicImageUrl samet) Firestore se fetch karo
            val pendingUsers: List<User> = when (usersResult) {
                is Resource.Success -> {
                    val filtered = usersResult.data.filter {
                        it.verificationStatus.equals(VerificationStatus.PENDING.name, ignoreCase = true) ||
                                it.verificationStatus.equals(VerificationStatus.UNDER_REVIEW.name, ignoreCase = true)
                    }
                    // Har user ki complete detail fetch karo taake cnicImageUrl aaye
                    filtered.map { basicUser ->
                        val fullResult = authRepository.getUser(basicUser.userId)
                        if (fullResult is Resource.Success) fullResult.data else basicUser
                    }
                }
                else -> emptyList()
            }

            val pendingProperties: List<Property> = when (propertiesResult) {
                is Resource.Success -> propertiesResult.data.filter {
                    it.status.equals(PropertyStatus.PENDING.name, ignoreCase = true) ||
                            it.status.equals(PropertyStatus.UNDER_REVIEW.name, ignoreCase = true)
                }
                else -> emptyList()
            }

            _uiState.update {
                it.copy(
                    isLoading         = false,
                    pendingUsers      = pendingUsers,
                    pendingProperties = pendingProperties
                )
            }
        }
    }

    // ✅ Approve property + landlord ko notification
    fun approveProperty(propertyId: String, adminNote: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = adminRepository.approveProperty(propertyId, adminNote)

            if (result is Resource.Success) {
                val property = _uiState.value.pendingProperties
                    .find { it.propertyId == propertyId }
                if (property != null) {
                    notificationRepository.sendPropertyApprovedNotification(
                        ownerId       = property.ownerId,
                        propertyId    = propertyId,
                        propertyTitle = property.title,
                        adminNote     = adminNote
                    )
                }
            }
            handleResult(result)
        }
    }

    // ✅ Reject property + landlord ko notification
    fun rejectProperty(propertyId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = adminRepository.rejectProperty(propertyId, reason)

            if (result is Resource.Success) {
                val property = _uiState.value.pendingProperties
                    .find { it.propertyId == propertyId }
                if (property != null) {
                    notificationRepository.sendPropertyRejectedNotification(
                        ownerId       = property.ownerId,
                        propertyId    = propertyId,
                        propertyTitle = property.title,
                        adminNote     = reason
                    )
                }
            }
            handleResult(result)
        }
    }

    // ✅ Approve user — verificationStatus bhi update karo
    fun approveUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = adminRepository.approveUser(userId)
            handleResult(result)
        }
    }

    // ✅ Reject/Ban user
    fun rejectUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = adminRepository.banUser(userId)
            handleResult(result)
        }
    }

    private fun handleResult(result: Resource<Unit>) {
        when (result) {
            is Resource.Success -> {
                _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                loadAllPending()
            }
            is Resource.Error -> _uiState.update {
                it.copy(isLoading = false, errorMessage = result.message)
            }
            is Resource.Loading -> Unit
        }
    }

    fun resetActionState() {
        _uiState.update { it.copy(actionSuccess = false, errorMessage = null) }
    }
}