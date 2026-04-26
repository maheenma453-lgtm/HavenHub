package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel    : ReportsViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    var selectedPeriod by remember { mutableStateOf("All Time") }

    val periods = listOf("All Time", "Today", "This Month")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        containerColor = Color(0xFFF4F6FB)
    ) { padding ->

        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding  = PaddingValues(bottom = 32.dp)
        ) {

            // ── Period Filter Banner ───────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(PrimaryBlue, Color(0xFF1565C0))))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                label    = { Text(period, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor     = PrimaryBlue,
                                    containerColor         = Color.White.copy(.15f),
                                    labelColor             = Color.White
                                ),
                                border   = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selected,
                                    selectedBorderColor = Color.Transparent,
                                    borderColor         = Color.White.copy(.3f)
                                )
                            )
                        }
                    }
                }
            }

            // ── Summary Section ────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PrimaryBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Stats Grid ─────────────────────────────────────
            item {
                if (uiState.isLoading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        Alignment.Center
                    ) { CircularProgressIndicator(color = PrimaryBlue) }
                } else {
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReportStatCard(
                                icon     = Icons.Default.AccountBalanceWallet,
                                label    = "Total Revenue",
                                value    = "PKR ${String.format("%,.0f", uiState.stats.totalRevenue)}",
                                gradient = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)),
                                modifier = Modifier.weight(1f)
                            )
                            ReportStatCard(
                                icon     = Icons.Default.CalendarMonth,
                                label    = "Total Bookings",
                                value    = "${uiState.stats.totalBookings}",
                                gradient = listOf(Color(0xFF00897B), Color(0xFF26C6DA)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReportStatCard(
                                icon     = Icons.Default.People,
                                label    = "Total Users",
                                value    = "${uiState.stats.totalUsers}",
                                gradient = listOf(Color(0xFF1A3A6B), Color(0xFF4A90D9)),
                                modifier = Modifier.weight(1f)
                            )
                            ReportStatCard(
                                icon     = Icons.Default.Home,
                                label    = "Active Props",
                                value    = "${uiState.stats.activeProperties}",
                                gradient = listOf(Color(0xFFE65100), Color(0xFFFFB300)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Booking Status Breakdown ───────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PrimaryBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Booking Status Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                }
                Spacer(Modifier.height(12.dp))

                val total = uiState.stats.totalBookings.toFloat()
                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModernStatusRow(
                        label      = "Completed",
                        count      = uiState.stats.completedBookings,
                        percentage = if (total > 0) (uiState.stats.completedBookings / total) * 100f else 0f,
                        color      = SuccessGreen
                    )
                    ModernStatusRow(
                        label      = "Pending",
                        count      = uiState.stats.totalBookings - uiState.stats.completedBookings - uiState.stats.cancelledBookings,
                        percentage = if (total > 0) ((uiState.stats.totalBookings - uiState.stats.completedBookings - uiState.stats.cancelledBookings) / total) * 100f else 0f,
                        color      = WarningOrange
                    )
                    ModernStatusRow(
                        label      = "Cancelled",
                        count      = uiState.stats.cancelledBookings,
                        percentage = if (total > 0) (uiState.stats.cancelledBookings / total) * 100f else 0f,
                        color      = ErrorRed
                    )
                }
            }

            // ── Detailed Reports Section ───────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PrimaryBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Detailed Reports", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                }
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    val navItems = listOf(
                        Triple(Icons.Default.Payment,      "Payment Transactions",  "View all ${uiState.payments.size} transactions"),
                        Triple(Icons.Default.Home,         "Property Analytics",    "Performance overview"),
                        Triple(Icons.Default.People,       "User Activity",         "Registration & engagement"),
                        Triple(Icons.Default.BarChart,     "Revenue Breakdown",     "Monthly & category split"),
                    )

                    navItems.forEachIndexed { idx, (icon, title, sub) ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier         = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlue.copy(.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                                Text(sub, fontSize = 11.sp, color = Color(0xFF888888))
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFCCCCCC))
                        }
                        if (idx < navItems.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        }
                    }
                }
            }
        }
    }
}

// ── Report Stat Card ──────────────────────────────────────────────
@Composable
private fun ReportStatCard(
    icon    : ImageVector,
    label   : String,
    value   : String,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = Color(0xFF888888))
        }
    }
}

// ── Status Progress Row ───────────────────────────────────────────
@Composable
private fun ModernStatusRow(
    label     : String,
    count     : Int,
    percentage: Float,
    color     : Color
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(color.copy(.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "$count (${String.format("%.1f", percentage)}%)",
                            fontSize   = 11.sp,
                            color      = color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress          = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color             = color,
                trackColor        = color.copy(.12f)
            )
        }
    }
}




















