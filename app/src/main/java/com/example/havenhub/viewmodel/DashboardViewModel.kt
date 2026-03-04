package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Property
import com.example.havenhub.data.Payment
import com.example.havenhub.data.Booking
import com.example.havenhub.repository.AuthRepository
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
import java.util.Calendar
import javax.inject.Inject

// Dashboard Stats Data Class
data class DashboardStats(
    val totalProperties: Int = 0,
    val totalBookings: Int = 0,
    val pendingBookings: Int = 0,
    val totalEarnings: Double = 0.0,
    val thisMonthEarnings: Double = 0.0,
    val averageRating: Double = 0.0
)

data class DashboardUiState(
    val isLoading: Boolean = false,
    val stats: DashboardStats = DashboardStats(),
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isLoading = true) }

            // Fetching results from repositories
            val propertiesResult = propertyRepository.getMyProperties(userId)

            // ✅ FIXED: Using 'getLandlordBookings' instead of 'getUserBookings'
            // Kyunke aapki repository mein yahi naam hai.
            val bookings = bookingRepository.getLandlordBookings(userId)

            val paymentsResult = paymentRepository.getLandlordPayments(userId)

            // Handling Property Results
            val properties = if (propertiesResult is Resource.Success<List<Property>>) {
                propertiesResult.data ?: emptyList()
            } else emptyList()

            // Handling Payment Results
            val payments = if (paymentsResult is Resource.Success<List<Payment>>) {
                paymentsResult.data ?: emptyList()
            } else emptyList()

            // Calculations
            val totalEarnings = payments.sumOf { it.amount }

            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val thisMonthEarnings = payments.filter { payment ->
                payment.createdAt?.let { timestamp ->
                    val pCal = Calendar.getInstance()
                    pCal.time = timestamp.toDate()
                    pCal.get(Calendar.MONTH) == currentMonth && pCal.get(Calendar.YEAR) == currentYear
                } ?: false
            }.sumOf { it.amount }

            val avgRating = if (properties.isNotEmpty()) {
                properties.map { it.averageRating.toDouble() }.average().takeIf { !it.isNaN() } ?: 0.0
            } else 0.0

            // Final State Update
            _uiState.update {
                it.copy(
                    isLoading = false,
                    stats = DashboardStats(
                        totalProperties = properties.size,
                        totalBookings = bookings.size, // 'bookings' is already a List from your Repo
                        pendingBookings = bookings.count { b -> b.status == BookingStatus.PENDING },
                        totalEarnings = totalEarnings,
                        thisMonthEarnings = thisMonthEarnings,
                        averageRating = avgRating
                    )
                )
            }
        }
    }
}