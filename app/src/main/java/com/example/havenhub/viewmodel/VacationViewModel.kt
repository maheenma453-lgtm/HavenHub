package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class VacationUiState(
    val isLoading: Boolean = false,
    val properties: List<Property> = emptyList(),
    val unavailableDates: List<Date> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class VacationViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VacationUiState())
    val uiState: StateFlow<VacationUiState> = _uiState.asStateFlow()

    // SRS Goal 2: Northern tourist destination cities
    private val northernCities = listOf(
        "Kaghan", "Naran", "Skardu", "Swat", "Hunza", "Murree",
        "Islamabad", "Gilgit", "Chitral", "Neelum", "Azad Kashmir"
    )

    // prop_007 = Murree, prop_008 = Naran, prop_009 = Kaghan valley,
    // prop_010 = Swat, prop_011 = Hunza, prop_012 = Skardu
    private val northernPropertyIds = listOf(
        "prop_007", "prop_008", "prop_009", "prop_010", "prop_011", "prop_012"
    )

    init {
        loadVacationProperties()
    }

    fun loadVacationProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result: Resource<List<Property>> = propertyRepository.getAllProperties()

            when (result) {
                is Resource.Success -> {
                    val filteredList = result.data
                        ?.filter { prop ->
                            // Match by known northern property IDs (trim spaces for safety)
                            val trimmedId = prop.propertyId.trim()
                            val matchById = trimmedId in northernPropertyIds

                            // OR match by city/title containing a northern area name
                            val matchByCity = northernCities.any { city ->
                                prop.city.contains(city, ignoreCase = true) ||
                                        prop.title.contains(city, ignoreCase = true)
                            }

                            matchById || matchByCity
                        }
                        ?: emptyList()

                    _uiState.update {
                        it.copy(isLoading = false, properties = filteredList)
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }

                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun loadUnavailableDates(propertyId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        viewModelScope.launch {
            bookingRepository.getBookingsFlow(currentUserId).collect { bookings ->
                val allDates = mutableListOf<Date>()

                bookings
                    .filter { it.propertyId == propertyId }
                    .forEach { booking ->
                        val startDate = try { booking.checkInDate?.toDate() } catch (e: Exception) { null }
                        val endDate   = try { booking.checkOutDate?.toDate() } catch (e: Exception) { null }

                        if (startDate != null && endDate != null) {
                            val calendar = Calendar.getInstance()
                            var current: Date = startDate
                            while (!current.after(endDate)) {
                                allDates.add(Date(current.time))
                                calendar.time = current
                                calendar.add(Calendar.DATE, 1)
                                current = calendar.time
                            }
                        }
                    }

                _uiState.update { it.copy(unavailableDates = allDates) }
            }
        }
    }
}