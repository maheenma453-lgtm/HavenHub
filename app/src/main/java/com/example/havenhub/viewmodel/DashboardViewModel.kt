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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

data class UserChartPoint(
    val label      : String,
    val newUsers   : Int,
    val activeUsers: Int
)

data class RevenueChartPoint(
    val label  : String,
    val revenue: Double
)

data class PropertyStatusSlice(
    val label   : String,
    val count   : Int,
    val colorHex: Long
)

data class DashboardStats(
    val totalProperties  : Int    = 0,
    val totalBookings    : Int    = 0,
    val pendingBookings  : Int    = 0,
    val totalEarnings    : Double = 0.0,
    val thisMonthEarnings: Double = 0.0,
    val averageRating    : Double = 0.0,
    val totalUsers       : Int    = 0
)

data class DashboardUiState(
    val isLoading            : Boolean                   = false,
    val stats                : DashboardStats            = DashboardStats(),
    val recentBookings       : List<Booking>             = emptyList(),
    val recentNotifications  : List<Notification>        = emptyList(),
    val recentActivities     : List<Notification>        = emptyList(),
    val unreadNotifCount     : Int                       = 0,
    val allUsers             : List<User>                = emptyList(),
    val allProperties        : List<Property>            = emptyList(),
    val errorMessage         : String?                   = null,
    val userChartPoints      : List<UserChartPoint>      = emptyList(),
    val revenueChartPoints   : List<RevenueChartPoint>   = emptyList(),
    val propertyStatusSlices : List<PropertyStatusSlice> = emptyList(),
    val chartRangeLabel      : String                    = "This Month",
    val adminUserName        : String?                   = null,
    val adminPhotoUrl        : String?                   = null,
    // null = still loading, "super_admin" or "sub_admin" when resolved
    val adminUserRole        : String?                   = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val adminRepository : AdminRepository,
    private val realtimeListener: FirebaseRealtimeListener
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadAdminProfile()
        loadDashboard()
        listenToAdminUserNotifications()
        listenToRecentActivities()
        listenToAllBookings()
        listenToAllUsers()
    }

    // =========================================================================
    // ADMIN PROFILE — directly Firestore se fetch karo
    //
    // ROLE RESOLUTION LOGIC (priority order):
    //   1. rawRole == "super_admin"              → super_admin
    //   2. rawRole == "sub_admin"                → sub_admin
    //   3. rawRole == "admin" + NO permissions   → super_admin
    //   4. rawRole == "admin" + HAS permissions  → sub_admin (legacy records)
    //   5. Anything else (tenant/landlord)       → should not reach dashboard
    //      but default to super_admin safely
    //
    // KEY FIX: "else -> super_admin" default hatao — null raho jab tak
    // Firestore se actual value na aaye. Agar role clearly "sub_admin" ya
    // "admin" without permissions hai tabhi super_admin set karo.
    // =========================================================================
    private fun loadAdminProfile() {
        val adminUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(adminUid)
                    .get()
                    .await()

                if (!doc.exists()) return@launch

                val fullName = doc.getString("fullName")?.ifBlank { null }
                val photoUrl = doc.getString("profileImageUrl")?.ifBlank { null }

                // Read raw role exactly as stored in Firestore — lowercase for comparison
                val rawRole = doc.getString("role")?.lowercase()?.trim() ?: ""

                // Check if adminPermissions map exists and is non-null
                // Sub-Admin ke doc mein yeh field ZAROOR hoti hai
                // Super Admin ke doc mein yeh field exist NAHI karti
                val hasPermissionsMap = doc.contains("adminPermissions") &&
                        doc.get("adminPermissions") != null

                // FIXED: Clear, unambiguous role resolution — no wrong defaults
                val resolvedRole: String = when {
                    rawRole == "super_admin" -> "super_admin"
                    rawRole == "sub_admin" -> "sub_admin"
                    rawRole == "admin" && hasPermissionsMap -> "sub_admin"   // legacy record
                    rawRole == "admin" -> "super_admin"
                    else -> {
                        // Unexpected role — log and return, don't guess
                        android.util.Log.w(
                            "DASHBOARD_VM",
                            "Unexpected role '$rawRole' for uid=$adminUid — skipping role set"
                        )
                        return@launch
                    }
                }

                android.util.Log.d(
                    "DASHBOARD_VM",
                    "loadAdminProfile: uid=$adminUid rawRole=$rawRole " +
                            "hasPermissions=$hasPermissionsMap resolvedRole=$resolvedRole"
                )

                _uiState.update { state ->
                    state.copy(
                        adminUserName = fullName,
                        adminPhotoUrl = photoUrl,
                        adminUserRole = resolvedRole
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "DASHBOARD_VM",
                    "loadAdminProfile failed: ${e.localizedMessage}"
                )
                // Silently fail — dashboard still works without role label
            }
        }
    }

    private fun listenToAdminUserNotifications() {
        val adminUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            realtimeListener.listenToNotifications(adminUserId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { notifications ->
                    _uiState.update { state ->
                        state.copy(
                            unreadNotifCount = notifications.count { !it.isRead },
                            recentNotifications = notifications.take(10)
                        )
                    }
                }
        }
    }

    private fun listenToRecentActivities() {
        viewModelScope.launch {
            realtimeListener.listenToRecentActivities()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { activities ->
                    _uiState.update { state ->
                        state.copy(recentActivities = activities.take(10))
                    }
                }
        }
    }

    private fun listenToAllBookings() {
        viewModelScope.launch {
            realtimeListener.listenToAllBookings()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { bookings ->
                    val recentFive = bookings
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        .take(5)
                    _uiState.update { state ->
                        state.copy(
                            recentBookings = recentFive,
                            stats = state.stats.copy(
                                totalBookings = bookings.size,
                                pendingBookings = bookings.count {
                                    it.status == BookingStatus.PENDING.name
                                }
                            )
                        )
                    }
                }
        }
    }

    private fun listenToAllUsers() {
        viewModelScope.launch {
            realtimeListener.listenToAllUsers()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { users ->
                    _uiState.update { state ->
                        state.copy(
                            allUsers = users,
                            stats = state.stats.copy(totalUsers = users.size)
                        )
                    }
                }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val propertiesResult = adminRepository.getAllProperties()
            val paymentsResult = adminRepository.getAllPayments()
            val usersResult = adminRepository.getAllUsers()

            val properties = if (propertiesResult is Resource.Success)
                propertiesResult.data ?: emptyList() else emptyList()
            val payments = if (paymentsResult is Resource.Success)
                paymentsResult.data ?: emptyList() else emptyList()
            val users = if (usersResult is Resource.Success)
                usersResult.data ?: emptyList() else emptyList()

            val totalEarnings = payments.sumOf { it.amountDouble }

            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val thisMonthEarnings = payments.filter { payment ->
                payment.createdAt?.let { ts ->
                    val pCal = Calendar.getInstance().apply { time = ts.toDate() }
                    pCal.get(Calendar.MONTH) == currentMonth &&
                            pCal.get(Calendar.YEAR) == currentYear
                } ?: false
            }.sumOf { it.amountDouble }

            val avgRating = if (properties.isNotEmpty())
                properties.map { it.averageRating.toDouble() }.average()
                    .takeIf { !it.isNaN() } ?: 0.0
            else 0.0

            val userChartPoints = buildUserChartPoints(users, payments)
            val revenueChartPoints = buildRevenueChartPoints(payments)
            val propertySlices = buildPropertyStatusSlices(properties)

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    allUsers = users,
                    allProperties = properties,
                    userChartPoints = userChartPoints,
                    revenueChartPoints = revenueChartPoints,
                    propertyStatusSlices = propertySlices,
                    // IMPORTANT: loadAdminProfile() se set hone wali values
                    // yahan overwrite mat karo — woh independent fetch hai
                    adminUserName = state.adminUserName,
                    adminPhotoUrl = state.adminPhotoUrl,
                    adminUserRole = state.adminUserRole,
                    stats = state.stats.copy(
                        totalProperties = properties.size,
                        totalEarnings = totalEarnings,
                        thisMonthEarnings = thisMonthEarnings,
                        averageRating = avgRating,
                        totalUsers = users.size
                    )
                )
            }
        }
    }

    private fun buildUserChartPoints(
        users: List<User>,
        payments: List<Payment>
    ): List<UserChartPoint> {
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return (6 downTo 0).mapIndexed { index, daysBack ->
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysBack) }
            val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
            val year = dayCal.get(Calendar.YEAR)

            val newUsers = users.count { user ->
                user.createdAt?.let { ts ->
                    val uCal = Calendar.getInstance().apply { time = ts.toDate() }
                    uCal.get(Calendar.DAY_OF_YEAR) == dayOfYear &&
                            uCal.get(Calendar.YEAR) == year
                } ?: false
            }

            val activeUserIds = payments.filter { payment ->
                payment.createdAt?.let { ts ->
                    val pCal = Calendar.getInstance().apply { time = ts.toDate() }
                    pCal.get(Calendar.DAY_OF_YEAR) == dayOfYear &&
                            pCal.get(Calendar.YEAR) == year
                } ?: false
            }.map { it.payerId }.toSet()

            val dayIndex = dayCal.get(Calendar.DAY_OF_WEEK) - 2
            val label = dayLabels.getOrElse(
                if (dayIndex < 0) dayIndex + 7 else dayIndex
            ) { dayLabels[index % 7] }

            UserChartPoint(label = label, newUsers = newUsers, activeUsers = activeUserIds.size)
        }
    }

    private fun buildRevenueChartPoints(payments: List<Payment>): List<RevenueChartPoint> {
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return (6 downTo 0).mapIndexed { index, daysBack ->
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysBack) }
            val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
            val year = dayCal.get(Calendar.YEAR)

            val dayRevenue = payments.filter { payment ->
                payment.createdAt?.let { ts ->
                    val pCal = Calendar.getInstance().apply { time = ts.toDate() }
                    pCal.get(Calendar.DAY_OF_YEAR) == dayOfYear &&
                            pCal.get(Calendar.YEAR) == year
                } ?: false
            }.sumOf { it.amountDouble }

            val dayIndex = dayCal.get(Calendar.DAY_OF_WEEK) - 2
            val label = dayLabels.getOrElse(
                if (dayIndex < 0) dayIndex + 7 else dayIndex
            ) { dayLabels[index % 7] }

            RevenueChartPoint(label = label, revenue = dayRevenue)
        }
    }

    private fun buildPropertyStatusSlices(properties: List<Property>): List<PropertyStatusSlice> {
        val approved = properties.count { it.status == PropertyStatus.APPROVED.name }
        val pending = properties.count { it.status == PropertyStatus.PENDING.name }
        val underReview = properties.count { it.status == PropertyStatus.UNDER_REVIEW.name }
        val rejected = properties.count { it.status == PropertyStatus.REJECTED.name }

        return listOf(
            PropertyStatusSlice("Published", approved, 0xFF2ECC71),
            PropertyStatusSlice("Pending", pending, 0xFF4A90D9),
            PropertyStatusSlice("Under Review", underReview, 0xFFE67E22),
            PropertyStatusSlice("Rejected", rejected, 0xFF9E9E9E)
        ).filter { it.count > 0 }
            .ifEmpty { listOf(PropertyStatusSlice("No Data", 1, 0xFFEEEEEE)) }
    }
}