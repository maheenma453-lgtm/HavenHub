package com.example.havenhub.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════
// MAIN ENTRY POINT — AuthViewModel se role check (landlord fix)
// ═══════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel    : HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val userRole  = authState.userRole
    val userId    = authState.currentUser?.uid ?: ""

    LaunchedEffect(userId, userRole) {
        when {
            userRole == "landlord" && userId.isNotEmpty() -> viewModel.loadLandlordStats(userId)
            userId.isNotEmpty()                           -> viewModel.loadHomeData()
            else                                          -> viewModel.loadHomeData()
        }
    }

    when (userRole) {
        "landlord" -> LandlordHomeScreen(navController = navController, uiState = uiState)
        else       -> TenantHomeScreen(navController = navController, viewModel = viewModel, uiState = uiState)
    }
}

// ═══════════════════════════════════════════════════════════════════
// IMAGE HELPER — remote URL ho toh Coil, warna drawable
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
// LANDLORD HOME SCREEN — same as original, unchanged
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

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Landlord Header ──────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF1A3A6B))))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFD4AF37), Color.Transparent)))
                )
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Welcome Back 👋", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text("Manage Your Haven", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("Track listings & booking requests", color = Color.White.copy(0.6f), fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .clickable { navController.navigate(Screen.Notifications.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LandlordStatChip("🏠", "Properties", "${uiState.totalProperties}", Modifier.weight(1f))
                        LandlordStatChip("📋", "Active",     "${uiState.activeBookingsCount}", Modifier.weight(1f))
                        LandlordStatChip("⭐", "Rating",
                            if (uiState.averageRating > 0f) "%.1f".format(uiState.averageRating) else "—",
                            Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Quick Actions ────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = Color(0xFF0D1B3E), modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f).height(100.dp).clickable { navController.navigate(Screen.AddProperty.route) },
                        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B3E)),
                        elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("➕", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Add",      color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Property", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).height(100.dp), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD4AF37)), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💰", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(formattedRevenue, color = Color(0xFF0D1B3E), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Revenue", color = Color(0xFF0D1B3E).copy(0.7f), fontSize = 11.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f).height(100.dp).clickable { navController.navigate(Screen.MyBookings.route) },
                        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📅", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${uiState.activeBookingsCount}", color = Color(0xFF0D1B3E), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Active Bookings", color = Color(0xFF8899AA), fontSize = 10.sp)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).height(100.dp).clickable { navController.navigate(Screen.MyBookings.route) },
                        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📋", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${uiState.pendingRequestsCount}", color = Color(0xFF0D1B3E), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Pending Req.", color = Color(0xFF8899AA), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // ── My Properties Preview ────────────────────────────────
        item {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("My Properties",         fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                    Text("Your listed properties", fontSize = 12.sp, color = Color(0xFF8899AA))
                }
                Text("See All", fontSize = 13.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Screen.MyProperties.route) })
            }
            Spacer(Modifier.height(12.dp))
        }

        when {
            uiState.isLoading -> item { LoadingShimmer() }
            uiState.featuredProperties.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color.White).clickable { navController.navigate(Screen.AddProperty.route) }.padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏠", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No properties yet", fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E), fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to add your first property", color = Color(0xFFD4AF37), fontSize = 13.sp)
                    }
                }
            }
            else -> items(uiState.featuredProperties.take(3)) { property ->
                NearbyPropertyCard(property) {
                    navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Landlord stat chip ───────────────────────────────────────────
@Composable
private fun LandlordStatChip(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(0.12f))
        .padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon,  fontSize = 16.sp)
            Text(value, color = Color.White,            fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(0.7f), fontSize = 10.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// TENANT HOME SCREEN
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun TenantHomeScreen(
    navController: NavController,
    viewModel    : HomeViewModel,
    uiState      : HomeUiState
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

    // Nearby properties bhi filter hogi selected category ke saath
    val filteredNearby = if (selectedCategory == "All") uiState.nearbyProperties
    else uiState.nearbyProperties.filter { it.propertyType.equals(selectedCategory, ignoreCase = true) }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {

        // ── 1. HEADER — same as original ─────────────────────────
        item {
            HomeHeaderSection(
                onSearchClick       = { navController.navigate(Screen.Search.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) }
            )
        }

        // ── 2. BROWSE BY TYPE — same as original ─────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Browse by Type",        fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                    Text("Find your perfect stay", fontSize = 12.sp, color = Color(0xFF8899AA))
                }
                Text("See All", fontSize = 13.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Screen.PropertyList.route) })
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { category ->
                    val isSelected   = selectedCategory == category
                    val categoryIcon = when (category) {
                        "House"     -> "🏡"
                        "Apartment" -> "🏢"
                        "Room"      -> "🛏"
                        "Villa"     -> "🏰"
                        "Studio"    -> "🏠"
                        else        -> "🔍"
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF0D1B3E) else Color.White)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(categoryIcon, fontSize = 20.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(category,
                            color      = if (isSelected) Color(0xFFD4AF37) else Color(0xFF0D1B3E),
                            fontSize   = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }

        // ── 3. FEATURED PROPERTIES — UI improved ─────────────────
        item {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Featured Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                    Text("Handpicked for you",  fontSize = 12.sp, color = Color(0xFF8899AA))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(if (featuredScrollState.value > 0) Color(0xFF0D1B3E) else Color(0xFFE0E0E0))
                            .clickable { scope.launch { featuredScrollState.animateScrollTo((featuredScrollState.value - 550).coerceAtLeast(0)) } },
                        contentAlignment = Alignment.Center
                    ) { Text("‹", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(if (featuredScrollState.value < featuredScrollState.maxValue) Color(0xFF0D1B3E) else Color(0xFFE0E0E0))
                            .clickable { scope.launch { featuredScrollState.animateScrollTo((featuredScrollState.value + 550).coerceAtMost(featuredScrollState.maxValue)) } },
                        contentAlignment = Alignment.Center
                    ) { Text("›", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                uiState.isLoading -> LoadingShimmer()
                uiState.errorMessage != null && allProperties.isEmpty() ->
                    Text("Error: ${uiState.errorMessage}", Modifier.padding(20.dp), color = Color.Red)
                filteredFeatured.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color.White).padding(20.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏠", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(if (selectedCategory == "All") "No featured properties"
                            else "No featured $selectedCategory properties",
                                color = Color(0xFF8899AA), fontSize = 13.sp)
                            if (selectedCategory != "All") {
                                Text("Check Nearby Properties below",
                                    color = Color(0xFFD4AF37), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                else -> Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(featuredScrollState).padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredFeatured.take(7).forEach { property ->
                        FeaturedPropertyCard(property) {
                            navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                        }
                    }
                    // See All card
                    Card(
                        modifier  = Modifier.width(160.dp).height(260.dp)
                            .clickable { navController.navigate(Screen.PropertyList.route) },
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                Text("→", fontSize = 28.sp, color = Color(0xFFD4AF37))
                                Spacer(Modifier.height(8.dp))
                                Text("See all",                  fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                Text("${allProperties.size} properties", fontSize = 11.sp, color = Color(0xFF8899AA))
                            }
                        }
                    }
                }
            }
        }

        // ── 4. VACATION BANNER — premium improved ────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0D1B3E), Color(0xFF1A3A6B))
                        )
                    )
            ) {
                // Decorative glow circles
                Box(Modifier.size(140.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp)
                    .clip(CircleShape).background(Color(0xFFD4AF37).copy(0.07f)))
                Box(Modifier.size(80.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = 20.dp)
                    .clip(CircleShape).background(Color(0xFFD4AF37).copy(0.05f)))

                // Gold top border line
                Box(
                    Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFFD4AF37), Color(0xFFD4AF37), Color.Transparent)
                            )
                        )
                )

                Column(Modifier.fillMaxWidth().padding(18.dp)) {

                    // ── Top row: emoji circle + badge ─────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Emoji with gold ring
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFFD4AF37).copy(0.25f), Color(0xFFD4AF37).copy(0.05f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏔️", fontSize = 24.sp)
                            }
                            Column {
                                // VACATION HUB pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFD4AF37).copy(0.18f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("VACATION HUB", color = Color(0xFFD4AF37),
                                        fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Explore Northern Stays",
                                    color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Arrow circle — sirf iss pe click se navigate hoga
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD4AF37))
                                .clickable { navController.navigate(Screen.VacationRentals.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowForward, null,
                                tint = Color(0xFF0D1B3E), modifier = Modifier.size(15.dp))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── Thin gold divider ─────────────────────────
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFD4AF37).copy(0.5f), Color.Transparent)
                                )
                            )
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Stats row ─────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VacationMiniStat("100+", "Stays", "🏠")
                        VacationMiniStat("PT-1", "Verified", "✅")
                        VacationMiniStat("4.8★", "Rating", "⭐")
                        VacationMiniStat("Secure", "Booking", "🔒")
                    }
                }
            }
        }

        // ── 5. NEARBY PROPERTIES — uiState.nearbyProperties ──────
        item {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Nearby Stays", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                    Text("${filteredNearby.size} properties near you", fontSize = 12.sp, color = Color(0xFF8899AA))
                }
                Text("See All", fontSize = 13.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Screen.PropertyList.route) })
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            uiState.isLoading -> item { LoadingShimmer() }
            filteredNearby.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No ${if (selectedCategory == "All") "" else "$selectedCategory "}nearby properties found",
                            color = Color(0xFF8899AA), fontSize = 14.sp
                        )
                    }
                }
            }
            else -> items(filteredNearby) { property ->
                NearbyPropertyCard(property) {
                    navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SHARED COMPOSABLES — same as original
// ═══════════════════════════════════════════════════════════════════

@Composable
fun HomeHeaderSection(
    onSearchClick      : () -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF1A3A6B))))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFD4AF37), Color.Transparent)))
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Welcome Back 👋", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Find Your Haven", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Pakistan's trusted rental platform", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(Color.White.copy(0.15f)).clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .clickable { onSearchClick() }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Search city, property type...", color = Color(0xFF8899AA), fontSize = 14.sp)
                    }
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF0D1B3E))
                        .padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Search", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .wrapContentHeight() // ✅ FIX: no more cut
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            // 🔹 IMAGE SECTION
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp) // ✅ thoda kam kiya for better spacing
            ) {
                PropertyImage(
                    property = property,
                    modifier = Modifier.fillMaxSize()
                )

                // 🔹 Gradient overlay
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.45f))
                            )
                        )
                )

                // 🔹 Price badge (top-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFD4AF37), Color(0xFFF5D060))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        property.formattedPrice,
                        color = Color(0xFF0D1B3E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // 🔹 Type badge (bottom-left)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D1B3E).copy(0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        property.propertyTypeEnum.displayName(),
                        color = Color(0xFFD4AF37),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 🔹 Available badge
                if (property.isAvailable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF4CAF50))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Available",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 🔹 CONTENT SECTION
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {

                Text(
                    property.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF0D1B3E)
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        " ${property.city}",
                        color = Color(0xFF8899AA),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 🔥 IMPORTANT ROW (was getting cut before)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ⭐ Rating + reviews
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            " ${property.averageRating}",
                            fontSize = 12.sp,
                            color = Color(0xFF0D1B3E),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            " (${property.reviewCount})",
                            fontSize = 11.sp,
                            color = Color(0xFF8899AA)
                        )
                    }

                    // 🛏 Beds + 👥 Guests
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.KingBed,
                            contentDescription = null,
                            tint = Color(0xFF8899AA),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            " ${property.bedrooms}",
                            fontSize = 11.sp,
                            color = Color(0xFF8899AA)
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = Color(0xFF8899AA),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            " ${property.maxGuests}",
                            fontSize = 11.sp,
                            color = Color(0xFF8899AA)
                        )
                    }
                }
            }
        }
    }
}

// ── NEARBY CARD — same as original ───────────────────────────────
@Composable
fun NearbyPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                PropertyImage(property = property, modifier = Modifier.fillMaxSize())
                if (property.isAvailable) {
                    Box(Modifier.align(Alignment.TopStart).padding(6.dp)
                        .size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(property.title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF0D1B3E), modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF0D1B3E).copy(0.08f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(property.propertyTypeEnum.displayName(), fontSize = 10.sp,
                            color = Color(0xFF0D1B3E), fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = Color(0xFF8899AA), fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KingBed, null, tint = Color(0xFF8899AA), modifier = Modifier.size(12.dp))
                    Text(" ${property.bedrooms} beds", fontSize = 11.sp, color = Color(0xFF8899AA))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.People, null, tint = Color(0xFF8899AA), modifier = Modifier.size(12.dp))
                    Text(" ${property.maxGuests} guests", fontSize = 11.sp, color = Color(0xFF8899AA))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${property.formattedPrice}/night", fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1B3E), fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = Color(0xFF0D1B3E), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VacationMiniStat(value: String, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = Color(0xFFD4AF37), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(0.45f), fontSize = 9.sp)
    }
}

@Composable
private fun VacationStat(value: String, label: String) {
    Column {
        Text(value, color = Color(0xFFD4AF37), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(0.45f), fontSize = 9.sp)
    }
}

@Composable
private fun VacationFeaturePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LoadingShimmer() {
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFFD4AF37))
    }
}