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

// ─────────────────────────────────────────────────────────────────────────────
// VACATION UI STATE
//
// checkInDay  — day-of-month selected by tenant for check-in  (-1 = not set)
// checkOutDay — day-of-month selected by tenant for check-out (-1 = not set)
// checkInMonth / checkInYear   — month+year for check-in date
// checkOutMonth / checkOutYear — month+year for check-out date
//
// Storing month+year separately lets us correctly build full Timestamps
// when the tenant picks dates across different months.
// ─────────────────────────────────────────────────────────────────────────────
data class VacationUiState(
    val isLoading             : Boolean             = false,
    val properties            : List<Property>      = emptyList(),
    val unavailableDates      : List<Date>          = emptyList(),
    val allPackages           : List<RentalPackage> = emptyList(),
    val propertyPackages      : List<RentalPackage> = emptyList(),
    val selectedPackage       : RentalPackage?      = null,
    val selectedPropertyId    : String              = "",
    val selectedPropertyTitle : String              = "",

    // Tenant-selected check-in date components
    val checkInDay            : Int                 = -1,
    val checkInMonth          : Int                 = Calendar.getInstance().get(Calendar.MONTH),
    val checkInYear           : Int                 = Calendar.getInstance().get(Calendar.YEAR),

    // Tenant-selected check-out date components
    val checkOutDay           : Int                 = -1,
    val checkOutMonth         : Int                 = Calendar.getInstance().get(Calendar.MONTH),
    val checkOutYear          : Int                 = Calendar.getInstance().get(Calendar.YEAR),

    val guestCount            : Int                 = 2,
    val errorMessage          : String?             = null,
    val successMessage        : String?             = null
)

@HiltViewModel
class VacationViewModel @Inject constructor(
    private val propertyRepository : PropertyRepository,
    private val bookingRepository  : BookingRepository,
    private val firebaseDataManager: FirebaseDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VacationUiState())
    val uiState: StateFlow<VacationUiState> = _uiState.asStateFlow()

    // Cities shown on Vacation Hub — add more here as needed
    private val vacationCities = setOf(
        "islamabad", "hunza", "naran", "skardu",
        "swat", "murree", "kaghan", "gilgit",
        "abbottabad", "chitral", "mansehra", "neelum"
    )

    init {
        loadVacationProperties()
        loadAllActivePackages()
    }

    // ── Properties ────────────────────────────────────────────────────────────

    fun loadVacationProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = propertyRepository.getAllProperties()) {
                is Resource.Success -> {
                    val vacationList = result.data.filter { property ->
                        property.city.lowercase().trim() in vacationCities
                    }
                    _uiState.update { it.copy(isLoading = false, properties = vacationList) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // ── Unavailable Dates ─────────────────────────────────────────────────────

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
            _uiState.update {
                it.copy(isLoading = true, propertyPackages = emptyList(), selectedPackage = null)
            }
            when (val result = firebaseDataManager.getPackagesByProperty(propertyId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, propertyPackages = result.data ?: emptyList())
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun selectPackage(pkg: RentalPackage) {
        _uiState.update { it.copy(selectedPackage = pkg) }
    }

    fun clearSelectedPackage() {
        _uiState.update { it.copy(selectedPackage = null) }
    }

    // ── Property Selection ────────────────────────────────────────────────────

    fun setSelectedProperty(propertyId: String, propertyTitle: String) {
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

    // ── Guest Count ───────────────────────────────────────────────────────────

    fun setGuestCount(count: Int) {
        _uiState.update { it.copy(guestCount = count.coerceIn(1, 20)) }
    }

    // ── Date Selection (tenant-driven) ────────────────────────────────────────
    //
    // setCheckInDate  — saves check-in day + month + year, clears check-out
    // setCheckOutDate — saves check-out day + month + year
    // clearDates      — resets both dates so tenant can pick again
    //
    // Month/year are stored so we can build correct Timestamps even when
    // check-in and check-out fall in different calendar months.
    // ─────────────────────────────────────────────────────────────────────────

    fun setCheckInDate(day: Int, month: Int, year: Int) {
        _uiState.update {
            it.copy(
                checkInDay    = day,
                checkInMonth  = month,
                checkInYear   = year,
                // Always clear check-out when check-in changes
                checkOutDay   = -1,
                checkOutMonth = month,
                checkOutYear  = year
            )
        }
    }

    fun setCheckOutDate(day: Int, month: Int, year: Int) {
        _uiState.update {
            it.copy(
                checkOutDay   = day,
                checkOutMonth = month,
                checkOutYear  = year
            )
        }
    }

    fun clearDates() {
        val now = Calendar.getInstance()
        _uiState.update {
            it.copy(
                checkInDay    = -1,
                checkInMonth  = now.get(Calendar.MONTH),
                checkInYear   = now.get(Calendar.YEAR),
                checkOutDay   = -1,
                checkOutMonth = now.get(Calendar.MONTH),
                checkOutYear  = now.get(Calendar.YEAR)
            )
        }
    }

    // ── Amount Calculations ───────────────────────────────────────────────────

    fun calculateTotalAmount(): Double {
        val pkg    = _uiState.value.selectedPackage ?: return 0.0
        val nights = calculateNights().coerceAtLeast(pkg.minNights)
        return pkg.discountedPricePerNight * nights
    }

    fun calculateDepositAmount(): Double = calculateTotalAmount() * 0.20

    // Calculates nights between selected check-in and check-out dates.
    // Returns minNights from package if dates are not fully selected.
    fun calculateNights(): Int {
        val state = _uiState.value
        val pkg   = state.selectedPackage ?: return 1

        if (state.checkInDay == -1 || state.checkOutDay == -1) {
            return pkg.minNights.coerceAtLeast(1)
        }

        val checkIn = Calendar.getInstance().apply {
            set(Calendar.YEAR, state.checkInYear)
            set(Calendar.MONTH, state.checkInMonth)
            set(Calendar.DAY_OF_MONTH, state.checkInDay)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val checkOut = Calendar.getInstance().apply {
            set(Calendar.YEAR, state.checkOutYear)
            set(Calendar.MONTH, state.checkOutMonth)
            set(Calendar.DAY_OF_MONTH, state.checkOutDay)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMs    = checkOut.timeInMillis - checkIn.timeInMillis
        val diffDays  = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        return diffDays.coerceAtLeast(1)
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}











