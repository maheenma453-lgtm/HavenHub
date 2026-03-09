package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.BookingViewModel

@Composable
fun BookingConfirmationScreen(
    navController : NavController,
    bookingId     : String,
    viewModel     : BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Get first booking for payment details ──────────────────────
    val booking = uiState.bookings.firstOrNull()

    // ── Animated checkmark (pop-in effect) ────────────────────────
    val scale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "confirmScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(60.dp))

        // ── Animated Success Circle ────────────────────────────────
        Box(
            modifier = Modifier
                .scale(scale)
                .size(110.dp)
                .clip(CircleShape)
                .background(SuccessGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = "Confirmed",
                tint               = BackgroundWhite,
                modifier           = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Success Text ───────────────────────────────────────────
        Text(
            text       = "Booking Confirmed!",
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text       = "Your property has been booked successfully.\nThe owner has been notified.",
            fontSize   = 14.sp,
            color      = TextSecondary,
            textAlign  = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Booking Summary Card ───────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = SurfaceVariantLight),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text("Booking Details", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                BookingDetailRow(label = "Booking ID", value = "#${bookingId.take(8).uppercase()}")
                BookingDetailRow(label = "Property ID", value = booking?.propertyId ?: "-")
                BookingDetailRow(label = "Status",      value = "Pending")
                BookingDetailRow(label = "Payment",     value = "Pending")

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = BorderGray
                )

                // Total amount
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text       = "PKR ${booking?.totalAmount?.toInt() ?: 12000}",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment status badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningOrange.copy(alpha = 0.12f))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "⏳ Payment Pending",
                        fontSize   = 13.sp,
                        color      = WarningOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Action Buttons ─────────────────────────────────────────
        Button(
            onClick = {
                // ✅ FIX: Screen.Payment.createRoute() needs 6 arguments
                navController.navigate(
                    Screen.Payment.createRoute(
                        bookingId = bookingId,
                        payerId   = booking?.tenantId     ?: "",
                        payeeId   = booking?.landlordId   ?: "",
                        payerName = booking?.tenantName   ?: "User",
                        payeeName = booking?.landlordName ?: "Owner",
                        amount    = booking?.totalAmount  ?: 12000.0
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Pay Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                navController.navigate(Screen.MyBookings.route) {
                    popUpTo(Screen.Home.route)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Pay Later – View Bookings", fontSize = 14.sp, color = PrimaryBlue)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Error Message ──────────────────────────────────────────
        uiState.errorMessage?.let { error ->
            Text(text = error, color = ErrorRed, fontSize = 14.sp)
        }
    }
}

// ── Single booking detail row ──────────────────────────────────────
@Composable
private fun BookingDetailRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value,  fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
