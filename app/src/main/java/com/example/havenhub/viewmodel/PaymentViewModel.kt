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
//
// isPreBooking flag is read by PaymentScreen / PaymentSuccessScreen to decide
// which success message and status strip to show (deposit vs. full payment).
// ─────────────────────────────────────────────────────────────────────────────
data class PaymentUiState(
    val isLoading      : Boolean        = false,          // true while any async operation is running
    val payment        : Payment?       = null,           // the latest payment record fetched from Firestore
    val paymentHistory : List<Payment>  = emptyList(),    // full payment list for the current user
    val selectedMethod : PaymentMethod? = null,           // payment method the user tapped in the UI
    val defaultMethod  : PaymentMethod? = null,           // pre-selected default method (if any)
    val errorMessage   : String?        = null,           // non-null when an error has occurred
    val actionSuccess  : Boolean        = false,          // one-shot flag: true after a successful payment
    val isPreBooking   : Boolean        = false           // true when the deposit-only (20%) flow was used
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository : PaymentRepository,
    private val bookingRepository : BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD PAYMENT HISTORY  (tenant)
    // Fetches all payment records where the tenant is the payer.
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

                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }

                    Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load payments"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROCESS PAYMENT
    // Core payment flow — handles both simple bookings and pre-bookings.
    //
    // Parameters:
    //   isFinalPayment     → true when the tenant is paying the remaining 80%
    //                        after a previous deposit
    //   isPreBookingDirect → true when called directly from the pre-booking
    //                        screen (i.e., this IS the 20% deposit payment)
    //   packageId          → rental package ID if applicable; "none" otherwise
    //
    // Booking status transition logic:
    //   isFinalPayment = true                         → PENDING_APPROVAL
    //   current status = CHECKED_IN / AWAITING_FINAL  → PENDING_APPROVAL
    //   isPreBookingDirect = true                     → DEPOSIT_PAID
    //   booking has isPreBooking flag or depositAmount → DEPOSIT_PAID
    //   anything else (simple full payment)           → PENDING_APPROVAL
    // ─────────────────────────────────────────────────────────────────────────
    fun processPayment(
        bookingId: String,
        payerId: String,
        payeeId: String,
        payerName: String,
        payeeName: String,
        amount: String,
        packageId: String = "none",
        method: PaymentMethod,
        isFinalPayment: Boolean = false,
        isPreBookingDirect: Boolean = false
    ) {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        if (amountDouble <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Invalid payment amount") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch the current booking to read its status and pre-booking flags
                val bookingResult = bookingRepository.getBookingById(bookingId)
                val currentBooking =
                    if (bookingResult is Resource.Success) bookingResult.data else null
                val currentStatusStr = currentBooking?.status ?: currentBooking?.bookingStatus ?: ""

                // Determine whether this is a pre-booking (deposit-only) payment
                val isPreBooking = when {
                    isFinalPayment -> false
                    currentStatusStr == BookingStatus.CHECKED_IN.name -> false
                    currentStatusStr == BookingStatus.AWAITING_FINAL_PAYMENT.name -> false
                    isPreBookingDirect -> true
                    else -> {
                        currentBooking?.isPreBooking == true ||
                                (currentBooking?.depositAmount ?: 0.0) > 0.0
                    }
                }

                // Build the payment record (status starts as PENDING; updated to COMPLETED below)
                val payment = Payment(
                    bookingId = bookingId,
                    payerId = payerId,
                    payerName = payerName,
                    payeeId = payeeId,
                    payeeName = payeeName,
                    amount = amount,
                    packageId = if (packageId == "none") "" else packageId,
                    paymentMethod = method.name,
                    status = PaymentStatus.PENDING.name,
                    type = PaymentType.BOOKING.name
                )

                when (val result = paymentRepository.savePayment(payment)) {
                    is Resource.Success -> {

                        // Determine the new booking status based on the payment type
                        val newBookingStatus = when {
                            // Final payment (80%) or tenant was already checked in → awaiting landlord approval
                            isFinalPayment ||
                                    currentStatusStr == BookingStatus.CHECKED_IN.name ||
                                    currentStatusStr == BookingStatus.AWAITING_FINAL_PAYMENT.name -> {
                                BookingStatus.PENDING_APPROVAL
                            }
                            // Deposit (20%) payment → booking secured, awaiting check-in
                            isPreBooking -> {
                                BookingStatus.DEPOSIT_PAID
                            }
                            // Simple full payment (100%) → awaiting landlord approval
                            else -> {
                                BookingStatus.PENDING_APPROVAL
                            }
                        }

                        // Update the booking status in Firestore
                        bookingRepository.updateBookingStatus(bookingId, newBookingStatus)

                        // Stamp the booking document with the completed payment info
                        bookingRepository.updatePaymentStatusOnBooking(
                            bookingId = bookingId,
                            paymentStatus = PaymentStatus.COMPLETED.name,
                            paymentId = result.data ?: ""
                        )

                        // Mark the payment record itself as COMPLETED
                        paymentRepository.updatePaymentStatus(
                            result.data ?: "",
                            PaymentStatus.COMPLETED.name
                        )

                        // Re-fetch the updated payment to reflect the latest state in the UI
                        val updated = paymentRepository.getPaymentByBooking(bookingId)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                payment = if (updated is Resource.Success) updated.data else null,
                                actionSuccess = true,
                                isPreBooking = isPreBooking
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

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY PAYMENT STATUS
    // Used by PaymentSuccessScreen (and PaymentScreen on resume) to refresh
    // the payment state from Firestore after returning from a payment gateway.
    //
    // FIX (from pull): PENDING_APPROVAL and CONFIRMED statuses no longer force
    // isPreBooking = false, because pre-booking flows can also reach those states.
    // Instead, the depositAmount field is checked to determine if it was a pre-booking.
    // ─────────────────────────────────────────────────────────────────────────
    fun verifyPaymentStatus(bookingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val paymentResult = paymentRepository.getPaymentByBooking(bookingId)
                val bookingResult = bookingRepository.getBookingById(bookingId)
                val booking = if (bookingResult is Resource.Success) bookingResult.data else null
                val currentStatusStr = booking?.status ?: booking?.bookingStatus ?: ""

                // Determine pre-booking flag — only exclude truly non-deposit states
                val isPreBooking = when {
                    currentStatusStr == BookingStatus.CHECKED_IN.name -> false
                    currentStatusStr == BookingStatus.AWAITING_FINAL_PAYMENT.name -> false
                    else -> booking?.isPreBooking == true ||
                            (booking?.depositAmount ?: 0.0) > 0.0
                }

                when (paymentResult) {
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            payment = paymentResult.data,
                            isPreBooking = isPreBooking
                        )
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

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD LANDLORD PAYMENTS
    // Fetches all payments received by a specific landlord (payee).
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

                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }

                    Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load payments"
                    )
                }
            }
        }
    }

    // ── Simple state helpers ──────────────────────────────────────────────────

    /** Store the payment method the user tapped on the payment screen. */
    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    /** Set a default payment method and immediately select it. */
    fun setDefaultMethod(method: PaymentMethod) {
        _uiState.update { it.copy(defaultMethod = method, selectedMethod = method) }
    }

    /** Push a validation error message to the UI (e.g. from PaymentScreen). */
    fun setError(msg: String) {
        _uiState.update { it.copy(errorMessage = msg) }
    }

    /** Clear transient messages after the UI has consumed them. */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}