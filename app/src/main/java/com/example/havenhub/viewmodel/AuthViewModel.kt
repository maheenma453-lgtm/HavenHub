package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.User
import com.havenhub.data.repository.AuthRepository
import com.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>>(Resource.Idle())
    val authState: StateFlow<Resource<User>> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
            _isLoggedIn.value = user != null
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.signIn(email, password)
            _authState.value = result
            if (result is Resource.Success) {
                _currentUser.value = result.data
                _isLoggedIn.value = true
            }
        }
    }

    fun signUp(
        name: String,
        email: String,
        password: String,
        role: String,
        phone: String
    ) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.signUp(name, email, password, role, phone)
            _authState.value = result
            if (result is Resource.Success) {
                _currentUser.value = result.data
                _isLoggedIn.value = true
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.sendPasswordResetEmail(email)
            _authState.value = result
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null
            _isLoggedIn.value = false
            _authState.value = Resource.Idle()
        }
    }

    fun resetState() {
        _authState.value = Resource.Idle()
    }
}
