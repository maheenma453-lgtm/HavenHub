package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    navController: NavController,
    propertyId: String,
    viewModel: BookingViewModel = hiltViewModel(),
    propertyViewModel: PropertyViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val propUiState by propertyViewModel.uiState.collectAsState()

    val currentUid = authUiState.currentUser?.uid ?: ""
    val currentName = authUiState.currentUser?.displayName ?: ""

    LaunchedEffect(propertyId) {
        propertyViewModel.loadPropertyDetail(propertyId)
    }

    val property = propUiState.propertyDetail

    var selectedDuration by remember { mutableStateOf("Daily") }
    var nights by remember { mutableIntStateOf(1) }
    var guests by remember { mutableIntStateOf(1) }
    var checkInDate by remember { mutableStateOf("") }
    var checkOutDate by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("JazzCash") }
    var showCheckInPicker by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }

    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val totalAmount = remember(nights, selectedDuration, property) {
        when (selectedDuration) {
            "Weekly" -> (property?.pricePerWeek ?: (property?.pricePerNight?.times(7) ?: 0.0)) * (nights / 7).coerceAtLeast(1)
            "Monthly" -> (property?.pricePerMonth ?: (property?.pricePerNight?.times(30) ?: 0.0)) * (nights / 30).coerceAtLeast(1)
            else -> (property?.pricePerNight ?: 0.0) * nights
        }
    }

    LaunchedEffect(uiState.actionSuccess, uiState.createdBookingId) {
        if (uiState.actionSuccess && !uiState.createdBookingId.isNullOrEmpty()) {
            navController.navigate(
                Screen.BookingConfirmation.createRoute(uiState.createdBookingId!!)
            ) {
                popUpTo(Screen.Booking.route) { inclusive = true }
            }
            viewModel.clearMessages()
        }
    }

    if (showCheckInPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showCheckInPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { checkInDate = dateFormatter.format(Date(it)) }
                    showCheckInPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCheckInPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showCheckOutPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() + 86400000L)
        DatePickerDialog(
            onDismissRequest = { showCheckOutPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { checkOutDate = dateFormatter.format(Date(it)) }
                    showCheckOutPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCheckOutPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Booking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B3E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F7FA))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                propUiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0D1B3E))
                    }
                }

                property == null -> {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), modifier = Modifier.fillMaxWidth()) {
                        Text("Property load nahi ho rahi. Wapas jao aur dobara try karo.", color = Color(0xFFB71C1C), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                    }
                }

                property.status != "APPROVED" -> {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), modifier = Modifier.fillMaxWidth()) {
                        Text("Yeh property abhi admin se approve nahi hui — booking nahi ho sakti.", color = Color(0xFFB71C1C), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                    }
                }

                else -> {
                    // ── Property Summary ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Property", fontSize = 13.sp, color = Color(0xFF8899AA))
                            Text(property.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(14.dp))
                                Text(" ${property.city}", fontSize = 13.sp, color = Color(0xFF8899AA))
                                Spacer(modifier = Modifier.weight(1f))
                                Text(property.formattedPrice + "/night", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                            }
                        }
                    }

                    // ── Duration Type ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Rental Duration Type", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Daily", "Weekly", "Monthly").forEach { duration ->
                                    val isSelected = selectedDuration == duration
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF0D1B3E) else Color(0xFFF5F7FA))
                                            .clickable { selectedDuration = duration }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(duration, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF8899AA))
                                    }
                                }
                            }
                        }
                    }

                    // ── Dates ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Select Dates", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { showCheckInPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Check-in", fontSize = 11.sp, color = Color(0xFF8899AA))
                                        Text(checkInDate.ifEmpty { "Select" }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B3E))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showCheckOutPicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Check-out", fontSize = 11.sp, color = Color(0xFF8899AA))
                                        Text(checkOutDate.ifEmpty { "Select" }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B3E))
                                    }
                                }
                            }
                        }
                    }

                    // ── Nights & Guests ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Stay Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Number of Nights", fontSize = 14.sp, color = Color(0xFF0D1B3E))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (nights > 1) nights-- },
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FA))
                                    ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                                    Text("$nights", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF0D1B3E))
                                    IconButton(
                                        onClick = { nights++ },
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FA))
                                    ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Number of Guests", fontSize = 14.sp, color = Color(0xFF0D1B3E))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (guests > 1) guests-- },
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FA))
                                    ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                                    Text("$guests", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF0D1B3E))
                                    IconButton(
                                        onClick = { if (guests < property.maxGuests) guests++ },
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FA))
                                    ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                                }
                            }
                            Text("Max ${property.maxGuests} guests allowed", fontSize = 12.sp, color = Color(0xFF8899AA), modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    // ── Payment Method ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Payment Method", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                            Spacer(modifier = Modifier.height(12.dp))
                            listOf("JazzCash" to "📱", "EasyPaisa" to "💚", "Bank Transfer" to "🏦", "Cash on Arrival" to "💵").forEach { (method, icon) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedPayment == method) Color(0xFF0D1B3E).copy(0.05f) else Color.Transparent)
                                        .clickable { selectedPayment = method }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(method, fontSize = 14.sp, color = Color(0xFF0D1B3E), modifier = Modifier.weight(1f))
                                    RadioButton(
                                        selected = selectedPayment == method,
                                        onClick = { selectedPayment = method },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0D1B3E))
                                    )
                                }
                            }
                        }
                    }

                    // ── Price Breakdown ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B3E)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Price Breakdown", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            PriceRow("${property.formattedPrice} × $nights nights", "PKR ${"%.0f".format(property.pricePerNight * nights)}")
                            if (property.securityDeposit > 0) {
                                PriceRow("Security Deposit", "PKR ${"%.0f".format(property.securityDeposit)}")
                            }
                            HorizontalDivider(color = Color.White.copy(0.2f), modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                                Text("PKR ${"%.0f".format(totalAmount + property.securityDeposit)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                            }
                        }
                    }

                    // ── Error ──
                    uiState.errorMessage?.let { error ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), modifier = Modifier.fillMaxWidth()) {
                            Text(error, color = Color(0xFFB71C1C), fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                        }
                    }

                    // ── Confirm Button ──
                    Button(
                        onClick = {
                            val booking = Booking(
                                propertyId = propertyId,
                                propertyTitle = property.title,
                                landlordId = property.ownerId,
                                landlordName = property.ownerName,
                                tenantId = currentUid,
                                tenantName = currentName,
                                pricePerNight = property.pricePerNight,
                                totalAmount = totalAmount,
                                subtotal = totalAmount,
                                securityDeposit = property.securityDeposit,
                                status = BookingStatus.PENDING.name,
                                paymentStatus = PaymentStatus.PENDING.name,
                                propertyAddress = "${property.address}, ${property.city}",
                                propertyCoverUrl = property.coverImageUrl,
                                totalNights = nights,
                                guestCount = guests,
                                paymentMethod = selectedPayment
                            )
                            viewModel.createBooking(booking)
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = !uiState.isLoading && checkInDate.isNotEmpty() && checkOutDate.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B3E))
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Confirm Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Text(
                        "By confirming, you agree to the cancellation policy",
                        fontSize = 11.sp,
                        color = Color(0xFF8899AA),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(0.8f))
        Text(value, fontSize = 13.sp, color = Color.White)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (bold) Color(0xFF0D1B3E) else TextPrimary)
    }
}