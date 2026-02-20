package com.example.havenhub.screens
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.HomeViewModel

// ─────────────────────────────────────────────────────────────────
// HomeScreen.kt
// PURPOSE : Main dashboard for logged-in users.
//           Shows greeting, search bar, property categories,
//           featured properties, and nearby listings.
//           Owner sees extra "My Properties" + "Add Property" shortcuts.
// NAVIGATION: HomeScreen → SearchScreen, PropertyDetailScreen, etc.
// ─────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    navController : NavController,
    viewModel     : HomeViewModel = hiltViewModel()
) {

    // ── State from ViewModel ────────────────────────────────────
    val userName         by viewModel.userName.collectAsState()
    val featuredProps    by viewModel.featuredProperties.collectAsState()
    val isOwner          by viewModel.isOwner.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    // Property type filter tabs
    val categories = listOf("All", "House", "Apartment", "Room", "Shop", "Villa")

    // ── UI ─────────────────────────────────────────────────────
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite),
        contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom nav
    ) {

        // ── Blue Header Section ──────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(PrimaryBlue, PrimaryDark)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {

                    // ── Top Row: Greeting + Notification Bell ────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text     = "Hello, $userName 👋",
                                fontSize = 14.sp,
                                color    = BackgroundWhite.copy(alpha = 0.85f)
                            )
                            Text(
                                text       = "Find Your Perfect Home",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color      = BackgroundWhite
                            )
                        }

                        // Notification bell with badge
                        Box {
                            IconButton(
                                onClick = { navController.navigate(Screen.Notifications.route) }
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint               = BackgroundWhite,
                                    modifier           = Modifier.size(26.dp)
                                )
                            }
                            // Red dot badge for unread notifications
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ErrorRed)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Search Bar (tappable, goes to SearchScreen) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundWhite)
                            .clickable { navController.navigate(Screen.Search.route) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Search,
                                contentDescription = "Search",
                                tint               = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text     = "Search city, area, property...",
                                color    = TextHint,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Owner Quick Actions ──────────────────────────────────
        if (isOwner) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Property shortcut
                    OwnerQuickAction(
                        emoji   = "➕",
                        label   = "Add Property",
                        onClick = { navController.navigate(Screen.AddProperty.route) },
                        modifier = Modifier.weight(1f)
                    )
                    // My Properties shortcut
                    OwnerQuickAction(
                        emoji   = "🏘️",
                        label   = "My Properties",
                        onClick = { navController.navigate(Screen.MyProperties.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Section: Category Filter Chips ───────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title    = "Browse by Type",
                onSeeAll = { navController.navigate(Screen.PropertyList.route) }
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding        = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick  = { viewModel.selectCategory(category) },
                        label    = { Text(category) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor  = PrimaryBlue,
                            selectedLabelColor      = BackgroundWhite,
                            containerColor          = SurfaceGray,
                            labelColor              = TextSecondary
                        )
                    )
                }
            }
        }

        // ── Section: Featured Properties ─────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title    = "Featured Properties",
                onSeeAll = { navController.navigate(Screen.PropertyList.route) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Featured property cards (horizontal scroll list)
        item {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(featuredProps) { property ->
                    FeaturedPropertyCard(
                        property = property,
                        onClick  = {
                            navController.navigate(
                                Screen.PropertyDetail.createRoute(property.id)
                            )
                        }
                    )
                }
            }
        }

        // ── Section: Nearby Properties ────────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title    = "Nearby Properties",
                onSeeAll = { navController.navigate(Screen.PropertyList.route) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Nearby property cards (vertical list)
        items(featuredProps.take(5)) { property ->
            NearbyPropertyCard(
                property = property,
                onClick  = {
                    navController.navigate(
                        Screen.PropertyDetail.createRoute(property.id)
                    )
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Helper Composables for Home Screen
// ─────────────────────────────────────────────────────────────────

// Section header with title + "See All" button
@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        TextButton(onClick = onSeeAll) {
            Text(text = "See All", color = PrimaryBlue, fontSize = 13.sp)
        }
    }
}

// Owner quick action button (e.g., "Add Property")
@Composable
private fun OwnerQuickAction(
    emoji    : String,
    label    : String,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    Card(
        modifier  = modifier
            .height(64.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue)
        }
    }
}

// Property card for Featured row (horizontal)
@Composable
private fun FeaturedPropertyCard(
    property : com.havenhub.data.model.Property,
    onClick  : () -> Unit
) {
    Card(
        modifier  = Modifier
            .width(220.dp)
            .height(200.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Property image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏠", fontSize = 40.sp)
                // Price badge (top right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryBlue)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text     = "PKR ${property.price}/mo",
                        fontSize = 10.sp,
                        color    = BackgroundWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Property details
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = property.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = property.city, fontSize = 11.sp, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⭐ ${property.rating}", fontSize = 11.sp, color = StarGold)
                    Text(text = " • ${property.type}", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

// Compact property card for Nearby section (vertical)
@Composable
private fun NearbyPropertyCard(
    property : com.havenhub.data.model.Property,
    onClick  : () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏠", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = property.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = property.city, fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⭐ ${property.rating}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text       = "PKR ${property.price}/mo",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryBlue
                    )
                }
            }
        }
    }
}

