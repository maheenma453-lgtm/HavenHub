package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.AdminPermissions
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.utils.Resource
import com.example.havenhub.utils.getPropertyImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val SUPER_ADMIN_EMAIL = "admin@havenhub.com"

data class ManagementUiState(
    val isLoading          : Boolean         = false,
    val users              : List<User>       = emptyList(),
    val properties         : List<Property>   = emptyList(),
    val bookings           : List<Booking>    = emptyList(),
    val bookingDrawableMap : Map<String, Int> = emptyMap(),
    val actionSuccess      : Boolean          = false,
    val errorMessage       : String?          = null,
    val successMessage     : String?          = null,
    val isSuperAdmin       : Boolean          = false
)

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val firestore     = FirebaseFirestore.getInstance()
    private val auth          = FirebaseAuth.getInstance()
    private val usersCol      = firestore.collection("users")
    private val propertiesCol = firestore.collection("properties")

    private val _uiState = MutableStateFlow(ManagementUiState())
    val uiState: StateFlow<ManagementUiState> = _uiState.asStateFlow()

    init {
        checkSuperAdminStatus()
        loadAllData()
    }

    private fun checkSuperAdminStatus() {
        val currentEmail = auth.currentUser?.email ?: ""
        val isSuperAdmin = currentEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
        _uiState.update { it.copy(isSuperAdmin = isSuperAdmin) }
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val usersDeferred      = async { adminRepository.getAllUsers() }
                val propertiesDeferred = async { adminRepository.getAllProperties() }
                val bookingsDeferred   = async { adminRepository.getAllBookings() }

                val usersResult      = usersDeferred.await()
                val propertiesResult = propertiesDeferred.await()
                val bookingsResult   = bookingsDeferred.await()

                val safeUsers = when (usersResult) {
                    is Resource.Success -> {
                        (usersResult.data ?: emptyList())
                            .map { user ->
                                if (user.role.isBlank() && user.userId.isNotBlank()) {
                                    try {
                                        val doc = usersCol.document(user.userId).get().await()
                                        val fetchedRole = doc.getString("role")
                                            ?: doc.getString("userRole")
                                            ?: doc.getString("userType")
                                            ?: "tenant"
                                        user.copy(role = fetchedRole)
                                    } catch (e: Exception) { user }
                                } else { user }
                            }
                            .distinctBy { it.userId.ifBlank { it.email } }
                    }
                    else -> emptyList()
                }

                val safeProperties = when (propertiesResult) {
                    is Resource.Success -> propertiesResult.data ?: emptyList()
                    else -> emptyList()
                }

                val (enrichedBookings, drawableMap) = when (bookingsResult) {
                    is Resource.Success -> enrichBookingsWithMissingData(bookingsResult.data ?: emptyList())
                    else -> Pair(emptyList(), emptyMap())
                }

                _uiState.update {
                    it.copy(
                        isLoading          = false,
                        users              = safeUsers,
                        properties         = safeProperties,
                        bookings           = enrichedBookings,
                        bookingDrawableMap = drawableMap,
                        errorMessage       = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Unexpected error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = adminRepository.getAllUsers()) {
                    is Resource.Success -> {
                        val safeUsers = (result.data ?: emptyList())
                            .map { user ->
                                if (user.role.isBlank() && user.userId.isNotBlank()) {
                                    try {
                                        val doc = usersCol.document(user.userId).get().await()
                                        val fetchedRole = doc.getString("role")
                                            ?: doc.getString("userRole")
                                            ?: doc.getString("userType")
                                            ?: "tenant"
                                        user.copy(role = fetchedRole)
                                    } catch (e: Exception) { user }
                                } else { user }
                            }
                            .distinctBy { it.userId.ifBlank { it.email } }
                        _uiState.update { it.copy(users = safeUsers, isLoading = false) }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(errorMessage = result.message ?: "Failed to load users", isLoading = false)
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Unexpected error: ${e.localizedMessage}", isLoading = false)
                }
            }
        }
    }

    fun loadAllProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = adminRepository.getAllProperties()) {
                    is Resource.Success -> _uiState.update {
                        it.copy(properties = result.data ?: emptyList(), isLoading = false)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(errorMessage = result.message ?: "Failed to load properties", isLoading = false)
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Unexpected error: ${e.localizedMessage}", isLoading = false)
                }
            }
        }
    }

    fun loadAllBookings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = adminRepository.getAllBookings()) {
                    is Resource.Success -> {
                        val (enrichedBookings, drawableMap) = enrichBookingsWithMissingData(
                            result.data ?: emptyList()
                        )
                        _uiState.update {
                            it.copy(bookings = enrichedBookings, bookingDrawableMap = drawableMap, isLoading = false)
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(errorMessage = result.message ?: "Failed to load bookings", isLoading = false)
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Unexpected error: ${e.localizedMessage}", isLoading = false)
                }
            }
        }
    }

    private suspend fun enrichBookingsWithMissingData(
        bookings: List<Booking>
    ): Pair<List<Booking>, Map<String, Int>> {
        val drawableMap = mutableMapOf<String, Int>()
        if (bookings.isEmpty()) return Pair(emptyList(), drawableMap)

        val enrichedList = coroutineScope {
            bookings.map { booking ->
                async {
                    var enriched = booking
                    try {
                        if (enriched.tenantId.isNotBlank()) {
                            val userDoc     = usersCol.document(enriched.tenantId).get().await()
                            val fetchedName = userDoc.getString("fullName")
                                ?: userDoc.getString("name")
                                ?: userDoc.getString("displayName")
                                ?: ""
                            if (fetchedName.isNotBlank()) enriched = enriched.copy(tenantName = fetchedName)
                        }
                    } catch (_: Exception) { }

                    try {
                        if (enriched.propertyCoverUrl.isBlank() && enriched.propertyId.isNotBlank()) {
                            var urlFound = false
                            val propDoc  = propertiesCol.document(enriched.propertyId).get().await()
                            val fetchedUrl = (propDoc.get("imageUrls") as? List<*>)
                                ?.filterIsInstance<String>()
                                ?.firstOrNull { it.isNotBlank() } ?: ""
                            if (fetchedUrl.isNotBlank()) {
                                enriched = enriched.copy(propertyCoverUrl = fetchedUrl)
                                urlFound = true
                            }
                            if (!urlFound) drawableMap[enriched.bookingId] = getPropertyImage(enriched.propertyId)
                        }
                    } catch (_: Exception) {
                        if (enriched.propertyId.isNotBlank()) drawableMap[enriched.bookingId] = getPropertyImage(enriched.propertyId)
                    }
                    enriched
                }
            }.awaitAll()
        }
        return Pair(enrichedList, drawableMap)
    }

    // ── Property Actions ──────────────────────────────────────────────────────

    fun approveProperty(propertyId: String) = viewModelScope.launch {
        if (propertyId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.approveProperty(propertyId)) { loadAllProperties() }
    }

    fun removeProperty(propertyId: String, reason: String = "Removed by admin") = viewModelScope.launch {
        if (propertyId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.rejectProperty(propertyId, reason)) { loadAllProperties() }
    }

    fun deleteProperty(propertyId: String) = viewModelScope.launch {
        // Guard: sirf Super Admin property delete kar sakta hai
        if (!_uiState.value.isSuperAdmin) {
            _uiState.update { it.copy(errorMessage = "Only Super Admin can delete properties.") }
            return@launch
        }
        if (propertyId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.deleteProperty(propertyId)) { loadAllProperties() }
    }

    // ── User Actions ──────────────────────────────────────────────────────────

    fun banUser(userId: String) = viewModelScope.launch {
        if (userId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.banUser(userId)) { loadAllUsers() }
    }

    fun unbanUser(userId: String) = viewModelScope.launch {
        if (userId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.unbanUser(userId)) { loadAllUsers() }
    }

    fun deleteUser(userId: String) = viewModelScope.launch {
        // Guard: sirf Super Admin user delete kar sakta hai
        if (!_uiState.value.isSuperAdmin) {
            _uiState.update { it.copy(errorMessage = "Only Super Admin can delete users.") }
            return@launch
        }
        if (userId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.deleteUser(userId)) { loadAllUsers() }
    }

    // ── Make/Remove Sub-Admin ─────────────────────────────────────────────────

    fun makeSubAdmin(userId: String, permissions: AdminPermissions) = viewModelScope.launch {
        if (!_uiState.value.isSuperAdmin) {
            _uiState.update { it.copy(errorMessage = "Only Super Admin can grant admin access.") }
            return@launch
        }
        if (userId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        handleActionResult(
            result    = adminRepository.makeSubAdmin(userId, permissions),
            onSuccess = {
                loadAllUsers()
                _uiState.update { it.copy(successMessage = "Admin access granted successfully.") }
            }
        )
    }

    fun removeSubAdmin(userId: String) = viewModelScope.launch {
        if (!_uiState.value.isSuperAdmin) {
            _uiState.update { it.copy(errorMessage = "Only Super Admin can revoke admin access.") }
            return@launch
        }
        if (userId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        handleActionResult(
            result    = adminRepository.removeSubAdmin(userId),
            onSuccess = {
                loadAllUsers()
                _uiState.update { it.copy(successMessage = "Admin access revoked successfully.") }
            }
        )
    }

    // ── Booking Actions ───────────────────────────────────────────────────────

    fun cancelBooking(bookingId: String) = viewModelScope.launch {
        if (bookingId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true) }
        handleActionResult(adminRepository.cancelBooking(bookingId)) { loadAllBookings() }
    }

    fun approveBooking(bookingId: String) = viewModelScope.launch {
        if (bookingId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val result = adminRepository.updateBookingStatus(bookingId, BookingStatus.CONFIRMED.name)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading      = false,
                            actionSuccess  = true,
                            successMessage = "Booking confirmed successfully!",
                            bookings       = state.bookings.map { b ->
                                if (b.bookingId == bookingId) b.copy(status = BookingStatus.CONFIRMED.name) else b
                            }
                        )
                    }
                    loadAllBookings()
                }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> Unit
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
        }
    }

    fun rejectBooking(bookingId: String) = viewModelScope.launch {
        if (bookingId.isBlank()) return@launch
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val result = adminRepository.updateBookingStatus(bookingId, BookingStatus.CANCELLED.name)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading      = false,
                            actionSuccess  = true,
                            successMessage = "Booking rejected.",
                            bookings       = state.bookings.map { b ->
                                if (b.bookingId == bookingId) b.copy(status = BookingStatus.CANCELLED.name) else b
                            }
                        )
                    }
                    loadAllBookings()
                }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> Unit
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
        }
    }

    private fun handleActionResult(result: Resource<Unit>, onSuccess: () -> Unit) {
        _uiState.update { state ->
            when (result) {
                is Resource.Success -> { onSuccess(); state.copy(isLoading = false, actionSuccess = true) }
                is Resource.Error   -> state.copy(isLoading = false, errorMessage = result.message ?: "Unknown error")
                is Resource.Loading -> state.copy(isLoading = true)
            }
        }
    }

    fun resetActionState() {
        _uiState.update { it.copy(actionSuccess = false, errorMessage = null, successMessage = null) }
    }
}