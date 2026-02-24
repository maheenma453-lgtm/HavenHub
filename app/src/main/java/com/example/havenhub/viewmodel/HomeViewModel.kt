package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Property
import com.havenhub.data.model.User
import com.havenhub.data.repository.AuthRepository
import com.havenhub.data.repository.PropertyRepository
import com.havenhub.utils.Resource
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

    private val _featuredProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Idle())
    val featuredProperties: StateFlow<Resource<List<Property>>> = _featuredProperties.asStateFlow()

    private val _nearbyProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Idle())
    val nearbyProperties: StateFlow<Resource<List<Property>>> = _nearbyProperties.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
        loadFeaturedProperties()
        loadNearbyProperties()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUser()
        }
    }

    fun loadFeaturedProperties() {
        viewModelScope.launch {
            _featuredProperties.value = Resource.Loading()
            _featuredProperties.value = propertyRepository.getFeaturedProperties()
        }
    }

    fun loadNearbyProperties(lat: Double = 0.0, lng: Double = 0.0) {
        viewModelScope.launch {
            _nearbyProperties.value = Resource.Loading()
            _nearbyProperties.value = propertyRepository.getNearbyProperties(lat, lng)
        }
    }

    fun refreshHome() {
        loadFeaturedProperties()
        loadNearbyProperties()
    }
}
