package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Resource.Loading used as initial state (your Resource.kt has no Idle class)
    private val _allProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Loading)
    val allProperties: StateFlow<Resource<List<Property>>> = _allProperties.asStateFlow()

    // authRepository.currentUser is a property (not a suspend fun), so no coroutine needed
    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private var lastLat: Double = 0.0
    private var lastLng: Double = 0.0

    init {
        loadAllProperties()
    }

    fun loadAllProperties() {
        viewModelScope.launch {
            _allProperties.value = Resource.Loading
            _allProperties.value = propertyRepository.getAllProperties()
        }
    }

    fun loadNearbyProperties(lat: Double = 0.0, lng: Double = 0.0) {
        lastLat = lat
        lastLng = lng
        // PropertyRepository has no getNearbyProperties — use getAllProperties
        // and filter by location in your UI/screen layer
        loadAllProperties()
    }

    fun refreshHome() {
        _currentUser.value = authRepository.currentUser
        loadAllProperties()
    }
}