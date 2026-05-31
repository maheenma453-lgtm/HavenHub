package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.BookingViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private val BN  = Color(0xFF0B1829)
private val BG  = Color(0xFFD4AF37)
private val BBg = Color(0xFFF0F3F8)
private val BW  = Color(0xFFFFFFFF)
private val BM  = Color(0xFF8A9BB0)
private val BL  = Color(0xFFE8EDF4)

private data class BStatusTheme(
    val fg: Color, val bg: Color, val icon: ImageVector, val label: String, val desc: String
)

private fun bTheme(s: BookingStatus) = when (s) {
    BookingStatus.PENDING                -> BStatusTheme(Color(0xFFB45309), Color(0xFFFEF3C7), Icons.Default.HourglassEmpty,        "Pending",               "Waiting for landlord approval.")
    BookingStatus.PENDING_APPROVAL       -> BStatusTheme(Color(0xFF6D28D9), Color(0xFFEDE9FE), Icons.Default.AccessTime,             "Awaiting Approval",     "Payment received — under review.")
    BookingStatus.CONFIRMED              -> BStatusTheme(Color(0xFF15803D), Color(0xFFDCFCE7), Icons.Default.CheckCircle,            "Confirmed",             "Landlord accepted your booking.")
    BookingStatus.DEPOSIT_PAID           -> BStatusTheme(Color(0xFF0369A1), Color(0xFFE0F2FE), Icons.Default.Savings,               "Deposit Paid",          "20% deposit paid. Awaiting check-in.")
    BookingStatus.CHECKED_IN             -> BStatusTheme(Color(0xFF1D4ED8), Color(0xFFDBEAFE), Icons.AutoMirrored.Filled.Login,      "Checked In",            "Guest is at the property.")
    BookingStatus.AWAITING_FINAL_PAYMENT -> BStatusTheme(Color(0xFFB45309), Color(0xFFFEF3C7), Icons.Default.AccountBalanceWallet,  "Final Payment Pending", "Pay remaining 80% on arrival.")
    BookingStatus.COMPLETED              -> BStatusTheme(Color(0xFF374151), Color(0xFFF3F4F6), Icons.Default.Done,                  "Completed",             "Stay completed successfully.")
    BookingStatus.CANCELLED              -> BStatusTheme(Color(0xFFB91C1C), Color(0xFFFEE2E2), Icons.Default.Cancel,                "Cancelled",             "Booking has been cancelled.")
}

private fun bfmt(d: Date?) = if (d == null) "—" else
    try { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(d) } catch (e: Exception) { "—" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    navController        : NavController,
    bookingId            : String,
    isCurrentUserLandlord: Boolean          = false,
    viewModel            : BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val booking  = uiState.currentBooking

    var showCancel   by remember { mutableStateOf(false) }
    var showReject   by remember { mutableStateOf(false) }
    var showApprove  by remember { mutableStateOf(false) }
    var showCheckIn  by remember { mutableStateOf(false) }
    var selectedTab  by remember { mutableIntStateOf(0) }

    var tenantName   by remember { mutableStateOf("...") }
    var hostName     by remember { mutableStateOf("...") }

    LaunchedEffect(booking?.tenantId) {
        val currentBooking = booking ?: return@LaunchedEffect
        val tid = currentBooking.tenantId
        tenantName = when {
            currentBooking.tenantName.isNotBlank() && currentBooking.tenantName != currentBooking.propertyTitle -> currentBooking.tenantName
            tid.isNotBlank() -> {
                try {
                    val doc = FirebaseFirestore.getInstance().collection("users").document(tid).get().await()
                    doc.getString("fullName") ?: doc.getString("name") ?: doc.getString("displayName")
                    ?: currentBooking.tenantEmail.takeIf { it.isNotBlank() } ?: "—"
                } catch (e: Exception) { currentBooking.tenantEmail.takeIf { it.isNotBlank() } ?: "—" }
            }
            currentBooking.tenantEmail.isNotBlank() -> currentBooking.tenantEmail
            else -> "—"
        }
    }

    LaunchedEffect(booking?.propertyId) {
        val currentBooking = booking ?: return@LaunchedEffect
        hostName = try {
            val propertyDoc = FirebaseFirestore.getInstance()
                .collection("properties")
                .document(currentBooking.propertyId)
                .get()
                .await()
            propertyDoc.getString("ownerName")
                ?: currentBooking.landlordName
                ?: "—"
        } catch (e: Exception) {
            currentBooking.landlordName.ifBlank { "—" }
        }
    }

    LaunchedEffect(bookingId) { viewModel.loadBookingById(bookingId) }
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) { navController.popBackStack(); viewModel.clearMessages() }
    }

    // ── Dialogs ───────────────────────────────────────────────────
    if (showCancel) BDialog(
        "Cancel Booking", "Cancel this booking? This cannot be undone.",
        "Yes, Cancel", Color(0xFFDC2626), Icons.Default.Cancel, Color(0xFFFEE2E2), Color(0xFFDC2626),
        onConfirm = { booking?.bookingId?.let { viewModel.cancelBooking(it) }; showCancel = false },
        onDismiss = { showCancel = false }
    )
    if (showReject) BDialog(
        "Reject Request", "Reject this booking? Tenant will be notified.",
        "Reject", Color(0xFFDC2626), Icons.Default.ThumbDown, Color(0xFFFEE2E2), Color(0xFFDC2626),
        onConfirm = { booking?.bookingId?.let { viewModel.updateStatusByAdmin(it, BookingStatus.CANCELLED) }; showReject = false },
        onDismiss = { showReject = false }
    )
    if (showApprove) BDialog(
        "Approve Booking", "Approve this booking? Tenant will be notified.",
        "Approve", Color(0xFF16A34A), Icons.Default.ThumbUp, Color(0xFFDCFCE7), Color(0xFF16A34A),
        onConfirm = {
            booking?.bookingId?.let { bid ->
                val newStatus = if (booking.isPreBooking)
                    BookingStatus.DEPOSIT_PAID
                else
                    BookingStatus.CONFIRMED
                viewModel.updateStatusByAdmin(bid, newStatus)
            }
            showApprove = false
        },
        onDismiss = { showApprove = false }
    )
    if (showCheckIn) BDialog(
        "Mark Checked In", "Confirm tenant has arrived? Remaining payment will be due.",
        "Mark Checked In", Color(0xFF1D4ED8), Icons.Default.Home, Color(0xFFDBEAFE), Color(0xFF1D4ED8),
        onConfirm = { booking?.bookingId?.let { viewModel.markCheckedIn(it) }; showCheckIn = false },
        onDismiss = { showCheckIn = false }
    )
    // ✅ FIX: showFinalPay dialog removed — tenant app se khud 80% pay karta hai

    Scaffold(containerColor = BBg) { pad ->
        val currentBooking = booking
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                CircularProgressIndicator(color = BN, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
            }
            currentBooking == null -> Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.SearchOff, null, tint = BM, modifier = Modifier.size(48.dp))
                    Text("Booking not found", color = BN, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            else -> {
                val t            = bTheme(currentBooking.bookingStatus)
                val isPending    = currentBooking.bookingStatus == BookingStatus.PENDING
                val isPreBooking = currentBooking.isPreBooking ||
                        currentBooking.depositAmount > 0 ||
                        currentBooking.bookingStatus == BookingStatus.DEPOSIT_PAID ||
                        currentBooking.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT ||
                        currentBooking.bookingStatus == BookingStatus.CHECKED_IN

                Column(Modifier.fillMaxSize()) {

                    // TOP BAR
                    Column(Modifier.fillMaxWidth().background(BN).statusBarsPadding()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = BW.copy(alpha = 0.8f))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Booking Details", fontWeight = FontWeight.SemiBold, color = BW, fontSize = 16.sp)
                                Text("#${currentBooking.bookingId.take(8).uppercase()}", color = BW.copy(alpha = 0.4f), fontSize = 11.sp)
                            }
                            if (isPreBooking) {
                                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(BG.copy(0.2f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("Pre-Booking", color = BG, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(t.bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(t.icon, null, tint = t.fg, modifier = Modifier.size(12.dp))
                                    Text(t.label, color = t.fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        val tabs = listOf("Details", "Payment", "Info")
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor   = BN,
                            contentColor     = BW,
                            indicator = { pos ->
                                if (selectedTab < pos.size)
                                    Box(Modifier.tabIndicatorOffset(pos[selectedTab]).height(2.dp).padding(horizontal = 20.dp).clip(RoundedCornerShape(2.dp)).background(BG))
                            },
                            divider = {}
                        ) {
                            tabs.forEachIndexed { i, tab ->
                                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, modifier = Modifier.height(42.dp)) {
                                    Text(tab, fontSize = 13.sp, color = if (selectedTab == i) BW else BW.copy(alpha = 0.4f), fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // TAB CONTENT
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {

                            // ── TAB 0: Details ──────────────────────────────────
                            0 -> {
                                BStrip(t.icon, t.desc, t.fg, t.bg)
                                BCard("Property", Icons.Default.Apartment) {
                                    BRow("Property Name", currentBooking.propertyTitle)
                                    BRow("Address",       currentBooking.propertyAddress)
                                }
                                BCard("Stay Dates", Icons.Default.CalendarMonth) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFDCFCE7)).padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                                                Text("Check-in", color = Color(0xFF15803D), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Text(bfmt(currentBooking.checkInDate?.toDate()), color = BN, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFEE2E2)).padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                                                Text("Check-out", color = Color(0xFFB91C1C), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Text(bfmt(currentBooking.checkOutDate?.toDate()), color = BN, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BPill(Icons.Default.NightsStay, "${currentBooking.totalNights} Night(s)")
                                        BPill(Icons.Default.Group, "${currentBooking.guestCount} Guest(s)")
                                    }
                                }
                            }

                            // ── TAB 1: Payment ──────────────────────────────────
                            1 -> {
                                if (isPreBooking) {
                                    BCard("Pre-Booking Payment", Icons.Default.AccountBalanceWallet) {

                                        val totalCost = when {
                                            currentBooking.depositAmount > 0 && currentBooking.remainingAmount > 0 ->
                                                currentBooking.depositAmount + currentBooking.remainingAmount
                                            currentBooking.totalAmount > 0 -> currentBooking.totalAmount
                                            else -> currentBooking.subtotal
                                        }
                                        val depositAmt = when {
                                            currentBooking.depositAmount > 0 -> currentBooking.depositAmount
                                            else -> totalCost * 0.2
                                        }
                                        val remainingAmt = when {
                                            currentBooking.remainingAmount > 0 -> currentBooking.remainingAmount
                                            currentBooking.depositAmount > 0   -> totalCost - currentBooking.depositAmount
                                            else -> totalCost * 0.8
                                        }

                                        BRow("Price / Night", "PKR ${"%,.0f".format(currentBooking.pricePerNight)}")
                                        BRow("Total Nights",  "${currentBooking.totalNights} nights")
                                        BRow("Total Property Cost", "PKR ${"%,.0f".format(totalCost)}")
                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(color = BL)
                                        Spacer(Modifier.height(8.dp))

                                        // Deposit paid box
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(12.dp)
                                        ) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text("Deposit Paid (20%)", fontWeight = FontWeight.Bold, color = Color(0xFF15803D), fontSize = 13.sp)
                                                    Text("Already paid ✓", color = Color(0xFF15803D).copy(0.7f), fontSize = 10.sp)
                                                }
                                                Text(
                                                    "PKR ${"%,.0f".format(depositAmt)}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color      = Color(0xFF15803D),
                                                    fontSize   = 16.sp
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        // Remaining amount box
                                        val isRemainPaid =
                                            currentBooking.paymentStatusEnum == PaymentStatus.PAID ||
                                                    currentBooking.bookingStatus == BookingStatus.COMPLETED ||
                                                    currentBooking.bookingStatus == BookingStatus.PENDING_APPROVAL ||
                                                    currentBooking.bookingStatus == BookingStatus.CONFIRMED
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        isRemainPaid -> Color(0xFFDCFCE7)
                                                        currentBooking.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> Color(0xFFFEE2E2)
                                                        else -> Color(0xFFFEF3C7)
                                                    }
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text(
                                                        "Due on Arrival (80%)",
                                                        fontWeight = FontWeight.Bold,
                                                        color = when {
                                                            isRemainPaid -> Color(0xFF15803D)
                                                            currentBooking.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> Color(0xFFDC2626)
                                                            else -> Color(0xFFB45309)
                                                        },
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        if (isRemainPaid) "Paid ✓" else "Pay at check-in",
                                                        color = when {
                                                            isRemainPaid -> Color(0xFF15803D)
                                                            currentBooking.bookingStatus == BookingStatus.AWAITING_FINAL_PAYMENT -> Color(0xFFDC2626)
                                                            else -> Color(0xFFB45309)
                                                        },
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Text(
                                                    "PKR ${"%,.0f".format(remainingAmt)}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color      = when {
                                                        isRemainPaid -> Color(0xFF15803D)
                                                        else -> Color(0xFFB45309)
                                                    },
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))
                                        HorizontalDivider(color = BG.copy(alpha = 0.2f))
                                        Spacer(Modifier.height(10.dp))

                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Total Paid Amount", fontWeight = FontWeight.SemiBold, color = BN, fontSize = 14.sp)
                                            Text(
                                                "PKR ${"%,.0f".format(if (isRemainPaid) totalCost else depositAmt)}",
                                                fontWeight = FontWeight.ExtraBold,
                                                color      = BN,
                                                fontSize   = 22.sp
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Total Booking Cost", fontWeight = FontWeight.SemiBold, color = BM, fontSize = 13.sp)
                                            Text(
                                                "PKR ${"%,.0f".format(totalCost)}",
                                                fontWeight = FontWeight.Bold,
                                                color      = BM,
                                                fontSize   = 15.sp
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Payment Status", color = BM, fontSize = 13.sp)
                                            val ps = currentBooking.paymentStatusEnum.displayName()
                                            val (pf, pb) = when (currentBooking.paymentStatusEnum) {
                                                PaymentStatus.PAID           -> Color(0xFF15803D) to Color(0xFFDCFCE7)
                                                PaymentStatus.DEPOSIT_PAID   -> Color(0xFF0369A1) to Color(0xFFE0F2FE)
                                                PaymentStatus.PARTIALLY_PAID -> Color(0xFFB45309) to Color(0xFFFEF3C7)
                                                PaymentStatus.PENDING        -> Color(0xFFB45309) to Color(0xFFFEF3C7)
                                                else                         -> Color(0xFFB91C1C) to Color(0xFFFEE2E2)
                                            }
                                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(pb).padding(horizontal = 12.dp, vertical = 5.dp)) {
                                                Text(ps, color = pf, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // ✅ Landlord: DEPOSIT_PAID pe "Mark Checked In" button
                                    if (isCurrentUserLandlord && currentBooking.bookingStatus == BookingStatus.DEPOSIT_PAID) {
                                        Button(
                                            onClick  = { showCheckIn = true },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape    = RoundedCornerShape(12.dp),
                                            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                                        ) {
                                            Icon(Icons.Default.Home, null, tint = BW, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Mark Tenant Checked In", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BW)
                                        }
                                    }

                                    // ✅ FIX: AWAITING_FINAL_PAYMENT landlord button removed
                                    // Tenant ab app se khud 80% pay karega — landlord ko manually confirm nahi karna

                                } else {
                                    // ── Normal (non-pre) booking payment ──
                                    BCard("Payment Breakdown", Icons.Default.Receipt) {
                                        BRow("Price / Night",    "PKR ${"%,.0f".format(currentBooking.pricePerNight)}")
                                        BRow("Subtotal",         "PKR ${"%,.0f".format(currentBooking.subtotal)}")
                                        BRow("Service Fee",      "PKR ${"%,.0f".format(currentBooking.serviceFee)}")
                                        BRow("Security Deposit", "PKR ${"%,.0f".format(currentBooking.securityDeposit)}")

                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(color = BL)
                                        Spacer(Modifier.height(8.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Payment Status", color = BM, fontSize = 13.sp)
                                            val ps = currentBooking.paymentStatusEnum.displayName()
                                            val (pf, pb) = when (ps) {
                                                "Paid"    -> Color(0xFF15803D) to Color(0xFFDCFCE7)
                                                "Pending" -> Color(0xFFB45309) to Color(0xFFFEF3C7)
                                                else      -> Color(0xFFB91C1C) to Color(0xFFFEE2E2)
                                            }
                                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(pb).padding(horizontal = 12.dp, vertical = 5.dp)) {
                                                Text(ps, color = pf, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        HorizontalDivider(color = BG.copy(alpha = 0.2f))
                                        Spacer(Modifier.height(10.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Total Amount", fontWeight = FontWeight.SemiBold, color = BN, fontSize = 14.sp)
                                            Text(currentBooking.formattedTotal, fontWeight = FontWeight.ExtraBold, color = BN, fontSize = 22.sp)
                                        }
                                    }

                                    if (!isCurrentUserLandlord && isPending && currentBooking.paymentStatusEnum == PaymentStatus.PENDING) {
                                        Button(
                                            onClick = {
                                                navController.navigate(Screen.Payment.createRoute(
                                                    bookingId = currentBooking.bookingId,
                                                    payerId   = currentBooking.tenantId,
                                                    payeeId   = currentBooking.landlordId,
                                                    payerName = currentBooking.tenantName,
                                                    payeeName = currentBooking.landlordName,
                                                    amount    = currentBooking.totalAmount
                                                ))
                                            },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape    = RoundedCornerShape(12.dp),
                                            colors   = ButtonDefaults.buttonColors(containerColor = BN)
                                        ) {
                                            Icon(Icons.Default.Payment, null, tint = BG, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Pay Now", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BG)
                                        }
                                    }
                                }
                            }

                            // ── TAB 2: Info ─────────────────────────────────────
                            2 -> {
                                BCard("Booking Info", Icons.Default.Info) {
                                    BRow("Tenant",         currentBooking.tenantEmail.ifBlank { tenantName })
                                    BRow("Host",           hostName)
                                    BRow("Payment Method", if (isPreBooking) "Partial / Cash on Arrival" else "Cash on Arrival")
                                    BRow("Booked On",       bfmt(currentBooking.createdAt?.toDate() ?: currentBooking.checkInDate?.toDate()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helper Composables ──────────────────────────────────────────────────────
@Composable private fun BCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BW), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BL)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = BN, modifier = Modifier.size(18.dp))
                Text(title, fontWeight = FontWeight.Bold, color = BN, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
@Composable private fun BRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = BM, fontSize = 14.sp)
        Text(value, color = BN, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable private fun BPill(icon: ImageVector, text: String) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(BL).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = BN, modifier = Modifier.size(14.dp))
            Text(text, color = BN, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
@Composable private fun BStrip(icon: ImageVector, text: String, fg: Color, bg: Color) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
            Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
@Composable private fun BDialog(title: String, desc: String, btnText: String, btnColor: Color, icon: ImageVector, iconBg: Color, iconTint: Color, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = btnColor)) { Text(btnText, color = BW) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = BM) } }, title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Box(Modifier.size(32.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp)) }; Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BN) } }, text = { Text(desc, fontSize = 14.sp, color = BN.copy(alpha = 0.7f)) }, shape = RoundedCornerShape(20.dp), containerColor = BW)
}