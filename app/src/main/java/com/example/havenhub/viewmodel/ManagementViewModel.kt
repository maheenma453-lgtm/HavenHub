package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManagementUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val properties: List<Property> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val actionSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagementUiState())
    val uiState: StateFlow<ManagementUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadAllUsers()
        loadAllProperties()
        loadAllBookings()
    }

    // ✅ Line 49 Fixed: Removed '?: emptyList()' because data is not nullable
    fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = adminRepository.getAllUsers()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(users = result.data, isLoading = false) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    // ✅ Line 71 Fixed: Direct assignment of result.data
    fun loadAllProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = adminRepository.getAllProperties()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(properties = result.data, isLoading = false) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    // ✅ Line 86 Fixed: Standardized with your Resource sealed class
    fun loadAllBookings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = adminRepository.getAllBookings()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(bookings = result.data, isLoading = false) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    // --- Actions (Ban/Unban/Cancel) ---

    fun banUser(userId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.banUser(userId)) { loadAllUsers() }
    }

    fun unbanUser(userId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.unbanUser(userId)) { loadAllUsers() }
    }

    fun removeProperty(propertyId: String, reason: String = "Removed by admin") = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.rejectProperty(propertyId, reason)) { loadAllProperties() }
    }

    fun cancelBooking(bookingId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.cancelBooking(bookingId)) { loadAllBookings() }
    }

    private fun handleActionResult(result: Resource<Unit>, onSuccess: () -> Unit) {
        _uiState.update { state ->
            when (result) {
                is Resource.Success -> {
                    onSuccess()
                    state.copy(isLoading = false, actionSuccess = true)
                }
                is Resource.Error -> state.copy(isLoading = false, errorMessage = result.message)
                is Resource.Loading -> state.copy(isLoading = true)
            }
        }
    }

    fun resetActionState() {
        _uiState.update { it.copy(actionSuccess = false, errorMessage = null) }
    }
}