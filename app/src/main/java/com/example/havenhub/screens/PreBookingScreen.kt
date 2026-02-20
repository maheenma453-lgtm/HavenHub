package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
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
import com.havenhub.ui.viewmodel.VacationViewModel

// ─────────────────────────────────────────────────────────────────
// PreBookingScreen.kt
// PURPOSE : Advance vacation booking form. User selects destination,
//           travel dates (up to 6 months ahead), number of guests,
//           and pays a 20% deposit to hold the booking.
//           Remaining amount is paid on arrival.
// NAVIGATION: PreBookingScreen → BookingConfirmationScreen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreBookingScreen(
    navController : NavController,
    viewModel     : VacationViewModel = hiltViewModel()
) {

    // ── State ──────────────────────────────────────────────────────
    var selectedDestination by remember { mutableStateOf("") }
    var checkInDate         by remember { mutableStateOf("") }
    var checkOutDate        by remember { mutableStateOf("") }
    var guestCount          by remember { mutableStateOf(2) }
    var propertyType        by remember { mutableStateOf("") }

    val isLoading   by viewModel.isLoading.collectAsState()
    val totalAmount = 25000L    // Example; computed in viewModel
    val depositAmount = totalAmount / 5   // 20% deposit

    val destinations = listOf("Murree", "Swat Valley", "Hunza", "Nathia Gali", "Chitral", "Kaghan")
    val propertyTypes = listOf("Cottage", "Hotel Room", "Guest House", "Cabin", "Chalet")

    // ── UI ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Book Vacation") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // ── Advance Booking Notice ─────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = PrimaryBlue.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier          = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📅", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text       = "Advance Booking — Pay 20% Deposit",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = PrimaryBlue
                        )
                        Text(
                            text     = "Book up to 6 months ahead. Rest of payment on arrival.",
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Select Destination ────────────────────────────────
            SectionLabel("Select Destination")
            Spacer(modifier = Modifier.height(8.dp))

            // Destination grid (2 columns)
            destinations.chunked(2).forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { dest ->
                        val isSelected = selectedDestination == dest
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryBlue else SurfaceGray)
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = BorderGray,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedDestination = dest },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = dest,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color      = if (isSelected) BackgroundWhite else TextPrimary
                            )
                        }
                    }
                    // Fill empty slot if odd number
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Travel Dates ───────────────────────────────────────
            SectionLabel("Travel Dates")
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Check-in
                VacDateField(
                    label    = "Check-In",
                    date     = checkInDate,
                    modifier = Modifier.weight(1f),
                    onClick  = { /* open date picker */ }
                )
                // Check-out
                VacDateField(
                    label    = "Check-Out",
                    date     = checkOutDate,
                    modifier = Modifier.weight(1f),
                    onClick  = { /* open date picker */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Property Type ──────────────────────────────────────
            SectionLabel("Property Type")
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                propertyTypes.take(3).forEach { type ->
                    FilterChip(
                        selected = propertyType == type,
                        onClick  = { propertyType = if (propertyType == type) "" else type },
                        label    = { Text(type, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor     = BackgroundWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Guests ─────────────────────────────────────────────
            SectionLabel("Number of Guests")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick  = { if (guestCount > 1) guestCount-- },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceGray)
                ) {
                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text("$guestCount", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick  = { if (guestCount < 20) guestCount++ },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlue)
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BackgroundWhite)
                }
                Text("guests", fontSize = 14.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Price Summary ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = SurfaceGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Payment Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated Total", fontSize = 13.sp, color = TextSecondary)
                        Text("PKR $totalAmount", fontSize = 13.sp, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deposit (20%)", fontSize = 13.sp, color = TextSecondary)
                        Text("PKR $depositAmount", fontSize = 13.sp, color = WarningAmber, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remaining on Arrival", fontSize = 13.sp, color = TextSecondary)
                        Text("PKR ${totalAmount - depositAmount}", fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Pre-Book Button ────────────────────────────────────
            Button(
                onClick = {
                    viewModel.createPreBooking(
                        destination = selectedDestination,
                        checkIn     = checkInDate,
                        checkOut    = checkOutDate,
                        guests      = guestCount,
                        type        = propertyType
                    ) { bookingId ->
                        navController.navigate(Screen.BookingConfirmation.createRoute(bookingId))
                    }
                },
                enabled  = selectedDestination.isNotEmpty() && checkInDate.isNotEmpty() && checkOutDate.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = PrimaryBlue,
                    disabledContainerColor = BorderGray
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = BackgroundWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Pre-Book & Pay Deposit (PKR $depositAmount)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
}

@Composable
private fun VacDateField(label: String, date: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                .background(SurfaceGray)
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp), tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text     = if (date.isEmpty()) "Select" else date,
                    fontSize = 13.sp,
                    color    = if (date.isEmpty()) TextHint else TextPrimary
                )
            }
        }
    }
}

