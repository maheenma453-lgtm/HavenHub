package com.example.havenhub.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.AppSettings
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Property
import com.example.havenhub.data.User
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.PaymentRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import com.google.firebase.auth.FirebaseAuth
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
// TenantInfo — ek tenant ki complete info (booking + user + property)
// ─────────────────────────────────────────────────────────────────────────────
data class TenantInfo(
    val user          : User    = User(),
    val booking       : Booking = Booking(),
    val propertyTitle : String  = "",
    val propertyCity  : String  = ""
)

// ═══════════════════════════════════════════════════════════════════════════════
// HomeUiState — single source of truth for HomeScreen UI
// ═══════════════════════════════════════════════════════════════════════════════
data class HomeUiState(
    val featuredProperties   : List<Property>   = emptyList(),
    val nearbyProperties     : List<Property>   = emptyList(),
    val allProperties        : List<Property>   = emptyList(),
    val isLoading            : Boolean          = false,
    val errorMessage         : String?          = null,

    // ✦ NEW — logged-in user ki profile info (name + photo)
    val currentUserName      : String           = "",
    val currentUserPhotoUrl  : String           = "",
    val currentUserInitials  : String           = "",

    // Landlord stats
    val totalProperties      : Int              = 0,
    val activeBookingsCount  : Int              = 0,
    val activeTenantsCount   : Int              = 0,
    val pendingRequestsCount : Int              = 0,
    val totalRevenue         : Double           = 0.0,
    val averageRating        : Float            = 0f,

    // Tenants list (landlord ke saare tenants)
    val tenants              : List<TenantInfo> = emptyList(),
    val isTenantsLoading     : Boolean          = false,
    val tenantsError         : String?          = null,

    // Favourites
    val favouriteProperties  : List<Property>   = emptyList(),
    val favouriteIds         : Set<String>      = emptySet(),
    val isFavouritesLoading  : Boolean          = false,

    // App-wide settings loaded from Firestore app_settings/global
    val appSettings          : AppSettings?     = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// HomeViewModel
// ═══════════════════════════════════════════════════════════════════════════════
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val propertyRepository : PropertyRepository,
    private val bookingRepository  : BookingRepository,
    private val paymentRepository  : PaymentRepository,
    private val firebaseDataManager: FirebaseDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _userRole = MutableStateFlow("")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val firestore  = FirebaseFirestore.getInstance()
    private val usersCol   = firestore.collection("users")
    private val propsCol   = firestore.collection("properties")

    init {
        loadUserInfo()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USER INFO
    // ══════════════════════════════════════════════════════════════════════════

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser == null) {
                    _userId.value   = ""
                    _userRole.value = "tenant"
                    return@launch
                }
                _userId.value = firebaseUser.uid

                val doc = firestore
                    .collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                _userRole.value = doc.getString("role") ?: "tenant"

                // ✦ NEW — Firestore se profile image + name load karo
                val fullName  = doc.getString("fullName") ?: ""
                val photoUrl  = doc.getString("profileImageUrl") ?: ""
                val initials  = fullName
                    .trim()
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "?" }

                _uiState.update { state ->
                    state.copy(
                        currentUserName     = fullName,
                        currentUserPhotoUrl = photoUrl,
                        currentUserInitials = initials
                    )
                }

                loadFavouriteIds(firebaseUser.uid)

            } catch (_: Exception) {
                _userId.value   = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                _userRole.value = "tenant"
            }
        }
    }

    // ✦ NEW — Call this after EditProfile saves so home icon updates immediately
    fun refreshCurrentUserProfile() {
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val doc = firestore.collection("users").document(uid).get().await()
                val fullName = doc.getString("fullName") ?: ""
                val photoUrl = doc.getString("profileImageUrl") ?: ""
                val initials = fullName
                    .trim()
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "?" }

                _uiState.update { state ->
                    state.copy(
                        currentUserName     = fullName,
                        currentUserPhotoUrl = photoUrl,
                        currentUserInitials = initials
                    )
                }
            } catch (e: Exception) {
                Log.w("HOME_VM", "refreshCurrentUserProfile: ${e.localizedMessage}")
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // APP SETTINGS
    // ══════════════════════════════════════════════════════════════════════════

    private suspend fun loadAppSettings(): AppSettings {
        return try {
            val doc = firestore
                .collection("app_settings")
                .document("global")
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            AppSettings(
                isMaintenanceMode     = doc.getBoolean("isMaintenanceMode")  ?: false,
                maintenanceMessage    = doc.getString("maintenanceMessage"),
                minimumAppVersion     = doc.getString("minimumAppVersion")   ?: "1.0.0",
                latestAppVersion      = doc.getString("latestAppVersion")    ?: "1.0.0",
                forceUpdate           = doc.getBoolean("forceUpdate")        ?: false,
                platformFeePercent    = doc.getDouble("platformFeePercent")  ?: 5.0,
                maxPropertyImages     = (doc.getLong("maxPropertyImages")    ?: 10L).toInt(),
                maxBookingDaysAdvance = (doc.getLong("maxBookingDaysAdvance") ?: 90L).toInt(),
                featuredPropertyIds   = (doc.get("featuredPropertyIds") as? List<*>)
                    ?.filterIsInstance<String>()                             ?: emptyList(),
                announcementBanner    = doc.getString("announcementBanner"),
                supportEmail          = doc.getString("supportEmail")        ?: "support@havenhub.co.za",
                termsOfServiceUrl     = doc.getString("termsOfServiceUrl")   ?: "https://havenhub.co.za/terms",
                privacyPolicyUrl      = doc.getString("privacyPolicyUrl")    ?: "https://havenhub.co.za/privacy",
                updatedAt             = doc.getTimestamp("updatedAt")
            )
        } catch (_: Exception) {
            AppSettings()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FAVOURITES
    // ══════════════════════════════════════════════════════════════════════════

    private fun loadFavouriteIds(userId: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = firebaseDataManager.getFavouriteIds(userId)
                if (result is Resource.Success) {
                    _uiState.update { it.copy(favouriteIds = result.data.toSet()) }
                }
            } catch (_: Exception) { /* silent */ }
        }
    }

    fun loadFavouriteProperties() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isFavouritesLoading = true) }
            try {
                val result = firebaseDataManager.getFavouriteProperties(uid)
                when (result) {
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            favouriteProperties = result.data,
                            favouriteIds        = result.data.map { p -> p.propertyId }.toSet(),
                            isFavouritesLoading = false
                        )
                    }
                    else -> _uiState.update { it.copy(isFavouritesLoading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isFavouritesLoading = false) }
            }
        }
    }

    fun toggleFavourite(propertyId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val isFav = _uiState.value.favouriteIds.contains(propertyId)
            if (isFav) {
                _uiState.update {
                    it.copy(
                        favouriteIds        = it.favouriteIds - propertyId,
                        favouriteProperties = it.favouriteProperties.filter { p -> p.propertyId != propertyId }
                    )
                }
                firebaseDataManager.removeFavourite(uid, propertyId)
            } else {
                _uiState.update { it.copy(favouriteIds = it.favouriteIds + propertyId) }
                firebaseDataManager.addFavourite(uid, propertyId)
                loadFavouriteProperties()
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TENANT HOME — approved properties + AppSettings
    // ══════════════════════════════════════════════════════════════════════════

    fun loadHomeData() {
        viewModelScope.launch { fetchAndUpdateTenantData() }
    }

    fun refreshHomeData() {
        viewModelScope.launch { fetchAndUpdateTenantData() }
    }

    private suspend fun fetchAndUpdateTenantData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val settings  = loadAppSettings()
            val allResult = propertyRepository.getAllProperties()

            val allList: List<Property> =
                if (allResult is Resource.Success) allResult.data else emptyList()

            val adminFeaturedIds = settings.featuredPropertyIds

            val finalFeatured: List<Property>
            val finalNearby  : List<Property>

            if (adminFeaturedIds.isNotEmpty()) {
                finalFeatured = allList.filter { adminFeaturedIds.contains(it.propertyId) }
                finalNearby   = allList.filter { !adminFeaturedIds.contains(it.propertyId) }
            } else {
                finalFeatured = allList.filter { it.isFeatured }
                finalNearby   = allList.filter { !it.isFeatured }
            }

            val errorMsg = if (allResult is Resource.Error) allResult.message else null

            _uiState.update { state ->
                state.copy(
                    featuredProperties = finalFeatured,
                    nearbyProperties   = finalNearby,
                    allProperties      = allList,
                    appSettings        = settings,
                    isLoading          = false,
                    errorMessage       = if (allList.isEmpty()) errorMsg else null
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LANDLORD — stats + revenue
    // ══════════════════════════════════════════════════════════════════════════

    fun loadLandlordStats(landlordId: String) {
        viewModelScope.launch { fetchAndUpdateLandlordStats(landlordId) }
    }

    fun refreshLandlordStats() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch { fetchAndUpdateLandlordStats(uid) }
    }

    private suspend fun fetchAndUpdateLandlordStats(landlordId: String) {
        try {
            val propertiesResult = propertyRepository.getMyProperties(landlordId)
            val properties: List<Property> =
                if (propertiesResult is Resource.Success) propertiesResult.data else emptyList()

            val totalProps = properties.size
            val avgRating  = if (properties.isNotEmpty())
                properties.map { it.averageRating }.average().toFloat()
            else 0f

            val bookings     = bookingRepository.getLandlordBookings(landlordId)
            val activeCount  = bookings.count { it.status == BookingStatus.CONFIRMED.name }
            val pendingCount = bookings.count { it.status == BookingStatus.PENDING.name }

            val activeTenantsCount = bookings
                .filter { it.status == BookingStatus.CONFIRMED.name }
                .map { it.tenantId }
                .distinct()
                .size

            val paymentsResult = paymentRepository.getLandlordPayments(landlordId)
            val revenue: Double =
                if (paymentsResult is Resource.Success)
                    paymentsResult.data.sumOf { it.amountDouble }
                else 0.0

            _uiState.update { state ->
                state.copy(
                    featuredProperties   = properties,
                    allProperties        = properties,
                    totalProperties      = totalProps,
                    activeBookingsCount  = activeCount,
                    activeTenantsCount   = activeTenantsCount,
                    pendingRequestsCount = pendingCount,
                    totalRevenue         = revenue,
                    averageRating        = avgRating
                )
            }

        } catch (_: Exception) { /* silent — stale data stays */ }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TENANTS — landlord ke saare tenants load karo
    // ══════════════════════════════════════════════════════════════════════════

    fun loadTenants() {
        val landlordId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTenantsLoading = true, tenantsError = null) }
            try {
                val bookings = bookingRepository.getLandlordBookings(landlordId)

                if (bookings.isEmpty()) {
                    _uiState.update { it.copy(isTenantsLoading = false, tenants = emptyList()) }
                    return@launch
                }

                // Latest booking per tenant (duplicates nahi)
                val latestPerTenant = bookings
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    .distinctBy { it.tenantId }

                val tenantInfoList = mutableListOf<TenantInfo>()

                for (booking in latestPerTenant) {
                    if (booking.tenantId.isBlank()) continue
                    try {
                        val userDoc = usersCol.document(booking.tenantId).get().await()
                        val user    = userDoc.toObject(User::class.java) ?: continue

                        var propTitle = booking.propertyTitle.ifBlank { "Property" }
                        var propCity  = ""
                        try {
                            val propDoc = propsCol.document(booking.propertyId).get().await()
                            propTitle = propDoc.getString("title") ?: propTitle
                            propCity  = propDoc.getString("city")  ?: ""
                        } catch (e: Exception) {
                            Log.w("HOME_VM", "Property fetch skip: ${e.localizedMessage}")
                        }

                        tenantInfoList.add(
                            TenantInfo(
                                user          = user,
                                booking       = booking,
                                propertyTitle = propTitle,
                                propertyCity  = propCity
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("HOME_VM", "Tenant fetch fail ${booking.tenantId}: ${e.localizedMessage}")
                    }
                }

                Log.d("HOME_VM", "loadTenants: ${tenantInfoList.size} tenants loaded")
                _uiState.update { it.copy(isTenantsLoading = false, tenants = tenantInfoList) }

            } catch (e: Exception) {
                Log.e("HOME_VM", "loadTenants FAIL: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isTenantsLoading = false,
                        tenantsError     = e.localizedMessage ?: "Failed to load tenants"
                    )
                }
            }
        }
    }

    fun refreshTenants() { loadTenants() }

    // ── FIX: user.name → user.fullName ──────────────────────────────────────
    fun filteredTenants(searchQuery: String): List<TenantInfo> {
        val q = searchQuery.lowercase().trim()
        if (q.isEmpty()) return _uiState.value.tenants
        return _uiState.value.tenants.filter {
            it.user.fullName.lowercase().contains(q)  ||
                    it.user.email.lowercase().contains(q)     ||
                    it.propertyTitle.lowercase().contains(q)  ||
                    it.propertyCity.lowercase().contains(q)
        }
    }
}
