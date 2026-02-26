package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyType
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class VacationViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _vacationProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Loading)
    val vacationProperties: StateFlow<Resource<List<Property>>> = _vacationProperties.asStateFlow()

    // FIX: RentalPackage doesn't exist in repository — removed
    // FIX: unavailableDates doesn't exist in repository — calculated from bookings
    private val _unavailableDates = MutableStateFlow<List<Date>>(emptyList())
    val unavailableDates: StateFlow<List<Date>> = _unavailableDates.asStateFlow()

    private val _selectedProperty = MutableStateFlow<Property?>(null)
    val selectedProperty: StateFlow<Property?> = _selectedProperty.asStateFlow()

    private val _propertyBookings = MutableStateFlow<Resource<List<Booking>>>(Resource.Loading)
    val propertyBookings: StateFlow<Resource<List<Booking>>> = _propertyBookings.asStateFlow()

    init {
        loadVacationProperties()
    }

    // FIX: getVacationRentals() doesn't exist — use getAllProperties() and filter by type
    fun loadVacationProperties(city: String? = null) {
        viewModelScope.launch {
            _vacationProperties.value = Resource.Loading
            val result = propertyRepository.getAllProperties()

            if (result is Resource.Error) {
                _vacationProperties.value = Resource.Error(result.message)
                return@launch
            }

            var list = (result as Resource.Success).data

            // Filter vacation-type properties (VILLA, FARMHOUSE, APARTMENT)
            list = list.filter {
                it.propertyType in listOf(
                    PropertyType.VILLA,
                    PropertyType.FARMHOUSE,
                    PropertyType.APARTMENT
                )
            }

            // Filter by city if provided
            city?.let { c ->
                list = list.filter { it.city.lowercase() == c.lowercase() }
            }

            _vacationProperties.value = Resource.Success(list)
        }
    }

    // FIX: getUnavailableDates() doesn't exist — observe bookings and extract dates
    fun loadUnavailableDates(propertyId: String) {
        viewModelScope.launch {
            bookingRepository.observePropertyBookings(propertyId).collect { bookings ->
                val dates = mutableListOf<Date>()
                bookings.forEach { booking ->
                    // Extract dates between checkIn and checkOut from each booking
                    val checkIn  = booking.checkInDate?.toDate()  ?: return@forEach
                    val checkOut = booking.checkOutDate?.toDate() ?: return@forEach
                    var current  = checkIn
                    while (!current.after(checkOut)) {
                        dates.add(current)
                        val cal = java.util.Calendar.getInstance()
                        cal.time = current
                        cal.add(java.util.Calendar.DATE, 1)
                        current = cal.time
                    }
                }
                _unavailableDates.value = dates
            }
        }
    }

    fun selectProperty(property: Property) {
        _selectedProperty.value = property
        // FIX: property.id → property.propertyId
        loadUnavailableDates(property.propertyId)
    }

    fun clearSelectedProperty() {
        _selectedProperty.value = null
        _unavailableDates.value = emptyList()
    }
}