package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.utils.Resource
import com.example.havenhub.utils.ValidationUtils
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: FirebaseUser? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isPasswordResetSent: Boolean = false,
    val selectedRole: String = "" // "tenant", "landlord", "admin"
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Form fields
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    // Validation errors
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    init {
        checkAuthState()
    }

    // ─────────────────────────────────────────
    //  Field Updates
    // ─────────────────────────────────────────

    fun onEmailChange(value: String) {
        _email.value = value
        _emailError.value = null
    }

    fun onPasswordChange(value: String) {
        _password.value = value
        _passwordError.value = null
    }

    fun onConfirmPasswordChange(value: String) {
        _confirmPassword.value = value
    }

    fun onFullNameChange(value: String) {
        _fullName.value = value
        _nameError.value = null
    }

    fun onRoleSelected(role: String) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    // ─────────────────────────────────────────
    //  Auth Check
    // ─────────────────────────────────────────

    private fun checkAuthState() {
        val firebaseUser = authRepository.currentUser
        _uiState.update {
            it.copy(
                currentUser = firebaseUser,
                isLoggedIn = firebaseUser != null
            )
        }
    }

    // ─────────────────────────────────────────
    //  Sign In
    // ─────────────────────────────────────────

    fun signIn() {
        if (!validateSignInForm()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signIn(
                email = _email.value.trim(),
                password = _password.value
            )
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = result.data
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ─────────────────────────────────────────
    //  Sign Up  →  registerUser() in Repository
    // ─────────────────────────────────────────

    fun signUp() {
        if (!validateSignUpForm()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.registerUser(
                email    = _email.value.trim(),
                password = _password.value,
                fullName = _fullName.value.trim(),
                role     = _uiState.value.selectedRole
            )
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = result.data,
                        successMessage = "Account created successfully!"
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ─────────────────────────────────────────
    //  Sign Out
    // ─────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { AuthUiState() }
            _email.value = ""
            _password.value = ""
        }
    }

    // ─────────────────────────────────────────
    //  Forgot Password
    // ─────────────────────────────────────────

    fun sendPasswordResetEmail() {
        val emailVal = _email.value.trim()
        if (!ValidationUtils.isValidEmail(emailVal)) {
            _emailError.value = "Please enter a valid email address"
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.sendPasswordResetEmail(emailVal)
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPasswordResetSent = true,
                        successMessage = "Password reset email sent!"
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ─────────────────────────────────────────
    //  Google Sign-In
    // ─────────────────────────────────────────

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithGoogle(idToken)
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = result.data
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ─────────────────────────────────────────
    //  Validation
    // ─────────────────────────────────────────

    private fun validateSignInForm(): Boolean {
        var isValid = true
        if (!ValidationUtils.isValidEmail(_email.value.trim())) {
            _emailError.value = "Please enter a valid email address"
            isValid = false
        }
        if (_password.value.length < 6) {
            _passwordError.value = "Password must be at least 6 characters"
            isValid = false
        }
        return isValid
    }

    private fun validateSignUpForm(): Boolean {
        var isValid = true
        if (_fullName.value.trim().length < 2) {
            _nameError.value = "Please enter your full name"
            isValid = false
        }
        if (!ValidationUtils.isValidEmail(_email.value.trim())) {
            _emailError.value = "Please enter a valid email address"
            isValid = false
        }
        if (_password.value.length < 6) {
            _passwordError.value = "Password must be at least 6 characters"
            isValid = false
        }
        if (_password.value != _confirmPassword.value) {
            _passwordError.value = "Passwords do not match"
            isValid = false
        }
        if (_uiState.value.selectedRole.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select a role to continue") }
            isValid = false
        }
        return isValid
    }

    // ─────────────────────────────────────────
    //  Error / Success
    // ─────────────────────────────────────────

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }
    fun isUserSignedIn(): Boolean = authRepository.isUserSignedIn()
}
