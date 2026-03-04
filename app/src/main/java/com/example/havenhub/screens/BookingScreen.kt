package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController: NavController,
    propertyId: String,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Success hone par back navigate karo
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearMessages()
        }
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
                    containerColor = PrimaryBlue,
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Order Summary Card ──────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Property ID: $propertyId", color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Amount: Rs. 12,000", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: Pending", color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Confirm & Pay Button ────────────────────────────
            Button(
                onClick = {
                    val booking = Booking(
                        propertyId = propertyId,
                        totalAmount = 12000.0,
                        status = BookingStatus.PENDING,
                        paymentStatus = PaymentStatus.PENDING
                    )
                    viewModel.createBooking(booking)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm & Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Error Message ───────────────────────────────────
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }
        }
    }
}