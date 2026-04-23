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
import com.example.havenhub.ui.theme.*
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.HomeViewModel
import com.example.havenhub.viewmodel.HomeUiState
import kotlinx.coroutines.launch

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
    viewModel    : HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val userRole  = authState.userRole
    val userId    = authState.currentUser?.uid ?: ""

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

    LazyColumn(
        modifier       = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {

        // ── Landlord Header ──────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0D1B3E), Color(0xFF1A3A6B))
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFFD4AF37), Color.Transparent)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Welcome Back 👋",
                                color      = Color(0xFFD4AF37),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Manage Your Haven",
                                color      = Color.White,
                                fontSize   = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Track listings & booking requests",
                                color    = Color.White.copy(0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { navController.navigate(Screen.Notifications.route) },
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
                                color    = Color(0xFF8899AA),
                                fontSize = 13.sp
                            )
                            Text(
                                "Check All Properties below",
                                color      = Color(0xFFD4AF37),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Medium
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
                            Box(
                                modifier         = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier            = Modifier.padding(12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            Image(
                                                painter            = painterResource(id = getPropertyImage("prop_008")),
                                                contentDescription = null,
                                                modifier           = Modifier.fillMaxSize(),
                                                contentScale       = ContentScale.Crop
                                            )
                                        }
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            Image(
                                                painter            = painterResource(id = getPropertyImage("prop_009")),
                                                contentDescription = null,
                                                modifier           = Modifier.fillMaxSize(),
                                                contentScale       = ContentScale.Crop
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            Image(
                                                painter            = painterResource(id = getPropertyImage("prop_010")),
                                                contentDescription = null,
                                                modifier           = Modifier.fillMaxSize(),
                                                contentScale       = ContentScale.Crop
                                            )
                                        }
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            Image(
                                                painter            = painterResource(id = getPropertyImage("prop_011")),
                                                contentDescription = null,
                                                modifier           = Modifier.fillMaxSize(),
                                                contentScale       = ContentScale.Crop
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "See all",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color(0xFF0D1B3E)
                                    )
                                    Text(
                                        "${allProperties.size} properties",
                                        fontSize = 11.sp,
                                        color    = Color(0xFF8899AA)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── All Properties ───────────────────────────────────────
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
                        "All Properties",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Color(0xFF0D1B3E)
                    )
                    Text(
                        if (selectedCategory == "All") "${filteredAll.size} properties available"
                        else "${filteredAll.size} $selectedCategory properties",
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
        }

        if (uiState.isLoading) {
            item { LoadingShimmer() }
        } else if (filteredAll.isEmpty()) {
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
        } else {
            items(filteredAll) { property ->
                NearbyPropertyCard(property) {
                    navController.navigate(
                        Screen.PropertyDetail.createRoute(property.propertyId)
                    )
                }
            }
        }

        // ── Vacation Banner ──────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1A3A6B), Color(0xFF0D1B3E))
                        )
                    )
                    .clickable { navController.navigate(Screen.VacationRentals.route) }
                    .padding(20.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "🏔️ Vacation Stays",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFFD4AF37)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Hunza • Swat • Murree • Naran",
                            fontSize = 12.sp,
                            color    = Color.White.copy(0.8f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD4AF37))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Pre-Book Now",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF0D1B3E)
                            )
                        }
                    }
                    Text("🏕️", fontSize = 48.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SHARED COMPOSABLES
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
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B3E), Color(0xFF1A3A6B))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFD4AF37), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Welcome Back 👋",
                        color      = Color(0xFFD4AF37),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Find Your Haven",
                        color      = Color.White,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Pakistan's trusted rental platform",
                        color    = Color.White.copy(0.6f),
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onNotificationClick() },
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable { onSearchClick() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search, null,
                            tint     = Color(0xFFD4AF37),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Search city, property type...",
                            color    = Color(0xFF8899AA),
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D1B3E))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Search",
                            fontSize   = 11.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
        modifier  = Modifier
            .width(260.dp)
            .height(260.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                            )
                        )
                )
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
                        color      = Color(0xFF0D1B3E),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D1B3E).copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        property.propertyTypeEnum.displayName(),
                        color      = Color(0xFFD4AF37),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (property.isAvailable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Available",
                            color      = Color.White,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    property.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    color      = Color(0xFF0D1B3E)
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                    Text(
                        " ${property.city}",
                        color    = Color(0xFF8899AA),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                        Text(
                            " ${property.averageRating}",
                            fontSize   = 12.sp,
                            color      = Color(0xFF0D1B3E),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(" (${property.reviewCount})", fontSize = 11.sp, color = Color(0xFF8899AA))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.KingBed, null, tint = Color(0xFF8899AA), modifier = Modifier.size(12.dp))
                        Text(" ${property.bedrooms}", fontSize = 11.sp, color = Color(0xFF8899AA))
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.People, null, tint = Color(0xFF8899AA), modifier = Modifier.size(12.dp))
                        Text(" ${property.maxGuests}", fontSize = 11.sp, color = Color(0xFF8899AA))
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
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
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
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Text(
                        property.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = Color(0xFF0D1B3E),
                        modifier   = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0D1B3E).copy(0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            property.propertyTypeEnum.displayName(),
                            fontSize   = 10.sp,
                            color      = Color(0xFF0D1B3E),
                            fontWeight = FontWeight.Medium
                        )
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
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "${property.formattedPrice}/night",
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF0D1B3E),
                        fontSize   = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                        Text(
                            " ${property.averageRating}",
                            fontSize   = 12.sp,
                            color      = Color(0xFF0D1B3E),
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
fun LoadingShimmer() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFD4AF37))
    }
}















