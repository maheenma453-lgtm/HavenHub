package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Property
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

data class HomeUiState(
    val featuredProperties   : List<Property> = emptyList(),
    val nearbyProperties     : List<Property> = emptyList(),
    val allProperties        : List<Property> = emptyList(),
    val isLoading            : Boolean        = false,
    val errorMessage         : String?        = null,
    val totalProperties      : Int            = 0,
    val activeBookingsCount  : Int            = 0,
    val activeTenantsCount   : Int            = 0,
    val pendingRequestsCount : Int            = 0,
    val totalRevenue         : Double         = 0.0,
    val averageRating        : Float          = 0f,
    val favouriteProperties  : List<Property> = emptyList(),
    val favouriteIds         : Set<String>    = emptySet(),
    val isFavouritesLoading  : Boolean        = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val propertyRepository : PropertyRepository,
    private val bookingRepository  : BookingRepository,
    private val paymentRepository  : PaymentRepository,
    private val firebaseDataManager: FirebaseDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _userId   = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _userRole = MutableStateFlow("")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

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

                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()
                _userRole.value = doc.getString("role") ?: "tenant"

                loadFavouriteIds(firebaseUser.uid)

            } catch (e: Exception) {
                _userId.value   = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                _userRole.value = "tenant"
            }
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
    // TENANT — load approved properties
    //
    // ✅ FIX: loadHomeData() aur refreshHomeData() dono fetchAndUpdateTenantData()
    //    call karte hain bina kisi condition ke.
    //
    // ✅ ROOT FIX (ExploreMap properties missing):
    //    Pehle fetchAndUpdateTenantData() mein:
    //      - getFeaturedProperties() sirf isFeatured=true wali laata tha
    //      - getNearbyProperties() sirf isFeatured=false wali
    //      - combined = featured + nearby
    //      - Nai properties jo isFeatured=false hain woh nearby mein aati thin
    //        LEKIN agar nearby result empty tha toh combined bhi incomplete tha
    //
    //    Ab:
    //      - seedAll = propertyRepository.getAllProperties() → SAARI APPROVED
    //      - featured = seedAll mein se isFeatured=true
    //      - nearby   = seedAll mein se isFeatured=false
    //      - allProperties = seedAll (poora list, koi miss nahi)
    //
    //    Isse ExploreMapScreen ko allProperties mein SAARI nai 12 properties
    //    milti hain — chahe koi featured ho ya nearby.
    // ══════════════════════════════════════════════════════════════════════════

    fun loadHomeData() {
        viewModelScope.launch {
            fetchAndUpdateTenantData()
        }
    }

    fun refreshHomeData() {
        viewModelScope.launch {
            fetchAndUpdateTenantData()
        }
    }

    private suspend fun fetchAndUpdateTenantData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            // ✅ SINGLE SOURCE OF TRUTH: Ek hi Firestore query → saari APPROVED properties
            // PropertyRepository.getAllProperties() → fetchApproved() → whereEqualTo("status","APPROVED")
            val allResult = propertyRepository.getAllProperties()

            val allList: List<Property> =
                if (allResult is Resource.Success) allResult.data else emptyList()

            // ✅ Client-side split: featured aur nearby allList se nikalte hain
            // Koi property miss nahi hogi — same list ko sirf filter kiya hai
            val finalFeatured = allList.filter { it.isFeatured }
            val finalNearby   = allList.filter { !it.isFeatured }
            val finalAll      = allList  // ExploreMap yahi use karta hai

            val errorMsg = if (allResult is Resource.Error) allResult.message else null

            _uiState.update { state ->
                state.copy(
                    featuredProperties = finalFeatured,
                    nearbyProperties   = finalNearby,
                    allProperties      = finalAll,
                    isLoading          = false,
                    errorMessage       = if (finalAll.isEmpty()) errorMsg else null
                )
            }

        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = e.localizedMessage)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LANDLORD — own properties + booking stats + revenue
    // ══════════════════════════════════════════════════════════════════════════

    fun loadLandlordStats(landlordId: String) {
        viewModelScope.launch {
            fetchAndUpdateLandlordStats(landlordId)
        }
    }

    fun refreshLandlordStats() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            fetchAndUpdateLandlordStats(uid)
        }
    }

    private suspend fun fetchAndUpdateLandlordStats(landlordId: String) {
        try {
            // getMyProperties → ALL statuses (PENDING, APPROVED, REJECTED)
            val propertiesResult = propertyRepository.getMyProperties(landlordId)
            val properties: List<Property> =
                if (propertiesResult is Resource.Success) propertiesResult.data
                else emptyList()

            val totalProps = properties.size

            val avgRating = if (properties.isNotEmpty())
                properties.map { it.averageRating }.average().toFloat()
            else 0f

            val bookings     = bookingRepository.getLandlordBookings(landlordId)
            val activeCount  = bookings.count { it.status == BookingStatus.CONFIRMED.name }
            val pendingCount = bookings.count { it.status == BookingStatus.PENDING.name }

            val activeTenantsCount = bookings
                .filter  { it.status == BookingStatus.CONFIRMED.name }
                .map     { it.tenantId }
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

        } catch (_: Exception) {
            // Silent fail — stale data stays visible
        }
    }
}
