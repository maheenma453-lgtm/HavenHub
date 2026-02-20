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
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.BookingViewModel

// ─────────────────────────────────────────────────────────────────
// BookingConfirmationScreen.kt
// PURPOSE : Success screen shown after booking is created.
//           Displays booking summary, ID, and next steps.
//           User can go to payment or view bookings.
// PARAMETERS: bookingId (from navigation)
// NAVIGATION:
//   → PaymentScreen (to pay now)
//   → MyBookingsScreen (view all bookings)
// ─────────────────────────────────────────────────────────────────

@Composable
fun BookingConfirmationScreen(
    navController : NavController,
    bookingId     : String,
    viewModel     : BookingViewModel = hiltViewModel()
) {

    // ── Load booking details ───────────────────────────────────────
    LaunchedEffect(bookingId) {
        viewModel.loadBookingDetails(bookingId)
    }

    val booking by viewModel.currentBooking.collectAsState()

    // ── Animated checkmark (pop-in effect) ────────────────────────
    val scale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "confirmScale"
    )

    // ── UI ─────────────────────────────────────────────────────────
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
            text      = "Your property has been booked successfully.\nThe owner has been notified.",
            fontSize  = 14.sp,
            color     = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Booking Summary Card ───────────────────────────────────
        booking?.let { b ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = SurfaceGray),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text("Booking Details", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Booking ID
                    BookingDetailRow(
                        label = "Booking ID",
                        value = "#${b.id.take(8).uppercase()}"
                    )
                    BookingDetailRow(label = "Property",    value = b.propertyTitle)
                    BookingDetailRow(label = "Location",    value = b.propertyCity)
                    BookingDetailRow(label = "Package",     value = b.rentalPackage)
                    BookingDetailRow(label = "Check-In",    value = b.checkInDate)
                    BookingDetailRow(label = "Check-Out",   value = b.checkOutDate)
                    BookingDetailRow(label = "Guests",      value = "${b.guestCount}")

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderGray)

                    // Total amount
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text       = "PKR ${b.totalPrice}",
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
                            .background(WarningAmber.copy(alpha = 0.12f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = "⏳ Payment Pending",
                            fontSize   = 13.sp,
                            color      = WarningAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Action Buttons ─────────────────────────────────────────

        // Pay Now → goes to payment
        Button(
            onClick = { navController.navigate(Screen.Payment.createRoute(bookingId)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Pay Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pay Later → view bookings
        OutlinedButton(
            onClick = {
                navController.navigate(Screen.MyBookings.route) {
                    // Clear back stack so user starts fresh
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
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}


