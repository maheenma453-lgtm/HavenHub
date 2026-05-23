package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.PaymentViewModel
import java.text.SimpleDateFormat
import java.util.*

// Semantic colors — intentional
private val PSGreen = Color(0xFF22C55E)
private val PSAmber = Color(0xFFD97706)
private val PSBlue  = Color(0xFF3B82F6) // PULL CHANGE: New color for deposit-paid state

private fun formatPayDate(date: Date?): String {
    if (date == null) return "—"
    return try {
        SimpleDateFormat("EEE, dd MMM yyyy · hh:mm a", Locale.getDefault()).format(date)
    } catch (e: Exception) { "—" }
}

@Composable
fun PaymentSuccessScreen(
    navController: NavController,
    bookingId    : String,
    viewModel    : PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.verifyPaymentStatus(bookingId)
    }

    // PULL CHANGE: Track isPreBooking to show different UI for deposit vs normal payment
    var isPreBooking by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isPreBooking) {
        isPreBooking = uiState.isPreBooking
    }

    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue   = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessLow
            )
        )
    }

    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val error            = MaterialTheme.colorScheme.error

    Scaffold(containerColor = background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Animated success circle ───────────────────────────
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(PSGreen.copy(0.25f), PSGreen.copy(0.08f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier         = Modifier.size(90.dp).clip(CircleShape).background(PSGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // PULL CHANGE: Title changes based on whether it was a deposit or full payment
            Text(
                if (isPreBooking) "Deposit Paid!" else "Payment Successful!",
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = onSurface
            )
            Spacer(Modifier.height(6.dp))
            // PULL CHANGE: Subtitle changes based on payment type
            Text(
                if (isPreBooking)
                    "Tumhara 20% deposit receive ho gaya!\nBaaki 80% arrival pe pay karna hoga."
                else
                    "Payment receive ho gayi!\nLandlord ki approval ka intezaar karo.",
                fontSize   = 14.sp,
                color      = onSurfaceVariant,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                uiState.payment?.formattedAmount ?: "—",
                fontSize   = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = tertiary
            )

            Spacer(Modifier.height(16.dp))

            // PULL CHANGE: Status strip is different for preBooking vs normal payment
            if (isPreBooking) {
                // Deposit paid — show blue "Booking Secured" strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PSBlue.copy(0.10f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = PSBlue, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Deposit Paid — Booking Secured", fontWeight = FontWeight.Bold, color = PSBlue, fontSize = 13.sp)
                        Text("Remaining amount check-in pe pay karein.", color = PSBlue.copy(0.75f), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            } else {
                // Normal payment — show amber "Awaiting Approval" strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PSAmber.copy(0.10f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.HourglassEmpty, null, tint = PSAmber, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Awaiting Landlord Approval", fontWeight = FontWeight.Bold, color = PSAmber, fontSize = 13.sp)
                        Text("Landlord approve kare ga tab booking confirmed hogi.", color = PSAmber.copy(0.75f), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Transaction Details Card ──────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(tertiary.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, null, tint = tertiary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Transaction Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurface)
                    }

                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Brush.horizontalGradient(listOf(tertiary.copy(0.4f), Color.Transparent)))
                    )
                    Spacer(Modifier.height(14.dp))

                    PSDetailRow("Booking ID",     "#${bookingId.take(8).uppercase()}",                        onSurface, onSurfaceVariant)
                    PSDetailRow("Transaction ID", uiState.payment?.gatewayTransactionId?.ifEmpty { "—" } ?: "—", onSurface, onSurfaceVariant)
                    PSDetailRow("Date",           formatPayDate(uiState.payment?.createdAt?.toDate()),        onSurface, onSurfaceVariant)
                    PSDetailRow("Method",         uiState.payment?.paymentMethodEnum?.displayName() ?: "—",  onSurface, onSurfaceVariant)
                    // PULL CHANGE: Show payment type row only for pre-booking (deposit)
                    if (isPreBooking) {
                        PSDetailRow("Type", "Advance Deposit (20%)", onSurface, onSurfaceVariant)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Payment status — always green "Paid"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Payment", color = onSurfaceVariant, fontSize = 13.sp)
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(PSGreen.copy(0.12f)).padding(horizontal = 14.dp, vertical = 5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = PSGreen, modifier = Modifier.size(12.dp))
                                Text("Paid", color = PSGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // PULL CHANGE: Booking status badge — blue for deposit, amber for awaiting approval
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Booking", color = onSurfaceVariant, fontSize = 13.sp)
                        if (isPreBooking) {
                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(PSBlue.copy(0.12f)).padding(horizontal = 14.dp, vertical = 5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Default.AccountBalanceWallet, null, tint = PSBlue, modifier = Modifier.size(12.dp))
                                    Text("Deposit Paid", color = PSBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(PSAmber.copy(0.12f)).padding(horizontal = 14.dp, vertical = 5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Default.HourglassEmpty, null, tint = PSAmber, modifier = Modifier.size(12.dp))
                                    Text("Awaiting Approval", color = PSAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // PULL CHANGE: Tab navigation is different:
            // PreBooking deposit → tab=3 (Deposit Paid tab)
            // Normal payment    → tab=1 (Awaiting Approval tab)
            Button(
                onClick = {
                    val tab = if (isPreBooking) 3 else 1
                    navController.navigate("my_bookings?tab=$tab") {
                        popUpTo("my_bookings?tab={tab}") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = primary, contentColor = tertiary)
            ) {
                Icon(Icons.Default.BookmarkBorder, null, tint = tertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                // PULL CHANGE: Button text is also dynamic
                Text(
                    if (isPreBooking) "View Deposit Booking" else "View My Booking",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(16.dp),
                border   = androidx.compose.foundation.BorderStroke(1.5.dp, primary),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = primary)
            ) {
                Icon(Icons.Default.Home, null, tint = primary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to Home", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = primary)
            }

            uiState.errorMessage?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(err, color = error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PSDetailRow(label: String, value: String, onSurface: Color, onSurfaceVariant: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = onSurfaceVariant, fontSize = 13.sp)
        Text(
            value,
            color = onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}