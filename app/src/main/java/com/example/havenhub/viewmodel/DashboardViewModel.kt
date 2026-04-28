package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Notification
import com.example.havenhub.data.Payment
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.data.User
import com.example.havenhub.remote.FirebaseRealtimeListener
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

// ── Chart Data Models ──────────────────────────────────────────────────────────

/** One point on the Users Overview line chart (last 7 days) */
data class UserChartPoint(
    val label      : String,  // e.g. "Mon", "Tue"
    val newUsers   : Int,     // users registered on that day
    val activeUsers: Int      // users who had a booking on that day
)

/** One point on the Revenue Overview line chart (last 7 days) */
data class RevenueChartPoint(
    val label  : String,      // e.g. "Mon", "Tue"
    val revenue: Double       // sum of completed payments that day
)

/** Slice for the Property Status donut chart */
data class PropertyStatusSlice(
    val label   : String,
    val count   : Int,
    val colorHex: Long        // ARGB e.g. 0xFF2ECC71
)

// ── Stats ──────────────────────────────────────────────────────────────────────

data class DashboardStats(
    val totalProperties   : Int    = 0,
    val totalBookings     : Int    = 0,
    val pendingBookings   : Int    = 0,
    val totalEarnings     : Double = 0.0,
    val thisMonthEarnings : Double = 0.0,
    val averageRating     : Double = 0.0,
    val totalUsers        : Int    = 0
)

// ── UI State ───────────────────────────────────────────────────────────────────

data class DashboardUiState(
    val isLoading              : Boolean                 = false,
    val stats                  : DashboardStats          = DashboardStats(),
    val recentBookings         : List<Booking>           = emptyList(),
    val recentNotifications    : List<Notification>      = emptyList(),
    val recentActivities       : List<Notification>      = emptyList(),
    val unreadNotifCount       : Int                     = 0,
    val allUsers               : List<User>              = emptyList(),
    val allProperties          : List<Property>          = emptyList(),
    val errorMessage           : String?                 = null,

    // ── Chart Data (real Firebase) ────────────────────────────────────────────
    val userChartPoints        : List<UserChartPoint>    = emptyList(),
    val revenueChartPoints     : List<RevenueChartPoint> = emptyList(),
    val propertyStatusSlices   : List<PropertyStatusSlice> = emptyList(),

    // Selected time range label shown in chart header dropdown
    val chartRangeLabel        : String                  = "This Month"
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val adminRepository  : AdminRepository,
    private val realtimeListener : FirebaseRealtimeListener
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        listenToAdminNotifications()
        listenToRecentActivities()
        listenToAllBookings()
    }

    // ── Real-time notifications ───────────────────────────────────────────────

    private fun listenToAdminNotifications() {
        viewModelScope.launch {
            realtimeListener.listenToAdminNotifications().collect { notifications ->
                _uiState.update { state ->
                    state.copy(
                        recentNotifications = notifications.take(10),
                        unreadNotifCount    = notifications.count { !it.isRead }
                    )
                }
            }
        }
    }

    // ── Real-time activity feed ───────────────────────────────────────────────

    private fun listenToRecentActivities() {
        viewModelScope.launch {
            realtimeListener.listenToRecentActivities().collect { activities ->
                _uiState.update { state ->
                    state.copy(recentActivities = activities.take(10))
                }
            }
        }
    }

    // ── Real-time bookings ────────────────────────────────────────────────────

    private fun listenToAllBookings() {
        viewModelScope.launch {
            realtimeListener.listenToAllBookings().collect { bookings ->
                val recentFive = bookings
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    .take(5)

                _uiState.update { state ->
                    state.copy(
                        recentBookings = recentFive,
                        stats = state.stats.copy(
                            totalBookings   = bookings.size,
                            pendingBookings = bookings.count {
                                it.status == BookingStatus.PENDING.name
                            }
                        )
                    )
                }
            }
        }
    }

    // ── Initial full load ─────────────────────────────────────────────────────

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val propertiesResult = adminRepository.getAllProperties()
            val paymentsResult   = adminRepository.getAllPayments()
            val usersResult      = adminRepository.getAllUsers()

            val properties = if (propertiesResult is Resource.Success) propertiesResult.data ?: emptyList() else emptyList()
            val payments   = if (paymentsResult   is Resource.Success) paymentsResult.data   ?: emptyList() else emptyList()
            val users      = if (usersResult      is Resource.Success) usersResult.data      ?: emptyList() else emptyList()

            val totalEarnings = payments.sumOf { it.amount }

            val cal          = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear  = cal.get(Calendar.YEAR)

            val thisMonthEarnings = payments.filter { payment ->
                payment.createdAt?.let { ts ->
                    val pCal = Calendar.getInstance()
                    pCal.time = ts.toDate()
                    pCal.get(Calendar.MONTH) == currentMonth &&
                            pCal.get(Calendar.YEAR) == currentYear
                } ?: false
            }.sumOf { it.amount }

            val avgRating = if (properties.isNotEmpty())
                properties.map { it.averageRating.toDouble() }.average()
                    .takeIf { !it.isNaN() } ?: 0.0
            else 0.0

            // ── Build chart data ──────────────────────────────────────────────
            val userChartPoints    = buildUserChartPoints(users, payments)
            val revenueChartPoints = buildRevenueChartPoints(payments)
            val propertySlices     = buildPropertyStatusSlices(properties)

            _uiState.update { state ->
                state.copy(
                    isLoading            = false,
                    allUsers             = users,
                    allProperties        = properties,
                    userChartPoints      = userChartPoints,
                    revenueChartPoints   = revenueChartPoints,
                    propertyStatusSlices = propertySlices,
                    stats = state.stats.copy(
                        totalProperties   = properties.size,
                        totalEarnings     = totalEarnings,
                        thisMonthEarnings = thisMonthEarnings,
                        averageRating     = avgRating,
                        totalUsers        = users.size
                    )
                )
            }
        }
    }

    // ── Chart Builders ────────────────────────────────────────────────────────

    /**
     * Last 7 days ka data — har din ke naye users aur active users (booking wale)
     */
    private fun buildUserChartPoints(
        users   : List<User>,
        payments: List<Payment>
    ): List<UserChartPoint> {
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val today     = Calendar.getInstance()

        return (6 downTo 0).mapIndexed { index, daysBack ->
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysBack)
            }
            val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
            val year      = dayCal.get(Calendar.YEAR)

            // Naye users registered is din
            val newUsers = users.count { user ->
                user.createdAt?.let { ts ->
                    val uCal = Calendar.getInstance().apply { time = ts.toDate() }
                    uCal.get(Calendar.DAY_OF_YEAR) == dayOfYear &&
                            uCal.get(Calendar.YEAR) == year
                } ?: false
            }

            // Active users = jo users ne is din payment ki
            val activeUserIds = payments
                .filter { payment ->
                    payment.createdAt?.let { ts ->
                        val pCal = Calendar.getInstance().apply { time = ts.toDate() }
                        pCal.get(Calendar.DAY_OF_YEAR) == dayOfYear &&
                                pCal.get(Calendar.YEAR) == year
                    } ?: false
                }
                .map { it.payerId }
                .toSet()

            val dayIndex = dayCal.get(Calendar.DAY_OF_WEEK) - 2  // Mon=0
            val label    = dayLabels.getOrElse(if (dayIndex < 0) dayIndex + 7 else dayIndex) {
                dayLabels[index % 7]
            }

            UserChartPoint(
                label       = label,
                newUsers    = newUsers,
                activeUsers = activeUserIds.size
            )
        }
    }

    /**
     * Last 7 days ka revenue — sirf COMPLETED payments count honge
     */
    private fun buildRevenueChartPoints(payments: List<Payment>): List<RevenueChartPoint> {
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        return (6 downTo 0).mapIndexed { index, daysBack ->
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysBack)
            }
            val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
            val year      = dayCal.get(Calendar.YEAR)

            val dayRevenue = payments
                .filter { payment ->
                    payment.createdAt?.let { ts ->
                        val pCal = Calendar.getInstance().apply { time = ts.toDate() }
                        pCal.get(Calendar.DAY_OF_YEAR) == dayOfYear &&
                                pCal.get(Calendar.YEAR) == year
                    } ?: false
                }
                .sumOf { it.amount }

            val dayIndex = dayCal.get(Calendar.DAY_OF_WEEK) - 2
            val label    = dayLabels.getOrElse(if (dayIndex < 0) dayIndex + 7 else dayIndex) {
                dayLabels[index % 7]
            }

            RevenueChartPoint(label = label, revenue = dayRevenue)
        }
    }

    /**
     * Property status breakdown — Published(APPROVED), Pending, Draft(UNDER_REVIEW), Rejected
     */
    private fun buildPropertyStatusSlices(properties: List<Property>): List<PropertyStatusSlice> {
        val approved    = properties.count { it.status == PropertyStatus.APPROVED.name }
        val pending     = properties.count { it.status == PropertyStatus.PENDING.name }
        val underReview = properties.count { it.status == PropertyStatus.UNDER_REVIEW.name }
        val rejected    = properties.count { it.status == PropertyStatus.REJECTED.name }

        return listOf(
            PropertyStatusSlice("Published",  approved,    0xFF2ECC71),
            PropertyStatusSlice("Pending",    pending,     0xFF4A90D9),
            PropertyStatusSlice("Draft",      underReview, 0xFFE67E22),
            PropertyStatusSlice("Rejected",   rejected,    0xFF9E9E9E)
        ).filter { it.count > 0 }
            .ifEmpty {
                // Agar koi bhi nahi toh placeholder show karo
                listOf(PropertyStatusSlice("No Data", 1, 0xFFEEEEEE))
            }
    }
}
