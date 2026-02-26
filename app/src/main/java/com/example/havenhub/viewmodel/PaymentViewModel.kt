package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Payment
import com.example.havenhub.data.PaymentMethod
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.PaymentRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _paymentState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val paymentState: StateFlow<Resource<String>> = _paymentState.asStateFlow()

    private val _paymentHistory = MutableStateFlow<Resource<List<Payment>>>(Resource.Loading)
    val paymentHistory: StateFlow<Resource<List<Payment>>> = _paymentHistory.asStateFlow()

    // FIX: store PaymentMethod enum instead of raw String
    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.JAZZCASH)
    val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()

    fun setPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun processPayment(
        bookingId: String,
        amount: Double
    ) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _paymentState.value = Resource.Loading

            // FIX: Payment has no propertyId or method(String) — use paymentMethod enum
            val payment = Payment(
                bookingId     = bookingId,
                amount        = amount,
                payerId       = userId,
                paymentMethod = _selectedPaymentMethod.value
            )
            _paymentState.value = paymentRepository.savePayment(payment)
        }
    }

    fun loadPaymentHistory() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _paymentHistory.value = Resource.Loading
            _paymentHistory.value = paymentRepository.getUserPayments(userId)
        }
    }

    fun resetState() {
        _paymentState.value = Resource.Loading
    }
}