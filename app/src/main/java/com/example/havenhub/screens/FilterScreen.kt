package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.SearchViewModel

// ─────────────────────────────────────────────────────────────────
// FilterScreen.kt
// PURPOSE : Advanced search filter screen.
//           Filters: City, Property Type, Rent Range, Duration.
//           User selects filters and taps "Apply" to get filtered results.
// NAVIGATION: FilterScreen → (back to SearchScreen with filters applied)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    navController : NavController,
    viewModel     : SearchViewModel = hiltViewModel()
) {

    // ── State Variables ────────────────────────────────────────────
    var selectedCity     by remember { mutableStateOf("") }
    var selectedType     by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf("") }
    var priceRange       by remember { mutableStateOf(0f..100000f) }

    // Filter options
    val cities     = listOf("Lahore", "Karachi", "Islamabad", "Rawalpindi", "Murree", "Swat", "Hunza")
    val types      = listOf("House", "Apartment", "Room", "Studio", "Villa", "Shop")
    val durations  = listOf("Daily", "Weekly", "Monthly")

    // ── UI ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filter Properties") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint               = BackgroundWhite
                        )
                    }
                },
                // Reset button clears all filters
                actions = {
                    TextButton(
                        onClick = {
                            selectedCity     = ""
                            selectedType     = ""
                            selectedDuration = ""
                            priceRange       = 0f..100000f
                        }
                    ) {
                        Text("Reset", color = BackgroundWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = PrimaryBlue,
                    titleContentColor      = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        },
        bottomBar = {
            // ── Apply Button (sticky at bottom) ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundWhite)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        // Pass filters to viewModel and go back
                        viewModel.applyFilters(
                            city     = selectedCity,
                            type     = selectedType,
                            duration = selectedDuration,
                            minPrice = priceRange.start.toLong(),
                            maxPrice = priceRange.endInclusive.toLong()
                        )
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(
                        text       = "Apply Filters",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            // ── Section: Select City ──────────────────────────────
            FilterSectionTitle(title = "City")
            Spacer(modifier = Modifier.height(10.dp))
            FilterChipGroup(
                options   = cities,
                selected  = selectedCity,
                onSelect  = { selectedCity = it }
            )

            FilterDivider()

            // ── Section: Property Type ────────────────────────────
            FilterSectionTitle(title = "Property Type")
            Spacer(modifier = Modifier.height(10.dp))
            FilterChipGroup(
                options  = types,
                selected = selectedType,
                onSelect = { selectedType = it }
            )

            FilterDivider()

            // ── Section: Rental Duration ──────────────────────────
            FilterSectionTitle(title = "Rental Duration")
            Spacer(modifier = Modifier.height(10.dp))
            FilterChipGroup(
                options  = durations,
                selected = selectedDuration,
                onSelect = { selectedDuration = it }
            )

            FilterDivider()

            // ── Section: Price Range Slider ───────────────────────
            FilterSectionTitle(title = "Monthly Rent Range")
            Spacer(modifier = Modifier.height(6.dp))

            // Show current price range values
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text     = "PKR ${priceRange.start.toInt()}",
                    fontSize = 13.sp,
                    color    = TextSecondary
                )
                Text(
                    text     = "PKR ${priceRange.endInclusive.toInt()}",
                    fontSize = 13.sp,
                    color    = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Range slider for price selection
            RangeSlider(
                value       = priceRange,
                onValueChange = { priceRange = it },
                valueRange  = 0f..500000f,
                steps       = 49,              // 10,000 PKR steps
                colors      = SliderDefaults.colors(
                    thumbColor         = PrimaryBlue,
                    activeTrackColor   = PrimaryBlue,
                    inactiveTrackColor = BorderGray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick price preset buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("< 20K", "20K-50K", "50K-100K", "100K+").forEach { label ->
                    OutlinedButton(
                        onClick = {
                            // Set price range presets
                            priceRange = when (label) {
                                "< 20K"   -> 0f..20000f
                                "20K-50K" -> 20000f..50000f
                                "50K-100K" -> 50000f..100000f
                                else       -> 100000f..500000f
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section: Amenities (multi-select) ────────────────
            FilterSectionTitle(title = "Amenities")
            Spacer(modifier = Modifier.height(10.dp))

            var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
            val amenities = listOf("WiFi", "Parking", "AC", "Generator", "Security", "Kitchen", "Furnished")

            // Wrap amenities in a flow-like row layout
            Column {
                amenities.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { amenity ->
                            val isSelected = amenity in selectedAmenities
                            FilterChip(
                                selected = isSelected,
                                onClick  = {
                                    selectedAmenities = if (isSelected)
                                        selectedAmenities - amenity
                                    else
                                        selectedAmenities + amenity
                                },
                                label  = { Text(amenity, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor     = BackgroundWhite
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Helper Composables
// ─────────────────────────────────────────────────────────────────

// Bold section title
@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text       = title,
        fontSize   = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextPrimary
    )
}

// Horizontal divider between filter sections
@Composable
private fun FilterDivider() {
    Spacer(modifier = Modifier.height(20.dp))
    Divider(color = BorderGray, thickness = 1.dp)
    Spacer(modifier = Modifier.height(20.dp))
}

// Group of filter chips (single selection)
@Composable
private fun FilterChipGroup(
    options  : List<String>,
    selected : String,
    onSelect : (String) -> Unit
) {
    // Wrap chips in rows of 4
    Column {
        options.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val isSelected = selected == option
                    FilterChip(
                        selected = isSelected,
                        onClick  = {
                            // Toggle: tap again to deselect
                            onSelect(if (isSelected) "" else option)
                        },
                        label  = { Text(option, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor     = BackgroundWhite,
                            containerColor         = SurfaceGray,
                            labelColor             = TextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}


