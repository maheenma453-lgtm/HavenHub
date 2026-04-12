package com.example.havenhub.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.PropertyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    navController : NavController,
    propertyId    : String,
    viewModel     : PropertyViewModel = hiltViewModel(),
    authViewModel : AuthViewModel     = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val property  = uiState.propertyDetail
    val isLoading = uiState.isLoading

    val currentUid = authUiState.currentUser?.uid ?: ""
    val userRole   = authUiState.userRole
    val isTenant   = userRole == "tenant"
    val isLandlord = userRole == "landlord"
    val isOwner    = isLandlord && property?.ownerId == currentUid

    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = PrimaryBlue
                )
            }
            property != null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        // ── Photo Gallery ──
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                                AsyncImage(
                                    model              = property.coverImageUrl,
                                    contentDescription = "Property Image",
                                    modifier           = Modifier.fillMaxSize(),
                                    contentScale       = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(PrimaryBlueDark.copy(alpha = 0.4f), Color.Transparent)
                                            )
                                        )
                                )
                                IconButton(
                                    onClick  = { navController.popBackStack() },
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.TopStart)
                                        .clip(CircleShape)
                                        .background(BackgroundWhite.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        "Back",
                                        tint = TextPrimary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryBlueDark.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text     = "${property.imageUrls.size} Photos",
                                        fontSize = 12.sp,
                                        color    = BackgroundWhite
                                    )
                                }
                            }
                        }

                        // ── Title & Price ──
                        item {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (property.isAvailable) {
                                        BadgeBox("Available", SuccessGreen)
                                    }
                                    BadgeBox(property.propertyType, PrimaryBlue)

                                    if (isOwner) {
                                        when (property.status) {
                                            PropertyStatus.PENDING.name ->
                                                BadgeBox("Approval Pending", Color(0xFFFF9800))
                                            PropertyStatus.REJECTED.name ->
                                                BadgeBox("Rejected", Color(0xFFE53935))
                                            PropertyStatus.APPROVED.name ->
                                                BadgeBox("Approved", SuccessGreen)
                                            else -> {}
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text       = property.title,
                                    fontSize   = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        null,
                                        tint     = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "${property.address}, ${property.city}",
                                        fontSize = 14.sp,
                                        color    = TextSecondary
                                    )
                                }

                                if (isOwner &&
                                    property.status == PropertyStatus.REJECTED.name &&
                                    property.adminNote.isNotEmpty()
                                ) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFEBEE)
                                        )
                                    ) {
                                        Row(
                                            modifier              = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Info, null, tint = Color(0xFFE53935))
                                            Text(
                                                text     = "Admin note: ${property.adminNote}",
                                                fontSize = 13.sp,
                                                color    = Color(0xFFB71C1C)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Price per night", fontSize = 12.sp, color = TextSecondary)
                                        Text(
                                            property.formattedPrice,
                                            fontSize   = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = PrimaryBlue
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${property.averageRating}",
                                            fontSize   = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${property.reviewCount} reviews",
                                            fontSize = 12.sp,
                                            color    = TextSecondary
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color    = BorderGray
                            )
                        }

                        // ── Quick Stats ──
                        item {
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("Bedrooms",  "${property.bedrooms}")
                                StatItem("Bathrooms", "${property.bathrooms}")
                                StatItem("Area",      "${property.areaSqFt ?: "-"} sqft")
                                StatItem("Guests",    "${property.maxGuests}")
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color    = BorderGray
                            )
                        }

                        // ── Description ──
                        item {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Description", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text       = property.description,
                                    fontSize   = 14.sp,
                                    color      = TextSecondary,
                                    lineHeight = 21.sp
                                )
                            }
                        }

                        // ── Amenities ──
                        item {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical   = 10.dp
                                )
                            ) {
                                Text("Amenities", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(property.amenities) { amenity ->
                                        AmenityChip(amenity)
                                    }
                                }
                            }
                        }
                    }

                    // ── Sticky Bottom Bar ──
                    Surface(
                        modifier        = Modifier.align(Alignment.BottomCenter),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundWhite)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when {
                                // ✅ Tenant — APPROVED property
                                isTenant && property.status == PropertyStatus.APPROVED.name -> {
                                    // ✅ Message Owner button
                                    OutlinedButton(
                                        onClick  = {
                                            navController.navigate(
                                                Screen.Chat.createRoute(property.ownerId)
                                            )
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape    = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Message,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Message")
                                    }
                                    // ✅ Book Now button
                                    Button(
                                        onClick  = {
                                            navController.navigate(
                                                Screen.Booking.createRoute(propertyId)
                                            )
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape    = RoundedCornerShape(12.dp),
                                        colors   = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryBlue
                                        )
                                    ) {
                                        Text(
                                            "Book Now",
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 16.sp
                                        )
                                    }
                                }

                                // ✅ Tenant — property approved nahi
                                isTenant && property.status != PropertyStatus.APPROVED.name -> {
                                    Button(
                                        onClick  = {},
                                        enabled  = false,
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape    = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "Not Available",
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 16.sp
                                        )
                                    }
                                }

                                // ✅ Apni property — edit aur manage
                                isOwner -> {
                                    OutlinedButton(
                                        onClick  = {
                                            navController.navigate(
                                                Screen.EditProperty.createRoute(propertyId)
                                            )
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape    = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit")
                                    }
                                    Button(
                                        onClick  = {
                                            navController.navigate(Screen.MyBookings.route)
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape    = RoundedCornerShape(12.dp),
                                        colors   = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryBlue
                                        )
                                    ) {
                                        Text("Manage", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // ✅ Doosre landlord ki property
                                else -> {}
                            }
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Property not found")
                }
            }
        }
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
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun AmenityChip(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantLight)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            null,
            tint     = SuccessGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 13.sp)
    }
}