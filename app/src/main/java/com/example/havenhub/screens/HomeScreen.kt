package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.HomeViewModel
import com.example.havenhub.viewmodel.HomeUiState
import com.example.havenhub.viewmodel.VacationViewModel
import kotlinx.coroutines.launch

// ── Design Tokens ────────────────────────────────────────────────
private val NavyDeep   = Color(0xFF071020)
private val NavyPrime  = Color(0xFF0D1B3E)
private val NavyMid    = Color(0xFF1A3A6B)
private val NavyLight  = Color(0xFF243F6E)
private val GoldPrime  = Color(0xFFD4AF37)
private val GoldLight  = Color(0xFFF5D060)
private val GoldFaint  = Color(0xFFFFF8E1)
private val GoldDim    = Color(0xFFB8962E)
private val PageBg     = Color(0xFFF0F4FA)
private val CardWhite  = Color(0xFFFFFFFF)
private val CardBg     = Color(0xFFFFFFFF)
private val TextMuted  = Color(0xFF8899AA)
private val TextDark   = Color(0xFF1A2744)
private val GreenOk    = Color(0xFF4CAF50)
private val GreenBg    = Color(0xFFE8F8F0)
private val OrangePend = Color(0xFFFF9800)
private val OrangeBg   = Color(0xFFFFF0EB)
private val DividerCol = Color(0xFFE8ECF4)

// ── Shared Brushes ───────────────────────────────────────────────
private val GoldBorderBrush = Brush.horizontalGradient(
    listOf(GoldPrime.copy(0.9f), GoldLight.copy(0.5f), GoldPrime.copy(0.9f))
)
private val NavyLinear  = Brush.linearGradient(listOf(NavyDeep, NavyMid))
private val GoldLinear  = Brush.linearGradient(listOf(GoldPrime, GoldLight))
private val GoldGradient = Brush.linearGradient(listOf(GoldPrime, GoldLight, GoldPrime))
private val NavyGradient = Brush.verticalGradient(listOf(NavyDeep, NavyMid))

// ═══════════════════════════════════════════════════════════════════
// MAIN ENTRY POINT
// ═══════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(
    navController    : NavController,
    viewModel        : HomeViewModel     = hiltViewModel(),
    authViewModel    : AuthViewModel     = hiltViewModel(),
    vacationViewModel: VacationViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val authState  by authViewModel.uiState.collectAsState()
    val vacUiState by vacationViewModel.uiState.collectAsState()

    val userRole = authState.userRole.lowercase().trim()
    val userId   = authState.currentUser?.uid ?: ""

    LaunchedEffect(userId, userRole) {
        when {
            userRole == "landlord" && userId.isNotEmpty() -> viewModel.loadLandlordStats(userId)
            else -> {
                viewModel.loadHomeData()
                vacationViewModel.loadVacationProperties()
            }
        }
    }

    when (userRole) {
        "landlord" -> LandlordHomeScreen(navController = navController, uiState = uiState)
        else       -> TenantHomeScreen(
            navController     = navController,
            viewModel         = viewModel,
            uiState           = uiState,
            vacationCount     = vacUiState.properties.size,
            isVacationLoading = vacUiState.isLoading
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// IMAGE HELPER
// ═══════════════════════════════════════════════════════════════════
private fun resolveDrawable(property: Property): Int {
    if (property.drawableImageName.isNotEmpty())    return getPropertyImage(property.drawableImageName)
    if (property.resolvedDrawableName.isNotEmpty()) return getPropertyImage(property.resolvedDrawableName)
    return getPropertyImage(property.propertyId)
}

@Composable
private fun PropertyImage(
    property    : Property,
    modifier    : Modifier = Modifier,
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

// ═══════════════════════════════════════════════════════════════════
// LANDLORD HOME SCREEN — Enhanced (from your version)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun LandlordHomeScreen(
    navController: NavController,
    uiState      : HomeUiState
) {
    val formattedRevenue = remember(uiState.totalRevenue) {
        when {
            uiState.totalRevenue >= 1_000_000 -> "PKR %.1fM".format(uiState.totalRevenue / 1_000_000)
            uiState.totalRevenue >= 1_000     -> "PKR %.0fK".format(uiState.totalRevenue / 1_000)
            else                              -> "PKR %.0f".format(uiState.totalRevenue)
        }
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(PageBg),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {

        // ── 1. HEADER ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier.fillMaxWidth().background(NavyGradient)
            ) {
                Box(Modifier.size(200.dp).align(Alignment.TopEnd).offset(x = 70.dp, y = (-70).dp).clip(CircleShape).background(GoldPrime.copy(alpha = 0.06f)))
                Box(Modifier.size(100.dp).align(Alignment.BottomStart).offset(x = (-30).dp, y = 40.dp).clip(CircleShape).background(GoldPrime.copy(alpha = 0.04f)))
                Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(GoldBorderBrush))

                Column(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 24.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Welcome Back 👋", color = GoldPrime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Manage Your Haven", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Track listings & booking requests", color = Color.White.copy(0.55f), fontSize = 12.sp)
                        }
                        // Notification Bell with badge
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clickable { navController.navigate(Screen.Notifications.route) }
                                .clip(CircleShape)
                                .background(GoldPrime.copy(0.12f))
                                .border(1.5.dp, GoldPrime.copy(0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = GoldPrime, modifier = Modifier.size(24.dp))
                            if (uiState.pendingRequestsCount > 0) {
                                Box(
                                    modifier = Modifier.size(14.dp).align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp).clip(CircleShape).background(OrangePend),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${uiState.pendingRequestsCount}", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // ── STATS STRIP — 3 cards ──────────────────────
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LandlordStatCard(
                            icon       = Icons.Default.People,
                            iconTint   = Color(0xFF5BC8FF),
                            bgColor    = Color(0xFF5BC8FF).copy(0.12f),
                            label      = "Active Tenants",
                            value      = "${uiState.activeTenantsCount}",
                            valueColor = Color.White,
                            modifier   = Modifier.weight(1f),
                            isWorking  = true,
                            onClick    = { navController.navigate(Screen.MyBookings.createRoute(tab = 2)) }
                        )
                        LandlordStatCard(
                            icon      = Icons.Default.Star,
                            iconTint  = GoldPrime,
                            bgColor   = GoldPrime.copy(0.12f),
                            label     = "Avg Rating",
                            value     = if (uiState.averageRating > 0f) "%.1f".format(uiState.averageRating) else "—",
                            valueColor = GoldLight,
                            modifier  = Modifier.weight(1f),
                            isWorking = true
                        )
                        LandlordStatCard(
                            icon       = Icons.Default.Notifications,
                            iconTint   = if (uiState.pendingRequestsCount > 0) OrangePend else TextMuted,
                            bgColor    = if (uiState.pendingRequestsCount > 0) OrangePend.copy(0.18f) else Color.White.copy(0.07f),
                            label      = "Pending Req",
                            value      = "${uiState.pendingRequestsCount}",
                            valueColor = if (uiState.pendingRequestsCount > 0) OrangePend.copy(pulseAlpha) else Color.White,
                            modifier   = Modifier.weight(1f),
                            isWorking  = true,
                            highlight  = uiState.pendingRequestsCount > 0,
                            onClick    = { navController.navigate(Screen.MyBookings.createRoute(tab = 0)) }
                        )
                    }
                }
            }
        }

        // ── 2. REVENUE CARD ────────────────────────────────────────
        item {
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(12.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(NavyLinear)
                    .border(1.5.dp, GoldBorderBrush, RoundedCornerShape(22.dp))
            ) {
                Box(Modifier.size(130.dp).align(Alignment.CenterEnd).offset(x = 40.dp).clip(CircleShape).background(GoldPrime.copy(0.08f)))
                Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(GoldBorderBrush))

                Row(
                    modifier              = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL REVENUE", color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(formattedRevenue, color = GoldPrime, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(GreenOk.copy(0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GreenOk, modifier = Modifier.size(10.dp))
                            }
                            Spacer(Modifier.width(5.dp))
                            Text("Updated in real-time", color = GreenOk.copy(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(GoldGradient)
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .clickable { navController.navigate(Screen.PaymentReports.route) }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Earnings", tint = NavyDeep, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(5.dp))
                            Text("Earnings", color = NavyDeep, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // ── 3. QUICK ACTIONS ───────────────────────────────────────
        item {
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Quick Actions", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NavyDeep)
                    Text("Manage everything fast", fontSize = 12.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Add New Property
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(GoldLinear)
                        .clickable { navController.navigate(Screen.AddProperty.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(NavyDeep.copy(0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Add New Property", color = NavyDeep, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("List your property for rent", color = NavyDeep.copy(0.65f), fontSize = 11.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = NavyDeep.copy(0.5f), modifier = Modifier.size(14.dp))
                    }
                }

                // Active Bookings + My Properties row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f).height(110.dp)
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(NavyLinear)
                            .border(1.5.dp, GoldBorderBrush, RoundedCornerShape(18.dp))
                            .clickable { navController.navigate(Screen.MyBookings.route) }
                    ) {
                        Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(GoldBorderBrush))
                        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldPrime, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("${uiState.activeBookingsCount}", color = GoldPrime, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                            Text("Active Bookings", color = Color.White.copy(0.65f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f).height(110.dp)
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(GoldFaint)
                            .border(1.5.dp, GoldPrime.copy(0.35f), RoundedCornerShape(18.dp))
                            .clickable { navController.navigate(Screen.MyProperties.route) }
                    ) {
                        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("${uiState.totalProperties}", color = NavyDeep, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                            Text("My Properties", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // ── 4. MY PROPERTIES LIST ──────────────────────────────────
        item {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("My Properties", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NavyDeep)
                    Text("${uiState.featuredProperties.size} listed properties", fontSize = 12.sp, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldFaint)
                        .border(1.dp, GoldPrime.copy(0.5f), RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Screen.MyProperties.route) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("See All", fontSize = 12.sp, color = GoldDim, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = GoldDim, modifier = Modifier.size(10.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            uiState.isLoading -> item { LoadingShimmer() }
            uiState.featuredProperties.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().padding(horizontal = 20.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(NavyLinear)
                        .border(1.5.dp, GoldBorderBrush, RoundedCornerShape(20.dp))
                        .clickable { navController.navigate(Screen.AddProperty.route) }
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(GoldPrime.copy(0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddHome, contentDescription = null, tint = GoldPrime, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("No properties yet", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to add your first property", color = GoldPrime, fontSize = 13.sp)
                    }
                }
            }
            else -> items(uiState.featuredProperties.take(3)) { property ->
                PremiumLandlordPropertyCard(property) {
                    navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════
// LANDLORD STAT CARD — Enhanced (from your version)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun LandlordStatCard(
    icon      : ImageVector,
    iconTint  : Color,
    bgColor   : Color,
    label     : String,
    value     : String,
    valueColor: Color,
    modifier  : Modifier   = Modifier,
    isWorking : Boolean    = true,
    highlight : Boolean    = false,
    onClick   : () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlight) OrangePend.copy(0.15f) else Color.White.copy(0.09f))
            .border(
                width = if (highlight) 1.5.dp else 1.dp,
                brush = if (highlight)
                    Brush.linearGradient(listOf(OrangePend, OrangePend.copy(0.5f)))
                else
                    Brush.linearGradient(listOf(GoldPrime.copy(0.5f), GoldLight.copy(0.2f))),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.White.copy(0.6f), fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// LANDLORD PROPERTY CARD — Enhanced (from your version)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun PremiumLandlordPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .border(1.dp, GoldPrime.copy(0.18f), RoundedCornerShape(18.dp)),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    Modifier.width(4.dp).height(88.dp).align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (property.isAvailable)
                                Brush.verticalGradient(listOf(GreenOk, GreenOk.copy(0.5f)))
                            else
                                Brush.verticalGradient(listOf(TextMuted, TextMuted.copy(0.5f)))
                        )
                )
                Box(Modifier.padding(start = 8.dp).size(88.dp).clip(RoundedCornerShape(14.dp))) {
                    PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                    Box(
                        Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (property.isAvailable) GreenOk else OrangePend)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(if (property.isAvailable) "Active" else "Inactive", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(property.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextDark)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = GoldPrime, modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = TextMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KingBed, null, tint = TextMuted.copy(0.7f), modifier = Modifier.size(12.dp))
                    Text(" ${property.bedrooms} beds", fontSize = 11.sp, color = TextMuted)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.People, null, tint = TextMuted.copy(0.7f), modifier = Modifier.size(12.dp))
                    Text(" ${property.maxGuests} guests", fontSize = 11.sp, color = TextMuted)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Price/month", fontSize = 9.sp, color = TextMuted)
                        Text(property.formattedPrice, fontWeight = FontWeight.ExtraBold, color = NavyDeep, fontSize = 14.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clip(RoundedCornerShape(10.dp)).background(GoldFaint).border(1.dp, GoldPrime.copy(0.3f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = GoldPrime, modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = NavyDeep, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// TENANT HOME SCREEN — from pulled code (with VacationViewModel)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun TenantHomeScreen(
    navController     : NavController,
    viewModel         : HomeViewModel,
    uiState           : HomeUiState,
    vacationCount     : Int     = 0,
    isVacationLoading : Boolean = false
) {
    val categories          = listOf("All", "House", "Apartment", "Room", "Villa", "Studio")
    var selectedCategory    by remember { mutableStateOf("All") }
    val featuredScrollState = rememberScrollState()
    val scope               = rememberCoroutineScope()

    val allProperties = uiState.allProperties.ifEmpty {
        (uiState.featuredProperties + uiState.nearbyProperties).distinctBy { it.propertyId }
    }
    val filteredFeatured = if (selectedCategory == "All") uiState.featuredProperties
    else uiState.featuredProperties.filter { it.propertyType.equals(selectedCategory, ignoreCase = true) }
    val filteredNearby = if (selectedCategory == "All") uiState.nearbyProperties
    else uiState.nearbyProperties.filter { it.propertyType.equals(selectedCategory, ignoreCase = true) }

    val vacCountLabel = when {
        isVacationLoading -> "..."
        vacationCount > 0 -> "$vacationCount"
        else              -> "0"
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(PageBg), contentPadding = PaddingValues(bottom = 80.dp)) {

        item {
            HomeHeaderSection(
                onSearchClick       = { navController.navigate(Screen.Search.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onProfileClick      = { navController.navigate(Screen.Profile.route) }
            )
        }

        // ── Category Filter ──────────────────────────────────────
        item {
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Browse by Type", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyPrime); Text("Find your perfect stay", fontSize = 12.sp, color = TextMuted) }
                Text("See All", fontSize = 13.sp, color = GoldPrime, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navController.navigate(Screen.PropertyList.route) })
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { category ->
                    val isSelected   = selectedCategory == category
                    val categoryIcon = when (category) { "House" -> "🏡"; "Apartment" -> "🏢"; "Room" -> "🛏"; "Villa" -> "🏰"; "Studio" -> "🏠"; else -> "🔍" }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSelected) NavyPrime else CardWhite).clickable { selectedCategory = category }.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(categoryIcon, fontSize = 20.sp); Spacer(Modifier.height(4.dp))
                        Text(category, color = if (isSelected) GoldPrime else NavyPrime, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }

        // ── Featured Properties ──────────────────────────────────
        item {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Featured Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyPrime); Text("Handpicked for you", fontSize = 12.sp, color = TextMuted) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(if (featuredScrollState.value > 0) NavyPrime else Color(0xFFE0E0E0)).clickable { scope.launch { featuredScrollState.animateScrollTo((featuredScrollState.value - 550).coerceAtLeast(0)) } }, contentAlignment = Alignment.Center) { Text("‹", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    Box(Modifier.size(32.dp).clip(CircleShape).background(if (featuredScrollState.value < featuredScrollState.maxValue) NavyPrime else Color(0xFFE0E0E0)).clickable { scope.launch { featuredScrollState.animateScrollTo((featuredScrollState.value + 550).coerceAtMost(featuredScrollState.maxValue)) } }, contentAlignment = Alignment.Center) { Text("›", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(14.dp))
            when {
                uiState.isLoading -> LoadingShimmer()
                uiState.errorMessage != null && allProperties.isEmpty() -> Text("Error: ${uiState.errorMessage}", Modifier.padding(20.dp), color = Color.Red)
                filteredFeatured.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(14.dp)).background(CardWhite).padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏠", fontSize = 32.sp); Spacer(Modifier.height(8.dp))
                            Text(if (selectedCategory == "All") "No featured properties" else "No featured $selectedCategory properties", color = TextMuted, fontSize = 13.sp)
                            if (selectedCategory != "All") Text("Check Nearby Properties below", color = GoldPrime, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                else -> Row(Modifier.fillMaxWidth().horizontalScroll(featuredScrollState).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    filteredFeatured.take(7).forEach { property ->
                        FeaturedPropertyCard(property = property, isFavourite = uiState.favouriteIds.contains(property.propertyId), onFavToggle = { viewModel.toggleFavourite(property.propertyId) }, onClick = { navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId)) })
                    }
                    Card(Modifier.width(160.dp).height(260.dp).clickable { navController.navigate(Screen.PropertyList.route) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(6.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                Text("→", fontSize = 28.sp, color = GoldPrime); Spacer(Modifier.height(8.dp))
                                Text("See all", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyPrime)
                                Text("${allProperties.size} properties", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        // ── VACATION BANNER ──────────────────────────────────────
        item {
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(NavyDeep, NavyPrime, NavyMid)))
            ) {
                Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(GoldBorderBrush))
                Box(Modifier.size(150.dp).align(Alignment.TopEnd).offset(x = 45.dp, y = (-45).dp).clip(CircleShape).background(GoldPrime.copy(0.07f)))
                Box(Modifier.size(90.dp).align(Alignment.BottomEnd).offset(x = 25.dp, y = 25.dp).clip(CircleShape).background(GoldPrime.copy(0.05f)))

                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier.size(54.dp).clip(CircleShape).background(Brush.radialGradient(listOf(GoldPrime.copy(0.28f), GoldPrime.copy(0.05f)))),
                                contentAlignment = Alignment.Center
                            ) { Text("🏔️", fontSize = 26.sp) }
                            Column {
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(GoldPrime.copy(0.18f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("VACATION HUB", color = GoldPrime, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Explore Northern Stays", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Box(
                            modifier = Modifier.size(52.dp).aspectRatio(1f).clip(CircleShape).background(Color.Transparent).border(1.5.dp, GoldPrime, CircleShape).clickable { navController.navigate(Screen.VacationRentals.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = GoldPrime, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(GoldPrime.copy(0.5f), Color.Transparent))))
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        VacationMiniStat(vacCountLabel, "Stays",   "🏠")
                        VacationMiniStat("PT-1",        "Verified","✅")
                        VacationMiniStat("4.8★",        "Rating",  "⭐")
                        VacationMiniStat("Secure",      "Booking", "🔒")
                    }
                }
            }
        }

        // ── Nearby Stays ─────────────────────────────────────────
        item {
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Nearby Stays", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyPrime); Text("${filteredNearby.size} properties near you", fontSize = 12.sp, color = TextMuted) }
                Text("See All", fontSize = 13.sp, color = GoldPrime, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navController.navigate(Screen.PropertyList.route) })
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            uiState.isLoading -> item { LoadingShimmer() }
            filteredNearby.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 40.sp); Spacer(Modifier.height(8.dp))
                        Text("No ${if (selectedCategory == "All") "" else "$selectedCategory "}nearby properties found", color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            else -> items(filteredNearby) { property ->
                NearbyPropertyCard(property = property, isFavourite = uiState.favouriteIds.contains(property.propertyId), onFavToggle = { viewModel.toggleFavourite(property.propertyId) }, onClick = { navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId)) })
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════
// HOME HEADER — from pulled code (statusBarsPadding fix)
// ═══════════════════════════════════════════════════════════════════
@Composable
fun HomeHeaderSection(
    onSearchClick      : () -> Unit,
    onNotificationClick: () -> Unit = {},
    onProfileClick     : () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(NavyDeep, NavyPrime, NavyMid)))) {
        Box(Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-50).dp).clip(CircleShape).background(GoldPrime.copy(0.06f)))
        Box(Modifier.size(80.dp).align(Alignment.BottomStart).offset(x = (-20).dp, y = 20.dp).clip(CircleShape).background(GoldPrime.copy(0.04f)))
        Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(GoldBorderBrush))

        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Welcome Back 👋", color = GoldPrime, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(5.dp))
                    Text("Find Your Haven", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Pakistan's trusted rental platform", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { onNotificationClick() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Notifications, null, tint = GoldPrime, modifier = Modifier.size(22.dp))
                    }
                    Box(Modifier.size(42.dp).clip(CircleShape).background(GoldPrime.copy(0.2f)).border(1.5.dp, GoldPrime, CircleShape).clickable { onProfileClick() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = GoldPrime, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(CardWhite).clickable { onSearchClick() }.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = GoldPrime, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                        Text("Search city, property type...", color = TextMuted, fontSize = 14.sp)
                    }
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(NavyPrime).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Search", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// FEATURED PROPERTY CARD — from pulled code
// ═══════════════════════════════════════════════════════════════════
@Composable
fun FeaturedPropertyCard(property: Property, isFavourite: Boolean = false, onFavToggle: () -> Unit = {}, onClick: () -> Unit) {
    Card(Modifier.width(260.dp).wrapContentHeight().clickable { onClick() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(6.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(155.dp)) {
                PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.45f)))))
                Box(Modifier.align(Alignment.TopEnd).padding(9.dp).size(32.dp).clip(CircleShape).background(Color.Black.copy(0.35f)).clickable { onFavToggle() }, contentAlignment = Alignment.Center) {
                    Icon(if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite", tint = if (isFavourite) Color(0xFFE53935) else Color.White, modifier = Modifier.size(17.dp))
                }
                Box(Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(GoldLinear).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(property.formattedPrice, color = NavyPrime, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(NavyPrime.copy(0.85f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(property.propertyTypeEnum.displayName(), color = GoldPrime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (property.isAvailable) {
                    Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(GreenOk).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Text("Available", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = NavyPrime)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = GoldPrime, modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GoldFaint).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Icon(Icons.Default.Star, null, tint = GoldPrime, modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = NavyPrime, fontWeight = FontWeight.SemiBold)
                        Text(" (${property.reviewCount})", fontSize = 11.sp, color = TextMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.KingBed, null, tint = TextMuted, modifier = Modifier.size(12.dp)); Text(" ${property.bedrooms}", fontSize = 11.sp, color = TextMuted)
                        Spacer(Modifier.width(6.dp)); Icon(Icons.Default.People, null, tint = TextMuted, modifier = Modifier.size(12.dp)); Text(" ${property.maxGuests}", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// NEARBY PROPERTY CARD — from pulled code
// ═══════════════════════════════════════════════════════════════════
@Composable
fun NearbyPropertyCard(property: Property, isFavourite: Boolean = false, onFavToggle: () -> Unit = {}, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable { onClick() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(100.dp).clip(RoundedCornerShape(14.dp))) {
                PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                if (property.isAvailable) Box(Modifier.align(Alignment.TopStart).padding(6.dp).size(8.dp).clip(CircleShape).background(GreenOk))
                Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(26.dp).clip(CircleShape).background(Color.Black.copy(0.3f)).clickable { onFavToggle() }, contentAlignment = Alignment.Center) {
                    Icon(if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite", tint = if (isFavourite) Color(0xFFE53935) else Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(property.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = NavyPrime, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(NavyPrime.copy(0.08f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(property.propertyTypeEnum.displayName(), fontSize = 10.sp, color = NavyPrime, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = GoldPrime, modifier = Modifier.size(12.dp)); Text(" ${property.city}", color = TextMuted, fontSize = 12.sp) }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KingBed, null, tint = TextMuted, modifier = Modifier.size(12.dp)); Text(" ${property.bedrooms} beds", fontSize = 11.sp, color = TextMuted)
                    Spacer(Modifier.width(8.dp)); Icon(Icons.Default.People, null, tint = TextMuted, modifier = Modifier.size(12.dp)); Text(" ${property.maxGuests} guests", fontSize = 11.sp, color = TextMuted)
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${property.formattedPrice}/night", fontWeight = FontWeight.ExtraBold, color = NavyPrime, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GoldFaint).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Star, null, tint = GoldPrime, modifier = Modifier.size(12.dp)); Text(" ${property.averageRating}", fontSize = 12.sp, color = NavyPrime, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SHARED COMPOSABLES
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun VacationMiniStat(value: String, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 14.sp); Spacer(Modifier.height(2.dp))
        Text(value, color = GoldPrime, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(0.45f), fontSize = 9.sp)
    }
}

@Composable
fun LoadingShimmer() {
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = GoldPrime)
    }
}

