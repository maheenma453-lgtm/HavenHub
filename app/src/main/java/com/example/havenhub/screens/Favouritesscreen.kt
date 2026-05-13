package com.example.havenhub.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.HomeViewModel

// ── Light Theme Design Tokens ─────────────────────────────────────────────────
private val NavyDeep  = Color(0xFF0D1B3E)
private val NavyMid   = Color(0xFF1A3A6B)
private val GoldPrime = Color(0xFFD4AF37)
private val GoldLight = Color(0xFFF5D060)
private val GoldFaint = Color(0xFFFFF8E1)
private val PageBg    = Color(0xFFF0F3F9)
private val TextMuted = Color(0xFF8899AA)
private val RedFav    = Color(0xFFE53935)

// ── Dark Theme Tokens ─────────────────────────────────────────────────────────
private val D_FavPageBg    = Color(0xFF060D1A)    // deep navy page bg
private val D_FavCardBg    = Color(0xFF112038)    // card surface
private val D_FavNavy      = Color(0xFF0D1B3E)    // header / avatar bg
private val D_FavNavyMid   = Color(0xFF122040)    // header gradient end
private val D_FavGold      = Color(0xFFD4AF37)    // gold accent
private val D_FavGoldLight = Color(0xFFF5D060)    // light gold
private val D_FavGoldFaint = Color(0xFF1A1608)    // very faint gold bg
private val D_FavTextPri   = Color(0xFFF0F4FF)    // primary text
private val D_FavTextSec   = Color(0xFF8899BB)    // secondary text
private val D_FavGreen     = Color(0xFF3DCC7A)    // availability dot
private val D_FavRed       = Color(0xFFE53935)    // favourite heart (same)

@Composable
fun FavouritesScreen(
    navController: NavController,
    viewModel    : HomeViewModel = hiltViewModel()
) {
    // ── Dark theme detection ──────────────────────────────────────────────────
    val isDark = isSystemInDarkTheme()

    // ── Theme-aware aliases ───────────────────────────────────────────────────
    val pageBg   = if (isDark) D_FavPageBg  else PageBg
    val headerBg = if (isDark)
        Brush.verticalGradient(listOf(D_FavNavy, D_FavNavyMid))
    else
        Brush.verticalGradient(listOf(NavyDeep, NavyMid))
    val goldC    = if (isDark) D_FavGold    else GoldPrime
    val goldL    = if (isDark) D_FavGoldLight else GoldLight
    val textMtd  = if (isDark) D_FavTextSec else TextMuted

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFavouriteProperties()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(pageBg)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().background(headerBg)
        ) {
            // Gold shimmer line at bottom
            Box(
                Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
                    .background(Brush.horizontalGradient(
                        listOf(Color.Transparent, goldC, goldL, goldC, Color.Transparent)
                    ))
            )

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .statusBarsPadding(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("My Favourites", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = if (uiState.favouriteProperties.isEmpty()) "No saved properties"
                        else "${uiState.favouriteProperties.size} saved properties",
                        color    = goldC,
                        fontSize = 11.sp
                    )
                }

                // Heart icon badge
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(D_FavRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, null, tint = D_FavRed, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Content ──────────────────────────────────────────────────────────
        when {
            uiState.isFavouritesLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = goldC, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading your favourites...", color = textMtd, fontSize = 14.sp)
                    }
                }
            }

            uiState.favouriteProperties.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(100.dp).clip(CircleShape)
                                .background(Brush.radialGradient(
                                    listOf(D_FavRed.copy(alpha = 0.15f), D_FavRed.copy(alpha = 0.05f))
                                )),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FavoriteBorder, null,
                                tint = D_FavRed.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                        }

                        Spacer(Modifier.height(24.dp))

                        Text(
                            "No Favourites Yet",
                            color      = if (isDark) D_FavTextPri else NavyDeep,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Properties jinhe aap pasand karo unhe heart icon se save karo",
                            color     = textMtd,
                            fontSize  = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(28.dp))

                        // Browse button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(
                                    if (isDark)
                                        listOf(D_FavNavy, D_FavNavyMid)
                                    else
                                        listOf(NavyDeep, NavyMid)
                                ))
                                .clickable { navController.navigate(Screen.Home.route) }
                                .padding(horizontal = 28.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Home, null, tint = goldC, modifier = Modifier.size(18.dp))
                                Text("Browse Properties", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(vertical = 16.dp, horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.favouriteProperties,
                        key   = { it.propertyId }
                    ) { property ->
                        FavouritePropertyCard(
                            property      = property,
                            onUnfavourite = { viewModel.toggleFavourite(property.propertyId) },
                            isDark        = isDark,
                            onClick       = {
                                navController.navigate(
                                    Screen.PropertyDetail.createRoute(property.propertyId)
                                )
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ── Image helper — remote URL first, then drawable fallback ──────────────────
@Composable
private fun FavPropertyImage(property: Property, modifier: Modifier = Modifier) {
    val remoteUrl = property.imageUrls.firstOrNull { it.isNotBlank() }
    if (!remoteUrl.isNullOrEmpty()) {
        coil.compose.AsyncImage(
            model              = remoteUrl,
            contentDescription = property.title,
            modifier           = modifier,
            contentScale       = ContentScale.Crop
        )
    } else {
        Image(
            painter            = painterResource(id = getPropertyImage(
                property.drawableImageName.ifEmpty { property.propertyId }
            )),
            contentDescription = property.title,
            modifier           = modifier,
            contentScale       = ContentScale.Crop
        )
    }
}

// ── Favourite Property Card — dark theme aware ───────────────────────────────
@Composable
private fun FavouritePropertyCard(
    property     : Property,
    onUnfavourite: () -> Unit,
    isDark       : Boolean   = false,
    onClick      : () -> Unit
) {
    // Theme-aware colors for this card
    val cardBg   = if (isDark) D_FavCardBg    else Color.White
    val navyBg   = if (isDark) D_FavNavy      else NavyDeep
    val navyMidC = if (isDark) D_FavNavyMid   else NavyMid
    val goldC    = if (isDark) D_FavGold      else GoldPrime
    val goldF    = if (isDark) D_FavGoldFaint else GoldFaint
    val textPri  = if (isDark) D_FavTextPri   else NavyDeep
    val textSec  = if (isDark) D_FavTextSec   else TextMuted
    val greenC   = if (isDark) D_FavGreen     else Color(0xFF4CAF50)

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 4.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Property image
            Box(modifier = Modifier.size(95.dp).clip(RoundedCornerShape(14.dp))) {
                FavPropertyImage(property = property, modifier = Modifier.fillMaxSize())
                // Availability dot
                if (property.isAvailable) {
                    Box(
                        Modifier.align(Alignment.TopStart).padding(6.dp).size(8.dp)
                            .clip(CircleShape).background(greenC)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Property info
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Text(
                        property.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = textPri,
                        modifier   = Modifier.weight(1f)
                    )
                    // Unfavourite button
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(D_FavRed.copy(alpha = 0.1f))
                            .clickable { onUnfavourite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = D_FavRed, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = goldC, modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", color = textSec, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KingBed, null, tint = textSec, modifier = Modifier.size(11.dp))
                    Text(" ${property.bedrooms} beds", fontSize = 11.sp, color = textSec)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.People, null, tint = textSec, modifier = Modifier.size(11.dp))
                    Text(" ${property.maxGuests} guests", fontSize = 11.sp, color = textSec)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            property.formattedPrice,
                            fontWeight = FontWeight.ExtraBold,
                            color      = textPri,
                            fontSize   = 14.sp
                        )
                        Text("/night", color = textSec, fontSize = 9.sp)
                    }

                    // Rating badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(goldF)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = goldC, modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = textPri, fontWeight = FontWeight.Bold)
                        Text(" (${property.reviewCount})", fontSize = 10.sp, color = textSec)
                    }
                }
            }
        }

        // Book Now footer bar
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(navyBg, navyMidC)))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = goldC, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Details & Book", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}