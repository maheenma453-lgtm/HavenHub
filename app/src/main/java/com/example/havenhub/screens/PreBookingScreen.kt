package com.example.havenhub.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.VacationViewModel

// ─── Semantic palette ────────────────────────────────────────────────────────
private val PB_NavyDeep    = Color(0xFF060E20)
private val PB_NavyPrime   = Color(0xFF0D1B3E)
private val PB_NavyMid     = Color(0xFF1A3A6B)
private val PB_Gold        = Color(0xFFD4AF37)
private val PB_GoldLight   = Color(0xFFF5D060)
private val PB_GoldDim     = Color(0xFFB8962E)
private val PB_GoldFaint   = Color(0xFFFFF8E1)
private val PB_Success     = Color(0xFF16A34A)
private val PB_Warning     = Color(0xFFD97706)
private val PB_CardBg      = Color(0xFFFFFFFF)
private val PB_PageBg      = Color(0xFFF0F4FA)
private val PB_TextDark    = Color(0xFF1A2744)
private val PB_TextMuted   = Color(0xFF8899AA)

private val GoldBorder = Brush.horizontalGradient(
    listOf(PB_Gold.copy(0.9f), PB_GoldLight.copy(0.5f), PB_Gold.copy(0.9f))
)
private val NavyGradient = Brush.linearGradient(listOf(PB_NavyDeep, PB_NavyMid))

// ─── Responsive helper ───────────────────────────────────────────────────────
private data class PBSizes(val hPad: Dp, val cardRadius: Dp, val titleSp: Float, val bodySp: Float)

@Composable
private fun rememberPBSizes(): PBSizes {
    val w = LocalConfiguration.current.screenWidthDp
    return remember(w) {
        when {
            w < 360 -> PBSizes(14.dp, 14.dp, 13f, 11f)
            w < 400 -> PBSizes(16.dp, 16.dp, 14f, 11.5f)
            w < 480 -> PBSizes(20.dp, 18.dp, 15f, 12f)
            else    -> PBSizes(24.dp, 20.dp, 16f, 13f)
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
    propertyId   : String           = "",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyGradient)
            ) {
                // Gold accent line at bottom
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(GoldBorder)
                )
                // Decorative circle
                Box(
                    Modifier
                        .size(100.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 35.dp, y = (-35).dp)
                        .clip(CircleShape)
                        .background(PB_Gold.copy(0.07f))
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PB_Gold.copy(0.15f))
                            .border(1.5.dp, GoldBorder, CircleShape)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint     = PB_Gold,
                            modifier = Modifier.size(20.dp)
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
                        Text(
                            if (uiState.selectedPropertyTitle.isNotEmpty()) uiState.selectedPropertyTitle
                            else if (propertyId.isNotEmpty()) propertyId else "Select a Package",
                            color    = Color.White.copy(0.55f),
                            fontSize = sz.bodySp.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },

        containerColor = PB_PageBg,

        // ── BOTTOM BAR ───────────────────────────────────────────────────────
        bottomBar = {
            if (selectedPkg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp)
                        .background(Color.White)
                        .padding(horizontal = sz.hPad, vertical = 14.dp)
                ) {
                    // Gold top line
                    Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter).background(GoldBorder))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Deposit (20%)",
                                color    = PB_TextMuted,
                                fontSize = sz.bodySp.sp
                            )
                            Text(
                                "PKR ${"%,.0f".format(depositAmount)}",
                                color      = PB_NavyDeep,
                                fontWeight = FontWeight.Black,
                                fontSize   = 20.sp
                            )
                            Text(
                                "Total: PKR ${"%,.0f".format(totalAmount)}",
                                color    = PB_TextMuted,
                                fontSize = (sz.bodySp - 1).sp
                            )
                        }

                        // Pay Deposit button
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NavyGradient)
                                .border(1.5.dp, GoldBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    navController.navigate(
                                        Screen.Booking.createRoute(propertyId.ifEmpty { selectedPkg.propertyId })
                                    )
                                }
                                .padding(horizontal = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = PB_Gold, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    "Pay Deposit",
                                    color      = PB_Gold,
                                    fontWeight = FontWeight.Black,
                                    fontSize   = sz.titleSp.sp
                                )
                            }
                        }
                    }
                }
            }
        }

    ) { paddingValues ->

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {

            // ── Advance Booking Notice ────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sz.hPad)
                        .shadow(6.dp, RoundedCornerShape(sz.cardRadius), ambientColor = PB_Gold.copy(0.15f))
                        .clip(RoundedCornerShape(sz.cardRadius))
                        .background(
                            Brush.linearGradient(listOf(PB_NavyDeep.copy(0.04f), PB_Gold.copy(0.04f)))
                        )
                        .border(1.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape)
                                .background(PB_Gold.copy(0.12f))
                                .border(1.dp, GoldBorder, CircleShape),
                            Alignment.Center
                        ) {
                            Text("📅", fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Advance Booking — Pay 20% Deposit",
                                fontWeight = FontWeight.Bold,
                                color      = PB_TextDark,
                                fontSize   = sz.titleSp.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Select a package below and pay 20% now to lock your stay.",
                                fontSize   = sz.bodySp.sp,
                                color      = PB_TextMuted,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // ── Section Header ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sz.hPad),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PB_Gold)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Available Packages",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp,
                        color      = PB_TextDark
                    )
                    Spacer(Modifier.weight(1f))
                    if (packages.isNotEmpty()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PB_Gold.copy(0.12f))
                                .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "${packages.size} Deals",
                                color      = PB_GoldDim,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Loading ───────────────────────────────────────────────────────
            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PB_Gold, strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Packages load ho rahe hain...", color = PB_TextMuted, fontSize = sz.bodySp.sp)
                        }
                    }
                }
            }

            // ── Empty State ───────────────────────────────────────────────────
            else if (packages.isEmpty()) {
                item {
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
                            Box(Modifier.size(64.dp).clip(CircleShape).background(PB_Gold.copy(0.1f)).border(1.5.dp, GoldBorder, CircleShape), Alignment.Center) {
                                Icon(Icons.Default.Inventory2, null, tint = PB_GoldDim, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("No packages available", color = PB_TextDark, fontWeight = FontWeight.SemiBold, fontSize = sz.titleSp.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (propertyId.isEmpty()) "propertyId missing!" else "Property: $propertyId",
                                color      = if (propertyId.isEmpty()) MaterialTheme.colorScheme.error else PB_TextMuted,
                                fontSize   = sz.bodySp.sp,
                                fontWeight = if (propertyId.isEmpty()) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Package Cards ─────────────────────────────────────────────────
            else {
                items(packages, key = { it.packageId }) { pkg ->
                    PBPackageCard(
                        pkg       = pkg,
                        isSelected = selectedPkg?.packageId == pkg.packageId,
                        sz        = sz,
                        onClick   = { viewModel.selectPackage(pkg) }
                    )
                }
            }

            // ── Guest Count ───────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sz.hPad)
                        .shadow(6.dp, RoundedCornerShape(sz.cardRadius))
                        .clip(RoundedCornerShape(sz.cardRadius))
                        .background(PB_CardBg)
                        .border(1.5.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
                        .padding(horizontal = sz.hPad, vertical = 16.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Number of Guests", fontWeight = FontWeight.Bold, color = PB_TextDark, fontSize = sz.titleSp.sp)
                            Text("Max 20 guests", color = PB_TextMuted, fontSize = sz.bodySp.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.guestCount > 1) NavyGradient else Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0))))
                                    .border(1.dp, if (uiState.guestCount > 1) GoldBorder else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)), CircleShape)
                                    .clickable(enabled = uiState.guestCount > 1) { viewModel.setGuestCount(uiState.guestCount - 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", color = if (uiState.guestCount > 1) PB_Gold else PB_TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${uiState.guestCount}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = PB_TextDark)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.guestCount < 20) NavyGradient else Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0))))
                                    .border(1.dp, if (uiState.guestCount < 20) GoldBorder else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)), CircleShape)
                                    .clickable(enabled = uiState.guestCount < 20) { viewModel.setGuestCount(uiState.guestCount + 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = if (uiState.guestCount < 20) PB_Gold else PB_TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Payment Summary ───────────────────────────────────────────────
            item {
                AnimatedVisibility(visible = selectedPkg != null, enter = fadeIn(), exit = fadeOut()) {
                    if (selectedPkg != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = sz.hPad)
                                .shadow(8.dp, RoundedCornerShape(sz.cardRadius), ambientColor = PB_Gold.copy(0.15f))
                                .clip(RoundedCornerShape(sz.cardRadius))
                                .background(PB_CardBg)
                                .border(2.dp, GoldBorder, RoundedCornerShape(sz.cardRadius))
                        ) {
                            // Gold top accent
                            Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(GoldBorder))

                            Column(Modifier.padding(horizontal = sz.hPad, vertical = 20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(PB_Gold.copy(0.12f)).border(1.dp, GoldBorder, CircleShape), Alignment.Center) {
                                        Icon(Icons.Default.Receipt, null, tint = PB_Gold, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text("Payment Summary", fontWeight = FontWeight.ExtraBold, color = PB_TextDark, fontSize = (sz.titleSp + 1).sp)
                                }

                                Spacer(Modifier.height(16.dp))
                                Divider(color = PB_TextMuted.copy(0.15f))
                                Spacer(Modifier.height(14.dp))

                                val nights = when {
                                    uiState.checkInDay != -1 && uiState.checkOutDay != -1 ->
                                        (uiState.checkOutDay - uiState.checkInDay).coerceAtLeast(1)
                                    else -> selectedPkg.minNights.coerceAtLeast(1)
                                }

                                PBSummaryRow("Package",    selectedPkg.packageName,             sz, strikethrough = null)
                                PBSummaryRow("Rate/Night", selectedPkg.formattedDiscountedPrice, sz, strikethrough = selectedPkg.formattedOriginalPrice)
                                PBSummaryRow("Nights",     "$nights nights",                    sz)
                                PBSummaryRow("Guests",     "${uiState.guestCount} guests",      sz)

                                Spacer(Modifier.height(14.dp))
                                Divider(color = PB_TextMuted.copy(0.15f))
                                Spacer(Modifier.height(14.dp))

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Amount", fontWeight = FontWeight.Bold, color = PB_TextDark, fontSize = sz.titleSp.sp)
                                    Text("PKR ${"%,.0f".format(totalAmount)}", fontWeight = FontWeight.Bold, color = PB_TextDark, fontSize = sz.titleSp.sp)
                                }
                                Spacer(Modifier.height(10.dp))

                                // Deposit highlight
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(listOf(PB_Warning.copy(0.08f), PB_Gold.copy(0.05f))))
                                        .border(1.dp, Brush.horizontalGradient(listOf(PB_Warning.copy(0.5f), PB_Gold.copy(0.4f), PB_Warning.copy(0.5f))), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Deposit Now (20%)", fontWeight = FontWeight.Bold, color = PB_Warning, fontSize = sz.titleSp.sp)
                                            Text("Due on Arrival: PKR ${"%,.0f".format(totalAmount - depositAmount)}", color = PB_TextMuted, fontSize = (sz.bodySp - 0.5f).sp)
                                        }
                                        Text("PKR ${"%,.0f".format(depositAmount)}", fontWeight = FontWeight.Black, color = PB_Warning, fontSize = (sz.titleSp + 2).sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            if (uiState.errorMessage != null) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = sz.hPad)
                            .clip(RoundedCornerShape(sz.cardRadius))
                            .background(MaterialTheme.colorScheme.error.copy(0.07f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(0.3f), RoundedCornerShape(sz.cardRadius))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = sz.bodySp.sp)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }
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
    val cardBg = if (isSelected)
        Brush.linearGradient(listOf(PB_NavyDeep.copy(0.03f), PB_Gold.copy(0.04f)))
    else
        Brush.linearGradient(listOf(PB_CardBg, PB_CardBg))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sz.hPad, vertical = 6.dp)
            .shadow(if (isSelected) 10.dp else 4.dp, RoundedCornerShape(sz.cardRadius), ambientColor = PB_Gold.copy(if (isSelected) 0.25f else 0.08f))
            .clip(RoundedCornerShape(sz.cardRadius))
            .background(cardBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) GoldBorder
                else Brush.horizontalGradient(listOf(PB_TextMuted.copy(0.2f), PB_TextMuted.copy(0.2f))),
                shape = RoundedCornerShape(sz.cardRadius)
            )
            .clickable { onClick() }
    ) {
        // Selected top gold stripe
        if (isSelected) {
            Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(GoldBorder))
        }

        Column(Modifier.padding(horizontal = sz.hPad, vertical = 16.dp)) {

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
                                .clip(RoundedCornerShape(6.dp))
                                .background(PB_Gold.copy(0.14f))
                                .border(1.dp, GoldBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(pkg.badgeLabel, color = PB_GoldDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(pkg.packageName, fontWeight = FontWeight.ExtraBold, fontSize = (sz.titleSp + 2).sp, color = PB_TextDark)
                    Text(pkg.propertyTitle, color = PB_TextMuted, fontSize = sz.bodySp.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PB_NavyDeep, PB_NavyMid)))
                            .border(1.5.dp, GoldBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = PB_Gold, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Price row ─────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pkg.formattedDiscountedPrice, fontWeight = FontWeight.Black, fontSize = (sz.titleSp + 6).sp, color = PB_TextDark)
                Text("/night", color = PB_TextMuted, fontSize = sz.bodySp.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                Spacer(Modifier.width(10.dp))
                Text(pkg.formattedOriginalPrice, color = PB_TextMuted, fontSize = sz.bodySp.sp, textDecoration = TextDecoration.LineThrough)
                if (pkg.savingsLabel.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PB_Success.copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(pkg.savingsLabel, color = PB_Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Info chips ────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PBInfoChip(Icons.Default.NightsStay, "Min ${pkg.minNights} nights", PB_TextMuted)
                if (pkg.maxNights != null) PBInfoChip(Icons.Default.EventAvailable, "Max ${pkg.maxNights} nights", PB_TextMuted)
                if (pkg.remainingSlots != null) {
                    PBInfoChip(
                        Icons.Default.ConfirmationNumber,
                        "${pkg.remainingSlots} slots left",
                        if ((pkg.remainingSlots ?: 0) <= 2) MaterialTheme.colorScheme.error else PB_Success
                    )
                }
            }

            // ── Inclusions ────────────────────────────────────────────────────
            if (pkg.inclusions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Divider(color = PB_TextMuted.copy(0.12f))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(PB_Gold))
                    Spacer(Modifier.width(7.dp))
                    Text("Includes", color = PB_TextDark, fontSize = sz.bodySp.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pkg.inclusions.take(4).chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(PB_Gold.copy(0.06f))
                                        .border(1.dp, GoldBorder, RoundedCornerShape(9.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = PB_Success, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(item, fontSize = sz.bodySp.sp, color = PB_TextDark, maxLines = 1)
                                    }
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
// INFO CHIP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUMMARY ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PBSummaryRow(label: String, value: String, sz: PBSizes, strikethrough: String? = null) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = PB_TextMuted, fontSize = sz.bodySp.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (strikethrough != null) Text(strikethrough, color = PB_TextMuted, fontSize = (sz.bodySp - 1).sp, textDecoration = TextDecoration.LineThrough)
            Text(value, fontWeight = FontWeight.SemiBold, color = PB_TextDark, fontSize = sz.bodySp.sp)
        }
    }
}









