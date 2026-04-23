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

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val allResult = propertyRepository.getAllProperties()

                android.util.Log.d("HAVENHUB", "Result type: ${allResult::class.simpleName}")

                val allProperties = if (allResult is Resource.Success)
                    allResult.data ?: emptyList() else emptyList()

                android.util.Log.d("HAVENHUB", "Total properties: ${allProperties.size}")

                val featured = allProperties.filter { it.isFeatured }
                val featuredToShow = if (featured.isEmpty()) allProperties else featured

                android.util.Log.d("HAVENHUB", "Featured: ${featuredToShow.size}, Nearby: ${allProperties.size}")

                _uiState.update { state ->
                    state.copy(
                        featuredProperties = featuredToShow,
                        nearbyProperties   = allProperties,
                        isLoading          = false,
                        errorMessage       = if (allResult is Resource.Error) allResult.message else null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("HAVENHUB", "Error loading: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun loadLandlordStats(landlordId: String) {
        viewModelScope.launch {
            try {
                val propertiesResult = propertyRepository.getMyProperties(landlordId)
                val properties = if (propertiesResult is Resource.Success)
                    propertiesResult.data ?: emptyList() else emptyList<Property>()

                val avgRating = if (properties.isNotEmpty())
                    properties.map { it.averageRating.toFloat() }.average().toFloat()
                else 0f

                val bookings = bookingRepository.getLandlordBookings(landlordId)
                val activeCount  = bookings.count { it.status == BookingStatus.CONFIRMED.name }
                val pendingCount = bookings.count { it.status == BookingStatus.PENDING.name }

                val paymentsResult = paymentRepository.getLandlordPayments(landlordId)
                val revenue = if (paymentsResult is Resource.Success)
                    paymentsResult.data?.sumOf { it.amount } ?: 0.0 else 0.0

                _uiState.update { state ->
                    state.copy(
                        totalProperties      = properties.size,
                        activeBookingsCount  = activeCount,
                        pendingRequestsCount = pendingCount,
                        totalRevenue         = revenue,
                        averageRating        = avgRating
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("HAVENHUB", "Landlord stats error: ${e.localizedMessage}")
            }
        }
    }
}