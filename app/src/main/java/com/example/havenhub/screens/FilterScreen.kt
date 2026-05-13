package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.PropertyType
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SearchViewModel

// ── Dark theme tokens for FilterScreen ───────────────────────────────────────
private val F_DarkBg      = Color(0xFF060D1A)   // page background
private val F_DarkCard    = Color(0xFF112038)   // card / field container
private val F_DarkNavy    = Color(0xFF0D1B3E)   // top bar
private val F_DarkGold    = Color(0xFFD4AF37)   // primary accent
private val F_DarkTextPri = Color(0xFFF0F4FF)   // primary text
private val F_DarkTextSec = Color(0xFF8899BB)   // secondary text
private val F_DarkBorder  = Color(0xFF1E2E50)   // divider / border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    navController : NavController,
    viewModel     : SearchViewModel = hiltViewModel()
) {
    // ── Dark theme detection ──────────────────────────────────────────────────
    val isDark = isSystemInDarkTheme()

    // ── Theme-aware aliases ───────────────────────────────────────────────────
    val pageBg   = if (isDark) F_DarkBg      else BackgroundWhite
    val topBarBg = if (isDark) F_DarkNavy    else PrimaryBlue
    val goldC    = if (isDark) F_DarkGold    else PrimaryBlue
    val textPri  = if (isDark) F_DarkTextPri else TextPrimary
    val textSec  = if (isDark) F_DarkTextSec else TextSecondary
    val dividerC = if (isDark) F_DarkBorder  else BorderGray
    val chipSelBg    = if (isDark) F_DarkGold  else PrimaryBlue
    val chipSelLabel = if (isDark) F_DarkNavy  else BackgroundWhite
    val chipBg       = if (isDark) F_DarkCard  else SurfaceVariantLight
    val chipLabel    = if (isDark) F_DarkTextSec else TextSecondary

    // ── Local UI State ────────────────────────────────────────────────────────
    var selectedCity      by remember { mutableStateOf("") }
    var selectedType      by remember { mutableStateOf("") }
    var priceRange        by remember { mutableStateOf(0f..100000f) }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }

    val cities    = listOf("Lahore", "Karachi", "Islamabad", "Rawalpindi", "Murree", "Swat", "Hunza")
    val types     = listOf("House", "Apartment", "Room", "Studio", "Villa", "Hostel")
    val amenities = listOf("WiFi", "Parking", "AC", "Generator", "Security", "Kitchen", "Furnished")

    Scaffold(
        containerColor = pageBg,                                // dark: deep navy
        topBar = {
            TopAppBar(
                title = { Text("Filter Properties") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BackgroundWhite)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            selectedCity      = ""
                            selectedType      = ""
                            priceRange        = 0f..100000f
                            selectedAmenities = setOf()
                            viewModel.clearFilters()
                        }
                    ) {
                        Text("Reset", color = BackgroundWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = topBarBg,      // dark: deep navy, light: primary blue
                    titleContentColor          = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        },
        bottomBar = {
            // Apply button — dark aware
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) F_DarkNavy else BackgroundWhite)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val propertyTypeEnum = PropertyType.entries.firstOrNull {
                            it.displayName() == selectedType
                        }
                        viewModel.applyFilters(
                            minPrice = priceRange.start.toDouble(),
                            maxPrice = priceRange.endInclusive.toDouble(),
                            city     = selectedCity.ifEmpty { null },
                            type     = propertyTypeEnum,
                            bedrooms = null
                        )
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) F_DarkGold else PrimaryBlue
                    )
                ) {
                    Text(
                        text       = "Apply Filters",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isDark) F_DarkNavy else Color.White
                    )
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBg)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // City filter
            FilterSectionTitle("City", isDark = isDark)
            Spacer(Modifier.height(10.dp))
            FilterChipGroup(
                options       = cities,
                selected      = selectedCity,
                onSelect      = { selectedCity = it },
                chipSelBg     = chipSelBg,
                chipSelLabel  = chipSelLabel,
                chipBg        = chipBg,
                chipLabel     = chipLabel
            )

            FilterDivider(color = dividerC)

            // Property type filter
            FilterSectionTitle("Property Type", isDark = isDark)
            Spacer(Modifier.height(10.dp))
            FilterChipGroup(
                options       = types,
                selected      = selectedType,
                onSelect      = { selectedType = it },
                chipSelBg     = chipSelBg,
                chipSelLabel  = chipSelLabel,
                chipBg        = chipBg,
                chipLabel     = chipLabel
            )

            FilterDivider(color = dividerC)

            // Price range
            FilterSectionTitle("Price Range (PKR/night)", isDark = isDark)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PKR ${priceRange.start.toInt()}", fontSize = 13.sp, color = textSec)
                Text("PKR ${priceRange.endInclusive.toInt()}", fontSize = 13.sp, color = textSec)
            }

            Spacer(Modifier.height(4.dp))

            RangeSlider(
                value         = priceRange,
                onValueChange = { priceRange = it },
                valueRange    = 0f..500000f,
                steps         = 49,
                colors        = SliderDefaults.colors(
                    thumbColor         = goldC,
                    activeTrackColor   = goldC,
                    inactiveTrackColor = dividerC
                )
            )

            Spacer(Modifier.height(8.dp))

            // Quick price preset buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("< 20K", "20K-50K", "50K-100K", "100K+").forEach { label ->
                    OutlinedButton(
                        onClick = {
                            priceRange = when (label) {
                                "< 20K"    -> 0f..20000f
                                "20K-50K"  -> 20000f..50000f
                                "50K-100K" -> 50000f..100000f
                                else       -> 100000f..500000f
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = goldC)
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }

            FilterDivider(color = dividerC)

            // Amenities (multi-select)
            FilterSectionTitle("Amenities", isDark = isDark)
            Spacer(Modifier.height(10.dp))

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
                                    selectedContainerColor = chipSelBg,
                                    selectedLabelColor     = chipSelLabel,
                                    containerColor         = chipBg,
                                    labelColor             = chipLabel
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Section title — dark aware ────────────────────────────────────────────────
@Composable
private fun FilterSectionTitle(title: String, isDark: Boolean = false) {
    Text(
        text       = title,
        fontSize   = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color      = if (isDark) F_DarkTextPri else TextPrimary
    )
}

// ── Divider with custom color ─────────────────────────────────────────────────
@Composable
private fun FilterDivider(color: Color = BorderGray) {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = color, thickness = 1.dp)
    Spacer(Modifier.height(20.dp))
}

// ── Single-select chip group — dark aware ─────────────────────────────────────
@Composable
private fun FilterChipGroup(
    options      : List<String>,
    selected     : String,
    onSelect     : (String) -> Unit,
    chipSelBg    : Color,
    chipSelLabel : Color,
    chipBg       : Color,
    chipLabel    : Color
) {
    Column {
        options.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val isSelected = selected == option
                    FilterChip(
                        selected = isSelected,
                        onClick  = { onSelect(if (isSelected) "" else option) },
                        label    = { Text(option, fontSize = 13.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipSelBg,
                            selectedLabelColor     = chipSelLabel,
                            containerColor         = chipBg,
                            labelColor             = chipLabel
                        )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}