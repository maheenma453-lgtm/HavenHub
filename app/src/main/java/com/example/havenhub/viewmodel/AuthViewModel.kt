package com.example.havenhub.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.remote.ImgBBUploadManager
import com.example.havenhub.utils.Constants
import com.example.havenhub.utils.Resource
import com.example.havenhub.utils.ValidationUtils
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val isLoading           : Boolean       = false,
    val isLoggedIn          : Boolean       = false,
    val currentUser         : FirebaseUser? = null,
    val errorMessage        : String?       = null,
    val successMessage      : String?       = null,
    val isPasswordResetSent : Boolean       = false,
    val selectedRole        : String        = "",
    val userRole            : String        = "",
    val isVerified          : Boolean       = false,
    val isAuthReady         : Boolean       = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository : AuthRepository,
    private val imgBBManager   : ImgBBUploadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email           = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password           = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword           = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _fullName           = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _cnicNumber           = MutableStateFlow("")
    val cnicNumber: StateFlow<String> = _cnicNumber.asStateFlow()

    private val _cnicImageUri         = MutableStateFlow<Uri?>(null)
    val cnicImageUri: StateFlow<Uri?> = _cnicImageUri.asStateFlow()

    private val _profileImageUri         = MutableStateFlow<Uri?>(null)
    val profileImageUri: StateFlow<Uri?> = _profileImageUri.asStateFlow()

    private val _emailError            = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError            = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _nameError            = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _cnicError            = MutableStateFlow<String?>(null)
    val cnicError: StateFlow<String?> = _cnicError.asStateFlow()

    init { checkAuthState() }

    private fun checkAuthState() {
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                viewModelScope.launch {
                    val uid        = firebaseUser.uid
                    val role       = authRepository.getUserRole(uid).lowercase().trim()
                    val isVerified = authRepository.getUserVerified(uid)
                    _uiState.update {
                        it.copy(
                            isLoading   = false,
                            isAuthReady = true,
                            isLoggedIn  = true,
                            currentUser = firebaseUser,
                            userRole    = role,
                            isVerified  = isVerified
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading   = false,
                        isAuthReady = true,
                        isLoggedIn  = false,
                        currentUser = null,
                        userRole    = "",
                        isVerified  = false
                    )
                }
            }
        }
    }

    // ✅ FIX: FCM token save karo login ke baad — yeh missing tha
    // Agar token Firestore mein nahi hai to Firebase Console se bhi notification nahi aati
    private fun saveFcmTokenAfterLogin(uid: String, role: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("AUTH_VM", "FCM token fetched: $token")

                // Firestore mein save karo
                authRepository.updateUserFields(uid, mapOf("fcmToken" to token))

                // Role topic subscribe karo
                val roleTopic = when (role.uppercase()) {
                    Constants.ROLE_ADMIN    -> Constants.TOPIC_ADMIN
                    Constants.ROLE_LANDLORD -> Constants.TOPIC_LANDLORD
                    else                    -> Constants.TOPIC_TENANT
                }
                FirebaseMessaging.getInstance().subscribeToTopic(roleTopic).await()
                FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ALL).await()

                Log.d("AUTH_VM", "✅ FCM token saved & subscribed to $roleTopic")
            } catch (e: Exception) {
                Log.e("AUTH_VM", "FCM token save failed: ${e.localizedMessage}")
                // Non-critical error — don't show to user, login still successful
            }
        }
    }

    fun signIn() {
        if (!validateSignInForm()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signIn(
                email    = _email.value.trim(),
                password = _password.value
            )
            when (result) {
                is Resource.Success -> {
                    val uid        = result.data?.uid ?: ""
                    val role       = authRepository.getUserRole(uid).lowercase().trim()
                    val isVerified = authRepository.getUserVerified(uid)

                    // ✅ FIX: Login ke baad FCM token save karo
                    if (uid.isNotEmpty()) saveFcmTokenAfterLogin(uid, role)

                    _uiState.update {
                        it.copy(
                            isLoading   = false,
                            isAuthReady = true,
                            isLoggedIn  = true,
                            currentUser = result.data,
                            userRole    = role,
                            isVerified  = isVerified
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun signUp() {
        if (!validateSignUpForm()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val role       = _uiState.value.selectedRole.lowercase()
            val isTenant   = role == "tenant"
            val isLandlord = role == "landlord"
            val needsCnic  = isTenant || isLandlord

            // Step 1: Profile image upload (optional)
            var profileImageUrl = ""
            _profileImageUri.value?.let { uri ->
                val result = imgBBManager.uploadImage(uri)
                if (result is Resource.Success) {
                    profileImageUrl = result.data ?: ""
                }
            }

            // Step 2: CNIC image upload
            var cnicImageUrl = ""
            if (needsCnic && _cnicImageUri.value != null) {
                val result = imgBBManager.uploadImage(_cnicImageUri.value!!)
                if (result is Resource.Error) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "CNIC image upload failed. Please try again.")
                    }
                    return@launch
                }
                cnicImageUrl = (result as? Resource.Success)?.data ?: ""
            }

            // Step 3: Register user
            val result = authRepository.registerUser(
                email           = _email.value.trim(),
                password        = _password.value,
                fullName        = _fullName.value.trim(),
                role            = _uiState.value.selectedRole,
                profileImageUrl = profileImageUrl,
                cnicNumber      = if (needsCnic) _cnicNumber.value.trim() else "",
                cnicImageUrl    = if (needsCnic) cnicImageUrl else ""
            )

            when (result) {
                is Resource.Success -> {
                    val uid = result.data?.uid ?: ""

                    // ✅ FIX: SignUp ke baad bhi FCM token save karo
                    if (uid.isNotEmpty()) saveFcmTokenAfterLogin(uid, _uiState.value.selectedRole)

                    val successMsg = when {
                        isLandlord -> "Account created! Admin will verify your CNIC before you can list properties."
                        isTenant   -> "Account created! Waiting for admin verification."
                        else       -> "Account created successfully!"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading      = false,
                            isAuthReady    = true,
                            isLoggedIn     = true,
                            currentUser    = result.data,
                            userRole       = _uiState.value.selectedRole.lowercase().trim(),
                            isVerified     = false,
                            successMessage = successMsg
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun removeProfileImage() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val fields = mapOf("profileImageUrl" to "")
            when (val result = authRepository.updateUserFields(uid, fields)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, successMessage = "Profile photo removed successfully")
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // ✅ FCM token clear karo aur topics unsubscribe karo
            try {
                val uid = authRepository.currentUser?.uid
                if (uid != null) {
                    authRepository.updateUserFields(uid, mapOf("fcmToken" to ""))
                }
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.TOPIC_ADMIN)
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.TOPIC_LANDLORD)
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.TOPIC_TENANT)
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.TOPIC_ALL)
            } catch (e: Exception) {
                Log.e("AUTH_VM", "Token cleanup failed: ${e.message}")
            }

            authRepository.signOut()
            _uiState.update { AuthUiState() }
            _email.value    = ""
            _password.value = ""
        }
    }

    fun sendPasswordResetEmail() {
        val emailVal = _email.value.trim()
        if (!ValidationUtils.isValidEmail(emailVal)) {
            _emailError.value = "Please enter a valid email address"
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.sendPasswordResetEmail(emailVal)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, isPasswordResetSent = true,
                        successMessage = "Password reset email sent!")
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "New password must be at least 6 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user       = authRepository.currentUser ?: throw Exception("No user logged in")
                val email      = user.email ?: throw Exception("User email not found")
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                _uiState.update { it.copy(isLoading = false, successMessage = "Password updated!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    val uid        = result.data?.uid ?: ""
                    val role       = authRepository.getUserRole(uid).lowercase().trim()
                    val isVerified = authRepository.getUserVerified(uid)

                    // ✅ FIX: Google sign in ke baad bhi FCM token save karo
                    if (uid.isNotEmpty()) saveFcmTokenAfterLogin(uid, role)

                    _uiState.update {
                        it.copy(
                            isLoading   = false,
                            isAuthReady = true,
                            isLoggedIn  = true,
                            currentUser = result.data,
                            userRole    = role,
                            isVerified  = isVerified
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.deleteAccount()) {
                is Resource.Success -> _uiState.update { AuthUiState() }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateProfileImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = imgBBManager.uploadImage(uri)
            when (result) {
                is Resource.Success -> {
                    val url = result.data ?: ""
                    val uid = authRepository.currentUser?.uid ?: return@launch
                    authRepository.updateUserFields(uid, mapOf("profileImageUrl" to url))
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "Profile image updated!")
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ── Event Handlers ────────────────────────────────────────────────────────
    fun onEmailChange(value: String)           { _email.value = value;           _emailError.value = null }
    fun onPasswordChange(value: String)        { _password.value = value;        _passwordError.value = null }
    fun onConfirmPasswordChange(value: String) { _confirmPassword.value = value }
    fun onFullNameChange(value: String)        { _fullName.value = value;        _nameError.value = null }
    fun onRoleSelected(role: String)           { _uiState.update { it.copy(selectedRole = role) } }
    fun onCnicNumberChange(value: String)      { _cnicNumber.value = value;      _cnicError.value = null }
    fun onCnicImageSelected(uri: Uri?)         { _cnicImageUri.value = uri }
    fun onProfileImageSelected(uri: Uri?)      { _profileImageUri.value = uri }

    fun clearError()   = _uiState.update { it.copy(errorMessage = null) }
    fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }
    fun isUserSignedIn(): Boolean = authRepository.isUserSignedIn()

    // ── Validation ────────────────────────────────────────────────────────────
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
        val role       = _uiState.value.selectedRole.lowercase()
        val isTenant   = role == "tenant"
        val isLandlord = role == "landlord"
        val needsCnic  = isTenant || isLandlord
        if (needsCnic) {
            if (_cnicNumber.value.trim().isEmpty()) {
                _cnicError.value = "CNIC number is required"
                isValid = false
            }
            if (_cnicImageUri.value == null) {
                _uiState.update { it.copy(errorMessage = "Please upload your CNIC image") }
                isValid = false
            }
        }
        return isValid
    }
}
