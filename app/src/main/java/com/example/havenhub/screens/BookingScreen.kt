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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.PaymentStatus
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

    // ── Theme colors ──────────────────────────────────────────────────────────
    val screenBg    = if (isDark) DarkBg           else Color(0xFFF5F7FA)
    val cardBg      = if (isDark) DarkSurface       else Color.White
    val navyColor   = if (isDark) DarkBgSecondary   else Color(0xFF0D1B3E)
    val goldColor   = if (isDark) DarkGoldPrimary   else Color(0xFFD4AF37)
    val textPrimary = if (isDark) DarkTextPrimary   else Color(0xFF0D1B3E)
    val hintColor   = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val categoryBg  = if (isDark) DarkBgElevated    else Color(0xFFF5F7FA)
    val dividerCol  = if (isDark) DarkBorder        else Color.White.copy(0.2f)

    val currentUid  = authUiState.currentUser?.uid         ?: ""
    val currentName = authUiState.currentUser?.displayName ?: ""

    // Load property details when screen opens
    LaunchedEffect(propertyId) {
        propertyViewModel.loadPropertyDetail(propertyId)
    }

    val property = propUiState.propertyDetail

    // ── Local state ───────────────────────────────────────────────────────────
    var selectedDuration   by remember { mutableStateOf("Daily") }
    var guests             by remember { mutableIntStateOf(1) }
    var checkInDate        by remember { mutableStateOf("") }
    var checkOutDate       by remember { mutableStateOf("") }
    var showCheckInPicker  by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }

    // Prevents double-tap on Confirm Booking button
    var isSubmitting by remember { mutableStateOf(false) }

    // Manual nights counter — user can increment/decrement directly
    // This is overridden automatically when both dates are selected
    var manualNights by remember { mutableIntStateOf(1) }

    // Date formatter — locale-aware, non-recomposing
    val context = LocalContext.current
    val staticDateFormatter = remember(context) {
        val locale = context.applicationContext.resources.configuration.locales[0]
        SimpleDateFormat("dd MMM yyyy", locale)
    }

    // ── Nights calculation logic ──────────────────────────────────────────────
    // Priority 1: Both dates selected → auto-calculate from date difference
    // Priority 2: Only manual counter set → use manualNights directly
    val calculatedNights = remember(checkInDate, checkOutDate, manualNights, staticDateFormatter) {
        try {
            if (checkInDate.isNotEmpty() && checkOutDate.isNotEmpty()) {
                // Auto mode — calculate from selected dates
                val dIn  = staticDateFormatter.parse(checkInDate)
                val dOut = staticDateFormatter.parse(checkOutDate)
                if (dIn != null && dOut != null && dOut.time > dIn.time) {
                    ((dOut.time - dIn.time) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                } else { manualNights }
            } else {
                // Manual mode — use counter value
                manualNights
            }
        } catch (e: Exception) { manualNights }
    }

    // ── Total price calculation (hybrid formula for packages) ─────────────────
    // Daily  : pricePerNight × nights
    // Weekly : full weeks × weeklyRate + leftover nights × nightly rate
    // Monthly: full months × monthlyRate + leftover nights × nightly rate
    val totalAmount = remember(calculatedNights, selectedDuration, property) {
        val baseNight = property?.pricePerNight ?: 0.0
        when (selectedDuration) {
            "Weekly" -> {
                val weeklyPrice = property?.pricePerWeek ?: (baseNight * 7.0)
                val fullWeeks   = calculatedNights / 7
                val extraNights = calculatedNights % 7
                (fullWeeks * weeklyPrice) + (extraNights * baseNight)
            }
            "Monthly" -> {
                val monthlyPrice = property?.pricePerMonth ?: (baseNight * 30.0)
                val fullMonths   = calculatedNights / 30
                val extraNights  = calculatedNights % 30
                (fullMonths * monthlyPrice) + (extraNights * baseNight)
            }
            else -> baseNight * calculatedNights
        }
    }

    // Navigate to confirmation screen after successful booking
    LaunchedEffect(uiState.actionSuccess, uiState.createdBookingId) {
        if (uiState.actionSuccess && !uiState.createdBookingId.isNullOrEmpty()) {
            isSubmitting = false
            navController.navigate(Screen.BookingConfirmation.createRoute(uiState.createdBookingId!!)) {
                popUpTo(Screen.Booking.route) { inclusive = true }
            }
            viewModel.clearMessages()
        }
    }

    // Reset submitting flag if an error occurs
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) isSubmitting = false
    }

    // ── Date pickers ──────────────────────────────────────────────────────────
    if (showCheckInPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showCheckInPicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { checkInDate = staticDateFormatter.format(Date(it)) }
                    showCheckInPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCheckInPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    if (showCheckOutPicker) {
        // Pre-offset the picker based on selected package type
        val offsetMillis = when (selectedDuration) {
            "Weekly"  -> 86400000L * 7
            "Monthly" -> 86400000L * 30
            else      -> 86400000L
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + offsetMillis)
        DatePickerDialog(
            onDismissRequest = { showCheckOutPicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { checkOutDate = staticDateFormatter.format(Date(it)) }
                    showCheckOutPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCheckOutPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    // ── Main scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        containerColor      = screenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Complete Booking", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                // Loading state
                propUiState.isLoading -> {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = if (isDark) DarkGoldPrimary else navyColor)
                    }
                }

                // Property not found
                property == null -> {
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Property load nahi ho rahi. Wapas jao aur dobara try karo.",
                            color    = Color(0xFFB71C1C),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Property not yet approved
                property.status != "APPROVED" -> {
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Yeh property abhi admin se approve nahi hui — booking nahi ho sakti.",
                            color    = Color(0xFFB71C1C),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {

                    // ── Card 1: Property Summary ──────────────────────────────
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Property", fontSize = 13.sp, color = hintColor)
                            Text(
                                property.title,
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    tint     = goldColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(" ${property.city}", fontSize = 13.sp, color = hintColor)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    property.formattedPrice + "/night",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = textPrimary
                                )
                            }
                        }
                    }

                    // ── Card 2: Rental Duration Type ──────────────────────────
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
                            Spacer(Modifier.height(12.dp))
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

                    // ── Card 3: Date Selection ────────────────────────────────
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
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick  = { showCheckInPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(8.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Check-in", fontSize = 11.sp, color = hintColor)
                                        Text(
                                            checkInDate.ifEmpty { "Select" },
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
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
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

                    // ── Card 4: Stay Details ──────────────────────────────────
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
                            Spacer(Modifier.height(14.dp))

                            // ── Number of Nights counter ──────────────────────
                            // User can manually set nights using +/- buttons.
                            // If both check-in and check-out dates are selected,
                            // the auto-calculated value is shown instead (read-only feel).
                            val datesSelected = checkInDate.isNotEmpty() && checkOutDate.isNotEmpty()

                            CounterRow(
                                label       = "Number of Nights",
                                count       = calculatedNights,
                                // Disable +/- if dates are selected (auto mode active)
                                onDecrement = { if (!datesSelected && manualNights > 1) manualNights-- },
                                onIncrement = { if (!datesSelected) manualNights++ },
                                isDark      = isDark,
                                // Dim the buttons when dates override manual input
                                isDisabled  = datesSelected
                            )

                            // Hint: tell user which mode is active
                            Text(
                                text     = if (datesSelected) "Auto-calculated from selected dates"
                                else "Or select dates above for auto-calculation",
                                fontSize = 11.sp,
                                color    = hintColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(Modifier.height(12.dp))

                            // ── Number of Guests counter ──────────────────────
                            CounterRow(
                                label       = "Number of Guests",
                                count       = guests,
                                onDecrement = { if (guests > 1) guests-- },
                                onIncrement = { if (guests < property.maxGuests) guests++ },
                                isDark      = isDark,
                                isDisabled  = false
                            )
                            Text(
                                "Max ${property.maxGuests} guests allowed",
                                fontSize = 12.sp,
                                color    = hintColor,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    // ── Card 5: Price Breakdown ───────────────────────────────
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
                            Spacer(Modifier.height(12.dp))

                            // Rate × nights row
                            // label uses weight(1f) so value never overflows vertically
                            PriceRow(
                                label = "${property.formattedPrice} ($selectedDuration) × $calculatedNights nights",
                                value = "PKR ${"%.0f".format(totalAmount)}"
                            )

                            // Security deposit row (only shown if > 0)
                            if (property.securityDeposit > 0) {
                                PriceRow(
                                    label = "Security Deposit",
                                    value = "PKR ${"%.0f".format(property.securityDeposit)}"
                                )
                            }

                            HorizontalDivider(
                                color    = dividerCol,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Total row
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
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
                            colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
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

                    // ── Confirm Booking Button ────────────────────────────────
                    // Enabled only when dates are selected OR manual nights >= 1
                    val isFormComplete = checkInDate.isNotEmpty() && checkOutDate.isNotEmpty()
                            || (checkInDate.isEmpty() && checkOutDate.isEmpty() && manualNights >= 1)

                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            isSubmitting = true

                            val checkIn  = try { staticDateFormatter.parse(checkInDate)  } catch (e: Exception) { null }
                            val checkOut = try { staticDateFormatter.parse(checkOutDate) } catch (e: Exception) { null }

                            val booking = Booking(
                                propertyId       = propertyId,
                                propertyTitle    = property.title,
                                landlordId       = property.ownerId,
                                landlordName     = property.ownerName,
                                tenantId         = currentUid,
                                tenantName       = currentName,
                                pricePerNight    = property.pricePerNight,
                                totalAmount      = totalAmount + property.securityDeposit,
                                subtotal         = totalAmount,
                                securityDeposit  = property.securityDeposit,
                                status           = BookingStatus.PENDING.name,
                                paymentStatus    = PaymentStatus.PENDING.name,
                                propertyAddress  = "${property.address}, ${property.city}",
                                propertyCoverUrl = property.coverImageUrl,
                                totalNights      = calculatedNights,
                                guestCount       = guests,
                                paymentMethod    = "Pending",
                                checkInDate      = checkIn?.let  { com.google.firebase.Timestamp(it) },
                                checkOutDate     = checkOut?.let { com.google.firebase.Timestamp(it) }
                            )
                            viewModel.createBooking(booking)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
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
                            Text("Confirm Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

// ─────────────────────────────────────────────────────────────────────────────
// CounterRow — reusable +/- counter widget
// isDisabled: when true, buttons are visually dimmed and clicks are ignored
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CounterRow(
    label      : String,
    count      : Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    isDark     : Boolean,
    isDisabled : Boolean = false
) {
    val textColor    = if (isDark) DarkTextPrimary else Color(0xFF0D1B3E)
    val minusBg      = if (isDark) DarkBgElevated  else Color(0xFFE0E6ED)
    val plusBg       = if (isDark) DarkBgSecondary else Color(0xFF0D1B3E)
    val plusIconTint = if (isDark) DarkGoldPrimary else Color.White

    // Dim buttons when counter is in auto/disabled mode
    val buttonAlpha  = if (isDisabled) 0.4f else 1f

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = textColor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick  = onDecrement,
                enabled  = !isDisabled,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(minusBg.copy(alpha = buttonAlpha))
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = null,
                    tint     = textColor.copy(alpha = buttonAlpha),
                    modifier = Modifier.size(18.dp)
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
                enabled  = !isDisabled,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(plusBg.copy(alpha = buttonAlpha))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint     = plusIconTint.copy(alpha = buttonAlpha),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PriceRow — label on left, value on right inside the dark price card
// label uses weight(1f) so long text wraps cleanly instead of pushing
// the value off-screen or splitting it vertically
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PriceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label wraps naturally on small screens
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        // Value always stays on one line
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1
        )
    }
}