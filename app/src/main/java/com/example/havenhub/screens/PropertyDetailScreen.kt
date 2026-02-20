package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.PropertyViewModel

// ─────────────────────────────────────────────────────────────────
// PropertyDetailScreen.kt
// PURPOSE : Full property details with photo gallery, amenities,
//           location, owner info, and reviews.
//           User can book, contact owner, or save to wishlist.
// PARAMETERS: propertyId (passed via navigation)
// NAVIGATION:
//   → BookingScreen
//   → ChatScreen (contact owner)
//   → ViewReviewsScreen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    navController : NavController,
    propertyId    : String,                          // Received from NavGraph
    viewModel     : PropertyViewModel = hiltViewModel()
) {

    // ── Load property details on screen open ─────────────────────
    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
    }

    val property  by viewModel.selectedProperty.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Wishlist toggle state
    var isSaved by remember { mutableStateOf(false) }

    // ── UI ────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(BackgroundWhite)) {

        if (isLoading || property == null) {
            // ── Loading State ────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            property?.let { prop ->

                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)  // Space for bottom book button
                ) {

                    // ── Photo Gallery Section ─────────────────────
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {

                            // Main image placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SurfaceGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🏠", fontSize = 80.sp)
                            }

                            // Gradient overlay at top (for back arrow visibility)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                AccentNavy.copy(alpha = 0.5f),
                                                AccentNavy.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )

                            // Back Button (top left)
                            IconButton(
                                onClick  = { navController.popBackStack() },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopStart)
                                    .clip(CircleShape)
                                    .background(BackgroundWhite.copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint               = TextPrimary
                                )
                            }

                            // Wishlist Button (top right)
                            IconButton(
                                onClick  = { isSaved = !isSaved },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(BackgroundWhite.copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Wishlist",
                                    tint = if (isSaved) ErrorRed else TextSecondary
                                )
                            }

                            // Photo count badge (bottom right)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentNavy.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "📷 6 Photos", fontSize = 12.sp, color = BackgroundWhite)
                            }
                        }
                    }

                    // ── Title & Basic Info ────────────────────────
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {

                            // Verified badge + Type chip
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (prop.isVerified) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SuccessGreen.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("✓ Verified", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(prop.type, fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(text = prop.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${prop.address}, ${prop.city}", fontSize = 14.sp, color = TextSecondary)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ── Price + Rating Row ────────────────
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.Bottom
                            ) {
                                Column {
                                    Text(text = "Monthly Rent", fontSize = 12.sp, color = TextSecondary)
                                    Text(
                                        text       = "PKR ${prop.price}",
                                        fontSize   = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = PrimaryBlue
                                    )
                                    Text(text = "per month", fontSize = 12.sp, color = TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "⭐", fontSize = 16.sp)
                                        Text(text = " ${prop.rating}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(text = "${prop.reviewCount} reviews", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderGray)
                    }

                    // ── Quick Stats (Beds, Baths, Size) ───────────
                    item {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(icon = "🛏", label = "Bedrooms", value = "${prop.bedrooms}")
                            StatItem(icon = "🚿", label = "Bathrooms", value = "${prop.bathrooms}")
                            StatItem(icon = "📐", label = "Area", value = "${prop.areaSqFt} sqft")
                            StatItem(icon = "🏢", label = "Floor", value = "${prop.floor}")
                        }
                        Divider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderGray)
                    }

                    // ── Description ────────────────────────────────
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Description", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text      = prop.description,
                                fontSize  = 14.sp,
                                color     = TextSecondary,
                                lineHeight = 21.sp
                            )
                        }
                        Divider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderGray)
                    }

                    // ── Amenities ──────────────────────────────────
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Amenities", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(prop.amenities) { amenity ->
                                    AmenityChip(label = amenity)
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderGray)
                    }

                    // ── Owner Info ─────────────────────────────────
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Property Owner", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Owner avatar
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "👤", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = prop.ownerName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Member since 2023", fontSize = 12.sp, color = TextSecondary)
                                }
                                // Chat button
                                OutlinedButton(
                                    onClick = { navController.navigate(Screen.Chat.createRoute(prop.ownerId)) },
                                    shape   = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Message, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chat", fontSize = 13.sp)
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderGray)
                    }

                    // ── Reviews Preview ────────────────────────────
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text("Reviews (${prop.reviewCount})", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { navController.navigate(Screen.ViewReviews.createRoute(propertyId)) }) {
                                    Text("See All", color = PrimaryBlue)
                                }
                            }
                        }
                    }
                }

                // ── Sticky Bottom: Book Now Button ─────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { navController.navigate(Screen.Booking.createRoute(propertyId)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(
                            text       = "Book Now",
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── Stat item composable ───────────────────────────────────────────
@Composable
private fun StatItem(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
    }
}

// ── Amenity chip composable ───────────────────────────────────────
@Composable
private fun AmenityChip(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceGray)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "✓", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 13.sp, color = TextPrimary)
    }
}


