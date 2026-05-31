package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel
import com.example.havenhub.viewmodel.PropertyViewModel
import com.example.havenhub.viewmodel.SeasonalAlertViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController     : NavController,
    propertyId        : String,
    viewModel         : BookingViewModel      = hiltViewModel(),
    propertyViewModel : PropertyViewModel     = hiltViewModel(),
    authViewModel     : AuthViewModel         = hiltViewModel(),
    // Injected to display active seasonal alerts as contextual tips on the booking form.
    seasonalViewModel : SeasonalAlertViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val authUiState   by authViewModel.uiState.collectAsState()
    val propUiState   by propertyViewModel.uiState.collectAsState()
    val seasonalState by seasonalViewModel.uiState.collectAsState()

    val isDark = isSystemInDarkTheme()

    // ── Theme Colors ──────────────────────────────────────────────────────────
    val screenBg    = if (isDark) DarkBg           else Color(0xFFF5F7FA)
    val cardBg      = if (isDark) DarkSurface       else Color.White
    val navyColor   = if (isDark) DarkBgSecondary   else Color(0xFF0D1B3E)
    val goldColor   = if (isDark) DarkGoldPrimary   else Color(0xFFD4AF37)
    val textPrimary = if (isDark) DarkTextPrimary   else Color(0xFF0D1B3E)
    val hintColor   = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val categoryBg  = if (isDark) DarkBgElevated    else Color(0xFFF5F7FA)
    val dividerCol  = if (isDark) DarkBorder        else Color.White.copy(0.2f)

    // ── Auth State ────────────────────────────────────────────────────────────
    val currentUid  = authUiState.currentUser?.uid         ?: ""
    val currentName = authUiState.currentUser?.displayName ?: ""
    // Used to gate seasonal alert loading (admins don't book properties).
    val userRole    = authUiState.userRole.lowercase().trim()

    // ── Load Property Details ─────────────────────────────────────────────────
    LaunchedEffect(propertyId) {
        propertyViewModel.loadPropertyDetail(propertyId)
    }

    // ── Load Seasonal Alerts ──────────────────────────────────────────────────
    // Only load for tenant/landlord roles; admins manage alerts, not view them here.
    LaunchedEffect(userRole) {
        if (userRole.isNotEmpty() && userRole != "admin") {
            seasonalViewModel.loadAlertsForRole(userRole)
        }
    }

    val property = propUiState.propertyDetail

    // ── Form State ────────────────────────────────────────────────────────────
    var selectedDuration   by remember { mutableStateOf("Daily") }
    var guests             by remember { mutableIntStateOf(1) }
    var checkInDate        by remember { mutableStateOf("") }
    var checkOutDate       by remember { mutableStateOf("") }
    var showCheckInPicker  by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }

    // Double-tap guard — prevents duplicate booking creation on rapid taps.
    var isSubmitting by remember { mutableStateOf(false) }

    // ── Date Formatter ────────────────────────────────────────────────────────
    // Keyed on the application context so it survives recompositions without
    // recreating the formatter on every frame.
    val context = LocalContext.current
    val staticDateFormatter = remember(context) {
        val staticLocale = context.applicationContext.resources.configuration.locales[0]
        SimpleDateFormat("dd MMM yyyy", staticLocale)
    }

    // ── Auto-Calculate Nights from Date Selection ─────────────────────────────
    // Automatically derives the number of nights from the selected check-in /
    // check-out dates so the tenant doesn't have to enter nights manually.
    val calculatedNights = remember(checkInDate, checkOutDate, staticDateFormatter) {
        try {
            if (checkInDate.isNotEmpty() && checkOutDate.isNotEmpty()) {
                val dIn  = staticDateFormatter.parse(checkInDate)
                val dOut = staticDateFormatter.parse(checkOutDate)
                if (dIn != null && dOut != null && dOut.time > dIn.time) {
                    ((dOut.time - dIn.time) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                } else { 1 }
            } else { 1 }
        } catch (e: Exception) { 1 }
    }

    // ── Hybrid Continuous Price Calculation ───────────────────────────────────
    // For Weekly/Monthly packages: full package units are priced at the
    // discounted package rate; leftover nights revert to the base nightly rate.
    // This avoids the discontinuity where crossing a week/month boundary would
    // suddenly make the total cheaper.
    val totalAmount = remember(calculatedNights, selectedDuration, property) {
        val baseNightPrice = property?.pricePerNight ?: 0.0
        when (selectedDuration) {
            "Weekly" -> {
                val weeklyPrice = property?.pricePerWeek ?: (baseNightPrice * 7.0)
                val fullWeeks   = calculatedNights / 7
                val extraNights = calculatedNights % 7
                (fullWeeks * weeklyPrice) + (extraNights * baseNightPrice)
            }
            "Monthly" -> {
                val monthlyPrice = property?.pricePerMonth ?: (baseNightPrice * 30.0)
                val fullMonths  = calculatedNights / 30
                val extraNights = calculatedNights % 30
                (fullMonths * monthlyPrice) + (extraNights * baseNightPrice)
            }
            else -> baseNightPrice * calculatedNights
        }
    }

    // ── Navigation: Booking Success ───────────────────────────────────────────
    LaunchedEffect(uiState.actionSuccess, uiState.createdBookingId) {
        if (uiState.actionSuccess && !uiState.createdBookingId.isNullOrEmpty()) {
            isSubmitting = false
            navController.navigate(
                Screen.BookingConfirmation.createRoute(uiState.createdBookingId!!)
            ) {
                popUpTo(Screen.Booking.route) { inclusive = true }
            }
            viewModel.clearMessages()
        }
    }

    // Reset submitting guard when an error is returned from the ViewModel.
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) isSubmitting = false
    }

    // ── Check-In Date Picker ──────────────────────────────────────────────────
    if (showCheckInPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showCheckInPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        checkInDate = staticDateFormatter.format(Date(it))
                    }
                    showCheckInPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckInPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // ── Check-Out Date Picker ─────────────────────────────────────────────────
    // Pre-selects a sensible default based on the chosen duration type so the
    // picker opens closer to the expected check-out date.
    if (showCheckOutPicker) {
        val offsetMillis = when (selectedDuration) {
            "Weekly"  -> 86400000L * 7
            "Monthly" -> 86400000L * 30
            else      -> 86400000L          // 1 day default for Daily
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() + offsetMillis
        )
        DatePickerDialog(
            onDismissRequest = { showCheckOutPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        checkOutDate = staticDateFormatter.format(Date(it))
                    }
                    showCheckOutPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckOutPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────
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

                // ── Loading State ─────────────────────────────────────────────
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

                // ── Property Failed to Load ───────────────────────────────────
                property == null -> {
                    BookingBlockedCard(
                        message = "Property load nahi ho rahi. Wapas jao aur dobara try karo."
                    )
                }

                // ── Property Already Booked ───────────────────────────────────
                property.status.equals("BOOKED", ignoreCase = true) -> {
                    BookingBlockedCard(
                        message  = "Yeh property abhi already booked hai — phir baad mein try karein.",
                        isBooked = true
                    )
                }

                // ── Property Not Yet Approved by Admin ────────────────────────
                !property.status.equals(PropertyStatus.APPROVED.name, ignoreCase = true) -> {
                    BookingBlockedCard(
                        message = "Yeh property abhi admin se approve nahi hui — booking nahi ho sakti."
                    )
                }

                // ── Booking Form (Property APPROVED + Available) ──────────────
                else -> {

                    // ── 1. Property Summary Card ──────────────────────────────
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

                    // ── 2. Seasonal Alert Warning Card ────────────────────────
                    // Shows the first active seasonal alert as an informational
                    // tip above the booking form (e.g. "Eid holidays! Book early").
                    // Helps tenants make informed decisions during peak seasons.
                    val firstAlert = seasonalState.alerts.firstOrNull()
                    if (firstAlert != null) {
                        SeasonalBookingWarningCard(
                            emoji   = firstAlert.iconEmoji,
                            title   = firstAlert.title,
                            message = firstAlert.message,
                            isDark  = isDark
                        )
                    }

                    // ── 3. Duration Type Card ─────────────────────────────────
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

                    // ── 4. Date Selection Card ────────────────────────────────
                    // Nights are auto-calculated from the date diff (calculatedNights)
                    // so the tenant only needs to pick dates — no manual night entry.
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

                    // ── 5. Stay Details Card ──────────────────────────────────
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
                            Spacer(modifier = Modifier.height(14.dp))

                            // Guest counter — capped at property.maxGuests.
                            val atGuestMax = guests >= property.maxGuests

                            CounterRow(
                                label       = "Number of Guests",
                                count       = guests,
                                onDecrement = { if (guests > 1) guests-- },
                                onIncrement = { if (guests < property.maxGuests) guests++ },
                                isDark      = isDark,
                                atMax       = atGuestMax   // Disables "+" and greys out the button.
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Guest limit notice — turns red with a warning when limit is reached.
                            Text(
                                text = if (atGuestMax)
                                    "⚠️ Maximum limit reached! (${property.maxGuests} guests allowed)"
                                else
                                    "Max ${property.maxGuests} guests allowed",
                                fontSize   = 12.sp,
                                color      = if (atGuestMax) Color(0xFFB71C1C) else hintColor,
                                fontWeight = if (atGuestMax) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    // ── 6. Price Breakdown Card ───────────────────────────────
                    // Shows the hybrid continuous price: package rate for full
                    // weeks/months + base nightly rate for leftover nights.
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

                            // Label includes the active package type and auto-calculated nights.
                            PriceRow(
                                "${property.formattedPrice} ($selectedDuration Package) × $calculatedNights nights",
                                "PKR ${"%.0f".format(totalAmount)}"
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

                    // ── Error Message Card ────────────────────────────────────
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

                    // ── 7. Confirm Booking Button ─────────────────────────────
                    // Disabled until both dates are selected and no request is in flight.
                    val isFormComplete = checkInDate.isNotEmpty() && checkOutDate.isNotEmpty()

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
                                totalNights      = calculatedNights,  // Auto-derived from date diff.
                                guestCount       = guests,
                                paymentMethod    = "Pending",
                                checkInDate      = checkIn?.let  { com.google.firebase.Timestamp(it) },
                                checkOutDate     = checkOut?.let { com.google.firebase.Timestamp(it) }
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
                } // end else (approved property)
            } // end when
        } // end Column
    } // end Scaffold
}

// =============================================================================
// SEASONAL BOOKING WARNING CARD
//
// Displays the first active seasonal alert as an amber-toned informational tip
// inside the booking form, positioned between the Property Summary card and the
// Duration Type card. Amber is intentionally distinct from error-red and
// success-green so tenants read it as a helpful heads-up, not an error.
//
// There is no dismiss button — the card stays visible as a reminder throughout
// the booking process.
//
// Example messages:
//   "Eid holidays aa rahi hain — book early to avoid disappointment!"
//   "Summer peak season: properties fill up fast. Confirm your dates soon."
// =============================================================================
@Composable
private fun SeasonalBookingWarningCard(
    emoji  : String,
    title  : String,
    message: String,
    isDark : Boolean
) {
    val bgColor     = if (isDark) Color(0xFF1A1608)              else Color(0xFFFFFBEA)
    val borderColor = if (isDark) Color(0xFFB8962E).copy(0.5f)  else Color(0xFFD4AF37).copy(0.5f)
    val titleColor  = if (isDark) Color(0xFFF5D060)              else Color(0xFF4A3800)
    val msgColor    = if (isDark) Color(0xFFD4AF37).copy(0.75f)  else Color(0xFF5C4800).copy(0.85f)
    val accentColor = if (isDark) Color(0xFFD4AF37)              else Color(0xFF9B7D2E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left accent line
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )

        // Emoji / icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji.ifEmpty { "🎉" }, fontSize = 18.sp)
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Celebration,
                    contentDescription = null,
                    tint               = accentColor,
                    modifier           = Modifier.size(11.dp)
                )
                Text(
                    text       = "Seasonal Alert",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text       = title,
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp,
                color      = titleColor
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text       = message,
                fontSize   = 11.sp,
                color      = msgColor,
                lineHeight = 16.sp
            )
        }
    }
}

// =============================================================================
// BOOKING BLOCKED CARD
//
// Replaces the inline error Cards for the three "can't book" states:
//   • Property failed to load
//   • Property is already booked (amber tone)
//   • Property not yet approved by admin (red tone)
// =============================================================================
@Composable
private fun BookingBlockedCard(
    message : String,
    isBooked: Boolean = false    // true → amber (already booked), false → red (unavailable)
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
                imageVector        = if (isBooked) Icons.Default.Home else Icons.Default.Warning,
                contentDescription = null,
                tint               = textColor,
                modifier           = Modifier.size(24.dp)
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

// =============================================================================
// COUNTER ROW
//
// Generic increment / decrement row used for Guests counter.
// atMax = true → "+" button is greyed out and disabled (used for guest limit).
// =============================================================================
@Composable
private fun CounterRow(
    label      : String,
    count      : Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    isDark     : Boolean,
    atMax      : Boolean = false    // When true, disables the increment button.
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

            // Decrement button
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

            // Current count
            Text(
                text       = "$count",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 18.dp),
                color      = textColor
            )

            // Increment button — disabled and greyed when atMax is true.
            IconButton(
                onClick  = onIncrement,
                enabled  = !atMax,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (atMax) Color(0xFFBDC3C7) else plusBg)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint               = if (atMax) Color.White.copy(alpha = 0.5f) else plusIconTint,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =============================================================================
// PRICE ROW
// Simple label + value row used inside the Price Breakdown card.
// =============================================================================
@Composable
private fun PriceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(0.8f))
        Text(value, fontSize = 13.sp, color = Color.White)
    }
}