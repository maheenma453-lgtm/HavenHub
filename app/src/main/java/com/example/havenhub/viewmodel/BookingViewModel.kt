package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Booking
import com.havenhub.data.repository.AuthRepository
import com.havenhub.data.repository.BookingRepository
import com.havenhub.utils.Resource
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

    private val _bookingState = MutableStateFlow<Resource<Booking>>(Resource.Idle())
    val bookingState: StateFlow<Resource<Booking>> = _bookingState.asStateFlow()

    private val _myBookings = MutableStateFlow<Resource<List<Booking>>>(Resource.Idle())
    val myBookings: StateFlow<Resource<List<Booking>>> = _myBookings.asStateFlow()

    private val _bookingDetail = MutableStateFlow<Resource<Booking>>(Resource.Idle())
    val bookingDetail: StateFlow<Resource<Booking>> = _bookingDetail.asStateFlow()

    private val _cancelState = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val cancelState: StateFlow<Resource<Boolean>> = _cancelState.asStateFlow()

    // Selected dates for booking
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

    private fun calculateTotal(pricePerDay: Double = 0.0) {
        val checkIn = _checkInDate.value ?: return
        val checkOut = _checkOutDate.value ?: return
        val days = ((checkOut.time - checkIn.time) / (1000 * 60 * 60 * 24)).toInt()
        _totalAmount.value = days * pricePerDay
    }

    fun createBooking(
        propertyId: String,
        propertyTitle: String,
        pricePerDay: Double
    ) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            val checkIn = _checkInDate.value ?: return@launch
            val checkOut = _checkOutDate.value ?: return@launch
            _bookingState.value = Resource.Loading()
            _bookingState.value = bookingRepository.createBooking(
                tenantId = userId,
                propertyId = propertyId,
                propertyTitle = propertyTitle,
                checkInDate = checkIn,
                checkOutDate = checkOut,
                totalAmount = _totalAmount.value,
                pricePerDay = pricePerDay
            )
        }
    }

    fun loadMyBookings() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            _myBookings.value = Resource.Loading()
            _myBookings.value = bookingRepository.getBookingsByTenant(userId)
        }
    }

    fun loadBookingDetail(bookingId: String) {
        viewModelScope.launch {
            _bookingDetail.value = Resource.Loading()
            _bookingDetail.value = bookingRepository.getBookingById(bookingId)
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            _cancelState.value = Resource.Loading()
            _cancelState.value = bookingRepository.cancelBooking(bookingId)
        }
    }

    fun confirmBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(bookingId, "confirmed")
        }
    }

    fun resetState() {
        _bookingState.value = Resource.Idle()
        _cancelState.value = Resource.Idle()
    }
}
