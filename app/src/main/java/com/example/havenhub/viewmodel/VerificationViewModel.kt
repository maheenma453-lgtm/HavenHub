package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Property
import com.havenhub.data.model.PropertyVerification
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
class VerificationViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _pendingProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Idle())
    val pendingProperties: StateFlow<Resource<List<Property>>> = _pendingProperties.asStateFlow()

    private val _pendingUsers = MutableStateFlow<Resource<List<User>>>(Resource.Idle())
    val pendingUsers: StateFlow<Resource<List<User>>> = _pendingUsers.asStateFlow()

    private val _propertyVerificationDetail = MutableStateFlow<Resource<PropertyVerification>>(Resource.Idle())
    val propertyVerificationDetail: StateFlow<Resource<PropertyVerification>> = _propertyVerificationDetail.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val actionState: StateFlow<Resource<Boolean>> = _actionState.asStateFlow()

    init {
        loadPendingProperties()
        loadPendingUsers()
    }

    fun loadPendingProperties() {
        viewModelScope.launch {
            _pendingProperties.value = Resource.Loading()
            _pendingProperties.value = adminRepository.getPendingProperties()
        }
    }

    fun loadPendingUsers() {
        viewModelScope.launch {
            _pendingUsers.value = Resource.Loading()
            _pendingUsers.value = adminRepository.getPendingUsers()
        }
    }

    fun loadPropertyVerificationDetail(propertyId: String) {
        viewModelScope.launch {
            _propertyVerificationDetail.value = Resource.Loading()
            _propertyVerificationDetail.value = adminRepository.getPropertyVerificationDetail(propertyId)
        }
    }

    fun approveProperty(propertyId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = adminRepository.approveProperty(propertyId)
            if (_actionState.value is Resource.Success) loadPendingProperties()
        }
    }

    fun rejectProperty(propertyId: String, reason: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = adminRepository.rejectProperty(propertyId, reason)
            if (_actionState.value is Resource.Success) loadPendingProperties()
        }
    }

    fun verifyUser(userId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = adminRepository.verifyUser(userId)
            if (_actionState.value is Resource.Success) loadPendingUsers()
        }
    }

    fun rejectUser(userId: String, reason: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = adminRepository.rejectUser(userId, reason)
            if (_actionState.value is Resource.Success) loadPendingUsers()
        }
    }

    fun resetActionState() {
        _actionState.value = Resource.Idle()
    }
}
