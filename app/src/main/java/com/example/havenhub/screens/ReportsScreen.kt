package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPeriod by remember { mutableStateOf("This Month") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
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
            // Period Selector
            item {
                val periods = listOf("Today", "This Week", "This Month", "This Year")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periods.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                selectedPeriod = period
                                viewModel.loadReports(period)
                            },
                            label = { Text(period, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Summary Cards
            item {
                Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReportSummaryCard(
                                title = "Total Revenue",
                                value = uiState.totalRevenue,
                                icon = Icons.Default.Payment,
                                modifier = Modifier.weight(1f)
                            )
                            ReportSummaryCard(
                                title = "Total Bookings",
                                value = uiState.totalBookings,
                                icon = Icons.Default.BarChart,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReportSummaryCard(
                                title = "New Users",
                                value = uiState.newUsers,
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f)
                            )
                            ReportSummaryCard(
                                title = "Active Properties",
                                value = uiState.activeProperties,
                                icon = Icons.Default.Home,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Report Sections
            item {
                Text("Detailed Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                ReportNavigationCard(
                    title = "Payment Reports",
                    description = "Revenue breakdown, transactions, refunds",
                    onClick = { navController.navigate(Screen.PaymentReports.route) }
                )
            }

            // Booking Status Breakdown
            if (!uiState.isLoading && uiState.bookingStatusBreakdown.isNotEmpty()) {
                item {
                    Text("Booking Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                items(uiState.bookingStatusBreakdown) { item ->
                    BookingStatusRow(
                        status = item.status,
                        count = item.count,
                        percentage = item.percentage
                    )
                }
            }

            // Top Properties
            if (!uiState.isLoading && uiState.topProperties.isNotEmpty()) {
                item {
                    Text("Top Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                items(uiState.topProperties.take(5), key = { it.propertyId }) { property ->
                    TopPropertyItem(
                        rank = uiState.topProperties.indexOf(property) + 1,
                        title = property.title,
                        bookingsCount = property.bookingsCount,
                        revenue = property.revenue
                    )
                }
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
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                Text("$count (${String.format("%.1f", percentage)}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TopPropertyItem(rank: Int, title: String, bookingsCount: Int, revenue: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("#$rank", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("$bookingsCount bookings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(revenue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}


