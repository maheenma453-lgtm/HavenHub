package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.ReportsViewModel

// ── Brand Colors ──────────────────────────────────────────────────────────────
private val NavyBlue      = Color(0xFF1B2A4A)
private val NavyLight     = Color(0xFF243658)
private val Gold          = Color(0xFFC9A227)
private val GoldDark      = Color(0xFFA07D10)
private val PageBg        = Color(0xFFF4F6FA)
private val GreenStat     = Color(0xFF27AE60)
private val OrangeStat    = Color(0xFFE67E22)
private val RedStat       = Color(0xFFE74C3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel    : ReportsViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    var selectedPeriod by remember { mutableStateOf("All Time") }
    val periods = listOf("All Time", "Today", "This Month")

    Scaffold(
        containerColor = PageBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NavyBlue, NavyLight)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Gold)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Reports",
                            color         = Color.White,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            selectedPeriod,
                            color    = Gold.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
                // Gold shimmer line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { padding ->

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Period Filter ──────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(NavyBlue, NavyLight)))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(periods) { period ->
                            val selected = selectedPeriod == period
                            FilterChip(
                                selected = selected,
                                onClick  = {
                                    selectedPeriod = period
                                    viewModel.loadAllReportsData()
                                },
                                label = {
                                    Text(
                                        period,
                                        fontSize   = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Gold,
                                    selectedLabelColor     = Color.White,
                                    containerColor         = Color.White.copy(0.12f),
                                    labelColor             = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selected,
                                    selectedBorderColor = GoldDark,
                                    borderColor         = Color.White.copy(0.25f),
                                    selectedBorderWidth = 1.5.dp,
                                    borderWidth         = 1.dp
                                )
                            )
                        }
                    }
                }
            }

            // ── Summary Header ─────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(22.dp))
                PremiumSectionHeader("Summary", modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(14.dp))
            }

            // ── Stats Grid ─────────────────────────────────────────────────────
            item {
                if (uiState.isLoading) {
                    Box(
                        Modifier.fillMaxWidth().height(160.dp),
                        Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold, strokeWidth = 3.dp)
                    }
                } else {
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PremiumStatCard(
                                icon     = Icons.Default.AccountBalanceWallet,
                                label    = "Total Revenue",
                                value    = "PKR ${String.format("%,.0f", uiState.stats.totalRevenue)}",
                                gradient = listOf(NavyBlue, Color(0xFF6A1B9A)),
                                accentColor = Color(0xFFAB47BC),
                                modifier = Modifier.weight(1f)
                            )
                            PremiumStatCard(
                                icon     = Icons.Default.CalendarMonth,
                                label    = "Total Bookings",
                                value    = "${uiState.stats.totalBookings}",
                                gradient = listOf(NavyBlue, Color(0xFF00796B)),
                                accentColor = GreenStat,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PremiumStatCard(
                                icon     = Icons.Default.People,
                                label    = "Total Users",
                                value    = "${uiState.stats.totalUsers}",
                                gradient = listOf(NavyBlue, NavyLight),
                                accentColor = Gold,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumStatCard(
                                icon     = Icons.Default.Home,
                                label    = "Active Props",
                                value    = "${uiState.stats.activeProperties}",
                                gradient = listOf(GoldDark, Gold),
                                accentColor = Gold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Booking Status Breakdown ───────────────────────────────────────
            item {
                Spacer(Modifier.height(26.dp))
                PremiumSectionHeader(
                    "Booking Status Breakdown",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(14.dp))

                val total = uiState.stats.totalBookings.toFloat()
                val pendingCount = uiState.stats.totalBookings -
                        uiState.stats.completedBookings -
                        uiState.stats.cancelledBookings

                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PremiumStatusRow(
                        label      = "Completed",
                        count      = uiState.stats.completedBookings,
                        percentage = if (total > 0) (uiState.stats.completedBookings / total) * 100f else 0f,
                        color      = GreenStat,
                        icon       = Icons.Default.CheckCircle
                    )
                    PremiumStatusRow(
                        label      = "Pending",
                        count      = pendingCount,
                        percentage = if (total > 0) (pendingCount / total) * 100f else 0f,
                        color      = OrangeStat,
                        icon       = Icons.Default.HourglassEmpty
                    )
                    PremiumStatusRow(
                        label      = "Cancelled",
                        count      = uiState.stats.cancelledBookings,
                        percentage = if (total > 0) (uiState.stats.cancelledBookings / total) * 100f else 0f,
                        color      = RedStat,
                        icon       = Icons.Default.Cancel
                    )
                }
            }

            // ── Detailed Reports ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(26.dp))
                PremiumSectionHeader(
                    "Detailed Reports",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation    = 4.dp,
                            shape        = RoundedCornerShape(16.dp),
                            ambientColor = NavyBlue.copy(0.08f),
                            spotColor    = NavyBlue.copy(0.12f)
                        )
                ) {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        // Top accent bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Brush.horizontalGradient(listOf(NavyBlue, Gold)))
                        )

                        val navItems = listOf(
                            Triple(Icons.Default.Payment,  "Payment Transactions", "View all ${uiState.payments.size} transactions"),
                            Triple(Icons.Default.Home,     "Property Analytics",   "Performance overview"),
                            Triple(Icons.Default.People,   "User Activity",        "Registration & engagement"),
                            Triple(Icons.Default.BarChart, "Revenue Breakdown",    "Monthly & category split"),
                        )

                        navItems.forEachIndexed { idx, (icon, title, sub) ->
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(NavyBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = NavyBlue
                                    )
                                    Text(
                                        sub,
                                        fontSize = 11.sp,
                                        color    = NavyBlue.copy(alpha = 0.45f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Gold.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        null,
                                        tint     = Gold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (idx < navItems.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color    = NavyBlue.copy(alpha = 0.07f)
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
private fun PremiumSectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(Gold, GoldDark)))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = NavyBlue,
            letterSpacing = 0.2.sp
        )
    }
}

// ── Premium Stat Card ──────────────────────────────────────────────────────────
@Composable
private fun PremiumStatCard(
    icon       : ImageVector,
    label      : String,
    value      : String,
    gradient   : List<Color>,
    accentColor: Color,
    modifier   : Modifier = Modifier
) {
    Box(
        modifier = modifier.shadow(
            elevation    = 4.dp,
            shape        = RoundedCornerShape(16.dp),
            ambientColor = NavyBlue.copy(0.08f),
            spotColor    = NavyBlue.copy(0.12f)
        )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Thin top accent bar
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
                Text(
                    value,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = NavyBlue
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    label,
                    fontSize = 11.sp,
                    color    = NavyBlue.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Premium Status Row ─────────────────────────────────────────────────────────
@Composable
private fun PremiumStatusRow(
    label     : String,
    count     : Int,
    percentage: Float,
    color     : Color,
    icon      : ImageVector
) {
    Box(
        modifier = Modifier.shadow(
            elevation    = 3.dp,
            shape        = RoundedCornerShape(14.dp),
            ambientColor = NavyBlue.copy(0.06f),
            spotColor    = NavyBlue.copy(0.08f)
        )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Left colored accent stripe
            Row(modifier = Modifier.fillMaxWidth()) {
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
                            Text(
                                label,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color      = NavyBlue
                            )
                        }
                        // Count + % badge
                        Surface(
                            color = color.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(20.dp),
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
                                .background(
                                    Brush.horizontalGradient(listOf(color.copy(0.7f), color))
                                )
                        )
                    }
                }
            }
        }
    }
}











