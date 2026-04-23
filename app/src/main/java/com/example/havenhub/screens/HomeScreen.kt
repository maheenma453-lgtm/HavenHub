package com.example.havenhub.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.HomeViewModel

// ═══════════════════════════════════════════════════════════════════
// HELPER: Property ke liye safe image resolve karo
// ═══════════════════════════════════════════════════════════════════
private fun resolveImage(property: Property): Int {
    if (property.drawableImageName.isNotEmpty()) {
        return getPropertyImage(property.drawableImageName)
    }
    if (property.resolvedDrawableName.isNotEmpty()) {
        return getPropertyImage(property.resolvedDrawableName)
    }
    return getPropertyImage(property.propertyId)
}

// ═══════════════════════════════════════════════════════════════════
// imgbb URL hai toh AsyncImage, warna local drawable
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun PropertyImage(
    property    : Property,
    modifier    : Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    //val remoteUrl = property.imageUrls.firstOrNull()
    //if (!remoteUrl.isNullOrEmpty()) {
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
            painter            = painterResource(id = resolveImage(property)),
            contentDescription = property.title,
            modifier           = modifier,
            contentScale       = contentScale
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// MAIN ENTRY POINT — Role check + data load
// ✅ FIX: userRole empty hone par return karo — race condition fix
// ═══════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("All", "House", "Apartment", "Room", "Villa", "Studio")
    var selectedCategory by remember { mutableStateOf("All") }

    // ✅ FIX: userRole empty hone tak kuch mat karo
    // Jab role Firebase se load hogi tab hi data fetch karega
    LaunchedEffect(userId, userRole) {
        if (userRole.isEmpty()) return@LaunchedEffect

        when {
            userRole == "landlord" && userId.isNotEmpty() -> {
                viewModel.loadLandlordStats(userId)
            }
            userRole != "landlord" && userId.isNotEmpty() -> {
                viewModel.loadHomeData()
            }
            userRole != "landlord" -> {
                // Guest / userId abhi nahi mila — phir bhi properties load karo
                viewModel.loadHomeData()
            }
        }
    }

    when (userRole) {
        "landlord" -> LandlordHomeScreen(navController = navController, uiState = uiState)
        else       -> TenantHomeScreen(navController = navController, viewModel = viewModel, uiState = uiState)
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
            uiState.totalRevenue >= 1_000_000 ->
                "PKR %.1fM".format(uiState.totalRevenue / 1_000_000)
            uiState.totalRevenue >= 1_000     ->
                "PKR %.0fK".format(uiState.totalRevenue / 1_000)
            else                              ->
                "PKR %.0f".format(uiState.totalRevenue)
        }
    }

    Scaffold(containerColor = HavenBackground) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ──
            item {
                HomeHeaderSection(onSearchClick = { navController.navigate(Screen.Search.route) })
            }

            // ── Categories ──
            item {
                CategorySection(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { selectedCategory = it }
                )
            }

            // ── Featured Properties ──
            item {
                SectionHeader("Featured Collection", "Top picks for your comfort") {
                    navController.navigate(Screen.PropertyList.route)
                }

                when {
                    uiState.isLoading -> LoadingShimmerRow()
                    filteredFeatured.isEmpty() && selectedCategory != "All" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications, null,
                                tint     = Color(0xFFD4AF37),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LandlordStatChip(
                            icon     = "🏠",
                            label    = "Properties",
                            value    = "${uiState.totalProperties}",
                            modifier = Modifier.weight(1f)
                        )
                        LandlordStatChip(
                            icon     = "📋",
                            label    = "Active",
                            value    = "${uiState.activeBookingsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        LandlordStatChip(
                            icon     = "⭐",
                            label    = "Rating",
                            value    = if (uiState.averageRating > 0f)
                                "%.1f".format(uiState.averageRating)
                            else "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── Quick Actions — 2x2 grid ──────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "Quick Actions",
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = Color(0xFF0D1B3E),
                modifier   = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier  = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { navController.navigate(Screen.AddProperty.route) },
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color(0xFF0D1B3E)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier            = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("➕", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Add",
                                color      = Color(0xFFD4AF37),
                                fontWeight = FontWeight.Bold,
                                fontSize   = 13.sp
                            )
                            Text("Property", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Card(
                        modifier  = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color(0xFFD4AF37)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier            = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💰", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                formattedRevenue,
                                color      = Color(0xFF0D1B3E),
                                fontWeight = FontWeight.Bold,
                                fontSize   = 12.sp,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            Text(
                                "Revenue",
                                color    = Color(0xFF0D1B3E).copy(0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier  = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { navController.navigate(Screen.MyBookings.route) },
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier            = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📅", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${uiState.activeBookingsCount}",
                                color      = Color(0xFF0D1B3E),
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp
                            )
                            Text("Active Bookings", color = Color(0xFF8899AA), fontSize = 10.sp)
                        }
                    }

                    Card(
                        modifier  = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { navController.navigate(Screen.MyBookings.route) },
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier            = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📋", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${uiState.pendingRequestsCount}",
                                color      = Color(0xFF0D1B3E),
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp
                            )
                            Text("Pending Req.", color = Color(0xFF8899AA), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // ── My Properties Preview ─────────────────────────────────
        item {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "My Properties",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Color(0xFF0D1B3E)
                    )
                    Text(
                        "Your listed properties",
                        fontSize = 12.sp,
                        color    = Color(0xFF8899AA)
                    )
                }
                Text(
                    "See All",
                    fontSize   = 13.sp,
                    color      = Color(0xFFD4AF37),
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable {
                        navController.navigate(Screen.MyProperties.route)
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (uiState.isLoading) {
            item { LoadingShimmer() }
        } else if (uiState.featuredProperties.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .clickable { navController.navigate(Screen.AddProperty.route) }
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏠", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No properties yet",
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF0D1B3E),
                            fontSize   = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap to add your first property",
                            color    = Color(0xFFD4AF37),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(uiState.featuredProperties.take(3)) { property ->
                NearbyPropertyCard(property) {
                    navController.navigate(
                        Screen.PropertyDetail.createRoute(property.propertyId)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Small stat chip inside landlord header ───────────────────────
@Composable
private fun LandlordStatChip(
    icon    : String,
    label   : String,
    value   : String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
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
    val categories       = listOf("All", "House", "Apartment", "Room", "Villa", "Studio")
    var selectedCategory by remember { mutableStateOf("All") }
    val featuredScrollState = rememberScrollState()
    val scope               = rememberCoroutineScope()

    val allProperties = uiState.allProperties.ifEmpty {
        (uiState.featuredProperties + uiState.nearbyProperties).distinctBy { it.propertyId }
    }

    val filteredFeatured = if (selectedCategory == "All")
        uiState.featuredProperties
    else
        uiState.featuredProperties.filter {
            it.propertyType.equals(selectedCategory, ignoreCase = true)
        }

    val filteredAll = if (selectedCategory == "All")
        allProperties
    else
        allProperties.filter {
            it.propertyType.equals(selectedCategory, ignoreCase = true)
        }

    LazyColumn(
        modifier       = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Tenant Header with Search ────────────────────────────
        item {
            HomeHeaderSection(
                onSearchClick       = { navController.navigate(Screen.Search.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) }
            )
        }

        // ── Browse by Type ───────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Browse by Type",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Color(0xFF0D1B3E)
                    )
                    Text(
                        "Find your perfect stay",
                        fontSize = 12.sp,
                        color    = Color(0xFF8899AA)
                    )
                }
                Text(
                    "See All",
                    fontSize   = 13.sp,
                    color      = Color(0xFFD4AF37),
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable {
                        navController.navigate(Screen.PropertyList.route)
                    }
                )
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            .background(
                                if (isSelected) Color(0xFF0D1B3E) else Color.White
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(categoryIcon, fontSize = 20.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text       = category,
                            color      = if (isSelected) Color(0xFFD4AF37) else Color(0xFF0D1B3E),
                            fontSize   = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ── Featured Properties ──────────────────────────────────
        item {
            Spacer(Modifier.height(28.dp))
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Featured Properties",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Color(0xFF0D1B3E)
                    )
                    Text("Handpicked for you", fontSize = 12.sp, color = Color(0xFF8899AA))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (featuredScrollState.value > 0)
                                    Color(0xFF0D1B3E) else Color(0xFFE0E0E0)
                            )
                            .clickable {
                                scope.launch {
                                    featuredScrollState.animateScrollTo(
                                        (featuredScrollState.value - 550).coerceAtLeast(0)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("‹", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (featuredScrollState.value < featuredScrollState.maxValue)
                                    Color(0xFF0D1B3E) else Color(0xFFE0E0E0)
                            )
                            .clickable {
                                scope.launch {
                                    featuredScrollState.animateScrollTo(
                                        (featuredScrollState.value + 550)
                                            .coerceAtMost(featuredScrollState.maxValue)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("›", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                uiState.isLoading -> LoadingShimmer()

                uiState.errorMessage != null && allProperties.isEmpty() -> {
                    Text(
                        text     = "Error: ${uiState.errorMessage}",
                        modifier = Modifier.padding(20.dp),
                        color    = Color.Red
                    )
                }

                filteredFeatured.isEmpty() && selectedCategory != "All" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏠", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No featured $selectedCategory properties",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                filteredFeatured.isEmpty() -> {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No featured properties",
                            color    = Color(0xFF8899AA),
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(featuredScrollState)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        filteredFeatured.take(7).forEach { property ->
                            FeaturedPropertyCard(property) {
                                navController.navigate(
                                    Screen.PropertyDetail.createRoute(property.propertyId)
                                )
                            }
                        }
                        // See All Card
                        Card(
                            modifier  = Modifier
                                .width(160.dp)
                                .height(260.dp)
                                .clickable { navController.navigate(Screen.PropertyList.route) },
                            shape     = RoundedCornerShape(20.dp),
                            colors    = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Text("No featured properties", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    else -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredFeatured) { property ->
                                FeaturedPropertyCard(property) {
                                    navController.navigate(
                                        Screen.PropertyDetail.createRoute(property.propertyId)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Vacation Banner ──
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (selectedCategory == "All") "No properties found"
                            else "No $selectedCategory properties found",
                            color    = Color(0xFF8899AA),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ── All Properties ──
            item {
                SectionHeader(
                    title = if (selectedCategory == "All") "All Properties" else "$selectedCategory Properties",
                    subtitle = "${filteredProperties.size} properties available"
                ) {
                    navController.navigate(Screen.PropertyList.route)
                }
            }

            when {
                uiState.isLoading -> {
                    items(3) { LoadingShimmerCard() }
                }
                filteredProperties.isEmpty() -> {
                    item { EmptyStateView(selectedCategory) }
                }
                else -> {
                    items(filteredProperties) { property ->
                        NearbyPropertyCard(property) {
                            navController.navigate(
                                Screen.PropertyDetail.createRoute(property.propertyId)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeaderSection(onSearchClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(HavenDeepBlue, Color(0xFF1E3A8A))))
            .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 32.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Explore Pakistan", color = HavenGold, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Find Your Haven", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Surface(color = Color.White.copy(0.15f), shape = CircleShape) {
                    Icon(
                        Icons.Default.Notifications,
                        null,
                        tint = HavenGold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onSearchClick() },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = HavenGold)
                    Spacer(Modifier.width(12.dp))
                    Text("Where do you want to stay?", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// FEATURED PROPERTY CARD
// ─────────────────────────────────────────────────────────────────
@Composable
fun FeaturedPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(280.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = HavenSurface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            ) {
                PropertyImage(
                    property     = property,
                    modifier     = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    color = Color.Black.copy(0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = HavenGold, modifier = Modifier.size(14.dp))
                        Text(
                            " ${property.averageRating}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Price badge
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    color = HavenGold,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        property.formattedPrice,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = HavenDeepBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                // Type badge
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomStart),
                    color = HavenDeepBlue.copy(0.85f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        property.propertyTypeEnum.displayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = HavenGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = property.title,
                    color = HavenDeepBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = HavenGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(" ${property.city}", color = Color.Gray, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.KingBed, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                        Text(" ${property.bedrooms} beds", color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.People, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                        Text(" ${property.maxGuests}", color = Color.Gray, fontSize = 12.sp)
                    }
                    if (property.isAvailable) {
                        Surface(
                            color = Color(0xFF4CAF50).copy(0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Available",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                color = Color(0xFF4CAF50),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// NEARBY / ALL PROPERTY CARD
// ─────────────────────────────────────────────────────────────────
@Composable
fun NearbyPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HavenSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                PropertyImage(
                    property     = property,
                    modifier     = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (property.isAvailable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }
            Column(
                Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                Text(
                    text = property.title,
                    color = HavenDeepBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = HavenGold, modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = HavenDeepBlue.copy(0.08f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            property.propertyTypeEnum.displayName(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = HavenDeepBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KingBed, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Text(" ${property.bedrooms} beds", color = Color.Gray, fontSize = 11.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.People, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Text(" ${property.maxGuests} guests", color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${property.formattedPrice}/night",
                        fontWeight = FontWeight.ExtraBold,
                        color = HavenDeepBlue,
                        fontSize = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = HavenGold, modifier = Modifier.size(12.dp))
                        Text(
                            " ${property.averageRating}",
                            fontSize = 12.sp,
                            color = HavenDeepBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// LOADING SHIMMER
// ─────────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, subtitle: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = HavenDeepBlue)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Text(
            "See All",
            color = HavenGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onSeeAll() }
        )
    }
}

@Composable
fun CategorySection(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories) { cat ->
            val isSelected = cat == selectedCategory
            Surface(
                modifier = Modifier.clickable { onCategorySelect(cat) },
                color = if (isSelected) HavenDeepBlue else Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) HavenDeepBlue else Color.LightGray.copy(0.5f))
            ) {
                Text(
                    cat,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isSelected) HavenGold else HavenDeepBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VacationPromoBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HavenDeepBlue)
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("VACATION HUB", color = HavenGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Explore Northern Stays", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Hunza • Swat • Murree • Naran", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
            Text("🏔️", fontSize = 40.sp)
        }
    }
}

@Composable
fun LoadingShimmerCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.LightGray.copy(0.3f))
    )
}

@Composable
fun LoadingShimmerRow() {
    Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .size(280.dp, 220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray.copy(0.3f))
        )
    }
}

@Composable
fun EmptyStateView(category: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏢", fontSize = 60.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "No $category properties yet",
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}















