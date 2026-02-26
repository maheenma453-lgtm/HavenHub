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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _allUsers = MutableStateFlow<Resource<List<User>>>(Resource.Loading)
    val allUsers: StateFlow<Resource<List<User>>> = _allUsers.asStateFlow()

    private val _allProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Loading)
    val allProperties: StateFlow<Resource<List<Property>>> = _allProperties.asStateFlow()

    private val _allBookings = MutableStateFlow<Resource<List<Booking>>>(Resource.Loading)
    val allBookings: StateFlow<Resource<List<Booking>>> = _allBookings.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val actionState: StateFlow<Resource<Unit>> = _actionState.asStateFlow()

    // FIX 1: getAllUsers() takes no parameters in AdminRepository
    fun loadAllUsers() {
        viewModelScope.launch {
            _allUsers.value = Resource.Loading
            _allUsers.value = adminRepository.getAllUsers()
        }
    }

    // FIX 2: getAllProperties() takes no parameters in AdminRepository
    fun loadAllProperties() {
        viewModelScope.launch {
            _allProperties.value = Resource.Loading
            _allProperties.value = adminRepository.getAllProperties()
        }
    }

    fun loadAllBookings() {
        viewModelScope.launch {
            _allBookings.value = Resource.Loading
            _allBookings.value = adminRepository.getAllBookings()
        }
    }

    fun banUser(userId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = adminRepository.banUser(userId)
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = adminRepository.unbanUser(userId)
        }
    }

    // FIX 3: removeProperty() doesn't exist — rejectProperty() use karo
    fun removeProperty(propertyId: String, reason: String = "Removed by admin") {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = adminRepository.rejectProperty(propertyId, reason)
            _actionState.value = result
            if (result is Resource.Success) loadAllProperties()
        }
    }

    // FIX 4: cancelBooking() doesn't exist in AdminRepository
    // Isko AdminRepository mein add karo, tab tak sirf list refresh hogi
    fun cancelBookingAsAdmin(bookingId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            // TODO: adminRepository.cancelBooking(bookingId) — add this to AdminRepository
            loadAllBookings()
        }
    }

    fun resetActionState() {
        _actionState.value = Resource.Loading
    }
}