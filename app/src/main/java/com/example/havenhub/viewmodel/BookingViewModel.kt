package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
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

    // ─────────────────────────────────────────────────────────
    //  Load all bookings (by role)
    // ─────────────────────────────────────────────────────────

    fun loadBookings(userId: String, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = when (role.lowercase()) {
                    "admin"    -> repository.getAllBookingsForAdmin()
                    "landlord" -> repository.getLandlordBookings(userId)
                    else       -> repository.getTenantBookings(userId)
                }
                _uiState.update { it.copy(isLoading = false, bookings = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Load single booking by ID
    // ─────────────────────────────────────────────────────────

    fun loadBookingById(bookingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getBookingById(bookingId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, currentBooking = result.data)
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Create booking
    // ─────────────────────────────────────────────────────────

    fun createBooking(booking: Booking) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, actionSuccess = false, errorMessage = null) }
            when (val result = repository.createBooking(booking)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading        = false,
                        actionSuccess    = true,
                        createdBookingId = result.data
                    )
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Update booking status (admin)
    // ─────────────────────────────────────────────────────────

    fun updateStatusByAdmin(bookingId: String, newStatus: BookingStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.updateBookingStatus(bookingId, newStatus)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, actionSuccess = true)
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Cancel booking (tenant)
    // ─────────────────────────────────────────────────────────

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                firestore.collection("bookings")
                    .document(bookingId)
                    .update(
                        mapOf(
                            "bookingStatus" to BookingStatus.CANCELLED.name,
                            "cancelledAt"   to FieldValue.serverTimestamp()
                        )
                    )
                    .await()

                _uiState.update {
                    it.copy(isLoading = false, actionSuccess = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Booking cancel failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Send message (landlord ↔ tenant)
    // ─────────────────────────────────────────────────────────

    fun sendMessage(
        toUserId      : String,
        message       : String,
        bookingId     : String,
        propertyTitle : String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true, errorMessage = null) }
            try {
                val currentUserId = auth.currentUser?.uid
                    ?: throw Exception("User not logged in")

                val msgData = hashMapOf(
                    "fromUserId"    to currentUserId,
                    "toUserId"      to toUserId,
                    "bookingId"     to bookingId,
                    "propertyTitle" to propertyTitle,
                    "message"       to message,
                    "isRead"        to false,
                    "timestamp"     to FieldValue.serverTimestamp()
                )

                firestore.collection("messages")
                    .add(msgData)
                    .await()

                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        successMessage   = "Message sent successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        errorMessage     = "Message send failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Clear messages / reset flags
    // ─────────────────────────────────────────────────────────

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage     = null,
                successMessage   = null,
                actionSuccess    = false,
                createdBookingId = null
            )
        }
    }
}