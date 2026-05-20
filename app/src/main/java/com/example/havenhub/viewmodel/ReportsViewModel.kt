package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Payment
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.repository.AdminRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────
data class ReportsUiState(
    val isLoading         : Boolean          = false,
    val stats             : AdminReportStats = AdminReportStats(),
    val payments          : List<Payment>    = emptyList(),
    val selectedDateRange : Pair<Date, Date>? = null,
    val errorMessage      : String?          = null
)

// ── Stats data class ──────────────────────────────────────────────────────────
data class AdminReportStats(
    val totalRevenue      : Double = 0.0,
    val thisMonthRevenue  : Double = 0.0,
    val totalUsers        : Int    = 0,
    val totalBookings     : Int    = 0,
    val completedBookings : Int    = 0,
    val cancelledBookings : Int    = 0,
    val totalProperties   : Int    = 0,
    val activeProperties  : Int    = 0
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    // ── Load on startup with default "All Time" period ────────────────────────
    init {
        loadReportsByPeriod("All Time")
    }

    // ── Main entry point called from UI when period chip changes ──────────────
    // period: "All Time" | "Today" | "This Month"
    fun loadReportsByPeriod(period: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // ── Fetch all raw data from Firebase ──────────────────────────────
            val usersResult      = adminRepository.getAllUsers()
            val propertiesResult = adminRepository.getAllProperties()
            val bookingsResult   = adminRepository.getAllBookings()
            val paymentsResult   = adminRepository.getAllPayments()

            if (usersResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = usersResult.message) }
                return@launch
            }

            val allUsers      = (usersResult      as? Resource.Success)?.data ?: emptyList()
            val allProperties = (propertiesResult as? Resource.Success)?.data ?: emptyList()
            val allBookings   = (bookingsResult   as? Resource.Success)?.data ?: emptyList()
            val allPayments   = (paymentsResult   as? Resource.Success)?.data ?: emptyList()

            // ── Calculate date range boundaries based on selected period ───────
            // "All Time"   → no filter, use everything
            // "Today"      → midnight of today to now
            // "This Month" → 1st of current month to now
            val (startDate, endDate) = when (period) {

                "Today" -> {
                    // Start = today at 00:00:00, End = now
                    val start = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    Pair(start, Date())
                }

                "This Month" -> {
                    // Start = 1st of this month at 00:00:00, End = now
                    val start = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    Pair(start, Date())
                }

                else -> Pair(null, null) // "All Time" — no filter
            }

            // ── Filter bookings by period ──────────────────────────────────────
            val filteredBookings = if (startDate != null && endDate != null) {
                allBookings.filter { booking ->
                    val ts = booking.createdAt?.toDate()
                    ts != null && ts >= startDate && ts <= endDate
                }
            } else {
                allBookings // All Time — no filter
            }

            // ── Filter payments by period ──────────────────────────────────────
            val filteredPayments = if (startDate != null && endDate != null) {
                allPayments.filter { payment ->
                    val ts = payment.createdAt?.toDate()
                    ts != null && ts >= startDate && ts <= endDate
                }
            } else {
                allPayments // All Time — no filter
            }

            // ── Filter users by period (registration date) ────────────────────
            val filteredUsers = if (startDate != null && endDate != null) {
                allUsers.filter { user ->
                    val ts = user.createdAt?.toDate()
                    ts != null && ts >= startDate && ts <= endDate
                }
            } else {
                allUsers // All Time — no filter
            }

            // ── Calculate revenue from filtered COMPLETED payments only ────────
            val totalRevenue = filteredPayments
                .filter { it.status == PaymentStatus.COMPLETED.name }
                .sumOf { it.amountDouble }

            // ── Booking status counts from filtered bookings ───────────────────
            val completedCount = filteredBookings.count { it.status == "COMPLETED" }
            val cancelledCount = filteredBookings.count { it.status == "CANCELLED" }

            // ── Properties are not time-filtered (static data) ────────────────
            // Properties count doesn't change based on time period —
            // showing total/active properties at all times makes more sense
            val activePropertiesCount = allProperties.count { it.status == "APPROVED" }

            // ── Update UI state with filtered results ─────────────────────────
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    payments  = filteredPayments,
                    stats     = AdminReportStats(
                        totalRevenue      = totalRevenue,
                        totalUsers        = filteredUsers.size,
                        totalBookings     = filteredBookings.size,
                        completedBookings = completedCount,
                        cancelledBookings = cancelledCount,
                        totalProperties   = allProperties.size,
                        activeProperties  = activePropertiesCount
                    )
                )
            }
        }
    }

    // ── Legacy method kept for backward compatibility ──────────────────────────
    // Called from old code — now delegates to loadReportsByPeriod("All Time")
    fun loadAllReportsData() {
        loadReportsByPeriod("All Time")
    }

    // ── Custom date range filter for payment screen ───────────────────────────
    fun loadFilteredPayments(startDate: Date? = null, endDate: Date? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = adminRepository.getAllPayments()) {
                is Resource.Success -> {
                    var payments = result.data
                    // Apply date filter if both dates provided
                    if (startDate != null && endDate != null) {
                        payments = payments.filter { payment ->
                            val ts = payment.createdAt?.toDate()
                            ts != null && ts >= startDate && ts <= endDate
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, payments = payments) }
                }
                is Resource.Error   -> {
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