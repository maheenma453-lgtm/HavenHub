package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Payment
import com.example.havenhub.data.PaymentMethod
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.data.PaymentType
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

// ─────────────────────────────────────────────────────────────────────────────
// PaymentUiState — holds all UI-related payment state
// isPreBooking flag is used by PaymentScreen to show correct success message
// ─────────────────────────────────────────────────────────────────────────────
data class PaymentUiState(
    val isLoading      : Boolean        = false,
    val payment        : Payment?       = null,
    val paymentHistory : List<Payment>  = emptyList(),
    val selectedMethod : PaymentMethod? = null,
    val defaultMethod  : PaymentMethod? = null,
    val errorMessage   : String?        = null,
    val actionSuccess  : Boolean        = false,
    val isPreBooking   : Boolean        = false   // true when deposit-only flow was used
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository : PaymentRepository,
    private val bookingRepository : BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Load full payment history for a tenant user
    // ─────────────────────────────────────────────────────────────────────────
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
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    Resource.Loading    -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load payments")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Process a payment for a booking
    //
    // isFinalPayment     → true when tenant pays the remaining 80% after deposit
    // isPreBookingDirect → true when called directly from the pre-booking flow
    //
    // Logic:
    //   • isFinalPayment = true              → status becomes PENDING_APPROVAL
    //   • booking already AWAITING_FINAL_PAYMENT → status becomes PENDING_APPROVAL
    //   • isPreBookingDirect = true          → status becomes DEPOSIT_PAID
    //   • booking has isPreBooking flag or depositAmount > 0 → DEPOSIT_PAID
    //   • anything else                      → PENDING_APPROVAL (full payment)
    // ─────────────────────────────────────────────────────────────────────────
    fun processPayment(
        bookingId          : String,
        payerId            : String,
        payeeId            : String,
        payerName          : String,
        payeeName          : String,
        amount             : String,
       // amount: Double,
        packageId: String,
        method             : PaymentMethod,
        isFinalPayment     : Boolean = false,
        isPreBookingDirect : Boolean = false
    ) {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        if (amountDouble <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Invalid payment amount") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch current booking to determine correct status transition
                val bookingResult  = bookingRepository.getBookingById(bookingId)
                val currentBooking = if (bookingResult is Resource.Success) bookingResult.data else null

                // Determine if this is a pre-booking (deposit-only) payment
                val isPreBooking = when {
                    isFinalPayment -> false
                    currentBooking?.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> false
                    isPreBookingDirect -> true
                    else -> {
                        currentBooking?.isPreBooking == true ||
                                (currentBooking?.depositAmount ?: 0.0) > 0.0
                    }
                }

                // Build the payment record
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

                        // Determine the new booking status after payment
                        val newBookingStatus = when {
                            isFinalPayment ||
                                    currentBooking?.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> {
                                // Final payment done → move to awaiting landlord approval
                                BookingStatus.PENDING_APPROVAL
                            }
                            isPreBooking -> {
                                // Deposit paid → waiting for final payment before check-in
                                BookingStatus.DEPOSIT_PAID
                            }
                            else -> {
                                // Full upfront payment → awaiting landlord approval
                                BookingStatus.PENDING_APPROVAL
                            }
                        }

                        // Update booking status in Firestore
                        bookingRepository.updateBookingStatus(bookingId, newBookingStatus)

                        // Stamp the booking document with payment info
                        bookingRepository.updatePaymentStatusOnBooking(
                            bookingId     = bookingId,
                            paymentStatus = PaymentStatus.COMPLETED.name,
                            paymentId     = result.data ?: ""
                        )

                        // Mark the payment record itself as completed
                        paymentRepository.updatePaymentStatus(
                            result.data ?: "",
                            PaymentStatus.COMPLETED.name
                        )

                        // Re-fetch the updated payment to reflect latest state
                        val updated = paymentRepository.getPaymentByBooking(bookingId)
                        _uiState.update {
                            it.copy(
                                isLoading     = false,
                                payment       = if (updated is Resource.Success) updated.data else null,
                                actionSuccess = true,
                                isPreBooking  = isPreBooking
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    Resource.Loading  -> Unit
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verify the payment status of an existing booking
    // Used by PaymentScreen after returning from a gateway or on resume
    // ─────────────────────────────────────────────────────────────────────────
    fun verifyPaymentStatus(bookingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val paymentResult = paymentRepository.getPaymentByBooking(bookingId)
                val bookingResult = bookingRepository.getBookingById(bookingId)
                val booking       = if (bookingResult is Resource.Success) bookingResult.data else null

                // Determine pre-booking flag from booking state
                val isPreBooking = when {
                    booking?.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> false
                    booking?.bookingStatus == BookingStatus.PENDING_APPROVAL        -> false
                    booking?.bookingStatus == BookingStatus.CONFIRMED                -> false
                    else -> booking?.isPreBooking == true ||
                            (booking?.depositAmount ?: 0.0) > 0.0
                }

                when (paymentResult) {
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading    = false,
                            payment      = paymentResult.data,
                            isPreBooking = isPreBooking
                        )
                    }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = paymentResult.message)
                    }
                    Resource.Loading    -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load all payments received by a landlord
    // ─────────────────────────────────────────────────────────────────────────
    fun loadLandlordPayments(landlordId: String) {
        if (landlordId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = paymentRepository.getLandlordPayments(landlordId)) {
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, paymentHistory = result.data)
                    }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    Resource.Loading    -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load payments")
                }
            }
        }
    }

    // ── Simple state helpers ──────────────────────────────────────────────────

    /** Store the payment method the user tapped on the payment screen */
    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    /** Set and immediately select a default payment method */
    fun setDefaultMethod(method: PaymentMethod) {
        _uiState.update { it.copy(defaultMethod = method, selectedMethod = method) }
    }

    /** Manually push an error message to the UI (e.g. from PaymentScreen validation) */
    fun setError(msg: String) {
        _uiState.update { it.copy(errorMessage = msg) }
    }

    /** Clear transient messages after they have been displayed */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}
