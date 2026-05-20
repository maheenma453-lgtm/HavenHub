package com.example.havenhub.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyType
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val isLoading     : Boolean          = false,
    val searchQuery   : String           = "",
    val searchResults : List<Property>   = emptyList(),
    val errorMessage  : String?          = null,
    val minPrice      : Double?          = null,
    val maxPrice      : Double?          = null,
    val selectedCity  : String?          = null,
    val propertyType  : PropertyType?    = null,
    val minBedrooms   : Int?             = null,
    val recentSearches: List<String>     = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("haven_hub_prefs", Context.MODE_PRIVATE)
    private val _uiState    = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadSavedHistory()
        performSearch()
        setupAutoSearch()
    }

    private fun loadSavedHistory() {
        val savedString = sharedPrefs.getString("recent_searches", "") ?: ""
        if (savedString.isNotEmpty()) {
            val list = savedString.split("|").filter { it.isNotBlank() }
            _uiState.update { it.copy(recentSearches = list) }
        }
    }

    private fun saveHistoryToStorage(history: List<String>) {
        val stringToSave = history.joinToString("|")
        sharedPrefs.edit().putString("recent_searches", stringToSave).apply()
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

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        _uiState.update { currentState ->
            val currentList = currentState.recentSearches.toMutableList()
            if (currentList.contains(query)) currentList.remove(query)
            currentList.add(0, query)
            val updatedList = currentList.take(5)
            saveHistoryToStorage(updatedList)
            currentState.copy(recentSearches = updatedList)
        }
    }

    fun removeFromHistory(query: String) {
        _uiState.update { currentState ->
            val currentList = currentState.recentSearches.toMutableList()
            currentList.remove(query)
            val updatedList = currentList.toList()
            saveHistoryToStorage(updatedList)
            currentState.copy(recentSearches = updatedList)
        }
    }

    fun clearHistory() {
        sharedPrefs.edit().remove("recent_searches").apply()
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    // ✅ FIX: performSearch() ab propertyRepository.getAllProperties() call karta hai
    // jo PropertyRepository.fetchApproved() → direct Firestore APPROVED query se jaata hai.
    // Screen pe wapas aane par refreshSearch() call karo fresh data ke liye.
    fun performSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // getAllProperties() → fetchApproved() → direct Firestore .whereEqualTo("status","APPROVED")
            val result = propertyRepository.getAllProperties()

            when (result) {
                is Resource.Success -> {
                    val currentState = _uiState.value
                    var filteredList = result.data

                    if (currentState.searchQuery.isNotBlank()) {
                        val q = currentState.searchQuery.lowercase().trim()
                        filteredList = filteredList.filter {
                            it.title.lowercase().contains(q)   ||
                                    it.city.lowercase().contains(q)    ||
                                    it.address.lowercase().contains(q)
                        }
                    }

                    currentState.minPrice?.let    { min  -> filteredList = filteredList.filter { it.pricePerNight >= min } }
                    currentState.maxPrice?.let    { max  -> filteredList = filteredList.filter { it.pricePerNight <= max } }
                    currentState.selectedCity?.let{ city -> filteredList = filteredList.filter { it.city.equals(city, ignoreCase = true) } }
                    currentState.propertyType?.let{ type -> filteredList = filteredList.filter { it.propertyType == type.toString() } }
                    currentState.minBedrooms?.let { min  -> filteredList = filteredList.filter { it.bedrooms >= min } }

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

    // ✅ NEW: SearchScreen ke LaunchedEffect(Unit) mein call karo
    // Ye ensure karta hai ke search screen pe wapas aane par fresh APPROVED
    // properties load hon. (Koi naya property approve hua to dikhega)
    fun refreshSearch() {
        performSearch()
    }

    fun applyFilters(
        minPrice : Double?,
        maxPrice : Double?,
        city     : String?,
        type     : PropertyType?,
        bedrooms : Int?
    ) {
        _uiState.update {
            it.copy(
                minPrice     = minPrice,
                maxPrice     = maxPrice,
                selectedCity = city,
                propertyType = type,
                minBedrooms  = bedrooms
            )
        }
        performSearch()
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                minPrice     = null,
                maxPrice     = null,
                selectedCity = null,
                propertyType = null,
                minBedrooms  = null
            )
        }
        performSearch()
    }
}
