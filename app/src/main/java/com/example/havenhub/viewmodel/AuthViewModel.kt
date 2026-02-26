package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseUser
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

    // Resource<FirebaseUser> — matches AuthRepository return type
    private val _authState = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val authState: StateFlow<Resource<FirebaseUser>?> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        // authRepository.currentUser is a property, not a suspend function
        val user = authRepository.currentUser
        _isLoggedIn.value = user != null
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading  // object hai, () nahi
            val result = authRepository.signIn(email, password)
            _authState.value = result
            if (result is Resource.Success) {
                _isLoggedIn.value = true
            }
        }
    }

    fun signUp(
        name: String,
        email: String,
        password: String,
        role: String
    ) {
        viewModelScope.launch {
            _authState.value = Resource.Loading  // object hai, () nahi
            // signUp → registerUser (Repository ka actual method name)
            val result = authRepository.registerUser(email, password, name, role)
            _authState.value = result
            if (result is Resource.Success) {
                _isLoggedIn.value = true
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            val result = authRepository.sendPasswordResetEmail(email)
            // sendPasswordResetEmail returns Resource<Unit>, store separately if needed
            if (result is Resource.Error) {
                _authState.value = Resource.Error(result.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _isLoggedIn.value = false
            _authState.value = null  // reset state
        }
    }

    fun resetState() {
        _authState.value = null
    }
}