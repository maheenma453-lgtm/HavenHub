package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.HomeViewModel
import com.example.havenhub.MainActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val NavyDeep        = Color(0xFF071020)
private val NavyPrime       = Color(0xFF0D1B3E)
private val GoldPrime       = Color(0xFFD4AF37)
private val GoldLight       = Color(0xFFF5D060)
private val D_BgDeep        = Color(0xFF060D1A)
private val D_BgCard        = Color(0xFF112038)
private val D_GoldPrimary   = Color(0xFFD4AF37)
private val D_TextPrimary   = Color(0xFFF0F4FF)
private val D_TextSecondary = Color(0xFF8899BB)

private const val PAKISTAN_CENTER_LAT = 30.3753
private const val PAKISTAN_CENTER_LNG = 69.3451

@Composable
fun ExploreMapScreen(
    navController : NavController,
    viewModel     : HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark by MainActivity.darkModeFlow.collectAsState()
    val context = LocalContext.current

    val goldP = if (isDark) D_GoldPrimary else GoldPrime
    val bgColor = if (isDark) D_BgDeep else Color(0xFFF0F4FA)
    val cardBg = if (isDark) D_BgCard else Color.White
    val textDk = if (isDark) D_TextPrimary else NavyPrime
    val textMtd = if (isDark) D_TextSecondary else Color(0xFF8899AA)

    var selectedProperty by remember { mutableStateOf<Property?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshHomeData()
    }

    Configuration.getInstance().userAgentValue = context.packageName

    val mapProperties: List<Triple<Property, Double, Double>> = remember(uiState.allProperties) {
        uiState.allProperties.mapNotNull { property ->
            if (!property.status.equals(PropertyStatus.APPROVED.name, ignoreCase = true)) {
                return@mapNotNull null
            }
            val lat = when {
                property.latitude != 0.0 -> property.latitude
                else -> Property.CITY_LATITUDES[property.city.lowercase().trim()]
                    ?: PAKISTAN_CENTER_LAT
            }
            val lng = when {
                property.longitude != 0.0 -> property.longitude
                else -> Property.CITY_LONGITUDES[property.city.lowercase().trim()]
                    ?: PAKISTAN_CENTER_LNG
            }
            if (lat == PAKISTAN_CENTER_LAT && lng == PAKISTAN_CENTER_LNG) {
                return@mapNotNull null
            }
            Triple(property, lat, lng)
        }
    }

    val startCenter = if (mapProperties.isNotEmpty())
        GeoPoint(mapProperties.first().second, mapProperties.first().third)
    else
        GeoPoint(PAKISTAN_CENTER_LAT, PAKISTAN_CENTER_LNG)

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(mapProperties) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        mapView.overlays.clear()
        mapProperties.forEach { (property, lat, lng) ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = property.title
                snippet = "${property.formattedPrice} • ${property.city}"
                setOnMarkerClickListener { _, _ ->
                    selectedProperty = property
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(if (mapProperties.isNotEmpty()) 6.0 else 5.5)
                    controller.setCenter(startCenter)
                    clipToOutline = true
                    mapViewRef.value = this
                }
            },
            update = { mapView ->
                mapViewRef.value = mapView
            }
        )

        // ─────────────────────────────────────────────────────────────────────
        // RESPONSIVE Top bar — back button left, title center, count badge right
        // Uses IntrinsicSize.Min for the title so it never pushes other items off
        // ─────────────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, goldP.copy(0.4f), CircleShape)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textDk,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Title — weight(1f) + padding so it scales between the two badges
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(NavyPrime, Color(0xFF1A3A6B))))
                    .border(1.dp, goldP.copy(0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Explore Nearby",
                    color = goldP,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, goldP.copy(0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    "${mapProperties.size} Stays",
                    color = textDk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = goldP, strokeWidth = 3.dp)
                        Text("Loading properties...", color = textMtd, fontSize = 13.sp)
                    }
                }
            }
        }

        // Empty state
        if (!uiState.isLoading && mapProperties.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No approved properties with location data found",
                            fontSize = 14.sp,
                            color = textMtd,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Properties submitted via app appear here after admin approval",
                            color = goldP.copy(0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // RESPONSIVE Bottom property card
        // NavigationBarsPadding ensures it clears gesture bars on all devices
        // ─────────────────────────────────────────────────────────────────────
        selectedProperty?.let { property ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        D_GoldPrimary.copy(0.9f),
                                        D_GoldPrimary.copy(0.3f),
                                        D_GoldPrimary.copy(0.9f)
                                    )
                                )
                            )
                    )
                }

                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                property.title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = textDk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = goldP,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    " ${property.city}",
                                    color = textMtd,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isDark) Color(0xFF1E2E50)
                                            else NavyPrime.copy(0.08f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        property.propertyTypeEnum.displayName(),
                                        fontSize = 10.sp,
                                        color = if (isDark) D_TextSecondary else NavyPrime,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isDark) D_BgCard else Color(0xFFF0F4FA))
                                .clickable { selectedProperty = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✕",
                                color = textMtd,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Price/night", fontSize = 10.sp, color = textMtd)
                            Text(
                                property.formattedPrice,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) D_GoldPrimary else NavyPrime,
                                fontSize = 17.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Star rating badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF1A1608) else Color(0xFFFFF8E1))
                                    .border(1.dp, goldP.copy(0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    null,
                                    tint = goldP,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    " ${property.averageRating}",
                                    fontSize = 12.sp,
                                    color = textDk,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // View Detail button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(GoldPrime, GoldLight))
                                    )
                                    .clickable {
                                        selectedProperty = null
                                        navController.navigate(
                                            Screen.PropertyDetail.createRoute(property.propertyId)
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "View Detail →",
                                    color = NavyPrime,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${property.bedrooms} beds  •  ${property.maxGuests} guests max",
                        color = textMtd,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}