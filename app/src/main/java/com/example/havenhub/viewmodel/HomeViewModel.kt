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
    val isLoading           : Boolean         = false,
    val errorMessage        : String?         = null,

    // Landlord Quick Actions ke liye
    val totalProperties     : Int             = 0,
    val activeBookingsCount : Int             = 0,
    val pendingRequestsCount: Int             = 0,   // ✅ NEW: PENDING status bookings
    val totalRevenue        : Double          = 0.0,
    val averageRating       : Float           = 0f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingRepository : BookingRepository,
    private val paymentRepository : PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val featuredResult = propertyRepository.getFeaturedProperties()
                val nearbyResult   = propertyRepository.getNearbyProperties()

                _uiState.update { state ->
                    state.copy(
                        featuredProperties = if (featuredResult is Resource.Success)
                            featuredResult.data ?: emptyList() else emptyList(),
                        nearbyProperties   = if (nearbyResult is Resource.Success)
                            nearbyResult.data ?: emptyList() else emptyList(),
                        isLoading          = false,
                        errorMessage       = when {
                            featuredResult is Resource.Error -> featuredResult.message
                            nearbyResult   is Resource.Error -> nearbyResult.message
                            else                             -> null
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun loadLandlordStats(landlordId: String) {
        viewModelScope.launch {
            try {
                // 1. Total Properties
                val propertiesResult = propertyRepository.getMyProperties(landlordId)
                val properties = if (propertiesResult is Resource.Success)
                    propertiesResult.data ?: emptyList() else emptyList<Property>()
                val totalProps = properties.size

                // Average Rating
                val avgRating = if (properties.isNotEmpty())
                    properties.map { it.averageRating.toFloat() }.average().toFloat()
                else 0f

                // 2. Bookings — Active (CONFIRMED) + Pending (PENDING)
                val bookings = bookingRepository.getLandlordBookings(landlordId)
                val activeCount  = bookings.count {
                    it.status == BookingStatus.CONFIRMED.name
                }
                val pendingCount = bookings.count {   // ✅ NEW
                    it.status == BookingStatus.PENDING.name
                }

                // 3. Total Revenue
                val paymentsResult = paymentRepository.getLandlordPayments(landlordId)
                val revenue = if (paymentsResult is Resource.Success)
                    paymentsResult.data?.sumOf { it.amount } ?: 0.0 else 0.0

                _uiState.update { state ->
                    state.copy(
                        totalProperties      = totalProps,
                        activeBookingsCount  = activeCount,
                        pendingRequestsCount = pendingCount,   // ✅ NEW
                        totalRevenue         = revenue,
                        averageRating        = avgRating
                    )
                }
            } catch (e: Exception) {
                // Stats load fail hone par quietly ignore
            }
        }
    }
}