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
    val featuredProperties      : List<Property> = emptyList(),
    val nearbyProperties        : List<Property> = emptyList(),
    val allProperties           : List<Property> = emptyList(),
    val isLoading               : Boolean        = false,
    val errorMessage            : String?        = null,
    val totalProperties         : Int            = 0,
    val activeBookingsCount     : Int            = 0,
    val pendingRequestsCount    : Int            = 0,
    val totalRevenue            : Double         = 0.0,
    val averageRating           : Float          = 0f,
    // ✦ Favourites state
    val favouriteProperties     : List<Property> = emptyList(),
    val favouriteIds            : Set<String>    = emptySet(),
    val isFavouritesLoading     : Boolean        = false
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

    // ─────────────────────────────────────────────────────────────
    // Load user info and favourite IDs
    // ─────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────
    // ✦ FAVOURITES — IDs load
    // ─────────────────────────────────────────────────────────────
    private fun loadFavouriteIds(userId: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = firebaseDataManager.getFavouriteIds(userId)
                if (result is Resource.Success) {
                    _uiState.update { it.copy(favouriteIds = result.data.toSet()) }
                }
            } catch (_: Exception) {
                // silent — heart icons stay in default state
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ✦ FAVOURITES — Full list load
    // ─────────────────────────────────────────────────────────────
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
                    is Resource.Error   -> _uiState.update { it.copy(isFavouritesLoading = false) }
                    else                -> _uiState.update { it.copy(isFavouritesLoading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isFavouritesLoading = false) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ✦ FAVOURITES — Toggle (add/remove)
    // ─────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────
    // TENANT: load all approved properties
    // ─────────────────────────────────────────────────────────────
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val featuredResult = propertyRepository.getFeaturedProperties()
                val featured: List<Property> =
                    if (featuredResult is Resource.Success) featuredResult.data else emptyList()

                val nearbyResult = propertyRepository.getNearbyProperties()
                val nearby: List<Property> =
                    if (nearbyResult is Resource.Success) nearbyResult.data else emptyList()

                val combined = (featured + nearby).distinctBy { it.propertyId }

                val finalAll      : List<Property>
                val finalFeatured : List<Property>
                val finalNearby   : List<Property>

                if (combined.isEmpty()) {
                    val allResult = propertyRepository.getAllProperties()
                    val allList: List<Property> =
                        if (allResult is Resource.Success) allResult.data else emptyList()
                    finalAll      = allList
                    finalFeatured = allList.filter {  it.isFeatured }
                    finalNearby   = allList.filter { !it.isFeatured }
                } else {
                    finalAll      = combined
                    finalFeatured = featured.ifEmpty { combined.filter {  it.isFeatured } }
                    finalNearby   = nearby.ifEmpty   { combined.filter { !it.isFeatured } }
                }

                val errorMsg = when {
                    featuredResult is Resource.Error -> featuredResult.message
                    nearbyResult   is Resource.Error -> nearbyResult.message
                    else                             -> null
                }

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
    }

    // ─────────────────────────────────────────────────────────────
    // LANDLORD: load stats and own properties
    // ─────────────────────────────────────────────────────────────
    fun loadLandlordStats(landlordId: String) {
        viewModelScope.launch {
            try {
                val propertiesResult = propertyRepository.getMyProperties(landlordId)
                val properties: List<Property> =
                    if (propertiesResult is Resource.Success) propertiesResult.data
                    else emptyList()

                val totalProps = properties.size
                val avgRating  = if (properties.isNotEmpty())
                    properties.map { it.averageRating }.average().toFloat()
                else 0f

                val bookings     = bookingRepository.getLandlordBookings(landlordId)
                val activeCount  = bookings.count { it.status == BookingStatus.CONFIRMED.name }
                val pendingCount = bookings.count { it.status == BookingStatus.PENDING.name }

                val paymentsResult = paymentRepository.getLandlordPayments(landlordId)
                val revenue: Double =
                    if (paymentsResult is Resource.Success) paymentsResult.data.sumOf { it.amount }
                    else 0.0

                _uiState.update { state ->
                    state.copy(
                        featuredProperties   = properties,
                        allProperties        = properties,
                        totalProperties      = totalProps,
                        activeBookingsCount  = activeCount,
                        pendingRequestsCount = pendingCount,
                        totalRevenue         = revenue,
                        averageRating        = avgRating
                    )
                }

            } catch (_: Exception) {
                // silent fail
            }
        }
    }
}