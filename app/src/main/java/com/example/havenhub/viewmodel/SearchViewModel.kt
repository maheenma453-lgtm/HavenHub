package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyType
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Property> = emptyList(),
    val errorMessage: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val selectedCity: String? = null,
    val propertyType: PropertyType? = null,
    val minBedrooms: Int? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        performSearch()
        setupAutoSearch()
    }

    @OptIn(FlowPreview::class)
    private fun setupAutoSearch() {
        viewModelScope.launch {
            _uiState
                .map { it.searchQuery }
                .debounce(500)
                .distinctUntilChanged()
                .collect { performSearch() }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun performSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = propertyRepository.getAllProperties()

            when (result) {
                is Resource.Success -> {
                    val currentState = _uiState.value
                    var filteredList = result.data

                    if (currentState.searchQuery.isNotBlank()) {
                        val q = currentState.searchQuery.lowercase().trim()
                        filteredList = filteredList.filter {
                            it.title.lowercase().contains(q) ||
                                    it.city.lowercase().contains(q) ||
                                    it.address.lowercase().contains(q)
                        }
                    }

                    currentState.minPrice?.let { min ->
                        filteredList = filteredList.filter { it.pricePerNight >= min }
                    }
                    currentState.maxPrice?.let { max ->
                        filteredList = filteredList.filter { it.pricePerNight <= max }
                    }

                    currentState.selectedCity?.let { city ->
                        filteredList = filteredList.filter { it.city.equals(city, ignoreCase = true) }
                    }

                    // ✅ FIX: Comparison logic fixed to avoid Type Mismatch
                    currentState.propertyType?.let { type ->
                        filteredList = filteredList.filter { it.propertyType == type.toString() }
                    }

                    currentState.minBedrooms?.let { min ->
                        filteredList = filteredList.filter { it.bedrooms >= min }
                    }

                    _uiState.update { it.copy(isLoading = false, searchResults = filteredList) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun applyFilters(minPrice: Double?, maxPrice: Double?, city: String?, type: PropertyType?, bedrooms: Int?) {
        _uiState.update { it.copy(
            minPrice = minPrice, maxPrice = maxPrice, selectedCity = city, propertyType = type, minBedrooms = bedrooms
        ) }
        performSearch()
    }

    fun clearFilters() {
        _uiState.update { it.copy(
            minPrice = null, maxPrice = null, selectedCity = null, propertyType = null, minBedrooms = null
        ) }
        performSearch()
    }
}