package com.example.havenhub.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class BookingUiState(
    val isLoading        : Boolean       = false,
    val isSendingMessage : Boolean       = false,
    val bookings         : List<Booking> = emptyList(),
    val currentBooking   : Booking?      = null,
    val errorMessage     : String?       = null,
    val successMessage   : String?       = null,
    val actionSuccess    : Boolean       = false,
    val createdBookingId : String?       = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository : BookingRepository,
    private val firestore  : FirebaseFirestore,
    private val auth       : FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private var cachedUserId: String = ""
    private var cachedRole: String = "tenant"
    private var isCreatingBooking: Boolean = false

    fun loadBookings(userId: String, role: String) {
        Log.d("BOOKING_VM", "loadBookings CALLED — userId='$userId' role='$role'")
        if (userId.isBlank()) {
            Log.e("BOOKING_VM", "userId BLANK — aborted"); return
        }

        val effectiveRole = role.ifBlank { "tenant" }
        cachedUserId = userId
        cachedRole = effectiveRole

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = when (effectiveRole.lowercase()) {
                    "admin" -> repository.getAllBookingsForAdmin()
                    "landlord" -> repository.getLandlordBookings(userId)
                    else -> repository.getTenantBookings(userId)
                }
                Log.d("BOOKING_VM", "Got ${result.size} bookings")
                _uiState.update { it.copy(isLoading = false, bookings = result) }
            } catch (e: Exception) {
                Log.e("BOOKING_VM", "EXCEPTION: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun forceRefreshBookings() {
        if (cachedUserId.isNotEmpty()) loadBookings(cachedUserId, cachedRole)
    }

    fun loadBookingById(bookingId: String) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getBookingById(bookingId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentBooking = result.data
                    )
                }

                is Resource.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }

                else -> {}
            }
        }
    }

    fun createBooking(booking: Booking) {
        if (isCreatingBooking) {
            Log.w("BOOKING_VM", "Already creating — ignored"); return
        }
        isCreatingBooking = true
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    actionSuccess = false,
                    errorMessage = null
                )
            }
            try {
                when (val result = repository.createBooking(booking)) {
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            actionSuccess = true,
                            createdBookingId = result.data
                        )
                    }

                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
            } finally {
                isCreatingBooking = false
            }
        }
    }

    fun updateStatusByAdmin(bookingId: String, newStatus: BookingStatus) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.updateBookingStatus(bookingId, newStatus)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        actionSuccess = true
                    )
                }

                is Resource.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }

                else -> {}
            }
        }
    }

    // Called after the tenant pays the 20% deposit for a pre-booking.
    // Sets status → DEPOSIT_PAID and records the deposit/remaining amounts in Firestore.
    fun markDepositPaid(bookingId: String, depositAmount: Double, totalAmount: Double) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val remainingAmount = totalAmount - depositAmount
                firestore.collection("bookings").document(bookingId)
                    .update(
                        mapOf(
                            "status" to BookingStatus.DEPOSIT_PAID.name,
                            "paymentStatus" to PaymentStatus.DEPOSIT_PAID.name,
                            "depositAmount" to depositAmount,
                            "remainingAmount" to remainingAmount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                Log.d("BOOKING_VM", "Deposit marked paid — remaining: $remainingAmount")
            } catch (e: Exception) {
                Log.e("BOOKING_VM", "markDepositPaid error: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // Called when the landlord confirms the tenant has arrived.
    // Sets status → CHECKED_IN so the tenant can then pay the remaining 80% from the app.
    fun markCheckedIn(bookingId: String) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                firestore.collection("bookings").document(bookingId)
                    .update(
                        mapOf(
                            "status" to BookingStatus.CHECKED_IN.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                Log.d("BOOKING_VM", "Tenant checked in — awaiting 80% final payment")
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // Called after the tenant pays the remaining 80% on arrival.
    // Sets status → PENDING_APPROVAL and paymentStatus → PAID so the landlord can confirm.
    fun markFinalPaymentComplete(bookingId: String) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                firestore.collection("bookings").document(bookingId)
                    .update(
                        mapOf(
                            "status" to BookingStatus.PENDING_APPROVAL.name,
                            "paymentStatus" to PaymentStatus.PAID.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                Log.d("BOOKING_VM", "Final payment complete — awaiting landlord approval")
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        if (bookingId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Invalid booking ID") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                firestore.collection("bookings").document(bookingId)
                    .update(
                        mapOf(
                            "status" to BookingStatus.CANCELLED.name,
                            "cancelledAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        actionSuccess = true,
                        bookings = state.bookings.map { b ->
                            if (b.bookingId == bookingId) b.copy(status = BookingStatus.CANCELLED.name) else b
                        }
                    )
                }

                if (cachedUserId.isNotEmpty()) {
                    val fresh = when (cachedRole.lowercase()) {
                        "admin" -> repository.getAllBookingsForAdmin()
                        "landlord" -> repository.getLandlordBookings(cachedUserId)
                        else -> repository.getTenantBookings(cachedUserId)
                    }
                    _uiState.update { it.copy(bookings = fresh) }
                }
            } catch (e: Exception) {
                Log.e("BOOKING_VM", "cancelBooking error: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Cancel failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun sendMessage(toUserId: String, message: String, bookingId: String, propertyTitle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true, errorMessage = null) }
            try {
                val currentUserId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val msgData = hashMapOf(
                    "fromUserId" to currentUserId,
                    "toUserId" to toUserId,
                    "bookingId" to bookingId,
                    "propertyTitle" to propertyTitle,
                    "message" to message,
                    "isRead" to false,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                firestore.collection("messages").add(msgData).await()
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        successMessage = "Message sent successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        errorMessage = "Message send failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null,
                actionSuccess = false,
                createdBookingId = null
            )
        }
    }
}