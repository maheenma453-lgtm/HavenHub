package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
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

// ── Design tokens (same as rest of app) ──────────────────────
private val WarningAmber = Color(0xFFD97706)
private val PendingBlue  = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBookingsScreen(
    navController: NavController,
    viewModel    : ManagementViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    var selectedStatus by remember { mutableStateOf("All") }
    val dateFormatter  = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }

    // ✅ Success / Error Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.resetActionState()
        }
    }

    val filteredBookings = remember(uiState.bookings, selectedStatus) {
        if (selectedStatus == "All") uiState.bookings
        else uiState.bookings.filter {
            it.status.equals(selectedStatus, ignoreCase = true)
        }
    }

    // ✅ Status filter list mein PENDING_APPROVAL bhi add kiya
    val statusList = listOf("All", "Pending", "Pending_Approval", "Confirmed", "Completed", "Cancelled")

    val statusCounts = remember(uiState.bookings) {
        mapOf(
            "All"              to uiState.bookings.size,
            "Pending"          to uiState.bookings.count { it.status.equals("PENDING",          ignoreCase = true) },
            "Pending_Approval" to uiState.bookings.count { it.status.equals("PENDING_APPROVAL", ignoreCase = true) },
            "Confirmed"        to uiState.bookings.count { it.status.equals("CONFIRMED",        ignoreCase = true) },
            "Completed"        to uiState.bookings.count { it.status.equals("COMPLETED",        ignoreCase = true) },
            "Cancelled"        to uiState.bookings.count { it.status.equals("CANCELLED",        ignoreCase = true) },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage Bookings",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                    .background(
                        Brush.verticalGradient(listOf(PrimaryBlue, Color(0xFF1565C0)))
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusList) { status ->
                        val selected = selectedStatus == status
                        val count    = statusCounts[status] ?: 0
                        val label    = if (status == "Pending_Approval") "Awaiting" else status

                        FilterChip(
                            selected = selected,
                            onClick  = { selectedStatus = status },
                            label    = {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        label,
                                        fontSize   = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (count > 0) {
                                        Box(
                                            modifier         = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    if (selected) PrimaryBlue
                                                    else Color.White.copy(0.3f)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$count",
                                                fontSize   = 9.sp,
                                                color      = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White,
                                selectedLabelColor     = PrimaryBlue,
                                containerColor         = Color.White.copy(0.15f),
                                labelColor             = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = selected,
                                selectedBorderColor = Color.Transparent,
                                borderColor         = Color.White.copy(0.3f)
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
                                .background(PrimaryBlue.copy(0.1f))
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

                        // ✅ isPendingApproval check — landlord ko Accept/Reject dikhane ke liye
                        val isPendingApproval = booking.status.equals("PENDING_APPROVAL", ignoreCase = true)

                        ModernBookingCard(
                            propertyTitle     = booking.propertyTitle,
                            tenantName        = booking.tenantName,
                            dateRange         = "$checkIn – $checkOut",
                            totalAmount       = booking.formattedTotal,
                            status            = booking.status,
                            isCancellable     = booking.isCancellable,
                            isPendingApproval = isPendingApproval,
                            onCancel          = { viewModel.cancelBooking(booking.bookingId) },
                            onApprove         = { viewModel.approveBooking(booking.bookingId) },
                            onReject          = { viewModel.rejectBooking(booking.bookingId) }
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MODERN BOOKING CARD
// ══════════════════════════════════════════════════════════════
@Composable
private fun ModernBookingCard(
    propertyTitle     : String,
    tenantName        : String,
    dateRange         : String,
    totalAmount       : String,
    status            : String,
    isCancellable     : Boolean,
    isPendingApproval : Boolean,          // ✅ NEW
    onCancel          : () -> Unit,
    onApprove         : () -> Unit,       // ✅ NEW
    onReject          : () -> Unit        // ✅ NEW
) {
    var menuExpanded        by remember { mutableStateOf(false) }
    var showApproveDialog   by remember { mutableStateOf(false) }
    var showRejectDialog    by remember { mutableStateOf(false) }

    // ── Approve confirmation dialog ────────────────────────────
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(0.12f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = SuccessGreen, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Approve Booking?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 17.sp,
                    color      = Color(0xFF0D1B3E)
                )
            },
            text = {
                Text(
                    "Tenant ki booking confirm kar doge?\nPayment already receive ho chuki hai.",
                    fontSize   = 13.sp,
                    color      = Color(0xFF8899AA),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showApproveDialog = false; onApprove() },
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, Approve", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showApproveDialog = false },
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, Color(0xFF0D1B3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color(0xFF0D1B3E), fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    // ── Reject confirmation dialog ─────────────────────────────
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ErrorRed.copy(0.12f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Cancel, null,
                        tint = ErrorRed, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Reject Booking?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 17.sp,
                    color      = Color(0xFF0D1B3E)
                )
            },
            text = {
                Text(
                    "Kya aap yeh booking reject karna chahte ho?\nTenant ko refund process karna hoga.",
                    fontSize   = 13.sp,
                    color      = Color(0xFF8899AA),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick  = { showRejectDialog = false; onReject() },
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, Reject", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showRejectDialog = false },
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, Color(0xFF0D1B3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back", color = Color(0xFF0D1B3E), fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    // ── Status color mapping ───────────────────────────────────
    val (statusColor, statusBg, statusLabel) = when {
        status.equals("PENDING_APPROVAL", ignoreCase = true) ->
            Triple(WarningAmber, WarningAmber.copy(0.12f), "Awaiting Approval")
        status.equals("CONFIRMED",  ignoreCase = true) ->
            Triple(SuccessGreen, SuccessGreen.copy(0.12f), "Confirmed")
        status.equals("PENDING",    ignoreCase = true) ->
            Triple(PendingBlue,  PendingBlue.copy(0.12f),  "Pending")
        status.equals("CANCELLED",  ignoreCase = true) ->
            Triple(ErrorRed,     ErrorRed.copy(0.12f),     "Cancelled")
        status.equals("COMPLETED",  ignoreCase = true) ->
            Triple(Color(0xFF1565C0), Color(0xFF1565C0).copy(0.12f), "Completed")
        else -> Triple(Color(0xFF888888), Color(0xFFF0F0F0), status)
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Top Row: icon + title + status badge + menu ────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(PrimaryBlue.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarMonth, null,
                        tint     = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
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
                    Text(
                        statusLabel,
                        fontSize   = 10.sp,
                        color      = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Cancel menu (sirf cancellable bookings ke liye — yani PENDING wali)
                if (isCancellable) {
                    Box {
                        IconButton(
                            onClick  = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert, null,
                                tint     = Color(0xFF888888),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded         = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor   = Color.White
                        ) {
                            DropdownMenuItem(
                                text        = {
                                    Text(
                                        "Cancel Booking",
                                        color      = ErrorRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Cancel, null, tint = ErrorRed)
                                },
                                onClick     = {
                                    menuExpanded = false
                                    onCancel()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(10.dp))

            // ── Date + Amount Row ──────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange, null,
                        tint     = Color(0xFF888888),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(dateRange, fontSize = 12.sp, color = Color(0xFF888888))
                }
                Text(
                    totalAmount,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = PrimaryBlue
                )
            }

            // ✅ PENDING_APPROVAL: Accept / Reject buttons dikhao
            if (isPendingApproval) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(10.dp))

                // Info strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningAmber.copy(0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Info, null,
                        tint     = WarningAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "Tenant ne payment kar di hai. Approve ya reject karo.",
                        fontSize = 11.sp,
                        color    = WarningAmber
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Reject button ──────────────────────────
                    OutlinedButton(
                        onClick  = { showRejectDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape    = RoundedCornerShape(10.dp),
                        border   = BorderStroke(1.5.dp, ErrorRed),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(
                            Icons.Default.Close, null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Reject",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // ── Accept button ──────────────────────────
                    Button(
                        onClick  = { showApproveDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.Check, null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Accept",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}