package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.BookingViewModel

// ─────────────────────────────────────────────────────────────────
// MyBookingsScreen.kt
// PURPOSE : Shows all bookings made by the current user.
//           Tab filter: Active, Pending, Completed, Cancelled.
//           Each card shows property name, dates, status, price.
// NAVIGATION:
//   → BookingDetailsScreen (tap on booking)
//   → PaymentScreen (if pending)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController : NavController,
    viewModel     : BookingViewModel = hiltViewModel()
) {

    // ── Load user's bookings ───────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.loadMyBookings()
    }

    // ── State ──────────────────────────────────────────────────────
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Active", "Pending", "Completed", "Cancelled")

    val bookings  by viewModel.myBookings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Filter bookings based on selected tab
    val filteredBookings = bookings.filter { booking ->
        when (selectedTab) {
            0 -> booking.status == "ACTIVE"
            1 -> booking.status == "PENDING"
            2 -> booking.status == "COMPLETED"
            3 -> booking.status == "CANCELLED"
            else -> true
        }
    }

    // ── UI ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = BackgroundWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = PrimaryBlue,
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

            // ── Status Tab Row ─────────────────────────────────────
            TabRow(
                selectedTabIndex  = selectedTab,
                containerColor    = BackgroundWhite,
                contentColor      = PrimaryBlue,
                indicator         = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier  = TabRowDefaults.tabIndicatorOffset(tabPositions[selectedTab]),
                        color     = PrimaryBlue,
                        height    = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text     = {
                            Text(
                                text  = title,
                                color = if (selectedTab == index) PrimaryBlue else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // ── Content ────────────────────────────────────────────
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredBookings.isEmpty()) {
                // Empty state per tab
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📋", fontSize = 52.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text       = "No ${tabs[selectedTab]} Bookings",
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text     = "Your ${tabs[selectedTab].lowercase()} bookings will appear here",
                            fontSize = 13.sp,
                            color    = TextSecondary
                        )
                    }
                }
            } else {
                // Bookings list
                LazyColumn(
                    contentPadding        = PaddingValues(16.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookings) { booking ->
                        BookingCard(
                            booking   = booking,
                            onTap     = { navController.navigate(Screen.BookingDetails.createRoute(booking.id)) },
                            onPayNow  = { navController.navigate(Screen.Payment.createRoute(booking.id)) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// BookingCard
// Individual booking item with status badge and action buttons
// ─────────────────────────────────────────────────────────────────
@Composable
private fun BookingCard(
    booking  : com.havenhub.data.model.Booking,
    onTap    : () -> Unit,
    onPayNow : () -> Unit
) {
    // Status color mapping
    val (statusColor, statusText) = when (booking.status) {
        "ACTIVE"    -> Pair(SuccessGreen, "✓ Active")
        "PENDING"   -> Pair(WarningAmber, "⏳ Pending Payment")
        "COMPLETED" -> Pair(TextSecondary, "✓ Completed")
        "CANCELLED" -> Pair(ErrorRed,     "✗ Cancelled")
        else        -> Pair(TextSecondary, booking.status)
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors    = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header: Property name + status badge ───────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = booking.propertyTitle,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = statusText, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "📍 ${booking.propertyCity}", fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(10.dp))

            Divider(color = BorderGray, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // ── Dates & Package Row ────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                BookingInfoItem(label = "Check-In",  value = booking.checkInDate)
                BookingInfoItem(label = "Check-Out", value = booking.checkOutDate)
                BookingInfoItem(label = "Package",   value = booking.rentalPackage)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Bottom: Price + Pay Now ────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Total", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text       = "PKR ${booking.totalPrice}",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryBlue
                    )
                }

                // Show "Pay Now" button only for pending bookings
                if (booking.status == "PENDING") {
                    Button(
                        onClick = onPayNow,
                        shape   = RoundedCornerShape(8.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Pay Now", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Small label-value pair ─────────────────────────────────────────
@Composable
private fun BookingInfoItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

