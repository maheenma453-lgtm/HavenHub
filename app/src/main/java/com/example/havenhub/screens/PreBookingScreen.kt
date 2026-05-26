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

// ─── Color Palette ────────────────────────────────────────────────────────────
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

private val GoldGradient = Brush.horizontalGradient(
    listOf(PB_Gold.copy(0.9f), PB_GoldLight.copy(0.6f), PB_Gold.copy(0.9f))
)
private val GoldBorder = Brush.horizontalGradient(
    listOf(PB_Gold.copy(0.85f), PB_GoldLight.copy(0.5f), PB_Gold.copy(0.85f))
)
private val NavyGradient     = Brush.linearGradient(listOf(PB_NavyDeep, PB_NavyMid))
private val NavySoftGradient = Brush.linearGradient(listOf(PB_NavyPrime, PB_NavyLight))

// ─── Responsive Size System ───────────────────────────────────────────────────
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
            w < 320 -> PBSizes(
                hPad = 10.dp, vPad = 8.dp, cardRadius = 10.dp,
                titleSp = 11f, bodySp = 9.5f, captionSp = 8.5f,
                iconSize = 13.dp, avatarSize = 32.dp, chipHeight = 24.dp,
                btnHeight = 40.dp, sectionGap = 10.dp, cardPad = 10.dp,
                priceSize = 18f, depositSize = 17f,
                checkSize = 14.dp, bannerIcon = 36.dp
            )
            w < 360 -> PBSizes(
                hPad = 12.dp, vPad = 10.dp, cardRadius = 12.dp,
                titleSp = 12f, bodySp = 10.5f, captionSp = 9f,
                iconSize = 14.dp, avatarSize = 34.dp, chipHeight = 26.dp,
                btnHeight = 44.dp, sectionGap = 12.dp, cardPad = 12.dp,
                priceSize = 20f, depositSize = 19f,
                checkSize = 15.dp, bannerIcon = 38.dp
            )
            w < 390 -> PBSizes(
                hPad = 14.dp, vPad = 12.dp, cardRadius = 14.dp,
                titleSp = 13f, bodySp = 11f, captionSp = 9.5f,
                iconSize = 15.dp, avatarSize = 36.dp, chipHeight = 28.dp,
                btnHeight = 46.dp, sectionGap = 14.dp, cardPad = 14.dp,
                priceSize = 21f, depositSize = 20f,
                checkSize = 16.dp, bannerIcon = 40.dp
            )
            w < 430 -> PBSizes(
                hPad = 16.dp, vPad = 14.dp, cardRadius = 16.dp,
                titleSp = 14f, bodySp = 11.5f, captionSp = 10f,
                iconSize = 16.dp, avatarSize = 38.dp, chipHeight = 30.dp,
                btnHeight = 48.dp, sectionGap = 16.dp, cardPad = 16.dp,
                priceSize = 22f, depositSize = 21f,
                checkSize = 18.dp, bannerIcon = 42.dp
            )
            w < 480 -> PBSizes(
                hPad = 18.dp, vPad = 15.dp, cardRadius = 17.dp,
                titleSp = 15f, bodySp = 12f, captionSp = 10.5f,
                iconSize = 17.dp, avatarSize = 40.dp, chipHeight = 32.dp,
                btnHeight = 50.dp, sectionGap = 17.dp, cardPad = 17.dp,
                priceSize = 23f, depositSize = 22f,
                checkSize = 19.dp, bannerIcon = 44.dp
            )
            w < 600 -> PBSizes(
                hPad = 20.dp, vPad = 16.dp, cardRadius = 18.dp,
                titleSp = 16f, bodySp = 13f, captionSp = 11f,
                iconSize = 18.dp, avatarSize = 42.dp, chipHeight = 34.dp,
                btnHeight = 52.dp, sectionGap = 18.dp, cardPad = 18.dp,
                priceSize = 24f, depositSize = 23f,
                checkSize = 20.dp, bannerIcon = 46.dp
            )
            else -> PBSizes(
                hPad = 26.dp, vPad = 20.dp, cardRadius = 20.dp,
                titleSp = 17f, bodySp = 14f, captionSp = 12f,
                iconSize = 20.dp, avatarSize = 46.dp, chipHeight = 38.dp,
                btnHeight = 56.dp, sectionGap = 20.dp, cardPad = 20.dp,
                priceSize = 26f, depositSize = 25f,
                checkSize = 22.dp, bannerIcon = 50.dp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRE-BOOKING SCREEN
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

    var selectedNights by remember(selectedPkg?.packageId) {
        mutableIntStateOf(minNights)
    }

    LaunchedEffect(selectedPkg?.packageId) {
        selectedNights = minNights
    }

    val totalAmount   = (selectedPkg?.discountedPricePerNight ?: 0.0) * selectedNights
    val depositAmount = totalAmount * 0.20

    LaunchedEffect(propertyId) {
        Log.d("PRE_DEBUG", "LaunchedEffect fired — propertyId = '$propertyId'")
        if (propertyId.isNotEmpty()) {
            viewModel.loadPackagesForProperty(propertyId)
        } else {
            Log.e("PRE_DEBUG", "propertyId is EMPTY — packages will not load!")
        }
    }

    // Navigate to Payment screen after booking is created successfully
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
                    paymentType = "DEPOSIT"
                )
            )
        }
    }

    val packages = uiState.propertyPackages

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
                visible = selectedPkg != null,
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
                                isPreBooking    = true,
                                securityDeposit = 0.0,
                                status          = BookingStatus.PENDING_APPROVAL.name,
                                paymentStatus   = PaymentStatus.PENDING.name,
                                propertyAddress = selectedPkg.propertyTitle,
                                totalNights     = selectedNights,
                                guestCount      = uiState.guestCount,
                                paymentMethod   = "Pending"
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

            // Advance Booking Banner
            item {
                Spacer(Modifier.height(sz.sectionGap))
                PBAdvanceBanner(sz)
            }

            // Section Header
            item {
                Spacer(Modifier.height(sz.sectionGap + 4.dp))
                PBSectionHeader(
                    title      = "Available Packages",
                    badgeCount = if (packages.isNotEmpty()) packages.size else null,
                    sz         = sz
                )
                Spacer(Modifier.height(sz.vPad - 4.dp))
            }

            // Loading State
            if (uiState.isLoading) {
                item { PBLoadingState(sz) }
            }
            // Empty State
            else if (packages.isEmpty()) {
                item { PBEmptyState(propertyId = propertyId, sz = sz) }
            }
            // Package Cards
            else {
                items(packages, key = { it.packageId }) { pkg ->
                    PBPackageCard(
                        pkg        = pkg,
                        isSelected = selectedPkg?.packageId == pkg.packageId,
                        sz         = sz,
                        onClick    = { viewModel.selectPackage(pkg) }
                    )
                }
            }

            // Guest Counter
            item {
                Spacer(Modifier.height(sz.sectionGap - 4.dp))
                PBGuestCounter(
                    guestCount = uiState.guestCount,
                    sz         = sz,
                    onMinus    = { viewModel.setGuestCount(uiState.guestCount - 1) },
                    onPlus     = { viewModel.setGuestCount(uiState.guestCount + 1) }
                )
            }

            // Nights Selector — only shown when a package is selected
            item {
                AnimatedVisibility(
                    visible = selectedPkg != null,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    if (selectedPkg != null) {
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

            // Payment Summary — only shown when a package is selected
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
                            nights        = selectedNights,
                            guestCount    = uiState.guestCount,
                            totalAmount   = totalAmount,
                            depositAmount = depositAmount,
                            sz            = sz
                        )
                    }
                }
            }

            // Error Message
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
// NIGHTS SELECTOR
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
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(GoldGradient)
        )
        Row(
            modifier = Modifier
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
                    Modifier
                        .size(sz.avatarSize)
                        .clip(CircleShape)
                        .background(PB_NavyDeep.copy(0.06f))
                        .border(1.dp, GoldBorder, CircleShape),
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Default.NightsStay,
                        contentDescription = null,
                        tint     = PB_NavyMid,
                        modifier = Modifier.size(sz.iconSize)
                    )
                }
                Spacer(Modifier.width(sz.hPad - 4.dp))
                Column {
                    Text(
                        "Number of Nights",
                        fontWeight = FontWeight.Bold,
                        color      = PB_TextDark,
                        fontSize   = sz.titleSp.sp
                    )
                    Text(
                        "Min ${minNights}N  •  Max ${maxNights}N",
                        color    = PB_TextMuted,
                        fontSize = sz.captionSp.sp
                    )
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
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBTopBar(
    propertyId   : String,
    propertyTitle: String,
    sz           : PBSizes,
    onBack       : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyGradient)
    ) {
        Box(
            Modifier
                .size((sz.avatarSize.value * 3.2f).dp)
                .align(Alignment.TopEnd)
                .offset(x = (sz.avatarSize.value * 1.2f).dp, y = -(sz.avatarSize.value * 1.2f).dp)
                .clip(CircleShape)
                .background(PB_Gold.copy(0.06f))
        )
        Box(
            Modifier
                .size((sz.avatarSize.value * 1.7f).dp)
                .align(Alignment.BottomStart)
                .offset(x = -(sz.avatarSize.value * 0.5f).dp, y = (sz.avatarSize.value * 0.5f).dp)
                .clip(CircleShape)
                .background(PB_Gold.copy(0.04f))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .background(GoldBorder)
        )
        Row(
            modifier          = Modifier
                .statusBarsPadding()
                .padding(horizontal = sz.hPad, vertical = sz.vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(sz.avatarSize)
                    .clip(CircleShape)
                    .background(PB_Gold.copy(0.15f))
                    .border(1.5.dp, GoldBorder, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint     = PB_Gold,
                    modifier = Modifier.size(sz.iconSize)
                )
            }
            Spacer(Modifier.width(sz.hPad - 4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "PRE-BOOKING",
                    color         = PB_Gold,
                    fontSize      = (sz.titleSp + 1f).sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp
                )
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape((sz.cardRadius.value * 0.55f).dp))
                    .background(PB_Gold.copy(0.12f))
                    .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.55f).dp))
                    .padding(horizontal = sz.hPad - 6.dp, vertical = sz.vPad - 8.dp)
            ) {
                Text(
                    "20% OFF",
                    color         = PB_Gold,
                    fontSize      = sz.captionSp.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBBottomBar(
    depositAmount: Double,
    totalAmount  : Double,
    isLoading    : Boolean,
    sz           : PBSizes,
    onPay        : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp)
            .background(Color.White)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(GoldBorder)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = sz.hPad, vertical = sz.vPad - 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(PB_Success)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Deposit Required (20%)",
                        color      = PB_TextMuted,
                        fontSize   = sz.captionSp.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "PKR ${"%,.0f".format(depositAmount)}",
                    color         = PB_NavyDeep,
                    fontWeight    = FontWeight.Black,
                    fontSize      = sz.depositSize.sp,
                    letterSpacing = (-0.5).sp,
                    maxLines      = 1
                )
                Text(
                    "Total: PKR ${"%,.0f".format(totalAmount)}",
                    color    = PB_TextMuted,
                    fontSize = sz.captionSp.sp,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .height(sz.btnHeight)
                    .clip(RoundedCornerShape((sz.cardRadius.value * 0.85f).dp))
                    .background(NavyGradient)
                    .border(1.5.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.85f).dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        enabled           = !isLoading
                    ) { onPay() }
                    .padding(horizontal = sz.hPad + 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = PB_Gold,
                        modifier    = Modifier.size(sz.iconSize + 4.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint     = PB_Gold,
                            modifier = Modifier.size(sz.iconSize - 2.dp)
                        )
                        Text(
                            "Pay Deposit",
                            color         = PB_Gold,
                            fontWeight    = FontWeight.Black,
                            fontSize      = sz.titleSp.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ADVANCE BOOKING BANNER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBAdvanceBanner(sz: PBSizes) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad)
            .shadow(
                elevation    = 8.dp,
                shape        = RoundedCornerShape(sz.cardRadius),
                ambientColor = PB_Gold.copy(0.18f),
                spotColor    = PB_Gold.copy(0.12f)
            )
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(
                Brush.linearGradient(listOf(PB_NavyDeep.copy(0.96f), PB_NavyMid.copy(0.96f)))
            )
            .border(1.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(GoldBorder)
        )
        Row(
            modifier          = Modifier.padding(horizontal = sz.hPad, vertical = sz.vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(sz.bannerIcon)
                    .clip(RoundedCornerShape((sz.cardRadius.value * 0.7f).dp))
                    .background(PB_Gold.copy(0.15f))
                    .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.7f).dp)),
                Alignment.Center
            ) {
                Text("📅", fontSize = (sz.titleSp + 5f).sp)
            }
            Spacer(Modifier.width(sz.hPad - 4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Advance Booking",
                    fontWeight    = FontWeight.Black,
                    color         = PB_Gold,
                    fontSize      = sz.titleSp.sp,
                    letterSpacing = 0.3.sp
                )
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
// SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBSectionHeader(title: String, badgeCount: Int?, sz: PBSizes) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height((sz.titleSp + 8f).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GoldGradient)
        )
        Spacer(Modifier.width(sz.hPad - 6.dp))
        Text(
            title,
            fontWeight    = FontWeight.ExtraBold,
            fontSize      = (sz.titleSp + 2f).sp,
            color         = PB_TextDark,
            letterSpacing = 0.2.sp
        )
        Spacer(Modifier.weight(1f))
        if (badgeCount != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PB_NavyDeep.copy(0.07f))
                    .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = sz.hPad - 4.dp, vertical = 5.dp)
            ) {
                Text(
                    "$badgeCount Deals",
                    color      = PB_GoldDim,
                    fontSize   = sz.captionSp.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOADING STATE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBLoadingState(sz: PBSizes) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp),
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color       = PB_Gold,
                strokeWidth = 3.dp,
                modifier    = Modifier.size(sz.bannerIcon)
            )
            Spacer(Modifier.height(sz.vPad))
            Text(
                "Loading packages...",
                color    = PB_TextMuted,
                fontSize = sz.bodySp.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBEmptyState(propertyId: String, sz: PBSizes) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad)
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(PB_CardBg)
            .border(1.5.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
            .padding(sz.hPad + sz.vPad),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(sz.bannerIcon + 20.dp)
                    .clip(CircleShape)
                    .background(PB_Gold.copy(0.1f))
                    .border(1.5.dp, GoldBorder, CircleShape),
                Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint     = PB_GoldDim,
                    modifier = Modifier.size(sz.iconSize + 8.dp)
                )
            }
            Spacer(Modifier.height(sz.vPad))
            Text(
                "No packages available",
                color      = PB_TextDark,
                fontWeight = FontWeight.SemiBold,
                fontSize   = sz.titleSp.sp
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad)
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(MaterialTheme.colorScheme.error.copy(0.06f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(0.3f), RoundedCornerShape(sz.cardRadius))
            .padding(sz.hPad - 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(sz.iconSize)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                message,
                color    = MaterialTheme.colorScheme.error,
                fontSize = sz.bodySp.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PACKAGE CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBPackageCard(
    pkg       : RentalPackage,
    isSelected: Boolean,
    sz        : PBSizes,
    onClick   : () -> Unit
) {
    val animatedElevation by animateDpAsState(
        targetValue   = if (isSelected) 12.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "cardElevation"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad, vertical = (sz.vPad.value * 0.45f).dp)
            .shadow(
                elevation    = animatedElevation,
                shape        = RoundedCornerShape(sz.cardRadius),
                ambientColor = if (isSelected) PB_Gold.copy(0.3f) else PB_TextMuted.copy(0.1f),
                spotColor    = if (isSelected) PB_Gold.copy(0.2f) else Color.Transparent
            )
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(
                if (isSelected)
                    Brush.linearGradient(listOf(PB_NavyDeep.copy(0.04f), PB_Gold.copy(0.03f), PB_CardBg))
                else
                    Brush.linearGradient(listOf(PB_CardBg, PB_CardBg))
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) GoldBorder
                else Brush.horizontalGradient(listOf(PB_Divider, PB_Divider)),
                shape = RoundedCornerShape(sz.cardRadius)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { onClick() }
    ) {
        if (isSelected) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(GoldBorder)
            )
        }
        Column(Modifier.padding(horizontal = sz.cardPad, vertical = sz.vPad + 4.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(Modifier.weight(1f).padding(end = sz.hPad - 4.dp)) {
                    if (pkg.badgeLabel.isNotEmpty()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape((sz.cardRadius.value * 0.55f).dp))
                                .background(PB_Gold.copy(0.12f))
                                .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.55f).dp))
                                .padding(horizontal = sz.hPad - 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                pkg.badgeLabel,
                                color         = PB_GoldDim,
                                fontSize      = sz.captionSp.sp,
                                fontWeight    = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        pkg.packageName,
                        fontWeight    = FontWeight.ExtraBold,
                        fontSize      = (sz.titleSp + 2f).sp,
                        color         = PB_TextDark,
                        letterSpacing = 0.1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        pkg.propertyTitle,
                        color    = PB_TextMuted,
                        fontSize = sz.bodySp.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AnimatedVisibility(
                    visible = isSelected,
                    enter   = scaleIn() + fadeIn(),
                    exit    = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(sz.checkSize + 16.dp)
                            .clip(CircleShape)
                            .background(NavyGradient)
                            .border(1.5.dp, GoldBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = PB_Gold,
                            modifier = Modifier.size(sz.checkSize)
                        )
                    }
                }
            }

            Spacer(Modifier.height(sz.vPad))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Text(
                    pkg.formattedDiscountedPrice,
                    fontWeight    = FontWeight.Black,
                    fontSize      = sz.priceSize.sp,
                    color         = PB_TextDark,
                    letterSpacing = (-1).sp
                )
                Text(
                    "/night",
                    color    = PB_TextMuted,
                    fontSize = sz.bodySp.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    pkg.formattedOriginalPrice,
                    color          = PB_TextMuted,
                    fontSize       = sz.bodySp.sp,
                    textDecoration = TextDecoration.LineThrough,
                    modifier       = Modifier.padding(bottom = 3.dp)
                )
                if (pkg.savingsLabel.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PB_SuccessLight)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            pkg.savingsLabel,
                            color      = PB_Success,
                            fontSize   = sz.captionSp.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(sz.vPad))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                PBInfoChip(Icons.Default.NightsStay, "Min ${pkg.minNights}N", PB_TextMuted, sz)
                if (pkg.maxNights != null)
                    PBInfoChip(Icons.Default.EventAvailable, "Max ${pkg.maxNights}N", PB_TextMuted, sz)
                if (pkg.remainingSlots != null) {
                    val isLow = (pkg.remainingSlots ?: 0) <= 2
                    PBInfoChip(
                        icon  = Icons.Default.ConfirmationNumber,
                        label = "${pkg.remainingSlots} slots left",
                        tint  = if (isLow) MaterialTheme.colorScheme.error else PB_Success,
                        sz    = sz
                    )
                }
            }

            if (pkg.inclusions.isNotEmpty()) {
                Spacer(Modifier.height(sz.vPad))
                Divider(color = PB_Divider)
                Spacer(Modifier.height(sz.vPad - 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(GoldGradient)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Includes",
                        color      = PB_TextDark,
                        fontSize   = sz.bodySp.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.height(sz.vPad - 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pkg.inclusions.take(4).chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape((sz.cardRadius.value * 0.65f).dp))
                                        .background(PB_Gold.copy(0.05f))
                                        .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.65f).dp))
                                        .padding(horizontal = sz.hPad - 6.dp, vertical = sz.vPad - 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(sz.iconSize + 2.dp)
                                                .clip(CircleShape)
                                                .background(PB_SuccessLight),
                                            Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint     = PB_Success,
                                                modifier = Modifier.size(sz.iconSize - 4.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            item,
                                            fontSize   = sz.captionSp.sp,
                                            color      = PB_TextDark,
                                            maxLines   = 1,
                                            overflow   = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Medium
                                        )
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
// INFO CHIP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBInfoChip(icon: ImageVector, label: String, tint: Color, sz: PBSizes) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
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
// GUEST COUNTER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBGuestCounter(
    guestCount: Int,
    sz        : PBSizes,
    onMinus   : () -> Unit,
    onPlus    : () -> Unit
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
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(GoldGradient)
        )
        Row(
            modifier = Modifier
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
                    Modifier
                        .size(sz.avatarSize)
                        .clip(CircleShape)
                        .background(PB_NavyDeep.copy(0.06f))
                        .border(1.dp, GoldBorder, CircleShape),
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint     = PB_NavyMid,
                        modifier = Modifier.size(sz.iconSize)
                    )
                }
                Spacer(Modifier.width(sz.hPad - 4.dp))
                Column {
                    Text(
                        "Number of Guests",
                        fontWeight = FontWeight.Bold,
                        color      = PB_TextDark,
                        fontSize   = sz.titleSp.sp
                    )
                    Text(
                        "Max 20 guests allowed",
                        color    = PB_TextMuted,
                        fontSize = sz.captionSp.sp
                    )
                }
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sz.hPad - 6.dp)
            ) {
                PBCounterButton("−", guestCount > 1, onMinus, sz)
                Text(
                    "$guestCount",
                    fontSize   = (sz.titleSp + 6f).sp,
                    fontWeight = FontWeight.Black,
                    color      = PB_TextDark,
                    modifier   = Modifier.widthIn(min = (sz.avatarSize.value * 0.9f).dp),
                    textAlign  = TextAlign.Center
                )
                PBCounterButton("+", guestCount < 20, onPlus, sz)
            }
        }
    }
}

@Composable
private fun PBCounterButton(symbol: String, enabled: Boolean, onClick: () -> Unit, sz: PBSizes) {
    Box(
        modifier = Modifier
            .size(sz.avatarSize - 2.dp)
            .clip(CircleShape)
            .background(
                if (enabled) NavyGradient
                else Brush.linearGradient(listOf(Color(0xFFEEEEEE), Color(0xFFE0E0E0)))
            )
            .border(
                width = 1.dp,
                brush = if (enabled) GoldBorder
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                shape = CircleShape
            )
            .clickable(
                enabled           = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            color      = if (enabled) PB_Gold else PB_TextMuted,
            fontSize   = (sz.titleSp + 3f).sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (sz.titleSp + 3f).sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAYMENT SUMMARY
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad)
            .shadow(
                elevation    = 10.dp,
                shape        = RoundedCornerShape(sz.cardRadius),
                ambientColor = PB_Gold.copy(0.18f),
                spotColor    = PB_Gold.copy(0.1f)
            )
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(PB_CardBg)
            .border(2.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(GoldBorder)
        )
        Column(Modifier.padding(horizontal = sz.cardPad, vertical = sz.vPad + 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(sz.avatarSize)
                        .clip(RoundedCornerShape((sz.cardRadius.value * 0.65f).dp))
                        .background(PB_NavyDeep.copy(0.06f))
                        .border(1.dp, GoldBorder, RoundedCornerShape((sz.cardRadius.value * 0.65f).dp)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Receipt, null, tint = PB_NavyMid, modifier = Modifier.size(sz.iconSize))
                }
                Spacer(Modifier.width(sz.hPad - 4.dp))
                Column {
                    Text(
                        "Payment Summary",
                        fontWeight    = FontWeight.ExtraBold,
                        color         = PB_TextDark,
                        fontSize      = (sz.titleSp + 1f).sp,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        "Transparent pricing breakdown",
                        color    = PB_TextMuted,
                        fontSize = sz.captionSp.sp
                    )
                }
            }
            Spacer(Modifier.height(sz.vPad))
            Divider(color = PB_Divider)
            Spacer(Modifier.height(sz.vPad - 2.dp))

            PBSummaryRow("Package",    pkg.packageName,               sz)
            PBSummaryRow("Rate/Night", pkg.formattedDiscountedPrice,   sz, strikethrough = pkg.formattedOriginalPrice)
            PBSummaryRow("Duration",   "$nights nights",               sz)
            PBSummaryRow("Guests",     "$guestCount guests",           sz)

            Spacer(Modifier.height(sz.vPad - 2.dp))
            Divider(color = PB_Divider)
            Spacer(Modifier.height(sz.vPad - 2.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Total Amount",
                    fontWeight = FontWeight.Bold,
                    color      = PB_TextDark,
                    fontSize   = sz.titleSp.sp
                )
                Text(
                    "PKR ${"%,.0f".format(totalAmount)}",
                    fontWeight = FontWeight.Black,
                    color      = PB_TextDark,
                    fontSize   = (sz.titleSp + 1f).sp
                )
            }

            Spacer(Modifier.height(sz.vPad - 2.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape((sz.cardRadius.value * 0.75f).dp))
                    .background(Brush.linearGradient(listOf(PB_WarningLight, PB_GoldFaint)))
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(PB_Warning.copy(0.6f), PB_Gold.copy(0.5f), PB_Warning.copy(0.6f))
                        ),
                        RoundedCornerShape((sz.cardRadius.value * 0.75f).dp)
                    )
                    .padding(horizontal = sz.cardPad, vertical = sz.vPad)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint     = PB_Warning,
                                modifier = Modifier.size(sz.iconSize - 3.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Pay Now (20%)",
                                fontWeight = FontWeight.ExtraBold,
                                color      = PB_Warning,
                                fontSize   = sz.titleSp.sp
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Due on arrival: PKR ${"%,.0f".format(totalAmount - depositAmount)}",
                            color    = PB_TextMuted,
                            fontSize = sz.captionSp.sp
                        )
                    }
                    Text(
                        "PKR ${"%,.0f".format(depositAmount)}",
                        fontWeight    = FontWeight.Black,
                        color         = PB_Warning,
                        fontSize      = (sz.titleSp + 4f).sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUMMARY ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBSummaryRow(
    label        : String,
    value        : String,
    sz           : PBSizes,
    strikethrough: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (sz.vPad.value * 0.28f).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = PB_TextMuted, fontSize = sz.bodySp.sp)
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (strikethrough != null) {
                Text(
                    strikethrough,
                    color          = PB_TextLight,
                    fontSize       = sz.captionSp.sp,
                    textDecoration = TextDecoration.LineThrough
                )
            }
            Text(
                value,
                fontWeight = FontWeight.SemiBold,
                color      = PB_TextDark,
                fontSize   = sz.bodySp.sp
            )
        }
    }
}

