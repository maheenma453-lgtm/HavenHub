package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // ✅ Latest Update: Collecting single UI State from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // ── Local UI State for Filtering ──
    val categories = listOf("All", "House", "Apartment", "Room", "Villa", "Studio")
    var selectedCategory by remember { mutableStateOf("All") }

    // ✅ Filtering logic based on selected category
    val filteredFeatured = if (selectedCategory == "All") uiState.featuredProperties
    else uiState.featuredProperties.filter { it.propertyType.displayName() == selectedCategory }

    val filteredNearby = if (selectedCategory == "All") uiState.nearbyProperties
    else uiState.nearbyProperties.filter { it.propertyType.displayName() == selectedCategory }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Header Section
        item {
            HomeHeaderSection(onSearchClick = {
                navController.navigate(Screen.Search.route)
            })
        }

        // 2. Category Chips
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Browse by Type") {}
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 3. Featured Section
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Featured Properties") {}

            when {
                uiState.isLoading -> LoadingShimmer()
                uiState.errorMessage != null -> {
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                filteredFeatured.isEmpty() -> {
                    Text(
                        text = "No properties found in this category",
                        modifier = Modifier.padding(20.dp),
                        color = TextSecondary
                    )
                }
                else -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredFeatured) { property ->
                            FeaturedPropertyCard(property) {
                                navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                            }
                        }
                    }
                }
            }
        }

        // 4. Nearby Section Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Nearby Properties") {}
        }

        // 5. Nearby Section List
        if (uiState.isLoading && uiState.nearbyProperties.isEmpty()) {
            item { LoadingShimmer() }
        } else {
            items(filteredNearby.take(10)) { property ->
                NearbyPropertyCard(property) {
                    navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                }
            }
        }
    }
}

// ── UI Components ──

@Composable
fun HomeHeaderSection(onSearchClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Brush.verticalGradient(listOf(PrimaryBlue, PrimaryBlueDark)))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Welcome Back 👋", color = BackgroundWhite.copy(0.8f), fontSize = 14.sp)
                    Text("Find Your Haven", color = BackgroundWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.Notifications, null, tint = BackgroundWhite, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onSearchClick() },
                shape = RoundedCornerShape(12.dp),
                color = BackgroundWhite
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(Icons.Default.Search, null, tint = TextSecondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Search location, house...", color = TextSecondary.copy(0.6f))
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        TextButton(onClick = onSeeAll) { Text("See All", color = PrimaryBlue) }
    }
}

@Composable
fun FeaturedPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .height(230.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(130.dp).background(SurfaceVariantLight), Alignment.Center) {
                Text("🏠", fontSize = 40.sp)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    color = PrimaryBlue,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = property.formattedPrice,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(property.city, color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                    Text(" ${property.averageRating} • ${property.propertyType.displayName()}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun NearbyPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                Text("🏢", fontSize = 30.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text("${property.city} • ${property.propertyType.displayName()}", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${property.formattedPrice}/night", fontWeight = FontWeight.ExtraBold, color = PrimaryBlue)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                        Text("${property.averageRating}", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrimaryBlue)
    }
}