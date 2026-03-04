package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSuccessScreen(
    navController: NavController,
    bookingId: String,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Payment details fetch karo
    LaunchedEffect(bookingId) {
        viewModel.verifyPaymentStatus(bookingId)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Success Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint     = SuccessGreen,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text("Payment Successful!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))

            // Real amount from viewModel
            Text(
                text       = uiState.payment?.formattedAmount ?: "-",
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                color      = PrimaryBlue
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text       = "Your payment has been processed successfully. Your booking is now confirmed!",
                fontSize   = 14.sp,
                color      = TextSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(32.dp))

            // Transaction Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PayRow(
                        label = "Transaction ID",
                        value = uiState.payment?.gatewayTransactionId?.ifEmpty { "-" } ?: "-"
                    )
                    PayRow(
                        label = "Date & Time",
                        value = uiState.payment?.createdAt?.toDate()?.toString() ?: "-"
                    )
                    PayRow(
                        label = "Method",
                        value = uiState.payment?.paymentMethod?.displayName() ?: "-"
                    )
                    PayRow(
                        label = "Status",
                        value = uiState.payment?.status?.displayName() ?: "-"
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // View Booking Button
            Button(
                onClick  = { navController.navigate(Screen.BookingDetails.createRoute(bookingId)) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("View Booking", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // Back to Home Button
            OutlinedButton(
                onClick  = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Home", fontSize = 15.sp, color = PrimaryBlue)
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(text = error, color = ErrorRed, fontSize = 14.sp)
            }
        }
    }
}