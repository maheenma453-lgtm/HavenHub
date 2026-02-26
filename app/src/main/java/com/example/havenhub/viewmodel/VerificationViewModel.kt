package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyVerification
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
class VerificationViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _pendingProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Loading)
    val pendingProperties: StateFlow<Resource<List<Property>>> = _pendingProperties.asStateFlow()

    // FIX: getPendingUsers() doesn't exist — use getAllUsers() and filter unverified
    private val _pendingUsers = MutableStateFlow<Resource<List<User>>>(Resource.Loading)
    val pendingUsers: StateFlow<Resource<List<User>>> = _pendingUsers.asStateFlow()

    // FIX: getPendingVerifications() returns List<PropertyVerification>, not single item
    private val _pendingVerifications = MutableStateFlow<Resource<List<PropertyVerification>>>(Resource.Loading)
    val pendingVerifications: StateFlow<Resource<List<PropertyVerification>>> = _pendingVerifications.asStateFlow()

    // FIX: actionState is Resource<Unit> not Resource<Boolean>
    private val _actionState = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val actionState: StateFlow<Resource<Unit>> = _actionState.asStateFlow()

    init {
        loadPendingProperties()
        loadPendingUsers()
        loadPendingVerifications()
    }

    fun loadPendingProperties() {
        viewModelScope.launch {
            _pendingProperties.value = Resource.Loading
            _pendingProperties.value = adminRepository.getPendingProperties()
        }
    }

    // FIX: getPendingUsers() doesn't exist — filter from getAllUsers()
    fun loadPendingUsers() {
        viewModelScope.launch {
            _pendingUsers.value = Resource.Loading
            val result = adminRepository.getAllUsers()
            if (result is Resource.Error) {
                _pendingUsers.value = Resource.Error(result.message)
                return@launch
            }
            val unverified = (result as Resource.Success).data.filter { !it.isVerified }
            _pendingUsers.value = Resource.Success(unverified)
        }
    }

    // FIX: getPropertyVerificationDetail() doesn't exist — use getPendingVerifications()
    fun loadPendingVerifications() {
        viewModelScope.launch {
            _pendingVerifications.value = Resource.Loading
            _pendingVerifications.value = adminRepository.getPendingVerifications()
        }
    }

    fun approveProperty(propertyId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = adminRepository.approveProperty(propertyId)
            _actionState.value = result
            if (result is Resource.Success) loadPendingProperties()
        }
    }

    fun rejectProperty(propertyId: String, reason: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = adminRepository.rejectProperty(propertyId, reason)
            _actionState.value = result
            if (result is Resource.Success) loadPendingProperties()
        }
    }

    fun verifyUser(userId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = adminRepository.verifyUser(userId)
            _actionState.value = result
            if (result is Resource.Success) loadPendingUsers()
        }
    }

    // FIX: rejectUser() doesn't exist in AdminRepository — use banUser() instead
    fun rejectUser(userId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = adminRepository.banUser(userId)
            _actionState.value = result
            if (result is Resource.Success) loadPendingUsers()
        }
    }

    fun resetActionState() {
        _actionState.value = Resource.Loading
    }
}