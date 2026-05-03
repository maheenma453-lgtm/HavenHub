package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
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

// ── Brand Colors ──────────────────────────────────────────────────────────────
private val NavyBlue     = Color(0xFF1B2A4A)
private val NavyLight    = Color(0xFF243658)
private val Gold         = Color(0xFFC9A227)
private val GoldDark     = Color(0xFFA07D10)
private val PageBg       = Color(0xFFF4F6FA)
private val WarningAmber = Color(0xFFD97706)
private val PendingBlue  = Color(0xFF3B82F6)
private val SuccessGreen = Color(0xFF27AE60)
private val ErrorRed     = Color(0xFFE74C3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBookingsScreen(
    navController: NavController,
    viewModel    : ManagementViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    var selectedStatus    by remember { mutableStateOf("All") }
    val dateFormatter     = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }
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

    val statusList = listOf("All", "Pending", "Pending_Approval", "Confirmed", "Completed", "Cancelled")

    val filteredBookings = remember(uiState.bookings, selectedStatus) {
        if (selectedStatus == "All") uiState.bookings
        else uiState.bookings.filter { it.status.equals(selectedStatus, ignoreCase = true) }
    }

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
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = PageBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NavyBlue, NavyLight)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Gold)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Manage Bookings",
                            color         = Color.White,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            "${uiState.bookings.size} total bookings",
                            color    = Gold.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
                // Gold shimmer line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Filter Chips ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(NavyBlue, NavyLight)))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusList) { status ->
                        val selected = selectedStatus == status
                        val count    = statusCounts[status] ?: 0
                        val label    = when (status) {
                            "Pending_Approval" -> "Awaiting"
                            else               -> status
                        }

                        FilterChip(
                            selected = selected,
                            onClick  = { selectedStatus = status },
                            label    = {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        label,
                                        fontSize   = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (selected) NavyBlue else Color.White.copy(0.25f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$count",
                                                fontSize   = 9.sp,
                                                color      = if (selected) Gold else Color.White,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold,
                                selectedLabelColor     = Color.White,
                                containerColor         = Color.White.copy(0.12f),
                                labelColor             = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = selected,
                                selectedBorderColor = GoldDark,
                                borderColor         = Color.White.copy(0.25f),
                                selectedBorderWidth = 1.5.dp,
                                borderWidth         = 1.dp
                            )
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Gold, strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Count banner
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(NavyBlue, NavyLight))
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(listOf(Gold.copy(0.6f), GoldDark.copy(0.4f))),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Gold)
                                )
                                Text(
                                    "${filteredBookings.size} booking${if (filteredBookings.size != 1) "s" else ""} found",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Gold
                                )
                            }
                        }
                    }

                    items(filteredBookings, key = { it.bookingId }) { booking ->
                        val checkIn  = booking.checkInDate?.toDate()?.let  { dateFormatter.format(it) } ?: "N/A"
                        val checkOut = booking.checkOutDate?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"
                        val isPendingApproval = booking.status.equals("PENDING_APPROVAL", ignoreCase = true)
                        val displayTenantName = booking.tenantName.takeIf { it.isNotBlank() } ?: "Unknown Tenant"

                        PremiumBookingCard(
                            propertyTitle     = booking.propertyTitle.ifBlank { "Property" },
                            tenantName        = displayTenantName,
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

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM BOOKING CARD
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PremiumBookingCard(
    propertyTitle    : String,
    tenantName       : String,
    dateRange        : String,
    totalAmount      : String,
    status           : String,
    isCancellable    : Boolean,
    isPendingApproval: Boolean,
    onCancel         : () -> Unit,
    onApprove        : () -> Unit,
    onReject         : () -> Unit
) {
    var menuExpanded      by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog  by remember { mutableStateOf(false) }

    // ── Approve Dialog ─────────────────────────────────────────────────────────
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(0.12f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text("Approve Booking?", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = NavyBlue)
            },
            text = {
                Text(
                    "Tenant ki booking confirm kar doge?\nPayment already receive ho chuki hai.",
                    fontSize = 13.sp, color = NavyBlue.copy(0.55f), lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick  = { showApproveDialog = false; onApprove() },
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Yes, Approve", fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showApproveDialog = false },
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, NavyBlue.copy(0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel", color = NavyBlue, fontWeight = FontWeight.Medium) }
            }
        )
    }

    // ── Reject Dialog ──────────────────────────────────────────────────────────
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ErrorRed.copy(0.12f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Cancel, null, tint = ErrorRed, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text("Reject Booking?", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = NavyBlue)
            },
            text = {
                Text(
                    "Kya aap yeh booking reject karna chahte ho?\nTenant ko refund process karna hoga.",
                    fontSize = 13.sp, color = NavyBlue.copy(0.55f), lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick  = { showRejectDialog = false; onReject() },
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Yes, Reject", fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showRejectDialog = false },
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, NavyBlue.copy(0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Go Back", color = NavyBlue, fontWeight = FontWeight.Medium) }
            }
        )
    }

    // ── Status config ──────────────────────────────────────────────────────────
    val (statusColor, statusBg, statusLabel) = when {
        status.equals("PENDING_APPROVAL", ignoreCase = true) ->
            Triple(WarningAmber,  WarningAmber.copy(0.12f),  "Awaiting")
        status.equals("CONFIRMED",        ignoreCase = true) ->
            Triple(SuccessGreen,  SuccessGreen.copy(0.12f),  "Confirmed")
        status.equals("PENDING",          ignoreCase = true) ->
            Triple(PendingBlue,   PendingBlue.copy(0.12f),   "Pending")
        status.equals("CANCELLED",        ignoreCase = true) ->
            Triple(ErrorRed,      ErrorRed.copy(0.12f),      "Cancelled")
        status.equals("COMPLETED",        ignoreCase = true) ->
            Triple(NavyLight,     NavyLight.copy(0.12f),     "Completed")
        else ->
            Triple(Color(0xFF888888), Color(0xFFF0F0F0), status)
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 4.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = NavyBlue.copy(alpha = 0.08f),
                spotColor    = NavyBlue.copy(alpha = 0.12f)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // Top accent bar: navy → gold
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(NavyBlue, Gold)))
        )

        Column(modifier = Modifier.padding(14.dp)) {

            // ── Top Row ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Icon circle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NavyBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarMonth, null,
                        tint     = Gold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        propertyTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = NavyBlue,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        "Tenant: $tenantName",
                        fontSize = 12.sp,
                        color    = NavyBlue.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = statusColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            statusLabel,
                            fontSize   = 10.sp,
                            color      = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Cancel overflow menu
                if (isCancellable) {
                    Box {
                        IconButton(
                            onClick  = { menuExpanded = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NavyBlue.copy(alpha = 0.06f))
                        ) {
                            Icon(
                                Icons.Default.MoreVert, null,
                                tint     = NavyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded         = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier         = Modifier.background(Color.White).width(170.dp)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ErrorRed)
                                    )
                                },
                                text = {
                                    Text(
                                        "Cancel Booking",
                                        color      = ErrorRed,
                                        fontWeight = FontWeight.Medium,
                                        fontSize   = 14.sp
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onCancel()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = NavyBlue.copy(alpha = 0.07f))
            Spacer(Modifier.height(10.dp))

            // ── Date + Amount Row ──────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange, null,
                        tint     = Gold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(dateRange, fontSize = 12.sp, color = NavyBlue.copy(alpha = 0.55f))
                }
                Text(
                    totalAmount,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = NavyBlue
                )
            }

            // ── Pending Approval Section ───────────────────────────────────────
            if (isPendingApproval) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = NavyBlue.copy(alpha = 0.07f))
                Spacer(Modifier.height(10.dp))

                // Info banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarningAmber.copy(0.08f))
                        .border(1.dp, WarningAmber.copy(0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                        Text(
                            "Tenant ne payment kar di hai. Approve ya reject karo.",
                            fontSize = 11.sp,
                            color    = WarningAmber,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = { showRejectDialog = true },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.5.dp, ErrorRed),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick  = { showApproveDialog = true },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = NavyBlue,
                            contentColor   = Gold
                        )
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
