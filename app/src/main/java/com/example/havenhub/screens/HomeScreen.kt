package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.MainActivity
import com.example.havenhub.components.NotificationIconButton
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.HomeViewModel
import com.example.havenhub.viewmodel.HomeUiState
import com.example.havenhub.viewmodel.NotificationViewModel
import com.example.havenhub.viewmodel.VacationViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// DESIGN TOKENS — LIGHT THEME
// ═══════════════════════════════════════════════════════════════════════════════
private val L_NavyDeep    = Color(0xFF060E20)
private val L_NavyPrime   = Color(0xFF0D1B3E)
private val L_NavyMid     = Color(0xFF1A3A6B)
private val L_GoldPrime   = Color(0xFFD4AF37)
private val L_GoldLight   = Color(0xFFF5D060)
private val L_GoldFaint   = Color(0xFFFFF8E1)
private val L_GoldDim     = Color(0xFFB8962E)
private val L_PageBg      = Color(0xFFF0F4FA)
private val L_CardBg      = Color(0xFFFFFFFF)
private val L_TextMuted   = Color(0xFF8899AA)
private val L_TextDark    = Color(0xFF1A2744)
private val L_GreenOk     = Color(0xFF4CAF50)
private val L_OrangePend  = Color(0xFFFF9800)

// ═══════════════════════════════════════════════════════════════════════════════
// DESIGN TOKENS — DARK THEME
// ═══════════════════════════════════════════════════════════════════════════════
private val D_BgDeep        = Color(0xFF060D1A)
private val D_BgPrimary     = Color(0xFF0D1B3E)
private val D_BgSecondary   = Color(0xFF122040)
private val D_BgCard        = Color(0xFF112038)
private val D_BgCardRaised  = Color(0xFF162648)
private val D_GoldPrimary   = Color(0xFFD4AF37)
private val D_GoldLight     = Color(0xFFF5D060)
private val D_GoldDim       = Color(0xFFB8962E)
private val D_GoldFaint     = Color(0xFF1A1608)
private val D_TextPrimary   = Color(0xFFF0F4FF)
private val D_TextSecondary = Color(0xFF8899BB)
private val D_Border        = Color(0xFF1E2E50)
private val D_GreenOk       = Color(0xFF3DCC7A)
private val D_OrangePend    = Color(0xFFFFB347)

// ═══════════════════════════════════════════════════════════════════════════════
// THEME-AWARE TOKEN RESOLVER
// ═══════════════════════════════════════════════════════════════════════════════
private data class ThemeTokens(
    val pageBg      : Color,
    val cardBg      : Color,
    val textDark    : Color,
    val textMuted   : Color,
    val goldPrime   : Color,
    val goldLight   : Color,
    val goldFaint   : Color,
    val goldDim     : Color,
    val greenOk     : Color,
    val orangePend  : Color,
    val navyLinear  : Brush,
    val navyGradient: Brush,
    val goldLinear  : Brush,
    val goldGradient: Brush,
    val goldBorder  : Brush
)

private fun resolveTokens(isDark: Boolean): ThemeTokens {
    val goldP = if (isDark) D_GoldPrimary else L_GoldPrime
    val goldL = if (isDark) D_GoldLight   else L_GoldLight
    return ThemeTokens(
        pageBg       = if (isDark) D_BgDeep       else L_PageBg,
        cardBg       = if (isDark) D_BgCard        else L_CardBg,
        textDark     = if (isDark) D_TextPrimary   else L_TextDark,
        textMuted    = if (isDark) D_TextSecondary else L_TextMuted,
        goldPrime    = goldP,
        goldLight    = goldL,
        goldFaint    = if (isDark) D_GoldFaint     else L_GoldFaint,
        goldDim      = if (isDark) D_GoldDim       else L_GoldDim,
        greenOk      = if (isDark) D_GreenOk       else L_GreenOk,
        orangePend   = if (isDark) D_OrangePend    else L_OrangePend,
        navyLinear   = if (isDark)
            Brush.linearGradient(listOf(D_BgDeep, D_BgSecondary))
        else
            Brush.linearGradient(listOf(L_NavyDeep, L_NavyMid)),
        navyGradient = if (isDark)
            Brush.verticalGradient(listOf(D_BgDeep, D_BgPrimary, D_BgSecondary))
        else
            Brush.verticalGradient(listOf(L_NavyDeep, L_NavyPrime, L_NavyMid)),
        goldLinear   = Brush.linearGradient(listOf(goldP, goldL)),
        goldGradient = Brush.linearGradient(listOf(goldP, goldL, goldP)),
        goldBorder   = Brush.horizontalGradient(
            listOf(goldP.copy(0.9f), goldL.copy(0.5f), goldP.copy(0.9f))
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// CARD CLICK SHINE HELPER
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun GoldShineOverlay(visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(250),
        label         = "shine"
    )
    if (alpha > 0f) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(
                        L_GoldPrime.copy(0f),
                        L_GoldPrime.copy(0.10f * alpha),
                        L_GoldPrime.copy(0f)
                    )
                )
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ✦ PROFILE AVATAR COMPOSABLE — reusable for Tenant + Landlord headers
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ProfileAvatarButton(
    photoUrl  : String,
    initials  : String,
    goldBorder: Brush,
    goldPrime : Color,
    onClick   : () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(goldPrime.copy(alpha = 0.18f))
            .border(2.dp, goldBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotEmpty()) {
            AsyncImage(
                model              = photoUrl,
                contentDescription = "Profile",
                modifier           = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale       = ContentScale.Crop
            )
        } else {
            Text(
                text       = initials.take(2),
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = goldPrime
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HOME SCREEN — Entry Point
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(
    navController        : NavController,
    viewModel            : HomeViewModel         = hiltViewModel(),
    authViewModel        : AuthViewModel         = hiltViewModel(),
    vacationViewModel    : VacationViewModel     = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val authState  by authViewModel.uiState.collectAsState()
    val vacUiState by vacationViewModel.uiState.collectAsState()
    val notifState by notificationViewModel.uiState.collectAsState()
    val isDark     by MainActivity.darkModeFlow.collectAsState()

    if (!authState.isAuthReady) {
        val tk = resolveTokens(isDark)
        Box(
            modifier         = Modifier.fillMaxSize().background(tk.pageBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = tk.goldPrime, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
        }
        return
    }

    val userRole = authState.userRole.lowercase().trim()
    val userId   = authState.currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) notificationViewModel.startListening(userId)
    }
    LaunchedEffect(userId, userRole) {
        when {
            userRole == "landlord" && userId.isNotEmpty() -> viewModel.loadLandlordStats(userId)
            else -> { viewModel.loadHomeData(); vacationViewModel.loadVacationProperties() }
        }
    }
    LaunchedEffect(Unit) {
        when (userRole) {
            "landlord" -> { if (userId.isNotEmpty()) viewModel.refreshLandlordStats() }
            else       -> { viewModel.refreshHomeData(); vacationViewModel.loadVacationProperties() }
        }
    }

    when (userRole) {
        "landlord" -> LandlordHomeScreen(
            navController = navController,
            uiState       = uiState,
            unreadCount   = notifState.unreadCount,
            isDark        = isDark
        )
        else -> TenantHomeScreen(
            navController     = navController,
            viewModel         = viewModel,
            uiState           = uiState,
            vacationCount     = vacUiState.properties.size,
            isVacationLoading = vacUiState.isLoading,
            unreadCount       = notifState.unreadCount,
            isDark            = isDark
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// IMAGE HELPER
// ═══════════════════════════════════════════════════════════════════════════════
private fun resolveDrawable(property: Property): Int {
    if (property.drawableImageName.isNotEmpty())    return getPropertyImage(property.drawableImageName)
    if (property.resolvedDrawableName.isNotEmpty()) return getPropertyImage(property.resolvedDrawableName)
    return getPropertyImage(property.propertyId)
}

@Composable
private fun PropertyImage(
    property    : Property,
    modifier    : Modifier     = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val remoteUrl = property.imageUrls.firstOrNull { it.isNotBlank() }
    if (!remoteUrl.isNullOrEmpty()) {
        AsyncImage(model = remoteUrl, contentDescription = property.title, modifier = modifier, contentScale = contentScale)
    } else {
        Image(painter = painterResource(id = resolveDrawable(property)), contentDescription = property.title, modifier = modifier, contentScale = contentScale)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LANDLORD HOME SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LandlordHomeScreen(
    navController: NavController,
    uiState      : HomeUiState,
    unreadCount  : Int     = 0,
    isDark       : Boolean = false
) {
    val tk = resolveTokens(isDark)

    val formattedRevenue = remember(uiState.totalRevenue) {
        when {
            uiState.totalRevenue >= 1_000_000 -> "PKR %.1fM".format(uiState.totalRevenue / 1_000_000)
            uiState.totalRevenue >= 1_000     -> "PKR %.0fK".format(uiState.totalRevenue / 1_000)
            else                              -> "PKR %.0f".format(uiState.totalRevenue)
        }
    }

    val pulseAnim  = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(tk.pageBg),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // ── 1. HEADER ─────────────────────────────────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth().background(tk.navyGradient)) {
                Box(Modifier.size(200.dp).align(Alignment.TopEnd).offset(x = 70.dp, y = (-70).dp).clip(CircleShape).background(tk.goldPrime.copy(0.06f)))
                Box(Modifier.size(100.dp).align(Alignment.BottomStart).offset(x = (-30).dp, y = 40.dp).clip(CircleShape).background(tk.goldPrime.copy(0.04f)))
                Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(tk.goldBorder))

                Column(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 26.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Welcome Back 👋", color = tk.goldPrime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("Manage Your Haven", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Track listings & booking requests", color = Color.White.copy(0.5f), fontSize = 12.sp)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            NotificationIconButton(
                                count   = unreadCount,
                                onClick = { navController.navigate(Screen.Notifications.route) },
                                tint    = tk.goldPrime
                            )
                            ProfileAvatarButton(
                                photoUrl   = uiState.currentUserPhotoUrl,
                                initials   = uiState.currentUserInitials,
                                goldBorder = tk.goldBorder,
                                goldPrime  = tk.goldPrime,
                                onClick    = { navController.navigate(Screen.Profile.route) }
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // ── Stat cards row ────────────────────────────────────────
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Active Tenants
                        LandlordStatCard(
                            icon       = Icons.Default.People,
                            iconTint   = Color(0xFF5BC8FF),
                            bgColor    = Color(0xFF5BC8FF).copy(0.15f),
                            label      = "Active Tenants",
                            value      = "${uiState.activeTenantsCount}",
                            valueColor = Color.White,
                            modifier   = Modifier.weight(1f),
                            isDark     = isDark,
                            onClick    = { navController.navigate(Screen.MyBookings.createRoute(tab = 2)) }
                        )
                        // ✦ CHANGE — Avg Rating card is now clickable → navigates to ViewReviews
                        LandlordStatCard(
                            icon       = Icons.Default.Star,
                            iconTint   = tk.goldPrime,
                            bgColor    = tk.goldPrime.copy(0.15f),
                            label      = "Avg Rating",
                            value      = if (uiState.averageRating > 0f) "%.1f".format(uiState.averageRating) else "—",
                            valueColor = tk.goldLight,
                            modifier   = Modifier.weight(1f),
                            isDark     = isDark,
                            onClick    = { navController.navigate(Screen.ViewReviews.route) }
                        )
                        // Pending Requests
                        LandlordStatCard(
                            icon       = Icons.Default.Notifications,
                            iconTint   = if (uiState.pendingRequestsCount > 0) tk.orangePend else tk.textMuted,
                            bgColor    = if (uiState.pendingRequestsCount > 0) tk.orangePend.copy(0.2f) else Color.White.copy(0.08f),
                            label      = "Pending Req",
                            value      = "${uiState.pendingRequestsCount}",
                            valueColor = if (uiState.pendingRequestsCount > 0) tk.orangePend.copy(pulseAlpha) else Color.White,
                            modifier   = Modifier.weight(1f),
                            isDark     = isDark,
                            highlight  = uiState.pendingRequestsCount > 0,
                            onClick    = { navController.navigate(Screen.MyBookings.createRoute(tab = 0)) }
                        )
                    }
                }
            }
        }

        // ── 2. REVENUE CARD ───────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            var revenuePressed by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = L_GoldPrime.copy(0.2f), spotColor = L_GoldPrime.copy(0.25f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(tk.navyLinear)
                    .border(2.dp, tk.goldBorder, RoundedCornerShape(24.dp))
            ) {
                GoldShineOverlay(revenuePressed)
                Box(Modifier.size(140.dp).align(Alignment.CenterEnd).offset(x = 45.dp).clip(CircleShape).background(tk.goldPrime.copy(0.08f)))
                Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(tk.goldBorder))

                Row(
                    modifier              = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL REVENUE", color = Color.White.copy(0.45f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(formattedRevenue, color = tk.goldPrime, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(18.dp).clip(CircleShape).background(tk.greenOk.copy(0.2f)), Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = tk.greenOk, modifier = Modifier.size(10.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Updated in real-time", color = tk.greenOk.copy(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(tk.goldGradient)
                            .shadow(10.dp, RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { revenuePressed = true; navController.navigate(Screen.PaymentReports.route) }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = if (isDark) D_BgDeep else L_NavyDeep, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Earnings", color = if (isDark) D_BgDeep else L_NavyDeep, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            LaunchedEffect(revenuePressed) { if (revenuePressed) { kotlinx.coroutines.delay(300); revenuePressed = false } }
        }

        // ── 3. QUICK ACTIONS ──────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Quick Actions", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = tk.textDark)
                    Text("Manage everything fast", fontSize = 12.sp, color = tk.textMuted)
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Add property button
                Box(
                    modifier = Modifier.fillMaxWidth().height(84.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = L_GoldPrime.copy(0.18f), spotColor = L_GoldPrime.copy(0.22f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(tk.navyLinear)
                        .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
                        .clickable { navController.navigate(Screen.AddProperty.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(tk.goldBorder))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 22.dp)
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape)
                                .background(tk.goldPrime.copy(0.18f))
                                .border(1.5.dp, tk.goldBorder, CircleShape),
                            Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = tk.goldPrime, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Add New Property", color = tk.goldPrime, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("List your property for rent", color = tk.goldLight.copy(0.6f), fontSize = 11.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = tk.goldPrime.copy(0.7f), modifier = Modifier.size(14.dp))
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Active bookings card
                    Box(
                        modifier = Modifier.weight(1f).height(114.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = L_GoldPrime.copy(0.15f), spotColor = L_GoldPrime.copy(0.18f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(tk.navyLinear)
                            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
                            .clickable { navController.navigate(Screen.MyBookings.route) }
                    ) {
                        Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(tk.goldBorder))
                        Column(
                            Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement   = Arrangement.Center,
                            horizontalAlignment   = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, tint = tk.goldPrime, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("${uiState.activeBookingsCount}", color = tk.goldPrime, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                            Text("Active Bookings", color = Color.White.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // My properties card
                    Box(
                        modifier = Modifier.weight(1f).height(114.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = L_GoldPrime.copy(0.15f), spotColor = L_GoldPrime.copy(0.18f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Brush.verticalGradient(listOf(D_BgCard, D_BgCardRaised)) else Brush.verticalGradient(listOf(L_GoldFaint, Color.White)))
                            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
                            .clickable { navController.navigate(Screen.MyProperties.route) }
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement   = Arrangement.Center,
                            horizontalAlignment   = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Home, null, tint = if (isDark) D_GoldPrimary else L_NavyDeep, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("${uiState.totalProperties}", color = if (isDark) D_GoldPrimary else L_NavyDeep, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                            Text("My Properties", color = tk.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // ── 4. MY PROPERTIES LIST ─────────────────────────────────────────────
        item {
            Spacer(Modifier.height(30.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("My Properties", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = tk.textDark)
                    Text("${uiState.featuredProperties.size} listed properties", fontSize = 12.sp, color = tk.textMuted)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) D_GoldFaint else L_GoldFaint)
                        .border(1.5.dp, tk.goldBorder, RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Screen.MyProperties.route) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("See All", fontSize = 12.sp, color = tk.goldDim, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = tk.goldDim, modifier = Modifier.size(10.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            uiState.isLoading -> item { LoadingShimmer(isDark) }
            uiState.featuredProperties.isEmpty() -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .shadow(10.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(tk.navyLinear)
                        .border(2.dp, tk.goldBorder, RoundedCornerShape(22.dp))
                        .clickable { navController.navigate(Screen.AddProperty.route) }
                        .padding(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(68.dp).clip(CircleShape).background(tk.goldPrime.copy(0.12f)), Alignment.Center) {
                            Icon(Icons.Default.AddHome, null, tint = tk.goldPrime, modifier = Modifier.size(34.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("No properties yet", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to add your first property", color = tk.goldPrime, fontSize = 13.sp)
                    }
                }
            }
            else -> items(uiState.featuredProperties.take(3)) { property ->
                PremiumLandlordPropertyCard(
                    property = property,
                    isDark   = isDark,
                    onClick  = { navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId)) }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LANDLORD STAT CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LandlordStatCard(
    icon      : ImageVector,
    iconTint  : Color,
    bgColor   : Color,
    label     : String,
    value     : String,
    valueColor: Color,
    modifier  : Modifier   = Modifier,
    isDark    : Boolean    = false,
    highlight : Boolean    = false,
    onClick   : () -> Unit = {}
) {
    val tk = resolveTokens(isDark)

    Box(
        modifier = modifier
            .shadow(if (isDark) 0.dp else 6.dp, RoundedCornerShape(18.dp), ambientColor = L_GoldPrime.copy(0.15f), spotColor = L_GoldPrime.copy(0.18f))
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    highlight -> Brush.linearGradient(listOf(tk.orangePend.copy(0.18f), tk.orangePend.copy(0.10f)))
                    isDark    -> Brush.linearGradient(listOf(D_BgCard, D_BgCardRaised))
                    else      -> Brush.linearGradient(listOf(Color.White.copy(0.13f), Color.White.copy(0.06f)))
                }
            )
            .border(
                width = 1.5.dp,
                brush = if (highlight)
                    Brush.horizontalGradient(listOf(tk.orangePend.copy(0.7f), tk.orangePend.copy(0.3f), tk.orangePend.copy(0.7f)))
                else
                    Brush.horizontalGradient(listOf(tk.goldPrime.copy(0.9f), tk.goldLight.copy(0.5f), tk.goldPrime.copy(0.9f))),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(
                        width = 1.dp,
                        brush = if (highlight)
                            Brush.horizontalGradient(listOf(tk.orangePend.copy(0.5f), tk.orangePend.copy(0.5f)))
                        else
                            Brush.horizontalGradient(listOf(tk.goldPrime.copy(0.9f), tk.goldLight.copy(0.5f), tk.goldPrime.copy(0.9f))),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, label, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, color = valueColor, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(2.dp))
            Text(label, color = if (isDark) D_TextSecondary.copy(0.85f) else Color.White.copy(0.7f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.3.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LANDLORD PROPERTY CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PremiumLandlordPropertyCard(
    property: Property,
    isDark  : Boolean    = false,
    onClick : () -> Unit
) {
    val tk = resolveTokens(isDark)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(if (isDark) 0.dp else 10.dp, RoundedCornerShape(20.dp), ambientColor = L_GoldPrime.copy(0.2f), spotColor = L_GoldPrime.copy(0.25f))
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Brush.verticalGradient(listOf(D_BgCard, D_BgCardRaised)) else Brush.verticalGradient(listOf(L_CardBg, L_GoldFaint.copy(0.3f))))
            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    Modifier.padding(start = 8.dp).size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, tk.goldBorder, RoundedCornerShape(16.dp))
                ) {
                    PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                    Box(
                        Modifier.align(Alignment.TopStart).padding(6.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (property.isAvailable) tk.greenOk else tk.orangePend)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (property.isAvailable) "Active" else "Inactive", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(property.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = tk.textDark)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = tk.goldPrime, modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = tk.textMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KingBed, null, tint = tk.textMuted.copy(0.7f), modifier = Modifier.size(12.dp))
                    Text(" ${property.bedrooms} beds", fontSize = 11.sp, color = tk.textMuted)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.People, null, tint = tk.textMuted.copy(0.7f), modifier = Modifier.size(12.dp))
                    Text(" ${property.maxGuests} guests", fontSize = 11.sp, color = tk.textMuted)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Price/month", fontSize = 9.sp, color = tk.textMuted)
                        Text(property.formattedPrice, fontWeight = FontWeight.ExtraBold, color = if (isDark) D_GoldPrimary else L_NavyDeep, fontSize = 14.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(tk.goldFaint)
                            .border(1.dp, tk.goldBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = tk.goldPrime, modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = tk.textDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TENANT HOME SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun TenantHomeScreen(
    navController     : NavController,
    viewModel         : HomeViewModel,
    uiState           : HomeUiState,
    vacationCount     : Int     = 0,
    isVacationLoading : Boolean = false,
    unreadCount       : Int     = 0,
    isDark            : Boolean = false
) {
    val categories       = listOf("All", "Premium", "House", "Apartment", "Room", "Villa", "Studio")
    var selectedCategory by remember { mutableStateOf("All") }
    val featScrollState  = rememberScrollState()
    val scope            = rememberCoroutineScope()
    val tk               = resolveTokens(isDark)

    val allProperties = uiState.allProperties.ifEmpty {
        (uiState.featuredProperties + uiState.nearbyProperties).distinctBy { it.propertyId }
    }

    val filteredFeatured = when (selectedCategory) {
        "All"     -> uiState.featuredProperties
        "Premium" -> {
            val fromFeatured = uiState.featuredProperties.filter { it.isPremium || it.propertyType.equals("PREMIUM", ignoreCase = true) }
            fromFeatured.ifEmpty { allProperties.filter { it.isPremium || it.propertyType.equals("PREMIUM", ignoreCase = true) } }
        }
        else -> {
            val fromFeatured = uiState.featuredProperties.filter { it.propertyType.equals(selectedCategory, ignoreCase = true) }
            fromFeatured.ifEmpty { allProperties.filter { it.propertyType.equals(selectedCategory, ignoreCase = true) } }
        }
    }

    val filteredNearby = when (selectedCategory) {
        "All"     -> uiState.nearbyProperties
        "Premium" -> uiState.nearbyProperties.filter { it.isPremium || it.propertyType.equals("PREMIUM", ignoreCase = true) }
        else      -> uiState.nearbyProperties.filter { it.propertyType.equals(selectedCategory, ignoreCase = true) }
    }

    val vacCountLabel = when {
        isVacationLoading -> "..."
        vacationCount > 0 -> "$vacationCount"
        else              -> "0"
    }

    val canScrollBack    by remember { derivedStateOf { featScrollState.value > 0 } }
    val canScrollForward by remember { derivedStateOf { featScrollState.value < featScrollState.maxValue } }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(tk.pageBg),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            HomeHeaderSection(
                onSearchClick       = { navController.navigate(Screen.Search.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onProfileClick      = { navController.navigate(Screen.Profile.route) },
                unreadCount         = unreadCount,
                profilePhotoUrl     = uiState.currentUserPhotoUrl,
                profileInitials     = uiState.currentUserInitials,
                isDark              = isDark
            )
        }

        item {
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Browse by Type", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tk.textDark)
                    Text("Find your perfect stay", fontSize = 12.sp, color = tk.textMuted)
                }
                Text("See All", fontSize = 13.sp, color = tk.goldPrime, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navController.navigate(Screen.PropertyList.route) })
            }
            Spacer(Modifier.height(14.dp))

            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { category ->
                    val isSelected    = selectedCategory == category
                    val isPremiumChip = category == "Premium"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isSelected && isPremiumChip -> Brush.linearGradient(listOf(L_GoldPrime, L_GoldLight, L_GoldPrime))
                                    isSelected                  -> Brush.linearGradient(listOf(if (isDark) D_BgSecondary else L_NavyPrime, if (isDark) D_BgPrimary else L_NavyMid))
                                    isPremiumChip               -> Brush.linearGradient(listOf(L_GoldPrime.copy(0.15f), L_GoldLight.copy(0.08f)))
                                    else                        -> Brush.linearGradient(listOf(tk.cardBg, tk.cardBg))
                                }
                            )
                            .border(
                                width = if (isPremiumChip) 1.5.dp else 1.dp,
                                brush = when {
                                    isPremiumChip -> Brush.horizontalGradient(listOf(L_GoldPrime.copy(0.8f), L_GoldLight.copy(0.5f), L_GoldPrime.copy(0.8f)))
                                    isSelected    -> Brush.horizontalGradient(listOf(tk.goldPrime.copy(0.5f), tk.goldPrime.copy(0.5f)))
                                    else          -> Brush.horizontalGradient(listOf(if (isDark) D_Border else Color.Transparent, if (isDark) D_Border else Color.Transparent))
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = if (isPremiumChip) 14.dp else 18.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (isPremiumChip) Text("👑", fontSize = 13.sp)
                            Text(
                                category,
                                color = when {
                                    isSelected && isPremiumChip -> L_NavyDeep
                                    isSelected                  -> tk.goldPrime
                                    isPremiumChip               -> L_GoldPrime
                                    else                        -> tk.textDark
                                },
                                fontSize   = 13.sp,
                                fontWeight = if (isSelected || isPremiumChip) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Featured Properties
        item {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Featured Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tk.textDark)
                    Text("Handpicked for you", fontSize = 12.sp, color = tk.textMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val activeBg = if (isDark) D_BgSecondary else L_NavyPrime
                    val inactBg  = if (isDark) D_BgCard else Color(0xFFE0E0E0)
                    Box(Modifier.size(34.dp).clip(CircleShape).background(if (canScrollBack) activeBg else inactBg).clickable { scope.launch { featScrollState.animateScrollTo((featScrollState.value - 550).coerceAtLeast(0)) } }, Alignment.Center) { Text("‹", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(if (canScrollForward) activeBg else inactBg).clickable { scope.launch { featScrollState.animateScrollTo((featScrollState.value + 550).coerceAtMost(featScrollState.maxValue)) } }, Alignment.Center) { Text("›", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(14.dp))

            when {
                uiState.isLoading -> LoadingShimmer(isDark)
                uiState.errorMessage != null && allProperties.isEmpty() -> Text("Error: ${uiState.errorMessage}", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
                filteredFeatured.isEmpty() -> Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp)).background(tk.cardBg).padding(28.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (selectedCategory == "Premium") "👑" else "🏠", fontSize = 34.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(when (selectedCategory) { "All" -> "No featured properties"; "Premium" -> "No premium properties available"; else -> "No featured $selectedCategory properties" }, color = tk.textMuted, fontSize = 13.sp)
                        if (selectedCategory != "All") { Spacer(Modifier.height(4.dp)); Text("Check Nearby Properties below", color = tk.goldPrime, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    }
                }
                else -> Row(Modifier.fillMaxWidth().horizontalScroll(featScrollState).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    filteredFeatured.take(7).forEach { property ->
                        FeaturedPropertyCard(
                            property    = property,
                            isFavourite = uiState.favouriteIds.contains(property.propertyId),
                            onFavToggle = { viewModel.toggleFavourite(property.propertyId) },
                            isDark      = isDark,
                            onClick     = { navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId)) }
                        )
                    }
                    Box(
                        modifier = Modifier.width(160.dp).height(265.dp)
                            .shadow(6.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(tk.cardBg)
                            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
                            .clickable { navController.navigate(Screen.PropertyList.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Box(Modifier.size(52.dp).clip(CircleShape).background(tk.goldFaint).border(1.5.dp, tk.goldBorder, CircleShape), Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = tk.goldPrime, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("See all", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tk.textDark)
                            Text("${allProperties.size} properties", fontSize = 11.sp, color = tk.textMuted)
                        }
                    }
                }
            }
        }

        // Vacation Hub
        item {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .shadow(14.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(tk.navyLinear)
                    .border(2.dp, tk.goldBorder, RoundedCornerShape(26.dp))
            ) {
                Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(tk.goldBorder))
                Box(Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-50).dp).clip(CircleShape).background(tk.goldPrime.copy(0.07f)))
                Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(tk.goldBorder))

                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(56.dp).clip(CircleShape).background(Brush.radialGradient(listOf(tk.goldPrime.copy(0.28f), tk.goldPrime.copy(0.05f)))), Alignment.Center) { Text("🏔️", fontSize = 28.sp) }
                            Column {
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(tk.goldPrime.copy(0.18f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("VACATION HUB", color = tk.goldPrime, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Explore Northern Stays", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Transparent).border(2.dp, tk.goldBorder, CircleShape).clickable { navController.navigate(Screen.VacationRentals.route) }, Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = tk.goldPrime, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(tk.goldPrime.copy(0.45f), Color.Transparent))))
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        VacationMiniStat(vacCountLabel, "Stays",   "🏠", tk.goldPrime)
                        VacationMiniStat("PT-1",        "Verified","✅", tk.goldPrime)
                        VacationMiniStat("4.8★",        "Rating",  "⭐", tk.goldPrime)
                        VacationMiniStat("Secure",      "Booking", "🔒", tk.goldPrime)
                    }
                }
            }
        }

        // Nearby Stays
        item {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Nearby Stays", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tk.textDark)
                    Text("${filteredNearby.size} properties near you", fontSize = 12.sp, color = tk.textMuted)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) D_BgCard else L_NavyPrime)
                        .border(1.dp, tk.goldBorder, RoundedCornerShape(10.dp))
                        .clickable { navController.navigate(Screen.ExploreMap.route) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🗺", fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("Explore Map", fontSize = 12.sp, color = tk.goldPrime, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            uiState.isLoading -> item { LoadingShimmer(isDark) }
            filteredNearby.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (selectedCategory == "Premium") "👑" else "🔍", fontSize = 42.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(when (selectedCategory) { "Premium" -> "No premium stays nearby"; "All" -> "No nearby properties found"; else -> "No $selectedCategory stays nearby" }, color = tk.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            else -> items(filteredNearby) { property ->
                NearbyPropertyCard(
                    property    = property,
                    isFavourite = uiState.favouriteIds.contains(property.propertyId),
                    onFavToggle = { viewModel.toggleFavourite(property.propertyId) },
                    isDark      = isDark,
                    onClick     = { navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId)) }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HOME HEADER — Tenant
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun HomeHeaderSection(
    onSearchClick      : () -> Unit,
    onNotificationClick: () -> Unit = {},
    onProfileClick     : () -> Unit = {},
    unreadCount        : Int        = 0,
    profilePhotoUrl    : String     = "",
    profileInitials    : String     = "",
    isDark             : Boolean    = false
) {
    val tk       = resolveTokens(isDark)
    val searchBg  = if (isDark) D_BgCard else L_CardBg
    val searchBtn = if (isDark) D_BgSecondary else L_NavyPrime

    Box(modifier = Modifier.fillMaxWidth().background(tk.navyGradient)) {
        Box(Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 55.dp, y = (-55).dp).clip(CircleShape).background(tk.goldPrime.copy(0.06f)))
        Box(Modifier.size(80.dp).align(Alignment.BottomStart).offset(x = (-20).dp, y = 20.dp).clip(CircleShape).background(tk.goldPrime.copy(0.04f)))
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(tk.goldBorder))

        Column(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 26.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Welcome Back 👋", color = tk.goldPrime, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Find Your Haven", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Pakistan's trusted rental platform", color = Color.White.copy(0.55f), fontSize = 11.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    NotificationIconButton(count = unreadCount, onClick = onNotificationClick, tint = tk.goldPrime)
                    ProfileAvatarButton(
                        photoUrl   = profilePhotoUrl,
                        initials   = profileInitials,
                        goldBorder = tk.goldBorder,
                        goldPrime  = tk.goldPrime,
                        onClick    = onProfileClick
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.fillMaxWidth().height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(searchBg)
                    .border(1.5.dp, tk.goldBorder, RoundedCornerShape(16.dp))
                    .clickable { onSearchClick() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = tk.goldPrime, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Search city, property type...", color = tk.textMuted, fontSize = 14.sp)
                    }
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(searchBtn).padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Text("Search", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FEATURED PROPERTY CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun FeaturedPropertyCard(
    property   : Property,
    isFavourite: Boolean    = false,
    onFavToggle: () -> Unit = {},
    isDark     : Boolean    = false,
    onClick    : () -> Unit
) {
    val tk = resolveTokens(isDark)

    Box(
        modifier = Modifier.width(265.dp).wrapContentHeight()
            .shadow(if (isDark) 0.dp else 10.dp, RoundedCornerShape(22.dp), ambientColor = L_GoldPrime.copy(0.2f), spotColor = L_GoldPrime.copy(0.25f))
            .clip(RoundedCornerShape(22.dp))
            .background(tk.cardBg)
            .border(2.dp, tk.goldBorder, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(158.dp)) {
                PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.5f)))))
                if (property.isPremium) {
                    Box(
                        Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(L_GoldDim, L_GoldPrime, L_GoldLight, L_GoldPrime, L_GoldDim)))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("👑", fontSize = 9.sp)
                            Text("PREMIUM", color = L_NavyDeep, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp).clip(CircleShape).background(Color.Black.copy(0.35f)).clickable { onFavToggle() }, Alignment.Center) {
                    Icon(if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite", tint = if (isFavourite) Color(0xFFE53935) else Color.White, modifier = Modifier.size(17.dp))
                }
                Box(Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(tk.goldLinear).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(property.formattedPrice, color = if (isDark) D_BgDeep else L_NavyDeep, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(if (isDark) D_BgSecondary.copy(0.9f) else L_NavyPrime.copy(0.85f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(property.propertyTypeEnum.displayName(), color = tk.goldPrime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (property.isAvailable) {
                    Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(tk.greenOk).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Text("Available", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = tk.textDark)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = tk.goldPrime, modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = tk.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tk.goldFaint).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Icon(Icons.Default.Star, null, tint = tk.goldPrime, modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = tk.textDark, fontWeight = FontWeight.SemiBold)
                        Text(" (${property.reviewCount})", fontSize = 11.sp, color = tk.textMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.KingBed, null, tint = tk.textMuted, modifier = Modifier.size(12.dp))
                        Text(" ${property.bedrooms}", fontSize = 11.sp, color = tk.textMuted)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.People, null, tint = tk.textMuted, modifier = Modifier.size(12.dp))
                        Text(" ${property.maxGuests}", fontSize = 11.sp, color = tk.textMuted)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NEARBY PROPERTY CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun NearbyPropertyCard(
    property   : Property,
    isFavourite: Boolean    = false,
    onFavToggle: () -> Unit = {},
    isDark     : Boolean    = false,
    onClick    : () -> Unit
) {
    val tk = resolveTokens(isDark)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(if (isDark) 0.dp else 8.dp, RoundedCornerShape(20.dp), ambientColor = L_GoldPrime.copy(0.15f), spotColor = L_GoldPrime.copy(0.2f))
            .clip(RoundedCornerShape(20.dp))
            .background(tk.cardBg)
            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(104.dp).clip(RoundedCornerShape(16.dp)).border(1.5.dp, tk.goldBorder, RoundedCornerShape(16.dp))) {
                PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                if (property.isAvailable) { Box(Modifier.align(Alignment.TopStart).padding(7.dp).size(8.dp).clip(CircleShape).background(tk.greenOk)) }
                if (property.isPremium) {
                    Box(Modifier.align(Alignment.BottomStart).padding(5.dp).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(L_GoldDim, L_GoldPrime))).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("👑", fontSize = 9.sp) }
                }
                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(0.3f)).clickable { onFavToggle() }, Alignment.Center) {
                    Icon(if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite", tint = if (isFavourite) Color(0xFFE53935) else Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(property.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = tk.textDark, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(if (isDark) D_BgSecondary else L_NavyPrime.copy(0.08f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(property.propertyTypeEnum.displayName(), fontSize = 10.sp, color = if (isDark) D_TextSecondary else L_NavyPrime, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = tk.goldPrime, modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = tk.textMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KingBed, null, tint = tk.textMuted, modifier = Modifier.size(12.dp))
                    Text(" ${property.bedrooms} beds", fontSize = 11.sp, color = tk.textMuted)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.People, null, tint = tk.textMuted, modifier = Modifier.size(12.dp))
                    Text(" ${property.maxGuests} guests", fontSize = 11.sp, color = tk.textMuted)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${property.formattedPrice}/night", fontWeight = FontWeight.ExtraBold, color = if (isDark) D_GoldPrimary else L_NavyDeep, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tk.goldFaint).border(1.dp, tk.goldBorder, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Star, null, tint = tk.goldPrime, modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = tk.textDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// VACATION MINI STAT
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun VacationMiniStat(value: String, label: String, emoji: String, goldColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = goldColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(0.45f), fontSize = 9.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOADING SHIMMER
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun LoadingShimmer(isDark: Boolean = false) {
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color       = if (isDark) D_GoldPrimary else L_GoldPrime,
            strokeWidth = 3.dp,
            modifier    = Modifier.size(42.dp)
        )
    }
}








