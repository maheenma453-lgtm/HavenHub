package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Payment
import com.example.havenhub.repository.PaymentRepository
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.NotificationRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PaymentMethod {
    JAZZCASH,
    EASYPAISA,
    BANK_TRANSFER,
    CASH_ON_ARRIVAL
}

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _paymentState = MutableStateFlow<Resource<Payment>?>(null)
    val paymentState: StateFlow<Resource<Payment>?> = _paymentState.asStateFlow()

    private val _paymentHistory = MutableStateFlow<Resource<List<Payment>>?>(null)
    val paymentHistory: StateFlow<Resource<List<Payment>>?> = _paymentHistory.asStateFlow()

    private val _selectedMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedMethod: StateFlow<PaymentMethod?> = _selectedMethod.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ✅ Process Payment
    fun processPayment(
        bookingId: String,
        userId: String,
        ownerId: String,
        amount: Double,
        method: PaymentMethod
    ) {
        if (amount <= 0.0) {
            _errorMessage.value = "Invalid payment amount"
            return
        }

        viewModelScope.launch {
            _paymentState.value = Resource.Loading()
            _isLoading.value = true

            try {
                val payment = Payment(
                    paymentId = "",
                    bookingId = bookingId,
                    userId = userId,
                    ownerId = ownerId,
                    amount = amount,
                    paymentMethod = method.name,
                    paymentStatus = PaymentStatus.PENDING.name,
                    transactionId = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val result = when (method) {
                    PaymentMethod.JAZZCASH -> paymentRepository.processJazzCashPayment(payment)
                    PaymentMethod.EASYPAISA -> paymentRepository.processEasyPaisaPayment(payment)
                    PaymentMethod.BANK_TRANSFER -> paymentRepository.processBankTransfer(payment)
                    PaymentMethod.CASH_ON_ARRIVAL -> paymentRepository.processCashPayment(payment)
                }

                when (result) {
                    is Resource.Success -> {
                        _paymentState.value = Resource.Success(result.data!!)

                        // Update booking status to CONFIRMED
                        bookingRepository.updateBookingStatus(bookingId, "CONFIRMED")

                        // Update payment status in booking
                        bookingRepository.updatePaymentStatus(bookingId, "PAID")

                        // Send notification to owner
                        notificationRepository.sendPaymentNotification(
                            ownerId = ownerId,
                            bookingId = bookingId,
                            amount = amount,
                            message = "Payment received for booking"
                        )
                    }
                    is Resource.Error -> {
                        _paymentState.value = Resource.Error(result.message ?: "Payment failed")
                        _errorMessage.value = result.message
                    }
                    else -> {}
                }

            } catch (e: Exception) {
                _paymentState.value = Resource.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Load Payment History
    fun loadPaymentHistory(userId: String) {
        viewModelScope.launch {
            _paymentHistory.value = Resource.Loading()
            _isLoading.value = true

            try {
                val result = paymentRepository.getUserPayments(userId)
                _paymentHistory.value = result
            } catch (e: Exception) {
                _paymentHistory.value = Resource.Error(e.message ?: "Failed to load payments")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Verify Payment Status
    fun verifyPaymentStatus(paymentId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = paymentRepository.verifyPayment(paymentId)
                when (result) {
                    is Resource.Success -> {
                        _paymentState.value = Resource.Success(result.data!!)
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Select Payment Method
    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedMethod.value = method
    }

    // ✅ Clear States
    fun clearError() {
        _errorMessage.value = null
    }

    fun resetPaymentState() {
        _paymentState.value = null
    }
}