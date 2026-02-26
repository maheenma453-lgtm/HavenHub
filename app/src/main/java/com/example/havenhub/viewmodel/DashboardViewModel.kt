package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.BookingRepository
import com.example.havenhub.repository.PaymentRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
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

    private val _dashboardStats = MutableStateFlow<Resource<DashboardStats>?>(null)
    val dashboardStats: StateFlow<Resource<DashboardStats>?> = _dashboardStats.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _dashboardStats.value = Resource.Loading

            val propertiesResult = propertyRepository.getMyProperties(userId)
            val bookingsResult   = bookingRepository.getUserBookings(userId)
            val paymentsResult   = paymentRepository.getLandlordPayments(userId)

            val properties = if (propertiesResult is Resource.Success) propertiesResult.data else emptyList()
            val bookings   = if (bookingsResult is Resource.Success) bookingsResult.data else emptyList()
            val payments   = if (paymentsResult is Resource.Success) paymentsResult.data else emptyList()

            val totalEarnings = payments.sumOf { it.amount }

            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear  = Calendar.getInstance().get(Calendar.YEAR)
            val thisMonthEarnings = payments.filter { payment ->
                payment.createdAt?.let {
                    val cal = Calendar.getInstance()
                    cal.time = it.toDate()
                    cal.get(Calendar.MONTH) == currentMonth &&
                            cal.get(Calendar.YEAR)  == currentYear
                } ?: false
            }.sumOf { it.amount }

            _dashboardStats.value = Resource.Success(
                DashboardStats(
                    totalProperties   = properties.size,
                    totalBookings     = bookings.size,
                    pendingBookings   = bookings.count { it.status == BookingStatus.PENDING },
                    totalEarnings     = totalEarnings,
                    thisMonthEarnings = thisMonthEarnings,
                    averageRating     = properties.map { it.averageRating.toDouble() }
                        .average()
                        .takeIf { !it.isNaN() } ?: 0.0
                )
            )
        }
    }
}