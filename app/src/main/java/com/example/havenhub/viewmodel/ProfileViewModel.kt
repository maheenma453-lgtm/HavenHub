package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.User
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.utils.Resource
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

    private val _profile = MutableStateFlow<Resource<User>>(Resource.Loading)
    val profile: StateFlow<Resource<User>> = _profile.asStateFlow()

    // FIX: updateProfile() doesn't exist in AuthRepository
    // Use Resource<Unit> — just signal success/failure, not return a User
    private val _updateState = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val updateState: StateFlow<Resource<Unit>> = _updateState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profile.value = Resource.Loading
            // FIX: getCurrentUser() → currentUser (FirebaseUser property)
            val firebaseUser = authRepository.currentUser
            _profile.value = if (firebaseUser != null) {
                // FirebaseUser se basic info — full User object chahiye toh dataManager.getUser() use karo
                Resource.Success(
                    User(
                        userId   = firebaseUser.uid,
                        email    = firebaseUser.email ?: "",
                        fullName = firebaseUser.displayName ?: ""
                    )
                )
            } else {
                Resource.Error("User not found")
            }
        }
    }

    fun resetState() {
        _updateState.value = Resource.Loading
    }
}