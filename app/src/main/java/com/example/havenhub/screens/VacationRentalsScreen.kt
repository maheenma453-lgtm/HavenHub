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

// ── Dark Navy + Gold palette (hardcoded, no theme dependency) ─────────────────
private val VR_NavyDeep   = Color(0xFF060E20)
private val VR_NavyMid    = Color(0xFF0D1B3E)
private val VR_NavyLight  = Color(0xFF1A3A6B)
private val VR_Gold       = Color(0xFFD4AF37)
private val VR_GoldLight  = Color(0xFFF5D060)
private val VR_GoldDim    = Color(0xFFB8962E)
private val VR_White      = Color(0xFFFFFFFF)
private val VR_WhiteDim   = Color(0xCCFFFFFF)
private val VR_WhiteFaint = Color(0x55FFFFFF)

private val VR_NavyGradient = Brush.verticalGradient(
    listOf(VR_NavyDeep, VR_NavyMid, VR_NavyLight)
)
private val VR_GoldGradient = Brush.horizontalGradient(
    listOf(VR_Gold.copy(0.9f), VR_GoldLight.copy(0.6f), VR_Gold.copy(0.9f))
)
private val VR_GoldBorder = Brush.horizontalGradient(
    listOf(VR_Gold.copy(0.8f), VR_GoldLight.copy(0.5f), VR_Gold.copy(0.8f))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationRentalsScreen(
    navController  : NavController,
    initialSeason  : String? = null,
    initialLocation: String? = null,
    viewModel      : VacationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedCategory by remember {
        mutableStateOf(
            if (!initialLocation.isNullOrEmpty() && initialLocation != "none") initialLocation else "All"
        )
    }

    val filteredProperties = if (selectedCategory == "All") {
        uiState.properties
    } else {
        uiState.properties.filter {
            it.city.equals(selectedCategory, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadVacationProperties() }

    // Keep rest of UI using theme colors
    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val background       = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            // ── TOP BAR — Dark Navy + Gold ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VR_NavyGradient)
            ) {
                // Decorative gold bottom line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(VR_GoldBorder)
                )
                // Subtle circle decoration top-right
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(VR_Gold.copy(0.06f))
                )

                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(VR_Gold.copy(0.12f))
                            .border(1.5.dp, VR_GoldBorder, CircleShape)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint     = VR_Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))

                    // Title
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "VACATION HUB",
                            color         = VR_Gold,
                            fontSize      = 15.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Northern Pakistan Stays",
                            color    = VR_WhiteDim,
                            fontSize = 11.sp
                        )
                    }

                    // Count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(VR_Gold.copy(0.15f))
                            .border(1.dp, VR_GoldBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${filteredProperties.size} Stays",
                            color      = VR_Gold,
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
            // ── Hero Banner — Dark Navy + Gold ────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(VR_NavyGradient)
                ) {
                    // Decorative mountain silhouette
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.07f)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height)
                            lineTo(size.width * 0.2f, size.height * 0.4f)
                            lineTo(size.width * 0.5f, size.height * 0.72f)
                            lineTo(size.width * 0.75f, size.height * 0.28f)
                            lineTo(size.width, size.height * 0.55f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }

                    // Decorative gold accent circles
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 60.dp, y = 60.dp)
                            .clip(CircleShape)
                            .background(VR_Gold.copy(0.05f))
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.TopStart)
                            .offset(x = (-20).dp, y = (-20).dp)
                            .clip(CircleShape)
                            .background(VR_Gold.copy(0.06f))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Premium badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VR_Gold.copy(0.18f))
                                .border(1.dp, VR_GoldBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "PREMIUM SELECTION",
                                color      = VR_Gold,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Your Gateway to\nthe North",
                            color      = VR_White,
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 31.sp
                        )

                        Spacer(Modifier.height(18.dp))

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
                                val isSelected = city == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) VR_GoldGradient
                                            else Brush.linearGradient(
                                                listOf(VR_White.copy(0.10f), VR_White.copy(0.08f))
                                            )
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            brush = if (isSelected) VR_GoldBorder
                                            else Brush.horizontalGradient(
                                                listOf(VR_WhiteFaint, VR_WhiteFaint)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedCategory = city }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        city,
                                        color      = if (isSelected) VR_NavyDeep else VR_WhiteDim,
                                        fontSize   = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
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
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Property Cards ────────────────────────────────────────────
            else {
                items(filteredProperties) { prop ->
                    val networkImageUrl = prop.imageUrls.firstOrNull()
                        ?.takeIf { it.startsWith("http") }

                    VacationPropertyCard(
                        propertyId      = prop.propertyId,
                        title           = prop.title,
                        location        = prop.city,
                        price           = prop.pricePerNight,
                        rating          = prop.averageRating.toDouble(),
                        amenities       = prop.amenities,
                        networkImageUrl = networkImageUrl,
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

// ── Property Card — unchanged ─────────────────────────────────────────────────
@Composable
fun VacationPropertyCard(
    propertyId      : String,
    title           : String,
    location        : String,
    price           : Double,
    rating          : Double,
    amenities       : List<String>,
    networkImageUrl : String?,
    onBookClick     : () -> Unit,
    onCardClick     : () -> Unit
) {
    val primary   = MaterialTheme.colorScheme.primary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onCardClick() },
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                if (networkImageUrl != null) {
                    AsyncImage(
                        model              = networkImageUrl,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop,
                        error              = painterResource(R.drawable.havenhub),
                        placeholder        = painterResource(R.drawable.havenhub)
                    )
                } else {
                    Image(
                        painter            = painterResource(id = getPropertyImage(propertyId)),
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
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
                            tint     = tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            " ${"%.1f".format(rating)}",
                            color      = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize   = 12.sp,
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
                        color      = onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp
                    )
                }
            }

            // Info
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    location.uppercase(),
                    color      = tertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 10.sp
                )
                Text(
                    title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 18.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))

                // Amenities
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (amenities.isEmpty()) {
                        Text(
                            "Basic Amenities Included",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        amenities.take(3).forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint     = tertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    feature,
                                    fontSize = 11.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Two buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onCardClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.5.dp, primary)
                    ) {
                        Text(
                            "Details",
                            color      = primary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp
                        )
                    }

                    Button(
                        onClick  = onBookClick,
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor   = onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text(
                            "Reserve Your Stay",
                            fontWeight = FontWeight.Black,
                            fontSize   = 14.sp
                        )
                    }
                }
            }
        }
    }
}