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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
private val NavyDeep  = Color(0xFF071020)
private val NavyPrime = Color(0xFF0D1B3E)
private val NavyMid   = Color(0xFF1A3A6B)
private val GoldPrime = Color(0xFFD4AF37)
private val GoldLight = Color(0xFFF5D060)
private val GoldFaint = Color(0xFFFFF8E1)
private val PageBg    = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFF8899AA)
private val GreenOk   = Color(0xFF4CAF50)
private val CardWhite = Color(0xFFFFFFFF)
private val OrangePend = Color(0xFFFF9800)

// ── Shared Brushes ───────────────────────────────────────────────
private val GoldBorderBrush = Brush.horizontalGradient(
    listOf(Color.Transparent, GoldPrime, GoldLight, GoldPrime, Color.Transparent)
)
private val NavyLinear = Brush.linearGradient(listOf(NavyDeep, NavyMid))
private val GoldLinear = Brush.linearGradient(listOf(GoldPrime, GoldLight))
private val GoldGradient = Brush.linearGradient(listOf(GoldPrime, GoldLight))
private val GoldDim = Color(0xFFB8962E)

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
            model = remoteUrl, contentDescription = property.title,
            modifier = modifier, contentScale = contentScale
        )
    } else {
        Image(
            painter = painterResource(id = resolveDrawable(property)),
            contentDescription = property.title,
            modifier = modifier, contentScale = contentScale
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// LANDLORD HOME SCREEN
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
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseAlpha"
    )

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(PageBg),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {

        // ── HEADER ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(NavyDeep, NavyPrime, NavyMid)))
            ) {
                Box(Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 55.dp, y = (-55).dp).clip(CircleShape).background(GoldPrime.copy(0.06f)))
                Box(Modifier.size(90.dp).align(Alignment.BottomStart).offset(x = (-25).dp, y = 35.dp).clip(CircleShape).background(GoldPrime.copy(0.05f)))
                Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(GoldBorderBrush))

                Column(
                    Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 24.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Welcome Back 👋", color = GoldPrime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Manage Your Haven", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Track listings & booking requests", color = Color.White.copy(0.5f), fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(0.13f)).clickable { navController.navigate(Screen.Notifications.route) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Notifications, null, tint = GoldPrime, modifier = Modifier.size(22.dp))
                            }
                            Box(Modifier.size(42.dp).clip(CircleShape).background(GoldPrime.copy(0.18f)).border(1.5.dp, GoldPrime, CircleShape).clickable { navController.navigate(Screen.Profile.route) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = GoldPrime, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LandlordStatChip("🏠", "Properties", "${uiState.totalProperties}", Modifier.weight(1f))
                        LandlordStatChip("📋", "Active",     "${uiState.activeBookingsCount}", Modifier.weight(1f))
                        LandlordStatChip("⭐", "Rating", if (uiState.averageRating > 0f) "%.1f".format(uiState.averageRating) else "—", Modifier.weight(1f))
                    }
                }
            }
        }

        // ── REVENUE CARD ─────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp)).background(NavyLinear)
            ) {
                Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(GoldBorderBrush))
                Box(Modifier.size(110.dp).align(Alignment.CenterEnd).offset(x = 35.dp).clip(CircleShape).background(GoldPrime.copy(0.07f)))
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TOTAL REVENUE", color = Color.White.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(formattedRevenue, color = GoldPrime, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(GoldPrime.copy(0.15f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("💰 Earnings", color = GoldPrime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(GoldPrime.copy(0.5f), Color.Transparent))))
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        RevenueMiniStat("${uiState.activeBookingsCount}", "Active Bookings", "📅")
                        Box(Modifier.width(1.dp).height(32.dp).background(Color.White.copy(0.1f)))
                        RevenueMiniStat("${uiState.pendingRequestsCount}", "Pending Req.", "📋")
                        Box(Modifier.width(1.dp).height(32.dp).background(Color.White.copy(0.1f)))
                        RevenueMiniStat(if (uiState.averageRating > 0f) "%.1f ★".format(uiState.averageRating) else "— ★", "Avg Rating", "⭐")
                    }
                }
            }
        }

        // ── QUICK ACTIONS ─────────────────────────────────────────
        item {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Quick Actions", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = NavyPrime)
                    Text("Manage everything fast", fontSize = 11.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(modifier = Modifier.weight(1f).height(100.dp).clickable { navController.navigate(Screen.AddProperty.route) },
                        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(0.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(GoldLinear), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("➕", fontSize = 22.sp); Spacer(Modifier.height(5.dp))
                                Text("Add Property", color = NavyPrime, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                Text("List new rental", color = NavyPrime.copy(0.6f), fontSize = 10.sp)
                            }
                        }
                    }
                    Card(modifier = Modifier.weight(1f).height(100.dp).clickable { navController.navigate(Screen.MyBookings.route) },
                        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(0.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(NavyLinear).clip(RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(GoldBorderBrush))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📅", fontSize = 22.sp); Spacer(Modifier.height(5.dp))
                                Text("${uiState.activeBookingsCount}", color = GoldPrime, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                Text("Active Bookings", color = Color.White.copy(0.6f), fontSize = 10.sp)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(modifier = Modifier.weight(1f).height(100.dp).clickable { navController.navigate(Screen.MyBookings.route) },
                        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(4.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📋", fontSize = 22.sp); Spacer(Modifier.height(5.dp))
                                Text("${uiState.pendingRequestsCount}", color = NavyPrime, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                Text("Pending Req.", color = TextMuted, fontSize = 10.sp)
                            }
                            if (uiState.pendingRequestsCount > 0) {
                                Box(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(GoldPrime.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("NEW", color = GoldPrime, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                    Card(modifier = Modifier.weight(1f).height(100.dp).clickable { navController.navigate(Screen.MyProperties.route) },
                        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = GoldFaint), elevation = CardDefaults.cardElevation(4.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏠", fontSize = 22.sp); Spacer(Modifier.height(5.dp))
                                Text("${uiState.totalProperties}", color = NavyPrime, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                Text("My Properties", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── MY PROPERTIES LIST ────────────────────────────────────
        item {
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("My Properties", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = NavyPrime)
                    Text("Your listed properties", fontSize = 11.sp, color = TextMuted)
                }
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(GoldPrime.copy(0.12f)).clickable { navController.navigate(Screen.MyProperties.route) }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text("See All →", fontSize = 12.sp, color = GoldPrime, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        when {
            uiState.isLoading -> item { LoadingShimmer() }
            uiState.featuredProperties.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(18.dp)).background(NavyLinear).clickable { navController.navigate(Screen.AddProperty.route) }.padding(36.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏠", fontSize = 40.sp); Spacer(Modifier.height(10.dp))
                        Text("No properties yet", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp); Spacer(Modifier.height(4.dp))
                        Text("Tap to add your first property", color = GoldPrime, fontSize = 13.sp)
                    }
                }
            }
            else -> items(uiState.featuredProperties.take(3)) { property ->
                PremiumLandlordPropertyCard(property) { navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId)) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════
// LANDLORD SUPPORTING COMPOSABLES
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun PremiumLandlordPropertyCard(property: Property, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(Modifier.width(3.dp).height(80.dp).align(Alignment.CenterStart).background(Brush.verticalGradient(listOf(GoldPrime, GoldLight))))
                Box(Modifier.padding(start = 6.dp).size(80.dp).clip(RoundedCornerShape(12.dp))) {
                    PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                    if (property.isAvailable) Box(Modifier.align(Alignment.TopStart).padding(5.dp).size(8.dp).clip(CircleShape).background(GreenOk))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(property.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = NavyPrime, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (property.isAvailable) GreenOk.copy(0.12f) else Color(0xFFFFF3E0)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text(if (property.isAvailable) "Active" else "Inactive", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (property.isAvailable) GreenOk else Color(0xFFE65100))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = GoldPrime, modifier = Modifier.size(11.dp)); Text(" ${property.city}", color = TextMuted, fontSize = 11.sp) }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KingBed, null, tint = TextMuted, modifier = Modifier.size(11.dp)); Text(" ${property.bedrooms} beds", fontSize = 10.sp, color = TextMuted)
                    Spacer(Modifier.width(8.dp)); Icon(Icons.Default.People, null, tint = TextMuted, modifier = Modifier.size(11.dp)); Text(" ${property.maxGuests} guests", fontSize = 10.sp, color = TextMuted)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(property.formattedPrice, fontWeight = FontWeight.ExtraBold, color = NavyPrime, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GoldFaint).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Icon(Icons.Default.Star, null, tint = GoldPrime, modifier = Modifier.size(11.dp)); Text(" ${property.averageRating}", fontSize = 11.sp, color = NavyPrime, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LandlordStatChip(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.1f))
        .border(1.dp, Brush.horizontalGradient(listOf(GoldPrime.copy(0.4f), GoldLight.copy(0.2f))), RoundedCornerShape(14.dp))
        .padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 16.sp); Spacer(Modifier.height(3.dp))
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.White.copy(0.6f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun RevenueMiniStat(value: String, label: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 14.sp); Spacer(Modifier.height(2.dp))
        Text(value, color = GoldPrime, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(0.45f), fontSize = 9.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════
// TENANT HOME SCREEN
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSelected) NavyPrime else CardWhite).clickable { selectedCategory = category }.padding(horizontal = 16.dp, vertical = 10.dp)) {
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

                    // Top row: icon+text LEFT, arrow circle RIGHT
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // LEFT: mountain icon + text
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier         = Modifier.size(54.dp).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(GoldPrime.copy(0.28f), GoldPrime.copy(0.05f)))),
                                contentAlignment = Alignment.Center
                            ) { Text("🏔️", fontSize = 26.sp) }

                            Column {
                                Box(
                                    Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(GoldPrime.copy(0.18f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("VACATION HUB", color = GoldPrime, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Explore Northern Stays", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // RIGHT: gold circle arrow
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .border(1.5.dp, GoldPrime, CircleShape)
                                .clickable {
                                    navController.navigate(Screen.VacationRentals.route)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = GoldPrime,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(GoldPrime.copy(0.5f), Color.Transparent))))
                    Spacer(Modifier.height(16.dp))

                    // Stats row
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
// HOME HEADER — statusBarsPadding fix
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
// FEATURED PROPERTY CARD
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
// NEARBY PROPERTY CARD
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