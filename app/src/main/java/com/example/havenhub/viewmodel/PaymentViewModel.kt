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
    val isLoading: Boolean = false,
    val payment: Payment? = null,
    val paymentHistory: List<Payment> = emptyList(),
    val selectedMethod: PaymentMethod? = null,
    val defaultMethod: PaymentMethod? = null,
    val errorMessage: String? = null,
    val actionSuccess: Boolean = false
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    // ── Process Payment ────────────────────────────────────────────
    fun processPayment(
        bookingId: String,
        payerId: String,
        payeeId: String,
        payerName: String,
        payeeName: String,
        amount: Double,
        method: PaymentMethod
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
                    paymentMethod = method,
                    status        = PaymentStatus.PENDING,
                    type          = PaymentType.BOOKING
                )

                when (val result = paymentRepository.savePayment(payment)) {
                    is Resource.Success -> {
                        bookingRepository.updateBookingStatus(bookingId, BookingStatus.CONFIRMED)
                        paymentRepository.updatePaymentStatus(result.data, PaymentStatus.COMPLETED.name)

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

    // ── Load Payment History ───────────────────────────────────────
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

    // ── Verify Payment ─────────────────────────────────────────────
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

    // ── Payment Method Selection ───────────────────────────────────
    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun setDefaultMethod(method: PaymentMethod) {
        _uiState.update { it.copy(defaultMethod = method, selectedMethod = method) }
    }

    // ── Clear ──────────────────────────────────────────────────────
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}