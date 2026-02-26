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
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

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

    private val _reportStats = MutableStateFlow<Resource<AdminReportStats>>(Resource.Loading)
    val reportStats: StateFlow<Resource<AdminReportStats>> = _reportStats.asStateFlow()

    private val _paymentReports = MutableStateFlow<Resource<List<Payment>>>(Resource.Loading)
    val paymentReports: StateFlow<Resource<List<Payment>>> = _paymentReports.asStateFlow()

    private val _selectedDateRange = MutableStateFlow<Pair<Date, Date>?>(null)
    val selectedDateRange: StateFlow<Pair<Date, Date>?> = _selectedDateRange.asStateFlow()

    init {
        loadReports()
    }

    // FIX: getAdminReportStats() doesn't exist — build stats from existing methods
    fun loadReports() {
        viewModelScope.launch {
            _reportStats.value = Resource.Loading

            val usersResult     = adminRepository.getAllUsers()
            val propertiesResult = adminRepository.getAllProperties()
            val bookingsResult  = adminRepository.getAllBookings()
            val paymentsResult  = adminRepository.getAllPayments()

            if (usersResult is Resource.Error) {
                _reportStats.value = Resource.Error(usersResult.message)
                return@launch
            }

            val users      = (usersResult as Resource.Success).data
            val properties = (propertiesResult as? Resource.Success)?.data ?: emptyList()
            val bookings   = (bookingsResult as? Resource.Success)?.data ?: emptyList()
            val payments   = (paymentsResult as? Resource.Success)?.data ?: emptyList()

            val totalRevenue = payments
                .filter { it.status == PaymentStatus.COMPLETED }
                .sumOf { it.amount }

            _reportStats.value = Resource.Success(
                AdminReportStats(
                    totalRevenue      = totalRevenue,
                    totalUsers        = users.size,
                    totalBookings     = bookings.size,
                    totalProperties   = properties.size,
                    activeProperties  = properties.count { it.isAvailable }
                )
            )
        }
    }

    // FIX: getPaymentReports() doesn't exist — use getAllPayments() and filter by date
    fun loadPaymentReports(startDate: Date? = null, endDate: Date? = null) {
        viewModelScope.launch {
            _paymentReports.value = Resource.Loading
            val result = adminRepository.getAllPayments()
            if (result is Resource.Error) {
                _paymentReports.value = Resource.Error(result.message)
                return@launch
            }

            var payments = (result as Resource.Success).data

            // Filter by date range if provided
            if (startDate != null && endDate != null) {
                payments = payments.filter { payment ->
                    val ts = payment.createdAt?.toDate()
                    ts != null && ts >= startDate && ts <= endDate
                }
            }

            _paymentReports.value = Resource.Success(payments)
        }
    }

    fun setDateRange(start: Date, end: Date) {
        _selectedDateRange.value = Pair(start, end)
        loadPaymentReports(start, end)
    }

    fun clearDateRange() {
        _selectedDateRange.value = null
        loadPaymentReports()
    }

    // FIX: exportReportToCsv() doesn't exist in AdminRepository — removed
    // Add this to AdminRepository if CSV export is needed
}