package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.remote.FirebaseDataManager
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
    val allPackages: List<RentalPackage> = emptyList(),
    val propertyPackages: List<RentalPackage> = emptyList(),
    val selectedPackage: RentalPackage? = null,
    val selectedPropertyId: String = "",
    val selectedPropertyTitle: String = "",
    val checkInDay: Int = -1,
    val checkOutDay: Int = -1,
    val guestCount: Int = 2,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class VacationViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingRepository: BookingRepository,
    private val firebaseDataManager: FirebaseDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VacationUiState())
    val uiState: StateFlow<VacationUiState> = _uiState.asStateFlow()

    private val northernCities = listOf(
        "Kaghan", "Naran", "Skardu", "Swat", "Hunza", "Murree",
        "Islamabad", "Gilgit", "Chitral", "Neelum", "Azad Kashmir"
    )

    private val northernPropertyIds = listOf(
        "prop_003",
        "prop_007",
        "prop_008",
        "prop_009",
        "prop_010",
        "prop_011",
        "prop_012"
    )

    init {
        loadVacationProperties()
        loadAllActivePackages()
    }

    // ── Properties ───────────────────────────────────────────────────────────

    fun loadVacationProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = propertyRepository.getAllProperties()) {
                is Resource.Success -> {
                    val filteredList = result.data
                        ?.filter { prop ->
                            val trimmedId = prop.propertyId.trim()
                            val matchById = trimmedId in northernPropertyIds
                            val matchByCity = northernCities.any { city ->
                                prop.city.contains(city, ignoreCase = true) ||
                                        prop.title.contains(city, ignoreCase = true)
                            }
                            matchById || matchByCity
                        }
                        ?: emptyList()

                    _uiState.update { it.copy(isLoading = false, properties = filteredList) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> { _uiState.update { it.copy(isLoading = false) } }
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

    // ── Rental Packages ───────────────────────────────────────────────────────

    fun loadAllActivePackages() {
        viewModelScope.launch {
            when (val result = firebaseDataManager.getActiveRentalPackages()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(allPackages = result.data ?: emptyList()) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun loadPackagesForProperty(propertyId: String) {
        viewModelScope.launch {
            // ✦ FIX: pehle stale data clear karo
            _uiState.update {
                it.copy(
                    isLoading        = true,
                    propertyPackages = emptyList(),
                    selectedPackage  = null
                )
            }
            when (val result = firebaseDataManager.getPackagesByProperty(propertyId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            propertyPackages = result.data ?: emptyList()
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> { _uiState.update { it.copy(isLoading = false) } }
            }
        }
    }

    fun selectPackage(pkg: RentalPackage) {
        _uiState.update { it.copy(selectedPackage = pkg) }
    }

    fun clearSelectedPackage() {
        _uiState.update { it.copy(selectedPackage = null) }
    }

    // ── Pre-Booking Form State ────────────────────────────────────────────────

    fun setSelectedProperty(propertyId: String, propertyTitle: String) {
        // ✦ FIX: pehle state clear karo, phir load karo
        _uiState.update {
            it.copy(
                selectedPropertyId    = propertyId,
                selectedPropertyTitle = propertyTitle,
                propertyPackages      = emptyList(),
                selectedPackage       = null
            )
        }
        loadPackagesForProperty(propertyId)
    }

    fun setGuestCount(count: Int) {
        _uiState.update { it.copy(guestCount = count.coerceIn(1, 20)) }
    }

    fun setCheckInDay(day: Int) {
        _uiState.update { it.copy(checkInDay = day, checkOutDay = -1) }
    }

    fun setCheckOutDay(day: Int) {
        _uiState.update { it.copy(checkOutDay = day) }
    }

    fun calculateTotalAmount(): Double {
        val pkg = _uiState.value.selectedPackage ?: return 0.0
        val nights = when {
            _uiState.value.checkInDay != -1 && _uiState.value.checkOutDay != -1 ->
                (_uiState.value.checkOutDay - _uiState.value.checkInDay).coerceAtLeast(1)
            else -> pkg.minNights.coerceAtLeast(1)
        }
        return pkg.discountedPricePerNight * nights
    }

    fun calculateDepositAmount(): Double = calculateTotalAmount() * 0.20

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}