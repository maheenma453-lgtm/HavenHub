package com.example.havenhub.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.PropertyViewModel
import com.example.havenhub.viewmodel.VacationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Semantic colors — intentional, do NOT theme these
private val Green  = Color(0xFF22C55E)
private val Red    = Color(0xFFEF4444)
private val Orange = Color(0xFFF97316)   // ✅ NEW: Already Booked banner color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    navController    : NavController,
    propertyId       : String,
    viewModel        : PropertyViewModel  = hiltViewModel(),
    vacationViewModel: VacationViewModel  = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val vacState  by vacationViewModel.uiState.collectAsState()
    val property  = uiState.propertyDetail
    val isLoading = uiState.isLoading

    // ✅ NEW: Booking status state
    // null = check abhi nahi hua, true = booked, false = available
    val isPropertyBooked    = uiState.isPropertyCurrentlyBooked
    val isCheckingBooking   = uiState.isCheckingBooking

    val configuration = LocalConfiguration.current
    val screenWidth   = configuration.screenWidthDp.dp

    var currentUserRole by remember { mutableStateOf("") }
    val currentUserId   = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(currentUserId).get().await()
                currentUserRole = doc.getString("role")?.trim() ?: ""
            } catch (e: Exception) { currentUserRole = "" }
        }
    }

    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
        vacationViewModel.loadPackagesForProperty(propertyId)
        // ✅ NEW: Property khulte hi booking status check karo
        viewModel.checkPropertyBookingStatus(propertyId)
    }

    val isTenant   = currentUserRole.equals("tenant",   ignoreCase = true)
    val isLandlord = currentUserRole.equals("landlord", ignoreCase = true)
    val roleLoaded = currentUserRole.isNotEmpty()

    val hasActivePackage = vacState.propertyPackages.isNotEmpty()

    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = tertiary, strokeWidth = 3.dp)
                }
            }

            property != null -> {
                val isOwner = property.ownerId == currentUserId

                val bottomPad = when {
                    roleLoaded && isTenant && !isOwner && hasActivePackage -> 140.dp
                    roleLoaded && isTenant && !isOwner                     -> 90.dp
                    else                                                   -> 110.dp
                }

                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomPad)
                ) {

                    // ── 1. HERO IMAGE ──────────────────────────────────────────
                    item {
                        val heroHeight = (screenWidth.value * 0.72f).dp.coerceIn(220.dp, 360.dp)

                        Box(Modifier.fillMaxWidth().height(heroHeight)) {

                            val remoteUrl = property.imageUrls.firstOrNull { it.startsWith("http") }

                            when {
                                !remoteUrl.isNullOrEmpty() -> {
                                    AsyncImage(
                                        model              = remoteUrl,
                                        contentDescription = property.title,
                                        modifier           = Modifier.fillMaxSize(),
                                        contentScale       = ContentScale.Crop
                                    )
                                }
                                property.drawableImageName.isNotBlank() -> {
                                    Image(
                                        painter            = painterResource(id = getPropertyImage(property.drawableImageName)),
                                        contentDescription = property.title,
                                        modifier           = Modifier.fillMaxSize(),
                                        contentScale       = ContentScale.Crop
                                    )
                                }
                                property.resolvedDrawableName.isNotBlank() -> {
                                    Image(
                                        painter            = painterResource(id = getPropertyImage(property.resolvedDrawableName)),
                                        contentDescription = property.title,
                                        modifier           = Modifier.fillMaxSize(),
                                        contentScale       = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Image(
                                        painter            = painterResource(id = getPropertyImage(propertyId)),
                                        contentDescription = property.title,
                                        modifier           = Modifier.fillMaxSize(),
                                        contentScale       = ContentScale.Crop
                                    )
                                }
                            }

                            // Gradient overlay
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        0f    to primary.copy(0.55f),
                                        0.35f to Color.Transparent,
                                        1f    to primary.copy(0.9f)
                                    )
                                )
                            )

                            // Back button
                            Box(
                                modifier = Modifier
                                    .statusBarsPadding().padding(16.dp).align(Alignment.TopStart)
                                    .size(40.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                                    .background(onPrimary.copy(0.18f)).clickable { navController.popBackStack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = onPrimary, modifier = Modifier.size(20.dp))
                            }

                            // ✅ UPDATED: Availability badge — ab booking check bhi include hai
                            // Priority: isPropertyBooked > property.isAvailable
                            Box(
                                modifier = Modifier
                                    .statusBarsPadding().padding(16.dp).align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        when {
                                            isPropertyBooked == true -> Orange  // Actively booked
                                            property.isAvailable     -> Green   // Available
                                            else                     -> Red     // Unavailable/Pending
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(onPrimary))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = when {
                                            isCheckingBooking        -> "Checking..."
                                            isPropertyBooked == true -> "Booked"
                                            property.isAvailable     -> "Available"
                                            else                     -> "Unavailable"
                                        },
                                        color = onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Title overlay
                            Column(
                                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 16.dp)
                            ) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(6.dp)).background(tertiary)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(property.propertyTypeEnum.displayName(), color = primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(property.title, color = onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = tertiary, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("${property.address}, ${property.city}", color = onPrimary.copy(0.85f), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // ✅ NEW: ALREADY BOOKED BANNER
                    // Sirf tenants ko dikhao jab property booked ho
                    // Landlord/owner ko nahi dikhega — unhe pata hota hai
                    item {
                        AnimatedVisibility(
                            visible = isPropertyBooked == true && roleLoaded && isTenant && property.ownerId != currentUserId,
                            enter   = fadeIn() + expandVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Orange.copy(alpha = 0.12f))
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Orange.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint               = Orange,
                                        modifier           = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text       = "Already Booked",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Orange
                                    )
                                    Text(
                                        text     = "Yeh property abhi kisi aur ne book kar li hai. Baad mein check karein.",
                                        fontSize = 12.sp,
                                        color    = onSurface.copy(alpha = 0.65f),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                        // Agar booked hai toh divider bhi dikhao
                        if (isPropertyBooked == true) {
                            HorizontalDivider(color = Orange.copy(alpha = 0.2f), thickness = 1.dp)
                        }
                    }

                    // 2. PRICE + RATING
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth().background(surface).padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Per night", fontSize = 11.sp, color = onSurfaceVariant)
                                Text(property.formattedPrice, fontSize = 24.sp, fontWeight = FontWeight.Black, color = onSurface)
                            }
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(primary).padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, tint = tertiary, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("${property.averageRating}", color = onPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text("(${property.reviewCount})", color = onPrimary.copy(0.55f), fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider(color = background, thickness = 6.dp)
                    }

                    // 3. QUICK STATS
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth().background(surface).padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PDDetailStatItem("🛏️", "${property.bedrooms}",                 "Beds",   onSurface, onSurfaceVariant)
                            Box(Modifier.width(1.dp).height(40.dp).background(background))
                            PDDetailStatItem("🚿", "${property.bathrooms}",                "Baths",  onSurface, onSurfaceVariant)
                            Box(Modifier.width(1.dp).height(40.dp).background(background))
                            PDDetailStatItem("👥", "${property.maxGuests}",                "Guests", onSurface, onSurfaceVariant)
                            Box(Modifier.width(1.dp).height(40.dp).background(background))
                            PDDetailStatItem("📐", "${property.areaSqFt?.toInt() ?: "—"}", "Sqft",  onSurface, onSurfaceVariant)
                        }
                        HorizontalDivider(color = background, thickness = 6.dp)
                    }

                    // 4. OWNER
                    item {
                        Row(
                            modifier          = Modifier.fillMaxWidth().background(surface).padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier         = Modifier.size(48.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(primary, MaterialTheme.colorScheme.primaryContainer))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    property.ownerName.firstOrNull { it.isLetter() }?.uppercase() ?: "O",
                                    color = tertiary, fontSize = 20.sp, fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(property.ownerName.ifEmpty { "Property Owner" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = onSurface)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Green, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Verified Owner", fontSize = 11.sp, color = Green)
                                }
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp)).background(tertiary.copy(0.12f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("⭐ Host", fontSize = 11.sp, color = tertiary, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = background, thickness = 6.dp)
                    }

                    // 5. DESCRIPTION
                    item {
                        Column(Modifier.fillMaxWidth().background(surface).padding(horizontal = 20.dp, vertical = 16.dp)) {
                            PDSectionTitle("About this place", tertiary, onSurface)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                property.description.ifEmpty { "A beautiful property waiting for you." },
                                fontSize = 14.sp, color = onSurface.copy(0.75f), lineHeight = 23.sp
                            )
                        }
                        HorizontalDivider(color = background, thickness = 6.dp)
                    }

                    // 6. AMENITIES
                    if (property.amenities.isNotEmpty()) {
                        item {
                            Column(Modifier.fillMaxWidth().background(surface).padding(horizontal = 20.dp, vertical = 16.dp)) {
                                PDSectionTitle("What's included", tertiary, onSurface)
                                Spacer(Modifier.height(12.dp))
                                property.amenities.chunked(2).forEach { pair ->
                                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pair.forEach { amenity ->
                                            Row(
                                                modifier          = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(background).padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(13.dp))
                                                Spacer(Modifier.width(7.dp))
                                                Text(amenity, fontSize = 12.sp, color = onSurface, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                            HorizontalDivider(color = background, thickness = 6.dp)
                        }
                    }

                    // 7. HOUSE RULES
                    item {
                        Column(Modifier.fillMaxWidth().background(surface).padding(horizontal = 20.dp, vertical = 16.dp)) {
                            PDSectionTitle("House Rules", tertiary, onSurface)
                            Spacer(Modifier.height(14.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PDTimeCard(Modifier.weight(1f), "🕐", "Check-in",  property.checkInTime,  primary.copy(0.06f),   onSurface,            onSurfaceVariant)
                                PDTimeCard(Modifier.weight(1f), "🕑", "Check-out", property.checkOutTime, tertiary.copy(0.08f), Color(0xFFB8860B), onSurfaceVariant)
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier          = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(background).padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🌙", fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Text("Minimum stay", fontSize = 13.sp, color = onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text(
                                    "${property.minNights} night${if (property.minNights > 1) "s" else ""}",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onSurface
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PDPolicyChip(Modifier.weight(1f).fillMaxHeight(), "🐾", "Pets",    property.petsAllowed,    onSurfaceVariant)
                                PDPolicyChip(Modifier.weight(1f).fillMaxHeight(), "🚬", "Smoking", property.smokingAllowed, onSurfaceVariant)
                                PDPolicyChip(Modifier.weight(1f).fillMaxHeight(), "🎉", "Parties", property.partiesAllowed, onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = background, thickness = 6.dp)
                    }

                    // 8. LOCATION
                    item {
                        Column(Modifier.fillMaxWidth().background(surface).padding(horizontal = 20.dp, vertical = 16.dp)) {
                            PDSectionTitle("Location", tertiary, onSurface)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(background).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(tertiary.copy(0.15f)),
                                    Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = tertiary, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(property.city,    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onSurface)
                                    Text(property.address, fontSize = 12.sp, color = onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // ── STICKY BOTTOM BAR ──────────────────────────────────────────
                Surface(
                    modifier        = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    shadowElevation = 16.dp,
                    color           = surface
                ) {
                    if (roleLoaded && isTenant && !isOwner) {
                        // ── TENANT BOTTOM BAR ──────────────────────────────────
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ✅ NEW: Already Booked mini-notice inside bottom bar
                            if (isPropertyBooked == true) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Orange.copy(alpha = 0.10f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint               = Orange,
                                        modifier           = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text       = "Yeh property already booked hai",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Orange,
                                        textAlign  = TextAlign.Center
                                    )
                                }
                            }

                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Per night", fontSize = 9.sp, color = onSurfaceVariant)
                                    Text(property.formattedPrice, fontSize = 16.sp, fontWeight = FontWeight.Black, color = onSurface, maxLines = 1)
                                }

                                // Message button — booked ho ya na ho, message kar sakte hain
                                if (property.ownerId.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick        = {
                                            navController.navigate(
                                                Screen.Chat.createRoute(
                                                    userId    = property.ownerId,
                                                    ownerName = property.ownerName.ifEmpty { "Owner" }
                                                )
                                            )
                                        },
                                        modifier       = Modifier.height(42.dp),
                                        shape          = RoundedCornerShape(10.dp),
                                        border         = BorderStroke(1.5.dp, primary),
                                        colors         = ButtonDefaults.outlinedButtonColors(contentColor = primary),
                                        contentPadding = PaddingValues(horizontal = 14.dp)
                                    ) {
                                        Icon(Icons.Default.Message, null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            property.ownerName.ifEmpty { "Owner" }.split(" ").first(),
                                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                        )
                                    }
                                }

                                // ✅ UPDATED: Book Now button — disabled agar property booked hai
                                Button(
                                    onClick  = {
                                        if (isPropertyBooked != true) {
                                            navController.navigate(Screen.Booking.createRoute(propertyId))
                                        }
                                    },
                                    enabled        = isPropertyBooked != true,   // ← KEY CHANGE
                                    modifier       = Modifier.height(42.dp),
                                    shape          = RoundedCornerShape(10.dp),
                                    colors         = ButtonDefaults.buttonColors(
                                        containerColor         = if (isPropertyBooked == true) onSurfaceVariant.copy(0.3f) else primary,
                                        contentColor           = if (isPropertyBooked == true) onSurfaceVariant else onPrimary,
                                        disabledContainerColor = onSurfaceVariant.copy(0.2f),
                                        disabledContentColor   = onSurfaceVariant.copy(0.5f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    elevation      = ButtonDefaults.buttonElevation(
                                        defaultElevation  = if (isPropertyBooked == true) 0.dp else 3.dp
                                    )
                                ) {
                                    // Button text change hoti hai booking status ke mutabiq
                                    Text(
                                        text       = when {
                                            isCheckingBooking        -> "Checking..."
                                            isPropertyBooked == true -> "Booked"
                                            else                     -> "Book Now"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 13.sp
                                    )
                                }
                            }

                            // ✅ UPDATED: "Book with Package" — sirf tab dikhao jab property booked na ho
                            if (hasActivePackage && isPropertyBooked != true) {
                                Button(
                                    onClick   = { navController.navigate(Screen.PreBooking.createRoute(propertyId)) },
                                    modifier  = Modifier.fillMaxWidth().height(46.dp),
                                    shape     = RoundedCornerShape(12.dp),
                                    colors    = ButtonDefaults.buttonColors(containerColor = tertiary, contentColor = primary),
                                    elevation = ButtonDefaults.buttonElevation(3.dp)
                                ) {
                                    Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("Book with Package", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            }

                            // ✅ NEW: Agar booked hai toh "Package" wali jagah yeh dikhao
                            if (hasActivePackage && isPropertyBooked == true) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(onSurfaceVariant.copy(0.08f))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint               = onSurfaceVariant.copy(0.5f),
                                        modifier           = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text       = "Package available — property free hone par book karein",
                                        fontSize   = 11.sp,
                                        color      = onSurfaceVariant.copy(0.6f),
                                        textAlign  = TextAlign.Center
                                    )
                                }
                            }
                        }

                    } else {
                        // ── LANDLORD / OWNER BOTTOM BAR ───────────────────────
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Per night", fontSize = 9.sp, color = onSurfaceVariant)
                                    Text(property.formattedPrice, fontSize = 16.sp, fontWeight = FontWeight.Black, color = onSurface, maxLines = 1)
                                }
                                if (roleLoaded && isLandlord && isOwner) {
                                    Button(
                                        onClick        = { navController.navigate(Screen.EditProperty.createRoute(propertyId)) },
                                        modifier       = Modifier.height(44.dp),
                                        shape          = RoundedCornerShape(10.dp),
                                        colors         = ButtonDefaults.buttonColors(containerColor = primary, contentColor = tertiary),
                                        contentPadding = PaddingValues(horizontal = 18.dp),
                                        elevation      = ButtonDefaults.buttonElevation(3.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit Property", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick        = { navController.navigate(Screen.ViewReviews.createRoute(propertyId)) },
                                modifier       = Modifier.fillMaxWidth().height(42.dp),
                                shape          = RoundedCornerShape(10.dp),
                                border         = BorderStroke(1.5.dp, tertiary),
                                colors         = ButtonDefaults.outlinedButtonColors(contentColor = tertiary),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Icon(Icons.Default.RateReview, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "View Reviews (${property.reviewCount})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 13.sp
                                )
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
                        Text("Property not found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ── Helper Composables ────────────────────────────────────────────────────────

@Composable
private fun PDSectionTitle(text: String, tertiary: Color, onSurface: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(tertiary))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
    }
}

@Composable
private fun PDDetailStatItem(emoji: String, value: String, label: String, onSurface: Color, onSurfaceVariant: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = onSurface)
        Text(label, fontSize = 10.sp, color = onSurfaceVariant)
    }
}

@Composable
private fun PDTimeCard(modifier: Modifier, emoji: String, label: String, value: String, bgColor: Color, valColor: Color, labelColor: Color) {
    Column(
        modifier            = modifier.clip(RoundedCornerShape(12.dp)).background(bgColor).padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = labelColor)
        Spacer(Modifier.height(3.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valColor)
    }
}

@Composable
private fun PDPolicyChip(modifier: Modifier, emoji: String, label: String, allowed: Boolean, labelColor: Color) {
    Column(
        modifier            = modifier.clip(RoundedCornerShape(12.dp)).background(if (allowed) Green.copy(0.08f) else Red.copy(0.07f)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 11.sp, color = labelColor)
        Spacer(Modifier.height(3.dp))
        Text(if (allowed) "Allowed" else "No", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (allowed) Green else Red)
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
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}