package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.HomeViewModel
import com.example.havenhub.viewmodel.TenantInfo
import java.text.SimpleDateFormat
import java.util.Locale

// ── Dark / Light design tokens ────────────────────────────────────────────────
private val T_NavyDeep   = Color(0xFF060E20)
private val T_NavyPrime  = Color(0xFF0D1B3E)
private val T_NavyMid    = Color(0xFF1A3A6B)
private val T_GoldPrime  = Color(0xFFD4AF37)
private val T_GoldLight  = Color(0xFFF5D060)
private val T_GoldFaint  = Color(0xFFFFF8E1)
private val T_GreenOk    = Color(0xFF4CAF50)
private val T_OrangePend = Color(0xFFFF9800)
private val T_RedErr     = Color(0xFFE53935)
private val T_PageBg     = Color(0xFFF0F4FA)
private val T_CardBg     = Color(0xFFFFFFFF)
private val T_TextDark   = Color(0xFF1A2744)
private val T_TextMuted  = Color(0xFF8899AA)

private val D_BgDeep     = Color(0xFF060D1A)
private val D_BgPrimary  = Color(0xFF0D1B3E)
private val D_BgCard     = Color(0xFF112038)
private val D_GoldPrime  = Color(0xFFD4AF37)
private val D_GoldLight  = Color(0xFFF5D060)
private val D_GoldFaint  = Color(0xFF1A1608)
private val D_TextPri    = Color(0xFFF0F4FF)
private val D_TextSec    = Color(0xFF8899BB)
private val D_Border     = Color(0xFF1E2E50)
private val D_GreenOk    = Color(0xFF3DCC7A)
private val D_OrangePend = Color(0xFFFFB347)
private val D_RedErr     = Color(0xFFFF5252)

// ── Date formatter helper ─────────────────────────────────────────────────────
private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

// ─────────────────────────────────────────────────────────────────────────────
// TenantsScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TenantsScreen(
    navController: NavController,
    viewModel    : HomeViewModel = hiltViewModel()
) {
    val isDark   = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val pageBg     = if (isDark) D_BgDeep    else T_PageBg
    val cardBg     = if (isDark) D_BgCard    else T_CardBg
    val textDark   = if (isDark) D_TextPri   else T_TextDark
    val textMuted  = if (isDark) D_TextSec   else T_TextMuted
    val goldP      = if (isDark) D_GoldPrime else T_GoldPrime
    val goldL      = if (isDark) D_GoldLight else T_GoldLight
    val goldF      = if (isDark) D_GoldFaint else T_GoldFaint
    val greenOk    = if (isDark) D_GreenOk   else T_GreenOk
    val orangePend = if (isDark) D_OrangePend else T_OrangePend
    val redErr     = if (isDark) D_RedErr    else T_RedErr

    val navyGrad = if (isDark)
        Brush.verticalGradient(listOf(D_BgDeep, D_BgPrimary))
    else
        Brush.verticalGradient(listOf(T_NavyDeep, T_NavyPrime, T_NavyMid))

    val goldBorder = Brush.horizontalGradient(listOf(goldP.copy(0.9f), goldL.copy(0.5f), goldP.copy(0.9f)))

    LaunchedEffect(Unit) { viewModel.loadTenants() }

    val filteredTenants = viewModel.filteredTenants(searchQuery)

    Column(modifier = Modifier.fillMaxSize().background(pageBg)) {

        // ── TOP BAR ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().background(navyGrad)) {

            // Gold accent line at bottom
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(goldBorder)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "My Tenants",
                            color      = Color.White,
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${uiState.tenants.size} total tenants",
                            color    = Color.White.copy(0.5f),
                            fontSize = 12.sp
                        )
                    }

                    // Refresh button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(goldP.copy(0.18f))
                            .border(1.5.dp, goldP.copy(0.5f), CircleShape)
                            .clickable { viewModel.refreshTenants() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = goldP, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Search bar
                Surface(
                    modifier       = Modifier.fillMaxWidth().height(50.dp),
                    shape          = RoundedCornerShape(14.dp),
                    color          = if (isDark) D_BgCard else T_CardBg,
                    tonalElevation = 0.dp
                ) {
                    TextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder   = {
                            Text(
                                "Search by name, email, property...",
                                color    = textMuted,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = goldP, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = textMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier        = Modifier.fillMaxSize(),
                        colors          = TextFieldDefaults.colors(
                            focusedContainerColor   = if (isDark) D_BgCard else T_CardBg,
                            unfocusedContainerColor = if (isDark) D_BgCard else T_CardBg,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor             = goldP,
                            focusedTextColor        = textDark,
                            unfocusedTextColor      = textDark
                        )
                    )
                }
            }
        }

        // ── CONTENT ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when {

                // Loading
                uiState.isTenantsLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color       = goldP,
                                strokeWidth = 3.dp,
                                modifier    = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text("Loading tenants...", color = textMuted, fontSize = 13.sp)
                        }
                    }
                }

                // Error
                uiState.tenantsError != null && uiState.tenants.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(40.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = redErr, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Failed to load tenants", fontWeight = FontWeight.Bold, color = textDark)
                            Spacer(Modifier.height(6.dp))
                            Text(uiState.tenantsError ?: "", color = textMuted, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { viewModel.refreshTenants() },
                                border  = androidx.compose.foundation.BorderStroke(1.dp, goldP),
                                shape   = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry", color = goldP, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Empty — no tenants at all
                uiState.tenants.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(goldP.copy(0.12f))
                                    .border(2.dp, Brush.linearGradient(listOf(goldP, goldL)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PeopleAlt, null, tint = goldP, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.height(18.dp))
                            Text("No Tenants Yet", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = textDark)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Jab koi tenant booking karega\ntab yahan dikhega",
                                color     = textMuted,
                                fontSize  = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Search — no results
                filteredTenants.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(40.dp)
                        ) {
                            Icon(Icons.Default.SearchOff, null, tint = textMuted, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No results for \"$searchQuery\"", color = textDark, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Naam, email ya property se search karo", color = textMuted, fontSize = 13.sp)
                        }
                    }
                }

                // Tenant list
                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(top = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "${filteredTenants.size} tenant${if (filteredTenants.size != 1) "s" else ""} found",
                                modifier   = Modifier.padding(horizontal = 20.dp),
                                fontSize   = 12.sp,
                                color      = textMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        items(filteredTenants, key = { it.user.userId + it.booking.bookingId }) { tenantInfo ->
                            TenantCard(
                                tenantInfo     = tenantInfo,
                                isDark         = isDark,
                                cardBg         = cardBg,
                                textDark       = textDark,
                                textMuted      = textMuted,
                                goldP          = goldP,
                                goldL          = goldL,
                                goldF          = goldF,
                                goldBorder     = goldBorder,
                                greenOk        = greenOk,
                                orangePend     = orangePend,
                                redErr         = redErr,
                                onMessageClick = {
                                    // ── FIX: Screen.Chat.createRoute matches Screen.kt signature ──
                                    navController.navigate(
                                        Screen.Chat.createRoute(
                                            userId    = tenantInfo.user.userId,
                                            ownerName = tenantInfo.user.fullName.ifBlank { "Tenant" },
                                            propertyId = tenantInfo.booking.propertyId
                                        )
                                    )
                                },
                                onBookingClick = {
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(tenantInfo.booking.bookingId)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TenantCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TenantCard(
    tenantInfo    : TenantInfo,
    isDark        : Boolean,
    cardBg        : Color,
    textDark      : Color,
    textMuted     : Color,
    goldP         : Color,
    goldL         : Color,
    goldF         : Color,
    goldBorder    : Brush,
    greenOk       : Color,
    orangePend    : Color,
    redErr        : Color,
    onMessageClick: () -> Unit,
    onBookingClick: () -> Unit
) {
    val user    = tenantInfo.user
    val booking = tenantInfo.booking

    val statusColor = when (booking.status) {
        BookingStatus.CONFIRMED.name -> greenOk
        BookingStatus.PENDING.name   -> orangePend
        BookingStatus.CANCELLED.name -> redErr
        BookingStatus.COMPLETED.name -> Color(if (isDark) 0xFF5BC8FF else 0xFF1565C0)
        else                         -> textMuted
    }

    val statusLabel = when (booking.status) {
        BookingStatus.CONFIRMED.name -> "Confirmed"
        BookingStatus.PENDING.name   -> "Pending"
        BookingStatus.CANCELLED.name -> "Cancelled"
        BookingStatus.COMPLETED.name -> "Completed"
        else -> booking.status.lowercase().replaceFirstChar { it.uppercase() }
    }

    // ── FIX: user.name → user.fullName ───────────────────────────────────────
    val initials = user.fullName
        .trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    // ── FIX: Timestamp → formatted String ────────────────────────────────────
    val checkInStr  = booking.checkInDate?.toDate()
        ?.let { dateFormatter.format(it) } ?: "—"
    val checkOutStr = booking.checkOutDate?.toDate()
        ?.let { dateFormatter.format(it) } ?: "—"

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(if (isDark) 0.dp else 6.dp, RoundedCornerShape(20.dp))
            .border(1.5.dp, goldBorder, RoundedCornerShape(20.dp)),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // Gold accent line at top
        Box(Modifier.fillMaxWidth().height(2.dp).background(goldBorder))

        Column(modifier = Modifier.padding(16.dp)) {

            // ── Avatar + Name + Status ────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(goldP.copy(0.28f), goldP.copy(0.08f))))
                        .border(2.dp, Brush.linearGradient(listOf(goldP, goldL)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = goldP, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // ── FIX: user.fullName ────────────────────────────────────
                    Text(
                        text       = user.fullName.ifBlank { "Unknown Tenant" },
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = textDark,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = textMuted, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text     = user.email.ifBlank { "No email" },
                            fontSize = 12.sp,
                            color    = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(0.15f))
                        .border(1.dp, statusColor.copy(0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = goldP.copy(0.15f), thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            // ── Phone ─────────────────────────────────────────────────────────
            if (user.phoneNumber.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, null, tint = goldP, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(user.phoneNumber, fontSize = 13.sp, color = textDark, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Property ──────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = goldP, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text       = tenantInfo.propertyTitle.ifBlank { "Property" },
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = textDark,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (tenantInfo.propertyCity.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = textMuted, modifier = Modifier.size(11.dp))
                            Text(" ${tenantInfo.propertyCity}", fontSize = 11.sp, color = textMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Booking dates (FIX: formatted strings) ────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookingDateChip(
                    label      = "Check-in",
                    date       = checkInStr,
                    goldF      = goldF,
                    goldBorder = goldBorder,
                    goldP      = goldP,
                    textDark   = textDark,
                    textMuted  = textMuted,
                    modifier   = Modifier.weight(1f)
                )
                BookingDateChip(
                    label      = "Check-out",
                    date       = checkOutStr,
                    goldF      = goldF,
                    goldBorder = goldBorder,
                    goldP      = goldP,
                    textDark   = textDark,
                    textMuted  = textMuted,
                    modifier   = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Amount ────────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = goldP, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "PKR ${"%,.0f".format(booking.totalAmount)}",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = textDark
                )
                Spacer(Modifier.width(6.dp))
                Text("total amount", fontSize = 11.sp, color = textMuted)
            }

            Spacer(Modifier.height(16.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                // Message — FIX: use AutoMirrored icon to remove deprecation warning
                OutlinedButton(
                    onClick  = onMessageClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.5.dp, goldP)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message, null,
                        tint     = goldP,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Message", color = goldP, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Booking details
                Button(
                    onClick  = onBookingClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = goldP)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth, null,
                        tint     = T_NavyDeep,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Booking", color = T_NavyDeep, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BookingDateChip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BookingDateChip(
    label      : String,
    date       : String,
    goldF      : Color,
    goldBorder : Brush,
    goldP      : Color,
    textDark   : Color,
    textMuted  : Color,
    modifier   : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(goldF)
            .border(1.dp, goldBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = textMuted, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(date, fontSize = 12.sp, color = textDark, fontWeight = FontWeight.Bold)
        }
    }
}