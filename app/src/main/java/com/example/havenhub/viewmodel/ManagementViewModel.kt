package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Booking
import com.havenhub.data.model.Property
import com.havenhub.data.model.User
import com.havenhub.data.repository.AdminRepository
import com.havenhub.utils.Resource
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

    private val _allUsers = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val allUsers: StateFlow<Resource<List<User>>> = _allUsers.asStateFlow()

    private val _allProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Idle())
    val allProperties: StateFlow<Resource<List<Property>>> = _allProperties.asStateFlow()

    private val _allBookings = MutableStateFlow<Resource<List<Booking>>>(Resource.Idle())
    val allBookings: StateFlow<Resource<List<Booking>>> = _allBookings.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val actionState: StateFlow<Resource<Boolean>> = _actionState.asStateFlow()

    fun loadAllUsers(searchQuery: String = "") {
        viewModelScope.launch {
            _allUsers.value = Resource.Loading()
            _allUsers.value = adminRepository.getAllUsers(searchQuery)
        }
    }

    fun loadAllProperties(searchQuery: String = "") {
        viewModelScope.launch {
            _allProperties.value = Resource.Loading()
            _allProperties.value = adminRepository.getAllProperties(searchQuery)
        }
    }

    fun loadAllBookings() {
        viewModelScope.launch {
            _allBookings.value = Resource.Loading()
            _allBookings.value = adminRepository.getAllBookings()
        }
    }

    fun banUser(userId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = adminRepository.banUser(userId)
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = adminRepository.unbanUser(userId)
        }
    }

    fun removeProperty(propertyId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = adminRepository.removeProperty(propertyId)
            if (_actionState.value is Resource.Success) loadAllProperties()
        }
    }

    fun cancelBookingAsAdmin(bookingId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = adminRepository.cancelBooking(bookingId)
            if (_actionState.value is Resource.Success) loadAllBookings()
        }
    }

    fun resetActionState() {
        _actionState.value = Resource.Idle()
    }
}
