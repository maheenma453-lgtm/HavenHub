package com.example.havenhub.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel
import com.example.havenhub.viewmodel.VacationViewModel
import com.google.firebase.Timestamp
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// COLOR PALETTE — navy / gold theme local to this screen
// ─────────────────────────────────────────────────────────────────────────────
private val PB_NavyDeep     = Color(0xFF060E20)
private val PB_NavyPrime    = Color(0xFF0D1B3E)
private val PB_NavyMid      = Color(0xFF1A3A6B)
private val PB_NavyLight    = Color(0xFF2A4E8A)
private val PB_Gold         = Color(0xFFD4AF37)
private val PB_GoldLight    = Color(0xFFF5D060)
private val PB_GoldDim      = Color(0xFFB8962E)
private val PB_GoldFaint    = Color(0xFFFFF8E1)
private val PB_Success      = Color(0xFF16A34A)
private val PB_SuccessLight = Color(0xFFDCFCE7)
private val PB_Warning      = Color(0xFFD97706)
private val PB_WarningLight = Color(0xFFFEF3C7)
private val PB_CardBg       = Color(0xFFFFFFFF)
private val PB_PageBg       = Color(0xFFF2F5FB)
private val PB_TextDark     = Color(0xFF1A2744)
private val PB_TextMuted    = Color(0xFF8899AA)
private val PB_TextLight    = Color(0xFFBBCCDD)
private val PB_Divider      = Color(0xFFE8EEF5)

private val GoldGradient     = Brush.horizontalGradient(listOf(PB_Gold.copy(0.9f), PB_GoldLight.copy(0.6f), PB_Gold.copy(0.9f)))
private val GoldBorder       = Brush.horizontalGradient(listOf(PB_Gold.copy(0.85f), PB_GoldLight.copy(0.5f), PB_Gold.copy(0.85f)))
private val NavyGradient     = Brush.linearGradient(listOf(PB_NavyDeep, PB_NavyMid))
private val NavySoftGradient = Brush.linearGradient(listOf(PB_NavyPrime, PB_NavyLight))

// ─────────────────────────────────────────────────────────────────────────────
// RESPONSIVE SIZE SYSTEM
// Adjusts padding, font sizes and icon sizes based on screen width buckets.
// ─────────────────────────────────────────────────────────────────────────────
private data class PBSizes(
    val hPad        : Dp,
    val vPad        : Dp,
    val cardRadius  : Dp,
    val titleSp     : Float,
    val bodySp      : Float,
    val captionSp   : Float,
    val iconSize    : Dp,
    val avatarSize  : Dp,
    val chipHeight  : Dp,
    val btnHeight   : Dp,
    val sectionGap  : Dp,
    val cardPad     : Dp,
    val priceSize   : Float,
    val depositSize : Float,
    val checkSize   : Dp,
    val bannerIcon  : Dp,
)

@Composable
private fun rememberPBSizes(): PBSizes {
    val w = LocalConfiguration.current.screenWidthDp
    return remember(w) {
        when {
            w < 320 -> PBSizes(10.dp, 8.dp,  10.dp, 11f,  9.5f, 8.5f, 13.dp, 32.dp, 24.dp, 40.dp, 10.dp, 10.dp, 18f, 17f, 14.dp, 36.dp)
            w < 360 -> PBSizes(12.dp, 10.dp, 12.dp, 12f,  10.5f, 9f,  14.dp, 34.dp, 26.dp, 44.dp, 12.dp, 12.dp, 20f, 19f, 15.dp, 38.dp)
            w < 390 -> PBSizes(14.dp, 12.dp, 14.dp, 13f,  11f,  9.5f, 15.dp, 36.dp, 28.dp, 46.dp, 14.dp, 14.dp, 21f, 20f, 16.dp, 40.dp)
            w < 430 -> PBSizes(16.dp, 14.dp, 16.dp, 14f,  11.5f, 10f, 16.dp, 38.dp, 30.dp, 48.dp, 16.dp, 16.dp, 22f, 21f, 18.dp, 42.dp)
            w < 480 -> PBSizes(18.dp, 15.dp, 17.dp, 15f,  12f,  10.5f,17.dp, 40.dp, 32.dp, 50.dp, 17.dp, 17.dp, 23f, 22f, 19.dp, 44.dp)
            w < 600 -> PBSizes(20.dp, 16.dp, 18.dp, 16f,  13f,  11f,  18.dp, 42.dp, 34.dp, 52.dp, 18.dp, 18.dp, 24f, 23f, 20.dp, 46.dp)
            else    -> PBSizes(26.dp, 20.dp, 20.dp, 17f,  14f,  12f,  20.dp, 46.dp, 38.dp, 56.dp, 20.dp, 20.dp, 26f, 25f, 22.dp, 50.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DATE HELPER
// Builds a Firebase Timestamp at 12:00 noon for the given day/month/year.
// Using noon avoids timezone edge-cases that could shift the date by one day.
// ─────────────────────────────────────────────────────────────────────────────
private fun buildTimestamp(day: Int, month: Int, year: Int): Timestamp {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR,         year)
        set(Calendar.MONTH,        month)   // 0-indexed (January = 0)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY,  12)
        set(Calendar.MINUTE,       0)
        set(Calendar.SECOND,       0)
        set(Calendar.MILLISECOND,  0)
    }
    return Timestamp(cal.time)
}

// Month names used by the calendar header and check-in/check-out summary boxes
private val MONTH_NAMES = arrayOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December"
)

// ─────────────────────────────────────────────────────────────────────────────
// PRE-BOOKING SCREEN
//
// Booking flow:
//   1. Tenant selects a rental package from the list
//   2. Tenant picks check-in date from the inline calendar
//   3. Tenant picks a check-out date after check-in; nights auto-calculated
//   4. If dates are NOT picked, a manual nights counter is shown as fallback
//   5. Tenant adjusts guest count
//   6. Payment summary shows deposit (20%) and remaining (80%) amounts
//   7. "Pay Deposit" creates the Booking document and navigates to PaymentScreen
//      with paymentType = "DEPOSIT" so the 20% flow is triggered correctly
//
// KEY FIX:
//   checkInDay and checkOutDay use -1 as "not selected" (not 0).
//   datesSelected = checkInDay > 0 && checkOutDay > 0
//   This correctly distinguishes "not chosen" from day 1 of a month.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreBookingScreen(
    navController   : NavController,
    propertyId      : String            = "",
    viewModel       : VacationViewModel = hiltViewModel(),
    authViewModel   : AuthViewModel     = hiltViewModel(),
    bookingViewModel: BookingViewModel  = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val bookingUiState by bookingViewModel.uiState.collectAsState()
    val authUiState    by authViewModel.uiState.collectAsState()
    val sz              = rememberPBSizes()

    val currentUserId   = authUiState.currentUser?.uid         ?: ""
    val currentUserName = authUiState.currentUser?.displayName ?: ""

    val selectedPkg = uiState.selectedPackage
    val minNights   = selectedPkg?.minNights ?: 1
    val maxNights   = selectedPkg?.maxNights ?: 30

    // Manual nights counter — shown when tenant has NOT picked dates from calendar.
    // Initialized to minNights and resets whenever the package changes.
    var selectedNights by remember(selectedPkg?.packageId) {
        mutableIntStateOf(minNights)
    }
    LaunchedEffect(selectedPkg?.packageId) {
        selectedNights = minNights
    }

    // Load available packages when the screen opens
    LaunchedEffect(propertyId) {
        Log.d("PRE_BOOKING", "Loading packages for propertyId='$propertyId'")
        if (propertyId.isNotEmpty()) {
            viewModel.loadPackagesForProperty(propertyId)
        } else {
            Log.e("PRE_BOOKING", "propertyId is EMPTY — packages cannot load")
        }
    }

    // Read date state from ViewModel (set by the inline calendar)
    val checkInDay    = uiState.checkInDay
    val checkInMonth  = uiState.checkInMonth
    val checkInYear   = uiState.checkInYear
    val checkOutDay   = uiState.checkOutDay
    val checkOutMonth = uiState.checkOutMonth
    val checkOutYear  = uiState.checkOutYear

    // ── FIX: use -1 sentinel, NOT 0, to detect "date not selected" ───────────
    // checkInDay  = -1 → tenant has not chosen check-in yet
    // checkOutDay = -1 → tenant has not chosen check-out yet
    // Both > 0    → both dates are selected, calendar drives the night count
    val datesSelected   = checkInDay > 0 && checkOutDay > 0

    // Nights from the calendar range takes priority over the manual counter
    val nightsFromDates = if (datesSelected) viewModel.calculateNights() else -1
    val effectiveNights = if (nightsFromDates > 0) nightsFromDates else selectedNights

    val totalAmount   = (selectedPkg?.discountedPricePerNight ?: 0.0) * effectiveNights
    val depositAmount = totalAmount * 0.20

    // Navigate to PaymentScreen once booking creation succeeds
    LaunchedEffect(bookingUiState.actionSuccess, bookingUiState.createdBookingId) {
        if (bookingUiState.actionSuccess && !bookingUiState.createdBookingId.isNullOrEmpty()) {
            val createdId = bookingUiState.createdBookingId!!
            val pkg       = selectedPkg ?: return@LaunchedEffect
            bookingViewModel.clearMessages()
            navController.navigate(
                Screen.Payment.createRoute(
                    bookingId   = createdId,
                    payerId     = currentUserId,
                    payeeId     = pkg.landlordId,
                    payerName   = currentUserName,
                    payeeName   = pkg.propertyTitle,
                    amount      = depositAmount,
                    paymentType = "DEPOSIT"   // triggers the 20% deposit flow
                )
            )
        }
    }

    val packages = uiState.propertyPackages
    // "Pay Deposit" is only enabled when a package is chosen and
    // effectiveNights meets the minimum requirement for that package
    val canBook  = selectedPkg != null && effectiveNights >= minNights

    Scaffold(
        topBar = {
            PBTopBar(
                propertyId    = propertyId,
                propertyTitle = uiState.selectedPropertyTitle,
                sz            = sz,
                onBack        = { navController.popBackStack() }
            )
        },
        containerColor = PB_PageBg,
        bottomBar = {
            AnimatedVisibility(
                visible = selectedPkg != null && canBook,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                if (selectedPkg != null) {
                    PBBottomBar(
                        depositAmount = depositAmount,
                        totalAmount   = totalAmount,
                        isLoading     = bookingUiState.isLoading,
                        sz            = sz,
                        onPay         = {
                            val pid = propertyId.ifEmpty { selectedPkg.propertyId }

                            // Build Firebase Timestamps only when tenant selected dates.
                            // If no dates were picked, timestamps are null and the
                            // nights counter value is used for pricing only.
                            val checkIn  = if (checkInDay  > 0) buildTimestamp(checkInDay,  checkInMonth,  checkInYear)  else null
                            val checkOut = if (checkOutDay > 0) buildTimestamp(checkOutDay, checkOutMonth, checkOutYear) else null

                            val booking = Booking(
                                propertyId      = pid,
                                propertyTitle   = selectedPkg.propertyTitle,
                                landlordId      = selectedPkg.landlordId,
                                landlordName    = selectedPkg.propertyTitle,
                                tenantId        = currentUserId,
                                tenantName      = currentUserName,
                                pricePerNight   = selectedPkg.discountedPricePerNight,
                                subtotal        = totalAmount,
                                totalAmount     = totalAmount,
                                depositAmount   = depositAmount,
                                remainingAmount = totalAmount - depositAmount,
                                isPreBooking    = true,         // flags this as a deposit-flow booking
                                securityDeposit = 0.0,
                                status          = BookingStatus.PENDING_APPROVAL.name,
                                paymentStatus   = PaymentStatus.PENDING.name,
                                propertyAddress = selectedPkg.propertyTitle,
                                totalNights     = effectiveNights,
                                guestCount      = uiState.guestCount,
                                paymentMethod   = "Pending",
                                checkInDate     = checkIn,
                                checkOutDate    = checkOut
                            )
                            bookingViewModel.createBooking(booking)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // Advance booking information banner
            item {
                Spacer(Modifier.height(sz.sectionGap))
                PBAdvanceBanner(sz)
            }

            // Section header for the packages list
            item {
                Spacer(Modifier.height(sz.sectionGap + 4.dp))
                PBSectionHeader(
                    title      = "Available Packages",
                    badgeCount = if (packages.isNotEmpty()) packages.size else null,
                    sz         = sz
                )
                Spacer(Modifier.height(sz.vPad - 4.dp))
            }

            // Package list — loading / empty / cards
            if (uiState.isLoading) {
                item { PBLoadingState(sz) }
            } else if (packages.isEmpty()) {
                item { PBEmptyState(propertyId = propertyId, sz = sz) }
            } else {
                items(packages, key = { it.packageId }) { pkg ->
                    PBPackageCard(
                        pkg        = pkg,
                        isSelected = selectedPkg?.packageId == pkg.packageId,
                        sz         = sz,
                        onClick    = { viewModel.selectPackage(pkg) }
                    )
                }
            }

            // Guest counter
            item {
                Spacer(Modifier.height(sz.sectionGap - 4.dp))
                PBGuestCounter(
                    guestCount = uiState.guestCount,
                    sz         = sz,
                    onMinus    = { viewModel.setGuestCount(uiState.guestCount - 1) },
                    onPlus     = { viewModel.setGuestCount(uiState.guestCount + 1) }
                )
            }

            // Inline calendar date picker — only shown when a package is selected.
            // Tenant picks check-in first, then check-out from the same calendar.
            item {
                AnimatedVisibility(
                    visible = selectedPkg != null,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    if (selectedPkg != null) {
                        Spacer(Modifier.height(sz.sectionGap - 4.dp))
                        PBDatePicker(
                            checkInDay         = checkInDay,
                            checkInMonth       = checkInMonth,
                            checkInYear        = checkInYear,
                            checkOutDay        = checkOutDay,
                            checkOutMonth      = checkOutMonth,
                            checkOutYear       = checkOutYear,
                            sz                 = sz,
                            onCheckInSelected  = { day, month, year ->
                                viewModel.setCheckInDate(day, month, year)
                            },
                            onCheckOutSelected = { day, month, year ->
                                viewModel.setCheckOutDate(day, month, year)
                            },
                            onReset = {
                                viewModel.clearDates()
                            }
                        )
                    }
                }
            }

            // Manual nights selector — shown only when dates are NOT selected.
            // Automatically hides once tenant picks both check-in and check-out.
            item {
                AnimatedVisibility(
                    visible = selectedPkg != null && !datesSelected,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    if (selectedPkg != null && !datesSelected) {
                        Spacer(Modifier.height(sz.sectionGap - 4.dp))
                        PBNightsSelector(
                            nights    = selectedNights,
                            minNights = minNights,
                            maxNights = maxNights,
                            sz        = sz,
                            onMinus   = { if (selectedNights > minNights) selectedNights-- },
                            onPlus    = { if (selectedNights < maxNights) selectedNights++ }
                        )
                    }
                }
            }

            // Payment summary card — total, deposit (20%), remaining (80%)
            item {
                AnimatedVisibility(
                    visible = selectedPkg != null,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    if (selectedPkg != null) {
                        Spacer(Modifier.height(sz.sectionGap - 4.dp))
                        PBPaymentSummary(
                            pkg           = selectedPkg,
                            nights        = effectiveNights,
                            guestCount    = uiState.guestCount,
                            totalAmount   = totalAmount,
                            depositAmount = depositAmount,
                            sz            = sz
                        )
                    }
                }
            }

            // Error banner from ViewModel
            if (uiState.errorMessage != null) {
                item {
                    Spacer(Modifier.height(sz.vPad - 4.dp))
                    PBErrorCard(message = uiState.errorMessage ?: "", sz = sz)
                }
            }

            item { Spacer(Modifier.height(sz.sectionGap)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INLINE DATE PICKER
//
// Two-step calendar selection (tenant-driven, no landlord involvement):
//   Step 1: Tenant taps any available day → sets check-in (checkInDay > 0)
//   Step 2: Tenant taps a day AFTER check-in → sets check-out (checkOutDay > 0)
//
// Features:
//   • Previous / next month navigation
//   • Days before today are disabled (no past bookings)
//   • Selected range highlighted in gold
//   • "Reset" button clears both dates back to -1
//
// Sentinel rules (IMPORTANT — do NOT change to 0):
//   checkInDay  = -1 → not selected
//   checkOutDay = -1 → not selected
//   day > 0          → date is selected
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBDatePicker(
    checkInDay         : Int,
    checkInMonth       : Int,
    checkInYear        : Int,
    checkOutDay        : Int,
    checkOutMonth      : Int,
    checkOutYear       : Int,
    sz                 : PBSizes,
    onCheckInSelected  : (day: Int, month: Int, year: Int) -> Unit,
    onCheckOutSelected : (day: Int, month: Int, year: Int) -> Unit,
    onReset            : () -> Unit
) {
    // Today's components — used to disable past dates
    val today      = Calendar.getInstance()
    val todayDay   = today.get(Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(Calendar.MONTH)
    val todayYear  = today.get(Calendar.YEAR)

    // Which month/year is currently visible in the calendar grid
    var displayMonth by remember { mutableIntStateOf(todayMonth) }
    var displayYear  by remember { mutableIntStateOf(todayYear) }

    // Step-state derived from sentinel values
    // -1 = not selected, > 0 = selected
    val isPickingCheckIn  = checkInDay == -1              // No check-in yet
    val isPickingCheckOut = checkInDay > 0 && checkOutDay == -1  // Check-in set, need check-out
    val bothSelected      = checkInDay > 0 && checkOutDay > 0    // Both dates confirmed

    // Days in the displayed month
    val daysInMonth = Calendar.getInstance().apply {
        set(Calendar.YEAR, displayYear)
        set(Calendar.MONTH, displayMonth)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Weekday offset for the 1st of the month (0 = Sunday … 6 = Saturday)
    val firstDayOfWeek = Calendar.getInstance().apply {
        set(Calendar.YEAR, displayYear)
        set(Calendar.MONTH, displayMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1

    // Convert a date to a comparable integer for easy range checks
    fun toAbsoluteDay(d: Int, m: Int, y: Int) = y * 366 + m * 32 + d

    val checkInAbs  = if (checkInDay  > 0) toAbsoluteDay(checkInDay,  checkInMonth,  checkInYear)  else -1
    val checkOutAbs = if (checkOutDay > 0) toAbsoluteDay(checkOutDay, checkOutMonth, checkOutYear) else -1
    val todayAbs    = toAbsoluteDay(todayDay, todayMonth, todayYear)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad)
            .shadow(8.dp, RoundedCornerShape(sz.cardRadius))
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(PB_CardBg)
            .border(1.5.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
    ) {
        // Left gold accent bar
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(GoldGradient)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = sz.cardPad + 6.dp, vertical = sz.vPad + 2.dp)
        ) {

            // ── Header: icon + title + step hint + reset ──────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .size(sz.avatarSize)
                        .clip(CircleShape)
                        .background(PB_NavyDeep.copy(0.06f))
                        .border(1.dp, GoldBorder, CircleShape),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = PB_NavyMid, modifier = Modifier.size(sz.iconSize))
                }
                Spacer(Modifier.width(sz.hPad - 4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Select Your Dates",
                        fontWeight = FontWeight.Bold,
                        color      = PB_TextDark,
                        fontSize   = sz.titleSp.sp
                    )
                    Text(
                        text = when {
                            isPickingCheckIn  -> "Tap a day to set check-in"
                            isPickingCheckOut -> "Now tap your check-out day"
                            else -> "${MONTH_NAMES[checkInMonth]} $checkInDay → " +
                                    "${MONTH_NAMES[checkOutMonth]} $checkOutDay  •  " +
                                    "${calcNightsLocal(checkInDay, checkInMonth, checkInYear, checkOutDay, checkOutMonth, checkOutYear)} night(s)"
                        },
                        color      = if (bothSelected) PB_Success else PB_TextMuted,
                        fontSize   = sz.captionSp.sp,
                        fontWeight = if (bothSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                // Reset button — visible only after check-in is selected
                if (checkInDay > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PB_TextLight.copy(0.3f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = onReset
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Reset", color = PB_TextMuted, fontSize = sz.captionSp.sp)
                    }
                }
            }

            Spacer(Modifier.height(sz.vPad))

            // ── Check-in / Check-out summary boxes ───────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Check-in summary box
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (checkInDay > 0) PB_SuccessLight else PB_NavyDeep.copy(0.04f))
                        .border(
                            1.dp,
                            if (isPickingCheckIn) GoldBorder
                            else Brush.horizontalGradient(listOf(PB_Divider, PB_Divider)),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (checkInDay > 0) PB_Success else PB_TextMuted))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Check-in",
                            color      = if (checkInDay > 0) PB_Success else PB_TextMuted,
                            fontSize   = sz.captionSp.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (checkInDay > 0) "${MONTH_NAMES[checkInMonth].take(3)} $checkInDay" else "—",
                        color      = PB_TextDark,
                        fontSize   = sz.titleSp.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Arrow between boxes
                Box(Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.Default.ArrowForward, null, tint = PB_TextLight, modifier = Modifier.size(sz.iconSize))
                }

                // Check-out summary box
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (checkOutDay > 0) Color(0xFFFEE2E2) else PB_NavyDeep.copy(0.04f))
                        .border(
                            1.dp,
                            if (isPickingCheckOut) GoldBorder
                            else Brush.horizontalGradient(listOf(PB_Divider, PB_Divider)),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (checkOutDay > 0) Color(0xFFDC2626) else PB_TextMuted))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Check-out",
                            color      = if (checkOutDay > 0) Color(0xFFB91C1C) else PB_TextMuted,
                            fontSize   = sz.captionSp.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (checkOutDay > 0) "${MONTH_NAMES[checkOutMonth].take(3)} $checkOutDay" else "—",
                        color      = PB_TextDark,
                        fontSize   = sz.titleSp.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(sz.vPad))

            // ── Month navigation: ← [Month Year] → ───────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Previous month — disabled if already at current month
                Box(
                    modifier = Modifier
                        .size(sz.avatarSize - 8.dp)
                        .clip(CircleShape)
                        .background(PB_NavyDeep.copy(0.07f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) {
                            val prevCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, displayYear)
                                set(Calendar.MONTH, displayMonth)
                                add(Calendar.MONTH, -1)
                            }
                            val prevMonth = prevCal.get(Calendar.MONTH)
                            val prevYear  = prevCal.get(Calendar.YEAR)
                            // Block navigation before the current month
                            if (toAbsoluteDay(1, prevMonth, prevYear) >= toAbsoluteDay(1, todayMonth, todayYear)) {
                                displayMonth = prevMonth
                                displayYear  = prevYear
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronLeft, null, tint = PB_TextMuted, modifier = Modifier.size(sz.iconSize))
                }

                Text(
                    "${MONTH_NAMES[displayMonth]} $displayYear",
                    fontWeight = FontWeight.ExtraBold,
                    color      = PB_TextDark,
                    fontSize   = sz.titleSp.sp
                )

                // Next month — no forward limit
                Box(
                    modifier = Modifier
                        .size(sz.avatarSize - 8.dp)
                        .clip(CircleShape)
                        .background(PB_NavyDeep.copy(0.07f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) {
                            val nextCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, displayYear)
                                set(Calendar.MONTH, displayMonth)
                                add(Calendar.MONTH, 1)
                            }
                            displayMonth = nextCal.get(Calendar.MONTH)
                            displayYear  = nextCal.get(Calendar.YEAR)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronRight, null, tint = PB_TextMuted, modifier = Modifier.size(sz.iconSize))
                }
            }

            Spacer(Modifier.height(sz.vPad - 4.dp))

            // ── Weekday column labels ─────────────────────────────────────────
            Row(Modifier.fillMaxWidth()) {
                listOf("Su","Mo","Tu","We","Th","Fr","Sa").forEach { label ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            fontSize   = sz.captionSp.sp,
                            color      = PB_TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Calendar day grid ─────────────────────────────────────────────
            // Leading empty cells align the 1st to the correct weekday column.
            val totalCells = firstDayOfWeek + daysInMonth
            val rows       = (totalCells + 6) / 7   // ceiling division

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until rows) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day       = cellIndex - firstDayOfWeek + 1

                            if (day < 1 || day > daysInMonth) {
                                // Empty filler cell before the 1st or after the last day
                                Box(Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val cellAbs    = toAbsoluteDay(day, displayMonth, displayYear)
                                val isPast     = cellAbs < todayAbs
                                val isToday    = cellAbs == todayAbs
                                val isCheckIn  = cellAbs == checkInAbs
                                val isCheckOut = cellAbs == checkOutAbs
                                val isInRange  = checkInAbs > 0 && checkOutAbs > 0 &&
                                        cellAbs > checkInAbs && cellAbs < checkOutAbs

                                // A cell is tappable when:
                                //   • It is not in the past
                                //   • When picking check-out: it must be after check-in
                                val isSelectable = !isPast && when {
                                    isPickingCheckIn  -> true
                                    isPickingCheckOut -> cellAbs > checkInAbs
                                    else              -> true // both set — tap resets to new check-in
                                }

                                val bgColor = when {
                                    isCheckIn || isCheckOut -> PB_NavyDeep
                                    isInRange               -> PB_Gold.copy(0.18f)
                                    isToday                 -> PB_NavyDeep.copy(0.06f)
                                    else                    -> Color.Transparent
                                }
                                val textColor = when {
                                    isCheckIn || isCheckOut -> PB_Gold
                                    isPast                  -> PB_TextLight
                                    isInRange               -> PB_NavyMid
                                    isToday                 -> PB_NavyMid
                                    else                    -> PB_TextDark
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .then(
                                            if (isSelectable) Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication        = null
                                            ) {
                                                when {
                                                    // Both dates already set → reset and start new check-in
                                                    checkInDay > 0 && checkOutDay > 0 -> {
                                                        onCheckInSelected(day, displayMonth, displayYear)
                                                    }
                                                    // No check-in yet (sentinel = -1) → set this day as check-in
                                                    checkInDay == -1 -> {
                                                        onCheckInSelected(day, displayMonth, displayYear)
                                                    }
                                                    // Check-in set, tapped day is after it → set as check-out
                                                    cellAbs > checkInAbs -> {
                                                        onCheckOutSelected(day, displayMonth, displayYear)
                                                    }
                                                    // Tapped day is before or on check-in → treat as new check-in
                                                    else -> {
                                                        onCheckInSelected(day, displayMonth, displayYear)
                                                    }
                                                }
                                            }
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$day",
                                        fontSize   = sz.captionSp.sp,
                                        color      = textColor,
                                        fontWeight = if (isCheckIn || isCheckOut) FontWeight.ExtraBold else FontWeight.Normal,
                                        textAlign  = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOCAL NIGHT CALCULATION
// Used inside PBDatePicker header subtitle only.
// ViewModel.calculateNights() is the source of truth for pricing.
// ─────────────────────────────────────────────────────────────────────────────
private fun calcNightsLocal(
    inDay  : Int, inMonth  : Int, inYear  : Int,
    outDay : Int, outMonth : Int, outYear : Int
): Int {
    if (inDay <= 0 || outDay <= 0) return 0
    val checkIn = Calendar.getInstance().apply {
        set(Calendar.YEAR, inYear); set(Calendar.MONTH, inMonth); set(Calendar.DAY_OF_MONTH, inDay)
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val checkOut = Calendar.getInstance().apply {
        set(Calendar.YEAR, outYear); set(Calendar.MONTH, outMonth); set(Calendar.DAY_OF_MONTH, outDay)
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    return ((checkOut.timeInMillis - checkIn.timeInMillis) / (1000L * 60 * 60 * 24))
        .toInt().coerceAtLeast(0)
}

// ─────────────────────────────────────────────────────────────────────────────
// NIGHTS SELECTOR
// Shown only when tenant has NOT selected dates from the calendar.
// Hides automatically once both check-in and check-out are chosen.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBNightsSelector(
    nights   : Int,
    minNights: Int,
    maxNights: Int,
    sz       : PBSizes,
    onMinus  : () -> Unit,
    onPlus   : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad)
            .shadow(8.dp, RoundedCornerShape(sz.cardRadius))
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(PB_CardBg)
            .border(1.5.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().align(Alignment.CenterStart).background(GoldGradient))
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = sz.cardPad + 6.dp, vertical = sz.vPad + 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Box(
                    Modifier.size(sz.avatarSize).clip(CircleShape)
                        .background(PB_NavyDeep.copy(0.06f)).border(1.dp, GoldBorder, CircleShape),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.NightsStay, null, tint = PB_NavyMid, modifier = Modifier.size(sz.iconSize))
                }
                Spacer(Modifier.width(sz.hPad - 4.dp))
                Column {
                    Text("Number of Nights", fontWeight = FontWeight.Bold, color = PB_TextDark, fontSize = sz.titleSp.sp)
                    Text("Min ${minNights}N  •  Max ${maxNights}N", color = PB_TextMuted, fontSize = sz.captionSp.sp)
                }
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sz.hPad - 6.dp)
            ) {
                PBCounterButton("−", nights > minNights, onMinus, sz)
                Text(
                    "$nights",
                    fontSize   = (sz.titleSp + 6f).sp,
                    fontWeight = FontWeight.Black,
                    color      = PB_TextDark,
                    modifier   = Modifier.widthIn(min = (sz.avatarSize.value * 0.9f).dp),
                    textAlign  = TextAlign.Center
                )
                PBCounterButton("+", nights < maxNights, onPlus, sz)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR — navy gradient with gold accent and "20% OFF" badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBTopBar(
    propertyId   : String,
    propertyTitle: String,
    sz           : PBSizes,
    onBack       : () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().background(NavyGradient)) {
        // Decorative circles
        Box(
            Modifier.size((sz.avatarSize.value * 3.2f).dp).align(Alignment.TopEnd)
                .offset(x = (sz.avatarSize.value * 1.2f).dp, y = -(sz.avatarSize.value * 1.2f).dp)
                .clip(CircleShape).background(PB_Gold.copy(0.06f))
        )
        Box(
            Modifier.size((sz.avatarSize.value * 1.7f).dp).align(Alignment.BottomStart)
                .offset(x = -(sz.avatarSize.value * 0.5f).dp, y = (sz.avatarSize.value * 0.5f).dp)
                .clip(CircleShape).background(PB_Gold.copy(0.04f))
        )
        // Gold bottom border line
        Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(GoldBorder))

        Row(
            modifier          = Modifier.statusBarsPadding().padding(horizontal = sz.hPad, vertical = sz.vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier.size(sz.avatarSize).clip(CircleShape)
                    .background(PB_Gold.copy(0.15f)).border(1.5.dp, GoldBorder, CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PB_Gold, modifier = Modifier.size(sz.iconSize))
            }
            Spacer(Modifier.width(sz.hPad - 4.dp))
            Column(Modifier.weight(1f)) {
                Text("PRE-BOOKING", color = PB_Gold, fontSize = (sz.titleSp + 1f).sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        propertyTitle.isNotEmpty() -> propertyTitle
                        propertyId.isNotEmpty()    -> propertyId
                        else                       -> "Select a Package"
                    },
                    color    = Color.White.copy(0.55f),
                    fontSize = sz.bodySp.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Discount badge
            Box(
                modifier = Modifier.clip(RoundedCornerShape((sz.cardRadius.value * 0.55f).dp))
                    .background(PB_Gold.copy(0.12f))
                    .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.55f).dp))
                    .padding(horizontal = sz.hPad - 6.dp, vertical = sz.vPad - 8.dp)
            ) {
                Text("20% OFF", color = PB_Gold, fontSize = sz.captionSp.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM BAR — sticky "Pay Deposit" button with deposit and total amounts
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBBottomBar(
    depositAmount: Double,
    totalAmount  : Double,
    isLoading    : Boolean,
    sz           : PBSizes,
    onPay        : () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().shadow(20.dp).background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(GoldBorder))
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = sz.hPad, vertical = sz.vPad - 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Amount info
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(PB_Success))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Deposit Required (20%)",
                        color    = PB_TextMuted,
                        fontSize = sz.captionSp.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text("PKR ${"%,.0f".format(depositAmount)}", color = PB_NavyDeep, fontWeight = FontWeight.Black, fontSize = sz.depositSize.sp, letterSpacing = (-0.5).sp, maxLines = 1)
                Text("Total: PKR ${"%,.0f".format(totalAmount)}", color = PB_TextMuted, fontSize = sz.captionSp.sp, maxLines = 1)
            }
            // Pay Deposit button
            Box(
                modifier = Modifier.height(sz.btnHeight)
                    .clip(RoundedCornerShape((sz.cardRadius.value * 0.85f).dp))
                    .background(NavyGradient)
                    .border(1.5.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.85f).dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isLoading) { onPay() }
                    .padding(horizontal = sz.hPad + 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = PB_Gold, modifier = Modifier.size(sz.iconSize + 4.dp), strokeWidth = 2.5.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Lock, null, tint = PB_Gold, modifier = Modifier.size(sz.iconSize - 2.dp))
                        Text("Pay Deposit", color = PB_Gold, fontWeight = FontWeight.Black, fontSize = sz.titleSp.sp, letterSpacing = 0.3.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ADVANCE BOOKING BANNER — explains the 20% deposit concept to the tenant
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBAdvanceBanner(sz: PBSizes) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = sz.hPad)
            .shadow(8.dp, RoundedCornerShape(sz.cardRadius), ambientColor = PB_Gold.copy(0.18f), spotColor = PB_Gold.copy(0.12f))
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(Brush.linearGradient(listOf(PB_NavyDeep.copy(0.96f), PB_NavyMid.copy(0.96f))))
            .border(1.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(GoldBorder))
        Row(
            modifier          = Modifier.padding(horizontal = sz.hPad, vertical = sz.vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(sz.bannerIcon).clip(RoundedCornerShape((sz.cardRadius.value * 0.7f).dp))
                    .background(PB_Gold.copy(0.15f)).border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.7f).dp)),
                Alignment.Center
            ) { Text("📅", fontSize = (sz.titleSp + 5f).sp) }
            Spacer(Modifier.width(sz.hPad - 4.dp))
            Column(Modifier.weight(1f)) {
                Text("Advance Booking", fontWeight = FontWeight.Black, color = PB_Gold, fontSize = sz.titleSp.sp, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Pay only 20% deposit now to secure your stay. Remaining amount due on arrival.",
                    fontSize   = sz.captionSp.sp,
                    color      = Color.White.copy(0.65f),
                    lineHeight = (sz.captionSp + 5f).sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER — gold accent bar + title + optional deal count badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBSectionHeader(title: String, badgeCount: Int?, sz: PBSizes) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = sz.hPad), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height((sz.titleSp + 8f).dp).clip(RoundedCornerShape(2.dp)).background(GoldGradient))
        Spacer(Modifier.width(sz.hPad - 6.dp))
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = (sz.titleSp + 2f).sp, color = PB_TextDark, letterSpacing = 0.2.sp)
        Spacer(Modifier.weight(1f))
        if (badgeCount != null) {
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(PB_NavyDeep.copy(0.07f))
                    .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = sz.hPad - 4.dp, vertical = 5.dp)
            ) {
                Text("$badgeCount Deals", color = PB_GoldDim, fontSize = sz.captionSp.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOADING STATE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBLoadingState(sz: PBSizes) {
    Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PB_Gold, strokeWidth = 3.dp, modifier = Modifier.size(sz.bannerIcon))
            Spacer(Modifier.height(sz.vPad))
            Text("Loading packages...", color = PB_TextMuted, fontSize = sz.bodySp.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY STATE — shown when no packages exist for the property
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBEmptyState(propertyId: String, sz: PBSizes) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = sz.hPad)
            .clip(RoundedCornerShape(sz.cardRadius)).background(PB_CardBg)
            .border(1.5.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
            .padding(sz.hPad + sz.vPad),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(sz.bannerIcon + 20.dp).clip(CircleShape)
                    .background(PB_Gold.copy(0.1f)).border(1.5.dp, GoldBorder, CircleShape),
                Alignment.Center
            ) {
                Icon(Icons.Default.Inventory2, null, tint = PB_GoldDim, modifier = Modifier.size(sz.iconSize + 8.dp))
            }
            Spacer(Modifier.height(sz.vPad))
            Text("No packages available", color = PB_TextDark, fontWeight = FontWeight.SemiBold, fontSize = sz.titleSp.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text       = if (propertyId.isEmpty()) "propertyId missing!" else "Property: $propertyId",
                color      = if (propertyId.isEmpty()) MaterialTheme.colorScheme.error else PB_TextMuted,
                fontSize   = sz.bodySp.sp,
                fontWeight = if (propertyId.isEmpty()) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ERROR CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBErrorCard(message: String, sz: PBSizes) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = sz.hPad)
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(MaterialTheme.colorScheme.error.copy(0.06f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(0.3f), RoundedCornerShape(sz.cardRadius))
            .padding(sz.hPad - 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(sz.iconSize))
            Spacer(Modifier.width(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = sz.bodySp.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PACKAGE CARD
// Shows rental package details with animated gold border when selected.
// Selecting a new package also resets dates (handled in ViewModel.selectPackage).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBPackageCard(pkg: RentalPackage, isSelected: Boolean, sz: PBSizes, onClick: () -> Unit) {
    val animatedElevation by animateDpAsState(
        targetValue   = if (isSelected) 12.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "cardElevation"
    )
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = sz.hPad, vertical = (sz.vPad.value * 0.45f).dp)
            .shadow(animatedElevation, RoundedCornerShape(sz.cardRadius),
                ambientColor = if (isSelected) PB_Gold.copy(0.3f) else PB_TextMuted.copy(0.1f),
                spotColor    = if (isSelected) PB_Gold.copy(0.2f) else Color.Transparent)
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(
                if (isSelected) Brush.linearGradient(listOf(PB_NavyDeep.copy(0.04f), PB_Gold.copy(0.03f), PB_CardBg))
                else            Brush.linearGradient(listOf(PB_CardBg, PB_CardBg))
            )
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) GoldBorder else Brush.horizontalGradient(listOf(PB_Divider, PB_Divider)),
                RoundedCornerShape(sz.cardRadius)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
    ) {
        // Gold top stripe shown only when selected
        if (isSelected) Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(GoldBorder))
        Column(Modifier.padding(horizontal = sz.cardPad, vertical = sz.vPad + 4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = sz.hPad - 4.dp)) {
                    // Optional badge (e.g. "🔥 Summer Deal")
                    if (pkg.badgeLabel.isNotEmpty()) {
                        Box(
                            Modifier.clip(RoundedCornerShape((sz.cardRadius.value * 0.55f).dp))
                                .background(PB_Gold.copy(0.12f))
                                .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.55f).dp))
                                .padding(horizontal = sz.hPad - 6.dp, vertical = 4.dp)
                        ) {
                            Text(pkg.badgeLabel, color = PB_GoldDim, fontSize = sz.captionSp.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(pkg.packageName, fontWeight = FontWeight.ExtraBold, fontSize = (sz.titleSp + 2f).sp, color = PB_TextDark, letterSpacing = 0.1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(pkg.propertyTitle, color = PB_TextMuted, fontSize = sz.bodySp.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Animated checkmark when selected
                AnimatedVisibility(visible = isSelected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Box(
                        Modifier.size(sz.checkSize + 16.dp).clip(CircleShape)
                            .background(NavyGradient).border(1.5.dp, GoldBorder, CircleShape),
                        Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = PB_Gold, modifier = Modifier.size(sz.checkSize))
                    }
                }
            }
            Spacer(Modifier.height(sz.vPad))
            // Pricing: discounted / original strikethrough / savings badge
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(pkg.formattedDiscountedPrice, fontWeight = FontWeight.Black, fontSize = sz.priceSize.sp, color = PB_TextDark, letterSpacing = (-1).sp)
                Text("/night", color = PB_TextMuted, fontSize = sz.bodySp.sp, modifier = Modifier.padding(start = 4.dp, bottom = 3.dp))
                Spacer(Modifier.width(8.dp))
                Text(pkg.formattedOriginalPrice, color = PB_TextMuted, fontSize = sz.bodySp.sp, textDecoration = TextDecoration.LineThrough, modifier = Modifier.padding(bottom = 3.dp))
                if (pkg.savingsLabel.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(PB_SuccessLight).padding(horizontal = 7.dp, vertical = 4.dp)) {
                        Text(pkg.savingsLabel, color = PB_Success, fontSize = sz.captionSp.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.height(sz.vPad))
            // Info chips: min nights, max nights, remaining slots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PBInfoChip(Icons.Default.NightsStay, "Min ${pkg.minNights}N", PB_TextMuted, sz)
                if (pkg.maxNights != null) PBInfoChip(Icons.Default.EventAvailable, "Max ${pkg.maxNights}N", PB_TextMuted, sz)
                if (pkg.remainingSlots != null) {
                    val isLow = (pkg.remainingSlots ?: 0) <= 2
                    PBInfoChip(Icons.Default.ConfirmationNumber, "${pkg.remainingSlots} slots left",
                        if (isLow) MaterialTheme.colorScheme.error else PB_Success, sz)
                }
            }
            // Inclusions (if any)
            if (pkg.inclusions.isNotEmpty()) {
                Spacer(Modifier.height(sz.vPad))
                Divider(color = PB_Divider)
                Spacer(Modifier.height(sz.vPad - 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(GoldGradient))
                    Spacer(Modifier.width(8.dp))
                    Text("Includes", color = PB_TextDark, fontSize = sz.bodySp.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(sz.vPad - 4.dp))
                // Up to 4 inclusions in a 2-column grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pkg.inclusions.take(4).chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape((sz.cardRadius.value * 0.65f).dp))
                                        .background(PB_Gold.copy(0.05f))
                                        .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.65f).dp))
                                        .padding(horizontal = sz.hPad - 6.dp, vertical = sz.vPad - 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(sz.iconSize + 2.dp).clip(CircleShape).background(PB_SuccessLight), Alignment.Center) {
                                            Icon(Icons.Default.Check, null, tint = PB_Success, modifier = Modifier.size(sz.iconSize - 4.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(item, fontSize = sz.captionSp.sp, color = PB_TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INFO CHIP — small pill for min/max nights and slot count
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBInfoChip(icon: ImageVector, label: String, tint: Color, sz: PBSizes) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(tint.copy(0.07f))
            .padding(horizontal = sz.hPad - 7.dp, vertical = sz.vPad - 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(sz.iconSize - 3.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = tint, fontSize = sz.captionSp.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GUEST COUNTER — min 1, max 20
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBGuestCounter(guestCount: Int, sz: PBSizes, onMinus: () -> Unit, onPlus: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = sz.hPad)
            .shadow(8.dp, RoundedCornerShape(sz.cardRadius))
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(PB_CardBg)
            .border(1.5.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().align(Alignment.CenterStart).background(GoldGradient))
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = sz.cardPad + 6.dp, vertical = sz.vPad + 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Box(Modifier.size(sz.avatarSize).clip(CircleShape).background(PB_NavyDeep.copy(0.06f)).border(1.dp, GoldBorder, CircleShape), Alignment.Center) {
                    Icon(Icons.Default.People, null, tint = PB_NavyMid, modifier = Modifier.size(sz.iconSize))
                }
                Spacer(Modifier.width(sz.hPad - 4.dp))
                Column {
                    Text("Number of Guests", fontWeight = FontWeight.Bold, color = PB_TextDark, fontSize = sz.titleSp.sp)
                    Text("Max 20 guests allowed", color = PB_TextMuted, fontSize = sz.captionSp.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sz.hPad - 6.dp)) {
                PBCounterButton("−", guestCount > 1, onMinus, sz)
                Text("$guestCount", fontSize = (sz.titleSp + 6f).sp, fontWeight = FontWeight.Black, color = PB_TextDark, modifier = Modifier.widthIn(min = (sz.avatarSize.value * 0.9f).dp), textAlign = TextAlign.Center)
                PBCounterButton("+", guestCount < 20, onPlus, sz)
            }
        }
    }
}

// Reusable circular counter button (increment / decrement)
@Composable
private fun PBCounterButton(symbol: String, enabled: Boolean, onClick: () -> Unit, sz: PBSizes) {
    Box(
        modifier = Modifier.size(sz.avatarSize - 2.dp).clip(CircleShape)
            .background(if (enabled) NavyGradient else Brush.linearGradient(listOf(Color(0xFFEEEEEE), Color(0xFFE0E0E0))))
            .border(1.dp, if (enabled) GoldBorder else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)), CircleShape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = if (enabled) PB_Gold else PB_TextMuted, fontSize = (sz.titleSp + 3f).sp, fontWeight = FontWeight.Bold, lineHeight = (sz.titleSp + 3f).sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAYMENT SUMMARY CARD
// Shows package, nights, guests, total, deposit (20%), and remaining (80%).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBPaymentSummary(
    pkg          : RentalPackage,
    nights       : Int,
    guestCount   : Int,
    totalAmount  : Double,
    depositAmount: Double,
    sz           : PBSizes
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = sz.hPad)
            .shadow(10.dp, RoundedCornerShape(sz.cardRadius), ambientColor = PB_Gold.copy(0.18f), spotColor = PB_Gold.copy(0.1f))
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(PB_CardBg)
            .border(2.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(GoldBorder))
        Column(Modifier.padding(horizontal = sz.cardPad, vertical = sz.vPad + 4.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(sz.avatarSize)
                        .clip(RoundedCornerShape((sz.cardRadius.value * 0.65f).dp))
                        .background(PB_NavyDeep.copy(0.06f))
                        .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.65f).dp)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Receipt, null, tint = PB_NavyMid, modifier = Modifier.size(sz.iconSize))
                }
                Spacer(Modifier.width(sz.hPad - 4.dp))
                Column {
                    Text("Payment Summary", fontWeight = FontWeight.ExtraBold, color = PB_TextDark, fontSize = (sz.titleSp + 1f).sp, letterSpacing = 0.2.sp)
                    Text("Transparent pricing breakdown", color = PB_TextMuted, fontSize = sz.captionSp.sp)
                }
            }
            Spacer(Modifier.height(sz.vPad))
            Divider(color = PB_Divider)
            Spacer(Modifier.height(sz.vPad - 2.dp))
            PBSummaryRow("Package",    pkg.packageName,              sz)
            PBSummaryRow("Rate/Night", pkg.formattedDiscountedPrice, sz, strikethrough = pkg.formattedOriginalPrice)
            PBSummaryRow("Duration",   "$nights nights",             sz)
            PBSummaryRow("Guests",     "$guestCount guests",         sz)
            Spacer(Modifier.height(sz.vPad - 2.dp))
            Divider(color = PB_Divider)
            Spacer(Modifier.height(sz.vPad - 2.dp))
            // Total amount row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total Amount", fontWeight = FontWeight.Bold, color = PB_TextDark, fontSize = sz.titleSp.sp)
                Text("PKR ${"%,.0f".format(totalAmount)}", fontWeight = FontWeight.Black, color = PB_TextDark, fontSize = (sz.titleSp + 1f).sp)
            }
            Spacer(Modifier.height(sz.vPad - 2.dp))
            // Deposit highlight box — 20% to pay now, 80% due on arrival
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape((sz.cardRadius.value * 0.75f).dp))
                    .background(Brush.linearGradient(listOf(PB_WarningLight, PB_GoldFaint)))
                    .border(1.5.dp,
                        Brush.horizontalGradient(listOf(PB_Warning.copy(0.6f), PB_Gold.copy(0.5f), PB_Warning.copy(0.6f))),
                        RoundedCornerShape((sz.cardRadius.value * 0.75f).dp))
                    .padding(horizontal = sz.cardPad, vertical = sz.vPad)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = PB_Warning, modifier = Modifier.size(sz.iconSize - 3.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Pay Now (20%)", fontWeight = FontWeight.ExtraBold, color = PB_Warning, fontSize = sz.titleSp.sp)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("Due on arrival: PKR ${"%,.0f".format(totalAmount - depositAmount)}", color = PB_TextMuted, fontSize = sz.captionSp.sp)
                    }
                    Text("PKR ${"%,.0f".format(depositAmount)}", fontWeight = FontWeight.Black, color = PB_Warning, fontSize = (sz.titleSp + 4f).sp, letterSpacing = (-0.5).sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUMMARY ROW — label on left, value (+ optional strikethrough) on right
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBSummaryRow(label: String, value: String, sz: PBSizes, strikethrough: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = (sz.vPad.value * 0.28f).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PB_TextMuted, fontSize = sz.bodySp.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (strikethrough != null) {
                Text(
                    strikethrough,
                    color = PB_TextLight,
                    fontSize = sz.captionSp.sp,
                    textDecoration = TextDecoration.LineThrough
                )
            }
            Text(
                value,
                fontWeight = FontWeight.SemiBold,
                color = PB_TextDark,
                fontSize = sz.bodySp.sp
            )
        }
    }
}