package com.example.havenhub.screens

import androidx.compose.foundation.background
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
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    navController: NavController,
    bookingId: String,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val booking = uiState.bookings.firstOrNull { it.bookingId == bookingId }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }

        if (booking == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Booking not found.", color = TextSecondary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Status Badge
            StatusBadge(status = booking.status.displayName())

            // Property Info
            SectionCard(title = "Property") {
                InfoRow(label = "Title",   value = booking.propertyTitle)
                InfoRow(label = "Address", value = booking.propertyAddress)
            }

            // Stay Details
            SectionCard(title = "Stay Details") {
                InfoRow(label = "Check-In",  value = booking.checkInDate?.toDate()?.toString() ?: "-")
                InfoRow(label = "Check-Out", value = booking.checkOutDate?.toDate()?.toString() ?: "-")
                InfoRow(label = "Guests",    value = "${booking.guestCount} Guest(s)")
                InfoRow(label = "Nights",    value = "${booking.totalNights} Night(s)")
            }

            // Payment Summary
            SectionCard(title = "Payment Summary") {
                InfoRow(label = "Price/Night",     value = "PKR ${booking.pricePerNight.toInt()}")
                InfoRow(label = "Subtotal",        value = "PKR ${booking.subtotal.toInt()}")
                InfoRow(label = "Service Fee",     value = "PKR ${booking.serviceFee.toInt()}")
                InfoRow(label = "Security Deposit",value = "PKR ${booking.securityDeposit.toInt()}")
                InfoRow(label = "Payment Status",  value = booking.paymentStatus.name)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderGray)
                InfoRow(
                    label      = "Total Amount",
                    value      = booking.formattedTotal,
                    valueColor = PrimaryBlue,
                    bold       = true
                )
            }

            // Host Info
            SectionCard(title = "Host Information") {
                InfoRow(label = "Host", value = booking.landlordName)
            }

            // Booking Info
            SectionCard(title = "Booking Info") {
                InfoRow(label = "Booking ID", value = "#${booking.bookingId.take(8).uppercase()}")
                InfoRow(label = "Tenant",     value = booking.tenantName)
                InfoRow(label = "Booked On",  value = booking.createdAt?.toDate()?.toString() ?: "-")
            }

            // Action Buttons
            if (booking.isCancellable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = { /* Contact host logic */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Contact Host")
                    }
                    Button(
                        onClick  = { /* viewModel.cancelBooking(bookingId) */ },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancel")
                    }
                }
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Text(text = error, color = ErrorRed, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Confirmed", "Checked In", "Completed" -> Color(0xFFE8F5E9) to SuccessGreen
        "Pending"   -> Color(0xFFFFF8E1) to WarningOrange
        "Cancelled" -> Color(0xFFFFEBEE) to ErrorRed
        else        -> SurfaceVariantLight to TextSecondary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (status) {
                "Confirmed", "Checked In", "Completed" -> Icons.Default.CheckCircle
                "Cancelled" -> Icons.Default.Cancel
                else        -> Icons.Default.HourglassEmpty
            },
            contentDescription = null,
            tint     = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text       = "Status: $status",
            color      = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp
        )
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(
    label      : String,
    value      : String,
    valueColor : Color = TextPrimary,
    bold       : Boolean = false
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(
            text       = value,
            color      = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize   = 14.sp
        )
    }
}