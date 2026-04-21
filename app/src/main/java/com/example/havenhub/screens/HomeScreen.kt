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
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.HomeViewModel

// --- HAVENHUB BRAND COLORS ---
private val HavenDeepBlue = Color(0xFF0D1B3E)
private val HavenGold = Color(0xFFD4AF37)
private val HavenBackground = Color(0xFFF1F5F9)
private val HavenSurface = Color(0xFFFFFFFF) // Shared surface color for cards

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("All", "House", "Apartment", "Room", "Villa", "Studio")
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredProperties by remember(uiState, selectedCategory) {
        derivedStateOf {
            val combined = (uiState.featuredProperties + uiState.nearbyProperties).distinctBy { it.propertyId }
            if (selectedCategory == "All") combined
            else combined.filter { it.propertyType.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = HavenBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { HomeHeaderSection(onSearchClick = { navController.navigate(Screen.Search.route) }) }

            item {
                CategorySection(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { selectedCategory = it }
                )
            }

            item {
                SectionHeader("Featured Collection", "Top picks for your comfort") {
                    navController.navigate(Screen.PropertyList.route)
                }

                if (uiState.isLoading) {
                    LoadingShimmerRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredProperties.take(5)) { property ->
                            FeaturedPropertyCard(property) {
                                navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                            }
                        }
                    }
                }
            }

            item { VacationPromoBanner { navController.navigate(Screen.VacationRentals.route) } }

            item {
                SectionHeader("Nearby Properties", "Handpicked stays near you") {
                    navController.navigate(Screen.PropertyList.route)
                }
            }

            if (uiState.isLoading) {
                items(3) { LoadingShimmerCard() }
            } else if (filteredProperties.isEmpty()) {
                item { EmptyStateView(selectedCategory) }
            } else {
                items(filteredProperties) { property ->
                    NearbyPropertyCard(property) {
                        navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
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
                    Icon(Icons.Default.Notifications, null, tint = HavenGold, modifier = Modifier.padding(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onSearchClick() },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = HavenGold)
                    Spacer(Modifier.width(12.dp))
                    Text("Where do you want to stay?", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun FeaturedPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(280.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = HavenSurface)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(160.dp)) {
                Image(
                    painter = painterResource(id = getPropertyImage(property.propertyId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopEnd),
                    color = Color.Black.copy(0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = HavenGold, modifier = Modifier.size(14.dp))
                        Text(" ${property.averageRating}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.padding(16.dp)) {
                // FIXED: Title color changed to HavenDeepBlue for visibility
                Text(
                    text = property.title,
                    color = HavenDeepBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(property.city, color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(property.formattedPrice, color = HavenDeepBlue, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("/night", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun NearbyPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HavenSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = getPropertyImage(property.propertyId)),
                contentDescription = null,
                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                // FIXED: Title color changed to HavenDeepBlue
                Text(
                    text = property.title,
                    color = HavenDeepBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(property.city, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("${property.formattedPrice}/night", color = HavenDeepBlue, fontWeight = FontWeight.ExtraBold)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = HavenGold, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = HavenDeepBlue)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Text("See All", color = HavenGold, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSeeAll() })
    }
}

@Composable
fun CategorySection(categories: List<String>, selectedCategory: String, onCategorySelect: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(categories) { cat ->
            val isSelected = cat == selectedCategory
            Surface(
                modifier = Modifier.clickable { onCategorySelect(cat) },
                color = if (isSelected) HavenDeepBlue else Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) HavenDeepBlue else Color.LightGray.copy(0.5f))
            ) {
                Text(cat, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isSelected) HavenGold else HavenDeepBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VacationPromoBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(24.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HavenDeepBlue)
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("VACATION HUB", color = HavenGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Explore Northern Stays", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = HavenGold)
        }
    }
}

@Composable fun LoadingShimmerCard() { Box(modifier = Modifier.fillMaxWidth().padding(24.dp).height(100.dp).clip(RoundedCornerShape(20.dp)).background(Color.LightGray.copy(0.3f))) }
@Composable fun LoadingShimmerRow() { Row(Modifier.padding(24.dp)) { Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(24.dp)).background(Color.LightGray.copy(0.3f))) } }
@Composable fun EmptyStateView(category: String) { Column(Modifier.fillMaxWidth().padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("🏢", fontSize = 60.sp); Text("No $category properties yet", color = Color.Gray, fontWeight = FontWeight.Medium) } }