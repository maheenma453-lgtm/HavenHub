package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.PaymentMethod
import com.example.havenhub.viewmodel.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    navController: NavController,
    viewModel    : PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val methods = remember {
        listOf(
            PaymentMethod.JAZZCASH,
            PaymentMethod.EASYPAISA,
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.DEBIT_CARD,
            PaymentMethod.BANK_TRANSFER,
            PaymentMethod.CASH
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Select Payment Method",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }

            items(methods) { method ->
                val isDefault  = uiState.defaultMethod == method
                val isSelected = uiState.selectedMethod == method

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (isDefault)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (method) {
                                PaymentMethod.CREDIT_CARD,
                                PaymentMethod.DEBIT_CARD    -> Icons.Default.CreditCard
                                PaymentMethod.BANK_TRANSFER -> Icons.Default.AccountBalance
                                else                        -> Icons.Default.Payment
                            },
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                method.displayName(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            if (isDefault) {
                                Text(
                                    "Default",
                                    fontSize   = 11.sp,
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!isDefault) {
                            TextButton(onClick = { viewModel.setDefaultMethod(method) }) {
                                Text(
                                    "Set Default",
                                    fontSize = 12.sp,
                                    color    = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick  = { viewModel.selectPaymentMethod(method) },
                            colors   = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            item {
                uiState.errorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}