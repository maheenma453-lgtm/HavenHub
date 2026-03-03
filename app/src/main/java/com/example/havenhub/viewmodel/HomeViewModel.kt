package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    // ✅ Fix: Resource.Loading is an object, no brackets ()
    private val _featuredProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Loading)
    val featuredProperties: StateFlow<Resource<List<Property>>> = _featuredProperties.asStateFlow()

    private val _nearbyProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Loading)
    val nearbyProperties: StateFlow<Resource<List<Property>>> = _nearbyProperties.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Parallel calls
                val job1 = launch { _featuredProperties.value = propertyRepository.getFeaturedProperties() }
                val job2 = launch { _nearbyProperties.value = propertyRepository.getNearbyProperties() }
                joinAll(job1, job2)
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Unknown Error"
                _errorMessage.value = errorMsg
                // ✅ Fix: Matching Resource.Error(message, code)
                _featuredProperties.value = Resource.Error(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchProperties(query: String) {
        if (query.isBlank()) {
            loadHomeData()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _featuredProperties.value = propertyRepository.searchPropertiesByName(query)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }
}