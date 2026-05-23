package com.example.havenhub.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.VacationViewModel

// ─── Color Palette ────────────────────────────────────────────────────────────
private val PB_NavyDeep    = Color(0xFF060E20)
private val PB_NavyPrime   = Color(0xFF0D1B3E)
private val PB_NavyMid     = Color(0xFF1A3A6B)
private val PB_NavyLight   = Color(0xFF2A4E8A)
private val PB_Gold        = Color(0xFFD4AF37)
private val PB_GoldLight   = Color(0xFFF5D060)
private val PB_GoldDim     = Color(0xFFB8962E)
private val PB_GoldFaint   = Color(0xFFFFF8E1)
private val PB_Success     = Color(0xFF16A34A)
private val PB_SuccessLight= Color(0xFFDCFCE7)
private val PB_Warning     = Color(0xFFD97706)
private val PB_WarningLight= Color(0xFFFEF3C7)
private val PB_CardBg      = Color(0xFFFFFFFF)
private val PB_PageBg      = Color(0xFFF2F5FB)
private val PB_TextDark    = Color(0xFF1A2744)
private val PB_TextMuted   = Color(0xFF8899AA)
private val PB_TextLight   = Color(0xFFBBCCDD)
private val PB_Divider     = Color(0xFFE8EEF5)

private val GoldGradient = Brush.horizontalGradient(
    listOf(PB_Gold.copy(0.9f), PB_GoldLight.copy(0.6f), PB_Gold.copy(0.9f))
)
private val GoldBorder = Brush.horizontalGradient(
    listOf(PB_Gold.copy(0.85f), PB_GoldLight.copy(0.5f), PB_Gold.copy(0.85f))
)
private val NavyGradient = Brush.linearGradient(listOf(PB_NavyDeep, PB_NavyMid))
private val NavySoftGradient = Brush.linearGradient(listOf(PB_NavyPrime, PB_NavyLight))

// ─── Responsive Sizes ─────────────────────────────────────────────────────────
private data class PBSizes(
    val hPad       : Dp,
    val vPad       : Dp,
    val cardRadius  : Dp,
    val titleSp    : Float,
    val bodySp     : Float,
    val captionSp  : Float,
    val iconSize   : Dp,
    val avatarSize : Dp,
    val chipHeight : Dp,
    val btnHeight  : Dp
)

@Composable
private fun rememberPBSizes(): PBSizes {
    val w = LocalConfiguration.current.screenWidthDp
    return remember(w) {
        when {
            w < 340  -> PBSizes(12.dp, 10.dp, 12.dp, 12f, 10.5f, 9.5f, 14.dp, 38.dp, 28.dp, 44.dp)
            w < 360  -> PBSizes(14.dp, 12.dp, 14.dp, 13f, 11f,   10f,  15.dp, 40.dp, 30.dp, 46.dp)
            w < 400  -> PBSizes(16.dp, 14.dp, 16.dp, 14f, 11.5f, 10.5f,16.dp, 42.dp, 32.dp, 48.dp)
            w < 480  -> PBSizes(20.dp, 16.dp, 18.dp, 15f, 12f,   11f,  17.dp, 44.dp, 34.dp, 50.dp)
            w < 600  -> PBSizes(22.dp, 18.dp, 20.dp, 16f, 13f,   11.5f,18.dp, 46.dp, 36.dp, 52.dp)
            else     -> PBSizes(28.dp, 20.dp, 22.dp, 17f, 14f,   12f,  20.dp, 50.dp, 38.dp, 56.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRE-BOOKING SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreBookingScreen(
    navController: NavController,
    propertyId   : String            = "",
    viewModel    : VacationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sz      = rememberPBSizes()

    LaunchedEffect(propertyId) {
        Log.d("PRE_DEBUG", "LaunchedEffect fired — propertyId = '$propertyId'")
        if (propertyId.isNotEmpty()) {
            viewModel.loadPackagesForProperty(propertyId)
        } else {
            Log.e("PRE_DEBUG", "propertyId is EMPTY — packages nahi load honge!")
        }
    }

    val packages      = uiState.propertyPackages
    val selectedPkg   = uiState.selectedPackage
    val totalAmount   = viewModel.calculateTotalAmount()
    val depositAmount = viewModel.calculateDepositAmount()

    Scaffold(
        // ── TOP BAR ──────────────────────────────────────────────────────────
        topBar = {
            PBTopBar(
                propertyId    = propertyId,
                propertyTitle = uiState.selectedPropertyTitle,
                sz            = sz,
                onBack        = { navController.popBackStack() }
            )
        },

        containerColor = PB_PageBg,

        // ── BOTTOM BAR ───────────────────────────────────────────────────────
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
                        isLoading     = uiState.isLoading,
                        sz            = sz,
                        onPay = {
                            navController.navigate(
                                Screen.Booking.createRoute(
                                    propertyId.ifEmpty { selectedPkg.propertyId }
                                )
                            )
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

            // ── Advance Booking Banner ────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                PBAdvanceBanner(sz)
            }

            // ── Section Header ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                PBSectionHeader(
                    title      = "Available Packages",
                    badgeCount = if (packages.isNotEmpty()) packages.size else null,
                    sz         = sz
                )
                Spacer(Modifier.height(10.dp))
            }

            // ── Loading State ─────────────────────────────────────────────────
            if (uiState.isLoading) {
                item { PBLoadingState(sz) }
            }

            // ── Empty State ───────────────────────────────────────────────────
            else if (packages.isEmpty()) {
                item { PBEmptyState(propertyId = propertyId, sz = sz) }
            }

            // ── Package Cards ─────────────────────────────────────────────────
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

            // ── Guest Count ───────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                PBGuestCounter(
                    guestCount = uiState.guestCount,
                    sz         = sz,
                    onMinus    = { viewModel.setGuestCount(uiState.guestCount - 1) },
                    onPlus     = { viewModel.setGuestCount(uiState.guestCount + 1) }
                )
            }

            // ── Payment Summary ───────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = selectedPkg != null,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    if (selectedPkg != null) {
                        val nights = when {
                            uiState.checkInDay != -1 && uiState.checkOutDay != -1 ->
                                (uiState.checkOutDay - uiState.checkInDay).coerceAtLeast(1)
                            else -> selectedPkg.minNights.coerceAtLeast(1)
                        }
                        Spacer(Modifier.height(12.dp))
                        PBPaymentSummary(
                            pkg           = selectedPkg,
                            nights        = nights,
                            guestCount    = uiState.guestCount,
                            totalAmount   = totalAmount,
                            depositAmount = depositAmount,
                            sz            = sz
                        )
                    }
                }
            }

            // ── Error Message ─────────────────────────────────────────────────
            if (uiState.errorMessage != null) {
                item {
                    Spacer(Modifier.height(10.dp))
                    PBErrorCard(message = uiState.errorMessage ?: "", sz = sz)
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
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
        // Subtle decorative circles
        Box(
            Modifier
                .size(130.dp)
                .align(Alignment.TopEnd)
                .offset(x = 45.dp, y = (-45).dp)
                .clip(CircleShape)
                .background(PB_Gold.copy(0.06f))
        )
        Box(
            Modifier
                .size(70.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 20.dp)
                .clip(CircleShape)
                .background(PB_Gold.copy(0.04f))
        )

        // Gold accent line at bottom
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
                .padding(horizontal = sz.hPad, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
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

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "PRE-BOOKING",
                    color         = PB_Gold,
                    fontSize      = (sz.titleSp + 1).sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = when {
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

            // Decorative gold badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PB_Gold.copy(0.12f))
                    .border(1.dp, GoldBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "20% OFF",
                    color      = PB_Gold,
                    fontSize   = sz.captionSp.sp,
                    fontWeight = FontWeight.ExtraBold,
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
        // Gold top line
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(GoldBorder)
        )

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = sz.hPad, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PB_Success)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Deposit Required (20%)",
                        color    = PB_TextMuted,
                        fontSize = sz.captionSp.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "PKR ${"%,.0f".format(depositAmount)}",
                    color      = PB_NavyDeep,
                    fontWeight = FontWeight.Black,
                    fontSize   = (sz.titleSp + 5).sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Total: PKR ${"%,.0f".format(totalAmount)}",
                    color    = PB_TextMuted,
                    fontSize = sz.captionSp.sp
                )
            }

            // Pay Deposit CTA
            Box(
                modifier = Modifier
                    .height(sz.btnHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyGradient)
                    .border(1.5.dp, GoldBorder, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        enabled           = !isLoading
                    ) { onPay() }
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color     = PB_Gold,
                        modifier  = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint     = PB_Gold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Pay Deposit",
                            color      = PB_Gold,
                            fontWeight = FontWeight.Black,
                            fontSize   = sz.titleSp.sp,
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
                Brush.linearGradient(
                    listOf(PB_NavyDeep.copy(0.96f), PB_NavyMid.copy(0.96f))
                )
            )
            .border(1.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
    ) {
        // Subtle pattern overlay
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(GoldBorder)
        )

        Row(
            modifier          = Modifier.padding(horizontal = sz.hPad, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(sz.avatarSize + 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PB_Gold.copy(0.15f))
                    .border(1.dp, GoldBorder, RoundedCornerShape(12.dp)),
                Alignment.Center
            ) {
                Text("📅", fontSize = (sz.titleSp + 6).sp)
            }
            Spacer(Modifier.width(14.dp))
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
                    lineHeight = (sz.captionSp + 5).sp
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
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GoldGradient)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            fontWeight = FontWeight.ExtraBold,
            fontSize   = (sz.titleSp + 2).sp,
            color      = PB_TextDark,
            letterSpacing = 0.2.sp
        )
        Spacer(Modifier.weight(1f))
        if (badgeCount != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PB_NavyDeep.copy(0.08f))
                    .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
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
            .height(220.dp),
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color       = PB_Gold,
                strokeWidth = 3.dp,
                modifier    = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Packages load ho rahe hain...",
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
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PB_Gold.copy(0.1f))
                    .border(1.5.dp, GoldBorder, CircleShape),
                Alignment.Center
            ) {
                Icon(Icons.Default.Inventory2, null, tint = PB_GoldDim, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "No packages available",
                color      = PB_TextDark,
                fontWeight = FontWeight.SemiBold,
                fontSize   = sz.titleSp.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (propertyId.isEmpty()) "propertyId missing!" else "Property: $propertyId",
                color      = if (propertyId.isEmpty()) MaterialTheme.colorScheme.error else PB_TextMuted,
                fontSize   = sz.bodySp.sp,
                fontWeight = if (propertyId.isEmpty()) FontWeight.Bold else FontWeight.Normal
            )
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
        // Subtle gold left accent bar
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(GoldGradient)
        )

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = sz.hPad + 6.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(Modifier.width(12.dp))
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PBCounterButton(
                    symbol  = "−",
                    enabled = guestCount > 1,
                    onClick = onMinus,
                    sz      = sz
                )
                Text(
                    "$guestCount",
                    fontSize   = (sz.titleSp + 6).sp,
                    fontWeight = FontWeight.Black,
                    color      = PB_TextDark,
                    modifier   = Modifier.widthIn(min = 32.dp),
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
                PBCounterButton(
                    symbol  = "+",
                    enabled = guestCount < 20,
                    onClick = onPlus,
                    sz      = sz
                )
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
            fontSize   = (sz.titleSp + 3).sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (sz.titleSp + 3).sp
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
        // Gold top stripe
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(GoldBorder)
        )

        Column(Modifier.padding(horizontal = sz.hPad, vertical = 20.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(sz.avatarSize)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PB_NavyDeep.copy(0.06f))
                        .border(1.dp, GoldBorder, RoundedCornerShape(10.dp)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Receipt, null, tint = PB_NavyMid, modifier = Modifier.size(sz.iconSize))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Payment Summary",
                        fontWeight    = FontWeight.ExtraBold,
                        color         = PB_TextDark,
                        fontSize      = (sz.titleSp + 1).sp,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        "Transparent pricing breakdown",
                        color    = PB_TextMuted,
                        fontSize = sz.captionSp.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = PB_Divider)
            Spacer(Modifier.height(14.dp))

            PBSummaryRow("Package",    pkg.packageName,                sz)
            PBSummaryRow("Rate/Night", pkg.formattedDiscountedPrice,    sz, strikethrough = pkg.formattedOriginalPrice)
            PBSummaryRow("Duration",   "$nights nights",                sz)
            PBSummaryRow("Guests",     "$guestCount guests",            sz)

            Spacer(Modifier.height(14.dp))
            Divider(color = PB_Divider)
            Spacer(Modifier.height(14.dp))

            // Total
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Total Amount", fontWeight = FontWeight.Bold, color = PB_TextDark, fontSize = sz.titleSp.sp)
                Text(
                    "PKR ${"%,.0f".format(totalAmount)}",
                    fontWeight = FontWeight.Black,
                    color      = PB_TextDark,
                    fontSize   = (sz.titleSp + 1).sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Deposit Highlight Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PB_WarningLight, PB_GoldFaint)
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(PB_Warning.copy(0.6f), PB_Gold.copy(0.5f), PB_Warning.copy(0.6f))
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = sz.hPad, vertical = 14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint     = PB_Warning,
                                modifier = Modifier.size(14.dp)
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
                        fontWeight = FontWeight.Black,
                        color      = PB_Warning,
                        fontSize   = (sz.titleSp + 4).sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
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
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ErrorOutline,
                null,
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
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardElevation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad, vertical = 7.dp)
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
        // Gold top stripe when selected
        if (isSelected) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(GoldBorder)
            )
        }

        Column(Modifier.padding(horizontal = sz.hPad, vertical = 18.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    if (pkg.badgeLabel.isNotEmpty()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PB_Gold.copy(0.12f))
                                .border(1.dp, GoldBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                pkg.badgeLabel,
                                color      = PB_GoldDim,
                                fontSize   = sz.captionSp.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        pkg.packageName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = (sz.titleSp + 2).sp,
                        color      = PB_TextDark,
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

                // Selected checkmark
                AnimatedVisibility(
                    visible = isSelected,
                    enter   = scaleIn() + fadeIn(),
                    exit    = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(NavyGradient)
                            .border(1.5.dp, GoldBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = PB_Gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Price row ─────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Text(
                    pkg.formattedDiscountedPrice,
                    fontWeight    = FontWeight.Black,
                    fontSize      = (sz.titleSp + 8).sp,
                    color         = PB_TextDark,
                    letterSpacing = (-1).sp
                )
                Text(
                    "/night",
                    color    = PB_TextMuted,
                    fontSize = sz.bodySp.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    pkg.formattedOriginalPrice,
                    color           = PB_TextMuted,
                    fontSize        = sz.bodySp.sp,
                    textDecoration  = TextDecoration.LineThrough,
                    modifier        = Modifier.padding(bottom = 3.dp)
                )
                if (pkg.savingsLabel.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PB_SuccessLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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

            Spacer(Modifier.height(14.dp))

            // ── Info Chips ────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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

            // ── Inclusions ────────────────────────────────────────────────────
            if (pkg.inclusions.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Divider(color = PB_Divider)
                Spacer(Modifier.height(12.dp))

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
                Spacer(Modifier.height(10.dp))

                // Inclusions grid — 2 per row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pkg.inclusions.take(4).chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PB_Gold.copy(0.05f))
                                        .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(PB_SuccessLight),
                                            Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint     = PB_Success,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            item,
                                            fontSize = sz.captionSp.sp,
                                            color    = PB_TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            // Fill empty slot if odd number
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
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = tint, fontSize = sz.captionSp.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUMMARY ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBSummaryRow(
    label       : String,
    value       : String,
    sz          : PBSizes,
    strikethrough: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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