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
import com.havenhub.ui.viewmodel.BookingViewModel

// ─────────────────────────────────────────────────────────────────
// BookingScreen.kt
// PURPOSE : Booking form where user selects:
//           • Rental package (Daily / Weekly / Monthly)
//           • Check-in and Check-out dates
//           • Number of guests
//           Shows live price calculation before confirming.
// PARAMETERS: propertyId (from navigation)
// NAVIGATION: BookingScreen → BookingConfirmationScreen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController : NavController,
    propertyId    : String,
    viewModel     : BookingViewModel = hiltViewModel()
) {

    // ── Load property info for booking ───────────────────────────
    LaunchedEffect(propertyId) {
        viewModel.loadPropertyForBooking(propertyId)
    }

    // ── State ────────────────────────────────────────────────────
    var selectedPackage  by remember { mutableStateOf("Monthly") }  // Daily / Weekly / Monthly
    var checkInDate      by remember { mutableStateOf("") }
    var checkOutDate     by remember { mutableStateOf("") }
    var guestCount       by remember { mutableStateOf(1) }
    var showDatePicker   by remember { mutableStateOf(false) }
    var isSelectingCheckIn by remember { mutableStateOf(true) }

    val property      by viewModel.bookingProperty.collectAsState()
    val totalPrice    by viewModel.calculatedPrice.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()

    val packages = listOf("Daily", "Weekly", "Monthly")

    // ── UI ────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Property") },
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
        ) {

            // ── Property Summary Card ──────────────────────────────
            property?.let { prop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGray)
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Property thumbnail
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BorderGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🏠", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = prop.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = "📍 ${prop.city}", fontSize = 12.sp, color = TextSecondary)
                            Text(text = "⭐ ${prop.rating} • ${prop.type}", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Padding(horizontal = 16.dp) {

                // ── Section: Rental Package ────────────────────────
                SectionTitle(title = "Rental Package")
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    packages.forEach { pkg ->
                        val isSelected = selectedPackage == pkg
                        PackageOption(
                            label      = pkg,
                            isSelected = isSelected,
                            modifier   = Modifier.weight(1f),
                            onClick    = {
                                selectedPackage = pkg
                                viewModel.calculatePrice(pkg, checkInDate, checkOutDate)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Section: Select Dates ──────────────────────────
                SectionTitle(title = "Select Dates")
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Check-In date picker
                    DatePickerField(
                        label     = "Check-In",
                        date      = checkInDate,
                        modifier  = Modifier.weight(1f),
                        onClick   = {
                            isSelectingCheckIn = true
                            showDatePicker     = true
                        }
                    )

                    // Check-Out date picker
                    DatePickerField(
                        label     = "Check-Out",
                        date      = checkOutDate,
                        modifier  = Modifier.weight(1f),
                        onClick   = {
                            isSelectingCheckIn = false
                            showDatePicker     = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Section: Number of Guests ──────────────────────
                SectionTitle(title = "Number of Guests")
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Decrease button
                    IconButton(
                        onClick  = { if (guestCount > 1) guestCount-- },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceGray)
                    ) {
                        Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Text(
                        text       = "$guestCount",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )

                    // Increase button
                    IconButton(
                        onClick  = { if (guestCount < 10) guestCount++ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue)
                    ) {
                        Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BackgroundWhite)
                    }

                    Text(text = "guests", fontSize = 14.sp, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Price Breakdown Card ───────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Price Breakdown", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        PriceRow(label = "Base rent ($selectedPackage)", amount = totalPrice * 0.9)
                        PriceRow(label = "Service fee",  amount = totalPrice * 0.05)
                        PriceRow(label = "Tax (5%)",     amount = totalPrice * 0.05)

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderGray)

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text       = "PKR $totalPrice",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = PrimaryBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Confirm Booking Button ─────────────────────────
                Button(
                    onClick = {
                        viewModel.createBooking(
                            propertyId = propertyId,
                            package_   = selectedPackage,
                            checkIn    = checkInDate,
                            checkOut   = checkOutDate,
                            guests     = guestCount
                        ) { bookingId ->
                            // Navigate to confirmation screen with booking ID
                            navController.navigate(Screen.BookingConfirmation.createRoute(bookingId))
                        }
                    },
                    enabled  = checkInDate.isNotEmpty() && checkOutDate.isNotEmpty() && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = PrimaryBlue,
                        disabledContainerColor = BorderGray
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = BackgroundWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Helper Composables
// ─────────────────────────────────────────────────────────────────

@Composable
private fun Padding(horizontal: androidx.compose.ui.unit.Dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = horizontal), content = content)
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
}

// Package selection card (Daily / Weekly / Monthly)
@Composable
private fun PackageOption(
    label      : String,
    isSelected : Boolean,
    modifier   : Modifier = Modifier,
    onClick    : () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryBlue else SurfaceGray)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = BorderGray,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (isSelected) BackgroundWhite else TextSecondary
        )
    }
}

// Date picker trigger field
@Composable
private fun DatePickerField(
    label    : String,
    date     : String,
    modifier : Modifier = Modifier,
    onClick  : () -> Unit
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                .background(SurfaceGray)
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text     = if (date.isEmpty()) "Select date" else date,
                    fontSize = 13.sp,
                    color    = if (date.isEmpty()) TextHint else TextPrimary
                )
            }
        }
    }
}

// Single price breakdown row
@Composable
private fun PriceRow(label: String, amount: Double) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = "PKR ${amount.toLong()}", fontSize = 13.sp, color = TextPrimary)
    }
}


