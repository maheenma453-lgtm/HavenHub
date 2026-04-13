package com.example.havenhub.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.PropertyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    navController: NavController,
    propertyId: String,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val property = uiState.propertyDetail
    val isLoading = uiState.isLoading

    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF0D1B3E)
                )
            }
            property != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // ── Photo ──
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                            Image(
                                painter = painterResource(id = getPropertyImage(propertyId)),
                                contentDescription = "Property Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF0D1B3E).copy(alpha = 0.5f), Color.Transparent)
                                        )
                                    )
                            )
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopStart)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF0D1B3E))
                            }

                            // Status badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(getStatusColor(property.status).copy(alpha = 0.9f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(getStatusLabel(property.status), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // Type badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0D1B3E).copy(alpha = 0.85f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(property.propertyTypeEnum.displayName(), fontSize = 11.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ── Title & Price ──
                    item {
                        Column(
                            modifier = Modifier
                                .background(Color.White)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = property.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D1B3E)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                                Text(" ${property.address}, ${property.city}", fontSize = 13.sp, color = Color(0xFF8899AA))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Price per night", fontSize = 12.sp, color = Color(0xFF8899AA))
                                    Text(property.formattedPrice, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                                        Text(" ${property.averageRating}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                    }
                                    Text("${property.reviewCount} reviews", fontSize = 12.sp, color = Color(0xFF8899AA))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ── Quick Stats ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("🛏", "Bedrooms", "${property.bedrooms}")
                                VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFEEEEEE))
                                StatItem("🚿", "Bathrooms", "${property.bathrooms}")
                                VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFEEEEEE))
                                StatItem("📐", "Area", "${property.areaSqFt ?: "-"} sqft")
                                VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFEEEEEE))
                                StatItem("👤", "Guests", "${property.maxGuests}")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ── Description ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("About this place", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = property.description,
                                    fontSize = 14.sp,
                                    color = Color(0xFF8899AA),
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ── Amenities ──
                    if (property.amenities.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Amenities", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(property.amenities) { amenity ->
                                            AmenityChip(amenity)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // ── House Rules ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("House Rules", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                Spacer(modifier = Modifier.height(12.dp))
                                RuleItem("Check-in time", property.checkInTime)
                                RuleItem("Check-out time", property.checkOutTime)
                                RuleItem("Minimum nights", "${property.minNights} night(s)")
                                RuleItem("Pets allowed", if (property.petsAllowed) "Yes" else "No")
                                RuleItem("Smoking allowed", if (property.smokingAllowed) "Yes" else "No")
                                RuleItem("Parties allowed", if (property.partiesAllowed) "Yes" else "No")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ── Location ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Location", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${property.address}, ${property.city}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF8899AA)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ── Reviews Section ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reviews", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                                    TextButton(onClick = {
                                        navController.navigate(Screen.ViewReviews.createRoute(propertyId))
                                    }) {
                                        Text("See All", color = Color(0xFFD4AF37), fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "${property.averageRating}",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D1B3E)
                                        )
                                        Text("${property.reviewCount} reviews", fontSize = 12.sp, color = Color(0xFF8899AA))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // ── Sticky Bottom Bar ──
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Price per night", fontSize = 11.sp, color = Color(0xFF8899AA))
                            Text(property.formattedPrice, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                        }
                        Button(
                            onClick = { navController.navigate(Screen.Booking.createRoute(propertyId)) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B3E))
                        ) {
                            Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Property not found", color = Color(0xFF8899AA))
                }
            }
        }
    }
}

@Composable
private fun RuleItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF8899AA))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B3E))
    }
}

@Composable
fun BadgeBox(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatItem(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
        Text(label, fontSize = 11.sp, color = Color(0xFF8899AA))
    }
}

@Composable
private fun AmenityChip(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F7FA))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF0D1B3E))
    }
}