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

// ── Semantic colors — intentional ─────────────────────────────────────────────
private val GreenOk = Color(0xFF27AE60)
private val RedErr  = Color(0xFFE74C3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReportsScreen(
    navController: NavController,
    viewModel    : ReportsViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val appLocale     = remember { Locale.getDefault() }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", appLocale) }

    // ── Period state — matches ReportsScreen periods ───────────────────────────
    // FIX: selectedPeriod is now tracked and passed to viewModel
    // Previously chip click called loadAllReportsData() without any period —
    // all data was shown regardless of which chip was selected
    var selectedPeriod by remember { mutableStateOf("All Time") }
    val periods = listOf("All Time", "Today", "This Month")

    // ── Theme colors ──────────────────────────────────────────────────────────
    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onBackground     = MaterialTheme.colorScheme.onBackground

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
                            "Payment Reports",
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
                                listOf(background.copy(0f), tertiary, background.copy(0f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { paddingValues ->

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(paddingValues),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        periods.forEach { period ->
                            val selected = selectedPeriod == period
                            FilterChip(
                                selected = selected,
                                onClick  = {
                                    // FIX: pass period to viewModel for filtered fetch
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
                            .background(tertiary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Revenue Overview",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = onBackground
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Revenue Summary Card ───────────────────────────────────────────
            item {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
                        CircularProgressIndicator(color = tertiary, strokeWidth = 3.dp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .shadow(
                                elevation    = 6.dp,
                                shape        = RoundedCornerShape(18.dp),
                                ambientColor = primary.copy(0.10f),
                                spotColor    = primary.copy(0.14f)
                            )
                    ) {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(18.dp),
                            colors    = CardDefaults.cardColors(containerColor = surface),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            // Top accent bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Brush.horizontalGradient(listOf(primary, tertiary)))
                            )
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            null,
                                            tint     = tertiary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            "Net Revenue",
                                            fontSize   = 12.sp,
                                            color      = onSurface.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        // Revenue updates based on filtered period
                                        Text(
                                            "PKR ${String.format(appLocale, "%,.0f", uiState.stats.totalRevenue)}",
                                            fontSize   = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = onSurface
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = onSurface.copy(alpha = 0.07f))
                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    PRRevenueStatPill(
                                        label    = "Total Sales",
                                        value    = "PKR ${String.format(appLocale, "%,.0f", uiState.stats.totalRevenue)}",
                                        color    = tertiary,
                                        onSurface = onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(40.dp)
                                            .background(onSurface.copy(alpha = 0.08f))
                                    )
                                    // Bookings count also filters by period
                                    PRRevenueStatPill(
                                        label    = "Bookings",
                                        value    = "${uiState.stats.totalBookings}",
                                        color    = primary,
                                        onSurface = onSurface
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
                            .background(tertiary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Recent Transactions",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = onBackground
                    )
                    Spacer(Modifier.weight(1f))
                    // Record count badge — updates with period filter
                    if (!uiState.isLoading && uiState.payments.isNotEmpty()) {
                        Surface(
                            color = primary.copy(0.08f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "${uiState.payments.size} records",
                                fontSize   = 11.sp,
                                color      = primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                            .background(surface)
                            .border(1.dp, onSurface.copy(0.08f), RoundedCornerShape(16.dp))
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
                                    .background(primary.copy(0.07f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    null,
                                    tint     = primary.copy(0.4f),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                "No transactions found",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = onSurface.copy(0.45f)
                            )
                            // Shows which period has no data
                            Text(
                                "No payments recorded for $selectedPeriod",
                                fontSize = 12.sp,
                                color    = onSurface.copy(0.3f)
                            )
                        }
                    }
                }
            }

            // ── Transaction List — filtered by selected period ─────────────────
            if (!uiState.isLoading) {
                items(uiState.payments) { payment ->
                    val dateString = payment.createdAt?.toDate()
                        ?.let { dateFormatter.format(it) } ?: "N/A"
                    PRTransactionItem(
                        transactionId = payment.paymentId,
                        amount        = "PKR ${String.format(appLocale, "%,.0f", payment.amount)}",
                        date          = dateString,
                        status        = payment.status,
                        primary       = primary,
                        tertiary      = tertiary,
                        surface       = surface,
                        onSurface     = onSurface,
                        modifier      = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ── Revenue Stat Pill ─────────────────────────────────────────────────────────
@Composable
private fun PRRevenueStatPill(
    label    : String,
    value    : String,
    color    : Color,
    onSurface: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize   = 11.sp,
            color      = onSurface.copy(alpha = 0.45f),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

// ── Transaction Item ──────────────────────────────────────────────────────────
@Composable
private fun PRTransactionItem(
    transactionId: String,
    amount       : String,
    date         : String,
    status       : String,
    primary      : Color,
    tertiary     : Color,
    surface      : Color,
    onSurface    : Color,
    modifier     : Modifier = Modifier
) {
    val isFailed  = status.contains("FAILED",    ignoreCase = true) ||
            status.contains("CANCELLED", ignoreCase = true)
    val isSuccess = status.contains("SUCCESS",   ignoreCase = true) ||
            status.contains("COMPLETED", ignoreCase = true)
    val iconColor = when { isFailed -> RedErr; isSuccess -> GreenOk; else -> tertiary }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(
                elevation    = 3.dp,
                shape        = RoundedCornerShape(14.dp),
                ambientColor = primary.copy(0.06f),
                spotColor    = primary.copy(0.08f)
            )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Colored left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(iconColor)
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
                            .background(primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = if (isFailed) Icons.Default.ArrowUpward
                            else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint               = if (isFailed) RedErr else tertiary,
                            modifier           = Modifier.size(20.dp)
                        )
                    }

                    // Transaction ID + date
                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "ID: ${transactionId.take(16)}…",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = onSurface,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(date, fontSize = 11.sp, color = onSurface.copy(alpha = 0.45f))
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
                            color      = if (isFailed) RedErr else onSurface
                        )
                        Surface(
                            color    = iconColor.copy(alpha = 0.10f),
                            shape    = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, iconColor.copy(0.3f), RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment     = Alignment.CenterVertically,
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