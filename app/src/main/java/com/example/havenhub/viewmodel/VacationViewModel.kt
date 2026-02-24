package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Property
import com.havenhub.data.model.RentalPackage
import com.havenhub.data.repository.BookingRepository
import com.havenhub.data.repository.PropertyRepository
import com.havenhub.utils.Resource
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

    private val _vacationProperties = MutableStateFlow<Resource<List<Property>>>(Resource.Idle())
    val vacationProperties: StateFlow<Resource<List<Property>>> = _vacationProperties.asStateFlow()

    private val _rentalPackages = MutableStateFlow<Resource<List<RentalPackage>>>(Resource.Idle())
    val rentalPackages: StateFlow<Resource<List<RentalPackage>>> = _rentalPackages.asStateFlow()

    private val _unavailableDates = MutableStateFlow<List<Date>>(emptyList())
    val unavailableDates: StateFlow<List<Date>> = _unavailableDates.asStateFlow()

    private val _selectedProperty = MutableStateFlow<Property?>(null)
    val selectedProperty: StateFlow<Property?> = _selectedProperty.asStateFlow()

    init {
        loadVacationProperties()
    }

    fun loadVacationProperties(city: String? = null) {
        viewModelScope.launch {
            _vacationProperties.value = Resource.Loading()
            _vacationProperties.value = propertyRepository.getVacationRentals(city)
        }
    }

    fun loadRentalPackages(propertyId: String) {
        viewModelScope.launch {
            _rentalPackages.value = Resource.Loading()
            _rentalPackages.value = propertyRepository.getRentalPackages(propertyId)
        }
    }

    fun loadUnavailableDates(propertyId: String) {
        viewModelScope.launch {
            val result = bookingRepository.getUnavailableDates(propertyId)
            if (result is Resource.Success) {
                _unavailableDates.value = result.data ?: emptyList()
            }
        }
    }

    fun selectProperty(property: Property) {
        _selectedProperty.value = property
        loadRentalPackages(property.id)
        loadUnavailableDates(property.id)
    }

    fun clearSelectedProperty() {
        _selectedProperty.value = null
        _rentalPackages.value = Resource.Idle()
        _unavailableDates.value = emptyList()
    }
}
