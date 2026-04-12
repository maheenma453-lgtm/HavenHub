package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingUiState(
    val isLoading        : Boolean       = false,
    val bookings         : List<Booking> = emptyList(),
    val currentBooking   : Booking?      = null,      // ✅ New
    val errorMessage     : String?       = null,
    val actionSuccess    : Boolean       = false,
    val createdBookingId : String?       = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun loadBookings(userId: String, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = when (role.lowercase()) {
                    "admin"    -> repository.getAllBookingsForAdmin()
                    "landlord" -> repository.getLandlordBookings(userId)
                    else       -> repository.getTenantBookings(userId)
                }
                _uiState.update { it.copy(isLoading = false, bookings = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ✅ New: bookingId se single booking load karo
    fun loadBookingById(bookingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getBookingById(bookingId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, currentBooking = result.data)
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {}
            }
        }
    }

    fun createBooking(booking: Booking) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, actionSuccess = false, errorMessage = null) }
            when (val result = repository.createBooking(booking)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            actionSuccess    = true,
                            createdBookingId = result.data
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun updateStatusByAdmin(bookingId: String, newStatus: BookingStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.updateBookingStatus(bookingId, newStatus)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage     = null,
                actionSuccess    = false,
                createdBookingId = null
            )
        }
    }
}