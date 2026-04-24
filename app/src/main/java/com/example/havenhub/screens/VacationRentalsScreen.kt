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
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.VacationViewModel

// ── Design Tokens ───────────────────────────────────────────
private val VNavy      = Color(0xFF0D1B3E)
private val VNavyMid   = Color(0xFF1A2F5E)
private val VNavyDeep  = Color(0xFF071020)
private val VGold      = Color(0xFFD4AF37)
private val VBg        = Color(0xFFF8FAFC)
private val VMuted     = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationRentalsScreen(
    navController: NavController,
    viewModel: VacationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── NEW: Selected category state ──
    var selectedCategory by remember { mutableStateOf("All") }

    // ── NEW: Filtered properties based on selected city ──
    val filteredProperties = if (selectedCategory == "All") {
        uiState.properties
    } else {
        uiState.properties.filter {
            it.city.equals(selectedCategory, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadVacationProperties() }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(VNavyDeep, VNavyMid)))
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
                            .background(Color.White.copy(0.10f))
                            .border(1.dp, VGold.copy(0.45f), CircleShape)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VGold, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("VACATION HUB", color = VGold, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Northern Pakistan Stays", color = Color.White.copy(0.50f), fontSize = 11.sp)
                    }

                    // ── UPDATED: Count badge now shows filtered count ──
                    Surface(
                        color = VGold.copy(0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, VGold.copy(0.45f))
                    ) {
                        Text(
                            "${filteredProperties.size} Stays",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = VGold, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = VBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(Brush.verticalGradient(listOf(VNavyMid, VBg)))
                ) {
                    Canvas(modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.08f)) {
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
                        Surface(color = VGold.copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "  PREMIUM SELECTION  ",
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = VGold, fontSize = 10.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your Gateway to\nthe North",
                            color = VNavyDeep, fontSize = 28.sp,
                            fontWeight = FontWeight.Black, lineHeight = 32.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        // ── UPDATED: City filter chips with working click ──
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = listOf("All", "Islamabad", "Hunza", "Naran", "Skardu", "Swat")
                            categories.forEach { city ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        selectedCategory = city  // ── FIXED: ab click kaam karega
                                    },
                                    color = if (city == selectedCategory) VNavy else Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        city,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = if (city == selectedCategory) Color.White else VNavyMid,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(4.dp, 18.dp)
                                .background(VGold, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        // ── UPDATED: Section title changes with selected city ──
                        Text(
                            if (selectedCategory == "All") "Nearby Stays" else "$selectedCategory Stays",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = VNavy
                        )
                    }
                    Text("View Map", color = VGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VGold)
                    }
                }
            } else if (filteredProperties.isEmpty()) {
                // ── NEW: Empty state jab koi property na mile ──
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = VMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No stays found in $selectedCategory",
                                color = VMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Try selecting a different city",
                                color = VMuted.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                // ── UPDATED: filteredProperties use ho rahi hain ab ──
                items(filteredProperties) { prop ->
                    DynamicPropertyCard(
                        propertyId = prop.propertyId,
                        title = prop.title,
                        location = prop.city,
                        price = prop.pricePerNight,
                        rating = prop.averageRating.toDouble(),
                        amenities = prop.amenities ?: emptyList(),
                        onClick = {
                            navController.navigate(Screen.PropertyDetail.createRoute(prop.propertyId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicPropertyCard(
    propertyId: String, title: String, location: String,
    price: Double, rating: Double, amenities: List<String>, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)) {
                Image(
                    painter = painterResource(id = getPropertyImage(propertyId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    color = Color.Black.copy(0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = VGold, modifier = Modifier.size(14.dp))
                        Text(
                            " ${"%.1f".format(rating)}",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(VNavy.copy(0.9f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "PKR ${"%.0f".format(price)}",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(location.uppercase(), color = VGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text(
                    title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                    color = VNavy, maxLines = 1, overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (amenities.isEmpty()) {
                        Text("Basic Amenities Included", fontSize = 11.sp, color = VMuted)
                    } else {
                        amenities.take(3).forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = VGold, modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(feature, fontSize = 11.sp, color = VMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VNavy,
                        contentColor = VGold
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Reserve Your Stay", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    }
}