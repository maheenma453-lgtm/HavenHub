package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onBack: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {},
    onSelectMethod: () -> Unit = {}
) {
    var selectedMethod by remember { mutableStateOf("JazzCash") }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
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
            // Order Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    PayRow("Property", "Luxury Sea View Apartment")
                    PayRow("Check-In", "Nov 10, 2024")
                    PayRow("Check-Out", "Nov 14, 2024")
                    PayRow("Nights", "4")
                    PayRow("Rs. 8,500 × 4 nights", "Rs. 34,000")
                    HorizontalDivider()
                    PayRow("Total", "Rs. 34,000", bold = true, highlight = true)
                }
            }

            // Payment Method
            Text("Payment Method", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

            listOf("JazzCash", "EasyPaisa", "Credit/Debit Card", "Bank Transfer").forEach { method ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(Modifier),
                    shape = RoundedCornerShape(10.dp),
                    border = if (selectedMethod == method)
                        CardDefaults.outlinedCardBorder().copy()
                    else CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedMethod == method, onClick = { selectedMethod = method })
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = when (method) {
                                "Credit/Debit Card" -> Icons.Default.CreditCard
                                "Bank Transfer" -> Icons.Default.AccountBalance
                                else -> Icons.Default.Payment
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(method, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    isProcessing = true
                    onPaymentSuccess()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Pay Rs. 34,000", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "Your payment is secured and encrypted.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun PayRow(label: String, value: String, bold: Boolean = false, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = if (highlight) MaterialTheme.colorScheme.onSurface else Color.Gray)
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
