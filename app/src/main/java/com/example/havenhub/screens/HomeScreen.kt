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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.MainActivity
import com.example.havenhub.components.NotificationIconButton
import com.example.havenhub.data.Property
import com.example.havenhub.data.SeasonalAlert
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.HomeViewModel
import com.example.havenhub.viewmodel.HomeUiState
import com.example.havenhub.viewmodel.NotificationViewModel
import com.example.havenhub.viewmodel.SeasonalAlertViewModel
import com.example.havenhub.viewmodel.VacationViewModel
import kotlinx.coroutines.launch

// =============================================================================
// DESIGN TOKENS — LIGHT THEME
// =============================================================================
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

// =============================================================================
// DESIGN TOKENS — DARK THEME
// =============================================================================
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

// =============================================================================
// THEME-AWARE TOKEN RESOLVER
// =============================================================================
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

// =============================================================================
// RESPONSIVE HELPERS
// =============================================================================
private data class ResponsiveValues(
    val screenWidth        : Dp,
    val horizontalPadding  : Dp,
    val headerTitleSize    : Float,
    val headerSubSize      : Float,
    val statCardIconSize   : Dp,
    val statCardValueSize  : Float,
    val statCardLabelSize  : Float,
    val statCardPadding    : Dp,
    val revenueFontSize    : Float,
    val avatarSize         : Dp,
    val featCardWidth      : Dp,
    val featCardImgHeight  : Dp,
    val nearbyImgSize      : Dp,
    val sectionTitleSize   : Float,
    val bodyTextSize       : Float,
    val chipPaddingH       : Dp,
    val chipPaddingV       : Dp
)

@Composable
private fun rememberResponsive(): ResponsiveValues {
    val config = LocalConfiguration.current
    val sw     = config.screenWidthDp.dp
    return when {
        sw < 360.dp -> ResponsiveValues(
            screenWidth       = sw,
            horizontalPadding = 14.dp,
            headerTitleSize   = 22f,
            headerSubSize     = 10f,
            statCardIconSize  = 36.dp,
            statCardValueSize = 18f,
            statCardLabelSize = 8f,
            statCardPadding   = 6.dp,
            revenueFontSize   = 28f,
            avatarSize        = 40.dp,
            featCardWidth     = 220.dp,
            featCardImgHeight = 130.dp,
            nearbyImgSize     = 88.dp,
            sectionTitleSize  = 16f,
            bodyTextSize      = 11f,
            chipPaddingH      = 12.dp,
            chipPaddingV      = 8.dp
        )
        sw < 400.dp -> ResponsiveValues(
            screenWidth       = sw,
            horizontalPadding = 16.dp,
            headerTitleSize   = 24f,
            headerSubSize     = 11f,
            statCardIconSize  = 40.dp,
            statCardValueSize = 20f,
            statCardLabelSize = 9f,
            statCardPadding   = 8.dp,
            revenueFontSize   = 32f,
            avatarSize        = 44.dp,
            featCardWidth     = 248.dp,
            featCardImgHeight = 148.dp,
            nearbyImgSize     = 96.dp,
            sectionTitleSize  = 17f,
            bodyTextSize      = 12f,
            chipPaddingH      = 14.dp,
            chipPaddingV      = 9.dp
        )
        sw < 480.dp -> ResponsiveValues(
            screenWidth       = sw,
            horizontalPadding = 20.dp,
            headerTitleSize   = 26f,
            headerSubSize     = 12f,
            statCardIconSize  = 42.dp,
            statCardValueSize = 21f,
            statCardLabelSize = 9f,
            statCardPadding   = 8.dp,
            revenueFontSize   = 34f,
            avatarSize        = 46.dp,
            featCardWidth     = 265.dp,
            featCardImgHeight = 158.dp,
            nearbyImgSize     = 104.dp,
            sectionTitleSize  = 18f,
            bodyTextSize      = 12f,
            chipPaddingH      = 18.dp,
            chipPaddingV      = 10.dp
        )
        else -> ResponsiveValues(
            screenWidth       = sw,
            horizontalPadding = 24.dp,
            headerTitleSize   = 28f,
            headerSubSize     = 13f,
            statCardIconSize  = 46.dp,
            statCardValueSize = 23f,
            statCardLabelSize = 10f,
            statCardPadding   = 10.dp,
            revenueFontSize   = 36f,
            avatarSize        = 50.dp,
            featCardWidth     = 285.dp,
            featCardImgHeight = 170.dp,
            nearbyImgSize     = 112.dp,
            sectionTitleSize  = 19f,
            bodyTextSize      = 13f,
            chipPaddingH      = 20.dp,
            chipPaddingV      = 11.dp
        )
    }
}

// =============================================================================
// CARD CLICK SHINE HELPER
// =============================================================================
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

// =============================================================================
// PROFILE AVATAR COMPOSABLE
// =============================================================================
@Composable
private fun ProfileAvatarButton(
    photoUrl  : String,
    initials  : String,
    goldBorder: Brush,
    goldPrime : Color,
    avatarSize: Dp    = 46.dp,
    onClick   : () -> Unit
) {
    Box(
        modifier = Modifier
            .size(avatarSize)
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
                modifier           = Modifier.fillMaxSize().clip(CircleShape),
                contentScale       = ContentScale.Crop
            )
        } else {
            Text(
                text       = initials.take(2),
                fontSize   = (avatarSize.value * 0.30f).sp,
                fontWeight = FontWeight.ExtraBold,
                color      = goldPrime
            )
        }
    }
}

// =============================================================================
// ✦ NEW — SEASONAL HOME BANNER
//
// Shown just below the header in both Tenant and Landlord home screens.
// Displays the first active seasonal alert as a compact dismissible banner.
// Tapping "View All" navigates to NotificationsScreen where full alert list
// is shown.
//
// Design: golden gradient card with emoji, title, and a subtle "View All" link.
// Only shown when at least one active alert exists for the current role.
// =============================================================================
@Composable
private fun SeasonalHomeBanner(
    alert        : SeasonalAlert,
    isDark       : Boolean,
    hPadding     : Dp,
    onViewAllClick: () -> Unit
) {
    val bannerBg     = if (isDark) Color(0xFF1A1608) else Color(0xFFFFFBEA)
    val bannerBorder = if (isDark) Color(0xFFB8962E).copy(0.5f) else Color(0xFFD4AF37).copy(0.55f)
    val titleColor   = if (isDark) Color(0xFFF5D060) else Color(0xFF4A3800)
    val msgColor     = if (isDark) Color(0xFFD4AF37).copy(0.75f) else Color(0xFF5C4800).copy(0.8f)
    val goldAccent   = if (isDark) Color(0xFFD4AF37) else Color(0xFF9B7D2E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = hPadding, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bannerBg)
            .border(1.5.dp, bannerBorder, RoundedCornerShape(14.dp))
    ) {
        // Left golden accent line
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .background(goldAccent)
        )

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Emoji bubble
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(goldAccent.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = alert.iconEmoji.ifEmpty { "🎉" },
                    fontSize = 18.sp
                )
            }

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = alert.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 12.sp,
                    color      = titleColor,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = alert.message,
                    fontSize = 11.sp,
                    color    = msgColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // "View All" button
            Text(
                text       = "View All",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = goldAccent,
                modifier   = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(goldAccent.copy(0.12f))
                    .clickable { onViewAllClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// =============================================================================
// HOME SCREEN — Entry Point
// =============================================================================
@Composable
fun HomeScreen(
    navController        : NavController,
    viewModel            : HomeViewModel            = hiltViewModel(),
    authViewModel        : AuthViewModel            = hiltViewModel(),
    vacationViewModel    : VacationViewModel        = hiltViewModel(),
    notificationViewModel: NotificationViewModel    = hiltViewModel(),
    // ✦ NEW — inject SeasonalAlertViewModel
    seasonalViewModel    : SeasonalAlertViewModel   = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val authState     by authViewModel.uiState.collectAsState()
    val vacUiState    by vacationViewModel.uiState.collectAsState()
    val notifState    by notificationViewModel.uiState.collectAsState()
    val seasonalState by seasonalViewModel.uiState.collectAsState()   // ✦ NEW
    val isDark        by MainActivity.darkModeFlow.collectAsState()

    if (!authState.isAuthReady) {
        val tk = resolveTokens(isDark)
        Box(
            modifier         = Modifier.fillMaxSize().background(tk.pageBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color       = tk.goldPrime,
                strokeWidth = 3.dp,
                modifier    = Modifier.size(40.dp)
            )
        }
        return
    }

    val userRole = authState.userRole.lowercase().trim()
    val userId   = authState.currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) notificationViewModel.startListening(userId)
    }

    // ✦ NEW — Load seasonal alerts once role is known (skip admin)
    LaunchedEffect(userRole) {
        if (userRole.isNotEmpty() && userRole != "admin" && userRole != "sub_admin") {
            seasonalViewModel.loadAlertsForRole(userRole)
        }
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

    // ✦ NEW — Pass the first active alert to both home screens as a banner
    val firstActiveAlert = seasonalState.alerts.firstOrNull()

    when (userRole) {
        "landlord" -> LandlordHomeScreen(
            navController    = navController,
            uiState          = uiState,
            unreadCount      = notifState.unreadCount,
            isDark           = isDark,
            seasonalAlert    = firstActiveAlert        // ✦ NEW
        )
        else -> TenantHomeScreen(
            navController     = navController,
            viewModel         = viewModel,
            uiState           = uiState,
            vacationCount     = vacUiState.properties.size,
            isVacationLoading = vacUiState.isLoading,
            unreadCount       = notifState.unreadCount,
            isDark            = isDark,
            seasonalAlert     = firstActiveAlert       // ✦ NEW
        )
    }
}

// =============================================================================
// IMAGE HELPER
// =============================================================================
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
        AsyncImage(
            model              = remoteUrl,
            contentDescription = property.title,
            modifier           = modifier,
            contentScale       = contentScale
        )
    } else {
        Image(
            painter            = painterResource(id = resolveDrawable(property)),
            contentDescription = property.title,
            modifier           = modifier,
            contentScale       = contentScale
        )
    }
}

// =============================================================================
// LANDLORD HOME SCREEN
// ✦ CHANGE: seasonalAlert parameter added, banner shown below header
// =============================================================================
@Composable
private fun LandlordHomeScreen(
    navController : NavController,
    uiState       : HomeUiState,
    unreadCount   : Int          = 0,
    isDark        : Boolean      = false,
    seasonalAlert : SeasonalAlert? = null   // ✦ NEW
) {
    val tk  = resolveTokens(isDark)
    val res = rememberResponsive()

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
        label         = "pulseAlpha"
    )

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(tk.pageBg),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. HEADER
        item {
            Box(modifier = Modifier.fillMaxWidth().background(tk.navyGradient)) {
                Box(Modifier.size(200.dp).align(Alignment.TopEnd).offset(x = 70.dp, y = (-70).dp).clip(CircleShape).background(tk.goldPrime.copy(0.06f)))
                Box(Modifier.size(100.dp).align(Alignment.BottomStart).offset(x = (-30).dp, y = 40.dp).clip(CircleShape).background(tk.goldPrime.copy(0.04f)))
                Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(tk.goldBorder))

                Column(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = res.horizontalPadding)
                        .padding(top = 18.dp, bottom = 26.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Welcome Back 👋",
                                color         = tk.goldPrime,
                                fontSize      = res.headerSubSize.sp,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Manage Your Haven",
                                color      = Color.White,
                                fontSize   = res.headerTitleSize.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Track listings & booking requests",
                                color    = Color.White.copy(0.5f),
                                fontSize = res.headerSubSize.sp
                            )
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
                                avatarSize = res.avatarSize,
                                onClick    = { navController.navigate(Screen.Profile.route) }
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LandlordStatCard(
                            icon        = Icons.Default.People,
                            iconTint    = Color(0xFF5BC8FF),
                            bgColor     = Color(0xFF5BC8FF).copy(0.15f),
                            label       = "Active Tenants",
                            value       = "${uiState.activeTenantsCount}",
                            valueColor  = Color.White,
                            modifier    = Modifier.weight(1f),
                            isDark      = isDark,
                            iconSize    = res.statCardIconSize,
                            valueSize   = res.statCardValueSize,
                            labelSize   = res.statCardLabelSize,
                            cardPadding = res.statCardPadding,
                            onClick     = { navController.navigate(Screen.MyBookings.createRoute(tab = 2)) }
                        )
                        LandlordStatCard(
                            icon        = Icons.Default.Notifications,
                            iconTint    = if (uiState.pendingRequestsCount > 0) tk.orangePend else tk.textMuted,
                            bgColor     = if (uiState.pendingRequestsCount > 0) tk.orangePend.copy(0.2f) else Color.White.copy(0.08f),
                            label       = "Pending Req",
                            value       = "${uiState.pendingRequestsCount}",
                            valueColor  = if (uiState.pendingRequestsCount > 0) tk.orangePend.copy(pulseAlpha) else Color.White,
                            modifier    = Modifier.weight(1f),
                            isDark      = isDark,
                            iconSize    = res.statCardIconSize,
                            valueSize   = res.statCardValueSize,
                            labelSize   = res.statCardLabelSize,
                            cardPadding = res.statCardPadding,
                            highlight   = uiState.pendingRequestsCount > 0,
                            onClick     = { navController.navigate(Screen.MyBookings.createRoute(tab = 0)) }
                        )
                    }
                }
            }
        }

        // ✦ NEW — Seasonal Banner (landlords see rent-related tips)
        // Shown between header and revenue card so it's immediately visible
        if (seasonalAlert != null) {
            item {
                Spacer(Modifier.height(14.dp))
                SeasonalHomeBanner(
                    alert         = seasonalAlert,
                    isDark        = isDark,
                    hPadding      = res.horizontalPadding,
                    onViewAllClick = { navController.navigate(Screen.Notifications.route) }
                )
            }
        }

        // 2. REVENUE CARD
        item {
            Spacer(Modifier.height(20.dp))
            var revenuePressed by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding)
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
                    Column(Modifier.weight(1f)) {
                        Text(
                            "TOTAL REVENUE",
                            color         = Color.White.copy(0.45f),
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.8.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            formattedRevenue,
                            color      = tk.goldPrime,
                            fontSize   = res.revenueFontSize.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(18.dp).clip(CircleShape).background(tk.greenOk.copy(0.2f)),
                                Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    null,
                                    tint     = tk.greenOk,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Updated in real-time",
                                color      = tk.greenOk.copy(0.9f),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(tk.goldGradient)
                            .shadow(10.dp, RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) {
                                revenuePressed = true
                                navController.navigate(Screen.PaymentReports.route)
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                null,
                                tint     = if (isDark) D_BgDeep else L_NavyDeep,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Earnings",
                                color      = if (isDark) D_BgDeep else L_NavyDeep,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            LaunchedEffect(revenuePressed) {
                if (revenuePressed) {
                    kotlinx.coroutines.delay(300)
                    revenuePressed = false
                }
            }
        }

        // 3. QUICK ACTIONS
        item {
            Spacer(Modifier.height(30.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Quick Actions",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = res.sectionTitleSize.sp,
                        color      = tk.textDark
                    )
                    Text(
                        "Manage everything fast",
                        fontSize = (res.sectionTitleSize - 6f).sp,
                        color    = tk.textMuted
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            Text(
                                "Add New Property",
                                color      = tk.goldPrime,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 16.sp
                            )
                            Text(
                                "List your property for rent",
                                color    = tk.goldLight.copy(0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            null,
                            tint     = tk.goldPrime.copy(0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, tint = tk.goldPrime, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${uiState.activeBookingsCount}",
                                color      = tk.goldPrime,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 28.sp
                            )
                            Text(
                                "Active Bookings",
                                color    = Color.White.copy(0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f).height(114.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = L_GoldPrime.copy(0.15f), spotColor = L_GoldPrime.copy(0.18f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isDark) Brush.verticalGradient(listOf(D_BgCard, D_BgCardRaised))
                                else Brush.verticalGradient(listOf(L_GoldFaint, Color.White))
                            )
                            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
                            .clickable { navController.navigate(Screen.MyProperties.route) }
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Home,
                                null,
                                tint     = if (isDark) D_GoldPrimary else L_NavyDeep,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${uiState.totalProperties}",
                                color      = if (isDark) D_GoldPrimary else L_NavyDeep,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 28.sp
                            )
                            Text(
                                "My Properties",
                                color    = tk.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4. MY PROPERTIES LIST
        item {
            Spacer(Modifier.height(30.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "My Properties",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = res.sectionTitleSize.sp,
                        color      = tk.textDark
                    )
                    Text(
                        "${uiState.featuredProperties.size} listed properties",
                        fontSize = (res.sectionTitleSize - 6f).sp,
                        color    = tk.textMuted
                    )
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) D_GoldFaint else L_GoldFaint)
                        .border(1.5.dp, tk.goldBorder, RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Screen.MyProperties.route) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "See All",
                            fontSize   = 12.sp,
                            color      = tk.goldDim,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            null,
                            tint     = tk.goldDim,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            uiState.isLoading -> item { LoadingShimmer(isDark) }
            uiState.featuredProperties.isEmpty() -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding)
                        .shadow(10.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(tk.navyLinear)
                        .border(2.dp, tk.goldBorder, RoundedCornerShape(22.dp))
                        .clickable { navController.navigate(Screen.AddProperty.route) }
                        .padding(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(68.dp).clip(CircleShape).background(tk.goldPrime.copy(0.12f)),
                            Alignment.Center
                        ) {
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
                    hPadding = res.horizontalPadding,
                    imgSize  = res.nearbyImgSize,
                    onClick  = {
                        navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                    }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// =============================================================================
// LANDLORD STAT CARD — Responsive
// =============================================================================
@Composable
private fun LandlordStatCard(
    icon        : ImageVector,
    iconTint    : Color,
    bgColor     : Color,
    label       : String,
    value       : String,
    valueColor  : Color,
    modifier    : Modifier   = Modifier,
    isDark      : Boolean    = false,
    highlight   : Boolean    = false,
    iconSize    : Dp         = 42.dp,
    valueSize   : Float      = 21f,
    labelSize   : Float      = 9f,
    cardPadding : Dp         = 8.dp,
    onClick     : () -> Unit = {}
) {
    val tk = resolveTokens(isDark)

    Box(
        modifier = modifier
            .shadow(
                elevation   = if (isDark) 0.dp else 6.dp,
                shape       = RoundedCornerShape(18.dp),
                ambientColor = L_GoldPrime.copy(0.15f),
                spotColor   = L_GoldPrime.copy(0.18f)
            )
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
            .padding(horizontal = cardPadding, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(iconSize)
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
                Icon(icon, label, tint = iconTint, modifier = Modifier.size(iconSize * 0.44f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                color         = valueColor,
                fontSize      = valueSize.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color         = if (isDark) D_TextSecondary.copy(0.85f) else Color.White.copy(0.7f),
                fontSize      = labelSize.sp,
                fontWeight    = FontWeight.SemiBold,
                maxLines      = 1,
                overflow      = TextOverflow.Ellipsis,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// =============================================================================
// LANDLORD PROPERTY CARD — Responsive
// =============================================================================
@Composable
private fun PremiumLandlordPropertyCard(
    property : Property,
    isDark   : Boolean    = false,
    hPadding : Dp         = 20.dp,
    imgSize  : Dp         = 90.dp,
    onClick  : () -> Unit
) {
    val tk = resolveTokens(isDark)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = hPadding, vertical = 6.dp)
            .shadow(
                elevation    = if (isDark) 0.dp else 10.dp,
                shape        = RoundedCornerShape(20.dp),
                ambientColor = L_GoldPrime.copy(0.2f),
                spotColor    = L_GoldPrime.copy(0.25f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDark) Brush.verticalGradient(listOf(D_BgCard, D_BgCardRaised))
                else Brush.verticalGradient(listOf(L_CardBg, L_GoldFaint.copy(0.3f)))
            )
            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    Modifier.padding(start = 8.dp).size(imgSize)
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
                        Text(
                            if (property.isAvailable) "Active" else "Inactive",
                            color      = Color.White,
                            fontSize   = 7.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    property.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    color      = tk.textDark
                )
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Price/month", fontSize = 9.sp, color = tk.textMuted)
                        Text(
                            property.formattedPrice,
                            fontWeight = FontWeight.ExtraBold,
                            color      = if (isDark) D_GoldPrimary else L_NavyDeep,
                            fontSize   = 14.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clip(RoundedCornerShape(10.dp))
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

// =============================================================================
// TENANT HOME SCREEN
// ✦ CHANGE: seasonalAlert parameter added, banner shown below header section
// =============================================================================
@Composable
private fun TenantHomeScreen(
    navController     : NavController,
    viewModel         : HomeViewModel,
    uiState           : HomeUiState,
    vacationCount     : Int          = 0,
    isVacationLoading : Boolean      = false,
    unreadCount       : Int          = 0,
    isDark            : Boolean      = false,
    seasonalAlert     : SeasonalAlert? = null   // ✦ NEW
) {
    val categories       = listOf("All", "Premium", "House", "Apartment", "Room", "Villa", "Studio")
    var selectedCategory by remember { mutableStateOf("All") }
    val featScrollState  = rememberScrollState()
    val scope            = rememberCoroutineScope()
    val tk               = resolveTokens(isDark)
    val res              = rememberResponsive()

    val allProperties = uiState.allProperties.ifEmpty {
        (uiState.featuredProperties + uiState.nearbyProperties).distinctBy { it.propertyId }
    }

    val filteredFeatured = when (selectedCategory) {
        "All"     -> uiState.featuredProperties
        "Premium" -> {
            val fromFeatured = uiState.featuredProperties.filter {
                it.isPremium || it.propertyType.equals("PREMIUM", ignoreCase = true)
            }
            fromFeatured.ifEmpty {
                allProperties.filter { it.isPremium || it.propertyType.equals("PREMIUM", ignoreCase = true) }
            }
        }
        else -> {
            val fromFeatured = uiState.featuredProperties.filter {
                it.propertyType.equals(selectedCategory, ignoreCase = true)
            }
            fromFeatured.ifEmpty {
                allProperties.filter { it.propertyType.equals(selectedCategory, ignoreCase = true) }
            }
        }
    }

    val filteredNearby = when (selectedCategory) {
        "All"     -> uiState.nearbyProperties
        "Premium" -> uiState.nearbyProperties.filter {
            it.isPremium || it.propertyType.equals("PREMIUM", ignoreCase = true)
        }
        else      -> uiState.nearbyProperties.filter {
            it.propertyType.equals(selectedCategory, ignoreCase = true)
        }
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

        // ✦ NEW — Seasonal Banner for tenants (shown right after header)
        // Compact single-line banner with emoji, title, and "View All" link
        if (seasonalAlert != null) {
            item {
                Spacer(Modifier.height(12.dp))
                SeasonalHomeBanner(
                    alert          = seasonalAlert,
                    isDark         = isDark,
                    hPadding       = res.horizontalPadding,
                    onViewAllClick = { navController.navigate(Screen.Notifications.route) }
                )
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Browse by Type",
                        fontWeight = FontWeight.Bold,
                        fontSize   = res.sectionTitleSize.sp,
                        color      = tk.textDark
                    )
                    Text(
                        "Find your perfect stay",
                        fontSize = (res.sectionTitleSize - 6f).sp,
                        color    = tk.textMuted
                    )
                }
                Text(
                    "See All",
                    fontSize   = 13.sp,
                    color      = tk.goldPrime,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable { navController.navigate(Screen.PropertyList.route) }
                )
            }
            Spacer(Modifier.height(14.dp))

            LazyRow(
                contentPadding        = PaddingValues(horizontal = res.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            .padding(horizontal = res.chipPaddingH, vertical = res.chipPaddingV)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isPremiumChip) Text("👑", fontSize = 13.sp)
                            Text(
                                category,
                                color = when {
                                    isSelected && isPremiumChip -> L_NavyDeep
                                    isSelected                  -> tk.goldPrime
                                    isPremiumChip               -> L_GoldPrime
                                    else                        -> tk.textDark
                                },
                                fontSize   = res.bodyTextSize.sp,
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
            Row(
                Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Featured Properties",
                        fontWeight = FontWeight.Bold,
                        fontSize   = res.sectionTitleSize.sp,
                        color      = tk.textDark
                    )
                    Text(
                        "Handpicked for you",
                        fontSize = (res.sectionTitleSize - 6f).sp,
                        color    = tk.textMuted
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    val activeBg = if (isDark) D_BgSecondary else L_NavyPrime
                    val inactBg  = if (isDark) D_BgCard else Color(0xFFE0E0E0)
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(if (canScrollBack) activeBg else inactBg)
                            .clickable {
                                scope.launch {
                                    featScrollState.animateScrollTo((featScrollState.value - 550).coerceAtLeast(0))
                                }
                            },
                        Alignment.Center
                    ) {
                        Text("‹", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(if (canScrollForward) activeBg else inactBg)
                            .clickable {
                                scope.launch {
                                    featScrollState.animateScrollTo((featScrollState.value + 550).coerceAtMost(featScrollState.maxValue))
                                }
                            },
                        Alignment.Center
                    ) {
                        Text("›", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            when {
                uiState.isLoading -> LoadingShimmer(isDark)
                uiState.errorMessage != null && allProperties.isEmpty() -> Text(
                    "Error: ${uiState.errorMessage}",
                    Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.error
                )
                filteredFeatured.isEmpty() -> Box(
                    Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tk.cardBg)
                        .padding(28.dp),
                    Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (selectedCategory == "Premium") "👑" else "🏠", fontSize = 34.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (selectedCategory) {
                                "All"     -> "No featured properties"
                                "Premium" -> "No premium properties available"
                                else      -> "No featured $selectedCategory properties"
                            },
                            color    = tk.textMuted,
                            fontSize = 13.sp
                        )
                        if (selectedCategory != "All") {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Check Nearby Properties below",
                                color      = tk.goldPrime,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                else -> Row(
                    Modifier.fillMaxWidth()
                        .horizontalScroll(featScrollState)
                        .padding(horizontal = res.horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredFeatured.take(7).forEach { property ->
                        FeaturedPropertyCard(
                            property    = property,
                            isFavourite = uiState.favouriteIds.contains(property.propertyId),
                            onFavToggle = { viewModel.toggleFavourite(property.propertyId) },
                            isDark      = isDark,
                            cardWidth   = res.featCardWidth,
                            imgHeight   = res.featCardImgHeight,
                            onClick     = {
                                navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(res.featCardWidth * 0.62f)
                            .height(res.featCardImgHeight + 107.dp)
                            .shadow(6.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(tk.cardBg)
                            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
                            .clickable { navController.navigate(Screen.PropertyList.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(16.dp)
                        ) {
                            Box(
                                Modifier.size(52.dp).clip(CircleShape)
                                    .background(tk.goldFaint)
                                    .border(1.5.dp, tk.goldBorder, CircleShape),
                                Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    null,
                                    tint     = tk.goldPrime,
                                    modifier = Modifier.size(22.dp)
                                )
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding)
                    .shadow(14.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(tk.navyLinear)
                    .border(2.dp, tk.goldBorder, RoundedCornerShape(26.dp))
            ) {
                Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(tk.goldBorder))
                Box(
                    Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-50).dp)
                        .clip(CircleShape).background(tk.goldPrime.copy(0.07f))
                )
                Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(tk.goldBorder))

                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(tk.goldPrime.copy(0.28f), tk.goldPrime.copy(0.05f)))),
                                Alignment.Center
                            ) {
                                Text("🏔️", fontSize = 28.sp)
                            }
                            Column {
                                Box(
                                    Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(tk.goldPrime.copy(0.18f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "VACATION HUB",
                                        color         = tk.goldPrime,
                                        fontSize      = 8.sp,
                                        fontWeight    = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Explore Northern Stays",
                                    color      = Color.White,
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Box(
                            Modifier.size(52.dp).clip(CircleShape)
                                .background(Color.Transparent)
                                .border(2.dp, tk.goldBorder, CircleShape)
                                .clickable { navController.navigate(Screen.VacationRentals.route) },
                            Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                null,
                                tint     = tk.goldPrime,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Brush.horizontalGradient(listOf(tk.goldPrime.copy(0.45f), Color.Transparent)))
                    )
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
            Row(
                Modifier.fillMaxWidth().padding(horizontal = res.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Nearby Stays",
                        fontWeight = FontWeight.Bold,
                        fontSize   = res.sectionTitleSize.sp,
                        color      = tk.textDark
                    )
                    Text(
                        "${filteredNearby.size} properties near you",
                        fontSize = (res.sectionTitleSize - 6f).sp,
                        color    = tk.textMuted
                    )
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
                        Text(
                            "Explore Map",
                            fontSize   = 12.sp,
                            color      = tk.goldPrime,
                            fontWeight = FontWeight.Bold
                        )
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
                        Text(
                            when (selectedCategory) {
                                "Premium" -> "No premium stays nearby"
                                "All"     -> "No nearby properties found"
                                else      -> "No $selectedCategory stays nearby"
                            },
                            color     = tk.textMuted,
                            fontSize  = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> items(filteredNearby) { property ->
                NearbyPropertyCard(
                    property    = property,
                    isFavourite = uiState.favouriteIds.contains(property.propertyId),
                    onFavToggle = { viewModel.toggleFavourite(property.propertyId) },
                    isDark      = isDark,
                    hPadding    = res.horizontalPadding,
                    imgSize     = res.nearbyImgSize,
                    onClick     = {
                        navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                    }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// =============================================================================
// HOME HEADER — Tenant
// =============================================================================
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
    val tk        = resolveTokens(isDark)
    val res       = rememberResponsive()
    val searchBg  = if (isDark) D_BgCard else L_CardBg
    val searchBtn = if (isDark) D_BgSecondary else L_NavyPrime

    Box(modifier = Modifier.fillMaxWidth().background(tk.navyGradient)) {
        Box(Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 55.dp, y = (-55).dp).clip(CircleShape).background(tk.goldPrime.copy(0.06f)))
        Box(Modifier.size(80.dp).align(Alignment.BottomStart).offset(x = (-20).dp, y = 20.dp).clip(CircleShape).background(tk.goldPrime.copy(0.04f)))
        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter).background(tk.goldBorder))

        Column(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = res.horizontalPadding)
                .padding(top = 18.dp, bottom = 26.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Welcome Back 👋",
                        color         = tk.goldPrime,
                        fontSize      = res.headerSubSize.sp,
                        fontWeight    = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text("Find Your Haven", color = Color.White, fontSize = res.headerTitleSize.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Pakistan's trusted rental platform", color = Color.White.copy(0.55f), fontSize = res.headerSubSize.sp)
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
                        avatarSize = res.avatarSize,
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
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = tk.goldPrime, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Search city, property type...", color = tk.textMuted, fontSize = 14.sp)
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(searchBtn)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text("Search", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =============================================================================
// FEATURED PROPERTY CARD — Responsive
// =============================================================================
@Composable
fun FeaturedPropertyCard(
    property   : Property,
    isFavourite: Boolean    = false,
    onFavToggle: () -> Unit = {},
    isDark     : Boolean    = false,
    cardWidth  : Dp         = 265.dp,
    imgHeight  : Dp         = 158.dp,
    onClick    : () -> Unit
) {
    val tk = resolveTokens(isDark)

    Box(
        modifier = Modifier.width(cardWidth).wrapContentHeight()
            .shadow(if (isDark) 0.dp else 10.dp, RoundedCornerShape(22.dp), ambientColor = L_GoldPrime.copy(0.2f), spotColor = L_GoldPrime.copy(0.25f))
            .clip(RoundedCornerShape(22.dp))
            .background(tk.cardBg)
            .border(2.dp, tk.goldBorder, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(imgHeight)) {
                PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.5f)))))
                if (property.isPremium) {
                    Box(
                        Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(L_GoldDim, L_GoldPrime, L_GoldLight, L_GoldPrime, L_GoldDim)))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("👑", fontSize = 9.sp)
                            Text("PREMIUM", color = L_NavyDeep, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }
                Box(
                    Modifier.align(Alignment.TopEnd).padding(10.dp)
                        .size(34.dp).clip(CircleShape)
                        .background(Color.Black.copy(0.35f))
                        .clickable { onFavToggle() },
                    Alignment.Center
                ) {
                    Icon(
                        if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favourite",
                        tint     = if (isFavourite) Color(0xFFE53935) else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Box(
                    Modifier.align(Alignment.TopStart).padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tk.goldLinear)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        property.formattedPrice,
                        color      = if (isDark) D_BgDeep else L_NavyDeep,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    Modifier.align(Alignment.BottomStart).padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) D_BgSecondary.copy(0.9f) else L_NavyPrime.copy(0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        property.propertyTypeEnum.displayName(),
                        color      = tk.goldPrime,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (property.isAvailable) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(tk.greenOk)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(tk.goldFaint)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
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

// =============================================================================
// NEARBY PROPERTY CARD — Responsive
// =============================================================================
@Composable
fun NearbyPropertyCard(
    property   : Property,
    isFavourite: Boolean    = false,
    onFavToggle: () -> Unit = {},
    isDark     : Boolean    = false,
    hPadding   : Dp         = 20.dp,
    imgSize    : Dp         = 104.dp,
    onClick    : () -> Unit
) {
    val tk = resolveTokens(isDark)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = hPadding, vertical = 6.dp)
            .shadow(if (isDark) 0.dp else 8.dp, RoundedCornerShape(20.dp), ambientColor = L_GoldPrime.copy(0.15f), spotColor = L_GoldPrime.copy(0.2f))
            .clip(RoundedCornerShape(20.dp))
            .background(tk.cardBg)
            .border(2.dp, tk.goldBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(imgSize).clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, tk.goldBorder, RoundedCornerShape(16.dp))
            ) {
                PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                if (property.isAvailable) {
                    Box(Modifier.align(Alignment.TopStart).padding(7.dp).size(8.dp).clip(CircleShape).background(tk.greenOk))
                }
                if (property.isPremium) {
                    Box(
                        Modifier.align(Alignment.BottomStart).padding(5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Brush.horizontalGradient(listOf(L_GoldDim, L_GoldPrime)))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("👑", fontSize = 9.sp)
                    }
                }
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .size(28.dp).clip(CircleShape)
                        .background(Color.Black.copy(0.3f))
                        .clickable { onFavToggle() },
                    Alignment.Center
                ) {
                    Icon(
                        if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favourite",
                        tint     = if (isFavourite) Color(0xFFE53935) else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Text(
                        property.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = tk.textDark,
                        modifier   = Modifier.weight(1f)
                    )
                    Box(
                        Modifier.clip(RoundedCornerShape(5.dp))
                            .background(if (isDark) D_BgSecondary else L_NavyPrime.copy(0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            property.propertyTypeEnum.displayName(),
                            fontSize   = 10.sp,
                            color      = if (isDark) D_TextSecondary else L_NavyPrime,
                            fontWeight = FontWeight.Medium
                        )
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "${property.formattedPrice}/night",
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (isDark) D_GoldPrimary else L_NavyDeep,
                        fontSize   = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(tk.goldFaint)
                            .border(1.dp, tk.goldBorder, RoundedCornerShape(8.dp))
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

// =============================================================================
// VACATION MINI STAT
// =============================================================================
@Composable
private fun VacationMiniStat(value: String, label: String, emoji: String, goldColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = goldColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(0.45f), fontSize = 9.sp)
    }
}

// =============================================================================
// LOADING SHIMMER
// =============================================================================
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