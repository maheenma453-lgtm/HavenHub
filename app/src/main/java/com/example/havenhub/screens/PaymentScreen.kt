package com.example.havenhub.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.PaymentMethod
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    bookingId: String,
    payerId: String,
    payeeId: String,
    payerName: String,
    payeeName: String,
    amount: Double,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Success hone par BookingConfirmation pe navigate karo
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.navigate("booking_confirmation/$bookingId") {
                popUpTo("payment/$bookingId") { inclusive = true }
            }
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Order Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                    HorizontalDivider(color = BorderGray)
                    PayRow("Booking ID", "#${bookingId.take(8).uppercase()}")
                    PayRow("Amount", "PKR ${amount.toInt()}")
                    HorizontalDivider(color = BorderGray)
                    PayRow("Total", "PKR ${amount.toInt()}", bold = true, highlight = true)
                }
            }

            // Payment Method Selection
            Text("Payment Method", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)

            listOf(
                PaymentMethod.JAZZCASH,
                PaymentMethod.EASYPAISA,
                PaymentMethod.CREDIT_CARD,
                PaymentMethod.BANK_TRANSFER
            ).forEach { method ->
                OutlinedCard(
                    onClick  = { viewModel.selectPaymentMethod(method) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedMethod == method,
                            onClick  = { viewModel.selectPaymentMethod(method) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = when (method) {
                                PaymentMethod.CREDIT_CARD,
                                PaymentMethod.DEBIT_CARD  -> Icons.Default.CreditCard
                                PaymentMethod.BANK_TRANSFER -> Icons.Default.AccountBalance
                                else -> Icons.Default.Payment
                            },
                            contentDescription = null,
                            tint     = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(method.displayName(), fontSize = 14.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Pay Button
            Button(
                onClick = {
                    uiState.selectedMethod?.let { method ->
                        viewModel.processPayment(
                            bookingId = bookingId,
                            payerId   = payerId,
                            payeeId   = payeeId,
                            payerName = payerName,
                            payeeName = payeeName,
                            amount    = amount,
                            method    = method
                        )
                    } ?: run {
                        // Method select nahi hua
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape   = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading && uiState.selectedMethod != null,
                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Pay PKR ${amount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Text(text = error, color = ErrorRed, fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Text(
                "Your payment is secured and encrypted.",
                fontSize = 12.sp,
                color    = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun PayRow(
    label    : String,
    value    : String,
    bold     : Boolean = false,
    highlight: Boolean = false
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(
            text       = value,
            fontSize   = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color      = if (highlight) PrimaryBlue else TextPrimary
        )
    }
}