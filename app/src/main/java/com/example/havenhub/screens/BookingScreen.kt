package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel
import com.example.havenhub.viewmodel.PropertyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController    : NavController,
    propertyId       : String,
    viewModel        : BookingViewModel  = hiltViewModel(),
    propertyViewModel: PropertyViewModel = hiltViewModel(),
    authViewModel    : AuthViewModel     = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val propUiState by propertyViewModel.uiState.collectAsState()

    val isDark = isSystemInDarkTheme()

    val screenBg    = if (isDark) DarkBg           else Color(0xFFF5F7FA)
    val cardBg      = if (isDark) DarkSurface       else Color.White
    val navyColor   = if (isDark) DarkBgSecondary   else Color(0xFF0D1B3E)
    val goldColor   = if (isDark) DarkGoldPrimary   else Color(0xFFD4AF37)
    val textPrimary = if (isDark) DarkTextPrimary   else Color(0xFF0D1B3E)
    val textSecond  = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val hintColor   = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val categoryBg  = if (isDark) DarkBgElevated    else Color(0xFFF5F7FA)
    val dividerCol  = if (isDark) DarkBorder        else Color.White.copy(0.2f)

    val currentUid  = authUiState.currentUser?.uid         ?: ""
    val currentName = authUiState.currentUser?.displayName ?: ""

    LaunchedEffect(propertyId) {
        propertyViewModel.loadPropertyDetail(propertyId)
    }

    val property = propUiState.propertyDetail

    var selectedDuration   by remember { mutableStateOf("Daily") }
    var nights             by remember { mutableIntStateOf(1) }
    var guests             by remember { mutableIntStateOf(1) }
    var checkInDate        by remember { mutableStateOf("") }
    var checkOutDate       by remember { mutableStateOf("") }
    var showCheckInPicker  by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }

    // Double-tap guard — booking ek baar hi create hogi
    var isSubmitting by remember { mutableStateOf(false) }

    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val totalAmount = remember(nights, selectedDuration, property) {
        when (selectedDuration) {
            "Weekly"  -> (property?.pricePerWeek  ?: (property?.pricePerNight?.times(7)  ?: 0.0)) * (nights / 7).coerceAtLeast(1)
            "Monthly" -> (property?.pricePerMonth ?: (property?.pricePerNight?.times(30) ?: 0.0)) * (nights / 30).coerceAtLeast(1)
            else      -> (property?.pricePerNight ?: 0.0) * nights
        }
    }

    // Navigate to confirmation on success
    LaunchedEffect(uiState.actionSuccess, uiState.createdBookingId) {
        if (uiState.actionSuccess && !uiState.createdBookingId.isNullOrEmpty()) {
            isSubmitting = false
            navController.navigate(Screen.BookingConfirmation.createRoute(uiState.createdBookingId!!)) {
                popUpTo(Screen.Booking.route) { inclusive = true }
            }
            viewModel.clearMessages()
        }
    }

    // Reset submitting flag on error
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            isSubmitting = false
        }
    }

    // ── Date pickers ──────────────────────────────────────────────────────────
    if (showCheckInPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showCheckInPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        checkInDate = dateFormatter.format(Date(it))
                    }
                    showCheckInPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckInPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showCheckOutPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() + 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showCheckOutPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        checkOutDate = dateFormatter.format(Date(it))
                    }
                    showCheckOutPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckOutPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        containerColor      = screenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text("Complete Booking", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = navyColor,
                    titleContentColor          = if (isDark) DarkGoldPrimary else Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .background(screenBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                // ── Loading ───────────────────────────────────────────────────
                propUiState.isLoading -> {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = if (isDark) DarkGoldPrimary else navyColor
                        )
                    }
                }

                // ── Property load failed ──────────────────────────────────────
                property == null -> {
                    BookingBlockedCard(
                        message = "Property load nahi ho rahi. Wapas jao aur dobara try karo."
                    )
                }

                // ════════════════════════════════════════════════════════════
                // ✅ FIX: Property "already booked" check
                //
                // Problem: Pehle sirf APPROVED check tha. Agar property ka
                // status "BOOKED" ho gaya (kisi ne already book kar liya)
                // toh tenant phir bhi booking screen khol sakta tha.
                //
                // Fix: status == "BOOKED" alag friendly message dikhao.
                // ════════════════════════════════════════════════════════════
                property.status.equals(PropertyStatus.APPROVED.name, ignoreCase = true).not()
                        && property.status.equals("BOOKED", ignoreCase = true) -> {
                    BookingBlockedCard(
                        message  = "Yeh property abhi already booked hai — phir baad mein try karein.",
                        isBooked = true
                    )
                }

                // ── Not approved by admin yet ─────────────────────────────────
                property.status.equals(PropertyStatus.APPROVED.name, ignoreCase = true).not() -> {
                    BookingBlockedCard(
                        message = "Yeh property abhi admin se approve nahi hui — booking nahi ho sakti."
                    )
                }

                // ── Property is APPROVED and available — show booking form ─────
                else -> {

                    // 1. Property Summary card
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Property",
                                fontSize = 13.sp,
                                color    = hintColor
                            )
                            Text(
                                property.title,
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint     = goldColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(" ${property.city}", fontSize = 13.sp, color = hintColor)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    property.formattedPrice + "/night",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = textPrimary
                                )
                            }
                        }
                    }

                    // 2. Duration Type card
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Rental Duration Type",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = textPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Daily", "Weekly", "Monthly").forEach { duration ->
                                    val isSelected = selectedDuration == duration
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) navyColor else categoryBg)
                                            .clickable { selectedDuration = duration }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            duration,
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color      = if (isSelected) goldColor else hintColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Date Selection card
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Select Dates",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = textPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick  = { showCheckInPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(8.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor = textPrimary
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Check-in",  fontSize = 11.sp, color = hintColor)
                                        Text(
                                            checkInDate.ifEmpty  { "Select" },
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color      = textPrimary
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick  = { showCheckOutPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(8.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor = textPrimary
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Check-out", fontSize = 11.sp, color = hintColor)
                                        Text(
                                            checkOutDate.ifEmpty { "Select" },
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color      = textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Stay Details card
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Stay Details",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = textPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CounterRow(
                                label       = "Number of Nights",
                                count       = nights,
                                onDecrement = { if (nights > 1) nights-- },
                                onIncrement = { nights++ },
                                isDark      = isDark
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CounterRow(
                                label       = "Number of Guests",
                                count       = guests,
                                onDecrement = { if (guests > 1) guests-- },
                                onIncrement = { if (guests < property.maxGuests) guests++ },
                                isDark      = isDark
                            )
                            Text(
                                "Max ${property.maxGuests} guests allowed",
                                fontSize = 12.sp,
                                color    = hintColor,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // 5. Price Breakdown card
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = navyColor),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Price Breakdown",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PriceRow(
                                "${property.formattedPrice} × $nights nights",
                                "PKR ${"%.0f".format(property.pricePerNight * nights)}"
                            )
                            if (property.securityDeposit > 0) {
                                PriceRow(
                                    "Security Deposit",
                                    "PKR ${"%.0f".format(property.securityDeposit)}"
                                )
                            }
                            HorizontalDivider(
                                color    = dividerCol,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = goldColor
                                )
                                Text(
                                    "PKR ${"%.0f".format(totalAmount + property.securityDeposit)}",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = goldColor
                                )
                            }
                        }
                    }

                    // Error message card
                    uiState.errorMessage?.let { error ->
                        Card(
                            colors   = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                error,
                                color    = Color(0xFFB71C1C),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // 6. Confirm Booking button
                    val isFormComplete =
                        checkInDate.isNotEmpty() && checkOutDate.isNotEmpty()

                    Button(
                        onClick = {
                            // Double tap guard
                            if (isSubmitting) return@Button
                            isSubmitting = true

                            val sdfParse = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            val checkIn  = try {
                                sdfParse.parse(checkInDate)
                            } catch (e: Exception) { null }
                            val checkOut = try {
                                sdfParse.parse(checkOutDate)
                            } catch (e: Exception) { null }

                            val booking = Booking(
                                propertyId       = propertyId,
                                propertyTitle    = property.title,
                                landlordId       = property.ownerId,
                                landlordName     = property.ownerName,
                                tenantId         = currentUid,
                                tenantName       = currentName,
                                pricePerNight    = property.pricePerNight,
                                totalAmount      = totalAmount,
                                subtotal         = totalAmount,
                                securityDeposit  = property.securityDeposit,
                                status           = BookingStatus.PENDING.name,
                                paymentStatus    = PaymentStatus.PENDING.name,
                                propertyAddress  = "${property.address}, ${property.city}",
                                propertyCoverUrl = property.coverImageUrl,
                                totalNights      = nights,
                                guestCount       = guests,
                                paymentMethod    = "Pending",
                                checkInDate      = checkIn?.let {
                                    com.google.firebase.Timestamp(it)
                                },
                                checkOutDate     = checkOut?.let {
                                    com.google.firebase.Timestamp(it)
                                }
                            )
                            viewModel.createBooking(booking)
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled  = !uiState.isLoading && isFormComplete && !isSubmitting,
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = navyColor,
                            contentColor           = if (isDark) DarkGoldPrimary else Color.White,
                            disabledContainerColor = Color(0xFFBDC3C7),
                            disabledContentColor   = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        if (uiState.isLoading || isSubmitting) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Confirm Booking",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        "By confirming, you agree to the cancellation policy",
                        fontSize = 11.sp,
                        color    = hintColor,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// BOOKING BLOCKED CARD
// Shown when booking is not possible — not approved, or already booked.
// isBooked = true shows a house icon + different wording.
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun BookingBlockedCard(
    message : String,
    isBooked: Boolean = false
) {
    val bgColor   = if (isBooked) Color(0xFFFFF3E0) else Color(0xFFFFEBEE)
    val textColor = if (isBooked) Color(0xFFE65100) else Color(0xFFB71C1C)

    Card(
        colors   = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                // House icon for booked, warning for unapproved
                imageVector = if (isBooked) Icons.Default.Home else Icons.Default.Warning,
                contentDescription = null,
                tint        = textColor,
                modifier    = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                color      = textColor,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// COUNTER ROW
// Reusable decrement / label / increment row for nights and guest counts.
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun CounterRow(
    label      : String,
    count      : Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    isDark     : Boolean
) {
    val textColor    = if (isDark) DarkTextPrimary else Color(0xFF0D1B3E)
    val minusBg      = if (isDark) DarkBgElevated  else Color(0xFFE0E6ED)
    val plusBg       = if (isDark) DarkBgSecondary else Color(0xFF0D1B3E)
    val plusIconTint = if (isDark) DarkGoldPrimary else Color.White

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = textColor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick  = onDecrement,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(minusBg)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = null,
                    tint               = textColor,
                    modifier           = Modifier.size(18.dp)
                )
            }
            Text(
                text       = "$count",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 18.dp),
                color      = textColor
            )
            IconButton(
                onClick  = onIncrement,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(plusBg)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint               = plusIconTint,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// PRICE ROW
// Label + value pair inside the dark price breakdown card.
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun PriceRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(0.8f))
        Text(value, fontSize = 13.sp, color = Color.White)
    }
}