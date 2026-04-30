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
    val actionSuccess  : Boolean        = false
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository : PaymentRepository,
    private val bookingRepository : BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun processPayment(
        bookingId : String,
        payerId   : String,
        payeeId   : String,
        payerName : String,
        payeeName : String,
        amount    : Double,
        method    : PaymentMethod
    ) {
        if (amount <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Invalid payment amount") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
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
                        // ✅ 1. Booking status CONFIRMED karo
                        bookingRepository.updateBookingStatus(
                            bookingId,
                            BookingStatus.CONFIRMED
                        )

                        // ✅ 2. Booking document mein paymentStatus = PAID aur paymentId update karo
                        //       Yeh woh fix hai jo missing tha — ab har booking pay hone ke baad
                        //       paymentStatus "PAID" show karega chahe koi bhi property ho
                        bookingRepository.updatePaymentStatusOnBooking(
                            bookingId     = bookingId,
                            paymentStatus = PaymentStatus.COMPLETED.name,
                            paymentId     = result.data ?: ""
                        )

                        // ✅ 3. Payment record ka status bhi COMPLETED karo
                        paymentRepository.updatePaymentStatus(
                            result.data ?: "",
                            PaymentStatus.COMPLETED.name
                        )

                        val updated = paymentRepository.getPaymentByBooking(bookingId)
                        _uiState.update {
                            it.copy(
                                isLoading     = false,
                                payment       = if (updated is Resource.Success) updated.data else null,
                                actionSuccess = true
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

    fun loadPaymentHistory(userId: String) {
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

    fun verifyPaymentStatus(bookingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = paymentRepository.getPaymentByBooking(bookingId)) {
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, payment = result.data)
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
