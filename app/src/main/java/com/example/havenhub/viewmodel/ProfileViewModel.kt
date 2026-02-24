package com.example.havenhub.viewmodel
import android.net.Uri
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
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<Resource<User>>(Resource.Idle())
    val profile: StateFlow<Resource<User>> = _profile.asStateFlow()

    private val _updateState = MutableStateFlow<Resource<User>>(Resource.Idle())
    val updateState: StateFlow<Resource<User>> = _updateState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profile.value = Resource.Loading()
            val user = authRepository.getCurrentUser()
            _profile.value = if (user != null) Resource.Success(user) else Resource.Error("User not found")
        }
    }

    fun updateProfile(
        name: String,
        phone: String,
        bio: String,
        profileImageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading()
            _updateState.value = authRepository.updateProfile(
                name = name,
                phone = phone,
                bio = bio,
                profileImageUri = profileImageUri
            )
            if (_updateState.value is Resource.Success) {
                loadProfile()
            }
        }
    }

    fun resetState() {
        _updateState.value = Resource.Idle()
    }
}
