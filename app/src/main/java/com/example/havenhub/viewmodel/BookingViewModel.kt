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

// ─────────────────────────────────────────────────────────────────────────────
// BookingUiState — holds all UI state related to bookings
// ─────────────────────────────────────────────────────────────────────────────
data class BookingUiState(
    val isLoading        : Boolean       = false,   // true while any async operation is running
    val isSendingMessage : Boolean       = false,   // true while a message is being sent
    val bookings         : List<Booking> = emptyList(), // list of bookings loaded for the current user
    val currentBooking   : Booking?      = null,    // single booking loaded for detail view
    val errorMessage     : String?       = null,    // non-null when an error has occurred
    val successMessage   : String?       = null,    // non-null when an action completed successfully
    val actionSuccess    : Boolean       = false,   // one-shot flag: true after a create/update/cancel succeeds
    val createdBookingId : String?       = null     // ID of the newly created booking (used for navigation)
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository : BookingRepository,
    private val firestore  : FirebaseFirestore,
    private val auth       : FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    // Cached credentials so forceRefresh can reload without re-passing params
    private var cachedUserId: String = ""
    private var cachedRole: String = "tenant"

    // Guard flag to prevent duplicate booking creation on double-tap
    private var isCreatingBooking: Boolean = false

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD BOOKINGS
    // Fetches bookings from the repository based on the user's role.
    //   admin    → all bookings in the system
    //   landlord → bookings for properties owned by this landlord
    //   tenant   → bookings made by this tenant
    // ─────────────────────────────────────────────────────────────────────────
    fun loadBookings(userId: String, role: String) {
        Log.d("BOOKING_VM", "loadBookings CALLED — userId='$userId' role='$role'")
        if (userId.isBlank()) {
            Log.e("BOOKING_VM", "userId BLANK — aborted")
            return
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

    // Re-fetches bookings using the last known userId and role.
    // Call this after a status change to keep the list in sync.
    fun forceRefreshBookings() {
        if (cachedUserId.isNotEmpty()) loadBookings(cachedUserId, cachedRole)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD SINGLE BOOKING
    // Used by BookingDetailScreen to load the full booking object by its ID.
    // ─────────────────────────────────────────────────────────────────────────
    fun loadBookingById(bookingId: String) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getBookingById(bookingId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, currentBooking = result.data)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                else -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE BOOKING
    // Creates a new booking document via the repository.
    // The isCreatingBooking flag prevents duplicate submissions on rapid taps.
    // On success, createdBookingId is set so the UI can navigate to confirmation.
    // ─────────────────────────────────────────────────────────────────────────
    fun createBooking(booking: Booking) {
        if (isCreatingBooking) {
            Log.w("BOOKING_VM", "Already creating — ignored")
            return
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
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            } finally {
                isCreatingBooking = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE BOOKING STATUS  (admin / landlord action)
    // Delegates to BookingRepository.updateBookingStatus(), which also handles
    // sending status-change notifications to the tenant automatically.
    // ─────────────────────────────────────────────────────────────────────────
    fun updateStatusByAdmin(bookingId: String, newStatus: BookingStatus) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.updateBookingStatus(bookingId, newStatus)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, actionSuccess = true)
                }

                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                else -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MARK DEPOSIT PAID  (pre-booking flow — tenant pays 20%)
    // Called by PaymentScreen after a successful 20% deposit payment.
    // Updates the booking with:
    //   • status / bookingStatus → DEPOSIT_PAID
    //   • paymentStatus          → DEPOSIT_PAID
    //   • depositAmount          → the amount paid
    //   • remainingAmount        → totalAmount - depositAmount (80% still owed)
    // ─────────────────────────────────────────────────────────────────────────
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
                            "bookingStatus" to BookingStatus.DEPOSIT_PAID.name,
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

    // ─────────────────────────────────────────────────────────────────────────
    // MARK CHECKED IN  (landlord confirms tenant has arrived)
    // Triggered by the "Mark Checked In" button in BookingDetailScreen /
    // MyBookingsScreen when the booking is in DEPOSIT_PAID state.
    //
    // Sets status → CHECKED_IN so the tenant can then pay the remaining 80%.
    // paymentStatus remains DEPOSIT_PAID because the full amount is not yet paid.
    //
    // FIX (from pull): previously this was setting status → AWAITING_FINAL_PAYMENT,
    // which was confusing. It now correctly sets CHECKED_IN.
    // ─────────────────────────────────────────────────────────────────────────
    fun markCheckedIn(bookingId: String) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                firestore.collection("bookings").document(bookingId)
                    .update(
                        mapOf(
                            "status" to BookingStatus.CHECKED_IN.name,
                            "bookingStatus" to BookingStatus.CHECKED_IN.name,
                            // paymentStatus stays DEPOSIT_PAID — remaining 80% still owed
                            "paymentStatus" to PaymentStatus.DEPOSIT_PAID.name,
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

    // ─────────────────────────────────────────────────────────────────────────
    // MARK FINAL PAYMENT COMPLETE  (landlord confirms the remaining 80% received)
    //
    // FIX (from pull): was previously setting status → PENDING_APPROVAL, which
    // was incorrect because the payment is fully done at this point.
    // Now correctly sets status → CONFIRMED and paymentStatus → PAID.
    // ─────────────────────────────────────────────────────────────────────────
    fun markFinalPaymentComplete(bookingId: String) {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                firestore.collection("bookings").document(bookingId)
                    .update(
                        mapOf(
                            // FIX: CONFIRMED, not PENDING_APPROVAL — payment is fully done
                            "status" to BookingStatus.CONFIRMED.name,
                            "bookingStatus" to BookingStatus.CONFIRMED.name,
                            // FIX: PAID, not DEPOSIT_PAID — full amount has now been received
                            "paymentStatus" to PaymentStatus.PAID.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                _uiState.update { it.copy(isLoading = false, actionSuccess = true) }
                Log.d("BOOKING_VM", "Final payment complete — booking fully CONFIRMED and PAID")
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL BOOKING
    // Sets the booking status to CANCELLED in Firestore, then optimistically
    // updates the local list and re-fetches a fresh copy from the server.
    // ─────────────────────────────────────────────────────────────────────────
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
                            "bookingStatus" to BookingStatus.CANCELLED.name,
                            "cancelledAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                // Optimistically update the in-memory list before the server refresh
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        actionSuccess = true,
                        bookings = state.bookings.map { b ->
                            if (b.bookingId == bookingId) b.copy(status = BookingStatus.CANCELLED.name) else b
                        }
                    )
                }

                // Re-fetch fresh data from Firestore to keep the list accurate
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

    // ─────────────────────────────────────────────────────────────────────────
    // SEND MESSAGE
    // Saves a chat message document to the "messages" Firestore collection.
    // Used from BookingDetailScreen so tenants/landlords can message each other.
    // ─────────────────────────────────────────────────────────────────────────
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
                    it.copy(isSendingMessage = false, successMessage = "Message sent successfully")
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

    // ─────────────────────────────────────────────────────────────────────────
    // CLEAR STATE MESSAGES
    // Call this from the UI after consuming errorMessage, successMessage,
    // actionSuccess, or createdBookingId to reset them back to defaults.
    // ─────────────────────────────────────────────────────────────────────────
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