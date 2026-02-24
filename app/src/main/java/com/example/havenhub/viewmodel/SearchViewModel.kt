package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Property
import com.havenhub.data.repository.PropertyRepository
import com.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<Resource<List<Property>>>(Resource.Idle())
    val searchResults: StateFlow<Resource<List<Property>>> = _searchResults.asStateFlow()

    // Filter states
    private val _minPrice = MutableStateFlow<Double?>(null)
    private val _maxPrice = MutableStateFlow<Double?>(null)
    private val _selectedCity = MutableStateFlow<String?>(null)
    private val _propertyType = MutableStateFlow<String?>(null)
    private val _minBedrooms = MutableStateFlow<Int?>(null)

    val minPrice: StateFlow<Double?> = _minPrice.asStateFlow()
    val maxPrice: StateFlow<Double?> = _maxPrice.asStateFlow()
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()
    val propertyType: StateFlow<String?> = _propertyType.asStateFlow()
    val minBedrooms: StateFlow<Int?> = _minBedrooms.asStateFlow()

    @OptIn(FlowPreview::class)
    fun setupAutoSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch(query: String = _searchQuery.value) {
        viewModelScope.launch {
            _searchResults.value = Resource.Loading()
            _searchResults.value = propertyRepository.searchProperties(
                query = query,
                minPrice = _minPrice.value,
                maxPrice = _maxPrice.value,
                city = _selectedCity.value,
                type = _propertyType.value,
                minBedrooms = _minBedrooms.value
            )
        }
    }

    fun applyFilters(
        minPrice: Double?,
        maxPrice: Double?,
        city: String?,
        type: String?,
        bedrooms: Int?
    ) {
        _minPrice.value = minPrice
        _maxPrice.value = maxPrice
        _selectedCity.value = city
        _propertyType.value = type
        _minBedrooms.value = bedrooms
        performSearch()
    }

    fun clearFilters() {
        _minPrice.value = null
        _maxPrice.value = null
        _selectedCity.value = null
        _propertyType.value = null
        _minBedrooms.value = null
        performSearch()
    }
}