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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.VacationViewModel

// Semantic colors — intentional
private val VSuccess = Color(0xFF16A34A)
private val VWarning = Color(0xFFD97706)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreBookingScreen(
    navController: NavController,
    propertyId   : String = "",
    viewModel    : VacationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onBackground     = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val error            = MaterialTheme.colorScheme.error

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(primary, primaryContainer)
                        )
                    )
            ) {
                Row(
                    modifier          = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp).clip(CircleShape)
                            .background(onPrimary.copy(0.10f))
                            .border(1.dp, tertiary.copy(0.45f), CircleShape)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = tertiary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PRE-BOOKING", color = tertiary, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text(
                            if (uiState.selectedPropertyTitle.isNotEmpty()) uiState.selectedPropertyTitle
                            else if (propertyId.isNotEmpty()) propertyId else "Select a Package",
                            color    = onPrimary.copy(0.55f),
                            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        containerColor = background,
        bottomBar = {
            if (selectedPkg != null) {
                Surface(shadowElevation = 12.dp, color = surface) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Deposit (20%)", color = onSurfaceVariant, fontSize = 12.sp)
                                Text("PKR ${"%,.0f".format(depositAmount)}", color = onSurface, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                Text("Total: PKR ${"%,.0f".format(totalAmount)}", color = onSurfaceVariant, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    navController.navigate(Screen.Booking.createRoute(propertyId.ifEmpty { selectedPkg.propertyId }))
                                },
                                modifier  = Modifier.height(52.dp).widthIn(min = 160.dp),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = ButtonDefaults.buttonColors(containerColor = primary, contentColor = tertiary),
                                elevation = ButtonDefaults.buttonElevation(6.dp)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(color = tertiary, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Pay Deposit", fontWeight = FontWeight.Black, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Advance Booking Notice ────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.06f)),
                    border   = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Advance Booking — Pay 20% Deposit", fontWeight = FontWeight.Bold, color = onSurface, fontSize = 13.sp)
                            Spacer(Modifier.height(2.dp))
                            Text("Select a package below and pay 20% now to lock your stay.", fontSize = 12.sp, color = onSurfaceVariant, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // ── Section Header ────────────────────────────────────
            item {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(4.dp, 20.dp).background(tertiary, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text("Available Packages", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = onBackground)
                    Spacer(Modifier.weight(1f))
                    if (packages.isNotEmpty()) {
                        Surface(color = tertiary.copy(0.12f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, tertiary.copy(0.4f))) {
                            Text("${packages.size} Deals", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = tertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Loading ───────────────────────────────────────────
            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(180.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = tertiary)
                            Spacer(Modifier.height(8.dp))
                            Text("Packages load ho rahe hain...", color = onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Empty State ───────────────────────────────────────
            else if (packages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inventory2, null, tint = onSurfaceVariant, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No packages available", color = onSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (propertyId.isEmpty()) "propertyId missing!" else "Property: $propertyId",
                                color      = if (propertyId.isEmpty()) error else onSurfaceVariant.copy(0.6f),
                                fontSize   = 12.sp,
                                fontWeight = if (propertyId.isEmpty()) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Package Cards ─────────────────────────────────────
            else {
                items(packages, key = { it.packageId }) { pkg ->
                    PBPackageCard(
                        pkg        = pkg,
                        isSelected = selectedPkg?.packageId == pkg.packageId,
                        primary    = primary,
                        tertiary   = tertiary,
                        surface    = surface,
                        onSurface  = onSurface,
                        onSurfaceVariant = onSurfaceVariant,
                        onClick    = { viewModel.selectPackage(pkg) }
                    )
                }
            }

            // ── Guest Count ───────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Number of Guests", fontWeight = FontWeight.Bold, color = onSurface, fontSize = 14.sp)
                            Text("Max 20 guests", color = onSurfaceVariant, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (uiState.guestCount > 1) primary else onSurfaceVariant.copy(0.2f))
                                    .clickable(enabled = uiState.guestCount > 1) { viewModel.setGuestCount(uiState.guestCount - 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", color = if (uiState.guestCount > 1) tertiary else onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${uiState.guestCount}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = onSurface)
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (uiState.guestCount < 20) primary else onSurfaceVariant.copy(0.2f))
                                    .clickable(enabled = uiState.guestCount < 20) { viewModel.setGuestCount(uiState.guestCount + 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = if (uiState.guestCount < 20) tertiary else onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Payment Summary ───────────────────────────────────
            item {
                AnimatedVisibility(visible = selectedPkg != null, enter = fadeIn(), exit = fadeOut()) {
                    if (selectedPkg != null) {
                        Card(
                            modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Receipt, null, tint = tertiary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Payment Summary", fontWeight = FontWeight.ExtraBold, color = onSurface, fontSize = 15.sp)
                                }
                                Spacer(Modifier.height(16.dp))
                                Divider(color = onSurface.copy(0.08f))
                                Spacer(Modifier.height(12.dp))

                                val nights = when {
                                    uiState.checkInDay != -1 && uiState.checkOutDay != -1 ->
                                        (uiState.checkOutDay - uiState.checkInDay).coerceAtLeast(1)
                                    else -> selectedPkg.minNights.coerceAtLeast(1)
                                }

                                PBSummaryRow("Package",    selectedPkg.packageName, onSurface, onSurfaceVariant)
                                PBSummaryRow("Rate/Night", selectedPkg.formattedDiscountedPrice, onSurface, onSurfaceVariant, strikethrough = selectedPkg.formattedOriginalPrice)
                                PBSummaryRow("Nights",     "$nights nights", onSurface, onSurfaceVariant)
                                PBSummaryRow("Guests",     "${uiState.guestCount} guests", onSurface, onSurfaceVariant)

                                Spacer(Modifier.height(12.dp))
                                Divider(color = onSurface.copy(0.08f))
                                Spacer(Modifier.height(12.dp))

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Amount", fontWeight = FontWeight.Bold, color = onSurface)
                                    Text("PKR ${"%,.0f".format(totalAmount)}", fontWeight = FontWeight.Bold, color = onSurface)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Deposit Now (20%)", fontWeight = FontWeight.Bold, color = VWarning)
                                    Text("PKR ${"%,.0f".format(depositAmount)}", fontWeight = FontWeight.Black, color = VWarning, fontSize = 16.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Due on Arrival", color = onSurfaceVariant, fontSize = 13.sp)
                                    Text("PKR ${"%,.0f".format(totalAmount - depositAmount)}", color = onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ── Error ─────────────────────────────────────────────
            if (uiState.errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        colors   = CardDefaults.cardColors(containerColor = error.copy(0.08f)),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, tint = error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(uiState.errorMessage ?: "", color = error, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PBPackageCard(
    pkg             : RentalPackage,
    isSelected      : Boolean,
    primary         : Color,
    tertiary        : Color,
    surface         : Color,
    onSurface       : Color,
    onSurfaceVariant: Color,
    onClick         : () -> Unit
) {
    val borderColor = if (isSelected) tertiary else MaterialTheme.colorScheme.outline.copy(0.3f)
    val bgColor     = if (isSelected) primary.copy(0.03f) else surface

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border    = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    if (pkg.badgeLabel.isNotEmpty()) {
                        Surface(color = tertiary.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(pkg.badgeLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = tertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(pkg.packageName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = onSurface)
                    Text(pkg.propertyTitle, color = onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (isSelected) {
                    Box(modifier = Modifier.size(28.dp).background(tertiary, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pkg.formattedDiscountedPrice, fontWeight = FontWeight.Black, fontSize = 22.sp, color = onSurface)
                Text("/night", color = onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.width(10.dp))
                Text(pkg.formattedOriginalPrice, color = onSurfaceVariant, fontSize = 13.sp, textDecoration = TextDecoration.LineThrough)
                if (pkg.savingsLabel.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = VSuccess.copy(0.12f), shape = RoundedCornerShape(6.dp)) {
                        Text(pkg.savingsLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = VSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PBInfoChip(Icons.Default.NightsStay, "Min ${pkg.minNights} nights", onSurfaceVariant)
                if (pkg.maxNights != null) PBInfoChip(Icons.Default.EventAvailable, "Max ${pkg.maxNights} nights", onSurfaceVariant)
                if (pkg.remainingSlots != null) {
                    PBInfoChip(Icons.Default.ConfirmationNumber, "${pkg.remainingSlots} slots left", if ((pkg.remainingSlots ?: 0) <= 2) MaterialTheme.colorScheme.error else VSuccess)
                }
            }

            if (pkg.inclusions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = onSurface.copy(0.08f))
                Spacer(Modifier.height(10.dp))
                Text("Includes", color = onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pkg.inclusions.take(4).chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f)), modifier = Modifier.wrapContentWidth()) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = VSuccess, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(item, fontSize = 11.sp, color = onSurface, maxLines = 1)
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

@Composable
private fun PBInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}

@Composable
private fun PBSummaryRow(label: String, value: String, onSurface: Color, onSurfaceVariant: Color, strikethrough: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = onSurfaceVariant, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (strikethrough != null) Text(strikethrough, color = onSurfaceVariant, fontSize = 11.sp, textDecoration = TextDecoration.LineThrough)
            Text(value, fontWeight = FontWeight.SemiBold, color = onSurface, fontSize = 13.sp)
        }
    }
}