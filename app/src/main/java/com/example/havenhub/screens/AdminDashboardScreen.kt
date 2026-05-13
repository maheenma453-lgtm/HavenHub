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

// ── Brand Colors (Light Theme) ────────────────────────────────────────────────
private val NavyBlue   = Color(0xFF1B2A4A)
private val NavyMid    = Color(0xFF243658)
private val NavyLight  = Color(0xFF2E4270)
private val Gold       = Color(0xFFC9A227)
private val GoldDark   = Color(0xFFA07D10)
private val PageBg     = Color(0xFFF4F6FA)
private val GreenOk    = Color(0xFF27AE60)
private val RedErr     = Color(0xFFE74C3C)
private val OrangeWarn = Color(0xFFE67E22)

// ── Dark Theme Color Tokens ───────────────────────────────────────────────────
private val D_PageBg    = Color(0xFF060D1A)    // darkest navy background
private val D_CardBg    = Color(0xFF112038)    // card surface
private val D_NavyBlue  = Color(0xFF0D1B3E)    // header/drawer bg
private val D_NavyMid   = Color(0xFF122040)    // secondary surfaces
private val D_Gold      = Color(0xFFD4AF37)    // primary gold accent
private val D_GoldDark  = Color(0xFFB8962E)    // darker gold
private val D_TextPri   = Color(0xFFF0F4FF)    // primary text
private val D_TextSec   = Color(0xFF8899BB)    // secondary/muted text
private val D_Border    = Color(0xFF1E2E50)    // subtle borders
private val D_GreenOk   = Color(0xFF3DCC7A)    // success green
private val D_RedErr    = Color(0xFFE74C3C)    // error red (same)
private val D_Orange    = Color(0xFFFFB347)    // warning orange
private val D_DividerC  = Color(0xFF1A2B45)    // divider color

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
    NotificationType.BOOKING_REQUESTED.name  -> Color(0xFF3498DB)
    NotificationType.BOOKING_CONFIRMED.name  -> GreenOk
    NotificationType.BOOKING_CANCELLED.name  -> RedErr
    NotificationType.BOOKING_COMPLETED.name  -> Color(0xFF00897B)
    NotificationType.PAYMENT_RECEIVED.name   -> Gold
    NotificationType.PAYMENT_FAILED.name     -> RedErr
    NotificationType.REFUND_ISSUED.name      -> OrangeWarn
    NotificationType.NEW_REVIEW.name,
    NotificationType.REVIEW_REPLY.name       -> OrangeWarn
    NotificationType.NEW_MESSAGE.name        -> Color(0xFF3498DB)
    NotificationType.PROPERTY_APPROVED.name  -> GreenOk
    NotificationType.PROPERTY_REJECTED.name  -> RedErr
    NotificationType.ACCOUNT_VERIFIED.name   -> GreenOk
    NotificationType.ACCOUNT_SUSPENDED.name  -> RedErr
    else                                     -> Color(0xFF9B59B6)
}

private fun timeAgo(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val diffMs = System.currentTimeMillis() - timestamp.toDate().time
    return when {
        diffMs < 60_000     -> "Now"
        diffMs < 3_600_000  -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)}m"
        diffMs < 86_400_000 -> "${TimeUnit.MILLISECONDS.toHours(diffMs)}h"
        else                -> "${TimeUnit.MILLISECONDS.toDays(diffMs)}d"
    }
}

private fun bookingStatusColor(status: String): Color = when (status) {
    BookingStatus.CONFIRMED.name  -> GreenOk
    BookingStatus.CANCELLED.name  -> RedErr
    BookingStatus.COMPLETED.name  -> NavyMid
    BookingStatus.CHECKED_IN.name -> Color(0xFF00897B)
    else                          -> OrangeWarn
}

private fun bookingStatusLabel(status: String): String =
    status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

// ─── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController : NavController,
    viewModel     : DashboardViewModel = hiltViewModel(),
    authViewModel : AuthViewModel      = hiltViewModel()
) {
    // ── Dark theme detection ──────────────────────────────────────────────────
    val isDark = isSystemInDarkTheme()

    // ── Theme-aware color aliases ─────────────────────────────────────────────
    val pageBg    = if (isDark) D_PageBg   else PageBg
    val cardBg    = if (isDark) D_CardBg   else Color.White
    val headerBg1 = if (isDark) D_NavyBlue else NavyBlue
    val headerBg2 = if (isDark) D_NavyMid  else NavyMid
    val goldC     = if (isDark) D_Gold     else Gold
    val goldDkC   = if (isDark) D_GoldDark else GoldDark
    val textPri   = if (isDark) D_TextPri  else NavyBlue
    val textSec   = if (isDark) D_TextSec  else NavyBlue.copy(alpha = 0.55f)
    val dividerC  = if (isDark) D_DividerC else NavyBlue.copy(alpha = 0.06f)
    val greenC    = if (isDark) D_GreenOk  else GreenOk
    val redC      = if (isDark) D_RedErr   else RedErr

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
            containerColor   = cardBg,                          // dark: dark card, light: white
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(redC.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = redC, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Logout?",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = textPri
                )
            },
            text = {
                Text(
                    "Are you sure you want to logout from HavenHub Admin?",
                    color     = textSec,
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.signOut()
                        navController.navigate(Screen.SignIn.route) { popUpTo(0) { inclusive = true } }
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = redC),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Yes, Logout", color = Color.White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showLogoutDialog = false },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.5.dp, goldC)
                ) { Text("Cancel", color = textPri, fontWeight = FontWeight.Medium) }
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
                },
                isDark        = isDark                          // pass dark flag to drawer
            )
        }
    ) {
        Scaffold(
            containerColor = pageBg,                            // dark: deep navy, light: off-white
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(headerBg1, headerBg2)))
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null, tint = Color.White)
                        }
                        Text(
                            "HavenHub",
                            color      = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 20.sp,
                            modifier   = Modifier.weight(1f)
                        )
                        // Notification bell with badge
                        BadgedBox(badge = {
                            if (uiState.unreadNotifCount > 0) {
                                Badge(containerColor = goldC) {
                                    Text(
                                        if (uiState.unreadNotifCount > 99) "99+" else "${uiState.unreadNotifCount}",
                                        fontSize   = 9.sp,
                                        color      = if (isDark) D_NavyBlue else NavyBlue,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }) {
                            IconButton(onClick = {
                                navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                            }) {
                                Icon(Icons.Default.Notifications, null, tint = Color.White)
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        // Avatar circle
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(2.dp, goldC, CircleShape)
                                .background(headerBg1)
                                .clickable {
                                    navController.navigate(Screen.Profile.route) { launchSingleTop = true }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!adminPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model              = adminPhotoUrl,
                                    contentDescription = null,
                                    modifier           = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale       = ContentScale.Crop
                                )
                            } else {
                                Text(adminInitial, color = goldC, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                        }
                    }
                    // Gold shimmer accent line at bottom of top bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, goldC, Color.Transparent)))
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        ) { padding ->

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = goldC, strokeWidth = 3.dp)
                }
                return@Scaffold
            }

            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {

                // ── Hero Banner ───────────────────────────────────────────────
                item { HeroBanner(adminName = adminName, isDark = isDark) }

                // ── Stats Grid ────────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(22.dp))
                    DashSectionHeader(
                        "Platform Overview",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        isDark   = isDark
                    )
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
                                gradient = listOf(
                                    if (isDark) D_NavyBlue else NavyBlue,
                                    if (isDark) Color(0xFF1E3A7A) else NavyLight
                                ),
                                isDark   = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatCard(
                                icon     = Icons.Default.People,
                                label    = "Total Users",
                                value    = "${stats.totalUsers}",
                                change   = "+12.5%",
                                positive = true,
                                gradient = listOf(Color(0xFF00695C), Color(0xFF26C6DA)),
                                isDark   = isDark,
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
                                isDark   = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatCard(
                                icon     = Icons.Default.AccountBalanceWallet,
                                label    = "Revenue",
                                value    = "PKR ${"%.0f".format(stats.totalEarnings)}",
                                change   = "+18.6%",
                                positive = true,
                                gradient = listOf(
                                    if (isDark) D_GoldDark else GoldDark,
                                    if (isDark) D_Gold     else Gold
                                ),
                                isDark   = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        PendingHighlightCard(
                            pendingCount = stats.pendingBookings,
                            isDark       = isDark
                        ) {
                            navController.navigate(Screen.ManageBookings.route)
                        }
                    }
                }

                // ── Quick Actions ─────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    DashSectionHeader(
                        "Quick Actions",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        isDark   = isDark
                    )
                    Spacer(Modifier.height(12.dp))
                    val actions = listOf(
                        Triple(Icons.Default.CheckCircle,    "Verify Properties", Screen.VerifyProperties.route),
                        Triple(Icons.Default.VerifiedUser,   "Verify Users",      Screen.VerifyUsers.route),
                        Triple(Icons.Default.ManageAccounts, "Manage Users",      Screen.ManageUsers.route),
                        Triple(Icons.Default.HomeWork,       "Manage Properties", Screen.ManageProperties.route),
                        Triple(Icons.Default.CalendarMonth,  "Manage Bookings",   Screen.ManageBookings.route),
                        Triple(Icons.Default.BarChart,       "View Reports",      Screen.Reports.route),
                        Triple(Icons.Default.Payment,        "Payments",          Screen.PaymentReports.route),
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
                                        isDark   = isDark,
                                        modifier = Modifier.weight(1f),
                                        onClick  = { navController.navigate(route) }
                                    )
                                }
                                if (rowActions.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ── Charts ────────────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    DashSectionHeader(
                        "User Overview",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        isDark   = isDark
                    )
                    Spacer(Modifier.height(12.dp))
                    UsersOverviewChart(
                        points   = uiState.userChartPoints,
                        isDark   = isDark,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    DashSectionHeader(
                        "Revenue Overview",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        isDark   = isDark
                    )
                    Spacer(Modifier.height(12.dp))
                    RevenueOverviewChart(
                        points   = uiState.revenueChartPoints,
                        isDark   = isDark,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    DashSectionHeader(
                        "Property Status",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        isDark   = isDark
                    )
                    Spacer(Modifier.height(12.dp))
                    PropertyStatusChart(
                        slices   = uiState.propertyStatusSlices,
                        total    = stats.totalProperties,
                        isDark   = isDark,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }

                // ── Recent Activity ───────────────────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        DashSectionHeader("Recent Activity", isDark = isDark)
                        TextButton(onClick = {
                            navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                        }) {
                            Text("View All", color = goldC, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .shadow(
                                4.dp,
                                RoundedCornerShape(18.dp),
                                ambientColor = if (isDark) Color.Black.copy(0.4f) else NavyBlue.copy(0.08f)
                            )
                    ) {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(18.dp),
                            colors    = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            // Top accent gradient bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                if (isDark) D_NavyBlue else NavyBlue,
                                                goldC
                                            )
                                        )
                                    )
                            )
                            if (uiState.recentActivities.isEmpty()) {
                                DashboardEmptyBox(
                                    Icons.Default.Notifications,
                                    "No recent activity",
                                    isDark = isDark
                                )
                            } else {
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    uiState.recentActivities.forEachIndexed { idx, notif ->
                                        PremiumActivityRow(notif, isDark = isDark)
                                        if (idx < uiState.recentActivities.lastIndex) {
                                            HorizontalDivider(
                                                modifier  = Modifier.padding(horizontal = 16.dp),
                                                color     = dividerC,
                                                thickness = 0.7.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Recent Bookings ───────────────────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        DashSectionHeader("Recent Bookings", isDark = isDark)
                        TextButton(onClick = { navController.navigate(Screen.ManageBookings.route) }) {
                            Text("View All", color = goldC, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .shadow(
                                4.dp,
                                RoundedCornerShape(18.dp),
                                ambientColor = if (isDark) Color.Black.copy(0.4f) else NavyBlue.copy(0.08f)
                            )
                    ) {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(18.dp),
                            colors    = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            // Top accent gradient bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                if (isDark) D_NavyBlue else NavyBlue,
                                                goldC
                                            )
                                        )
                                    )
                            )
                            if (uiState.recentBookings.isEmpty()) {
                                DashboardEmptyBox(
                                    Icons.Default.CalendarMonth,
                                    "No bookings yet",
                                    isDark = isDark
                                )
                            } else {
                                Column {
                                    // Table header row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isDark) D_NavyBlue.copy(alpha = 0.6f)
                                                else NavyBlue.copy(alpha = 0.04f)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text("Guest",    modifier = Modifier.weight(1f),    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) D_TextSec else NavyBlue.copy(0.6f))
                                        Text("Property", modifier = Modifier.weight(1.2f),  fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) D_TextSec else NavyBlue.copy(0.6f))
                                        Text("Amount",   modifier = Modifier.weight(0.9f),  fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) D_TextSec else NavyBlue.copy(0.6f))
                                        Text("Status",   modifier = Modifier.weight(0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) D_TextSec else NavyBlue.copy(0.6f), textAlign = TextAlign.Center)
                                    }
                                    uiState.recentBookings.forEachIndexed { idx, booking ->
                                        PremiumBookingRow(booking, isDark = isDark)
                                        if (idx < uiState.recentBookings.lastIndex) {
                                            HorizontalDivider(
                                                modifier  = Modifier.padding(horizontal = 14.dp),
                                                color     = dividerC,
                                                thickness = 0.7.dp
                                            )
                                        }
                                    }
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
// PREMIUM ACTIVITY ROW — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun PremiumActivityRow(notification: Notification, isDark: Boolean = false) {
    val icon     = notifIcon(notification.type)
    val color    = notifColor(notification.type)
    val circleBg = if (isDark) D_NavyBlue else NavyBlue
    val textPri  = if (isDark) D_TextPri  else NavyBlue
    val textSec  = if (isDark) D_TextSec  else NavyBlue.copy(alpha = 0.45f)
    val badgeBg  = if (isDark) D_NavyMid  else NavyBlue.copy(alpha = 0.06f)
    val badgeTxt = if (isDark) D_TextSec  else NavyBlue.copy(alpha = 0.55f)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon circle — navy background with colored icon
        Box(
            modifier         = Modifier.size(40.dp).clip(CircleShape).background(circleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notification.title.ifBlank { notification.body },
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textPri,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (notification.body.isNotBlank() && notification.title.isNotBlank()) {
                Text(
                    notification.body,
                    fontSize = 11.sp,
                    color    = textSec,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Time badge
        Surface(color = badgeBg, shape = RoundedCornerShape(20.dp)) {
            Text(
                timeAgo(notification.createdAt),
                fontSize   = 10.sp,
                color      = badgeTxt,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM BOOKING ROW — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun PremiumBookingRow(booking: Booking, isDark: Boolean = false) {
    val statusColor = bookingStatusColor(booking.status)
    val statusLabel = bookingStatusLabel(booking.status)
    val goldC       = if (isDark) D_Gold    else Gold
    val goldDkC     = if (isDark) D_GoldDark else GoldDark
    val textPri     = if (isDark) D_TextPri else NavyBlue
    val textSec     = if (isDark) D_TextSec else NavyBlue.copy(alpha = 0.55f)
    val avatarBg    = if (isDark) D_NavyBlue else NavyBlue

    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Guest column with avatar
        Row(
            modifier          = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(28.dp).clip(CircleShape).background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    booking.tenantName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = goldC
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(
                booking.tenantName.ifBlank { "Guest" }.split(" ").first(),
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textPri,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        // Property name column
        Text(
            booking.propertyTitle.ifBlank { "Property" },
            modifier = Modifier.weight(1.2f),
            fontSize = 12.sp,
            color    = textSec,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Amount column
        Text(
            "PKR ${"%.0f".format(booking.totalAmount)}",
            modifier   = Modifier.weight(0.9f),
            fontSize   = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = goldDkC,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )

        // Status badge column
        Surface(
            color    = statusColor.copy(alpha = 0.10f),
            shape    = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(0.85f)
                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Text(
                statusLabel,
                fontSize   = 10.sp,
                color      = statusColor,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// HERO BANNER — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun HeroBanner(adminName: String = "Admin", isDark: Boolean = false) {
    val bg1  = if (isDark) D_NavyBlue else NavyBlue
    val bg2  = if (isDark) D_NavyMid  else NavyMid
    val goldC = if (isDark) D_Gold    else Gold

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(bg1, bg2)))
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        // Decorative circles — gold tinted
        Box(
            modifier = Modifier.size(100.dp).align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-20).dp).clip(CircleShape)
                .background(goldC.copy(alpha = if (isDark) 0.12f else 0.08f))
        )
        Box(
            modifier = Modifier.size(60.dp).align(Alignment.BottomEnd)
                .offset(x = 15.dp, y = 20.dp).clip(CircleShape)
                .background(goldC.copy(alpha = if (isDark) 0.18f else 0.12f))
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
                color    = Color.White.copy(alpha = 0.70f)
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, goldC.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = if (isDark) 0.06f else 0.08f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AdminPanelSettings, null, tint = goldC, modifier = Modifier.size(14.dp))
                Text("Super Admin  •  Platform Overview", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// STAT CARD — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ModernStatCard(
    icon    : ImageVector,
    label   : String,
    value   : String,
    change  : String,
    positive: Boolean,
    gradient: List<Color>,
    isDark  : Boolean    = false,
    modifier: Modifier   = Modifier
) {
    val cardBg  = if (isDark) D_CardBg  else Color.White
    val textPri = if (isDark) D_TextPri else NavyBlue
    val textSec = if (isDark) D_TextSec else NavyBlue.copy(alpha = 0.5f)
    val greenC  = if (isDark) D_GreenOk else GreenOk
    val redC    = if (isDark) D_RedErr  else RedErr

    Box(
        modifier = modifier.shadow(
            elevation    = 4.dp,
            shape        = RoundedCornerShape(18.dp),
            ambientColor = if (isDark) Color.Black.copy(0.4f) else NavyBlue.copy(0.08f),
            spotColor    = if (isDark) Color.Black.copy(0.5f) else NavyBlue.copy(0.10f)
        )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Top gradient accent bar
            Box(
                modifier = Modifier.fillMaxWidth().height(3.dp)
                    .background(Brush.horizontalGradient(gradient))
            )
            Column(modifier = Modifier.padding(14.dp)) {
                // Icon circle with gradient background
                Box(
                    modifier         = Modifier.size(42.dp).clip(CircleShape)
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    value,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = textPri,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 11.sp, color = textSec, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (positive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        null,
                        tint     = if (positive) greenC else redC,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "$change this week",
                        fontSize = 10.sp,
                        color    = if (positive) greenC else redC
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PENDING HIGHLIGHT CARD — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun PendingHighlightCard(pendingCount: Int, isDark: Boolean = false, onClick: () -> Unit) {
    val goldC      = if (isDark) D_Gold    else Gold
    val goldDkC    = if (isDark) D_GoldDark else GoldDark
    val greenC     = if (isDark) D_GreenOk else GreenOk
    val textPri    = if (isDark) D_TextPri else NavyBlue
    val textSec    = if (isDark) D_TextSec else NavyBlue.copy(alpha = 0.45f)
    val navyBg     = if (isDark) D_NavyBlue else NavyBlue
    // Card backgrounds differ by state
    val cardBgPend = if (isDark) Color(0xFF1A1608) else Color(0xFFFFFBF0) // pending: dark amber tint
    val cardBgOk   = if (isDark) Color(0xFF0A1A14) else Color(0xFFF0FBF4) // ok: dark green tint

    Box(
        modifier = Modifier.fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp),
                ambientColor = if (isDark) Color.Black.copy(0.4f) else NavyBlue.copy(0.08f))
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().clickable { onClick() },
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(
                containerColor = if (pendingCount > 0) cardBgPend else cardBgOk
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Top accent bar
            Box(
                modifier = Modifier.fillMaxWidth().height(3.dp).background(
                    if (pendingCount > 0)
                        Brush.horizontalGradient(listOf(navyBg, goldC))
                    else
                        Brush.horizontalGradient(listOf(navyBg, greenC))
                )
            )
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
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(
                            if (pendingCount > 0) navyBg else greenC.copy(alpha = 0.15f)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (pendingCount > 0) Icons.Default.PendingActions else Icons.Default.CheckCircle,
                            null,
                            tint     = if (pendingCount > 0) goldC else greenC,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            "$pendingCount Pending Bookings",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (pendingCount > 0) goldDkC else greenC
                        )
                        Text(
                            if (pendingCount > 0) "Tap to review & take action" else "All bookings handled",
                            fontSize = 11.sp,
                            color    = textSec
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(goldC.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronRight, null, tint = goldC, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// QUICK ACTION CARD — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun QuickActionCard(
    icon    : ImageVector,
    label   : String,
    isDark  : Boolean    = false,
    modifier: Modifier   = Modifier,
    onClick : () -> Unit
) {
    val cardBg  = if (isDark) D_CardBg  else Color.White
    val iconBg  = if (isDark) D_NavyBlue else NavyBlue
    val goldC   = if (isDark) D_Gold    else Gold
    val textPri = if (isDark) D_TextPri else NavyBlue

    Box(
        modifier = modifier.shadow(
            elevation    = 3.dp,
            shape        = RoundedCornerShape(14.dp),
            ambientColor = if (isDark) Color.Black.copy(0.4f) else NavyBlue.copy(0.06f)
        )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().clickable { onClick() },
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(38.dp).clip(CircleShape).background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = goldC, modifier = Modifier.size(18.dp))
                }
                Text(
                    label,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = textPri,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// DRAWER — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun AdminDrawerContent(
    navController : NavController,
    adminName     : String,
    adminInitial  : String,
    adminPhotoUrl : String?,
    unreadCount   : Int     = 0,
    onClose       : () -> Unit,
    onLogoutClick : () -> Unit,
    isDark        : Boolean = false
) {
    // Theme-aware drawer colors
    val drawerBg    = if (isDark) D_NavyBlue  else Color.White
    val headerBg1   = if (isDark) D_NavyBlue  else NavyBlue
    val headerBg2   = if (isDark) D_NavyMid   else NavyMid
    val goldC       = if (isDark) D_Gold      else Gold
    val textPri     = if (isDark) D_TextPri   else NavyBlue
    val textSec     = if (isDark) D_TextSec   else NavyBlue.copy(alpha = 0.65f)
    val selectedBg  = if (isDark) D_NavyMid   else NavyBlue.copy(alpha = 0.08f)
    val greenC      = if (isDark) D_GreenOk   else GreenOk
    val redC        = if (isDark) D_RedErr    else RedErr

    data class DrawerItem(val icon: ImageVector, val label: String, val route: String, val badge: Int = 0)

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
        drawerContainerColor = drawerBg,            // dark: dark navy, light: white
        modifier             = Modifier.width(290.dp)
    ) {
        // Drawer header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(headerBg1, headerBg2)))
                .padding(20.dp)
        ) {
            // Decorative gold circle
            Box(
                modifier = Modifier.size(70.dp).align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-15).dp).clip(CircleShape)
                    .background(goldC.copy(alpha = 0.15f))
            )
            Column {
                Box(
                    modifier         = Modifier.size(62.dp).clip(CircleShape)
                        .border(2.5.dp, goldC, CircleShape).background(headerBg1),
                    contentAlignment = Alignment.Center
                ) {
                    if (!adminPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model              = adminPhotoUrl,
                            contentDescription = null,
                            modifier           = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Text(adminInitial, color = goldC, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(adminName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(greenC))
                    Text("Super Admin  •  Online", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
            }
        }

        // Gold shimmer line separator
        Box(
            modifier = Modifier.fillMaxWidth().height(2.dp)
                .background(Brush.horizontalGradient(
                    listOf(Color.Transparent, goldC.copy(alpha = 0.6f), Color.Transparent)
                ))
        )

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(6.dp))
            drawerItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationDrawerItem(
                    icon = {
                        BadgedBox(badge = {
                            if (item.badge > 0) {
                                Badge(containerColor = goldC) {
                                    Text(
                                        if (item.badge > 99) "99+" else "${item.badge}",
                                        fontSize   = 8.sp,
                                        color      = if (isDark) D_NavyBlue else NavyBlue,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }) {
                            Icon(
                                item.icon, null,
                                tint     = if (isSelected) goldC else textSec,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            item.label,
                            fontSize   = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color      = if (isSelected) textPri else textSec
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
                        selectedContainerColor   = selectedBg,
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
        }

        HorizontalDivider(
            color    = goldC.copy(alpha = 0.2f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        // Logout item
        NavigationDrawerItem(
            icon = {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(redC.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = redC, modifier = Modifier.size(16.dp))
                }
            },
            label    = { Text("Logout", color = redC, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = false,
            onClick  = onLogoutClick,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            colors   = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = redC.copy(alpha = 0.05f)
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// USERS OVERVIEW CHART — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun UsersOverviewChart(points: List<UserChartPoint>, isDark: Boolean = false, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(
        targetValue   = if (points.isNotEmpty()) 1f else 0f,
        animationSpec = tween(900, easing = EaseOutCubic), label = "userChart"
    )
    val cardBg  = if (isDark) D_CardBg  else Color.White
    val goldC   = if (isDark) D_Gold    else Gold
    val navyC   = if (isDark) D_NavyBlue else NavyBlue
    val textPri = if (isDark) D_TextPri else NavyBlue
    val textSec = if (isDark) D_TextSec else NavyBlue.copy(alpha = 0.5f)

    Box(modifier = modifier.shadow(
        4.dp, RoundedCornerShape(18.dp),
        ambientColor = if (isDark) Color.Black.copy(0.4f) else NavyBlue.copy(0.08f)
    )) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Top accent bar
            Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                .background(Brush.horizontalGradient(listOf(navyC, goldC))))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${points.sumOf { it.newUsers }} new", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = textPri)
                        Text("users this week", fontSize = 11.sp, color = textSec)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendDot(if (isDark) D_NavyBlue else NavyMid, "New",    isDark = isDark)
                        LegendDot(goldC,                                "Active", isDark = isDark)
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (points.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
                        Text("No data yet", fontSize = 13.sp, color = textSec)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        drawUsersLineChart(points, animProgress)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        points.forEach { pt ->
                            Text(pt.label, fontSize = 10.sp, color = textSec, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
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
    val padTop = 8.dp.toPx(); val padBot = 4.dp.toPx(); val drawH = h - padTop - padBot
    for (i in 0..3) drawLine(Color(0xFFEEEEEE), Offset(0f, padTop + drawH * i / 3f), Offset(w, padTop + drawH * i / 3f), 1.dp.toPx())
    fun xOf(i: Int) = i * stepX
    fun yOf(v: Int) = padTop + drawH * (1f - v.toFloat() / maxVal)
    val newPath = Path(); val newFill = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i); val y = yOf(pt.newUsers)
        if (i == 0) { newPath.moveTo(x, y); newFill.moveTo(x, h); newFill.lineTo(x, y) }
        else { val px = xOf(i-1); val py = yOf(points[i-1].newUsers); val cx = px + (x-px)/2f; newPath.cubicTo(cx, py, cx, y, x, y); newFill.cubicTo(cx, py, cx, y, x, y) }
    }
    newFill.lineTo(xOf(points.lastIndex), h); newFill.close()
    clipRect(right = w * progress) {
        drawPath(newFill, Brush.verticalGradient(listOf(NavyMid.copy(0.20f), Color.Transparent)))
        drawPath(newPath, color = NavyMid, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    val activePath = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i); val y = yOf(pt.activeUsers)
        if (i == 0) activePath.moveTo(x, y)
        else { val px = xOf(i-1); val py = yOf(points[i-1].activeUsers); val cx = px + (x-px)/2f; activePath.cubicTo(cx, py, cx, y, x, y) }
    }
    clipRect(right = w * progress) {
        drawPath(activePath, color = Color(0xFFC9A84C), style = Stroke(
            2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        ))
    }
    val lastIdx = ((points.size - 1) * progress).toInt().coerceIn(0, points.lastIndex)
    drawCircle(NavyMid, 5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].newUsers)))
    drawCircle(Color.White, 2.5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].newUsers)))
    drawCircle(Color(0xFFC9A84C), 5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].activeUsers)))
    drawCircle(Color.White, 2.5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].activeUsers)))
}

// ══════════════════════════════════════════════════════════════════════════════
// REVENUE OVERVIEW CHART — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun RevenueOverviewChart(points: List<RevenueChartPoint>, isDark: Boolean = false, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(
        targetValue   = if (points.isNotEmpty()) 1f else 0f,
        animationSpec = tween(900, easing = EaseOutCubic), label = "revenueChart"
    )
    val cardBg  = if (isDark) D_CardBg  else Color.White
    val goldC   = if (isDark) D_Gold    else Gold
    val navyC   = if (isDark) D_NavyBlue else NavyBlue
    val textPri = if (isDark) D_TextPri else NavyBlue
    val textSec = if (isDark) D_TextSec else NavyBlue.copy(alpha = 0.5f)

    Box(modifier = modifier.shadow(
        4.dp, RoundedCornerShape(18.dp),
        ambientColor = if (isDark) Color.Black.copy(0.4f) else NavyBlue.copy(0.08f)
    )) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                .background(Brush.horizontalGradient(listOf(navyC, goldC))))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PKR ${"%.0f".format(points.sumOf { it.revenue })}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = textPri)
                        Text("this week", fontSize = 11.sp, color = textSec)
                    }
                    LegendDot(goldC, "Revenue", isDark = isDark)
                }
                Spacer(Modifier.height(14.dp))
                if (points.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
                        Text("No revenue data yet", fontSize = 13.sp, color = textSec)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        drawRevenueLineChart(points, animProgress)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        points.forEach { pt ->
                            Text(pt.label, fontSize = 10.sp, color = textSec, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
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
    val padTop = 8.dp.toPx(); val padBot = 4.dp.toPx(); val drawH = h - padTop - padBot
    for (i in 0..3) drawLine(Color(0xFFEEEEEE), Offset(0f, padTop + drawH * i / 3f), Offset(w, padTop + drawH * i / 3f), 1.dp.toPx())
    fun xOf(i: Int) = i * stepX
    fun yOf(v: Double) = (padTop + drawH * (1.0 - v / maxVal)).toFloat()
    val linePath = Path(); val fillPath = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i); val y = yOf(pt.revenue)
        if (i == 0) { linePath.moveTo(x, y); fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
        else { val px = xOf(i-1); val py = yOf(points[i-1].revenue); val cx = px + (x-px)/2f; linePath.cubicTo(cx, py, cx, y, x, y); fillPath.cubicTo(cx, py, cx, y, x, y) }
    }
    fillPath.lineTo(xOf(points.lastIndex), h); fillPath.close()
    clipRect(right = w * progress) {
        drawPath(fillPath, Brush.verticalGradient(listOf(Color(0xFFC9A84C).copy(0.25f), Color.Transparent)))
        drawPath(linePath, color = Color(0xFFC9A84C), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    val lastIdx = ((points.size - 1) * progress).toInt().coerceIn(0, points.lastIndex)
    drawCircle(Color(0xFFC9A84C), 5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].revenue)))
    drawCircle(Color.White, 2.5.dp.toPx(), Offset(xOf(lastIdx), yOf(points[lastIdx].revenue)))
}

// ══════════════════════════════════════════════════════════════════════════════
// PROPERTY STATUS CHART — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun PropertyStatusChart(
    slices  : List<PropertyStatusSlice>,
    total   : Int,
    isDark  : Boolean  = false,
    modifier: Modifier = Modifier
) {
    val animProgress by animateFloatAsState(
        targetValue   = if (slices.isNotEmpty()) 1f else 0f,
        animationSpec = tween(1000, easing = EaseOutCubic), label = "donutChart"
    )
    val cardBg  = if (isDark) D_CardBg  else Color.White
    val goldC   = if (isDark) D_Gold    else Gold
    val navyC   = if (isDark) D_NavyBlue else NavyBlue
    val textPri = if (isDark) D_TextPri else NavyBlue
    val textSec = if (isDark) D_TextSec else NavyBlue.copy(alpha = 0.5f)

    Box(modifier = modifier.shadow(
        4.dp, RoundedCornerShape(18.dp),
        ambientColor = if (isDark) Color.Black.copy(0.4f) else NavyBlue.copy(0.08f)
    )) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                .background(Brush.horizontalGradient(listOf(navyC, goldC))))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawDonutChart(slices, animProgress)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$total", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textPri)
                            Text("Total", fontSize = 11.sp, color = textSec)
                        }
                    }
                    Column(
                        modifier            = Modifier.weight(1f).padding(start = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        slices.forEach { slice ->
                            val sliceColor = Color(slice.colorHex)
                            val pct = if (total > 0) (slice.count * 100f / total) else 0f
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(sliceColor))
                                Column {
                                    Text(slice.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPri)
                                    Text("${slice.count} (${"%.1f".format(pct)}%)", fontSize = 11.sp, color = textSec)
                                }
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
    val total = slices.sumOf { it.count }.coerceAtLeast(1)
    val stroke = 24.dp.toPx(); val padding = stroke / 2f + 4.dp.toPx()
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
// SMALL HELPERS — dark theme aware
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LegendDot(color: Color, label: String, isDark: Boolean = false) {
    val textSec = if (isDark) D_TextSec else NavyBlue.copy(alpha = 0.5f)
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = textSec)
    }
}

@Composable
fun DashboardEmptyBox(icon: ImageVector, text: String, isDark: Boolean = false) {
    val goldC   = if (isDark) D_Gold   else Gold
    val textSec = if (isDark) D_TextSec else NavyBlue.copy(alpha = 0.3f)
    Column(
        modifier            = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = goldC.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
        Text(text, color = textSec, fontSize = 13.sp)
    }
}

@Composable
fun DashSectionHeader(text: String, modifier: Modifier = Modifier, isDark: Boolean = false) {
    val goldC   = if (isDark) D_Gold    else Gold
    val goldDkC = if (isDark) D_GoldDark else GoldDark
    val textPri = if (isDark) D_TextPri else NavyBlue

    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gold vertical accent bar
        Box(
            modifier = Modifier.size(width = 4.dp, height = 20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(goldC, goldDkC)))
        )
        Text(
            text,
            fontSize      = 16.sp,
            fontWeight    = FontWeight.Bold,
            color         = textPri,
            letterSpacing = 0.2.sp
        )
    }
}

// Backward-compatible aliases — no logic changed
@Composable
fun PremiumSectionTitle(text: String, modifier: Modifier = Modifier) = DashSectionHeader(text, modifier)

@Composable
fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyBlue.copy(0.6f))
}

@Composable
fun RealActivityRow(notification: Notification) = PremiumActivityRow(notification)

@Composable
fun RealBookingRow(booking: Booking) = PremiumBookingRow(booking)