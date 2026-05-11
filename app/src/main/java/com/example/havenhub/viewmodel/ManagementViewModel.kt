package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.utils.Resource
import com.example.havenhub.utils.getPropertyImage
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ManagementUiState(
    val isLoading          : Boolean         = false,
    val users              : List<User>       = emptyList(),
    val properties         : List<Property>   = emptyList(),
    val bookings           : List<Booking>    = emptyList(),
    val bookingDrawableMap : Map<String, Int> = emptyMap(),
    val actionSuccess      : Boolean          = false,
    val errorMessage       : String?          = null,
    val successMessage     : String?          = null
)

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val firestore     = FirebaseFirestore.getInstance()
    private val usersCol      = firestore.collection("users")
    private val propertiesCol = firestore.collection("properties")

    private val _uiState = MutableStateFlow(ManagementUiState())
    val uiState: StateFlow<ManagementUiState> = _uiState.asStateFlow()

    init { loadAllData() }

    fun loadAllData() {
        loadAllUsers()
        loadAllProperties()
        loadAllBookings()
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = adminRepository.getAllUsers()) {
                is Resource.Success -> _uiState.update { it.copy(users = result.data, isLoading = false) }
                is Resource.Error   -> _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun loadAllProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = adminRepository.getAllProperties()) {
                is Resource.Success -> _uiState.update { it.copy(properties = result.data, isLoading = false) }
                is Resource.Error   -> _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun loadAllBookings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = adminRepository.getAllBookings()) {
                is Resource.Success -> {
                    val (enrichedBookings, drawableMap) = enrichBookingsWithMissingData(result.data)
                    _uiState.update {
                        it.copy(
                            bookings           = enrichedBookings,
                            bookingDrawableMap = drawableMap,
                            isLoading          = false
                        )
                    }
                }
                is Resource.Error   -> _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    private suspend fun enrichBookingsWithMissingData(
        bookings: List<Booking>
    ): Pair<List<Booking>, Map<String, Int>> {

        val drawableMap = mutableMapOf<String, Int>()

        val enrichedList = bookings.map { booking ->
            viewModelScope.async {
                var enriched = booking

                // ✅ FIX: condition se isBlank() check hata diya
                // Hamesha Firestore se fresh naam fetch karo
                // Taake purane blank-name bookings bhi fix ho jaayein
                if (enriched.tenantId.isNotBlank()) {
                    try {
                        val userDoc = usersCol.document(enriched.tenantId).get().await()
                        val fetchedName = userDoc.getString("fullName")   // ✅ Firestore field
                            ?: userDoc.getString("name")
                            ?: userDoc.getString("displayName")
                            ?: ""
                        if (fetchedName.isNotBlank()) {
                            enriched = enriched.copy(tenantName = fetchedName)
                        }
                    } catch (_: Exception) {}
                }

                // propertyCoverUrl missing check
                if (enriched.propertyCoverUrl.isBlank() && enriched.propertyId.isNotBlank()) {
                    var urlFound = false
                    try {
                        val propDoc    = propertiesCol.document(enriched.propertyId).get().await()
                        val fetchedUrl = (propDoc.get("imageUrls") as? List<*>)
                            ?.filterIsInstance<String>()
                            ?.firstOrNull { it.isNotBlank() } ?: ""
                        if (fetchedUrl.isNotBlank()) {
                            enriched = enriched.copy(propertyCoverUrl = fetchedUrl)
                            urlFound = true
                        }
                    } catch (_: Exception) {}

                    if (!urlFound) {
                        drawableMap[enriched.bookingId] = getPropertyImage(enriched.propertyId)
                    }
                }

                enriched
            }
        }.awaitAll()

        return Pair(enrichedList, drawableMap)
    }

    // ── Property Actions ──────────────────────────────────────────────────────

    fun approveProperty(propertyId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.approveProperty(propertyId)) { loadAllProperties() }
    }

    fun removeProperty(propertyId: String, reason: String = "Removed by admin") = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.rejectProperty(propertyId, reason)) { loadAllProperties() }
    }

    fun deleteProperty(propertyId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.deleteProperty(propertyId)) { loadAllProperties() }
    }

    // ── User Actions ──────────────────────────────────────────────────────────

    fun banUser(userId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.banUser(userId)) { loadAllUsers() }
    }

    fun unbanUser(userId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.unbanUser(userId)) { loadAllUsers() }
    }

    // ── Booking Actions ───────────────────────────────────────────────────────

    fun cancelBooking(bookingId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.cancelBooking(bookingId)) { loadAllBookings() }
    }

    fun approveBooking(bookingId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val result = adminRepository.updateBookingStatus(bookingId, BookingStatus.CONFIRMED.name)
        when (result) {
            is Resource.Success -> {
                _uiState.update { state ->
                    state.copy(
                        isLoading      = false,
                        actionSuccess  = true,
                        successMessage = "Booking confirmed successfully!",
                        bookings       = state.bookings.map { b ->
                            if (b.bookingId == bookingId) b.copy(status = BookingStatus.CONFIRMED.name)
                            else b
                        }
                    )
                }
                loadAllBookings()
            }
            is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            Resource.Loading  -> Unit
        }
    }

    fun rejectBooking(bookingId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val result = adminRepository.updateBookingStatus(bookingId, BookingStatus.CANCELLED.name)
        when (result) {
            is Resource.Success -> {
                _uiState.update { state ->
                    state.copy(
                        isLoading      = false,
                        actionSuccess  = true,
                        successMessage = "Booking rejected.",
                        bookings       = state.bookings.map { b ->
                            if (b.bookingId == bookingId) b.copy(status = BookingStatus.CANCELLED.name)
                            else b
                        }
                    )
                }
                loadAllBookings()
            }
            is Resource.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            Resource.Loading  -> Unit
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun handleActionResult(result: Resource<Unit>, onSuccess: () -> Unit) {
        _uiState.update { state ->
            when (result) {
                is Resource.Success -> { onSuccess(); state.copy(isLoading = false, actionSuccess = true) }
                is Resource.Error   -> state.copy(isLoading = false, errorMessage = result.message)
                is Resource.Loading -> state.copy(isLoading = true)
            }
        }
    }

    fun resetActionState() {
        _uiState.update { it.copy(actionSuccess = false, errorMessage = null, successMessage = null) }
    }
}