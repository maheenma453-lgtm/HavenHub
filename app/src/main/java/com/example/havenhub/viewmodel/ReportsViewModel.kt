package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Payment
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.repository.PaymentRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// ✅ Reports ke liye UI State define ki
data class ReportsUiState(
    val isLoading: Boolean = false,
    val stats: AdminReportStats = AdminReportStats(),
    val payments: List<Payment> = emptyList(),
    val selectedDateRange: Pair<Date, Date>? = null,
    val errorMessage: String? = null
)

data class AdminReportStats(
    val totalRevenue: Double = 0.0,
    val thisMonthRevenue: Double = 0.0,
    val totalUsers: Int = 0,
    val totalBookings: Int = 0,
    val completedBookings: Int = 0,
    val cancelledBookings: Int = 0,
    val totalProperties: Int = 0,
    val activeProperties: Int = 0
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadAllReportsData()
    }

    // ✅ Unified loading method
    fun loadAllReportsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Sab data parallel mein fetch karne ke liye
            val usersResult = adminRepository.getAllUsers()
            val propertiesResult = adminRepository.getAllProperties()
            val bookingsResult = adminRepository.getAllBookings()
            val paymentsResult = adminRepository.getAllPayments()

            // Check for critical errors (Users lazmi hain stats ke liye)
            if (usersResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = usersResult.message) }
                return@launch
            }

            // Data extraction using your Resource class logic
            val users = (usersResult as? Resource.Success)?.data ?: emptyList()
            val properties = (propertiesResult as? Resource.Success)?.data ?: emptyList()
            val bookings = (bookingsResult as? Resource.Success)?.data ?: emptyList()
            val allPayments = (paymentsResult as? Resource.Success)?.data ?: emptyList()

            // Stats Calculation
            val totalRevenue = allPayments
                .filter { it.status == PaymentStatus.COMPLETED }
                .sumOf { it.amount }

            val completed = bookings.count { it.status.name == "COMPLETED" }
            val cancelled = bookings.count { it.status.name == "CANCELLED" }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    payments = allPayments,
                    stats = AdminReportStats(
                        totalRevenue = totalRevenue,
                        totalUsers = users.size,
                        totalBookings = bookings.size,
                        completedBookings = completed,
                        cancelledBookings = cancelled,
                        totalProperties = properties.size,
                        activeProperties = properties.count { it.status.name == "APPROVED" }
                    )
                )
            }
        }
    }

    // ✅ Date Filter logic with UI State update
    fun loadFilteredPayments(startDate: Date? = null, endDate: Date? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = adminRepository.getAllPayments()

            when (result) {
                is Resource.Success -> {
                    var payments = result.data
                    if (startDate != null && endDate != null) {
                        payments = payments.filter { payment ->
                            val ts = payment.createdAt?.toDate()
                            ts != null && ts >= startDate && ts <= endDate
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, payments = payments) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun setDateRange(start: Date, end: Date) {
        _uiState.update { it.copy(selectedDateRange = Pair(start, end)) }
        loadFilteredPayments(start, end)
    }

    fun clearDateRange() {
        _uiState.update { it.copy(selectedDateRange = null) }
        loadFilteredPayments()
    }
}