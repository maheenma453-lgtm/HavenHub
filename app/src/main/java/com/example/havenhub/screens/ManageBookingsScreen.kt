package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.viewmodel.ManagementViewModel
import java.text.SimpleDateFormat
import java.util.*

// Semantic status colors — intentional
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
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.resetActionState() }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar("Error: $it"); viewModel.resetActionState() }
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
            "Cancelled"        to uiState.bookings.count { it.status.equals("CANCELLED",        ignoreCase = true) }
        )
    }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = background,
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tertiary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text("Manage Bookings", color = onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                        Text("${uiState.bookings.size} total bookings", color = tertiary.copy(0.85f), fontSize = 12.sp)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(background.copy(0f), tertiary, background.copy(0f)))).align(Alignment.BottomCenter))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Filter chips ──────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(primary, primaryContainer))).padding(horizontal = 16.dp, vertical = 12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusList) { status ->
                        val selected = selectedStatus == status
                        val count    = statusCounts[status] ?: 0
                        val label    = if (status == "Pending_Approval") "Awaiting" else status
                        FilterChip(
                            selected = selected,
                            onClick  = { selectedStatus = status },
                            label    = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier.clip(CircleShape).background(if (selected) primary else onPrimary.copy(0.25f)).padding(horizontal = 5.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("$count", fontSize = 9.sp, color = if (selected) tertiary else onPrimary, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tertiary, selectedLabelColor = onPrimary, containerColor = onPrimary.copy(0.12f), labelColor = onPrimary),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, selectedBorderColor = tertiary, borderColor = onPrimary.copy(0.25f), selectedBorderWidth = 1.5.dp, borderWidth = 1.dp)
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = tertiary, strokeWidth = 3.dp) }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Count banner
                    item {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
                                .border(1.dp, Brush.horizontalGradient(listOf(tertiary.copy(0.6f), tertiary.copy(0.4f))), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(tertiary))
                                Text("${filteredBookings.size} booking${if (filteredBookings.size != 1) "s" else ""} found", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tertiary)
                            }
                        }
                    }

                    items(filteredBookings, key = { it.bookingId }) { booking ->
                        val checkIn  = booking.checkInDate?.toDate()?.let  { dateFormatter.format(it) } ?: "N/A"
                        val checkOut = booking.checkOutDate?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"
                        val isPendingApproval  = booking.status.equals("PENDING_APPROVAL", ignoreCase = true)
                        val displayTenantName  = booking.tenantName.takeIf { it.isNotBlank() } ?: "Unknown Tenant"

                        MBPremiumBookingCard(
                            propertyTitle       = booking.propertyTitle.ifBlank { "Property" },
                            propertyCoverUrl    = booking.propertyCoverUrl,
                            propertyDrawableRes = uiState.bookingDrawableMap[booking.bookingId],
                            tenantName          = displayTenantName,
                            dateRange           = "$checkIn – $checkOut",
                            totalAmount         = booking.formattedTotal,
                            status              = booking.status,
                            isCancellable       = booking.isCancellable,
                            isPendingApproval   = isPendingApproval,
                            primary             = primary,
                            tertiary            = tertiary,
                            surface             = surface,
                            onSurface           = onSurface,
                            onCancel            = { viewModel.cancelBooking(booking.bookingId) },
                            onApprove           = { viewModel.approveBooking(booking.bookingId) },
                            onReject            = { viewModel.rejectBooking(booking.bookingId) }
                        )
                    }
                }
            }
        }
    }
}

// ── Booking card ──────────────────────────────────────────────────────────────
@Composable
private fun MBPremiumBookingCard(
    propertyTitle      : String,
    propertyCoverUrl   : String,
    propertyDrawableRes: Int?,
    tenantName         : String,
    dateRange          : String,
    totalAmount        : String,
    status             : String,
    isCancellable      : Boolean,
    isPendingApproval  : Boolean,
    primary            : Color,
    tertiary           : Color,
    surface            : Color,
    onSurface          : Color,
    onCancel           : () -> Unit,
    onApprove          : () -> Unit,
    onReject           : () -> Unit
) {
    var menuExpanded      by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog  by remember { mutableStateOf(false) }

    // ── Approve dialog ────────────────────────────────────────────────────────
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(Modifier.size(56.dp).clip(CircleShape).background(SuccessGreen.copy(0.12f)), Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                }
            },
            title            = { Text("Approve Booking?", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = onSurface) },
            text             = { Text("Tenant ki booking confirm kar doge?\nPayment already receive ho chuki hai.", fontSize = 13.sp, color = onSurface.copy(0.55f), lineHeight = 20.sp) },
            confirmButton    = {
                Button(onClick = { showApproveDialog = false; onApprove() }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), modifier = Modifier.fillMaxWidth()) {
                    Text("Yes, Approve", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton    = {
                OutlinedButton(onClick = { showApproveDialog = false }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Reject dialog ─────────────────────────────────────────────────────────
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(Modifier.size(56.dp).clip(CircleShape).background(ErrorRed.copy(0.12f)), Alignment.Center) {
                    Icon(Icons.Default.Cancel, null, tint = ErrorRed, modifier = Modifier.size(28.dp))
                }
            },
            title            = { Text("Reject Booking?", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = onSurface) },
            text             = { Text("Kya aap yeh booking reject karna chahte ho?\nTenant ko refund process karna hoga.", fontSize = 13.sp, color = onSurface.copy(0.55f), lineHeight = 20.sp) },
            confirmButton    = {
                Button(onClick = { showRejectDialog = false; onReject() }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), modifier = Modifier.fillMaxWidth()) {
                    Text("Yes, Reject", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton    = {
                OutlinedButton(onClick = { showRejectDialog = false }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Go Back")
                }
            }
        )
    }

    // Status config
    val (statusColor, statusBg, statusLabel) = when {
        status.equals("PENDING_APPROVAL", ignoreCase = true) -> Triple(WarningAmber,  WarningAmber.copy(0.12f),  "Awaiting")
        status.equals("CONFIRMED",        ignoreCase = true) -> Triple(SuccessGreen,  SuccessGreen.copy(0.12f),  "Confirmed")
        status.equals("PENDING",          ignoreCase = true) -> Triple(PendingBlue,   PendingBlue.copy(0.12f),   "Pending")
        status.equals("CANCELLED",        ignoreCase = true) -> Triple(ErrorRed,      ErrorRed.copy(0.12f),      "Cancelled")
        status.equals("COMPLETED",        ignoreCase = true) -> Triple(Color(0xFF888888), Color(0xFFF0F0F0),     "Completed")
        else                                                  -> Triple(Color(0xFF888888), Color(0xFFF0F0F0), status)
    }

    Card(
        modifier  = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = primary.copy(0.08f), spotColor = primary.copy(0.12f)),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(primary, tertiary))))

        Column(modifier = Modifier.padding(14.dp)) {

            // Header row: image + title + status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(primary), contentAlignment = Alignment.Center) {
                    when {
                        propertyCoverUrl.isNotBlank() -> {
                            AsyncImage(model = propertyCoverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        propertyDrawableRes != null -> {
                            Image(painter = painterResource(id = propertyDrawableRes), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        else -> {
                            Icon(Icons.Default.Home, null, tint = tertiary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(propertyTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Person, null, tint = onSurface.copy(0.4f), modifier = Modifier.size(12.dp))
                        Text(tenantName, fontSize = 12.sp, color = onSurface.copy(0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                // Status badge
                Surface(color = statusBg, shape = RoundedCornerShape(20.dp), modifier = Modifier.border(1.dp, statusColor.copy(0.35f), RoundedCornerShape(20.dp))) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                        Spacer(Modifier.width(5.dp))
                        Text(statusLabel, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                    }
                }
                // Cancel menu
                if (isCancellable) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp).clip(CircleShape).background(primary.copy(0.06f))) {
                            Icon(Icons.Default.MoreVert, null, tint = onSurface, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, modifier = Modifier.background(surface).width(170.dp)) {
                            DropdownMenuItem(
                                leadingIcon = { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ErrorRed)) },
                                text        = { Text("Cancel Booking", color = ErrorRed, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                onClick     = { menuExpanded = false; onCancel() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = onSurface.copy(0.07f))
            Spacer(Modifier.height(10.dp))

            // Date + amount
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.DateRange, null, tint = tertiary, modifier = Modifier.size(14.dp))
                    Text(dateRange, fontSize = 12.sp, color = onSurface.copy(0.55f))
                }
                Text(totalAmount, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = onSurface)
            }

            // Pending approval section
            if (isPendingApproval) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = onSurface.copy(0.07f))
                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(WarningAmber.copy(0.08f)).border(1.dp, WarningAmber.copy(0.25f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Default.Info, null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                        Text("Tenant ne payment kar di hai. Approve ya reject karo.", fontSize = 11.sp, color = WarningAmber, lineHeight = 16.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showRejectDialog = true }, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, ErrorRed), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { showApproveDialog = true }, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = tertiary)) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
