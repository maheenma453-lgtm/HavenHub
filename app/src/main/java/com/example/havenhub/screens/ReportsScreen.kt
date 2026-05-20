package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.ReportsViewModel

// ── Semantic status colors — intentional, outside theme ──────────────────────
private val GreenStat  = Color(0xFF27AE60)
private val OrangeStat = Color(0xFFE67E22)
private val RedStat    = Color(0xFFE74C3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel    : ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Period state — drives both UI highlight and filtered data fetch ────────
    var selectedPeriod by remember { mutableStateOf("All Time") }
    val periods = listOf("All Time", "Today", "This Month")

    // ── Theme colors ──────────────────────────────────────────────────────────
    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val background       = MaterialTheme.colorScheme.background
    val onBackground     = MaterialTheme.colorScheme.onBackground
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface

    Scaffold(
        containerColor = background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tertiary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Reports",
                            color         = onPrimary,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        // Subtitle shows active period
                        Text(
                            selectedPeriod,
                            color    = tertiary.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
                // Gold shimmer accent line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(background.copy(alpha = 0f), tertiary, background.copy(alpha = 0f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { padding ->

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Period Filter Chips ────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(primary, primaryContainer)))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(periods) { period ->
                            val selected = selectedPeriod == period
                            FilterChip(
                                selected = selected,
                                onClick  = {
                                    // FIX: period is now passed to viewModel
                                    selectedPeriod = period
                                    viewModel.loadReportsByPeriod(period)
                                },
                                label = {
                                    Text(
                                        period,
                                        fontSize   = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tertiary,
                                    selectedLabelColor     = onPrimary,
                                    containerColor         = onPrimary.copy(0.12f),
                                    labelColor             = onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selected,
                                    selectedBorderColor = tertiary,
                                    borderColor         = onPrimary.copy(0.25f),
                                    selectedBorderWidth = 1.5.dp,
                                    borderWidth         = 1.dp
                                )
                            )
                        }
                    }
                }
            }

            // ── Summary Section Header ─────────────────────────────────────────
            item {
                Spacer(Modifier.height(22.dp))
                PremiumSectionHeader(
                    text      = "Summary",
                    lineColor = tertiary,
                    textColor = onBackground,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Stats Grid ─────────────────────────────────────────────────────
            item {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().height(160.dp), Alignment.Center) {
                        CircularProgressIndicator(color = tertiary, strokeWidth = 3.dp)
                    }
                } else {
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PremiumStatCard(
                                icon           = Icons.Default.AccountBalanceWallet,
                                label          = "Total Revenue",
                                value          = "PKR ${String.format("%,.0f", uiState.stats.totalRevenue)}",
                                gradient       = listOf(primary, Color(0xFF6A1B9A)),
                                accentColor    = Color(0xFFAB47BC),
                                surfaceColor   = surface,
                                onSurfaceColor = onSurface,
                                modifier       = Modifier.weight(1f)
                            )
                            PremiumStatCard(
                                icon           = Icons.Default.CalendarMonth,
                                label          = "Total Bookings",
                                value          = "${uiState.stats.totalBookings}",
                                gradient       = listOf(primary, Color(0xFF00796B)),
                                accentColor    = GreenStat,
                                surfaceColor   = surface,
                                onSurfaceColor = onSurface,
                                modifier       = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PremiumStatCard(
                                icon           = Icons.Default.People,
                                label          = "Total Users",
                                value          = "${uiState.stats.totalUsers}",
                                gradient       = listOf(primary, primaryContainer),
                                accentColor    = tertiary,
                                surfaceColor   = surface,
                                onSurfaceColor = onSurface,
                                modifier       = Modifier.weight(1f)
                            )
                            PremiumStatCard(
                                icon           = Icons.Default.Home,
                                label          = "Active Props",
                                value          = "${uiState.stats.activeProperties}",
                                gradient       = listOf(tertiary.copy(0.8f), tertiary),
                                accentColor    = tertiary,
                                surfaceColor   = surface,
                                onSurfaceColor = onSurface,
                                modifier       = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Booking Status Breakdown ───────────────────────────────────────
            item {
                Spacer(Modifier.height(26.dp))
                PremiumSectionHeader(
                    text      = "Booking Status Breakdown",
                    lineColor = tertiary,
                    textColor = onBackground,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(14.dp))

                val total        = uiState.stats.totalBookings.toFloat()
                val pendingCount = (uiState.stats.totalBookings -
                        uiState.stats.completedBookings -
                        uiState.stats.cancelledBookings).coerceAtLeast(0)

                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PremiumStatusRow(
                        label        = "Completed",
                        count        = uiState.stats.completedBookings,
                        percentage   = if (total > 0) (uiState.stats.completedBookings / total) * 100f else 0f,
                        color        = GreenStat,
                        icon         = Icons.Default.CheckCircle,
                        surfaceColor = surface,
                        textColor    = onSurface
                    )
                    PremiumStatusRow(
                        label        = "Pending",
                        count        = pendingCount,
                        percentage   = if (total > 0) (pendingCount / total) * 100f else 0f,
                        color        = OrangeStat,
                        icon         = Icons.Default.HourglassEmpty,
                        surfaceColor = surface,
                        textColor    = onSurface
                    )
                    PremiumStatusRow(
                        label        = "Cancelled",
                        count        = uiState.stats.cancelledBookings,
                        percentage   = if (total > 0) (uiState.stats.cancelledBookings / total) * 100f else 0f,
                        color        = RedStat,
                        icon         = Icons.Default.Cancel,
                        surfaceColor = surface,
                        textColor    = onSurface
                    )
                }
            }

            // ── Detailed Reports ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(26.dp))
                PremiumSectionHeader(
                    text      = "Detailed Reports",
                    lineColor = tertiary,
                    textColor = onBackground,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation    = 4.dp,
                            shape        = RoundedCornerShape(16.dp),
                            ambientColor = primary.copy(0.08f),
                            spotColor    = primary.copy(0.12f)
                        )
                ) {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = surface),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        // Top accent bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Brush.horizontalGradient(listOf(primary, tertiary)))
                        )

                        // FIX: Each item now has a clickable route to an existing screen.
                        // No new screens needed — reusing what's already built.
                        //
                        // Payment Transactions → PaymentReportsScreen (existing)
                        // Property Analytics   → ManagePropertiesScreen (existing)
                        // User Activity        → ManageUsersScreen (existing)
                        // Revenue Breakdown    → PaymentReportsScreen (same, existing)
                        val navItems = listOf(
                            Triple(
                                Icons.Default.Payment,
                                "Payment Transactions",
                                "View all ${uiState.payments.size} transactions"
                            ) to Screen.PaymentReports.route,

                            Triple(
                                Icons.Default.Home,
                                "Property Analytics",
                                "Properties performance overview"
                            ) to Screen.ManageProperties.route,

                            Triple(
                                Icons.Default.People,
                                "User Activity",
                                "Registration & user management"
                            ) to Screen.ManageUsers.route,

                            Triple(
                                Icons.Default.BarChart,
                                "Revenue Breakdown",
                                "Payment history & revenue data"
                            ) to Screen.PaymentReports.route,
                        )

                        navItems.forEachIndexed { idx, (item, route) ->
                            val (icon, title, sub) = item

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // FIX: clickable navigates to existing screen
                                    .clickable {
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Icon circle
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = tertiary, modifier = Modifier.size(20.dp))
                                }

                                // Title + subtitle
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = onSurface
                                    )
                                    Text(
                                        sub,
                                        fontSize = 11.sp,
                                        color    = onSurface.copy(alpha = 0.45f)
                                    )
                                }

                                // Arrow indicator
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(tertiary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        null,
                                        tint     = tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Divider between items, not after last
                            if (idx < navItems.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color    = onSurface.copy(alpha = 0.07f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Premium Section Header ─────────────────────────────────────────────────────
@Composable
private fun PremiumSectionHeader(
    text     : String,
    lineColor: Color,
    textColor: Color,
    modifier : Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(lineColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize      = 16.sp,
            fontWeight    = FontWeight.Bold,
            color         = textColor,
            letterSpacing = 0.2.sp
        )
    }
}

// ── Premium Stat Card ──────────────────────────────────────────────────────────
@Composable
private fun PremiumStatCard(
    icon          : ImageVector,
    label         : String,
    value         : String,
    gradient      : List<Color>,
    accentColor   : Color,
    surfaceColor  : Color,
    onSurfaceColor: Color,
    modifier      : Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.shadow(
            elevation    = 4.dp,
            shape        = RoundedCornerShape(16.dp),
            ambientColor = primary.copy(0.08f),
            spotColor    = primary.copy(0.12f)
        )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(gradient))
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = onSurfaceColor)
                Spacer(Modifier.height(3.dp))
                Text(
                    label,
                    fontSize   = 11.sp,
                    color      = onSurfaceColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Premium Status Row ─────────────────────────────────────────────────────────
@Composable
private fun PremiumStatusRow(
    label       : String,
    count       : Int,
    percentage  : Float,
    color       : Color,
    icon        : ImageVector,
    surfaceColor: Color,
    textColor   : Color
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.shadow(
            elevation    = 3.dp,
            shape        = RoundedCornerShape(14.dp),
            ambientColor = primary.copy(0.06f),
            spotColor    = primary.copy(0.08f)
        )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Colored left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(color)
                )
                Column(
                    modifier            = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                            }
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                        Surface(
                            color    = color.copy(alpha = 0.10f),
                            shape    = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, color.copy(0.3f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                "$count  •  ${String.format("%.1f", percentage)}%",
                                fontSize   = 11.sp,
                                color      = color,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.10f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((percentage / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(color.copy(0.7f), color)))
                        )
                    }
                }
            }
        }
    }
}