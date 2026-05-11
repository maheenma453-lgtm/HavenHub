package com.example.havenhub.screens

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

private val BookingGreen = Color(0xFF22C55E)
private val BookingRed   = Color(0xFFEF4444)
private val BookingAmber = Color(0xFFD97706)
private val BookingBlue  = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController   : NavController,
    userId          : String,
    initialTab      : Int               = 0,
    viewModel       : BookingViewModel  = hiltViewModel(),
    authViewModel   : AuthViewModel     = hiltViewModel(),
    paymentViewModel: PaymentViewModel  = hiltViewModel()
) {
    val authUiState    by authViewModel.uiState.collectAsState()
    val userRole        = authUiState.userRole
    val paymentUiState by paymentViewModel.uiState.collectAsState()

    LaunchedEffect(userId, userRole) {
        if (userRole.isNotEmpty()) viewModel.loadBookings(userId = userId, role = userRole)
    }
    LaunchedEffect(Unit) {
        if (userRole.isNotEmpty()) viewModel.loadBookings(userId = userId, role = userRole)
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            paymentViewModel.loadPaymentHistory(userId)
        }
    }

    val uiState    by viewModel.uiState.collectAsState()
    val isLandlord  = userRole.lowercase() == "landlord"

    var selectedTab    by remember { mutableIntStateOf(initialTab) }
    var focusBookingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialTab) { selectedTab = initialTab }

    LaunchedEffect(uiState.bookings, focusBookingId) {
        val target = uiState.bookings.find { it.bookingId == focusBookingId } ?: return@LaunchedEffect
        selectedTab = when (target.bookingStatus) {
            BookingStatus.PENDING          -> 0
            BookingStatus.PENDING_APPROVAL -> 1
            BookingStatus.CONFIRMED        -> 2
            BookingStatus.CHECKED_IN       -> 3
            BookingStatus.COMPLETED        -> 4
            BookingStatus.CANCELLED        -> 5
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelBookingId  by remember { mutableStateOf("") }

    val tabs     = listOf("Pending", "Awaiting", "Confirmed", "Checked In", "Completed", "Cancelled")
    val tabIcons = listOf(
        Icons.Default.HourglassEmpty,
        Icons.Default.AccessTime,
        Icons.Default.CheckCircle,
        Icons.AutoMirrored.Filled.Login,
        Icons.Default.Done,
        Icons.Default.Cancel
    )

    val filteredBookings = uiState.bookings.filter { b ->
        when (selectedTab) {
            0    -> b.bookingStatus == BookingStatus.PENDING
            1    -> b.bookingStatus == BookingStatus.PENDING_APPROVAL
            2    -> b.bookingStatus == BookingStatus.CONFIRMED
            3    -> b.bookingStatus == BookingStatus.CHECKED_IN
            4    -> b.bookingStatus == BookingStatus.COMPLETED
            5    -> b.bookingStatus == BookingStatus.CANCELLED
            else -> true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            snackbarHostState.showSnackbar("Action completed successfully")
            viewModel.clearMessages()
            if (userRole.isNotEmpty()) viewModel.loadBookings(userId = userId, role = userRole)
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
                    fontSize  = 14.sp, color = onSurfaceVariant,
                    textAlign = TextAlign.Center, lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // ✅ FIX: sirf tab cancel karo jab bookingId blank nahi ho
                        if (cancelBookingId.isNotBlank()) {
                            viewModel.cancelBooking(cancelBookingId)
                            selectedTab = 5
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
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
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

            // ── Tab Row ───────────────────────────────────────────
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
                                        1    -> b.bookingStatus == BookingStatus.PENDING_APPROVAL
                                        2    -> b.bookingStatus == BookingStatus.CONFIRMED
                                        3    -> b.bookingStatus == BookingStatus.CHECKED_IN
                                        4    -> b.bookingStatus == BookingStatus.COMPLETED
                                        5    -> b.bookingStatus == BookingStatus.CANCELLED
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

            // ── Content ───────────────────────────────────────────
            when {
                uiState.isLoading || userRole.isEmpty() -> {
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
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Your ${tabs[selectedTab].lowercase()} bookings\nwill appear here",
                                color     = onSurfaceVariant, fontSize = 13.sp,
                                textAlign = TextAlign.Center, lineHeight = 20.sp
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
                                onTap         = { navController.navigate(Screen.BookingDetails.createRoute(booking.bookingId)) },
                                onPayNow      = {
                                    // ✅ FIX: totalAmount Double ko String mein convert karo
                                    focusBookingId = booking.bookingId
                                    navController.navigate(
                                        Screen.Payment.createRoute(
                                            bookingId = booking.bookingId,
                                            payerId   = booking.tenantId,
                                            payeeId   = booking.landlordId,
                                            payerName = booking.tenantName,
                                            payeeName = booking.landlordName,
                                            amount    = booking.totalAmount

                                        )
                                    )
                                },
                                onCancel  = {
                                    // ✅ FIX: sirf set karo jab bookingId valid ho
                                    if (booking.bookingId.isNotBlank()) {
                                        cancelBookingId  = booking.bookingId
                                        showCancelDialog = true
                                    }
                                },
                                onApprove = { viewModel.updateStatusByAdmin(booking.bookingId, BookingStatus.CONFIRMED) },
                                onReject  = { viewModel.updateStatusByAdmin(booking.bookingId, BookingStatus.CANCELLED) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Booking Card ──────────────────────────────────────────────────
@Composable
private fun PremiumBookingCard(
    booking       : Booking,
    isLandlord    : Boolean,
    paymentStatus : String?,
    paymentMethod : String?,
    onTap         : () -> Unit,
    onPayNow      : () -> Unit,
    onCancel      : () -> Unit,
    onApprove     : () -> Unit,
    onReject      : () -> Unit
) {
    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val (statusColor, statusText, statusBg) = when (booking.bookingStatus) {
        BookingStatus.PENDING          -> Triple(Color(0xFFF59E0B), "Pending",           Color(0xFFFFF8E1))
        BookingStatus.PENDING_APPROVAL -> Triple(BookingAmber,      "Awaiting Approval",  Color(0xFFFFF3E0))
        BookingStatus.CONFIRMED        -> Triple(BookingGreen,       "Confirmed",          Color(0xFFE8F5E9))
        BookingStatus.CHECKED_IN       -> Triple(BookingBlue,        "Checked In",         Color(0xFFE3F2FD))
        BookingStatus.COMPLETED        -> Triple(onSurfaceVariant,   "Completed",          MaterialTheme.colorScheme.surfaceVariant)
        BookingStatus.CANCELLED        -> Triple(BookingRed,         "Cancelled",          Color(0xFFFFEBEE))
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

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MBStat(Icons.Default.NightlightRound, "Nights", "${booking.totalNights}", onSurface, onSurfaceVariant, primary)
                    MBStat(Icons.Default.People,          "Guests", "${booking.guestCount}",  onSurface, onSurfaceVariant, primary)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total", color = onSurfaceVariant, fontSize = 11.sp)
                        Text(booking.formattedTotal, fontWeight = FontWeight.ExtraBold, color = tertiary, fontSize = 17.sp)
                    }
                }

                if (paymentStatus != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (paymentStatus) {
                                    "COMPLETED" -> BookingGreen.copy(0.09f)
                                    "PENDING"   -> Color(0xFFFFF8E1)
                                    "FAILED"    -> BookingRed.copy(0.09f)
                                    else        -> Color(0xFFF5F5F5)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Payment, null,
                                tint = when (paymentStatus) {
                                    "COMPLETED" -> BookingGreen
                                    "PENDING"   -> BookingAmber
                                    "FAILED"    -> BookingRed
                                    else        -> Color(0xFF9E9E9E)
                                },
                                modifier = Modifier.size(15.dp)
                            )
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

                // TENANT PENDING: Cancel + Pay Now
                if (!isLandlord && booking.bookingStatus == BookingStatus.PENDING) {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = onCancel,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.5.dp, BookingRed),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BookingRed)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick  = onPayNow,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = primary)
                        ) {
                            Icon(Icons.Default.Payment, null, tint = tertiary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Pay Now", fontSize = 13.sp, color = tertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // LANDLORD: Approve + Reject
                if (isLandlord && (booking.bookingStatus == BookingStatus.PENDING || booking.bookingStatus == BookingStatus.PENDING_APPROVAL)) {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = onReject,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.5.dp, BookingRed),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BookingRed)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick  = onApprove,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = BookingGreen)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Approve", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // TENANT awaiting approval strip
                if (!isLandlord && booking.bookingStatus == BookingStatus.PENDING_APPROVAL) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BookingAmber.copy(0.09f)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HourglassEmpty, null, tint = BookingAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Payment received — awaiting landlord approval", fontSize = 12.sp, color = BookingAmber, fontWeight = FontWeight.Medium)
                            Text("Landlord approve kare ga tab confirmed hogi",   fontSize = 11.sp, color = BookingAmber.copy(0.7f))
                        }
                    }
                }

                // CONFIRMED strip
                if (booking.bookingStatus == BookingStatus.CONFIRMED) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BookingGreen.copy(0.08f)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = BookingGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Booking confirmed — payment received", fontSize = 12.sp, color = BookingGreen, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ── Stat pill ─────────────────────────────────────────────────────
@Composable
private fun MBStat(
    icon        : androidx.compose.ui.graphics.vector.ImageVector,
    label       : String,
    value       : String,
    onSurface   : Color,
    onSurfaceVar: Color,
    primary     : Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(primary.copy(0.06f)),
            Alignment.Center
        ) {
            Icon(icon, null, tint = primary, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, color = onSurfaceVar, fontSize = 10.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, color = onSurface, fontSize = 15.sp)
        }
    }
}