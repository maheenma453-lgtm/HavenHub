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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
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

    val filteredFeatured = if (selectedCategory == "All") uiState.featuredProperties
    else uiState.featuredProperties.filter { it.propertyType.toString() == selectedCategory }

    val filteredNearby = if (selectedCategory == "All") uiState.nearbyProperties
    else uiState.nearbyProperties.filter { it.propertyType.toString() == selectedCategory }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            HomeHeaderSection(onSearchClick = {
                navController.navigate(Screen.Search.route)
            })
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Browse by Type", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                Text("See All", fontSize = 13.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF0D1B3E) else Color.White)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF0D1B3E),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Featured Properties",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0D1B3E)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (featuredScrollState.value > 0) Color(0xFF0D1B3E)
                                else Color(0xFFE0E0E0)
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
                                if (featuredScrollState.value < featuredScrollState.maxValue) Color(0xFF0D1B3E)
                                else Color(0xFFE0E0E0)
                            )
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
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        modifier = Modifier.padding(20.dp),
                        color = Color.Red
                    )
                }
                filteredFeatured.isEmpty() -> {
                    Text(
                        text = "No properties found",
                        modifier = Modifier.padding(20.dp),
                        color = Color(0xFF8899AA)
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(featuredScrollState)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        filteredFeatured.forEach { property ->
                            FeaturedPropertyCard(property) {
                                navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nearby Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1B3E))
                Text("See All", fontSize = 13.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

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

@Composable
fun HomeHeaderSection(onSearchClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF1A3A6B)))
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome Back 👋", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Find Your Haven", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable { onSearchClick() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Search location, house...", color = Color(0xFF8899AA), fontSize = 14.sp)
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
        modifier = Modifier.width(260.dp).height(250.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(155.dp)) {
                Image(
                    painter = painterResource(id = getPropertyImage(property.propertyId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)))
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color(0xFFF5D060))))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(text = property.formattedPrice, color = Color(0xFF0D1B3E), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D1B3E).copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = property.propertyType.toString(), color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF0D1B3E))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = Color(0xFF8899AA), fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                    Text(" ${property.averageRating}", fontSize = 12.sp, color = Color(0xFF0D1B3E), fontWeight = FontWeight.SemiBold)
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
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp))) {
                Image(
                    painter = painterResource(id = getPropertyImage(property.propertyId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF0D1B3E))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                    Text(" ${property.city} • ${property.propertyType.toString()}", color = Color(0xFF8899AA), fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${property.formattedPrice}/night", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1B3E), fontSize = 14.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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