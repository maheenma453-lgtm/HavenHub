package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Notification
import com.example.havenhub.data.NotificationType
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.DashboardViewModel
import com.example.havenhub.viewmodel.PropertyStatusSlice
import com.example.havenhub.viewmodel.RevenueChartPoint
import com.example.havenhub.viewmodel.UserChartPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun notifIcon(type: String): ImageVector = when (type) {
    NotificationType.BOOKING_REQUESTED.name,
    NotificationType.BOOKING_CONFIRMED.name,
    NotificationType.BOOKING_CANCELLED.name,
    NotificationType.BOOKING_COMPLETED.name  -> Icons.Default.CalendarMonth
    NotificationType.PAYMENT_RECEIVED.name,
    NotificationType.PAYMENT_FAILED.name,
    NotificationType.REFUND_ISSUED.name      -> Icons.Default.Payment
    NotificationType.NEW_REVIEW.name,
    NotificationType.REVIEW_REPLY.name       -> Icons.Default.Star
    NotificationType.NEW_MESSAGE.name        -> Icons.Default.Message
    NotificationType.PROPERTY_APPROVED.name,
    NotificationType.PROPERTY_REJECTED.name  -> Icons.Default.Home
    NotificationType.ACCOUNT_VERIFIED.name,
    NotificationType.ACCOUNT_SUSPENDED.name  -> Icons.Default.Person
    else                                     -> Icons.Default.Notifications
}

private fun notifColor(type: String): Color = when (type) {
    NotificationType.BOOKING_REQUESTED.name  -> Color(0xFF4A90D9)
    NotificationType.BOOKING_CONFIRMED.name  -> SuccessGreen
    NotificationType.BOOKING_CANCELLED.name  -> ErrorRed
    NotificationType.BOOKING_COMPLETED.name  -> Color(0xFF00897B)
    NotificationType.PAYMENT_RECEIVED.name   -> GoldAccent
    NotificationType.PAYMENT_FAILED.name     -> ErrorRed
    NotificationType.REFUND_ISSUED.name      -> WarningOrange
    NotificationType.NEW_REVIEW.name,
    NotificationType.REVIEW_REPLY.name       -> WarningOrange
    NotificationType.NEW_MESSAGE.name        -> Color(0xFF4A90D9)
    NotificationType.PROPERTY_APPROVED.name  -> SuccessGreen
    NotificationType.PROPERTY_REJECTED.name  -> ErrorRed
    NotificationType.ACCOUNT_VERIFIED.name   -> SuccessGreen
    NotificationType.ACCOUNT_SUSPENDED.name  -> ErrorRed
    else                                     -> Color(0xFF9B59B6)
}

private fun timeAgo(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val diffMs = System.currentTimeMillis() - timestamp.toDate().time
    return when {
        diffMs < 60_000     -> "Just now"
        diffMs < 3_600_000  -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)}m ago"
        diffMs < 86_400_000 -> "${TimeUnit.MILLISECONDS.toHours(diffMs)}h ago"
        else                -> "${TimeUnit.MILLISECONDS.toDays(diffMs)}d ago"
    }
}

private fun bookingStatusColor(status: String): Color = when (status) {
    BookingStatus.CONFIRMED.name  -> SuccessGreen
    BookingStatus.CANCELLED.name  -> ErrorRed
    BookingStatus.COMPLETED.name  -> PrimaryNavyLight
    BookingStatus.CHECKED_IN.name -> Color(0xFF00897B)
    else                          -> WarningOrange
}

private fun bookingStatusLabel(status: String): String =
    status.lowercase().replaceFirstChar { it.uppercase() }

// ─── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController : NavController,
    viewModel     : DashboardViewModel = hiltViewModel(),
    authViewModel : AuthViewModel      = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val stats      = uiState.stats

    val adminName     = authState.currentUser?.displayName?.ifBlank { "Admin" } ?: "Admin"
    val adminInitial  = adminName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
    val adminPhotoUrl = authState.currentUser?.photoUrl?.toString()

    val drawerState      = rememberDrawerState(DrawerValue.Closed)
    val scope            = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // ── Logout Dialog ──────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ErrorRed.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = ErrorRed, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text("Logout?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to logout from HavenHub Admin?",
                    color     = TextSecondary,
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.signOut()
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, Logout", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showLogoutDialog = false },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.5.dp, GoldAccent)       // ✅ Gold border
                ) {
                    Text("Cancel", color = PrimaryNavy, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            AdminDrawerContent(
                navController = navController,
                adminName     = adminName,
                adminInitial  = adminInitial,
                adminPhotoUrl = adminPhotoUrl,
                unreadCount   = uiState.unreadNotifCount,
                onClose       = { scope.launch { drawerState.close() } },
                onLogoutClick = {
                    scope.launch { drawerState.close() }
                    showLogoutDialog = true
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopBar(
                    notifCount          = uiState.unreadNotifCount,
                    adminName           = adminName,
                    adminInitial        = adminInitial,
                    adminPhotoUrl       = adminPhotoUrl,
                    onMenuClick         = { scope.launch { drawerState.open() } },
                    onNotificationClick = {
                        navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route) { launchSingleTop = true }
                    }
                )
            },
            containerColor = BackgroundGray
        ) { padding ->

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryNavy)
                }
                return@Scaffold
            }

            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item { HeroBanner(adminName = adminName) }

                // ── Stats ────────────────────────────────────────
                item {
                    Spacer(Modifier.height(20.dp))
                    PremiumSectionTitle("Platform Overview", Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModernStatCard(
                                icon     = Icons.Default.Home,
                                label    = "Properties",
                                value    = "${stats.totalProperties}",
                                change   = "+8.3%",
                                positive = true,
                                gradient = listOf(PrimaryNavy, PrimaryNavyLight),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatCard(
                                icon     = Icons.Default.People,
                                label    = "Total Users",
                                value    = "${stats.totalUsers}",
                                change   = "+12.5%",
                                positive = true,
                                gradient = listOf(Color(0xFF00695C), Color(0xFF26C6DA)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModernStatCard(
                                icon     = Icons.Default.CalendarMonth,
                                label    = "Bookings",
                                value    = "${stats.totalBookings}",
                                change   = "+15.7%",
                                positive = true,
                                gradient = listOf(Color(0xFFE65100), Color(0xFFFF9800)),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatCard(
                                icon     = Icons.Default.AccountBalanceWallet,
                                label    = "Revenue",
                                value    = "PKR ${"%.0f".format(stats.totalEarnings)}",
                                change   = "+18.6%",
                                positive = true,
                                gradient = listOf(GoldAccentDark, GoldAccent),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        PendingHighlightCard(pendingCount = stats.pendingBookings) {
                            navController.navigate(Screen.ManageBookings.route)
                        }
                    }
                }

                // ── Quick Actions ─────────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    PremiumSectionTitle("Quick Actions", Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(12.dp))
                    val actions = listOf(
                        Triple(Icons.Default.CheckCircle,    "Verify Properties", Screen.VerifyProperties.route),
                        Triple(Icons.Default.VerifiedUser,   "Verify Users",      Screen.VerifyUsers.route),
                        Triple(Icons.Default.ManageAccounts, "Manage Users",      Screen.ManageUsers.route),
                        Triple(Icons.Default.HomeWork,       "Manage Properties", Screen.ManageProperties.route),
                        Triple(Icons.Default.CalendarMonth,  "Manage Bookings",   Screen.ManageBookings.route),
                        Triple(Icons.Default.BarChart,       "View Reports",      Screen.Reports.route),
                        Triple(Icons.Default.Payment,        "Payment Reports",   Screen.PaymentReports.route),
                    )
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        actions.chunked(2).forEach { rowActions ->
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowActions.forEach { (icon, label, route) ->
                                    QuickActionCard(
                                        icon     = icon,
                                        label    = label,
                                        modifier = Modifier.weight(1f),
                                        onClick  = { navController.navigate(route) }
                                    )
                                }
                                if (rowActions.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ── Charts ────────────────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    PremiumSectionTitle("User Overview", Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(12.dp))
                    UsersOverviewChart(
                        points   = uiState.userChartPoints,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    PremiumSectionTitle("Revenue Overview", Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(12.dp))
                    RevenueOverviewChart(
                        points   = uiState.revenueChartPoints,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    PremiumSectionTitle("Property Status", Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(12.dp))
                    PropertyStatusChart(
                        slices   = uiState.propertyStatusSlices,
                        total    = stats.totalProperties,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }

                // ── Recent Activity ───────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        PremiumSectionTitle("Recent Activity")
                        TextButton(onClick = {
                            navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                        }) {
                            Text("View All", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // ✅ Gold border card
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape     = RoundedCornerShape(18.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border    = BorderStroke(1.dp, GoldAccent.copy(0.35f))
                    ) {
                        if (uiState.recentActivities.isEmpty()) {
                            DashboardEmptyBox(Icons.Default.Notifications, "No recent activity")
                        } else {
                            Column(Modifier.padding(4.dp)) {
                                uiState.recentActivities.forEachIndexed { idx, notif ->
                                    RealActivityRow(notif)
                                    if (idx < uiState.recentActivities.lastIndex)
                                        HorizontalDivider(
                                            modifier  = Modifier.padding(horizontal = 16.dp),
                                            color     = GoldAccent.copy(0.15f)
                                        )
                                }
                            }
                        }
                    }
                }

                // ── Recent Bookings ───────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        PremiumSectionTitle("Recent Bookings")
                        TextButton(onClick = { navController.navigate(Screen.ManageBookings.route) }) {
                            Text("View All", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // ✅ Gold border card
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape     = RoundedCornerShape(18.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border    = BorderStroke(1.dp, GoldAccent.copy(0.35f))
                    ) {
                        if (uiState.recentBookings.isEmpty()) {
                            DashboardEmptyBox(Icons.Default.CalendarMonth, "No bookings yet")
                        } else {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PrimaryNavy.copy(0.06f), GoldAccent.copy(0.06f))
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    TableHeaderCell("Guest",    Modifier.weight(1.3f))
                                    TableHeaderCell("Property", Modifier.weight(1.7f))
                                    TableHeaderCell("Amount",   Modifier.weight(1.3f))
                                    TableHeaderCell("Status",   Modifier.weight(1.2f))
                                }
                                uiState.recentBookings.forEachIndexed { idx, booking ->
                                    RealBookingRow(booking)
                                    if (idx < uiState.recentBookings.lastIndex)
                                        HorizontalDivider(
                                            modifier  = Modifier.padding(horizontal = 16.dp),
                                            color     = GoldAccent.copy(0.12f)
                                        )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP BAR
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    notifCount         : Int,
    adminName          : String,
    adminInitial       : String,
    adminPhotoUrl      : String?,
    onMenuClick        : () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick     : () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, "Menu", tint = Color.White)
            }
        },
        title = {
            Text("HavenHub", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        actions = {
            BadgedBox(badge = {
                if (notifCount > 0) {
                    Badge(containerColor = GoldAccent) {
                        Text(
                            if (notifCount > 99) "99+" else "$notifCount",
                            fontSize = 9.sp,
                            color    = PrimaryNavy
                        )
                    }
                }
            }) {
                IconButton(onClick = onNotificationClick) {
                    Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                }
            }
            Spacer(Modifier.width(4.dp))
            // ✅ Gold avatar border
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(2.dp, GoldAccent, CircleShape)
                    .background(PrimaryNavy)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (!adminPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model              = adminPhotoUrl,
                        contentDescription = "Admin Photo",
                        modifier           = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Text(adminInitial, color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy)
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// DRAWER
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminDrawerContent(
    navController : NavController,
    adminName     : String,
    adminInitial  : String,
    adminPhotoUrl : String?,
    unreadCount   : Int = 0,
    onClose       : () -> Unit,
    onLogoutClick : () -> Unit
) {
    data class DrawerItem(
        val icon  : ImageVector,
        val label : String,
        val route : String,
        val badge : Int = 0
    )

    val drawerItems = listOf(
        DrawerItem(Icons.Default.Dashboard,      "Dashboard",         Screen.AdminDashboard.route),
        DrawerItem(Icons.Default.CheckCircle,    "Verify Properties", Screen.VerifyProperties.route),
        DrawerItem(Icons.Default.VerifiedUser,   "Verify Users",      Screen.VerifyUsers.route),
        DrawerItem(Icons.Default.ManageAccounts, "Manage Users",      Screen.ManageUsers.route),
        DrawerItem(Icons.Default.HomeWork,       "Manage Properties", Screen.ManageProperties.route),
        DrawerItem(Icons.Default.CalendarMonth,  "Manage Bookings",   Screen.ManageBookings.route),
        DrawerItem(Icons.Default.BarChart,       "Analytics",         Screen.Reports.route),
        DrawerItem(Icons.Default.Payment,        "Payment Reports",   Screen.PaymentReports.route),
        DrawerItem(Icons.Default.Notifications,  "Notifications",     Screen.Notifications.route, badge = unreadCount),
        DrawerItem(Icons.Default.Settings,       "Settings",          Screen.Settings.route),
    )

    val currentRoute = navController.currentBackStackEntry?.destination?.route

    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier             = Modifier.width(290.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryNavyDark, PrimaryNavy, PrimaryNavyLight),
                        start = Offset(0f, 0f),
                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(20.dp)
        ) {
            // Decorative gold circles
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 25.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(0.15f))
            )
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-10).dp, y = 15.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(0.10f))
            )

            Column {
                // ✅ Gold bordered avatar
                Box(
                    modifier         = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, GoldAccent, CircleShape)
                        .background(PrimaryNavyDark),
                    contentAlignment = Alignment.Center
                ) {
                    if (!adminPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model              = adminPhotoUrl,
                            contentDescription = "Admin Photo",
                            modifier           = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Text(adminInitial, color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(adminName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(SuccessGreen))
                    Text("Super Admin  •  Online", color = Color.White.copy(0.80f), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            drawerItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationDrawerItem(
                    icon = {
                        BadgedBox(badge = {
                            if (item.badge > 0) {
                                Badge(containerColor = GoldAccent) {
                                    Text(
                                        if (item.badge > 99) "99+" else "${item.badge}",
                                        fontSize = 8.sp,
                                        color    = PrimaryNavy
                                    )
                                }
                            }
                        }) {
                            Icon(
                                item.icon, null,
                                tint     = if (isSelected) GoldAccent else Color(0xFF555577),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            item.label,
                            fontSize   = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color      = if (isSelected) PrimaryNavy else Color(0xFF333344)
                        )
                    },
                    selected = isSelected,
                    onClick  = {
                        navController.navigate(item.route) {
                            popUpTo(Screen.AdminDashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                        onClose()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                    colors   = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor   = PrimaryNavy.copy(0.08f),
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
        }

        // ── Logout ────────────────────────────────────────────────────────────
        HorizontalDivider(color = GoldAccent.copy(0.2f), modifier = Modifier.padding(horizontal = 12.dp))
        NavigationDrawerItem(
            icon = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ErrorRed.copy(0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            },
            label = {
                Text("Logout", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            },
            selected = false,
            onClick  = onLogoutClick,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            colors   = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = ErrorRed.copy(0.05f)
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// HERO BANNER
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun HeroBanner(adminName: String = "Admin") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(PrimaryNavyDark, PrimaryNavy, PrimaryNavyLight))
            )
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(120.dp).align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(GoldAccent.copy(0.08f))
        )
        Box(
            modifier = Modifier
                .size(75.dp).align(Alignment.BottomEnd)
                .offset(x = 18.dp, y = 28.dp)
                .clip(CircleShape)
                .background(GoldAccent.copy(0.12f))
        )

        Column {
            Text(
                "Welcome back, $adminName 👋",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Here's what's happening today.",
                fontSize = 13.sp,
                color    = Color.White.copy(0.75f)
            )
            Spacer(Modifier.height(14.dp))
            // ✅ Gold accent pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, GoldAccent.copy(0.5f), RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.08f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AdminPanelSettings, null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                Text("Super Admin  •  Platform Overview", fontSize = 11.sp, color = Color.White.copy(0.9f))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// STAT CARD  — ✅ gold outline
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ModernStatCard(
    icon    : ImageVector,
    label   : String,
    value   : String,
    change  : String,
    positive: Boolean,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        border    = BorderStroke(1.dp, GoldAccent.copy(0.35f))   // ✅ gold outline
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier         = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (positive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    null,
                    tint     = if (positive) SuccessGreen else ErrorRed,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "$change vs last week",
                    fontSize = 10.sp,
                    color    = if (positive) SuccessGreen else ErrorRed
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PENDING HIGHLIGHT CARD — ✅ gold outline
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PendingHighlightCard(pendingCount: Int, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (pendingCount > 0) Color(0xFFFFF8F0) else Color(0xFFF0FBF4)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(
            1.5.dp,
            if (pendingCount > 0) GoldAccent.copy(0.6f) else SuccessGreen.copy(0.4f)
        )                                                              // ✅ gold outline
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (pendingCount > 0) GoldAccent.copy(0.15f) else SuccessGreen.copy(0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (pendingCount > 0) Icons.Default.PendingActions else Icons.Default.CheckCircle,
                        null,
                        tint     = if (pendingCount > 0) GoldAccentDark else SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        "$pendingCount Pending Bookings",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (pendingCount > 0) GoldAccentDark else SuccessGreen
                    )
                    Text(
                        if (pendingCount > 0) "Tap to review & take action" else "All bookings handled",
                        fontSize = 11.sp,
                        color    = TextSecondary
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = GoldAccent, modifier = Modifier.size(20.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// QUICK ACTION CARD — ✅ gold outline
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun QuickActionCard(
    icon    : ImageVector,
    label   : String,
    modifier: Modifier = Modifier,
    onClick : () -> Unit
) {
    Card(
        modifier  = modifier.clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = BorderStroke(1.dp, GoldAccent.copy(0.30f))    // ✅ gold outline
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(PrimaryNavy, PrimaryNavyLight))),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(
                label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CHARTS — gold outlines added
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun UsersOverviewChart(points: List<UserChartPoint>, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(
        targetValue   = if (points.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label         = "userChart"
    )
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = BorderStroke(1.dp, GoldAccent.copy(0.30f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("${points.sumOf { it.newUsers }} new", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("users this week", fontSize = 11.sp, color = TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(PrimaryNavyLight, "New")
                    LegendDot(GoldAccent,       "Active")
                }
            }
            Spacer(Modifier.height(14.dp))
            if (points.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
                    Text("No data yet", fontSize = 13.sp, color = Color(0xFFCCCCCC))
                }
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    drawUsersLineChart(points, animProgress)
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    points.forEach { pt ->
                        Text(pt.label, fontSize = 10.sp, color = Color(0xFFAAAAAA), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawUsersLineChart(points: List<UserChartPoint>, progress: Float) {
    if (points.isEmpty()) return
    val maxVal = maxOf(points.maxOf { it.newUsers }, points.maxOf { it.activeUsers }, 1)
    val w = size.width; val h = size.height
    val stepX = w / (points.size - 1).coerceAtLeast(1).toFloat()
    val padTop = 8.dp.toPx(); val padBot = 4.dp.toPx()
    val drawH = h - padTop - padBot

    for (i in 0..3) drawLine(Color(0xFFEEEEEE), Offset(0f, padTop + drawH * i / 3f), Offset(w, padTop + drawH * i / 3f), 1.dp.toPx())

    fun xOf(i: Int) = i * stepX
    fun yOf(v: Int): Float = padTop + drawH * (1f - v.toFloat() / maxVal)

    val newPath = Path(); val newFill = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i); val y = yOf(pt.newUsers)
        if (i == 0) { newPath.moveTo(x, y); newFill.moveTo(x, h); newFill.lineTo(x, y) }
        else {
            val px = xOf(i - 1); val py = yOf(points[i - 1].newUsers); val cx = px + (x - px) / 2f
            newPath.cubicTo(cx, py, cx, y, x, y); newFill.cubicTo(cx, py, cx, y, x, y)
        }
    }
    newFill.lineTo(xOf(points.lastIndex), h); newFill.close()

    clipRect(right = w * progress) {
        drawPath(newFill, Brush.verticalGradient(listOf(PrimaryNavyLight.copy(alpha = 0.20f), Color.Transparent)))
        drawPath(newPath, color = PrimaryNavyLight, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    val activePath = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i); val y = yOf(pt.activeUsers)
        if (i == 0) activePath.moveTo(x, y)
        else {
            val px = xOf(i - 1); val py = yOf(points[i - 1].activeUsers); val cx = px + (x - px) / 2f
            activePath.cubicTo(cx, py, cx, y, x, y)
        }
    }
    clipRect(right = w * progress) {
        drawPath(activePath, color = Color(0xFFC9A84C), style = Stroke(
            2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        ))
    }

    val lastIdx = ((points.size - 1) * progress).toInt().coerceIn(0, points.lastIndex)
    drawCircle(PrimaryNavyLight, 5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].newUsers)))
    drawCircle(Color.White, 2.5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].newUsers)))
    drawCircle(Color(0xFFC9A84C), 5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].activeUsers)))
    drawCircle(Color.White, 2.5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].activeUsers)))
}

@Composable
fun RevenueOverviewChart(points: List<RevenueChartPoint>, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(
        targetValue   = if (points.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label         = "revenueChart"
    )
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = BorderStroke(1.dp, GoldAccent.copy(0.30f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("PKR ${"%.0f".format(points.sumOf { it.revenue })}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("this week", fontSize = 11.sp, color = TextSecondary)
                }
                LegendDot(GoldAccent, "Revenue")
            }
            Spacer(Modifier.height(14.dp))
            if (points.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
                    Text("No revenue data yet", fontSize = 13.sp, color = Color(0xFFCCCCCC))
                }
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    drawRevenueLineChart(points, animProgress)
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    points.forEach { pt ->
                        Text(pt.label, fontSize = 10.sp, color = Color(0xFFAAAAAA), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawRevenueLineChart(points: List<RevenueChartPoint>, progress: Float) {
    if (points.isEmpty()) return
    val maxVal = points.maxOf { it.revenue }.coerceAtLeast(1.0)
    val w = size.width; val h = size.height
    val stepX = w / (points.size - 1).coerceAtLeast(1).toFloat()
    val padTop = 8.dp.toPx(); val padBot = 4.dp.toPx()
    val drawH = h - padTop - padBot

    for (i in 0..3) drawLine(Color(0xFFEEEEEE), Offset(0f, padTop + drawH * i / 3f), Offset(w, padTop + drawH * i / 3f), 1.dp.toPx())

    fun xOf(i: Int) = i * stepX
    fun yOf(v: Double) = (padTop + drawH * (1.0 - v / maxVal)).toFloat()

    val linePath = Path(); val fillPath = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i); val y = yOf(pt.revenue)
        if (i == 0) { linePath.moveTo(x, y); fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
        else {
            val px = xOf(i - 1); val py = yOf(points[i - 1].revenue); val cx = px + (x - px) / 2f
            linePath.cubicTo(cx, py, cx, y, x, y); fillPath.cubicTo(cx, py, cx, y, x, y)
        }
    }
    fillPath.lineTo(xOf(points.lastIndex), h); fillPath.close()

    clipRect(right = w * progress) {
        drawPath(fillPath, Brush.verticalGradient(listOf(Color(0xFFC9A84C).copy(alpha = 0.25f), Color.Transparent)))
        drawPath(linePath, color = Color(0xFFC9A84C), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    val lastIdx = ((points.size - 1) * progress).toInt().coerceIn(0, points.lastIndex)
    drawCircle(Color(0xFFC9A84C), 5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].revenue)))
    drawCircle(Color.White, 2.5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].revenue)))
}

@Composable
fun PropertyStatusChart(slices: List<PropertyStatusSlice>, total: Int, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(
        targetValue   = if (slices.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label         = "donutChart"
    )
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = BorderStroke(1.dp, GoldAccent.copy(0.30f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) { drawDonutChart(slices, animProgress) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$total", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Total", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                Column(
                    modifier            = Modifier.weight(1f).padding(start = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    slices.forEach { slice ->
                        val sliceColor = Color(slice.colorHex)
                        val pct = if (total > 0) (slice.count * 100f / total) else 0f
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(sliceColor))
                            Column {
                                Text(slice.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("${slice.count} (${"%.1f".format(pct)}%)", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawDonutChart(slices: List<PropertyStatusSlice>, progress: Float) {
    if (slices.isEmpty()) return
    val total   = slices.sumOf { it.count }.coerceAtLeast(1)
    val stroke  = 24.dp.toPx()
    val padding = stroke / 2f + 4.dp.toPx()
    val diameter = size.minDimension - padding * 2
    var startAngle = -90f
    slices.forEach { slice ->
        val sweep = (slice.count.toFloat() / total) * 360f * progress - 3f
        drawArc(
            color      = Color(slice.colorHex),
            startAngle = startAngle,
            sweepAngle = sweep.coerceAtLeast(0f),
            useCenter  = false,
            topLeft    = Offset(padding, padding),
            size       = Size(diameter, diameter),
            style      = Stroke(stroke, cap = StrokeCap.Round)
        )
        startAngle += (slice.count.toFloat() / total) * 360f
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ROW COMPOSABLES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun RealActivityRow(notification: Notification) {
    val icon  = notifIcon(notification.type)
    val color = notifColor(notification.type)
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(38.dp).clip(CircleShape).background(color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notification.title.ifBlank { notification.body },
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (notification.body.isNotBlank() && notification.title.isNotBlank()) {
                Text(notification.body, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(timeAgo(notification.createdAt), fontSize = 11.sp, color = Color(0xFFAAAAAA))
    }
}

@Composable
fun RealBookingRow(booking: Booking) {
    val statusColor = bookingStatusColor(booking.status)
    val statusLabel = bookingStatusLabel(booking.status)
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1.3f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(28.dp).clip(CircleShape).background(PrimaryNavy.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    booking.tenantName.firstOrNull()?.toString() ?: "?",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = PrimaryNavy
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                booking.tenantName.ifBlank { "Guest" },
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
        Text(
            booking.propertyTitle.ifBlank { "Property" },
            modifier = Modifier.weight(1.7f),
            fontSize = 12.sp,
            color    = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "PKR ${"%.0f".format(booking.totalAmount)}",
            modifier   = Modifier.weight(1.3f),
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = GoldAccentDark
        )
        Box(
            modifier         = Modifier
                .weight(1.2f)
                .clip(RoundedCornerShape(20.dp))
                .background(statusColor.copy(0.12f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(statusLabel, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SMALL HELPERS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
fun DashboardEmptyBox(icon: ImageVector, text: String) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = GoldAccent.copy(0.4f), modifier = Modifier.size(40.dp))
        Text(text, color = Color(0xFFAAAAAA), fontSize = 13.sp)
    }
}

@Composable
fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
}

// ✅ Premium section title — gold accent bar
@Composable
fun PremiumSectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GoldAccent)
        )
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

















