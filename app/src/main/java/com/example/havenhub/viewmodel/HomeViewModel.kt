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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ✅ ZAROORI: Ye class ViewModel ke bahar magar file ke andar lazmi honi chahiye
data class HomeUiState(
    val featuredProperties: List<Property> = emptyList(),
    val nearbyProperties: List<Property> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    // ✅ StateFlow with explicit type to fix "Cannot infer type"
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val featuredResult = propertyRepository.getFeaturedProperties()
                val nearbyResult = propertyRepository.getNearbyProperties()

                _uiState.update { state ->
                    state.copy(
                        featuredProperties = if (featuredResult is Resource.Success) featuredResult.data ?: emptyList() else emptyList(),
                        nearbyProperties = if (nearbyResult is Resource.Success) nearbyResult.data ?: emptyList() else emptyList(),
                        isLoading = false,
                        errorMessage = if (featuredResult is Resource.Error) featuredResult.message else if (nearbyResult is Resource.Error) nearbyResult.message else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }
}