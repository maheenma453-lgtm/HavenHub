package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.BackgroundWhite
import com.example.havenhub.ui.theme.BorderGray
import com.example.havenhub.ui.theme.ErrorRed
import com.example.havenhub.ui.theme.PrimaryBlue
import com.example.havenhub.ui.theme.SuccessGreen
import com.example.havenhub.ui.theme.TextPrimary
import com.example.havenhub.ui.theme.TextSecondary
import com.example.havenhub.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// MyBookingsScreen.kt
// Compatible with Booking.kt + BookingViewModel.kt
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController: NavController,
    userId: String,
    viewModel: BookingViewModel = hiltViewModel()
) {

    LaunchedEffect(userId) {
        viewModel.loadBookings(userId = userId, isLandlord = false)
    }

    val uiState by viewModel.uiState.collectAsState()

    // FIX: mutableIntStateOf instead of mutableStateOf for Int
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Confirmed", "Checked In", "Completed", "Cancelled")

    // FIX: BookingStatus enum se match — ACTIVE nahi hai, sahi values use ki
    val filteredBookings = uiState.bookings.filter { booking ->
        when (selectedTab) {
            0 -> booking.status == BookingStatus.PENDING
            1 -> booking.status == BookingStatus.CONFIRMED
            2 -> booking.status == BookingStatus.CHECKED_IN
            3 -> booking.status == BookingStatus.COMPLETED
            4 -> booking.status == BookingStatus.CANCELLED
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
            TopAppBar(
                title = { Text("My Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BackgroundWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
        ) {

            // FIX: TabRow deprecated → SecondaryTabRow (Material3)
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundWhite,
                contentColor = PrimaryBlue,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) PrimaryBlue else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }

                filteredBookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📋", fontSize = 52.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No ${tabs[selectedTab]} Bookings",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Your ${tabs[selectedTab].lowercase()} bookings will appear here",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredBookings,
                            // FIX: booking.id nahi — bookingId hai Booking model mein
                            key = { it.bookingId }
                        ) { booking ->
                            BookingCard(
                                booking = booking,
                                onTap = {
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(booking.bookingId)
                                    )
                                },
                                onPayNow = {
                                    navController.navigate(
                                        Screen.Payment.createRoute(booking.bookingId)
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

// ─────────────────────────────────────────────────────────────────
// BookingCard
// ─────────────────────────────────────────────────────────────────
@Composable
private fun BookingCard(
    booking: Booking,
    onTap: () -> Unit,
    onPayNow: () -> Unit
) {
    // FIX: WarningAmber nahi — Orange use kiya; ACTIVE nahi — sahi enum values
    val (statusColor, statusText) = when (booking.status) {
        BookingStatus.PENDING    -> Pair(MaterialTheme.colorScheme.tertiary,  "⏳ ${booking.status.displayName()}")
        BookingStatus.CONFIRMED  -> Pair(SuccessGreen,                         "✓ ${booking.status.displayName()}")
        BookingStatus.CHECKED_IN -> Pair(PrimaryBlue,                          "🏠 ${booking.status.displayName()}")
        BookingStatus.COMPLETED  -> Pair(TextSecondary,                        "✓ ${booking.status.displayName()}")
        BookingStatus.CANCELLED  -> Pair(ErrorRed,                             "✗ ${booking.status.displayName()}")
    }

    // FIX: Timestamp? → String conversion helper
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val checkIn  = booking.checkInDate?.toDate()?.let  { dateFormatter.format(it) } ?: "—"
    val checkOut = booking.checkOutDate?.toDate()?.let { dateFormatter.format(it) } ?: "—"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: Property title + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.propertyTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // FIX: propertyCity nahi — propertyAddress hai Booking model mein
            Text(
                text = "📍 ${booking.propertyAddress}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Dates & Guests row
            // FIX: rentalPackage nahi — guestCount + totalNights use kiye
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                BookingInfoItem(label = "Check-In",  value = checkIn)
                BookingInfoItem(label = "Check-Out", value = checkOut)
                BookingInfoItem(label = "Nights",    value = "${booking.totalNights}")
                BookingInfoItem(label = "Guests",    value = "${booking.guestCount}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price + Pay Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Total", fontSize = 11.sp, color = TextSecondary)
                    // FIX: totalPrice nahi — formattedTotal property use ki (already in Booking)
                    Text(
                        text = booking.formattedTotal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                if (booking.status == BookingStatus.PENDING) {
                    Button(
                        onClick = onPayNow,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Pay Now", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Label-value pair
@Composable
private fun BookingInfoItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}