package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _bookingState = MutableStateFlow<Resource<String>?>(null)
    val bookingState: StateFlow<Resource<String>?> = _bookingState.asStateFlow()

    private val _myBookings = MutableStateFlow<Resource<List<Booking>>?>(null)
    val myBookings: StateFlow<Resource<List<Booking>>?> = _myBookings.asStateFlow()

    private val _cancelState = MutableStateFlow<Resource<Unit>?>(null)
    val cancelState: StateFlow<Resource<Unit>?> = _cancelState.asStateFlow()

    private val _checkInDate = MutableStateFlow<Date?>(null)
    val checkInDate: StateFlow<Date?> = _checkInDate.asStateFlow()

    private val _checkOutDate = MutableStateFlow<Date?>(null)
    val checkOutDate: StateFlow<Date?> = _checkOutDate.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    fun setCheckInDate(date: Date) {
        _checkInDate.value = date
        calculateTotal()
    }

    fun setCheckOutDate(date: Date) {
        _checkOutDate.value = date
        calculateTotal()
    }

    private fun calculateTotal(pricePerNight: Double = 0.0) {
        val checkIn = _checkInDate.value ?: return
        val checkOut = _checkOutDate.value ?: return
        val days = ((checkOut.time - checkIn.time) / (1000 * 60 * 60 * 24)).toInt()
        _totalAmount.value = days * pricePerNight
    }

    fun createBooking(
        propertyId: String,
        propertyTitle: String,
        pricePerNight: Double
    ) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            val checkIn = _checkInDate.value ?: return@launch
            val checkOut = _checkOutDate.value ?: return@launch

            val booking = Booking(
                tenantId      = userId,
                propertyId    = propertyId,
                propertyTitle = propertyTitle,
                checkInDate   = Timestamp(checkIn),   // ✅ Date → Timestamp
                checkOutDate  = Timestamp(checkOut),  // ✅ Date → Timestamp
                totalAmount   = _totalAmount.value,
                pricePerNight = pricePerNight         // ✅ pricePerDay → pricePerNight
            )

            _bookingState.value = Resource.Loading
            _bookingState.value = bookingRepository.createBooking(booking)
        }
    }

    fun loadMyBookings() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _myBookings.value = Resource.Loading
            _myBookings.value = bookingRepository.getUserBookings(userId)
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            _cancelState.value = Resource.Loading
            _cancelState.value = bookingRepository.updateBookingStatus(
                bookingId,
                BookingStatus.CANCELLED
            )
        }
    }

    fun confirmBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(
                bookingId,
                BookingStatus.CONFIRMED
            )
        }
    }

    fun resetState() {
        _bookingState.value = null
        _cancelState.value = null
    }
}