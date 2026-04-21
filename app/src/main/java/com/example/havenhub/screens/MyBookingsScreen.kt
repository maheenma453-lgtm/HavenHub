package com.example.havenhub.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel

private val MNavy  = Color(0xFF0D1B3E)
private val MGold  = Color(0xFFD4AF37)
private val MBg    = Color(0xFFF1F5F9)
private val MMuted = Color(0xFF8899AA)
private val MCard  = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController: NavController,
    userId: String,
    viewModel: BookingViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val userRole = authUiState.userRole

    LaunchedEffect(userId) {
        viewModel.loadBookings(
            userId = userId,
            role   = userRole.ifEmpty { "tenant" }
        )
    }

    val uiState     by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Confirmed", "Checked In", "Completed", "Cancelled")

    val tabIcons = listOf(
        Icons.Default.HourglassEmpty,
        Icons.Default.CheckCircle,
        Icons.Default.Login,
        Icons.Default.Done,
        Icons.Default.Cancel
    )

    val filteredBookings = uiState.bookings.filter { booking ->
        when (selectedTab) {
            0 -> booking.bookingStatus == BookingStatus.PENDING
            1 -> booking.bookingStatus == BookingStatus.CONFIRMED
            2 -> booking.bookingStatus == BookingStatus.CHECKED_IN
            3 -> booking.bookingStatus == BookingStatus.COMPLETED
            4 -> booking.bookingStatus == BookingStatus.CANCELLED
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(MNavy, Color(0xFF1A2F5E)))
                    )
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "My Bookings",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            "${uiState.bookings.size} total bookings",
                            color = Color.White.copy(0.6f),
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
                .padding(paddingValues)
        ) {

            // ── Custom Tab Row ────────────────────────────────
            Box(
                modifier = Modifier
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
                            selected  = isSelected,
                            onClick   = { selectedTab = index },
                            modifier  = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = null,
                                    tint = if (isSelected) MGold else MMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text       = title,
                                    color      = if (isSelected) MNavy else MMuted,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                // Count badge
                                val count = uiState.bookings.count { booking ->
                                    when (index) {
                                        0    -> booking.bookingStatus == BookingStatus.PENDING
                                        1    -> booking.bookingStatus == BookingStatus.CONFIRMED
                                        2    -> booking.bookingStatus == BookingStatus.CHECKED_IN
                                        3    -> booking.bookingStatus == BookingStatus.COMPLETED
                                        4    -> booking.bookingStatus == BookingStatus.CANCELLED
                                        else -> false
                                    }
                                }
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) MGold else MMuted.copy(0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$count",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) MNavy else MMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color    = MGold,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                filteredBookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(MNavy.copy(0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    tabIcons[selectedTab],
                                    contentDescription = null,
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
                                color     = MMuted,
                                fontSize  = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(booking.bookingId)
                                    )
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBookingCard(booking: Booking, onTap: () -> Unit, onPayNow: () -> Unit) {
    val (statusColor, statusText, statusBg) = when (booking.bookingStatus) {
        BookingStatus.PENDING    -> Triple(Color(0xFFF59E0B), "Pending",    Color(0xFFFFF8E1))
        BookingStatus.CONFIRMED  -> Triple(Color(0xFF10B981), "Confirmed",  Color(0xFFE8F5E9))
        BookingStatus.CHECKED_IN -> Triple(Color(0xFF3B82F6), "Checked In", Color(0xFFE3F2FD))
        BookingStatus.COMPLETED  -> Triple(Color(0xFF6B7280), "Completed",  Color(0xFFF3F4F6))
        BookingStatus.CANCELLED  -> Triple(Color(0xFFEF4444), "Cancelled",  Color(0xFFFFEBEE))
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onTap() },
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MCard),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // ── Colored top accent bar ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(listOf(statusColor, statusColor.copy(0.4f)))
                    )
            )

            Column(modifier = Modifier.padding(18.dp)) {

                // Title + Status badge
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = booking.propertyTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        color      = MNavy,
                        modifier   = Modifier.weight(1f),
                        maxLines   = 1
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            statusText,
                            color      = statusColor,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Address
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint     = MGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        " ${booking.propertyAddress}",
                        color    = MMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEF2F7))
                Spacer(Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BookingStat(icon = Icons.Default.NightlightRound, label = "Nights",  value = "${booking.totalNights}")
                    BookingStat(icon = Icons.Default.People,          label = "Guests",  value = "${booking.guestCount}")
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total", color = MMuted, fontSize = 11.sp)
                        Text(
                            booking.formattedTotal,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MGold,
                            fontSize   = 17.sp
                        )
                    }
                }

                // Pay Now button (only for Pending)
                if (booking.bookingStatus == BookingStatus.PENDING) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick   = onPayNow,
                        modifier  = Modifier.fillMaxWidth().height(48.dp),
                        shape     = RoundedCornerShape(14.dp),
                        colors    = ButtonDefaults.buttonColors(containerColor = MNavy)
                    ) {
                        Icon(
                            Icons.Default.Payment,
                            contentDescription = null,
                            tint     = MGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Pay Now",
                            fontSize   = 14.sp,
                            color      = MGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingStat(
    icon  : androidx.compose.ui.graphics.vector.ImageVector,
    label : String,
    value : String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MNavy.copy(0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MNavy, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, color = MMuted, fontSize = 10.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, color = MNavy, fontSize = 15.sp)
        }
    }
}