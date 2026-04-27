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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Design tokens ─────────────────────────────────────────────
private val DNavy  = Color(0xFF0D1B3E)
private val DGold  = Color(0xFFD4AF37)
private val DBg    = Color(0xFFF5F7FA)
private val DMuted = Color(0xFF8899AA)
private val DRed   = Color(0xFFEF4444)
private val DGreen = Color(0xFF22C55E)
private val DWhite = Color(0xFFFFFFFF)

private fun formatDate(date: Date?): String {
    if (date == null) return "—"
    return try {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) { "—" }
}

// ══════════════════════════════════════════════════════════════
// MAIN SCREEN
// ══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    navController        : NavController,
    bookingId            : String,
    isCurrentUserLandlord: Boolean       = false,
    viewModel            : BookingViewModel = hiltViewModel(),
    authViewModel        : AuthViewModel    = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val booking   = uiState.currentBooking
    var showCancelDialog by remember { mutableStateOf(false) }

    val tenantName = remember(booking, authState) {
        when {
            !booking?.tenantName.isNullOrBlank()                          -> booking!!.tenantName
            authState.currentUser?.displayName?.isNotBlank() == true      -> authState.currentUser!!.displayName!!
            authState.currentUser?.email?.isNotBlank() == true            -> authState.currentUser!!.email!!
            else                                                           -> "—"
        }
    }

    LaunchedEffect(bookingId) { viewModel.loadBookingById(bookingId) }

    // Cancel success → wapas MyBookings
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearMessages()
        }
    }

    // Cancel dialog
    if (showCancelDialog) {
        CancelConfirmDialog(
            onConfirm = {
                booking?.bookingId?.let { viewModel.cancelBooking(it) }
                showCancelDialog = false
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(DNavy, Color(0xFF1A2F5E))))
            ) {
                Row(
                    modifier          = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DGold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Booking Details", fontWeight = FontWeight.Bold, color = DWhite, fontSize = 17.sp)
                        booking?.let {
                            Text(
                                "#${it.bookingId.take(8).uppercase()}",
                                color    = DWhite.copy(0.55f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = DBg
    ) { padding ->

        when {
            // ── Loading ──────────────────────────────────────
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = DGold, strokeWidth = 3.dp)
                }
            }

            // ── Not found ────────────────────────────────────
            booking == null -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Booking not found", color = DMuted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ── Content ──────────────────────────────────────
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .verticalScroll(rememberScrollState())
                ) {

                    // ══════════════════════════════════════════
                    // 1. STATUS BANNER
                    // ══════════════════════════════════════════
                    BookingStatusBanner(status = booking.bookingStatus.displayName())

                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // ══════════════════════════════════════
                        // 2. PROPERTY
                        // ══════════════════════════════════════
                        BDSection(title = "Property", icon = Icons.Default.Home) {
                            BDInfoItem("Property", booking.propertyTitle)
                            BDInfoItem("Address",  booking.propertyAddress)
                        }

                        // ══════════════════════════════════════
                        // 3. STAY DETAILS — check-in/out cards
                        // ══════════════════════════════════════
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = DWhite),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                BDSectionHeader(Icons.Default.CalendarMonth, "Stay Details")
                                Spacer(Modifier.height(14.dp))

                                // Check-in / Check-out side by side
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Check-in
                                    Column(
                                        modifier = Modifier.weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DGreen.copy(0.10f))
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FlightLand, null, tint = DGreen, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Check-in", color = DGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            formatDate(booking.checkInDate?.toDate()),
                                            color      = DNavy,
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 17.sp
                                        )
                                    }

                                    // Check-out
                                    Column(
                                        modifier = Modifier.weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DRed.copy(0.08f))
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FlightTakeoff, null, tint = DRed, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Check-out", color = DRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            formatDate(booking.checkOutDate?.toDate()),
                                            color      = DNavy,
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Nights + Guests pills
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    BDStayPill(Icons.Default.NightsStay, "${booking.totalNights} Night(s)")
                                    BDStayPill(Icons.Default.People,     "${booking.guestCount} Guest(s)")
                                }
                            }
                        }

                        // ══════════════════════════════════════
                        // 4. PAYMENT SUMMARY
                        // ══════════════════════════════════════
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = DWhite),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                BDSectionHeader(Icons.Default.Payments, "Payment Summary")
                                Spacer(Modifier.height(14.dp))

                                BDPayRow("Price / Night",    "PKR ${booking.pricePerNight.toInt()}")
                                BDPayRow("Subtotal",         "PKR ${booking.subtotal.toInt()}")
                                BDPayRow("Service Fee",      "PKR ${booking.serviceFee.toInt()}")
                                BDPayRow("Security Deposit", "PKR ${booking.securityDeposit.toInt()}")

                                Spacer(Modifier.height(6.dp))

                                // Payment status chip
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("Payment", color = DMuted, fontSize = 13.sp)
                                    val pStatus = booking.paymentStatusEnum.displayName()
                                    val pColor  = when (pStatus) {
                                        "Paid"    -> DGreen
                                        "Pending" -> DGold
                                        else      -> DRed
                                    }
                                    Box(
                                        Modifier.clip(RoundedCornerShape(20.dp))
                                            .background(pColor.copy(0.12f))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(pStatus, color = pColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Box(
                                    Modifier.fillMaxWidth().height(1.dp)
                                        .background(Brush.horizontalGradient(listOf(DGold.copy(0.5f), Color.Transparent)))
                                )
                                Spacer(Modifier.height(10.dp))

                                // Total
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("Total Amount", fontWeight = FontWeight.Bold, color = DNavy, fontSize = 15.sp)
                                    Text(booking.formattedTotal, fontWeight = FontWeight.ExtraBold, color = DGold, fontSize = 20.sp)
                                }
                            }
                        }

                        // ══════════════════════════════════════
                        // 5. HOST INFO
                        // ══════════════════════════════════════
                        BDSection(title = "Host", icon = Icons.Default.Person) {
                            BDInfoItem("Host Name", booking.landlordName)
                        }

                        // ══════════════════════════════════════
                        // 6. BOOKING INFO
                        // ══════════════════════════════════════
                        BDSection(title = "Booking Info", icon = Icons.Default.Info) {
                            BDInfoItem("Tenant",         tenantName)
                            BDInfoItem("Payment Method", booking.paymentMethod.ifBlank { "—" })
                            BDInfoItem("Booked On",      formatDate(booking.createdAt?.toDate()))
                        }

                        // ══════════════════════════════════════
                        // 7. CONFIRMED info strip
                        // ══════════════════════════════════════
                        if (booking.bookingStatus == BookingStatus.CONFIRMED) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DGreen.copy(0.09f))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = DGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Booking Confirmed", fontWeight = FontWeight.Bold, color = DGreen, fontSize = 13.sp)
                                    Text("Your landlord has accepted this booking.", color = DMuted, fontSize = 12.sp)
                                }
                            }
                        }

                        // ══════════════════════════════════════
                        // 8. CANCEL BUTTON — sirf PENDING tenant ke liye
                        //    SRS BR-3
                        // ══════════════════════════════════════
                        val isPending = booking.bookingStatus == BookingStatus.PENDING
                        if (!isCurrentUserLandlord && isPending) {
                            Button(
                                onClick  = { showCancelDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = DRed,
                                    contentColor   = DWhite
                                )
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Cancel Booking", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // ══════════════════════════════════════
                        // 9. PAY NOW — tenant + PENDING + unpaid
                        // ══════════════════════════════════════
                        if (!isCurrentUserLandlord && isPending &&
                            booking.paymentStatusEnum.displayName() == "Pending") {
                            Button(
                                onClick  = {
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
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = DNavy,
                                    contentColor   = DWhite
                                )
                            ) {
                                Icon(Icons.Default.Payment, null, tint = DGold, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Pay Now", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DGold)
                            }
                        }

                        // Error
                        uiState.errorMessage?.let {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DRed.copy(0.08f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = DRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(it, color = DRed, fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// STATUS BANNER
// ══════════════════════════════════════════════════════════════
@Composable
fun BookingStatusBanner(status: String) {
    data class BannerStyle(val bg: Color, val fg: Color, val emoji: String)

    val style = when (status) {
        "Confirmed"  -> BannerStyle(Color(0xFF166534), DGreen,           "✅")
        "Checked In" -> BannerStyle(Color(0xFF1E3A8A), Color(0xFF60A5FA),"🏠")
        "Completed"  -> BannerStyle(Color(0xFF374151), Color(0xFF9CA3AF),"🎉")
        "Cancelled"  -> BannerStyle(Color(0xFF7F1D1D), Color(0xFFF87171),"❌")
        else         -> BannerStyle(Color(0xFF78350F), Color(0xFFFBBF24),"⏳")
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(style.bg, DNavy)))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(style.emoji, fontSize = 30.sp)
            Column {
                Text("Booking Status", color = DWhite.copy(0.55f), fontSize = 11.sp)
                Text(status, color = style.fg, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// CANCEL DIALOG
// ══════════════════════════════════════════════════════════════
@Composable
fun CancelConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DWhite,
        shape            = RoundedCornerShape(20.dp),
        icon = {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(DRed.copy(0.10f)),
                Alignment.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = DRed, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text("Cancel Booking?", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DNavy)
        },
        text = {
            Text(
                "Are you sure you want to cancel this booking?\nThis action cannot be undone.",
                fontSize = 14.sp, color = DMuted, lineHeight = 21.sp
            )
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = DRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Yes, Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick  = onDismiss,
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.5.dp, DNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keep Booking", color = DNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    )
}

// ══════════════════════════════════════════════════════════════
// SHARED SUB-COMPOSABLES
// ══════════════════════════════════════════════════════════════

@Composable
fun BDSection(
    title  : String,
    icon   : ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = DWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            BDSectionHeader(icon, title)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun BDSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(DNavy.copy(0.08f)),
            Alignment.Center
        ) {
            Icon(icon, null, tint = DNavy, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DNavy)
    }
}

@Composable
fun BDInfoItem(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = DMuted, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(value.ifBlank { "—" }, color = DNavy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun BDPayRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = DMuted, fontSize = 13.sp)
        Text(value, color = DNavy, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BDStayPill(icon: ImageVector, text: String) {
    Row(
        modifier              = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DNavy.copy(0.07f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, tint = DNavy, modifier = Modifier.size(13.dp))
        Text(text, color = DNavy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Legacy aliases (backward compatibility) ───────────────────
@Composable
fun BookingSection(
    title  : String,
    icon   : ImageVector,
    content: @Composable ColumnScope.() -> Unit
) = BDSection(title, icon, content)

@Composable
fun InfoItem(label: String, value: String) = BDInfoItem(label, value)

@Composable
fun StatusBadge(status: String) = BookingStatusBanner(status)

fun String?.displayName(): String = this ?: "Unknown"