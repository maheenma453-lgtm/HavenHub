package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.PaymentMethod
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.PaymentViewModel

// Green color used for the "Secured" badge on the order summary card
private val PGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    bookingId    : String,
    payerId      : String,
    payeeId      : String,
    payerName    : String,
    payeeName    : String,
    amount       : String,
    paymentType  : String = "FULL",   // "FULL" | "DEPOSIT" | "REMAINING"
    packageId    : String = "none",   // rental package reference; "none" for simple bookings
    viewModel    : PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Verify if payment already exists for this booking when the screen opens
    LaunchedEffect(bookingId) {
        viewModel.verifyPaymentStatus(bookingId)
    }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val error            = MaterialTheme.colorScheme.error

    val amountDouble = amount.toDoubleOrNull() ?: 0.0

    // FIX: "FULL" OR "REMAINING" both count as final/full payment phase.
    // "DEPOSIT" is the only case where we are paying partial (20%) upfront.
    // Pull version had only `paymentType == "FULL"` which was wrong —
    // it would show "Complete Deposit Payment" title for REMAINING payments.
    val isFinalPaymentPhase = paymentType == "FULL" || paymentType == "REMAINING"

    // Navigate to PaymentSuccess after processPayment() succeeds.
    // popUpTo uses all 9 route segments to correctly clear the payment screen from back stack.
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.navigate(Screen.PaymentSuccess.createRoute(bookingId)) {
                popUpTo(
                    "payment/$bookingId/$payerId/$payeeId/$payerName/$payeeName/$amount/$paymentType/$packageId"
                ) { inclusive = true }
            }
            viewModel.clearMessages()
        }
    }

    // Available payment methods shown as selectable rows
    val methods = listOf(
        Triple(PaymentMethod.JAZZCASH,      "📱", Color(0xFFD50000)),
        Triple(PaymentMethod.EASYPAISA,     "💚", Color(0xFF2E7D32)),
        Triple(PaymentMethod.CREDIT_CARD,   "💳", Color(0xFF1565C0)),
        Triple(PaymentMethod.BANK_TRANSFER, "🏦", Color(0xFF4A148C))
    )

    Scaffold(
        topBar = {
            // Gradient top bar — title changes based on payment phase
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
            ) {
                Row(
                    modifier          = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tertiary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            // Show correct title for each payment type
                            text = when (paymentType) {
                                "DEPOSIT"   -> "Complete Deposit Payment"
                                "REMAINING" -> "Pay Remaining Balance"
                                else        -> "Complete Payment"   // FULL
                            },
                            fontWeight = FontWeight.Bold,
                            color      = onPrimary,
                            fontSize   = 17.sp
                        )
                        Text("Secure & encrypted", fontSize = 11.sp, color = onPrimary.copy(0.55f))
                    }
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(onPrimary.copy(0.1f)),
                        Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = tertiary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        },
        containerColor = background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Order Summary card ────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = primary),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💳", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onPrimary)
                    }
                    Spacer(Modifier.height(14.dp))

                    PaySummaryRow("Booking ID",   "#${bookingId.take(8).uppercase()}",  onPrimary)
                    PaySummaryRow("From",          payerName.ifBlank { "Tenant" },       onPrimary)
                    PaySummaryRow("To",            payeeName.ifBlank { "Landlord" },     onPrimary)
                    // Payment type label shown clearly so user knows what they are paying
                    PaySummaryRow(
                        "Payment Type",
                        when (paymentType) {
                            "DEPOSIT"   -> "Advance Deposit (20%)"
                            "REMAINING" -> "Remaining Balance (80%)"
                            else        -> "Full Amount (100%)"
                        },
                        onPrimary
                    )

                    Spacer(Modifier.height(8.dp))
                    // Decorative gradient divider line
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Brush.horizontalGradient(listOf(tertiary.copy(0.6f), Color.Transparent)))
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            // Label changes based on payment type
                            Text(
                                when (paymentType) {
                                    "DEPOSIT"   -> "Deposit Payable"
                                    "REMAINING" -> "Remaining Payable"
                                    else        -> "Total Payable"
                                },
                                fontSize = 12.sp,
                                color    = onPrimary.copy(0.6f)
                            )
                            Text(
                                "PKR ${"%,.0f".format(amountDouble)}",
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.Black,
                                color      = tertiary
                            )
                        }
                        // "Secured" badge
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(PGreen.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, null, tint = PGreen, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Secured", fontSize = 11.sp, color = PGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Payment method selection ──────────────────────────
            Text("Select Payment Method", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurface)

            methods.forEach { (method, icon, accent) ->
                val isSelected = uiState.selectedMethod == method
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) primary.copy(0.07f) else surface)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) primary.copy(0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.selectPaymentMethod(method) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Method icon box
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.1f)),
                        Alignment.Center
                    ) {
                        Text(icon, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(method.displayName(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = onSurface)
                        Text(
                            when (method) {
                                PaymentMethod.JAZZCASH      -> "Pay via JazzCash mobile account"
                                PaymentMethod.EASYPAISA     -> "Pay via EasyPaisa mobile account"
                                PaymentMethod.CREDIT_CARD   -> "Cash on Arrival"
                                PaymentMethod.BANK_TRANSFER -> "Direct bank transfer"
                                else                        -> ""
                            },
                            fontSize = 11.sp,
                            color    = onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick  = { viewModel.selectPaymentMethod(method) },
                        colors   = RadioButtonDefaults.colors(selectedColor = primary)
                    )
                }

                // Divider between methods (not after the last one)
                if (method != methods.last().first) {
                    HorizontalDivider(color = background, thickness = 2.dp)
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Pay button ────────────────────────────────────────
            Button(
                onClick = {
                    val selected = uiState.selectedMethod ?: return@Button
                    viewModel.processPayment(
                        bookingId          = bookingId,
                        payerId            = payerId,
                        payeeId            = payeeId,
                        payerName          = payerName,
                        payeeName          = payeeName,
                        amount             = amount,
                        packageId          = packageId,
                        method             = selected,
                        // isFinalPayment = true for FULL or REMAINING; false for DEPOSIT
                        isFinalPayment     = isFinalPaymentPhase,
                        // isPreBookingDirect = true only for DEPOSIT payment (paying 20% upfront)
                        isPreBookingDirect = paymentType == "DEPOSIT"
                    )
                },
                modifier  = Modifier.fillMaxWidth().height(54.dp),
                shape     = RoundedCornerShape(14.dp),
                enabled   = !uiState.isLoading && uiState.selectedMethod != null,
                colors    = ButtonDefaults.buttonColors(
                    containerColor         = primary,
                    contentColor           = onPrimary,
                    disabledContainerColor = onSurfaceVariant.copy(0.3f),
                    disabledContentColor   = onPrimary.copy(0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Payment, null, tint = tertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Pay PKR ${"%,.0f".format(amountDouble)}",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Hint: shown when no method is selected ────────────
            if (uiState.selectedMethod == null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = onSurfaceVariant, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Please select a payment method", fontSize = 11.sp, color = onSurfaceVariant)
                }
            }

            // ── Error message ─────────────────────────────────────
            uiState.errorMessage?.let { errMsg ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(error.copy(0.08f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(errMsg, color = error, fontSize = 13.sp)
                }
            }

            // ── Security note at the bottom ───────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, tint = onSurfaceVariant, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Your payment is secured and encrypted", fontSize = 11.sp, color = onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helper: single row in the order summary card ──────────────────────────────
@Composable
private fun PaySummaryRow(label: String, value: String, onPrimary: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = onPrimary.copy(0.6f))
        Text(value, fontSize = 12.sp, color = onPrimary, fontWeight = FontWeight.Medium)
    }
}