package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
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
import com.example.havenhub.viewmodel.PropertyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val Navy      = Color(0xFF0D1B3E)
private val NavyLight = Color(0xFF1A2F5E)
private val Gold      = Color(0xFFD4AF37)
private val BgLight   = Color(0xFFF5F7FA)
private val Muted     = Color(0xFF8899AA)
private val White     = Color(0xFFFFFFFF)
private val Green     = Color(0xFF22C55E)
private val Red       = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    navController : NavController,
    propertyId    : String,
    viewModel     : PropertyViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val property  = uiState.propertyDetail
    val isLoading = uiState.isLoading

    var currentUserRole by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ✅ .trim() — trailing space se bachao
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(currentUserId).get().await()
                currentUserRole = doc.getString("role")?.trim() ?: ""
            } catch (e: Exception) { currentUserRole = "" }
        }
    }

    LaunchedEffect(propertyId) { viewModel.loadPropertyDetail(propertyId) }

    Box(modifier = Modifier.fillMaxSize().background(BgLight)) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Gold, strokeWidth = 3.dp)
                }
            }

            property != null -> {
                val isTenant   = currentUserRole.equals("tenant", ignoreCase = true)
                val isLandlord = currentUserRole.equals("landlord", ignoreCase = true)
                val isAdmin    = currentUserRole.equals("admin", ignoreCase = true)
                val isOwner    = property.ownerId == currentUserId
                val roleLoaded = currentUserRole.isNotEmpty()

                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 70.dp)
                ) {

                    // 1. HERO IMAGE
                    item {
                        Box(Modifier.fillMaxWidth().height(300.dp)) {
                            Image(
                                painter            = painterResource(
                                    id = getPropertyImage(
                                        property.drawableImageName.ifEmpty {
                                            property.resolvedDrawableName.ifEmpty { propertyId }
                                        }
                                    )
                                ),
                                contentDescription = null,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop
                            )
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        0f    to Navy.copy(0.55f),
                                        0.35f to Color.Transparent,
                                        1f    to Navy.copy(0.9f)
                                    )
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                                    .align(Alignment.TopStart)
                                    .size(40.dp)
                                    .shadow(6.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(White.copy(0.18f))
                                    .clickable { navController.popBackStack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White, modifier = Modifier.size(20.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (property.isAvailable) Green else Red)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(White))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        if (property.isAvailable) "Available" else "Unavailable",
                                        color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(
                                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp)
                            ) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(6.dp)).background(Gold)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(property.propertyTypeEnum.displayName(), color = Navy, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(property.title, color = White, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = Gold, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("${property.address}, ${property.city}", color = White.copy(0.85f), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 2. PRICE + RATING
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth().background(White)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Per night", fontSize = 11.sp, color = Muted)
                                Text(property.formattedPrice, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Navy)
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Navy)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("${property.averageRating}", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text("(${property.reviewCount})", color = White.copy(0.55f), fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider(color = BgLight, thickness = 6.dp)
                    }

                    // 3. QUICK STATS
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth().background(White).padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DetailStatItem("🛏️", "${property.bedrooms}", "Beds")
                            Box(Modifier.width(1.dp).height(40.dp).background(BgLight))
                            DetailStatItem("🚿", "${property.bathrooms}", "Baths")
                            Box(Modifier.width(1.dp).height(40.dp).background(BgLight))
                            DetailStatItem("👥", "${property.maxGuests}", "Guests")
                            Box(Modifier.width(1.dp).height(40.dp).background(BgLight))
                            DetailStatItem("📐", "${property.areaSqFt?.toInt() ?: "—"}", "Sqft")
                        }
                        HorizontalDivider(color = BgLight, thickness = 6.dp)
                    }

                    // 4. OWNER
                    item {
                        Row(
                            modifier          = Modifier.fillMaxWidth().background(White)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier         = Modifier.size(48.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Navy, NavyLight))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    property.ownerName.firstOrNull { it.isLetter() }?.uppercase() ?: "O",
                                    color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(property.ownerName.ifEmpty { "Property Owner" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Green, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Verified Owner", fontSize = 11.sp, color = Green)
                                }
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp)).background(Gold.copy(0.12f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("⭐ Host", fontSize = 11.sp, color = Gold, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = BgLight, thickness = 6.dp)
                    }

                    // 5. DESCRIPTION
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(White)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            SectionTitle("About this place")
                            Spacer(Modifier.height(10.dp))
                            Text(
                                property.description.ifEmpty { "A beautiful property waiting for you." },
                                fontSize = 14.sp, color = Color(0xFF4A5568), lineHeight = 23.sp
                            )
                        }
                        HorizontalDivider(color = BgLight, thickness = 6.dp)
                    }

                    // 6. AMENITIES
                    if (property.amenities.isNotEmpty()) {
                        item {
                            Column(
                                Modifier.fillMaxWidth().background(White)
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                SectionTitle("What's included")
                                Spacer(Modifier.height(12.dp))
                                property.amenities.chunked(2).forEach { pair ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { amenity ->
                                            Row(
                                                modifier          = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(BgLight)
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(13.dp))
                                                Spacer(Modifier.width(7.dp))
                                                Text(amenity, fontSize = 12.sp, color = Navy, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                            HorizontalDivider(color = BgLight, thickness = 6.dp)
                        }
                    }

                    // 7. HOUSE RULES
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(White)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            SectionTitle("House Rules")
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TimeCard(
                                    modifier = Modifier.weight(1f),
                                    emoji    = "🕐",
                                    label    = "Check-in",
                                    value    = property.checkInTime,
                                    bgColor  = Navy.copy(0.06f),
                                    valColor = Navy
                                )
                                TimeCard(
                                    modifier = Modifier.weight(1f),
                                    emoji    = "🕑",
                                    label    = "Check-out",
                                    value    = property.checkOutTime,
                                    bgColor  = Gold.copy(0.08f),
                                    valColor = Color(0xFFB8860B)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier          = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)).background(BgLight)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🌙", fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Text("Minimum stay", fontSize = 13.sp, color = Muted, modifier = Modifier.weight(1f))
                                Text(
                                    "${property.minNights} night${if (property.minNights > 1) "s" else ""}",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PolicyChip(Modifier.weight(1f), "🐾", "Pets",    property.petsAllowed)
                                PolicyChip(Modifier.weight(1f), "🚬", "Smoking", property.smokingAllowed)
                                PolicyChip(Modifier.weight(1f), "🎉", "Parties", property.partiesAllowed)
                            }
                        }
                        HorizontalDivider(color = BgLight, thickness = 6.dp)
                    }

                    // 8. LOCATION
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(White)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            SectionTitle("Location")
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)).background(BgLight)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Gold.copy(0.15f)),
                                    Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = Gold, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(property.city, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
                                    Text(property.address, fontSize = 12.sp, color = Muted)
                                }
                            }
                        }
                        HorizontalDivider(color = BgLight, thickness = 6.dp)
                    }

                    // 9. REVIEWS
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(White)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                SectionTitle("Reviews")
                                TextButton(onClick = { navController.navigate(Screen.ViewReviews.createRoute(propertyId)) }) {
                                    Text("See all", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(Navy, NavyLight)))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier            = Modifier.width(78.dp)
                                ) {
                                    Text("${property.averageRating}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Gold)
                                    Row {
                                        repeat(5) { i ->
                                            Icon(
                                                Icons.Default.Star, null,
                                                tint     = if (i < property.averageRating.toInt()) Gold else White.copy(0.25f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text("${property.reviewCount} reviews", fontSize = 10.sp, color = White.copy(0.55f))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    listOf(
                                        "Cleanliness" to 0.92f,
                                        "Location"    to 0.88f,
                                        "Value"       to (property.averageRating / 5f),
                                        "Comfort"     to 0.85f
                                    ).forEach { (label, frac) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(label, fontSize = 10.sp, color = White.copy(0.65f), modifier = Modifier.width(68.dp))
                                            Box(
                                                Modifier.weight(1f).height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)).background(White.copy(0.15f))
                                            ) {
                                                Box(
                                                    Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f))
                                                        .clip(RoundedCornerShape(2.dp)).background(Gold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // STICKY BOTTOM BAR
                Surface(
                    modifier        = Modifier.align(Alignment.BottomCenter),
                    shadowElevation = 12.dp,
                    color           = White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Per night", fontSize = 9.sp, color = Muted)
                            Text(
                                property.formattedPrice,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Black,
                                color      = Navy,
                                maxLines   = 1
                            )
                        }

                        // ✅ Message button — tenant only
                        if (roleLoaded && isTenant && property.ownerId.isNotEmpty() && !isOwner) {
                            OutlinedButton(
                                onClick        = {
                                    navController.navigate(
                                        Screen.Chat.createRoute(
                                            userId    = property.ownerId,
                                            ownerName = property.ownerName.ifEmpty { "Owner" }
                                            // ✅ propertyId pass nahi — simple chatId guarantee
                                        )
                                    )
                                },
                                modifier       = Modifier.height(38.dp),
                                shape          = RoundedCornerShape(10.dp),
                                border         = BorderStroke(1.dp, Navy),
                                colors         = ButtonDefaults.outlinedButtonColors(contentColor = Navy),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.Message, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    property.ownerName.ifEmpty { "Owner" }.split(" ").first(),
                                    fontWeight = FontWeight.Medium,
                                    fontSize   = 12.sp
                                )
                            }
                        }

                        // Book Now — tenant only
                        if (roleLoaded && isTenant && !isOwner) {
                            Button(
                                onClick        = { navController.navigate(Screen.Booking.createRoute(propertyId)) },
                                modifier       = Modifier.height(38.dp),
                                shape          = RoundedCornerShape(10.dp),
                                colors         = ButtonDefaults.buttonColors(
                                    containerColor = Navy,
                                    contentColor   = White
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                elevation      = ButtonDefaults.buttonElevation(2.dp)
                            ) {
                                Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Edit — landlord + owner only
                        if (roleLoaded && isLandlord && isOwner) {
                            Button(
                                onClick        = { navController.navigate(Screen.EditProperty.createRoute(propertyId)) },
                                modifier       = Modifier.height(38.dp),
                                shape          = RoundedCornerShape(10.dp),
                                colors         = ButtonDefaults.buttonColors(
                                    containerColor = Navy,
                                    contentColor   = Gold
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Edit Property", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            else -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏠", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Property not found", color = Muted, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ── Helper Composables ────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Gold))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy)
    }
}

@Composable
private fun DetailStatItem(emoji: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.padding(vertical = 12.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(label, fontSize = 10.sp, color = Muted)
    }
}

@Composable
private fun TimeCard(
    modifier: Modifier,
    emoji   : String,
    label   : String,
    value   : String,
    bgColor : Color,
    valColor: Color
) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valColor)
    }
}

@Composable
private fun PolicyChip(modifier: Modifier, emoji: String, label: String, allowed: Boolean) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (allowed) Green.copy(0.08f) else Red.copy(0.07f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(2.dp))
        Text(
            if (allowed) "Allowed" else "No",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = if (allowed) Green else Red
        )
    }
}

fun getStatusColor(status: String): Color = when (status.uppercase()) {
    "AVAILABLE"   -> Color(0xFF22C55E)
    "BOOKED"      -> Color(0xFFEF4444)
    "PENDING"     -> Color(0xFFF59E0B)
    "MAINTENANCE" -> Color(0xFF6B7280)
    else          -> Color(0xFF6B7280)
}

fun getStatusLabel(status: String): String = when (status.uppercase()) {
    "AVAILABLE"   -> "Available"
    "BOOKED"      -> "Booked"
    "PENDING"     -> "Pending"
    "MAINTENANCE" -> "Maintenance"
    else          -> status
}

@Composable
fun BadgeBox(label: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}