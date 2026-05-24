package com.example.havenhub.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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

// ── Semantic status colors ────────────────────────────────────────────────────
private val WarningAmber = Color(0xFFD97706)
private val PendingBlue  = Color(0xFF3B82F6)
private val SuccessGreen = Color(0xFF27AE60)
private val ErrorRed     = Color(0xFFE74C3C)

// ── Design tokens ─────────────────────────────────────────────────────────────
private val GoldPrime    = Color(0xFFD4AF37)
private val GoldLight    = Color(0xFFF5D060)
private val NavyDeep     = Color(0xFF060E20)
private val NavyPrime    = Color(0xFF0D1B3E)
private val NavyMid      = Color(0xFF1A3A6B)

private val GoldBorder   = Brush.horizontalGradient(
    listOf(GoldPrime.copy(0.9f), GoldLight.copy(0.6f), GoldPrime.copy(0.9f))
)
private val NavyGradient = Brush.verticalGradient(
    listOf(NavyDeep, NavyPrime, NavyMid)
)

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
    val isDark            = isSystemInDarkTheme()

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

    val background = MaterialTheme.colorScheme.background

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyGradient)
                    .statusBarsPadding()
            ) {
                Box(
                    Modifier.size(120.dp).align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-40).dp)
                        .clip(CircleShape).background(GoldPrime.copy(0.06f))
                )
                Box(
                    Modifier.fillMaxWidth().height(2.dp)
                        .background(GoldBorder)
                        .align(Alignment.BottomCenter)
                )
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GoldPrime)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Manage Bookings",
                            color      = Color.White,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            "${uiState.bookings.size} total bookings",
                            color    = GoldPrime.copy(0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Filter chips ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyGradient)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusList) { status ->
                        val selected = selectedStatus == status
                        val count    = statusCounts[status] ?: 0
                        val label    = if (status == "Pending_Approval") "Awaiting" else status

                        val chipBg = if (selected)
                            Brush.linearGradient(listOf(GoldPrime, GoldLight, GoldPrime))
                        else
                            Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.06f)))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(chipBg)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    brush = if (selected) GoldBorder
                                    else Brush.horizontalGradient(listOf(Color.White.copy(0.2f), Color.White.copy(0.2f))),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedStatus = status }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize   = 12.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                                    color      = if (selected) NavyDeep else Color.White
                                )
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (selected) NavyDeep.copy(0.3f) else GoldPrime.copy(0.3f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$count",
                                            fontSize   = 9.sp,
                                            color      = if (selected) NavyDeep else GoldPrime,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrime, strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(NavyPrime, NavyMid)))
                                .border(1.5.dp, GoldBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(GoldPrime))
                                Text(
                                    "${filteredBookings.size} booking${if (filteredBookings.size != 1) "s" else ""} found",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = GoldPrime
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
                            propertyTitle       = booking.propertyTitle.ifBlank { "Property" },
                            propertyCoverUrl    = booking.propertyCoverUrl,
                            propertyDrawableRes = uiState.bookingDrawableMap[booking.bookingId],
                            tenantName          = displayTenantName,
                            dateRange           = "$checkIn – $checkOut",
                            totalAmount         = booking.formattedTotal,
                            status              = booking.status,
                            isCancellable       = booking.isCancellable,
                            isPendingApproval   = isPendingApproval,
                            isDark              = isDark,
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

// ── Premium Booking Card ──────────────────────────────────────────────────────
@Composable
private fun PremiumBookingCard(
    propertyTitle      : String,
    propertyCoverUrl   : String,
    propertyDrawableRes: Int?,
    tenantName         : String,
    dateRange          : String,
    totalAmount        : String,
    status             : String,
    isCancellable      : Boolean,
    isPendingApproval  : Boolean,
    isDark             : Boolean,
    onCancel           : () -> Unit,
    onApprove          : () -> Unit,
    onReject           : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val shimmerAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(200),
        label = "shimmer"
    )

    val cardBg = if (isDark) NavyPrime else Color.White
    val cardBgEnd = if (isDark) NavyDeep else Color(0xFFF9FAFB)
    val titleColor = if (isDark) Color.White else Color(0xFF111827)
    val subtitleColor = if (isDark) Color.White.copy(0.55f) else Color(0xFF6B7280)
    val dateColor = if (isDark) Color.White.copy(0.6f) else Color(0xFF6B7280)
    val dividerColor = if (isDark) GoldPrime.copy(0.15f) else Color(0xFFE5E7EB)
    val iconBg = if (isDark) GoldPrime.copy(0.15f) else Color(0xFFF3F4F6)
    val amountBg = if (isDark) GoldPrime.copy(0.12f) else Color(0xFFFEF9EC)
    val amountBorder = if (isDark) GoldBorder else Brush.horizontalGradient(
        listOf(GoldPrime.copy(0.5f), GoldLight.copy(0.4f), GoldPrime.copy(0.5f))
    )
    val cardBorder = if (isDark) GoldBorder else Brush.horizontalGradient(
        listOf(GoldPrime.copy(0.6f), GoldLight.copy(0.3f), GoldPrime.copy(0.6f))
    )
    val cardGradient = Brush.verticalGradient(listOf(cardBg, cardBgEnd))

    val dialogContainerColor = if (isDark) NavyPrime else Color.White
    val dialogTextColor = if (isDark) Color.White else Color(0xFF111827)
    val dialogSubtextColor = if (isDark) Color.White.copy(0.6f) else Color(0xFF6B7280)

    // ── Approve Dialog ────────────────────────────────────────────────────────
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = dialogContainerColor,
            icon = {
                Box(
                    Modifier.size(60.dp).clip(CircleShape)
                        .background(SuccessGreen.copy(0.15f))
                        .border(1.5.dp, SuccessGreen.copy(0.4f), CircleShape),
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    "Approve Booking?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = dialogTextColor
                )
            },
            text = {
                // ✅ FIXED: English text
                Text(
                    "Tenant's payment has been received.\nDo you want to confirm this booking?",
                    fontSize = 13.sp, color = dialogSubtextColor, lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showApproveDialog = false; onApprove() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, Approve", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showApproveDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldPrime.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = GoldPrime)
                }
            }
        )
    }

    // ── Reject Dialog ─────────────────────────────────────────────────────────
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = dialogContainerColor,
            icon = {
                Box(
                    Modifier.size(60.dp).clip(CircleShape)
                        .background(ErrorRed.copy(0.15f))
                        .border(1.5.dp, ErrorRed.copy(0.4f), CircleShape),
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        null,
                        tint = ErrorRed,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    "Reject Booking?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = dialogTextColor
                )
            },
            text = {
                // ✅ FIXED: English text
                Text(
                    "Are you sure you want to reject this booking?\nA refund will need to be processed for the tenant.",
                    fontSize = 13.sp, color = dialogSubtextColor, lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showRejectDialog = false; onReject() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, Reject", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRejectDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldPrime.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back", color = GoldPrime)
                }
            }
        )
    }

    // Status config
    val (statusColor, statusBg, statusLabel) = when {
        status.equals("PENDING_APPROVAL", ignoreCase = true) -> Triple(
            WarningAmber,
            WarningAmber.copy(0.12f),
            "Awaiting"
        )

        status.equals("CONFIRMED", ignoreCase = true) -> Triple(
            SuccessGreen,
            SuccessGreen.copy(0.12f),
            "Confirmed"
        )

        status.equals("PENDING", ignoreCase = true) -> Triple(
            PendingBlue,
            PendingBlue.copy(0.12f),
            "Pending"
        )

        status.equals("CANCELLED", ignoreCase = true) -> Triple(
            ErrorRed,
            ErrorRed.copy(if (isDark) 0.12f else 0.08f),
            "Cancelled"
        )

        status.equals("COMPLETED", ignoreCase = true) -> Triple(
            Color(0xFF888888),
            if (isDark) Color(0xFF2A2A2A) else Color(0xFFF3F4F6),
            "Completed"
        )

        else -> Triple(
            Color(0xFF888888),
            if (isDark) Color(0xFF2A2A2A) else Color(0xFFF3F4F6),
            status
        )
    }

    val shadowAmb = if (isDark) GoldPrime.copy(0.15f) else Color(0x1A000000)
    val shadowSpt = if (isDark) GoldPrime.copy(0.20f) else Color(0x26000000)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                if (isDark) 8.dp else 4.dp,
                RoundedCornerShape(20.dp),
                ambientColor = shadowAmb,
                spotColor = shadowSpt
            )
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isDark) 2.dp else 1.5.dp,
                brush = cardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .background(cardGradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { isPressed = true }
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(GoldBorder).align(Alignment.TopCenter))

        if (shimmerAlpha > 0f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(
                            GoldPrime.copy(0f),
                            GoldPrime.copy(0.06f * shimmerAlpha),
                            GoldPrime.copy(0f)
                        )
                    )
                )
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) NavyMid else Color(0xFFEEF2FF))
                        .border(1.5.dp, cardBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        propertyCoverUrl.isNotBlank() -> {
                            AsyncImage(
                                model = propertyCoverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                            )
                        }

                        propertyDrawableRes != null -> {
                            Image(
                                painter = painterResource(id = propertyDrawableRes),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                            )
                        }

                        else -> {
                            Icon(
                                Icons.Default.Home,
                                null,
                                tint = GoldPrime,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        propertyTitle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            tint = GoldPrime.copy(0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            tenantName,
                            fontSize = 12.sp,
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(
                        1.dp,
                        statusColor.copy(0.35f),
                        RoundedCornerShape(20.dp)
                    ).widthIn(min = 80.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            statusLabel,
                            fontSize = 10.sp,
                            color = statusColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (isCancellable) {
                    Spacer(Modifier.width(6.dp))
                    Box {
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                .background(GoldPrime.copy(0.12f))
                                .border(1.dp, GoldBorder, CircleShape)
                                .clickable { menuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                null,
                                tint = GoldPrime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .background(if (isDark) NavyPrime else Color.White)
                                .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                                .width(180.dp)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        Modifier.size(8.dp).clip(CircleShape).background(ErrorRed)
                                    )
                                },
                                text = {
                                    Text(
                                        "Cancel Booking",
                                        color = ErrorRed,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = { menuExpanded = false; onCancel() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(Modifier.height(12.dp))

            // ── Date + amount row ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(iconBg),
                        Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            null,
                            tint = GoldPrime,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(dateRange, fontSize = 12.sp, color = dateColor)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(amountBg)
                        .border(1.dp, amountBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        totalAmount,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrime
                    )
                }
            }

            // ── Pending approval section ──────────────────────────────────────
            if (isPendingApproval) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = dividerColor)
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarningAmber.copy(0.10f))
                        .border(1.dp, WarningAmber.copy(0.30f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = WarningAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        // ✅ FIXED: English text
                        Text(
                            "Tenant has made the payment. Please approve or reject the booking.",
                            fontSize = 11.sp,
                            color = WarningAmber,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, ErrorRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = { showApproveDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrime,
                            contentColor = NavyDeep
                        )
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(300)
            isPressed = false
        }
    }
}