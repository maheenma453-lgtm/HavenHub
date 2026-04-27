package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel

private val MNavy  = Color(0xFF0D1B3E)
private val MGold  = Color(0xFFD4AF37)
private val MBg    = Color(0xFFF1F5F9)
private val MMuted = Color(0xFF8899AA)
private val MCard  = Color(0xFFFFFFFF)
private val MRed   = Color(0xFFEF4444)
private val MGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController: NavController,
    userId       : String,
    viewModel    : BookingViewModel = hiltViewModel(),
    authViewModel: AuthViewModel    = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val userRole     = authUiState.userRole

    LaunchedEffect(userId) {
        viewModel.loadBookings(
            userId = userId,
            role   = userRole.ifEmpty { "tenant" }
        )
    }

    val uiState     by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── Cancel confirm dialog state ───────────────────────────────
    var showCancelDialog  by remember { mutableStateOf(false) }
    var cancelBookingId   by remember { mutableStateOf("") }

    val tabs     = listOf("Pending", "Confirmed", "Checked In", "Completed", "Cancelled")
    val tabIcons = listOf(
        Icons.Default.HourglassEmpty,
        Icons.Default.CheckCircle,
        Icons.Default.Login,
        Icons.Default.Done,
        Icons.Default.Cancel
    )

    val filteredBookings = uiState.bookings.filter { booking ->
        when (selectedTab) {
            0    -> booking.bookingStatus == BookingStatus.PENDING
            1    -> booking.bookingStatus == BookingStatus.CONFIRMED
            2    -> booking.bookingStatus == BookingStatus.CHECKED_IN
            3    -> booking.bookingStatus == BookingStatus.COMPLETED
            4    -> booking.bookingStatus == BookingStatus.CANCELLED
            else -> true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            snackbarHostState.showSnackbar("Booking cancelled successfully")
            viewModel.clearMessages()
        }
    }

    // ── Cancel confirmation dialog ────────────────────────────────
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor   = MCard,
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier         = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(MRed.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Cancel, null, tint = MRed, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text(
                    "Cancel Booking?",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = MNavy
                )
            },
            text = {
                Text(
                    "Are you sure you want to cancel this booking?\nThis action cannot be undone.",
                    fontSize   = 14.sp,
                    color      = MMuted,
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelBooking(cancelBookingId)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MRed),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Cancel", color = MCard, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCancelDialog = false },
                    border  = BorderStroke(1.dp, MNavy),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text("Keep Booking", color = MNavy, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar       = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(MNavy, Color(0xFF1A2F5E))))
            ) {
                Row(
                    modifier          = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MCard)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("My Bookings", fontWeight = FontWeight.Bold, color = MCard, fontSize = 18.sp)
                        Text(
                            "${uiState.bookings.size} total bookings",
                            color    = MCard.copy(0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MBg)
                .padding(top = paddingValues.calculateTopPadding())
        ) {

            // ── Tab Row ───────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().background(MCard).padding(vertical = 4.dp)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MCard,
                    contentColor     = MNavy,
                    edgePadding      = 12.dp,
                    indicator        = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .height(3.dp)
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MGold)
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick  = { selectedTab = index },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier              = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    tabIcons[index], null,
                                    tint     = if (isSelected) MGold else MMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    title,
                                    color      = if (isSelected) MNavy else MMuted,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                val count = uiState.bookings.count { b ->
                                    when (index) {
                                        0    -> b.bookingStatus == BookingStatus.PENDING
                                        1    -> b.bookingStatus == BookingStatus.CONFIRMED
                                        2    -> b.bookingStatus == BookingStatus.CHECKED_IN
                                        3    -> b.bookingStatus == BookingStatus.COMPLETED
                                        4    -> b.bookingStatus == BookingStatus.CANCELLED
                                        else -> false
                                    }
                                }
                                if (count > 0) {
                                    Box(
                                        modifier         = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) MGold else MMuted.copy(0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$count",
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = if (isSelected) MNavy else MMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = MGold, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                    }
                }

                filteredBookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier         = Modifier.size(100.dp).clip(RoundedCornerShape(28.dp))
                                    .background(MNavy.copy(0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(tabIcons[selectedTab], null, tint = MNavy.copy(0.25f), modifier = Modifier.size(46.dp))
                            }
                            Spacer(Modifier.height(20.dp))
                            Text("No ${tabs[selectedTab]} Bookings", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MNavy)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Your ${tabs[selectedTab].lowercase()} bookings\nwill appear here",
                                color      = MMuted,
                                fontSize   = 13.sp,
                                textAlign  = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(items = filteredBookings, key = { it.bookingId }) { booking ->
                            PremiumBookingCard(
                                booking  = booking,
                                onTap    = {
                                    navController.navigate(Screen.BookingDetails.createRoute(booking.bookingId))
                                },
                                onPayNow = {
                                    navController.navigate(
                                        Screen.Payment.createRoute(
                                            bookingId = booking.bookingId,
                                            payerId   = booking.tenantId,
                                            payeeId   = booking.landlordId,
                                            payerName = booking.tenantName,
                                            payeeName = booking.landlordName,
                                            amount    = booking.totalAmount
                                        )
                                    )
                                },
                                // ✅ Cancel — sirf PENDING pe, dialog open karo
                                onCancel = {
                                    cancelBookingId  = booking.bookingId
                                    showCancelDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// BOOKING CARD
// ════════════════════════════════════════════════════════════════
@Composable
private fun PremiumBookingCard(
    booking : Booking,
    onTap   : () -> Unit,
    onPayNow: () -> Unit,
    onCancel: () -> Unit        // ✅ new param
) {
    val (statusColor, statusText, statusBg) = when (booking.bookingStatus) {
        BookingStatus.PENDING    -> Triple(Color(0xFFF59E0B), "Pending",    Color(0xFFFFF8E1))
        BookingStatus.CONFIRMED  -> Triple(MGreen,            "Confirmed",  Color(0xFFE8F5E9))
        BookingStatus.CHECKED_IN -> Triple(Color(0xFF3B82F6), "Checked In", Color(0xFFE3F2FD))
        BookingStatus.COMPLETED  -> Triple(Color(0xFF6B7280), "Completed",  Color(0xFFF3F4F6))
        BookingStatus.CANCELLED  -> Triple(MRed,              "Cancelled",  Color(0xFFFFEBEE))
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onTap() },
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MCard),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Colored top accent bar
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp)
                    .background(Brush.horizontalGradient(listOf(statusColor, statusColor.copy(0.35f))))
            )

            Column(Modifier.padding(18.dp)) {

                // Title + status badge
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        booking.propertyTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        color      = MNavy,
                        modifier   = Modifier.weight(1f),
                        maxLines   = 1
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = MGold, modifier = Modifier.size(13.dp))
                    Text(" ${booking.propertyAddress}", color = MMuted, fontSize = 12.sp, maxLines = 1)
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEF2F7))
                Spacer(Modifier.height(12.dp))

                // Stats
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BookingStat(Icons.Default.NightlightRound, "Nights",  "${booking.totalNights}")
                    BookingStat(Icons.Default.People,          "Guests",  "${booking.guestCount}")
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total", color = MMuted, fontSize = 11.sp)
                        Text(booking.formattedTotal, fontWeight = FontWeight.ExtraBold, color = MGold, fontSize = 17.sp)
                    }
                }

                // ✅ PENDING: Cancel + Pay Now buttons side by side (SRS BR-3)
                if (booking.bookingStatus == BookingStatus.PENDING) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cancel
                        OutlinedButton(
                            onClick  = onCancel,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.5.dp, MRed),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MRed)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Pay Now
                        Button(
                            onClick  = onPayNow,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = MNavy)
                        ) {
                            Icon(Icons.Default.Payment, null, tint = MGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Pay Now", fontSize = 13.sp, color = MGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ✅ CONFIRMED: Show confirmed info only (no actions)
                if (booking.bookingStatus == BookingStatus.CONFIRMED) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(MGreen.copy(0.08f)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Booking confirmed by landlord",
                            fontSize   = 12.sp,
                            color      = MGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingStat(
    icon : androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MNavy.copy(0.06f)),
            Alignment.Center
        ) {
            Icon(icon, null, tint = MNavy, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, color = MMuted, fontSize = 10.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, color = MNavy, fontSize = 15.sp)
        }
    }
}