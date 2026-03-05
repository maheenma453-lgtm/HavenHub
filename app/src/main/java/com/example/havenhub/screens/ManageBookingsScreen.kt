package com.example.havenhub.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.havenhub.viewmodel.ManagementViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBookingsScreen(
    navController: NavController,
    viewModel: ManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedStatus by remember { mutableStateOf("All") }

    // Date formatting helper for Timestamps
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Filtering logic
    val filteredBookings = remember(uiState.bookings, selectedStatus) {
        if (selectedStatus == "All") uiState.bookings
        else uiState.bookings.filter { it.status.name.equals(selectedStatus, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Status Filter Chips
            val statuses = listOf("All", "Pending", "Confirmed", "Completed", "Cancelled")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "${filteredBookings.size} bookings found",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(filteredBookings, key = { it.bookingId }) { booking ->
                        // Converting Timestamp to readable string
                        val checkIn = booking.checkInDate?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"
                        val checkOut = booking.checkOutDate?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"

                        BookingManagementCard(
                            propertyTitle = booking.propertyTitle,
                            tenantName = booking.tenantName,
                            dateRange = "$checkIn - $checkOut",
                            totalAmount = booking.formattedTotal, // Getter from Booking class
                            status = booking.status.displayName(), // Enum function
                            isCancellable = booking.isCancellable, // Logic from Booking class
                            onCancel = { viewModel.cancelBooking(booking.bookingId) } // ✅ Now matches ViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingManagementCard(
    propertyTitle: String,
    tenantName: String,
    dateRange: String,
    totalAmount: String,
    status: String,
    isCancellable: Boolean,
    onCancel: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(propertyTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Tenant: $tenantName", style = MaterialTheme.typography.bodySmall)
                Text(dateRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(totalAmount, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    SuggestionChip(
                        onClick = {},
                        label = { Text(status, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Options menu: only shows if booking is cancellable (Pending/Confirmed)
            if (isCancellable) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Cancel Booking", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onCancel()
                            }
                        )
                    }
                }
            }
        }
    }
}