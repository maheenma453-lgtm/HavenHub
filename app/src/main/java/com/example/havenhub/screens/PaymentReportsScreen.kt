package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.paymentsUiState.collectAsState()
    var selectedPeriod by remember { mutableStateOf("This Month") }
    var selectedType by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Filter
            item {
                val periods = listOf("Today", "This Week", "This Month", "This Year")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periods.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                selectedPeriod = period
                                viewModel.loadPaymentReports(period)
                            },
                            label = { Text(period, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Revenue Overview
            item {
                Text("Revenue Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text("Total Revenue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(uiState.totalRevenue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                RevenueStatItem("Gross", uiState.grossRevenue)
                                RevenueStatItem("Refunds", uiState.totalRefunds, isNegative = true)
                                RevenueStatItem("Platform Fee", uiState.platformFee)
                                RevenueStatItem("Net", uiState.netRevenue)
                            }
                        }
                    }
                }
            }

            // Payment Type Filter
            item {
                Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                val types = listOf("All", "Received", "Refunded", "Pending")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                viewModel.filterPaymentsByType(type)
                            },
                            label = { Text(type) }
                        )
                    }
                }
            }

            // Transaction List
            if (!uiState.isLoading) {
                items(uiState.transactions, key = { it.transactionId }) { tx ->
                    PaymentTransactionItem(
                        transactionId = tx.transactionId,
                        propertyTitle = tx.propertyTitle,
                        tenantName = tx.tenantName,
                        amount = tx.amount,
                        date = tx.date,
                        type = tx.type
                    )
                }
            }
        }
    }
}

@Composable
private fun RevenueStatItem(label: String, value: String, isNegative: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaymentTransactionItem(
    transactionId: String,
    propertyTitle: String,
    tenantName: String,
    amount: String,
    date: String,
    type: String
) {
    val isRefund = type.lowercase() == "refunded"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isRefund) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isRefund) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isRefund) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(propertyTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("By: $tenantName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isRefund) "-$amount" else amount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRefund) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        type,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}


