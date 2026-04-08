package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

data class DashboardUiState(
    val isLoading: Boolean = false,
    val stats: DashboardStats = DashboardStats(),
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val propertiesResult = adminRepository.getAllProperties()
            val bookingsResult = adminRepository.getAllBookings()
            val paymentsResult = adminRepository.getAllPayments()

            val properties = if (propertiesResult is Resource.Success) {
                propertiesResult.data
            } else emptyList()

            val bookings = if (bookingsResult is Resource.Success) {
                bookingsResult.data
            } else emptyList()

            val payments = if (paymentsResult is Resource.Success) {
                paymentsResult.data
            } else emptyList()

            val totalEarnings = payments.sumOf { it.amount }

            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val thisMonthEarnings = payments.filter { payment ->
                payment.createdAt?.let { timestamp ->
                    val pCal = Calendar.getInstance()
                    pCal.time = timestamp.toDate()
                    pCal.get(Calendar.MONTH) == currentMonth &&
                            pCal.get(Calendar.YEAR) == currentYear
                } ?: false
            }.sumOf { it.amount }

            val avgRating = if (properties.isNotEmpty()) {
                properties.map { it.averageRating.toDouble() }
                    .average()
                    .takeIf { !it.isNaN() } ?: 0.0
            } else 0.0

            _uiState.update {
                it.copy(
                    isLoading = false,
                    stats = DashboardStats(
                        totalProperties   = properties.size,
                        totalBookings     = bookings.size,
                        pendingBookings   = bookings.count { b -> b.status == BookingStatus.PENDING.name },
                        totalEarnings     = totalEarnings,
                        thisMonthEarnings = thisMonthEarnings,
                        averageRating     = avgRating
                    )
                )
            }
        }
    }
}