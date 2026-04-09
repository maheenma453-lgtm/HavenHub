package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyType
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
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

    init {
        loadVacationProperties()
    }

    fun loadVacationProperties(city: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result: Resource<List<Property>> = propertyRepository.getAllProperties()

            when (result) {
                is Resource.Success -> {
                    val list = result.data?.filter {
                        // ✅ FIXED: String vs Enum comparison fix using .name
                        it.propertyType == PropertyType.VILLA.name ||
                                it.propertyType == PropertyType.FARMHOUSE.name ||
                                it.propertyType == PropertyType.APARTMENT.name
                    }?.filter { city == null || it.city.equals(city, ignoreCase = true) } ?: emptyList()

                    _uiState.update { it.copy(isLoading = false, properties = list) }
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

    fun loadUnavailableDates(propertyId: String) {
        viewModelScope.launch {
            bookingRepository.observeUserBookings(propertyId).collect { bookings ->
                val allDates = mutableListOf<Date>()
                bookings.forEach { booking ->
                    val startDate = booking.checkInDate?.toDate()
                    val endDate = booking.checkOutDate?.toDate()

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