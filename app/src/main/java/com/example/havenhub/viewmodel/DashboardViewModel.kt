package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Notification
import com.example.havenhub.data.Property
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

data class DashboardStats(
    val totalProperties   : Int    = 0,
    val totalBookings     : Int    = 0,
    val pendingBookings   : Int    = 0,
    val totalEarnings     : Double = 0.0,
    val thisMonthEarnings : Double = 0.0,
    val averageRating     : Double = 0.0,
    val totalUsers        : Int    = 0
)

data class DashboardUiState(
    val isLoading           : Boolean             = false,
    val stats               : DashboardStats      = DashboardStats(),
    val recentBookings      : List<Booking>       = emptyList(),
    val recentNotifications : List<Notification>  = emptyList(),
    val recentActivities    : List<Notification>  = emptyList(),   // ✅ Real activities
    val unreadNotifCount    : Int                 = 0,
    val allUsers            : List<User>          = emptyList(),
    val allProperties       : List<Property>      = emptyList(),
    val errorMessage        : String?             = null
)

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
        listenToRecentActivities()   // ✅ Real-time activity feed
        listenToAllBookings()        // ✅ Real-time bookings
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

    // ── ✅ Real-time activity feed ─────────────────────────────────────────────
    private fun listenToRecentActivities() {
        viewModelScope.launch {
            realtimeListener.listenToRecentActivities().collect { activities ->
                _uiState.update { state ->
                    state.copy(recentActivities = activities.take(10))
                }
            }
        }
    }

    // ── ✅ Real-time all bookings ──────────────────────────────────────────────
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
                            pendingBookings = bookings.count { it.status == BookingStatus.PENDING.name }
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

            val properties = if (propertiesResult is Resource.Success) propertiesResult.data else emptyList()
            val payments   = if (paymentsResult   is Resource.Success) paymentsResult.data   else emptyList()
            val users      = if (usersResult      is Resource.Success) usersResult.data      else emptyList()

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

            _uiState.update { state ->
                state.copy(
                    isLoading     = false,
                    allUsers      = users,
                    allProperties = properties,
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
}











