package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.repository.AuthRepository
import com.havenhub.data.repository.BookingRepository
import com.havenhub.data.repository.PaymentRepository
import com.havenhub.data.repository.PropertyRepository
import com.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardStats(
    val totalProperties: Int = 0,
    val totalBookings: Int = 0,
    val pendingBookings: Int = 0,
    val totalEarnings: Double = 0.0,
    val thisMonthEarnings: Double = 0.0,
    val averageRating: Double = 0.0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _dashboardStats = MutableStateFlow<Resource<DashboardStats>>(Resource.Idle())
    val dashboardStats: StateFlow<Resource<DashboardStats>> = _dashboardStats.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            _dashboardStats.value = Resource.Loading()

            val propertiesResult = propertyRepository.getPropertiesByOwner(userId)
            val bookingsResult = bookingRepository.getBookingsByOwner(userId)
            val earningsResult = paymentRepository.getTotalEarnings(userId)

            val properties = if (propertiesResult is Resource.Success) propertiesResult.data ?: emptyList() else emptyList()
            val bookings = if (bookingsResult is Resource.Success) bookingsResult.data ?: emptyList() else emptyList()
            val earnings = if (earningsResult is Resource.Success) earningsResult.data ?: 0.0 else 0.0

            _dashboardStats.value = Resource.Success(
                DashboardStats(
                    totalProperties = properties.size,
                    totalBookings = bookings.size,
                    pendingBookings = bookings.count { it.status == "pending" },
                    totalEarnings = earnings,
                    thisMonthEarnings = paymentRepository.getThisMonthEarnings(userId),
                    averageRating = properties.mapNotNull { it.averageRating }.average().takeIf { !it.isNaN() } ?: 0.0
                )
            )
        }
    }
}
