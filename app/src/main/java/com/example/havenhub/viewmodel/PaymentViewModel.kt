package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Payment
import com.havenhub.data.repository.AuthRepository
import com.havenhub.data.repository.PaymentRepository
import com.havenhub.utils.Resource
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

    private val _paymentState = MutableStateFlow<Resource<Payment>>(Resource.Idle())
    val paymentState: StateFlow<Resource<Payment>> = _paymentState.asStateFlow()

    private val _paymentHistory = MutableStateFlow<Resource<List<Payment>>>(Resource.Idle())
    val paymentHistory: StateFlow<Resource<List<Payment>>> = _paymentHistory.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow("card")
    val selectedPaymentMethod: StateFlow<String> = _selectedPaymentMethod.asStateFlow()

    fun setPaymentMethod(method: String) {
        _selectedPaymentMethod.value = method
    }

    fun processPayment(
        bookingId: String,
        amount: Double,
        propertyId: String
    ) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            _paymentState.value = Resource.Loading()
            _paymentState.value = paymentRepository.processPayment(
                userId = userId,
                bookingId = bookingId,
                amount = amount,
                propertyId = propertyId,
                method = _selectedPaymentMethod.value
            )
        }
    }

    fun loadPaymentHistory() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            _paymentHistory.value = Resource.Loading()
            _paymentHistory.value = paymentRepository.getPaymentsByUser(userId)
        }
    }

    fun resetState() {
        _paymentState.value = Resource.Idle()
    }
}
