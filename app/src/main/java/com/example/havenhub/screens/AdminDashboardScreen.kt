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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.Notification
import com.example.havenhub.data.NotificationType
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.DashboardViewModel
import com.example.havenhub.viewmodel.PropertyStatusSlice
import com.example.havenhub.viewmodel.RevenueChartPoint
import com.example.havenhub.viewmodel.UserChartPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

// ─── Activity helpers ──────────────────────────────────────────────────────────

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
    NotificationType.BOOKING_CONFIRMED.name  -> Color(0xFF2ECC71)
    NotificationType.BOOKING_CANCELLED.name  -> Color(0xFFBA1A1A)
    NotificationType.BOOKING_COMPLETED.name  -> Color(0xFF00897B)
    NotificationType.PAYMENT_RECEIVED.name   -> Color(0xFFC9A84C)
    NotificationType.PAYMENT_FAILED.name     -> Color(0xFFBA1A1A)
    NotificationType.REFUND_ISSUED.name      -> Color(0xFFE67E22)
    NotificationType.NEW_REVIEW.name,
    NotificationType.REVIEW_REPLY.name       -> Color(0xFFE67E22)
    NotificationType.NEW_MESSAGE.name        -> Color(0xFF4A90D9)
    NotificationType.PROPERTY_APPROVED.name  -> Color(0xFF2ECC71)
    NotificationType.PROPERTY_REJECTED.name  -> Color(0xFFBA1A1A)
    NotificationType.ACCOUNT_VERIFIED.name   -> Color(0xFF2ECC71)
    NotificationType.ACCOUNT_SUSPENDED.name  -> Color(0xFFBA1A1A)
    else                                     -> Color(0xFF9B59B6)
}

private fun timeAgo(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val diffMs = System.currentTimeMillis() - timestamp.toDate().time
    return when {
        diffMs < 60_000     -> "Just now"
        diffMs < 3_600_000  -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)} min ago"
        diffMs < 86_400_000 -> "${TimeUnit.MILLISECONDS.toHours(diffMs)} hr ago"
        else                -> "${TimeUnit.MILLISECONDS.toDays(diffMs)}d ago"
    }
}

private fun bookingStatusColor(status: String): Color = when (status) {
    BookingStatus.CONFIRMED.name  -> Color(0xFF2ECC71)
    BookingStatus.CANCELLED.name  -> Color(0xFFBA1A1A)
    BookingStatus.COMPLETED.name  -> Color(0xFF4A90D9)
    BookingStatus.CHECKED_IN.name -> Color(0xFF00897B)
    else                          -> Color(0xFFE67E22)
}

private fun bookingStatusLabel(status: String): String =
    status.lowercase().replaceFirstChar { it.uppercase() }

// ─── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController : NavController,
    viewModel     : DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats    = uiState.stats

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            AdminDrawerContent(
                navController = navController,
                onClose       = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AdminTopBar(
                    notifCount          = uiState.unreadNotifCount,
                    onMenuClick         = { scope.launch { drawerState.open() } },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) }
                )
            },
            containerColor = Color(0xFFF4F6FB)
        ) { padding ->

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                return@Scaffold
            }

            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding      = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // ── Hero Banner ───────────────────────────────────
                item { HeroBanner() }

                // ── Stats Cards ───────────────────────────────────
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionTitle(
                        "Platform Overview",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernStatCard(
                                icon     = Icons.Default.Home,
                                label    = "Total Properties",
                                value    = "${stats.totalProperties}",
                                change   = "+8.3%",
                                positive = true,
                                gradient = listOf(Color(0xFF1A3A6B), Color(0xFF4A90D9)),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatCard(
                                icon     = Icons.Default.People,
                                label    = "Total Users",
                                value    = "${stats.totalUsers}",
                                change   = "+12.5%",
                                positive = true,
                                gradient = listOf(Color(0xFF00897B), Color(0xFF26C6DA)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernStatCard(
                                icon     = Icons.Default.CalendarMonth,
                                label    = "Total Bookings",
                                value    = "${stats.totalBookings}",
                                change   = "+15.7%",
                                positive = true,
                                gradient = listOf(Color(0xFF1565C0), Color(0xFF42A5F5)),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatCard(
                                icon     = Icons.Default.AccountBalanceWallet,
                                label    = "Total Revenue",
                                value    = "PKR ${"%.0f".format(stats.totalEarnings)}",
                                change   = "+18.6%",
                                positive = true,
                                gradient = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)),
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
                    SectionTitle(
                        "Quick Actions",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val actions = listOf(
                            Triple(Icons.Default.CheckCircle,    "Verify\nProperties", Screen.VerifyProperties.route),
                            Triple(Icons.Default.VerifiedUser,   "Verify\nUsers",      Screen.VerifyUsers.route),
                            Triple(Icons.Default.ManageAccounts, "Manage\nUsers",      Screen.ManageUsers.route),
                            Triple(Icons.Default.HomeWork,       "Manage\nProps",      Screen.ManageProperties.route),
                            Triple(Icons.Default.CalendarMonth,  "Manage\nBookings",   Screen.ManageBookings.route),
                            Triple(Icons.Default.BarChart,       "View\nReports",      Screen.Reports.route),
                            Triple(Icons.Default.Payment,        "Payment\nReports",   Screen.PaymentReports.route),
                        )
                        items(actions) { (icon, label, route) ->
                            QuickActionChip(icon, label) { navController.navigate(route) }
                        }
                    }
                }

                // ── ✅ NEW: Charts Section ─────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle(
                        "Analytics Overview",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    // Users Overview + Revenue Overview — side by side (scrollable row)
                    // Then Property Status below — full width
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1: Users Overview + Revenue Overview
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UsersOverviewChart(
                                points   = uiState.userChartPoints,
                                modifier = Modifier.weight(1f)
                            )
                            RevenueOverviewChart(
                                points   = uiState.revenueChartPoints,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 2: Property Status — full width donut
                        PropertyStatusChart(
                            slices   = uiState.propertyStatusSlices,
                            total    = stats.totalProperties,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Recent Activity ───────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        SectionTitle("Recent Activity")
                        TextButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Text("View All", color = PrimaryBlue, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        if (uiState.recentActivities.isEmpty()) {
                            EmptyStateBox(Icons.Default.Notifications, "No recent activity")
                        } else {
                            Column(Modifier.padding(4.dp)) {
                                uiState.recentActivities.forEachIndexed { idx, notification ->
                                    RealActivityRow(notification)
                                    if (idx < uiState.recentActivities.lastIndex) {
                                        HorizontalDivider(
                                            modifier  = Modifier.padding(horizontal = 16.dp),
                                            color     = Color(0xFFF0F0F0),
                                            thickness = 1.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Recent Bookings ───────────────────────────────
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        SectionTitle("Recent Bookings")
                        TextButton(onClick = { navController.navigate(Screen.ManageBookings.route) }) {
                            Text("View All", color = PrimaryBlue, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        if (uiState.recentBookings.isEmpty()) {
                            EmptyStateBox(Icons.Default.CalendarMonth, "No bookings yet")
                        } else {
                            Column(Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8F9FC))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    TableHeaderCell("Guest",    Modifier.weight(1.4f))
                                    TableHeaderCell("Property", Modifier.weight(1.8f))
                                    TableHeaderCell("Amount",   Modifier.weight(1.4f))
                                    TableHeaderCell("Status",   Modifier.weight(1.2f))
                                }
                                uiState.recentBookings.forEachIndexed { idx, booking ->
                                    RealBookingRow(booking)
                                    if (idx < uiState.recentBookings.lastIndex) {
                                        HorizontalDivider(
                                            modifier  = Modifier.padding(horizontal = 16.dp),
                                            color     = Color(0xFFF0F0F0),
                                            thickness = 1.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ✅ CHART COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════

// ─── Users Overview Line Chart ────────────────────────────────────────────────

@Composable
fun UsersOverviewChart(
    points  : List<UserChartPoint>,
    modifier: Modifier = Modifier
) {
    val animProgress by animateFloatAsState(
        targetValue  = if (points.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label        = "userChart"
    )

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Users Overview",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1A1A2E)
                )
            }
            Spacer(Modifier.height(4.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendDot(Color(0xFF4A90D9), "New")
                LegendDot(Color(0xFF26C6DA), "Active")
            }
            Spacer(Modifier.height(8.dp))

            if (points.isEmpty()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data", fontSize = 11.sp, color = Color(0xFFCCCCCC))
                }
            } else {
                // Canvas line chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    drawUsersLineChart(points, animProgress)
                }
                Spacer(Modifier.height(4.dp))
                // X-axis labels
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEach { pt ->
                        Text(
                            pt.label,
                            fontSize = 8.sp,
                            color    = Color(0xFFAAAAAA),
                            modifier = Modifier.weight(1f),
                            textAlign= TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawUsersLineChart(points: List<UserChartPoint>, progress: Float) {
    if (points.isEmpty()) return

    val maxVal  = maxOf(points.maxOf { it.newUsers }, points.maxOf { it.activeUsers }, 1)
    val w       = size.width
    val h       = size.height
    val stepX   = w / (points.size - 1).coerceAtLeast(1).toFloat()
    val padTop  = 8.dp.toPx()
    val padBot  = 4.dp.toPx()
    val drawH   = h - padTop - padBot

    fun xOf(i: Int)      = i * stepX
    fun yOf(v: Int): Float = padTop + drawH * (1f - v.toFloat() / maxVal)

    // Draw filled area + line for newUsers (blue)
    val newPath = Path()
    val newFill = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i)
        val y = yOf(pt.newUsers)
        if (i == 0) { newPath.moveTo(x, y); newFill.moveTo(x, h) ; newFill.lineTo(x, y) }
        else { newPath.lineTo(x, y); newFill.lineTo(x, y) }
    }
    newFill.lineTo(xOf(points.lastIndex), h)
    newFill.close()

    // Clip to animated progress
    clipRect(right = w * progress) {
        drawPath(newFill, Brush.verticalGradient(
            listOf(Color(0xFF4A90D9).copy(alpha = 0.18f), Color.Transparent)
        ))
        drawPath(newPath, color = Color(0xFF4A90D9), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // Draw line for activeUsers (teal)
    val activePath = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i)
        val y = yOf(pt.activeUsers)
        if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
    }
    clipRect(right = w * progress) {
        drawPath(activePath, color = Color(0xFF26C6DA), style = Stroke(
            width = 2.dp.toPx(),
            cap   = StrokeCap.Round,
            join  = StrokeJoin.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
        ))
    }

    // Dots on last visible point
    val lastIdx = ((points.size - 1) * progress).toInt().coerceIn(0, points.lastIndex)
    drawCircle(Color(0xFF4A90D9), radius = 4.dp.toPx(), center = Offset(xOf(lastIdx), yOf(points[lastIdx].newUsers)))
    drawCircle(Color.White,       radius = 2.dp.toPx(), center = Offset(xOf(lastIdx), yOf(points[lastIdx].newUsers)))
    drawCircle(Color(0xFF26C6DA), radius = 4.dp.toPx(), center = Offset(xOf(lastIdx), yOf(points[lastIdx].activeUsers)))
    drawCircle(Color.White,       radius = 2.dp.toPx(), center = Offset(xOf(lastIdx), yOf(points[lastIdx].activeUsers)))
}

// ─── Revenue Overview Line Chart ──────────────────────────────────────────────

@Composable
fun RevenueOverviewChart(
    points  : List<RevenueChartPoint>,
    modifier: Modifier = Modifier
) {
    val animProgress by animateFloatAsState(
        targetValue   = if (points.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label         = "revenueChart"
    )

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Revenue Overview",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1A1A2E)
                )
            }
            Spacer(Modifier.height(4.dp))
            Row { LegendDot(Color(0xFF2ECC71), "Revenue") }
            Spacer(Modifier.height(8.dp))

            if (points.isEmpty()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data", fontSize = 11.sp, color = Color(0xFFCCCCCC))
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    drawRevenueLineChart(points, animProgress)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEach { pt ->
                        Text(
                            pt.label,
                            fontSize  = 8.sp,
                            color     = Color(0xFFAAAAAA),
                            modifier  = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawRevenueLineChart(points: List<RevenueChartPoint>, progress: Float) {
    if (points.isEmpty()) return

    val maxVal = points.maxOf { it.revenue }.coerceAtLeast(1.0)
    val w      = size.width
    val h      = size.height
    val stepX  = w / (points.size - 1).coerceAtLeast(1).toFloat()
    val padTop = 8.dp.toPx()
    val padBot = 4.dp.toPx()
    val drawH  = h - padTop - padBot

    fun xOf(i: Int)      = i * stepX
    fun yOf(v: Double)   = (padTop + drawH * (1.0 - v / maxVal)).toFloat()

    val linePath = Path()
    val fillPath = Path()
    points.forEachIndexed { i, pt ->
        val x = xOf(i)
        val y = yOf(pt.revenue)
        if (i == 0) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, h)
            fillPath.lineTo(x, y)
        } else {
            // Smooth curve using cubic bezier
            val prevX = xOf(i - 1)
            val prevY = yOf(points[i - 1].revenue)
            val cp1X  = prevX + (x - prevX) / 2f
            linePath.cubicTo(cp1X, prevY, cp1X, y, x, y)
            fillPath.cubicTo(cp1X, prevY, cp1X, y, x, y)
        }
    }
    fillPath.lineTo(xOf(points.lastIndex), h)
    fillPath.close()

    clipRect(right = w * progress) {
        drawPath(fillPath, Brush.verticalGradient(
            listOf(Color(0xFF2ECC71).copy(alpha = 0.22f), Color.Transparent)
        ))
        drawPath(linePath, color = Color(0xFF2ECC71), style = Stroke(
            width = 2.dp.toPx(),
            cap   = StrokeCap.Round,
            join  = StrokeJoin.Round
        ))
    }

    // Dot on last animated point
    val lastIdx = ((points.size - 1) * progress).toInt().coerceIn(0, points.lastIndex)
    drawCircle(Color(0xFF2ECC71), radius = 4.dp.toPx(), center = Offset(xOf(lastIdx), yOf(points[lastIdx].revenue)))
    drawCircle(Color.White,       radius = 2.dp.toPx(), center = Offset(xOf(lastIdx), yOf(points[lastIdx].revenue)))
}

// ─── Property Status Donut Chart ──────────────────────────────────────────────

@Composable
fun PropertyStatusChart(
    slices  : List<PropertyStatusSlice>,
    total   : Int,
    modifier: Modifier = Modifier
) {
    val animProgress by animateFloatAsState(
        targetValue   = if (slices.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label         = "donutChart"
    )

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Property Status",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1A1A2E)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut canvas
                Box(
                    modifier         = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawDonutChart(slices, animProgress)
                    }
                    // Center label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$total",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF1A1A2E)
                        )
                        Text(
                            "Total",
                            fontSize = 10.sp,
                            color    = Color(0xFF888888)
                        )
                    }
                }

                // Legend — right side
                Column(
                    modifier            = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    slices.forEach { slice ->
                        val sliceColor = Color(slice.colorHex)
                        val pct = if (total > 0) (slice.count * 100f / total) else 0f
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(sliceColor)
                            )
                            Column {
                                Text(
                                    slice.label,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Color(0xFF1A1A2E)
                                )
                                Text(
                                    "${slice.count} (${"%.1f".format(pct)}%)",
                                    fontSize = 10.sp,
                                    color    = Color(0xFF888888)
                                )
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

    val total      = slices.sumOf { it.count }.coerceAtLeast(1)
    val stroke     = 22.dp.toPx()
    val padding    = stroke / 2f + 4.dp.toPx()
    val diameter   = size.minDimension - padding * 2
    val topLeft    = Offset(padding, padding)
    val arcSize    = Size(diameter, diameter)
    val gap        = 3f   // degrees gap between slices

    var startAngle = -90f

    slices.forEach { slice ->
        val sweep      = (slice.count.toFloat() / total) * 360f * progress - gap
        val color      = Color(slice.colorHex)

        drawArc(
            color      = color,
            startAngle = startAngle,
            sweepAngle = sweep.coerceAtLeast(0f),
            useCenter  = false,
            topLeft    = topLeft,
            size       = arcSize,
            style      = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        startAngle += (slice.count.toFloat() / total) * 360f
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, fontSize = 9.sp, color = Color(0xFF888888))
    }
}

@Composable
private fun EmptyStateBox(icon: ImageVector, text: String) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(40.dp))
        Text(text, color = Color(0xFFAAAAAA), fontSize = 13.sp)
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    notifCount         : Int,
    onMenuClick        : () -> Unit,
    onNotificationClick: () -> Unit
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
                    Badge(containerColor = AccentGold) {
                        Text(
                            if (notifCount > 99) "99+" else "$notifCount",
                            fontSize = 9.sp,
                            color    = Color.White
                        )
                    }
                }
            }) {
                IconButton(onClick = onNotificationClick) {
                    Icon(Icons.Default.Notifications, null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier         = Modifier
                    .padding(end = 12.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AccentGold),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
    )
}

// ─── Hero Banner ──────────────────────────────────────────────────────────────

@Composable
fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(PrimaryBlue, Color(0xFF0D4F8C), Color(0xFF1565C0))
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 20.dp)
                .clip(CircleShape)
                .background(AccentGold.copy(alpha = 0.15f))
        )
        Column {
            Text("Welcome back, Admin 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("Here's what's happening today.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = AccentGold, modifier = Modifier.size(13.dp))
                Text("Super Admin  •  Platform Overview", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

// ─── Modern Stat Card ─────────────────────────────────────────────────────────

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
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = Color(0xFF888888))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (positive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint     = if (positive) Color(0xFF2ECC71) else Color(0xFFBA1A1A),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "$change from last week",
                    fontSize = 10.sp,
                    color    = if (positive) Color(0xFF2ECC71) else Color(0xFFBA1A1A)
                )
            }
        }
    }
}

// ─── Pending Highlight Card ───────────────────────────────────────────────────

@Composable
fun PendingHighlightCard(pendingCount: Int, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (pendingCount > 0) Color(0xFFFFF3E0) else Color(0xFFF1F8F1)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(
            1.dp,
            if (pendingCount > 0) Color(0xFFE67E22).copy(alpha = 0.3f) else Color(0xFF2ECC71).copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(36.dp).clip(CircleShape)
                        .background(if (pendingCount > 0) Color(0xFFE67E22).copy(alpha = 0.15f) else Color(0xFF2ECC71).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (pendingCount > 0) Icons.Default.PendingActions else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = if (pendingCount > 0) Color(0xFFE67E22) else Color(0xFF2ECC71),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        "$pendingCount Pending Bookings",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (pendingCount > 0) Color(0xFFE67E22) else Color(0xFF2ECC71)
                    )
                    Text(
                        if (pendingCount > 0) "Tap to review & take action" else "All bookings are handled",
                        fontSize = 11.sp,
                        color    = Color(0xFF888888)
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
        }
    }
}

// ─── Quick Action Chip ────────────────────────────────────────────────────────

@Composable
fun QuickActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier         = Modifier.size(42.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444444), lineHeight = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

// ─── Real Activity Row ────────────────────────────────────────────────────────

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
            modifier         = Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(notification.title.ifBlank { notification.body }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A2E), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (notification.body.isNotBlank() && notification.title.isNotBlank()) {
                Text(notification.body, fontSize = 11.sp, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(timeAgo(notification.createdAt), fontSize = 11.sp, color = Color(0xFFAAAAAA))
    }
}

// ─── Real Booking Row ─────────────────────────────────────────────────────────

@Composable
fun RealBookingRow(booking: Booking) {
    val statusColor = bookingStatusColor(booking.status)
    val statusLabel = bookingStatusLabel(booking.status)
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(28.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(booking.tenantName.firstOrNull()?.toString() ?: "?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            }
            Spacer(Modifier.width(6.dp))
            Text(booking.tenantName.ifBlank { "Guest" }, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A2E), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(booking.propertyTitle.ifBlank { "Property" }, modifier = Modifier.weight(1.8f), fontSize = 12.sp, color = Color(0xFF555555), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("PKR ${"%.0f".format(booking.totalAmount)}", modifier = Modifier.weight(1.4f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
        Box(
            modifier         = Modifier.weight(1.2f).clip(RoundedCornerShape(20.dp)).background(statusColor.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(statusLabel, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Table Header Cell ────────────────────────────────────────────────────────

@Composable
fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF888888))
}

// ─── Section Title ────────────────────────────────────────────────────────────

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
}

// ─── Side Drawer ──────────────────────────────────────────────────────────────

@Composable
fun AdminDrawerContent(navController: NavController, onClose: () -> Unit) {
    val drawerItems = listOf(
        Triple(Icons.Default.Dashboard,      "Dashboard",         Screen.AdminDashboard.route),
        Triple(Icons.Default.CheckCircle,    "Verify Properties", Screen.VerifyProperties.route),
        Triple(Icons.Default.VerifiedUser,   "Verify Users",      Screen.VerifyUsers.route),
        Triple(Icons.Default.ManageAccounts, "Manage Users",      Screen.ManageUsers.route),
        Triple(Icons.Default.HomeWork,       "Manage Properties", Screen.ManageProperties.route),
        Triple(Icons.Default.CalendarMonth,  "Manage Bookings",   Screen.ManageBookings.route),
        Triple(Icons.Default.BarChart,       "Reports",           Screen.Reports.route),
        Triple(Icons.Default.Payment,        "Payment Reports",   Screen.PaymentReports.route),
        Triple(Icons.Default.Notifications,  "Notifications",     Screen.Notifications.route),
        Triple(Icons.Default.Settings,       "Settings",          Screen.Settings.route),
    )
    ModalDrawerSheet(drawerContainerColor = Color.White, modifier = Modifier.width(280.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF1565C0))))
                .padding(20.dp)
        ) {
            Column {
                Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(AccentGold), contentAlignment = Alignment.Center) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text("Admin",       color = Color.White,                     fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Super Admin", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        drawerItems.forEach { (icon, label, route) ->
            NavigationDrawerItem(
                icon     = { Icon(icon, null, tint = PrimaryBlue) },
                label    = { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                selected = false,
                onClick  = {
                    navController.navigate(route) {
                        popUpTo(Screen.AdminDashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                    onClose()
                },
                modifier = Modifier.padding(horizontal = 8.dp),
                colors   = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    selectedContainerColor   = PrimaryBlue.copy(alpha = 0.1f)
                )
            )
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = Color(0xFFEEEEEE))
        NavigationDrawerItem(
            icon     = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFBA1A1A)) },
            label    = { Text("Logout", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Medium) },
            selected = false,
            onClick  = { /* handle logout */ },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            colors   = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
    }
}











