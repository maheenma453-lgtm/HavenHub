package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    // ViewModel ke 'uiState' ko observe kar rahe hain
    val uiState by viewModel.uiState.collectAsState()
    var selectedPeriod by remember { mutableStateOf("All Time") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Period Selector logic (Filhal loadAllReportsData hi call kar rahe hain)
            item {
                val periods = listOf("All Time", "Today", "This Month")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periods.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                selectedPeriod = period
                                viewModel.loadAllReportsData() // Reset/Reload data
                            },
                            label = { Text(period, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            item {
                Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            // --- Summary Cards Section ---
            item {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReportSummaryCard(
                                title = "Total Revenue",
                                value = "PKR ${String.format("%,.0f", uiState.stats.totalRevenue)}",
                                icon = Icons.Default.Payment,
                                modifier = Modifier.weight(1f)
                            )
                            ReportSummaryCard(
                                title = "Total Bookings",
                                value = uiState.stats.totalBookings.toString(),
                                icon = Icons.Default.BarChart,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReportSummaryCard(
                                title = "Total Users",
                                value = uiState.stats.totalUsers.toString(),
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f)
                            )
                            ReportSummaryCard(
                                title = "Active Props",
                                value = uiState.stats.activeProperties.toString(),
                                icon = Icons.Default.Home,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- Detailed Sections ---
            item {
                Text("Booking Status Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val total = uiState.stats.totalBookings.toFloat()

                    // Completed Bookings Row
                    BookingStatusRow(
                        status = "Completed",
                        count = uiState.stats.completedBookings,
                        percentage = if (total > 0) (uiState.stats.completedBookings / total) * 100f else 0f
                    )

                    // Cancelled Bookings Row
                    BookingStatusRow(
                        status = "Cancelled",
                        count = uiState.stats.cancelledBookings,
                        percentage = if (total > 0) (uiState.stats.cancelledBookings / total) * 100f else 0f
                    )
                }
            }

            item {
                Text("Detailed Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                ReportNavigationCard(
                    title = "Payment Transactions",
                    description = "View all ${uiState.payments.size} transactions",
                    onClick = { /* Navigate to Payment List Screen */ }
                )
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportNavigationCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.BarChart, contentDescription = null)
        }
    }
}

@Composable
private fun BookingStatusRow(status: String, count: Int, percentage: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(status, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("$count (${String.format("%.1f", percentage)}%)", style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}