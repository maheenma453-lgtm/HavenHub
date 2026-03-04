package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.User
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val actionSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataManager: FirebaseDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    // Load Profile
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val uid = authRepository.currentUser?.uid
            if (uid != null) {
                when (val result = dataManager.getUser(uid)) {
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, user = result.data)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    Resource.Loading -> Unit
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "User not found")
                }
            }
        }
    }

    // Update Profile
    fun updateProfile(
        fullName   : String,
        phoneNumber: String,
        city       : String
    ) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val fields = mapOf(
                "fullName"    to fullName,
                "phoneNumber" to phoneNumber,
                "location"    to mapOf("city" to city)
            )
            when (val result = dataManager.updateUserFields(uid, fields)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading     = false,
                            actionSuccess = true,
                            user          = it.user?.copy(
                                fullName    = fullName,
                                phoneNumber = phoneNumber
                            )
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    // Clear Messages
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}