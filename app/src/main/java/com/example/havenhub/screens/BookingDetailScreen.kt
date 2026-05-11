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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.BookingStatus
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
    BookingStatus.PENDING          -> BStatusTheme(Color(0xFFB45309), Color(0xFFFEF3C7), Icons.Default.HourglassEmpty,    "Pending",           "Waiting for landlord approval.")
    BookingStatus.PENDING_APPROVAL -> BStatusTheme(Color(0xFF6D28D9), Color(0xFFEDE9FE), Icons.Default.AccessTime,         "Awaiting Approval", "Payment received — under review.")
    BookingStatus.CONFIRMED        -> BStatusTheme(Color(0xFF15803D), Color(0xFFDCFCE7), Icons.Default.CheckCircle,        "Confirmed",         "Landlord accepted your booking.")
    BookingStatus.CHECKED_IN       -> BStatusTheme(Color(0xFF1D4ED8), Color(0xFFDBEAFE), Icons.AutoMirrored.Filled.Login,  "Checked In",        "Guest is at the property.")
    BookingStatus.COMPLETED        -> BStatusTheme(Color(0xFF374151), Color(0xFFF3F4F6), Icons.Default.Done,               "Completed",         "Stay completed successfully.")
    BookingStatus.CANCELLED        -> BStatusTheme(Color(0xFFB91C1C), Color(0xFFFEE2E2), Icons.Default.Cancel,             "Cancelled",         "Booking has been cancelled.")
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

    var showCancel  by remember { mutableStateOf(false) }
    var showReject  by remember { mutableStateOf(false) }
    var showApprove by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // ✅ FIX: tenantName Firestore se fetch karo — UID ya email kabhi show nahi hoga
    var tenantName by remember { mutableStateOf("...") }

    LaunchedEffect(booking?.tenantId) {
        val tid = booking?.tenantId
        tenantName = when {
            // 1st priority: booking object mein naam already stored hai
            !booking?.tenantName.isNullOrBlank() -> booking!!.tenantName

            // 2nd priority: tenantId se Firestore users collection se fetch karo
            !tid.isNullOrBlank() -> {
                try {
                    val doc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(tid)
                        .get()
                        .await()
                    // ✅ Firestore mein "fullName" field hai — screenshot se confirmed
                    doc.getString("fullName")
                        ?: doc.getString("name")
                        ?: doc.getString("displayName")
                        ?: booking?.tenantEmail?.takeIf { it.isNotBlank() }
                        ?: "—"
                } catch (e: Exception) {
                    booking?.tenantEmail?.takeIf { it.isNotBlank() } ?: "—"
                }
            }

            // 3rd priority: email fallback
            !booking?.tenantEmail.isNullOrBlank() -> booking!!.tenantEmail

            else -> "—"
        }
    }

    LaunchedEffect(bookingId) { viewModel.loadBookingById(bookingId) }
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearMessages()
        }
    }

    if (showCancel) BDialog(
        "Cancel Booking", "Cancel this booking? This cannot be undone.",
        "Yes, Cancel", Color(0xFFDC2626), Icons.Default.Cancel,
        Color(0xFFFEE2E2), Color(0xFFDC2626),
        onConfirm = { booking?.bookingId?.let { viewModel.cancelBooking(it) }; showCancel = false },
        onDismiss = { showCancel = false }
    )
    if (showReject) BDialog(
        "Reject Request", "Reject this booking? Tenant will be notified.",
        "Reject", Color(0xFFDC2626), Icons.Default.ThumbDown,
        Color(0xFFFEE2E2), Color(0xFFDC2626),
        onConfirm = { booking?.bookingId?.let { viewModel.updateStatusByAdmin(it, BookingStatus.CANCELLED) }; showReject = false },
        onDismiss = { showReject = false }
    )
    if (showApprove) BDialog(
        "Approve Booking", "Approve this booking? Tenant will be notified.",
        "Approve", Color(0xFF16A34A), Icons.Default.ThumbUp,
        Color(0xFFDCFCE7), Color(0xFF16A34A),
        onConfirm = { booking?.bookingId?.let { viewModel.updateStatusByAdmin(it, BookingStatus.CONFIRMED) }; showApprove = false },
        onDismiss = { showApprove = false }
    )

    Scaffold(containerColor = BBg) { pad ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(pad),
                Alignment.Center
            ) {
                CircularProgressIndicator(
                    color       = BN,
                    strokeWidth = 2.5.dp,
                    modifier    = Modifier.size(36.dp)
                )
            }

            booking == null -> Box(
                Modifier.fillMaxSize().padding(pad),
                Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SearchOff, null, tint = BM, modifier = Modifier.size(48.dp))
                    Text("Booking not found", color = BN, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            else -> {
                val t         = bTheme(booking.bookingStatus)
                val isPending = booking.bookingStatus == BookingStatus.PENDING

                Column(Modifier.fillMaxSize()) {

                    // ── TOP BAR ────────────────────────────────────────────
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(BN)
                            .statusBarsPadding()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Back",
                                    tint = BW.copy(alpha = 0.8f)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Booking Details",
                                    fontWeight = FontWeight.SemiBold,
                                    color      = BW,
                                    fontSize   = 16.sp
                                )
                                Text(
                                    "#${booking.bookingId.take(8).uppercase()}",
                                    color    = BW.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(t.bg)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(t.icon, null, tint = t.fg, modifier = Modifier.size(12.dp))
                                    Text(t.label, color = t.fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        // ── TABS ───────────────────────────────────────────
                        val tabs = listOf("Details", "Payment", "Info")
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor   = BN,
                            contentColor     = BW,
                            indicator = { pos ->
                                if (selectedTab < pos.size)
                                    Box(
                                        Modifier
                                            .tabIndicatorOffset(pos[selectedTab])
                                            .height(2.dp)
                                            .padding(horizontal = 20.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(BG)
                                    )
                            },
                            divider = {}
                        ) {
                            tabs.forEachIndexed { i, tab ->
                                Tab(
                                    selected = selectedTab == i,
                                    onClick  = { selectedTab = i },
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Text(
                                        tab,
                                        fontSize   = 13.sp,
                                        color      = if (selectedTab == i) BW else BW.copy(alpha = 0.4f),
                                        fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // ── TAB CONTENT ────────────────────────────────────────
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {

                            // ── TAB 0: Details ─────────────────────────────
                            0 -> {
                                BStrip(t.icon, t.desc, t.fg, t.bg)

                                BCard("Property", Icons.Default.Apartment) {
                                    BRow("Property Name", booking.propertyTitle)
                                    BRow("Address",       booking.propertyAddress)
                                }

                                BCard("Stay Dates", Icons.Default.CalendarMonth) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Box(
                                                    Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF16A34A))
                                                )
                                                Text(
                                                    "Check-in",
                                                    color      = Color(0xFF15803D),
                                                    fontSize   = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Text(
                                                bfmt(booking.checkInDate?.toDate()),
                                                color      = BN,
                                                fontSize   = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFFEE2E2))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Box(
                                                    Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFDC2626))
                                                )
                                                Text(
                                                    "Check-out",
                                                    color      = Color(0xFFB91C1C),
                                                    fontSize   = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Text(
                                                bfmt(booking.checkOutDate?.toDate()),
                                                color      = BN,
                                                fontSize   = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BPill(Icons.Default.NightsStay, "${booking.totalNights} Night(s)")
                                        BPill(Icons.Default.Group,      "${booking.guestCount} Guest(s)")
                                    }
                                }
                            }

                            // ── TAB 1: Payment ─────────────────────────────
                            1 -> {
                                BCard("Payment Breakdown", Icons.Default.Receipt) {
                                    BPayRow("Price / Night",    "PKR ${booking.pricePerNight.toInt()}")
                                    BPayRow("Subtotal",         "PKR ${booking.subtotal.toInt()}")
                                    BPayRow("Service Fee",      "PKR ${booking.serviceFee.toInt()}")
                                    BPayRow("Security Deposit", "PKR ${booking.securityDeposit.toInt()}")
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(color = BL)
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Text("Payment Status", color = BM, fontSize = 13.sp)
                                        val ps = booking.paymentStatusEnum.displayName()
                                        val (pf, pb) = when (ps) {
                                            "Paid"    -> Color(0xFF15803D) to Color(0xFFDCFCE7)
                                            "Pending" -> Color(0xFFB45309) to Color(0xFFFEF3C7)
                                            else      -> Color(0xFFB91C1C) to Color(0xFFFEE2E2)
                                        }
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(pb)
                                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                ps,
                                                color      = pf,
                                                fontSize   = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = BG.copy(alpha = 0.2f))
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Total Amount",
                                            fontWeight = FontWeight.SemiBold,
                                            color      = BN,
                                            fontSize   = 14.sp
                                        )
                                        Text(
                                            booking.formattedTotal,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = BN,
                                            fontSize   = 22.sp
                                        )
                                    }
                                }

                                if (!isCurrentUserLandlord && isPending &&
                                    booking.paymentStatusEnum.displayName() == "Pending"
                                ) {
                                    Button(
                                        onClick = {
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
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape    = RoundedCornerShape(12.dp),
                                        colors   = ButtonDefaults.buttonColors(containerColor = BN)
                                    ) {
                                        Icon(
                                            Icons.Default.Payment,
                                            null,
                                            tint     = BG,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Pay Now",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize   = 14.sp,
                                            color      = BG
                                        )
                                    }
                                }
                            }

                            // ── TAB 2: Info ────────────────────────────────
                            2 -> {
                                BCard("Booking Info", Icons.Default.Info) {
                                    // ✅ FIX: tenantName ab Firestore se fetch hota hai
                                    // UID ya email kabhi nahi dikhega — sirf actual naam
                                    BRow("Tenant",         tenantName)
                                    BRow("Host",           booking.landlordName)
                                    BRow("Payment Method", booking.paymentMethod.ifBlank { "—" })
                                    BRow("Booked On",      bfmt(booking.createdAt?.toDate()))
                                }

                                if (!isCurrentUserLandlord && isPending) {
                                    OutlinedButton(
                                        onClick  = { showCancel = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape    = RoundedCornerShape(12.dp),
                                        border   = BorderStroke(1.dp, Color(0xFFDC2626)),
                                        colors   = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFDC2626)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Cancel Booking",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize   = 14.sp
                                        )
                                    }
                                }

                                if (isCurrentUserLandlord && isPending) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick  = { showReject = true },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape    = RoundedCornerShape(12.dp),
                                            border   = BorderStroke(1.dp, Color(0xFFDC2626)),
                                            colors   = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFDC2626)
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Reject",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize   = 14.sp
                                            )
                                        }
                                        Button(
                                            onClick  = { showApprove = true },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape    = RoundedCornerShape(12.dp),
                                            colors   = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF16A34A)
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Approve",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize   = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        uiState.errorMessage?.let {
                            BStrip(Icons.Default.ErrorOutline, it, Color(0xFFB91C1C), Color(0xFFFEE2E2))
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// ── Private Composables ───────────────────────────────────────────────────────

@Composable
private fun BCard(
    title  : String,
    icon   : ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BW),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, BL)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BN.copy(alpha = 0.07f)),
                    Alignment.Center
                ) {
                    Icon(icon, null, tint = BN, modifier = Modifier.size(14.dp))
                }
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BN)
            }
            HorizontalDivider(color = BL, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun BRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = BM, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            value.ifBlank { "—" },
            color      = BN,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.End,
            modifier   = Modifier.weight(1.5f),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
    HorizontalDivider(color = BL, thickness = 0.5.dp)
}

@Composable
private fun BPayRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = BM, fontSize = 13.sp)
        Text(value, color = BN, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BPill(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BN.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, tint = BN, modifier = Modifier.size(12.dp))
        Text(text, color = BN, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BStrip(icon: ImageVector, msg: String, fg: Color, bg: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(msg, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
    }
}

@Composable
private fun BDialog(
    title       : String,
    message     : String,
    confirmLabel: String,
    confirmColor: Color,
    icon        : ImageVector,
    iconBg      : Color,
    iconTint    : Color,
    onConfirm   : () -> Unit,
    onDismiss   : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = BW,
        shape            = RoundedCornerShape(20.dp),
        icon = {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(iconBg),
                Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
        },
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize   = 17.sp,
                color      = BN,
                textAlign  = TextAlign.Center
            )
        },
        text = {
            Text(
                message,
                fontSize   = 14.sp,
                color      = BM,
                lineHeight = 20.sp,
                textAlign  = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = confirmColor),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Go Back", color = BM, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    )
}

// ── Public aliases ────────────────────────────────────────────────────────────
@Composable fun BDCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) = BCard(title, icon, content)
@Composable fun BDField(label: String, value: String) = BRow(label, value)
@Composable fun BDPayRow(label: String, value: String) = BPayRow(label, value)
@Composable fun BDPill(icon: ImageVector, text: String) = BPill(icon, text)
@Composable fun BDSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(BN.copy(alpha = 0.07f)),
            Alignment.Center
        ) {
            Icon(icon, null, tint = BN, modifier = Modifier.size(14.dp))
        }
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BN)
    }
}
@Composable fun BDSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) = BCard(title, icon, content)
@Composable fun BDInfoItem(label: String, value: String) = BRow(label, value)
@Composable fun BDStayPill(icon: ImageVector, text: String) = BPill(icon, text)
@Composable fun BDStatusCard(status: String) {
    val t = bTheme(when (status) {
        "Confirmed"  -> BookingStatus.CONFIRMED
        "Checked In" -> BookingStatus.CHECKED_IN
        "Completed"  -> BookingStatus.COMPLETED
        "Cancelled"  -> BookingStatus.CANCELLED
        "Awaiting", "Awaiting Approval" -> BookingStatus.PENDING_APPROVAL
        else         -> BookingStatus.PENDING
    })
    BStrip(t.icon, t.label + " — " + t.desc, t.fg, t.bg)
}
@Composable fun BDActionSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BW).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content             = content
    )
}
@Composable fun BookingSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) = BCard(title, icon, content)
@Composable fun InfoItem(label: String, value: String) = BRow(label, value)
@Composable fun BookingStatusBanner(status: String) = BDStatusCard(status)
@Composable fun StatusBadge(status: String) = BDStatusCard(status)
@Composable fun CancelConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) =
    BDialog(
        "Cancel Booking", "Cancel this booking? This cannot be undone.",
        "Yes, Cancel", Color(0xFFDC2626), Icons.Default.Cancel,
        Color(0xFFFEE2E2), Color(0xFFDC2626), onConfirm, onDismiss
    )
@Composable fun BookingActionDialog(
    title       : String,
    message     : String,
    confirmLabel: String,
    confirmColor: Color,
    icon        : ImageVector,
    iconBg      : Color,
    iconTint    : Color,
    onConfirm   : () -> Unit,
    onDismiss   : () -> Unit
) = BDialog(title, message, confirmLabel, confirmColor, icon, iconBg, iconTint, onConfirm, onDismiss)

fun String?.displayName(): String = this ?: "Unknown"