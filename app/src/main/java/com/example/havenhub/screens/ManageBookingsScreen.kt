package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.viewmodel.ManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBookingsScreen(
    navController: NavController,
    viewModel: ManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.bookingsState.collectAsState()
    var selectedStatus by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Filter
            val statuses = listOf("All", "Pending", "Confirmed", "Completed", "Cancelled")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = {
                            selectedStatus = status
                            viewModel.filterBookingsByStatus(status)
                        },
                        label = { Text(status) }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                "${uiState.bookings.size} bookings",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(uiState.bookings, key = { it.bookingId }) { booking ->
                            BookingManagementCard(
                                bookingId = booking.bookingId,
                                propertyTitle = booking.propertyTitle,
                                tenantName = booking.tenantName,
                                dateRange = booking.dateRange,
                                totalAmount = booking.totalAmount,
                                status = booking.status,
                                onCancel = { viewModel.cancelBooking(booking.bookingId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingManagementCard(
    bookingId: String,
    propertyTitle: String,
    tenantName: String,
    dateRange: String,
    totalAmount: String,
    status: String,
    onCancel: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val statusColor = when (status.lowercase()) {
        "confirmed" -> MaterialTheme.colorScheme.tertiaryContainer
        "pending" -> MaterialTheme.colorScheme.secondaryContainer
        "cancelled" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(propertyTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Tenant: $tenantName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dateRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(totalAmount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Surface(shape = MaterialTheme.shapes.extraSmall, color = statusColor) {
                        Text(
                            status,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (status.lowercase() !in listOf("cancelled", "completed")) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Cancel Booking", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onCancel() }
                        )
                    }
                }
            }
        }
    }
}


