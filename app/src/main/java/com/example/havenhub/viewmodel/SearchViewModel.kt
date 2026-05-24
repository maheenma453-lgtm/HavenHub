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
    val isLoading      : Boolean        = false,
    val searchQuery    : String         = "",
    val searchResults  : List<Property> = emptyList(),
    val errorMessage   : String?        = null,
    val minPrice       : Double?        = null,
    val maxPrice       : Double?        = null,
    val selectedCity   : String?        = null,
    val propertyType   : PropertyType?  = null,
    val minBedrooms    : Int?           = null,
    val recentSearches : List<String>   = emptyList(),
    val hasActiveFilter: Boolean        = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("haven_hub_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadSavedHistory()
        // BUG FIX: removed performSearch() from init.
        // Previously, performSearch() here caused a race with applyFilters():
        // FilterScreen sets filters then pops back → SearchScreen's
        // LaunchedEffect(Unit) fired refreshSearch() → overwrote filters with
        // a clean state. Filters appeared to do nothing.
        // Now the initial load is triggered only by SearchScreen's first composition.
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
        sharedPrefs.edit().putString("recent_searches", history.joinToString("|")).apply()
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
            val list = currentState.recentSearches.toMutableList()
            list.remove(query)
            list.add(0, query)
            val updated = list.take(5)
            saveHistoryToStorage(updated)
            currentState.copy(recentSearches = updated)
        }
    }

    fun removeFromHistory(query: String) {
        _uiState.update { currentState ->
            val updated = currentState.recentSearches.filter { it != query }
            saveHistoryToStorage(updated)
            currentState.copy(recentSearches = updated)
        }
    }

    fun clearHistory() {
        sharedPrefs.edit().remove("recent_searches").apply()
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    // -------------------------------------------------------------------------
    // performSearch / performSearchWithState
    //
    // FIX (race condition):
    // applyFilters() builds newState explicitly and passes it directly to
    // performSearchWithState(newState) — guaranteed to use fresh filter values,
    // not whatever _uiState.value might be at coroutine scheduling time.
    // -------------------------------------------------------------------------

    fun performSearch() {
        performSearchWithState(_uiState.value)
    }

    private fun performSearchWithState(state: SearchUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = propertyRepository.getAllProperties()) {
                is Resource.Success -> {
                    var list = result.data

                    // Text search filter
                    if (state.searchQuery.isNotBlank()) {
                        val q = state.searchQuery.lowercase().trim()
                        list = list.filter {
                            it.title.lowercase().contains(q) ||
                                    it.city.lowercase().contains(q) ||
                                    it.address.lowercase().contains(q)
                        }
                    }

                    // Price range filter
                    state.minPrice?.let { min -> list = list.filter { it.pricePerNight >= min } }
                    state.maxPrice?.let { max -> list = list.filter { it.pricePerNight <= max } }

                    // City filter — case-insensitive exact match
                    state.selectedCity?.let { city ->
                        list = list.filter { it.city.equals(city, ignoreCase = true) }
                    }

                    // Property type filter — compare by enum .name ("HOUSE", "APARTMENT" …)
                    // so it matches however Firestore stored the string value
                    state.propertyType?.let { type ->
                        list = list.filter {
                            it.propertyType.equals(type.name, ignoreCase = true)
                        }
                    }

                    // Bedrooms filter
                    state.minBedrooms?.let { min -> list = list.filter { it.bedrooms >= min } }

                    _uiState.update { it.copy(isLoading = false, searchResults = list) }
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

    // Called ONCE by SearchScreen's LaunchedEffect on first composition only.
    // Does NOT re-run when navigating back from FilterScreen because
    // LaunchedEffect(Unit) only runs on initial entry to the composition — but
    // since SearchScreen stays in the back stack while FilterScreen is open,
    // the LaunchedEffect does NOT re-fire on pop. This is correct behavior.
    fun initialLoad() {
        // Only trigger if we have no results yet (truly first visit)
        if (_uiState.value.searchResults.isEmpty() && !_uiState.value.isLoading) {
            performSearch()
        }
    }

    fun applyFilters(
        minPrice: Double?,
        maxPrice: Double?,
        city: String?,
        type: PropertyType?,
        bedrooms: Int?
    ) {
        // hasActiveFilter is true when ANY real filter param is set
        val anyFilterActive = minPrice != null ||
                maxPrice != null ||
                city != null ||
                type != null ||
                bedrooms != null

        // Build new state explicitly first, then search with it.
        // Avoids the race condition where performSearch() reads stale _uiState.value.
        val newState = _uiState.value.copy(
            minPrice = minPrice,
            maxPrice = maxPrice,
            selectedCity = city,
            propertyType = type,
            minBedrooms = bedrooms,
            hasActiveFilter = anyFilterActive
        )
        _uiState.value = newState
        performSearchWithState(newState)
    }

    fun clearFilters() {
        val newState = _uiState.value.copy(
            minPrice = null,
            maxPrice = null,
            selectedCity = null,
            propertyType = null,
            minBedrooms = null,
            hasActiveFilter = false
        )
        _uiState.value = newState
        performSearchWithState(newState)
    }
}