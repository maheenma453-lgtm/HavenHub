package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Property
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.PaymentRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featuredProperties  : List<Property> = emptyList(),
    val nearbyProperties    : List<Property> = emptyList(),
    val allProperties       : List<Property> = emptyList(),
    val isLoading           : Boolean        = false,
    val errorMessage        : String?        = null,
    val totalProperties     : Int            = 0,
    val activeBookingsCount : Int            = 0,
    val pendingRequestsCount: Int            = 0,
    val totalRevenue        : Double         = 0.0,
    val averageRating       : Float          = 0f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingRepository : BookingRepository,
    private val paymentRepository : PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // TENANT: Saari approved properties load karo
    // ─────────────────────────────────────────────────────────────
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {

                // Featured properties
                val featuredResult = propertyRepository.getFeaturedProperties()
                val featured: List<Property> = when (featuredResult) {
                    is Resource.Success -> featuredResult.data
                    is Resource.Error   -> emptyList()
                    Resource.Loading    -> emptyList()
                }

                // Nearby = saari approved
                val nearbyResult = propertyRepository.getNearbyProperties()
                val nearby: List<Property> = when (nearbyResult) {
                    is Resource.Success -> nearbyResult.data
                    is Resource.Error   -> emptyList()
                    Resource.Loading    -> emptyList()
                }

                // Merge — deduplicate by propertyId
                val combined = (featured + nearby).distinctBy { it.propertyId }

                // Agar dono empty hain — getAllProperties fallback
                val finalAll     : List<Property>
                val finalFeatured: List<Property>
                val finalNearby  : List<Property>

                if (combined.isEmpty()) {
                    val allResult = propertyRepository.getAllProperties()
                    val allList: List<Property> = when (allResult) {
                        is Resource.Success -> allResult.data
                        else                -> emptyList()
                    }
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
    // LANDLORD: Stats + apni properties load karo
    // ─────────────────────────────────────────────────────────────
    fun loadLandlordStats(landlordId: String) {
        viewModelScope.launch {
            try {
                // Landlord ki apni properties
                val propertiesResult = propertyRepository.getMyProperties(landlordId)
                val properties: List<Property> = when (propertiesResult) {
                    is Resource.Success -> propertiesResult.data
                    else                -> emptyList()
                }

                val totalProps = properties.size
                val avgRating  = if (properties.isNotEmpty())
                    properties.map { it.averageRating }.average().toFloat()
                else 0f

                // Bookings
                val bookings     = bookingRepository.getLandlordBookings(landlordId)
                val activeCount  = bookings.count { it.status == BookingStatus.CONFIRMED.name }
                val pendingCount = bookings.count { it.status == BookingStatus.PENDING.name }

                // Revenue
                val paymentsResult = paymentRepository.getLandlordPayments(landlordId)
                val revenue: Double = when (paymentsResult) {
                    is Resource.Success -> paymentsResult.data.sumOf { it.amount }
                    else                -> 0.0
                }

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

            } catch (e: Exception) {
                // Silent fail — stats na aayein toh crash mat karo
            }
        }
    }
}
