package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Payment
import com.havenhub.data.repository.AdminRepository
import com.havenhub.data.repository.PaymentRepository
import com.havenhub.utils.Resource
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
    val newUsersThisMonth: Int = 0,
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

    private val _reportStats = MutableStateFlow<Resource<AdminReportStats>>(Resource.Idle())
    val reportStats: StateFlow<Resource<AdminReportStats>> = _reportStats.asStateFlow()

    private val _paymentReports = MutableStateFlow<Resource<List<Payment>>>(Resource.Idle())
    val paymentReports: StateFlow<Resource<List<Payment>>> = _paymentReports.asStateFlow()

    private val _selectedDateRange = MutableStateFlow<Pair<Date, Date>?>(null)
    val selectedDateRange: StateFlow<Pair<Date, Date>?> = _selectedDateRange.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _reportStats.value = Resource.Loading()
            _reportStats.value = adminRepository.getAdminReportStats()
        }
    }

    fun loadPaymentReports(startDate: Date? = null, endDate: Date? = null) {
        viewModelScope.launch {
            _paymentReports.value = Resource.Loading()
            _paymentReports.value = adminRepository.getPaymentReports(startDate, endDate)
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

    fun exportReport() {
        viewModelScope.launch {
            adminRepository.exportReportToCsv()
        }
    }
}
