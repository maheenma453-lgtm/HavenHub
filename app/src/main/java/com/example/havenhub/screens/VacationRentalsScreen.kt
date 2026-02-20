package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import com.havenhub.ui.viewmodel.VacationViewModel

// ─────────────────────────────────────────────────────────────────
// VacationRentalsScreen.kt
// PURPOSE : Dedicated screen for Northern Areas vacation rentals.
//           Shows popular destinations (Murree, Swat, Hunza, etc.)
//           with seasonal availability and advance booking options.
//           Users can pre-book vacation properties months ahead.
// NAVIGATION:
//   → PreBookingScreen  (book a vacation property)
//   → PropertyDetailScreen (view property details)
// ─────────────────────────────────────────────────────────────────

// Data class for destination cards
data class Destination(
    val name       : String,
    val emoji      : String,
    val properties : Int,
    val season     : String    // "Peak" / "Off-Season" / "Year-Round"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationRentalsScreen(
    navController : NavController,
    viewModel     : VacationViewModel = hiltViewModel()
) {

    LaunchedEffect(Unit) { viewModel.loadVacationProperties() }

    val vacationProps by viewModel.vacationProperties.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()

    // Popular northern areas destinations
    val destinations = listOf(
        Destination("Murree",     "🏔️", 124, "Peak"),
        Destination("Swat",       "🌿", 89,  "Year-Round"),
        Destination("Hunza",      "🏞️", 67,  "Peak"),
        Destination("Nathia Gali","🌲", 45,  "Peak"),
        Destination("Chitral",    "🦅", 38,  "Year-Round"),
        Destination("Kaghan",     "💧", 52,  "Peak")
    )

    // ── UI ────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vacation Rentals") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = BackgroundWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = PrimaryBlue,
                    titleContentColor = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Hero Banner ───────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryBlue, AccentCyan)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text       = "Explore Pakistan's North 🏔️",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = BackgroundWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text     = "Pre-book vacation homes in advance\nand secure the best rates",
                            fontSize = 13.sp,
                            color    = BackgroundWhite.copy(alpha = 0.85f),
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundWhite.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text     = "✈️ 300+ Vacation Properties",
                                fontSize = 12.sp,
                                color    = BackgroundWhite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Section: Popular Destinations ─────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text       = "Popular Destinations",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal scrollable destination cards
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(destinations) { dest ->
                        DestinationCard(
                            destination = dest,
                            onClick     = {
                                viewModel.selectDestination(dest.name)
                            }
                        )
                    }
                }
            }

            // ── Section: Pre-Booking Info Card ────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentCyan.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "📅", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text       = "Advance Pre-Booking Available",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = AccentNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text     = "Book vacation homes up to 6 months in advance. Pay a small deposit now and the rest on arrival.",
                                fontSize = 12.sp,
                                color    = TextSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { navController.navigate(Screen.PreBooking.route) },
                                shape  = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Pre-Book Now", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ── Section: Available Vacation Properties ─────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Available Properties",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        text     = "${vacationProps.size} found",
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Vacation property cards (vertical list)
            if (isLoading) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = PrimaryBlue) }
                }
            } else {
                items(vacationProps) { prop ->
                    VacationPropertyCard(
                        title     = prop.title,
                        location  = prop.city,
                        price     = prop.price,
                        rating    = prop.rating,
                        duration  = "Daily",
                        onClick   = {
                            navController.navigate(Screen.PropertyDetail.createRoute(prop.id))
                        }
                    )
                }
            }
        }
    }
}

// ── Destination card (horizontal scroll) ─────────────────────────
@Composable
private fun DestinationCard(destination: Destination, onClick: () -> Unit) {
    val seasonColor = when (destination.season) {
        "Peak"       -> ErrorRed
        "Off-Season" -> TextSecondary
        else         -> SuccessGreen
    }

    Card(
        modifier  = Modifier.width(120.dp).clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors    = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = destination.emoji, fontSize = 34.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text       = destination.name,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
            Text(
                text     = "${destination.properties} properties",
                fontSize = 11.sp,
                color    = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(seasonColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = destination.season, fontSize = 9.sp, color = seasonColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Vacation property list card ───────────────────────────────────
@Composable
private fun VacationPropertyCard(
    title    : String,
    location : String,
    price    : Long,
    rating   : Float,
    duration : String,
    onClick  : () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) { Text("🏔️", fontSize = 30.sp) }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                    Text(text = location, fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "⭐ $rating", fontSize = 12.sp)
                    Text(
                        text       = "PKR $price/$duration",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryBlue
                    )
                }
            }
        }
    }
}
