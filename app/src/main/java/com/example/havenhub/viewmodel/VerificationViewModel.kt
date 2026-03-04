package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerificationUiState(
    val isLoading: Boolean = false,
    val pendingUsers: List<User> = emptyList(),
    val pendingProperties: List<Property> = emptyList(),
    val actionSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    init { loadAllPending() }

    fun loadAllPending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val usersResult = adminRepository.getAllUsers()
            val propertiesResult = adminRepository.getPendingProperties()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    pendingUsers = if (usersResult is Resource.Success)
                        usersResult.data?.filter { !it.isVerified } ?: emptyList()
                    else emptyList(),
                    pendingProperties = if (propertiesResult is Resource.Success)
                        propertiesResult.data ?: emptyList()
                    else emptyList()
                )
            }
        }
    }

    fun approveUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            handleResult(adminRepository.verifyUser(userId))
        }
    }

    fun rejectUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            handleResult(adminRepository.banUser(userId))
        }
    }

    fun approveProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Fixed: Only passing propertyId as per your Repository
            handleResult(adminRepository.approveProperty(propertyId))
        }
    }

    fun rejectProperty(propertyId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Fixed: Matches (propertyId, reason) in your Repository
            handleResult(adminRepository.rejectProperty(propertyId, reason))
        }
    }

    private fun handleResult(result: Resource<Unit>) {
        if (result is Resource.Success) {
            _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
            loadAllPending()
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = (result as? Resource.Error)?.message) }
        }
    }

    fun resetActionState() {
        _uiState.update { it.copy(actionSuccess = false, errorMessage = null) }
    }
}