package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.viewmodel.ReportsViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Brand Colors ──────────────────────────────────────────────────────────────
private val NavyBlue  = Color(0xFF1B2A4A)
private val NavyLight = Color(0xFF243658)
private val Gold      = Color(0xFFC9A227)
private val GoldDark  = Color(0xFFA07D10)
private val PageBg    = Color(0xFFF4F6FA)
private val GreenOk   = Color(0xFF27AE60)
private val RedErr    = Color(0xFFE74C3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReportsScreen(
    navController: NavController,
    viewModel    : ReportsViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    var selectedPeriod by remember { mutableStateOf("All") }
    val appLocale      = remember { Locale.getDefault() }
    val dateFormatter  = remember { SimpleDateFormat("dd MMM yyyy", appLocale) }
    val periods        = listOf("All", "Today", "This Month")

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
                            "Payment Reports",
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
    ) { paddingValues ->

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        periods.forEach { period ->
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

            // ── Revenue Overview Header ────────────────────────────────────────
            item {
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.verticalGradient(listOf(Gold, GoldDark)))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Revenue Overview",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = NavyBlue
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Revenue Card ───────────────────────────────────────────────────
            item {
                if (uiState.isLoading) {
                    Box(
                        Modifier.fillMaxWidth().height(140.dp),
                        Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold, strokeWidth = 3.dp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .shadow(
                                elevation    = 6.dp,
                                shape        = RoundedCornerShape(18.dp),
                                ambientColor = NavyBlue.copy(0.10f),
                                spotColor    = NavyBlue.copy(0.14f)
                            )
                    ) {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(18.dp),
                            colors    = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            // Top gradient accent bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Brush.horizontalGradient(listOf(NavyBlue, Gold)))
                            )

                            Column(modifier = Modifier.padding(20.dp)) {
                                // Net Revenue label + value
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(NavyBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            null,
                                            tint     = Gold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            "Net Revenue",
                                            fontSize = 12.sp,
                                            color    = NavyBlue.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "PKR ${String.format(appLocale, "%,.0f", uiState.stats.totalRevenue)}",
                                            fontSize   = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = NavyBlue
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = NavyBlue.copy(alpha = 0.07f))
                                Spacer(Modifier.height(14.dp))

                                // Stats row
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    RevenueStatPill(
                                        label = "Total Sales",
                                        value = "PKR ${String.format(appLocale, "%,.0f", uiState.stats.totalRevenue)}",
                                        color = Gold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(40.dp)
                                            .background(NavyBlue.copy(alpha = 0.08f))
                                    )
                                    RevenueStatPill(
                                        label = "Bookings",
                                        value = "${uiState.stats.totalBookings}",
                                        color = NavyBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Recent Transactions Header ─────────────────────────────────────
            item {
                Spacer(Modifier.height(26.dp))
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.verticalGradient(listOf(Gold, GoldDark)))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Recent Transactions",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = NavyBlue
                    )
                    Spacer(Modifier.weight(1f))
                    if (!uiState.isLoading && uiState.payments.isNotEmpty()) {
                        Surface(
                            color = NavyBlue.copy(0.08f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "${uiState.payments.size} records",
                                fontSize = 11.sp,
                                color    = NavyBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Empty State ────────────────────────────────────────────────────
            if (!uiState.isLoading && uiState.payments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, NavyBlue.copy(0.08f), RoundedCornerShape(16.dp))
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(NavyBlue.copy(0.07f)),
                                contentAlignment = Alignment.Center
                            ) {
                                // FIX line 339: use AutoMirrored version
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    null,
                                    tint     = NavyBlue.copy(0.4f),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                "No transactions found",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = NavyBlue.copy(0.45f)
                            )
                            Text(
                                "Transactions will appear here",
                                fontSize = 12.sp,
                                color    = NavyBlue.copy(0.3f)
                            )
                        }
                    }
                }
            }

            // ── Transaction Items ──────────────────────────────────────────────
            if (!uiState.isLoading) {
                items(uiState.payments) { payment ->
                    val dateString = payment.createdAt?.toDate()
                        ?.let { dateFormatter.format(it) } ?: "N/A"

                    PremiumTransactionItem(
                        transactionId = payment.paymentId,
                        amount        = "PKR ${String.format(appLocale, "%,.0f", payment.amount)}",
                        date          = dateString,
                        status        = payment.status,
                        modifier      = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ── Revenue Stat Pill ──────────────────────────────────────────────────────────
@Composable
private fun RevenueStatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize   = 11.sp,
            color      = NavyBlue.copy(alpha = 0.45f),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontSize   = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = color
        )
    }
}

// ── Premium Transaction Item ───────────────────────────────────────────────────
@Composable
private fun PremiumTransactionItem(
    transactionId: String,
    amount       : String,
    date         : String,
    status       : String,
    modifier     : Modifier = Modifier
) {
    val isFailed = status.contains("FAILED",    ignoreCase = true) ||
            status.contains("CANCELLED", ignoreCase = true)
    val isSuccess = status.contains("SUCCESS",  ignoreCase = true) ||
            status.contains("COMPLETED",ignoreCase = true)

    val iconColor  = when {
        isFailed  -> RedErr
        isSuccess -> GreenOk
        else      -> Gold
    }
    val statusBg = iconColor.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(
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
            // Left colored stripe
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(iconColor)   // FIX line 459: plain Color, no Brush needed here
                )

                Row(
                    modifier              = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NavyBlue),   // FIX line 459: use solid Color, not conditional Brush
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFailed) Icons.Default.ArrowUpward
                            else           Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint     = if (isFailed) RedErr else Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ID + date
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "ID: ${transactionId.take(16)}…",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = NavyBlue,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            date,
                            fontSize = 11.sp,
                            color    = NavyBlue.copy(alpha = 0.45f)
                        )
                    }

                    // Amount + status badge
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            amount,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = if (isFailed) RedErr else NavyBlue
                        )
                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, iconColor.copy(0.3f), RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(iconColor)
                                )
                                Text(
                                    status,
                                    fontSize   = 10.sp,
                                    color      = iconColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
