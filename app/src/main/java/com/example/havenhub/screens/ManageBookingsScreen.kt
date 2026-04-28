package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.ManagementViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBookingsScreen(
    navController: NavController,
    viewModel    : ManagementViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    var selectedStatus by remember { mutableStateOf("All") }
    val dateFormatter  = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }

    val filteredBookings = remember(uiState.bookings, selectedStatus) {
        if (selectedStatus == "All") uiState.bookings
        else uiState.bookings.filter { it.status.equals(selectedStatus, ignoreCase = true) }
    }

    // Status counts for badges
    val statusCounts = remember(uiState.bookings) {
        mapOf(
            "All"       to uiState.bookings.size,
            "Pending"   to uiState.bookings.count { it.status.equals("Pending",   ignoreCase = true) },
            "Confirmed" to uiState.bookings.count { it.status.equals("Confirmed", ignoreCase = true) },
            "Completed" to uiState.bookings.count { it.status.equals("Completed", ignoreCase = true) },
            "Cancelled" to uiState.bookings.count { it.status.equals("Cancelled", ignoreCase = true) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Bookings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        containerColor = Color(0xFFF4F6FB)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Filter Banner ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(PrimaryBlue, Color(0xFF1565C0))))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val statuses = listOf("All", "Pending", "Confirmed", "Completed", "Cancelled")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statuses) { status ->
                        val selected = selectedStatus == status
                        val count    = statusCounts[status] ?: 0
                        FilterChip(
                            selected = selected,
                            onClick  = { selectedStatus = status },
                            label    = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(status, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    if (count > 0) {
                                        Box(
                                            modifier         = Modifier
                                                .clip(CircleShape)
                                                .background(if (selected) PrimaryBlue else Color.White.copy(.3f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("$count", fontSize = 9.sp, color = if (selected) Color.White else Color.White.copy(.8f), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White,
                                selectedLabelColor     = PrimaryBlue,
                                containerColor         = Color.White.copy(.15f),
                                labelColor             = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = selected,
                                selectedBorderColor = Color.Transparent,
                                borderColor         = Color.White.copy(.3f)
                            )
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryBlue.copy(.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${filteredBookings.size} bookings found",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = PrimaryBlue
                            )
                        }
                    }

                    items(filteredBookings, key = { it.bookingId }) { booking ->
                        val checkIn  = booking.checkInDate?.toDate()?.let  { dateFormatter.format(it) } ?: "N/A"
                        val checkOut = booking.checkOutDate?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"

                        ModernBookingCard(
                            propertyTitle = booking.propertyTitle,
                            tenantName    = booking.tenantName,
                            dateRange     = "$checkIn – $checkOut",
                            totalAmount   = booking.formattedTotal,
                            status        = booking.bookingStatus.displayName(),
                            isCancellable = booking.isCancellable,
                            onCancel      = { viewModel.cancelBooking(booking.bookingId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernBookingCard(
    propertyTitle : String,
    tenantName    : String,
    dateRange     : String,
    totalAmount   : String,
    status        : String,
    isCancellable : Boolean,
    onCancel      : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (statusColor, statusBg) = when (status.lowercase()) {
        "confirmed" -> Pair(SuccessGreen,    SuccessGreen.copy(.12f))
        "pending"   -> Pair(WarningOrange,   WarningOrange.copy(.12f))
        "cancelled" -> Pair(ErrorRed,        ErrorRed.copy(.12f))
        "completed" -> Pair(Color(0xFF1565C0), Color(0xFF1565C0).copy(.12f))
        else        -> Pair(Color(0xFF888888), Color(0xFFF0F0F0))
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(PrimaryBlue.copy(.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        propertyTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = Color(0xFF1A1A2E),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        "Tenant: $tenantName",
                        fontSize = 12.sp,
                        color    = Color(0xFF888888),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(status, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                }

                if (isCancellable) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, null, tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded         = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor   = Color.White
                        ) {
                            DropdownMenuItem(
                                text        = { Text("Cancel Booking", color = ErrorRed, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.Cancel, null, tint = ErrorRed) },
                                onClick     = { menuExpanded = false; onCancel() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Date range
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.DateRange, null, tint = Color(0xFF888888), modifier = Modifier.size(14.dp))
                    Text(dateRange, fontSize = 12.sp, color = Color(0xFF888888))
                }

                // Amount
                Text(
                    totalAmount,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = PrimaryBlue
                )
            }
        }
    }
}
........



















