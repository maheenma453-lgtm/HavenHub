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
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

private val PSNavy   = Color(0xFF0D1B3E)
private val PSGold   = Color(0xFFD4AF37)
private val PSBg     = Color(0xFFF5F7FA)
private val PSMuted  = Color(0xFF8899AA)
private val PSGreen  = Color(0xFF22C55E)
private val PSAmber  = Color(0xFFD97706)   // ✅ NEW: awaiting approval color

private fun formatPayDate(date: Date?): String {
    if (date == null) return "—"
    return try {
        SimpleDateFormat("EEE, dd MMM yyyy · hh:mm a", Locale.getDefault()).format(date)
    } catch (e: Exception) { "—" }
}

@Composable
fun PaymentSuccessScreen(
    navController : NavController,
    bookingId     : String,
    viewModel     : PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.verifyPaymentStatus(bookingId)
    }

    // Bounce animation for checkmark
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

    Scaffold(containerColor = PSBg) { padding ->
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
                        Brush.radialGradient(
                            listOf(PSGreen.copy(0.25f), PSGreen.copy(0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(PSGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check, null,
                        tint     = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Payment Successful!",
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = PSNavy
            )

            Spacer(Modifier.height(6.dp))

            // ✅ FIX: "Your booking is confirmed" → correct message
            Text(
                "Payment receive ho gayi!\nLandlord ki approval ka intezaar karo.",
                fontSize  = 14.sp,
                color     = PSMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(8.dp))

            // Amount
            Text(
                uiState.payment?.formattedAmount ?: "—",
                fontSize   = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = PSGold
            )

            Spacer(Modifier.height(16.dp))

            // ✅ NEW: Awaiting Approval info strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PSAmber.copy(0.10f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.HourglassEmpty, null,
                    tint     = PSAmber,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        "Awaiting Landlord Approval",
                        fontWeight = FontWeight.Bold,
                        color      = PSAmber,
                        fontSize   = 13.sp
                    )
                    Text(
                        "Landlord approve kare ga tab booking confirmed hogi.",
                        color    = PSAmber.copy(0.75f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Transaction Details Card ──────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PSGold.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Receipt, null,
                                tint     = PSGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Transaction Details",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            color      = PSNavy
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Gold divider
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PSGold.copy(0.4f), Color.Transparent)
                                )
                            )
                    )

                    Spacer(Modifier.height(14.dp))

                    SuccessDetailRow(
                        "Booking ID",
                        "#${bookingId.take(8).uppercase()}"
                    )
                    SuccessDetailRow(
                        "Transaction ID",
                        uiState.payment?.gatewayTransactionId?.ifEmpty { "—" } ?: "—"
                    )
                    SuccessDetailRow(
                        "Date",
                        formatPayDate(uiState.payment?.createdAt?.toDate())
                    )
                    SuccessDetailRow(
                        "Method",
                        uiState.payment?.paymentMethodEnum?.displayName() ?: "—"
                    )

                    Spacer(Modifier.height(8.dp))

                    // ✅ FIX: Status chip — "Awaiting Approval" dikhao, "Paid" nahi
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Payment", color = PSMuted, fontSize = 13.sp)
                        // Payment status: PAID (paise aa gaye)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PSGreen.copy(0.12f))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint     = PSGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "Paid",
                                    color      = PSGreen,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // ✅ NEW: Booking status row alag dikhao
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Booking", color = PSMuted, fontSize = 13.sp)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PSAmber.copy(0.12f))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.HourglassEmpty, null,
                                    tint     = PSAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "Awaiting Approval",
                                    color      = PSAmber,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── View Booking button ───────────────────────────────
            Button(
                onClick = {
                    navController.navigate("my_bookings?tab=1") {
                        popUpTo("my_bookings?tab={tab}") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PSNavy,
                    contentColor   = PSGold
                )
            ) {
                Icon(
                    Icons.Default.BookmarkBorder, null,
                    tint     = PSGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "View My Booking",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Back to Home
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape  = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PSNavy),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PSNavy)
            ) {
                Icon(
                    Icons.Default.Home, null,
                    tint     = PSNavy,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Back to Home",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = PSNavy
                )
            }

            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(error, color = Color(0xFFEF4444), fontSize = 13.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SuccessDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(label, color = PSMuted, fontSize = 13.sp)
        Text(
            value,
            color      = PSNavy,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.End,
            modifier   = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
    }
}
