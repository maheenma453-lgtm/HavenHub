package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.PropertyViewModel

// ─────────────────────────────────────────────────────────────────
// PropertyListScreen.kt
// PURPOSE : Displays all available properties in a scrollable list/grid.
//           Toggle between list view and grid view.
//           Filter chips at top for quick type filtering.
//           Sort options: Price, Rating, Newest.
// NAVIGATION:
//   → PropertyDetailScreen (on card tap)
//   → FilterScreen (filter button)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListScreen(
    navController : NavController,
    viewModel     : PropertyViewModel = hiltViewModel()
) {

    // ── State ──────────────────────────────────────────────────────
    var isGridView       by remember { mutableStateOf(false) }    // Toggle list/grid
    var selectedFilter   by remember { mutableStateOf("All") }
    var showSortMenu     by remember { mutableStateOf(false) }
    var selectedSort     by remember { mutableStateOf("Newest") }

    val properties   by viewModel.properties.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()

    val filterOptions = listOf("All", "House", "Apartment", "Room", "Studio", "Villa")
    val sortOptions   = listOf("Newest", "Price ↑", "Price ↓", "Rating")

    // ── UI ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("All Properties") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = BackgroundWhite
                        )
                    }
                },
                actions = {
                    // Sort dropdown menu trigger
                    Box {
                        TextButton(
                            onClick = { showSortMenu = true }
                        ) {
                            Text("Sort", color = BackgroundWhite)
                        }
                        DropdownMenu(
                            expanded         = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            sortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text    = { Text(option) },
                                    onClick = {
                                        selectedSort = option
                                        showSortMenu = false
                                        viewModel.sortProperties(option)
                                    }
                                )
                            }
                        }
                    }

                    // Filter button
                    IconButton(onClick = { navController.navigate(Screen.Filter.route) }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = BackgroundWhite
                        )
                    }

                    // Toggle grid / list view
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = BackgroundWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor     = PrimaryBlue,
                    titleContentColor  = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
        ) {

            // ── Type Filter Chips Row ──────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { option ->
                    val isSelected = selectedFilter == option
                    FilterChip(
                        selected = isSelected,
                        onClick  = {
                            selectedFilter = option
                            viewModel.filterByType(option)
                        },
                        label  = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor     = BackgroundWhite,
                            containerColor         = SurfaceGray,
                            labelColor             = TextSecondary
                        )
                    )
                }
            }

            // ── Results Count ──────────────────────────────────────
            Text(
                text     = "${properties.size} properties",
                fontSize = 13.sp,
                color    = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // ── Loading State ──────────────────────────────────────
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (properties.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🏚️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text       = "No properties found",
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                        Text(
                            text     = "Try changing your filters",
                            fontSize = 13.sp,
                            color    = TextSecondary
                        )
                    }
                }
            } else {
                // ── Property List ──────────────────────────────────
                LazyColumn(
                    contentPadding        = PaddingValues(
                        horizontal = 16.dp,
                        vertical   = 8.dp
                    ),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    items(properties) { property ->
                        PropertyListCard(
                            title    = property.title,
                            city     = property.city,
                            type     = property.type,
                            price    = property.price,
                            rating   = property.rating,
                            bedrooms = property.bedrooms,
                            isVerified = property.isVerified,
                            onClick  = {
                                navController.navigate(
                                    Screen.PropertyDetail.createRoute(property.id)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// PropertyListCard
// Full-width card shown in list view
// ─────────────────────────────────────────────────────────────────
@Composable
private fun PropertyListCard(
    title      : String,
    city       : String,
    type       : String,
    price      : Long,
    rating     : Float,
    bedrooms   : Int,
    isVerified : Boolean,
    onClick    : () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors    = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column {
            // ── Property Image Area ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏠", fontSize = 52.sp)

                // Verified badge
                if (isVerified) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SuccessGreen)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "✓ Verified", fontSize = 11.sp, color = BackgroundWhite, fontWeight = FontWeight.Medium)
                    }
                }

                // Price tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryBlue)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text       = "PKR $price/mo",
                        fontSize   = 12.sp,
                        color      = BackgroundWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Details Section ────────────────────────────────────
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "📍 $city • $type", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                // Stats row: bedrooms, rating, availability
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(text = "🛏 $bedrooms BHK", fontSize = 13.sp, color = TextSecondary)
                    Text(text = "⭐ $rating", fontSize = 13.sp, color = StarGold)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusAvailable.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = "Available", fontSize = 11.sp, color = StatusAvailable, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

