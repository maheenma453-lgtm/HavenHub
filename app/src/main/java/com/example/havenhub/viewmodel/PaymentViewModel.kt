package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Payment
import com.example.havenhub.data.PaymentMethod
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.data.PaymentType
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.PaymentRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val isLoading      : Boolean        = false,
    val payment        : Payment?       = null,
    val paymentHistory : List<Payment>  = emptyList(),
    val selectedMethod : PaymentMethod? = null,
    val defaultMethod  : PaymentMethod? = null,
    val errorMessage   : String?        = null,
    val actionSuccess  : Boolean        = false,
    val isPreBooking   : Boolean        = false  // ✅ ADD
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository : PaymentRepository,
    private val bookingRepository : BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun loadPaymentHistory(userId: String) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "User ID missing") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = paymentRepository.getUserPayments(userId)) {
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, paymentHistory = result.data)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load payments")
                }
            }
        }
    }

    // ✅ ADD: isFinalPayment aur isPreBookingDirect parameters add kiye
    fun processPayment(
        bookingId          : String,
        payerId            : String,
        payeeId            : String,
        payerName          : String,
        payeeName          : String,
        amount             : String,
        method             : PaymentMethod,
        isFinalPayment     : Boolean = false,  // ✅ ADD
        isPreBookingDirect : Boolean = false   // ✅ ADD
    ) {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        if (amountDouble <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Invalid payment amount") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val bookingResult  = bookingRepository.getBookingById(bookingId)
                val currentBooking = if (bookingResult is Resource.Success) bookingResult.data else null

                // ✅ ADD: Pre-booking check
                val isPreBooking = when {
                    isFinalPayment -> false
                    currentBooking?.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> false
                    isPreBookingDirect -> true
                    else -> {
                        currentBooking?.isPreBooking == true ||
                                (currentBooking?.depositAmount ?: 0.0) > 0.0
                    }
                }

                val payment = Payment(
                    bookingId     = bookingId,
                    payerId       = payerId,
                    payerName     = payerName,
                    payeeId       = payeeId,
                    payeeName     = payeeName,
                    amount        = amount,
                    paymentMethod = method.name,
                    status        = PaymentStatus.PENDING.name,
                    type          = PaymentType.BOOKING.name
                )

                when (val result = paymentRepository.savePayment(payment)) {
                    is Resource.Success -> {

                        // ✅ ADD: Pre-booking flow logic
                        val newBookingStatus = when {
                            isFinalPayment ||
                                    currentBooking?.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> {
                                BookingStatus.PENDING_APPROVAL
                            }
                            isPreBooking -> {
                                BookingStatus.DEPOSIT_PAID
                            }
                            else -> {
                                BookingStatus.PENDING_APPROVAL
                            }
                        }

                        bookingRepository.updateBookingStatus(bookingId, newBookingStatus)

                        bookingRepository.updatePaymentStatusOnBooking(
                            bookingId     = bookingId,
                            paymentStatus = PaymentStatus.COMPLETED.name,
                            paymentId     = result.data ?: ""
                        )

                        paymentRepository.updatePaymentStatus(
                            result.data ?: "",
                            PaymentStatus.COMPLETED.name
                        )

                        val updated = paymentRepository.getPaymentByBooking(bookingId)
                        _uiState.update {
                            it.copy(
                                isLoading     = false,
                                payment       = if (updated is Resource.Success) updated.data else null,
                                actionSuccess = true,
                                isPreBooking  = isPreBooking  // ✅ ADD
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    Resource.Loading -> Unit
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    // ✅ ADD: isPreBooking handling in verifyPaymentStatus
    fun verifyPaymentStatus(bookingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val paymentResult = paymentRepository.getPaymentByBooking(bookingId)
                val bookingResult = bookingRepository.getBookingById(bookingId)
                val booking       = if (bookingResult is Resource.Success) bookingResult.data else null

                val isPreBooking = when {
                    booking?.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> false
                    booking?.bookingStatus == BookingStatus.PENDING_APPROVAL       -> false
                    booking?.bookingStatus == BookingStatus.CONFIRMED               -> false
                    else -> booking?.isPreBooking == true ||
                            (booking?.depositAmount ?: 0.0) > 0.0
                }

                when (paymentResult) {
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, payment = paymentResult.data, isPreBooking = isPreBooking)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = paymentResult.message)
                    }
                    Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    fun loadLandlordPayments(landlordId: String) {
        if (landlordId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = paymentRepository.getLandlordPayments(landlordId)) {
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, paymentHistory = result.data)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load payments")
                }
            }
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun setDefaultMethod(method: PaymentMethod) {
        _uiState.update { it.copy(defaultMethod = method, selectedMethod = method) }
    }

    fun setError(msg: String) {
        _uiState.update { it.copy(errorMessage = msg) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}