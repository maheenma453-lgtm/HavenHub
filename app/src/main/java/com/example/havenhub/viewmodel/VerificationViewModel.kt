package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.data.User
import com.example.havenhub.data.VerificationStatus
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────

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

    // ─────────────────────────────────────────
    //  Load Pending Users + Properties
    // ─────────────────────────────────────────

    fun loadAllPending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val usersResult      = adminRepository.getAllUsers()
            val propertiesResult = adminRepository.getAllProperties()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,

                    // Resource<List<User>> — data is non-null List<User>
                    // filter by verificationStatus (not isVerified)
                    pendingUsers = when (usersResult) {
                        is Resource.Success -> usersResult.data.filter {
                            it.verificationStatus == VerificationStatus.PENDING ||
                                    it.verificationStatus == VerificationStatus.UNDER_REVIEW
                        }
                        else -> emptyList()
                    },

                    // getAllProperties() se PENDING/UNDER_REVIEW filter
                    pendingProperties = when (propertiesResult) {
                        is Resource.Success -> propertiesResult.data.filter {
                            it.status == PropertyStatus.PENDING ||
                                    it.status == PropertyStatus.UNDER_REVIEW
                        }
                        else -> emptyList()
                    }
                )
            }
        }
    }

    // ─────────────────────────────────────────
    //  User Actions
    // ─────────────────────────────────────────

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

    // ─────────────────────────────────────────
    //  Property Actions
    // ─────────────────────────────────────────

    fun rejectProperty(propertyId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            handleResult(adminRepository.rejectProperty(propertyId, reason))
        }
    }

    // ─────────────────────────────────────────
    //  Shared Result Handler
    // ─────────────────────────────────────────

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

    // ─────────────────────────────────────────
    //  Reset State
    // ─────────────────────────────────────────

    fun resetActionState() {
        _uiState.update { it.copy(actionSuccess = false, errorMessage = null) }
    }
}