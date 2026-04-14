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
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("All", "House", "Apartment", "Room", "Villa", "Studio")
    var selectedCategory by remember { mutableStateOf("All") }
    val featuredScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // All properties combined
    val allProperties = (uiState.featuredProperties + uiState.nearbyProperties).distinctBy { it.propertyId }

    val filteredFeatured = if (selectedCategory == "All") uiState.featuredProperties
    else uiState.featuredProperties.filter {
        it.propertyType.equals(selectedCategory.uppercase(), ignoreCase = true)
    }

    val filteredAll = if (selectedCategory == "All") allProperties
    else allProperties.filter {
        it.propertyType.equals(selectedCategory.uppercase(), ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Header ──
        item {
            HomeHeaderSection(
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) }
            )
        }

        // ── Quick Stats ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard("🏠", "Properties", "${allProperties.size}", Modifier.weight(1f))
                QuickStatCard("📍", "Cities", "12+", Modifier.weight(1f))
                QuickStatCard("⭐", "Verified", "100%", Modifier.weight(1f))
            }
        }

        // ── Browse by Type ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Browse by Type", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                    Text("Find your perfect stay", fontSize = 12.sp, color = Color(0xFF8899AA))
                }
                Text(
                    "See All",
                    fontSize = 13.sp,
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Screen.PropertyList.route) }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    val categoryIcon = when (category) {
                        "House" -> "🏡"
                        "Apartment" -> "🏢"
                        "Room" -> "🛏"
                        "Villa" -> "🏰"
                        "Studio" -> "🏠"
                        else -> "🔍"
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF0D1B3E) else Color.White)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(categoryIcon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category,
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF0D1B3E),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ── Featured Properties ──
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Featured Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                    Text("Handpicked for you", fontSize = 12.sp, color = Color(0xFF8899AA))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (featuredScrollState.value > 0) Color(0xFF0D1B3E) else Color(0xFFE0E0E0))
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
                            .background(if (featuredScrollState.value < featuredScrollState.maxValue) Color(0xFF0D1B3E) else Color(0xFFE0E0E0))
                            .clickable {
                                scope.launch {
                                    featuredScrollState.animateScrollTo(
                                        (featuredScrollState.value + 550).coerceAtMost(featuredScrollState.maxValue)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("›", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                uiState.isLoading -> LoadingShimmer()
                uiState.errorMessage != null -> {
                    Text(text = "Error: ${uiState.errorMessage}", modifier = Modifier.padding(20.dp), color = Color.Red)
                }
                filteredFeatured.isEmpty() && selectedCategory != "All" -> {
                    // Koi featured nahi is type mein — message show karo
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No featured $selectedCategory properties",
                                color = Color(0xFF8899AA),
                                fontSize = 13.sp
                            )
                            Text(
                                "Check All Properties below",
                                color = Color(0xFFD4AF37),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                filteredFeatured.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No featured properties", color = Color(0xFF8899AA), fontSize = 14.sp)
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
                                navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                            }
                        }
                        // See All Card
                        Card(
                            modifier = Modifier.width(160.dp).height(260.dp).clickable {
                                navController.navigate(Screen.PropertyList.route)
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            Image(painter = painterResource(id = getPropertyImage("prop_008")), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            Image(painter = painterResource(id = getPropertyImage("prop_009")), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            Image(painter = painterResource(id = getPropertyImage("prop_010")), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            Image(painter = painterResource(id = getPropertyImage("prop_011")), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text("See all", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                    Text("${allProperties.size} properties", fontSize = 11.sp, color = Color(0xFF8899AA))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── All Properties (Nearby) ──
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("All Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                    Text(
                        if (selectedCategory == "All") "${filteredAll.size} properties available"
                        else "${filteredAll.size} $selectedCategory properties",
                        fontSize = 12.sp,
                        color = Color(0xFF8899AA)
                    )
                }
                Text(
                    "See All",
                    fontSize = 13.sp,
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Screen.PropertyList.route) }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (uiState.isLoading) {
            item { LoadingShimmer() }
        } else if (filteredAll.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No $selectedCategory properties found", color = Color(0xFF8899AA), fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(filteredAll) { property ->
                NearbyPropertyCard(property) {
                    navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                }
            }
        }

        // ── Vacation Banner ──
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A3A6B), Color(0xFF0D1B3E))))
                    .clickable { navController.navigate(Screen.VacationRentals.route) }
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🏔️ Vacation Stays", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Hunza • Swat • Murree • Naran", fontSize = 12.sp, color = Color.White.copy(0.8f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD4AF37))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Pre-Book Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                        }
                    }
                    Text("🏕️", fontSize = 48.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickStatCard(icon: String, label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
            Text(label, fontSize = 11.sp, color = Color(0xFF8899AA))
        }
    }
}

@Composable
fun HomeHeaderSection(onSearchClick: () -> Unit, onNotificationClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF1A3A6B))))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFD4AF37), Color.Transparent)))
        )
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome Back 👋", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Find Your Haven", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Pakistan's trusted rental platform", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(22.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Search city, property type...", color = Color(0xFF8899AA), fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D1B3E))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Search", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
        TextButton(onClick = onSeeAll) { Text("See All", color = Color(0xFFD4AF37)) }
    }
}

@Composable
fun FeaturedPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(260.dp).height(260.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(165.dp)) {
                Image(
                    painter = painterResource(id = getPropertyImage(property.propertyId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color(0xFFF5D060))))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(text = property.formattedPrice, color = Color(0xFF0D1B3E), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D1B3E).copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = property.propertyTypeEnum.displayName(), color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (property.isAvailable) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("Available", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF0D1B3E))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = Color(0xFF8899AA), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = Color(0xFF0D1B3E), fontWeight = FontWeight.SemiBold)
                        Text(" (${property.reviewCount})", fontSize = 11.sp, color = Color(0xFF8899AA))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.KingBed, null, tint = Color(0xFF8899AA), modifier = Modifier.size(12.dp))
                        Text(" ${property.bedrooms}", fontSize = 11.sp, color = Color(0xFF8899AA))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.People, null, tint = Color(0xFF8899AA), modifier = Modifier.size(12.dp))
                        Text(" ${property.maxGuests}", fontSize = 11.sp, color = Color(0xFF8899AA))
                    }
                }
            }
        }
    }
}

@Composable
fun NearbyPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                Image(
                    painter = painterResource(id = getPropertyImage(property.propertyId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (property.isAvailable) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                            .size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50))
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(property.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF0D1B3E), modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0D1B3E).copy(0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(property.propertyTypeEnum.displayName(), fontSize = 10.sp, color = Color(0xFF0D1B3E), fontWeight = FontWeight.Medium)
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.People, null, tint = Color(0xFF8899AA), modifier = Modifier.size(12.dp))
                    Text(" ${property.maxGuests} guests", fontSize = 11.sp, color = Color(0xFF8899AA))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${property.formattedPrice}/night", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1B3E), fontSize = 14.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFF8E1)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = Color(0xFF0D1B3E), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingShimmer() {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFFD4AF37))
    }
}