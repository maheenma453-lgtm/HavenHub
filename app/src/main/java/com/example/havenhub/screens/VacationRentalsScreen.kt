package com.example.havenhub.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.VacationViewModel
import com.example.havenhub.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationRentalsScreen(
    navController: NavController,
    viewModel    : VacationViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    var selectedCategory  by remember { mutableStateOf("All") }

    val filteredProperties = if (selectedCategory == "All") {
        uiState.properties
    } else {
        uiState.properties.filter {
            it.city.equals(selectedCategory, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadVacationProperties() }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val background       = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(primary, primaryContainer)))
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(onPrimary.copy(0.10f))
                            .border(1.dp, tertiary.copy(0.45f), CircleShape)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint     = tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "VACATION HUB",
                            color         = tertiary,
                            fontSize      = 15.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Northern Pakistan Stays",
                            color    = onPrimary.copy(0.50f),
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color  = tertiary.copy(0.15f),
                        shape  = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, tertiary.copy(0.45f))
                    ) {
                        Text(
                            "${filteredProperties.size} Stays",
                            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color      = tertiary,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = background
    ) { paddingValues ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            // ── Hero Banner ───────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(listOf(primaryContainer, background))
                        )
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.08f)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height)
                            lineTo(size.width * 0.2f, size.height * 0.4f)
                            lineTo(size.width * 0.5f, size.height * 0.8f)
                            lineTo(size.width * 0.8f, size.height * 0.3f)
                            lineTo(size.width, size.height * 0.6f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = tertiary.copy(0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "  PREMIUM SELECTION  ",
                                modifier   = Modifier.padding(vertical = 4.dp),
                                color      = tertiary,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your Gateway to\nthe North",
                            color      = MaterialTheme.colorScheme.onBackground,
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 32.sp
                        )
                        Spacer(Modifier.height(16.dp))

                        // ── City filter chips ─────────────────────────────
                        Row(
                            modifier              = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = listOf(
                                "All", "Islamabad", "Hunza", "Naran",
                                "Skardu", "Swat", "Murree"
                            )
                            categories.forEach { city ->
                                Surface(
                                    modifier        = Modifier.clickable { selectedCategory = city },
                                    color           = if (city == selectedCategory) primary
                                    else MaterialTheme.colorScheme.surface,
                                    shape           = RoundedCornerShape(12.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        city,
                                        modifier   = Modifier.padding(
                                            horizontal = 16.dp, vertical = 8.dp
                                        ),
                                        color      = if (city == selectedCategory) onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Section Header ────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(4.dp, 18.dp)
                                .background(tertiary, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (selectedCategory == "All") "Nearby Stays"
                            else "$selectedCategory Stays",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 18.sp,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        "View Map",
                        color      = tertiary,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.clickable {
                            navController.navigate(Screen.ExploreMap.route)
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Loading ───────────────────────────────────────────────────
            if (uiState.isLoading) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        Alignment.Center
                    ) {
                        CircularProgressIndicator(color = tertiary)
                    }
                }
            }

            // ── Empty State ───────────────────────────────────────────────
            else if (filteredProperties.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.LocationOff, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No stays found in $selectedCategory",
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Try selecting a different city",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Property Cards ────────────────────────────────────────────
            else {
                items(filteredProperties) { prop ->
                    // ── IMAGE SOURCE DECISION ─────────────────────────────
                    // Auto-added properties (auto-id): imageUrls list mein
                    //   ImgBB URLs hoti hain — AsyncImage se load karo.
                    // Manually seeded (prop_001..prop_012): imageUrls empty
                    //   hoti hain — purana drawable system use karo.
                    val networkImageUrl = prop.imageUrls.firstOrNull()
                        ?.takeIf { it.startsWith("http") }

                    VacationPropertyCard(
                        propertyId      = prop.propertyId,
                        title           = prop.title,
                        location        = prop.city,
                        price           = prop.pricePerNight,
                        rating          = prop.averageRating.toDouble(),
                        amenities       = prop.amenities,
                        networkImageUrl = networkImageUrl,   // ← NEW
                        onBookClick     = {
                            navController.navigate(
                                Screen.PreBooking.createRoute(prop.propertyId)
                            )
                        },
                        onCardClick     = {
                            navController.navigate(
                                Screen.PropertyDetail.createRoute(prop.propertyId)
                            )
                        }
                    )
                }
            }
        }
    }
}

// ── Property Card ─────────────────────────────────────────────────────────────
@Composable
fun VacationPropertyCard(
    propertyId      : String,
    title           : String,
    location        : String,
    price           : Double,
    rating          : Double,
    amenities       : List<String>,
    networkImageUrl : String?,          // ← NEW: ImgBB URL for auto-added props
    onBookClick     : () -> Unit,
    onCardClick     : () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // ── Image ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                // ══════════════════════════════════════════════════════════
                // IMAGE LOADING FIX
                //
                // networkImageUrl != null  → auto-added property
                //   Load from ImgBB URL using Coil AsyncImage.
                //   Fallback: havenhub logo while loading / on error.
                //
                // networkImageUrl == null  → manually seeded property
                //   Load from res/drawable using getPropertyImage(propertyId).
                //   This is the old behaviour — unchanged.
                // ══════════════════════════════════════════════════════════
                if (networkImageUrl != null) {
                    AsyncImage(
                        model = networkImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.havenhub),
                        placeholder = painterResource(R.drawable.havenhub)
                    )
                } else {
                    Image(
                        painter = painterResource(id = getPropertyImage(propertyId)),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Rating badge
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star, null,
                            tint = tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            " ${"%.1f".format(rating)}",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Price badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(primary.copy(0.9f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "PKR ${"%.0f".format(price)}/night",
                        color = onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // ── Info ───────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    location.uppercase(),
                    color = tertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))

                // Amenities
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (amenities.isEmpty()) {
                        Text(
                            "Basic Amenities Included",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        amenities.take(3).forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = tertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    feature,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Two buttons ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCardClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, primary)
                    ) {
                        Text(
                            "Details",
                            color = primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onBookClick,
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text(
                            "Reserve Your Stay",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}