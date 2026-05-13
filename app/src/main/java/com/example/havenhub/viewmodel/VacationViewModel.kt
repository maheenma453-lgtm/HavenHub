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
    val isLoading            : Boolean          = false,
    val properties           : List<Property>   = emptyList(),
    val unavailableDates     : List<Date>        = emptyList(),
    val allPackages          : List<RentalPackage> = emptyList(),
    val propertyPackages     : List<RentalPackage> = emptyList(),
    val selectedPackage      : RentalPackage?   = null,
    val selectedPropertyId   : String           = "",
    val selectedPropertyTitle: String           = "",
    val checkInDay           : Int              = -1,
    val checkOutDay          : Int              = -1,
    val guestCount           : Int              = 2,
    val errorMessage         : String?          = null,
    val successMessage       : String?          = null
)

@HiltViewModel
class VacationViewModel @Inject constructor(
    private val propertyRepository : PropertyRepository,
    private val bookingRepository  : BookingRepository,
    private val firebaseDataManager: FirebaseDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VacationUiState())
    val uiState: StateFlow<VacationUiState> = _uiState.asStateFlow()

    init {
        loadVacationProperties()
        loadAllActivePackages()
    }

    // ── Properties ───────────────────────────────────────────────────────────

    // ════════════════════════════════════════════════════════════════
    // BUG FIX: Pehle sirf northern city/id filter tha — iska matlab
    // saari 12 properties aa sakti thi chahe unpe package ho ya na ho.
    //
    // Ab flow:
    // 1. Firestore se saare ACTIVE rental packages fetch karo
    // 2. Un packages mein se unique propertyIds nikalo
    // 3. Sirf woh properties show karo jinka propertyId us list mein ho
    //
    // Result: Sirf woh 6 properties dikhti hain jinpe deal lagi hui hai.
    // ════════════════════════════════════════════════════════════════
    fun loadVacationProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Step 1: Saare active packages fetch karo
            val packagesResult = firebaseDataManager.getActiveRentalPackages()
            if (packagesResult is Resource.Error) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = packagesResult.message)
                }
                return@launch
            }

            val activePackages = (packagesResult as Resource.Success).data ?: emptyList()

            // Step 2: Un packages se unique propertyIds nikalo
            val packagedPropertyIds = activePackages
                .map { it.propertyId.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            // Agar koi package nahi to empty list dikhao
            if (packagedPropertyIds.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        properties = emptyList(),
                        allPackages = activePackages
                    )
                }
                return@launch
            }

            // Step 3: Saari properties fetch karo aur sirf package-wali filter karo
            when (val propertiesResult = propertyRepository.getAllProperties()) {
                is Resource.Success -> {
                    val filteredList = (propertiesResult.data ?: emptyList())
                        .filter { prop ->
                            prop.propertyId.trim() in packagedPropertyIds
                        }

                    _uiState.update {
                        it.copy(
                            isLoading   = false,
                            properties  = filteredList,
                            allPackages = activePackages
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = propertiesResult.message)
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
                        val startDate = try { booking.checkInDate?.toDate()  } catch (e: Exception) { null }
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
            _uiState.value.checkInDay  != -1 && _uiState.value.checkOutDay != -1 ->
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