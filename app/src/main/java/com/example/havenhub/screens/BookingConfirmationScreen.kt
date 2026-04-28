package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel

@Composable
fun BookingConfirmationScreen(
    navController: NavController,
    bookingId    : String,
    viewModel    : BookingViewModel = hiltViewModel(),
    authViewModel: AuthViewModel    = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val booking   = uiState.currentBooking

    LaunchedEffect(bookingId) {
        viewModel.loadBookingById(bookingId)
    }

    // ── Bounce animation ──────────────────────────────────────
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

    // ── Detect if this is post-payment (status = CONFIRMED) ──
    val isPaymentDone = booking?.bookingStatus == BookingStatus.CONFIRMED ||
            booking?.paymentStatusEnum?.displayName() == "Paid"

    val statusColor = if (isPaymentDone) Color(0xFF22C55E) else Color(0xFFF59E0B)
    val statusText  = if (isPaymentDone) "Confirmed" else "Pending"
    val headerTitle = if (isPaymentDone) "Payment Successful!" else "Booking Submitted!"
    val headerSub   = if (isPaymentDone)
        "Your booking is confirmed & ready"
    else
        "Waiting for landlord confirmation"

    val userId = authState.currentUser?.uid ?: ""

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                color           = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ✅ View My Bookings — fresh load karke Confirmed tab pe le jao
                    Button(
                        onClick = {
                            // Fresh bookings load karo pehle
                            viewModel.loadBookings(
                                userId = userId,
                                role   = authState.userRole.ifEmpty { "tenant" }
                            )
                            navController.navigate(Screen.MyBookings.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D1B3E),
                            contentColor   = Color.White
                        )
                    ) {
                        Text(
                            if (isPaymentDone) "View Confirmed Booking" else "View My Bookings",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ✅ Pay Now — sirf jab payment pending ho
                    if (!isPaymentDone) {
                        OutlinedButton(
                            onClick  = {
                                booking?.let {
                                    navController.navigate(
                                        Screen.Payment.createRoute(
                                            bookingId = it.bookingId,
                                            payerId   = it.tenantId,
                                            payeeId   = it.landlordId,
                                            payerName = it.tenantName,
                                            payeeName = it.landlordName,
                                            amount    = it.totalAmount
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF0D1B3E)
                            )
                        ) {
                            Text("Pay Now", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0D1B3E).copy(0.08f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .scale(scale.value)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (isPaymentDone) Color(0xFF22C55E) else Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(headerTitle, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1B3E))
                    Spacer(Modifier.height(4.dp))
                    Text(headerSub, fontSize = 14.sp, color = Color(0xFF64748B))
                }
            }

            // ── Details Card ─────────────────────────────────────
            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0D1B3E))
                }
            } else {
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape     = RoundedCornerShape(24.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.5.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        // Header row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("Booking Details", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "#${bookingId.take(8).uppercase()}",
                                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        BCDetailRow("Property", booking?.propertyTitle ?: "-")
                        BCDetailRow("Location", booking?.propertyAddress ?: "-", isMultiLine = true)
                        BCDetailRow("Tenant",   booking?.tenantName ?: "-")

                        // ✅ Status badge — dynamic color
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("Status", fontSize = 14.sp, color = Color(0xFF94A3B8))
                            Surface(
                                color = statusColor.copy(0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    statusText,
                                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = statusColor
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(Modifier.height(12.dp))

                        // Total
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("Total Amount", fontSize = 15.sp, color = Color(0xFF64748B))
                            Text(
                                booking?.formattedTotal ?: "PKR 0",
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color(0xFF0D1B3E)
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(Modifier.height(10.dp))

                        // Hint note
                        Text(
                            if (isPaymentDone)
                                "✅ Payment received. Check Confirmed tab in My Bookings."
                            else
                                "💡 Pay from My Bookings → Pending tab, or tap Pay Now below.",
                            fontSize  = 12.sp,
                            color     = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = Color.Red, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Private composables ───────────────────────────────────────
@Composable
private fun BCDetailRow(label: String, value: String, isMultiLine: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF334155),
            textAlign  = TextAlign.End,
            modifier   = Modifier.weight(1.5f),
            maxLines   = if (isMultiLine) 2 else 1
        )
    }
}