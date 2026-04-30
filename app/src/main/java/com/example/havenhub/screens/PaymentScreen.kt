package com.example.havenhub.screens

import androidx.compose.foundation.background
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
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.PaymentMethod
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.BookingViewModel
import com.example.havenhub.viewmodel.PaymentViewModel

// ── Design tokens ─────────────────────────────────────────────
private val PNavy  = Color(0xFF0D1B3E)
private val PGold  = Color(0xFFD4AF37)
private val PBg    = Color(0xFFF5F7FA)
private val PMuted = Color(0xFF8899AA)
private val PWhite = Color(0xFFFFFFFF)
private val PGreen = Color(0xFF22C55E)
private val PRed   = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController   : NavController,
    bookingId       : String,
    payerId         : String,
    payeeId         : String,
    payerName       : String,
    payeeName       : String,
    amount          : Double,
    viewModel       : PaymentViewModel  = hiltViewModel(),
    bookingViewModel: BookingViewModel  = hiltViewModel()   // ✅ add kiya
) {
    val uiState by viewModel.uiState.collectAsState()
    val bookingUiState by bookingViewModel.uiState.collectAsState()
    val bookingMethod = bookingUiState.currentBooking?.paymentMethod

    // ✅ Payment success → PaymentSuccess screen pe jao
    // confirmPayment() wahan se hoga ya PaymentViewModel already handle karta hai
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {

            // ✅ STEP 3.1: Booking status update karo
            bookingViewModel.updateStatusByAdmin(
                bookingId,
                BookingStatus.PENDING_APPROVAL
            )

            // ✅ STEP 3.2: Navigate karo
            navController.navigate(
                Screen.PaymentSuccess.createRoute(bookingId)
            ) {
                popUpTo("payment/$bookingId/$payerId/$payeeId/$payerName/$payeeName/$amount") {
                    inclusive = true
                }
            }

            // ✅ STEP 3.3: Clear state
            viewModel.clearMessages()
        }
    }
// ✅ YEH NAYA CODE (STEP 2)
    LaunchedEffect(bookingId) {
        bookingViewModel.loadBookingById(bookingId)
    }
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(PNavy, Color(0xFF1A2F5E))))
            ) {
                Row(
                    modifier          = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PGold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Complete Payment", fontWeight = FontWeight.Bold, color = PWhite, fontSize = 17.sp)
                        Text("Secure & encrypted", fontSize = 11.sp, color = PWhite.copy(0.55f))
                    }
                    // Lock icon
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(PWhite.copy(0.1f)),
                        Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = PGold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        },
        containerColor = PBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ══════════════════════════════════════════════════
            // 1. ORDER SUMMARY
            // ══════════════════════════════════════════════════
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = PNavy),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💳", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PWhite)
                    }
                    Spacer(Modifier.height(14.dp))

                    PSummaryRow("Booking ID", "#${bookingId.take(8).uppercase()}")
                    PSummaryRow("From",       payerName.ifBlank { "Tenant" })
                    PSummaryRow("To",         payeeName.ifBlank { "Landlord" })

                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Brush.horizontalGradient(listOf(PGold.copy(0.6f), Color.Transparent)))
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Amount", fontSize = 12.sp, color = PWhite.copy(0.6f))
                            Text(
                                "PKR ${"%,.0f".format(amount)}",
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.Black,
                                color      = PGold
                            )
                        }
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

            // ══════════════════════════════════════════════════
            // 2. PAYMENT METHOD SELECTION
            // ══════════════════════════════════════════════════
            Text(
                "Select Payment Method",
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = PNavy
            )

            val methods = listOf(
                Triple(PaymentMethod.JAZZCASH,      "📱", Color(0xFFD50000)),
                Triple(PaymentMethod.EASYPAISA,     "💚", Color(0xFF2E7D32)),
                Triple(PaymentMethod.CREDIT_CARD,   "💳", Color(0xFF1565C0)),
                Triple(PaymentMethod.BANK_TRANSFER, "🏦", Color(0xFF4A148C))
            )
            LaunchedEffect(bookingMethod) {
                bookingMethod?.let {
                    val matched = methods.find { m ->
                        when (it) {
                            "JazzCash" -> m.first == PaymentMethod.JAZZCASH
                            "EasyPaisa" -> m.first == PaymentMethod.EASYPAISA
                            "Bank Transfer" -> m.first == PaymentMethod.BANK_TRANSFER
                            "Cash on Arrival" -> m.first == PaymentMethod.CREDIT_CARD
                            else -> false
                        }
                    }?.first
                    matched?.let { viewModel.selectPaymentMethod(it) }
                }
            }
            methods.forEach { (method, icon, accent) ->
                val isSelected = uiState.selectedMethod == method
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) PNavy.copy(0.07f) else PWhite
                        )
                        .clickable { viewModel.selectPaymentMethod(method) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon box
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(0.1f)),
                        Alignment.Center
                    ) {
                        Text(icon, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(method.displayName(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PNavy)
                        Text(
                            when (method) {
                                PaymentMethod.JAZZCASH      -> "Pay via JazzCash mobile account"
                                PaymentMethod.EASYPAISA     -> "Pay via EasyPaisa mobile account"
                                PaymentMethod.CREDIT_CARD   -> "Cash on Arrival"
                                PaymentMethod.BANK_TRANSFER -> "Direct bank transfer"
                                else                        -> ""
                            },
                            fontSize = 11.sp,
                            color    = PMuted
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick  = { viewModel.selectPaymentMethod(method) },
                        colors   = RadioButtonDefaults.colors(selectedColor = PNavy)
                    )
                }

                if (method != methods.last().first) {
                    HorizontalDivider(color = PBg, thickness = 2.dp)
                }
            }

            // ══════════════════════════════════════════════════
            // 3. PAY BUTTON
            // ══════════════════════════════════════════════════
            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val selected = uiState.selectedMethod

                    if (selected == null) return@Button

                    val isMatch = when (bookingMethod) {
                        "JazzCash" -> selected == PaymentMethod.JAZZCASH
                        "EasyPaisa" -> selected == PaymentMethod.EASYPAISA
                        "Bank Transfer" -> selected == PaymentMethod.BANK_TRANSFER
                        "Cash on Arrival" -> selected == PaymentMethod.CREDIT_CARD
                        else -> false
                    }

                    if (!isMatch) {
                        viewModel.setError("Booking and payment method do not match")
                        return@Button
                    }

                    viewModel.processPayment(
                        bookingId = bookingId,
                        payerId   = payerId,
                        payeeId   = payeeId,
                        payerName = payerName,
                        payeeName = payeeName,
                        amount    = amount,
                        method    = selected
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                enabled  = !uiState.isLoading && uiState.selectedMethod != null,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = PNavy,
                    contentColor           = PWhite,
                    disabledContainerColor = PMuted.copy(0.3f),
                    disabledContentColor   = PWhite.copy(0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PWhite, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Payment, null, tint = PGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pay PKR ${"%,.0f".format(amount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // No method selected hint
            if (uiState.selectedMethod == null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = PMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Please select a payment method", fontSize = 11.sp, color = PMuted)
                }
            }

            // Error
            uiState.errorMessage?.let { error ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(PRed.copy(0.08f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = PRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = PRed, fontSize = 13.sp)
                }
            }

            // Security note
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, tint = PMuted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Your payment is secured and encrypted", fontSize = 11.sp, color = PMuted)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helper composables ────────────────────────────────────────
@Composable
private fun PSummaryRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = PWhite.copy(0.6f))
        Text(value, fontSize = 12.sp, color = PWhite, fontWeight = FontWeight.Medium)
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
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = PMuted)
        Text(
            value,
            fontSize   = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color      = if (highlight) PNavy else PNavy.copy(0.8f)
        )
    }
}
