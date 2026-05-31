package com.example.havenhub.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel
import com.example.havenhub.viewmodel.PaymentViewModel
import com.google.firebase.auth.FirebaseAuth

private val BookingGreen  = Color(0xFF22C55E)
private val BookingRed    = Color(0xFFEF4444)
private val BookingAmber  = Color(0xFFD97706)
private val BookingBlue   = Color(0xFF3B82F6)
private val BookingPurple = Color(0xFF8B5CF6)
private val BookingNavy   = Color(0xFF1A3A6B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController   : NavController,
    userId          : String,
    initialTab      : Int              = 0,
    viewModel       : BookingViewModel = hiltViewModel(),
    authViewModel   : AuthViewModel    = hiltViewModel(),
    paymentViewModel: PaymentViewModel = hiltViewModel()
) {
    val authUiState    by authViewModel.uiState.collectAsState()
    val userRole        = authUiState.userRole
    val paymentUiState by paymentViewModel.uiState.collectAsState()
    val uiState        by viewModel.uiState.collectAsState()
    val isLandlord      = userRole.lowercase() == "landlord"

    val resolvedUserId = authUiState.currentUser?.uid
        ?: FirebaseAuth.getInstance().currentUser?.uid
        ?: userId

    LaunchedEffect(resolvedUserId, userRole, authUiState.isAuthReady) {
        if (resolvedUserId.isBlank() || !authUiState.isAuthReady) return@LaunchedEffect
        val effectiveRole = userRole.ifBlank { "tenant" }
        viewModel.loadBookings(userId = resolvedUserId, role = effectiveRole)
    }

    LaunchedEffect(resolvedUserId, userRole, authUiState.isAuthReady) {
        if (resolvedUserId.isBlank() || !authUiState.isAuthReady) return@LaunchedEffect
        val effectiveRole = userRole.ifBlank { "tenant" }
        if (effectiveRole.lowercase() == "landlord") {
            paymentViewModel.loadLandlordPayments(resolvedUserId)
        } else {
            paymentViewModel.loadPaymentHistory(resolvedUserId)
        }
    }

    var selectedTab    by remember { mutableIntStateOf(initialTab) }
    var focusBookingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialTab) { selectedTab = initialTab }

    LaunchedEffect(uiState.bookings, focusBookingId) {
        val target = uiState.bookings.find { it.bookingId == focusBookingId } ?: return@LaunchedEffect
        selectedTab = when (target.bookingStatus) {
            BookingStatus.PENDING                -> 0
            BookingStatus.DEPOSIT_PAID           -> 1
            BookingStatus.CHECKED_IN             -> 2
            BookingStatus.PENDING_APPROVAL       -> 3
            BookingStatus.CONFIRMED              -> 4
            BookingStatus.COMPLETED              -> 5
            BookingStatus.CANCELLED              -> 6
            BookingStatus.AWAITING_FINAL_PAYMENT -> 2
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelBookingId  by remember { mutableStateOf("") }

    val tabs     = listOf("Pending", "Deposit Paid", "Checked In", "Awaiting", "Confirmed", "Completed", "Cancelled")
    val tabIcons = listOf(
        Icons.Default.HourglassEmpty,
        Icons.Default.AccountBalanceWallet,
        Icons.AutoMirrored.Filled.Login,
        Icons.Default.AccessTime,
        Icons.Default.CheckCircle,
        Icons.Default.Done,
        Icons.Default.Cancel
    )

    val filteredBookings = uiState.bookings.filter { b ->
        when (selectedTab) {
            0    -> b.bookingStatus == BookingStatus.PENDING
            1    -> b.bookingStatus == BookingStatus.DEPOSIT_PAID
            2    -> b.bookingStatus == BookingStatus.CHECKED_IN || b.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT
            3    -> b.bookingStatus == BookingStatus.PENDING_APPROVAL
            4    -> b.bookingStatus == BookingStatus.CONFIRMED
            5    -> b.bookingStatus == BookingStatus.COMPLETED
            6    -> b.bookingStatus == BookingStatus.CANCELLED
            else -> true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            snackbarHostState.showSnackbar("Action completed successfully")
            viewModel.clearMessages()
            viewModel.forceRefreshBookings()
        }
    }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BookingRed.copy(0.1f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Cancel, null, tint = BookingRed, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Cancel Booking?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = onSurface) },
            text  = {
                Text(
                    "Are you sure you want to cancel this booking?\nThis action cannot be undone.",
                    fontSize = 14.sp, color = onSurfaceVariant,
                    textAlign = TextAlign.Center, lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelBookingId.isNotBlank()) {
                            viewModel.cancelBooking(cancelBookingId)
                            selectedTab = 6
                        }
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BookingRed),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCancelDialog = false },
                    border  = BorderStroke(1.dp, primary),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text("Keep Booking", color = primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
            ) {
                Row(
                    Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = onPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isLandlord) "Booking Requests" else "My Bookings",
                            fontWeight = FontWeight.Bold, color = onPrimary, fontSize = 18.sp
                        )
                        Text("${uiState.bookings.size} total bookings", color = onPrimary.copy(0.6f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            Modifier.fillMaxSize().background(background).padding(top = paddingValues.calculateTopPadding())
        ) {
            Box(Modifier.fillMaxWidth().background(surface).padding(vertical = 4.dp)) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = surface,
                    contentColor     = onSurface,
                    edgePadding      = 12.dp,
                    indicator        = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            Box(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]).height(3.dp)
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(tertiary)
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick  = { selectedTab = index },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    tabIcons[index], null,
                                    tint     = if (isSelected) tertiary else onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    title,
                                    color      = if (isSelected) onSurface else onSurfaceVariant,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                val count = uiState.bookings.count { b ->
                                    when (index) {
                                        0    -> b.bookingStatus == BookingStatus.PENDING
                                        1    -> b.bookingStatus == BookingStatus.DEPOSIT_PAID
                                        2    -> b.bookingStatus == BookingStatus.CHECKED_IN || b.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT
                                        3    -> b.bookingStatus == BookingStatus.PENDING_APPROVAL
                                        4    -> b.bookingStatus == BookingStatus.CONFIRMED
                                        5    -> b.bookingStatus == BookingStatus.COMPLETED
                                        6    -> b.bookingStatus == BookingStatus.CANCELLED
                                        else -> false
                                    }
                                }
                                if (count > 0) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) tertiary else onSurfaceVariant.copy(0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        Alignment.Center
                                    ) {
                                        Text(
                                            "$count",
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = if (isSelected) primary else onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when {
                !authUiState.isAuthReady || uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = tertiary, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                    }
                }

                filteredBookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(100.dp).clip(RoundedCornerShape(28.dp)).background(primary.copy(0.06f)),
                                Alignment.Center
                            ) {
                                Icon(tabIcons[selectedTab], null, tint = primary.copy(0.25f), modifier = Modifier.size(46.dp))
                            }
                            Spacer(Modifier.height(20.dp))
                            Text("No ${tabs[selectedTab]} Bookings", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = onSurface)
                            Spacer(Modifier.height(6.6.dp))
                            Text(
                                "Your ${tabs[selectedTab].lowercase()} bookings\nwill appear here",
                                color = onSurfaceVariant, fontSize = 13.sp,
                                textAlign = TextAlign.Center, lineHeight = 20.dp.value.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(items = filteredBookings, key = { it.bookingId }) { booking ->
                            val bookingPayment = paymentUiState.paymentHistory
                                .find { it.bookingId == booking.bookingId }

                            PremiumBookingCard(
                                booking       = booking,
                                isLandlord    = isLandlord,
                                paymentStatus = bookingPayment?.status,
                                paymentMethod = bookingPayment?.paymentMethod,
                                onTap         = {
                                    navController.navigate(Screen.BookingDetails.createRoute(booking.bookingId))
                                },
                                onPayNow = {
                                    focusBookingId = booking.bookingId
                                    navController.navigate(
                                        Screen.Payment.createRoute(
                                            bookingId   = booking.bookingId,
                                            payerId     = booking.tenantId,
                                            payeeId     = booking.landlordId,
                                            payerName   = booking.tenantName,
                                            payeeName   = booking.landlordName,
                                            amount      = booking.totalAmount,
                                            paymentType = "FULL" // Simple booking — 100% payment
                                        )
                                    )
                                },
                                onPayRemaining = {
                                    focusBookingId = booking.bookingId
                                    val remainingAmt = when {
                                        booking.remainingAmount > 0 -> booking.remainingAmount
                                        booking.depositAmount > 0   -> booking.totalAmount - booking.depositAmount
                                        else                        -> booking.totalAmount * 0.8
                                    }
                                    navController.navigate(
                                        Screen.Payment.createRoute(
                                            bookingId   = booking.bookingId,
                                            payerId     = booking.tenantId,
                                            payeeId     = booking.landlordId,
                                            payerName   = booking.tenantName,
                                            payeeName   = booking.landlordName,
                                            amount      = remainingAmt,
                                            paymentType = "REMAINING" // ✅ FIX: Pre-booking — 80% remaining payment
                                        )
                                    )
                                },
                                onCancel = {
                                    if (booking.bookingId.isNotBlank()) {
                                        cancelBookingId  = booking.bookingId
                                        showCancelDialog = true
                                    }
                                },
                                onApprove     = { viewModel.updateStatusByAdmin(booking.bookingId, BookingStatus.CONFIRMED) },
                                onReject      = { viewModel.updateStatusByAdmin(booking.bookingId, BookingStatus.CANCELLED) },
                                onMarkCheckIn = { viewModel.markCheckedIn(booking.bookingId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBookingCard(
    booking        : Booking,
    isLandlord     : Boolean,
    paymentStatus  : String?,
    paymentMethod  : String?,
    onTap          : () -> Unit,
    onPayNow       : () -> Unit,
    onPayRemaining : () -> Unit,
    onCancel       : () -> Unit,
    onApprove      : () -> Unit,
    onReject       : () -> Unit,
    onMarkCheckIn  : () -> Unit
) {
    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val (statusColor, statusText, statusBg) = when (booking.bookingStatus) {
        BookingStatus.PENDING                -> Triple(Color(0xFFF59E0B), "Pending",          Color(0xFFFFF8E1))
        BookingStatus.DEPOSIT_PAID           -> Triple(BookingNavy,       "Deposit Paid",      Color(0xFFE8EEF8))
        BookingStatus.CHECKED_IN             -> Triple(BookingBlue,       "Checked In",        Color(0xFFE3F2FD))
        BookingStatus.PENDING_APPROVAL       -> Triple(BookingAmber,      "Awaiting Approval", Color(0xFFFFF3E0))
        BookingStatus.CONFIRMED              -> Triple(BookingGreen,      "Confirmed",         Color(0xFFE8F5E9))
        BookingStatus.COMPLETED              -> Triple(onSurfaceVariant,  "Completed",         MaterialTheme.colorScheme.surfaceVariant)
        BookingStatus.CANCELLED              -> Triple(BookingRed,        "Cancelled",         Color(0xFFFFEBEE))
        BookingStatus.AWAITING_FINAL_PAYMENT -> Triple(BookingBlue,       "Awaiting Payment",  Color(0xFFE3F2FD))
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onTap() },
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(listOf(statusColor, statusColor.copy(0.35f)))))

            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(booking.propertyTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(statusBg).padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                if (isLandlord && booking.tenantName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = onSurfaceVariant, modifier = Modifier.size(13.dp))
                        Text("  Tenant: ${booking.tenantName}", color = onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = tertiary, modifier = Modifier.size(13.dp))
                    Text("  ${booking.propertyAddress}", color = onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))
                Spacer(Modifier.height(12.dp))

                val trueTotal = when {
                    booking.remainingAmount > 0 && booking.depositAmount > 0 ->
                        booking.depositAmount + booking.remainingAmount
                    booking.totalAmount > 0 -> booking.totalAmount
                    else -> 0.0
                }
                val displayTotal = if (trueTotal > 0)
                    "PKR ${"%,.0f".format(trueTotal)}"
                else
                    booking.formattedTotal

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MBStat(Icons.Default.NightlightRound, "Nights", "${booking.totalNights}", onSurface, onSurfaceVariant, primary)
                    MBStat(Icons.Default.People,          "Guests", "${booking.guestCount}",  onSurface, onSurfaceVariant, primary)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total", color = onSurfaceVariant, fontSize = 11.sp)
                        Text(displayTotal, fontWeight = FontWeight.ExtraBold, color = tertiary, fontSize = 17.sp)
                    }
                }

                if (booking.depositAmount > 0) {
                    Spacer(Modifier.height(12.dp))

                    val displayRemainingAmt = when {
                        booking.remainingAmount > 0 -> booking.remainingAmount
                        booking.depositAmount > 0   -> booking.totalAmount - booking.depositAmount
                        else                        -> booking.totalAmount * 0.8
                    }

                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF8F9FB)).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = BookingGreen, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Deposit paid (20%)", color = onSurfaceVariant, fontSize = 12.sp)
                            }
                            Text("PKR ${"%,.0f".format(booking.depositAmount)}", color = BookingGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        val remainingPaid = booking.bookingStatus == BookingStatus.CONFIRMED
                                || booking.bookingStatus == BookingStatus.COMPLETED
                                || booking.bookingStatus == BookingStatus.PENDING_APPROVAL
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (remainingPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    null,
                                    tint     = if (remainingPaid) BookingGreen else BookingAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (remainingPaid) "Remaining paid (80%)" else "Remaining due (80%)",
                                    color    = onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                "PKR ${"%,.0f".format(displayRemainingAmt)}",
                                color      = if (remainingPaid) BookingGreen else BookingAmber,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (paymentStatus != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(when (paymentStatus) {
                                "COMPLETED" -> BookingGreen.copy(0.09f)
                                "PENDING"   -> Color(0xFFFFF8E1)
                                "FAILED"    -> BookingRed.copy(0.09f)
                                else        -> Color(0xFFF5F5F5)
                            }).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, null, tint = when (paymentStatus) {
                                "COMPLETED" -> BookingGreen
                                "PENDING"   -> BookingAmber
                                "FAILED"    -> BookingRed
                                else        -> Color(0xFF9E9E9E)
                            }, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Payment: $paymentStatus",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = when (paymentStatus) {
                                    "COMPLETED" -> BookingGreen
                                    "PENDING"   -> BookingAmber
                                    "FAILED"    -> BookingRed
                                    else        -> Color(0xFF9E9E9E)
                                }
                            )
                        }
                        if (!paymentMethod.isNullOrEmpty()) {
                            Text(paymentMethod, fontSize = 11.sp, color = Color(0xFF9E9E9E), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ── ACTION BUTTONS ────────────────────────────────────────────
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // ── Tenant Controls ───────────────────────────────────────
                    if (!isLandlord) {
                        when (booking.bookingStatus) {
                            BookingStatus.PENDING -> {
                                OutlinedButton(
                                    onClick = onCancel,
                                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = BookingRed),
                                    border  = BorderStroke(1.dp, BookingRed)
                                ) {
                                    Text("Cancel Booking", fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = onPayNow,
                                    colors  = ButtonDefaults.buttonColors(containerColor = BookingGreen)
                                ) {
                                    Text("Pay Full Amount", fontSize = 12.sp, color = Color.White)
                                }
                            }
                            BookingStatus.CHECKED_IN,
                            BookingStatus.AWAITING_FINAL_PAYMENT -> {
                                Button(
                                    onClick = onPayRemaining,
                                    colors  = ButtonDefaults.buttonColors(containerColor = primary)
                                ) {
                                    Text("Pay Remaining 80%", fontSize = 12.sp, color = Color.White)
                                }
                            }
                            else -> { /* No action */ }
                        }
                    }
                    // ── Landlord Controls ─────────────────────────────────────
                    else {
                        when (booking.bookingStatus) {
                            BookingStatus.DEPOSIT_PAID -> {
                                Button(
                                    onClick = onMarkCheckIn,
                                    colors  = ButtonDefaults.buttonColors(containerColor = BookingBlue),
                                    shape   = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Mark Checked In", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            BookingStatus.PENDING_APPROVAL -> {
                                OutlinedButton(
                                    onClick = onReject,
                                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = BookingRed),
                                    border  = BorderStroke(1.dp, BookingRed)
                                ) {
                                    Text("Reject", fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = onApprove,
                                    colors  = ButtonDefaults.buttonColors(containerColor = BookingGreen)
                                ) {
                                    Text("Approve & Confirm", fontSize = 12.sp, color = Color.White)
                                }
                            }
                            else -> { /* No action */ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MBStat(
    icon            : androidx.compose.ui.graphics.vector.ImageVector,
    label           : String,
    value           : String,
    onSurface       : Color,
    onSurfaceVariant: Color,
    tint            : Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(0.08f)), Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, color = onSurfaceVariant, fontSize = 10.sp)
            Text(value, fontWeight = FontWeight.Bold, color = onSurface, fontSize = 13.sp)
        }
    }
}