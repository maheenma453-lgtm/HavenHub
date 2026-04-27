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
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel
import com.example.havenhub.viewmodel.HomeViewModel

@Composable
fun BookingConfirmationScreen(
    navController: NavController,
    bookingId    : String,
    viewModel    : BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.loadBookingById(bookingId)
    }

    val booking = uiState.currentBooking

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

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                color           = Color.White,
                shadowElevation = 8.dp
            ) {
                // ── Sirf View My Bookings — Pay Now hata diya ────
                Button(
                    onClick = {
                        navController.navigate(Screen.MyBookings.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(56.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D1B3E),
                        contentColor   = Color.White
                    )
                ) {
                    Text(
                        "View My Bookings",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header with checkmark animation ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D1B3E).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Animated checkmark circle
                    Box(
                        modifier = Modifier
                            .scale(scale.value)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(50.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text       = "Booking Submitted!",
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF0D1B3E)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = "Your request is being processed",
                        fontSize = 15.sp,
                        color    = Color(0xFF64748B)
                    )
                }
            }

            // ── Booking Details Card ──────────────────────────────
            if (uiState.isLoading) {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0D1B3E))
                }
            } else {
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape     = RoundedCornerShape(24.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Header row
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "Booking Details",
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF1E293B)
                            )
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "#${bookingId.take(8).uppercase()}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        DetailRow(label = "Property", value = booking?.propertyTitle ?: "-")
                        DetailRow(label = "Location", value = booking?.propertyAddress ?: "-", isMultiLine = true)
                        DetailRow(label = "Tenant",   value = booking?.tenantName ?: "-")

                        StatusBadgeRow(
                            label      = "Status",
                            status     = booking?.bookingStatus?.displayName() ?: "Pending",
                            badgeColor = Color(0xFFF59E0B)
                        )

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        Spacer(Modifier.height(16.dp))

                        // Total amount
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Amount",
                                fontSize = 15.sp,
                                color    = Color(0xFF64748B)
                            )
                            Text(
                                text       = booking?.formattedTotal ?: "PKR 0",
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color(0xFF0D1B3E)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        Spacer(Modifier.height(12.dp))

                        // Info note
                        Text(
                            text      = "💡 You can pay from My Bookings → Pending tab",
                            fontSize  = 12.sp,
                            color     = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            uiState.errorMessage?.let { error ->
                Text(
                    text      = error,
                    color     = Color.Red,
                    fontSize  = 13.sp,
                    modifier  = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Reusable components
// ─────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(
    label      : String,
    value      : String,
    isMultiLine: Boolean = false
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color    = Color(0xFF94A3B8),
            modifier = Modifier.weight(1f)
        )
        Text(
            text       = value,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF334155),
            textAlign  = TextAlign.End,
            modifier   = Modifier.weight(1.5f),
            maxLines   = if (isMultiLine) 2 else 1
        )
    }
}

@Composable
private fun StatusBadgeRow(
    label     : String,
    status    : String,
    badgeColor: Color
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF94A3B8))
        Surface(
            color = badgeColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text     = status,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = badgeColor
            )
        }
    }
}