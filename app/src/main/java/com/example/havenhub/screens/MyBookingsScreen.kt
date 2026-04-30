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
import androidx.compose.material.icons.automirrored.filled.Login

private val MNavy  = Color(0xFF0D1B3E)
private val MGold  = Color(0xFFD4AF37)
private val MBg    = Color(0xFFF1F5F9)
private val MMuted = Color(0xFF8899AA)
private val MCard  = Color(0xFFFFFFFF)
private val MRed   = Color(0xFFEF4444)
private val MGreen = Color(0xFF22C55E)
private val MAmber = Color(0xFFD97706)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController: NavController,
    userId       : String,
    initialTab   : Int              = 0,
    viewModel    : BookingViewModel = hiltViewModel(),
    authViewModel: AuthViewModel    = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val userRole     = authUiState.userRole

    LaunchedEffect(userId, userRole) {
        if (userRole.isNotEmpty()) {
            viewModel.loadBookings(
                userId = userId,
                role   = userRole
            )
        }
    }
    LaunchedEffect(Unit) {
        if (userRole.isNotEmpty()) {
            viewModel.loadBookings(userId = userId, role = userRole)
        }
    }
    val uiState    by viewModel.uiState.collectAsState()
    val isLandlord  = userRole.lowercase() == "landlord"

    var selectedTab      by remember { mutableIntStateOf(initialTab) }
    var focusBookingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialTab) { selectedTab = initialTab }
    LaunchedEffect(uiState.bookings, focusBookingId) {

        val target = uiState.bookings.find { it.bookingId == focusBookingId }
            ?: return@LaunchedEffect

        selectedTab = when (target.bookingStatus) {
            BookingStatus.PENDING          -> 0
            BookingStatus.PENDING_APPROVAL -> 1
            BookingStatus.CONFIRMED        -> 2
            BookingStatus.CHECKED_IN       -> 3
            BookingStatus.COMPLETED        -> 4
            BookingStatus.CANCELLED        -> 5
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelBookingId  by remember { mutableStateOf("") }

    val tabs     = listOf("Pending", "Awaiting", "Confirmed", "Checked In", "Completed", "Cancelled")
    val tabIcons = listOf(
        Icons.Default.HourglassEmpty,
        Icons.Default.AccessTime,
        Icons.Default.CheckCircle,
        Icons.AutoMirrored.Filled.Login,
        Icons.Default.Done,
        Icons.Default.Cancel
    )

    val filteredBookings = uiState.bookings.filter { b ->
        when (selectedTab) {
            0    -> b.bookingStatus == BookingStatus.PENDING
            1    -> b.bookingStatus == BookingStatus.PENDING_APPROVAL
            2    -> b.bookingStatus == BookingStatus.CONFIRMED
            3    -> b.bookingStatus == BookingStatus.CHECKED_IN
            4    -> b.bookingStatus == BookingStatus.COMPLETED
            5    -> b.bookingStatus == BookingStatus.CANCELLED
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
            snackbarHostState.showSnackbar("Action completed successfully")
            viewModel.clearMessages()
            if (userRole.isNotEmpty()) {
                viewModel.loadBookings(userId = userId, role = userRole)
            }
        }
    }

    // ── Cancel Dialog ──────────────────────────────────────────
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor   = MCard,
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MRed.copy(0.1f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Cancel, null,
                        tint = MRed, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text("Cancel Booking?",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MNavy)
            },
            text = {
                Text(
                    "Are you sure you want to cancel this booking?\nThis action cannot be undone.",
                    fontSize = 14.sp, color = MMuted,
                    textAlign = TextAlign.Center, lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelBooking(cancelBookingId)
                        selectedTab      = 5
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
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(MNavy, Color(0xFF1A2F5E))))
            ) {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MCard)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isLandlord) "Booking Requests" else "My Bookings",
                            fontWeight = FontWeight.Bold, color = MCard, fontSize = 18.sp
                        )
                        Text("${uiState.bookings.size} total bookings",
                            color = MCard.copy(0.6f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MBg)
                .padding(top = paddingValues.calculateTopPadding())
        ) {

            // ── Tab Row ─────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MCard)
                    .padding(vertical = 4.dp)
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
                                Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
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
                                        1    -> b.bookingStatus == BookingStatus.PENDING_APPROVAL
                                        2    -> b.bookingStatus == BookingStatus.CONFIRMED
                                        3    -> b.bookingStatus == BookingStatus.CHECKED_IN
                                        4    -> b.bookingStatus == BookingStatus.COMPLETED
                                        5    -> b.bookingStatus == BookingStatus.CANCELLED
                                        else -> false
                                    }
                                }
                                if (count > 0) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MGold else MMuted.copy(0.15f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        Alignment.Center
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

            // ── Content ──────────────────────────────────────────
            when {
                uiState.isLoading || userRole.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(
                            color       = MGold,
                            modifier    = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                filteredBookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(MNavy.copy(0.06f)),
                                Alignment.Center
                            ) {
                                Icon(
                                    tabIcons[selectedTab], null,
                                    tint     = MNavy.copy(0.25f),
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "No ${tabs[selectedTab]} Bookings",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 17.sp,
                                color      = MNavy
                            )
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
                                booking    = booking,
                                isLandlord = isLandlord,
                                onTap      = {
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(booking.bookingId)
                                    )
                                },
                                onPayNow   = {
                                    focusBookingId = booking.bookingId
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
                                onCancel   = {
                                    cancelBookingId  = booking.bookingId
                                    showCancelDialog = true
                                },
                                onApprove  = {
                                    viewModel.updateStatusByAdmin(
                                        booking.bookingId,
                                        BookingStatus.CONFIRMED
                                    )
                                },
                                onReject   = {
                                    viewModel.updateStatusByAdmin(
                                        booking.bookingId,
                                        BookingStatus.CANCELLED
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// BOOKING CARD
// ══════════════════════════════════════════════════════════════
@Composable
private fun PremiumBookingCard(
    booking   : Booking,
    isLandlord: Boolean,
    onTap     : () -> Unit,
    onPayNow  : () -> Unit,
    onCancel  : () -> Unit,
    onApprove : () -> Unit,
    onReject  : () -> Unit
) {
    val (statusColor, statusText, statusBg) = when (booking.bookingStatus) {
        BookingStatus.PENDING          -> Triple(Color(0xFFF59E0B), "Pending",          Color(0xFFFFF8E1))
        BookingStatus.PENDING_APPROVAL -> Triple(MAmber,            "Awaiting Approval", Color(0xFFFFF3E0))
        BookingStatus.CONFIRMED        -> Triple(MGreen,            "Confirmed",         Color(0xFFE8F5E9))
        BookingStatus.CHECKED_IN       -> Triple(Color(0xFF3B82F6), "Checked In",        Color(0xFFE3F2FD))
        BookingStatus.COMPLETED        -> Triple(Color(0xFF6B7280), "Completed",         Color(0xFFF3F4F6))
        BookingStatus.CANCELLED        -> Triple(MRed,              "Cancelled",         Color(0xFFFFEBEE))
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onTap() },
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MCard),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Top color strip
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(listOf(statusColor, statusColor.copy(0.35f)))
                    )
            )

            Column(Modifier.padding(18.dp)) {

                // Title + Status badge
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
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(statusText,
                            color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Landlord view: Tenant ka naam
                if (isLandlord && booking.tenantName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null,
                            tint = MMuted, modifier = Modifier.size(13.dp))
                        Text(
                            "  Tenant: ${booking.tenantName}",
                            color = MMuted, fontSize = 12.sp, maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null,
                        tint = MGold, modifier = Modifier.size(13.dp))
                    Text(
                        "  ${booking.propertyAddress}",
                        color = MMuted, fontSize = 12.sp, maxLines = 1
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFEEF2F7))
                Spacer(Modifier.height(12.dp))

                // Stats row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BookingStat(Icons.Default.NightlightRound, "Nights", "${booking.totalNights}")
                    BookingStat(Icons.Default.People,          "Guests", "${booking.guestCount}")
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total", color = MMuted, fontSize = 11.sp)
                        Text(booking.formattedTotal,
                            fontWeight = FontWeight.ExtraBold, color = MGold, fontSize = 17.sp)
                    }
                }

                // ════════════════════════════════════════════
                // TENANT: PENDING → Cancel + Pay Now
                // ════════════════════════════════════════════
                if (!isLandlord && booking.bookingStatus == BookingStatus.PENDING) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                        Button(
                            onClick  = onPayNow,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = MNavy)
                        ) {
                            Icon(Icons.Default.Payment, null,
                                tint = MGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Pay Now",
                                fontSize = 13.sp, color = MGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ════════════════════════════════════════════
                // ✅ LANDLORD: PENDING + PENDING_APPROVAL dono
                //    pe Approve + Reject dikhao
                // ════════════════════════════════════════════
                if (isLandlord && (
                            booking.bookingStatus == BookingStatus.PENDING ||
                                    booking.bookingStatus == BookingStatus.PENDING_APPROVAL
                            )) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = onReject,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.5.dp, MRed),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MRed)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick  = onApprove,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = MGreen)
                        ) {
                            Icon(Icons.Default.Check, null,
                                tint = MCard, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Approve",
                                fontSize = 13.sp, color = MCard, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ════════════════════════════════════════════
                // ✅ PENDING_APPROVAL info strip — sirf TENANT ko
                // ════════════════════════════════════════════
                if (!isLandlord && booking.bookingStatus == BookingStatus.PENDING_APPROVAL) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MAmber.copy(0.09f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HourglassEmpty, null,
                            tint = MAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Payment received — awaiting landlord approval",
                                fontSize = 12.sp, color = MAmber, fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Landlord approve kare ga tab confirmed hogi",
                                fontSize = 11.sp, color = MAmber.copy(0.7f)
                            )
                        }
                    }
                }

                // CONFIRMED info strip
                if (booking.bookingStatus == BookingStatus.CONFIRMED) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MGreen.copy(0.08f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Booking confirmed — payment received",
                            fontSize = 12.sp, color = MGreen, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── Stat pill ─────────────────────────────────────────────────
@Composable
private fun BookingStat(
    icon : androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MNavy.copy(0.06f)),
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