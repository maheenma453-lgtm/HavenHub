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

// ── Dark theme tokens ─────────────────────────────────────────────────────────
private val F_DarkBg      = Color(0xFF060D1A)
private val F_DarkCard    = Color(0xFF112038)
private val F_DarkNavy    = Color(0xFF0D1B3E)
private val F_DarkGold    = Color(0xFFD4AF37)
private val F_DarkTextPri = Color(0xFFF0F4FF)
private val F_DarkTextSec = Color(0xFF8899BB)
private val F_DarkBorder  = Color(0xFF1E2E50)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterScreen(
    navController : NavController,
    viewModel     : SearchViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()

    val pageBg       = if (isDark) F_DarkBg      else BackgroundWhite
    val topBarBg     = if (isDark) F_DarkNavy    else PrimaryBlue
    val goldC        = if (isDark) F_DarkGold    else PrimaryBlue
    val textPri      = if (isDark) F_DarkTextPri else TextPrimary
    val textSec      = if (isDark) F_DarkTextSec else TextSecondary
    val dividerC     = if (isDark) F_DarkBorder  else BorderGray
    val chipSelBg    = if (isDark) F_DarkGold    else PrimaryBlue
    val chipSelLabel = if (isDark) F_DarkNavy    else BackgroundWhite
    val chipBg       = if (isDark) F_DarkCard    else SurfaceVariantLight
    val chipLabel    = if (isDark) F_DarkTextSec else TextSecondary

    var selectedCity      by remember { mutableStateOf("") }
    var selectedType      by remember { mutableStateOf("") }
    var priceRange        by remember { mutableStateOf(0f..100000f) }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }

    val cities = listOf(
        "Lahore", "Karachi", "Islamabad", "Rawalpindi", "Quetta", "Faisalabad", "Multan",
        "Sialkot", "Gujranwala",
        "Murree", "Swat", "Hunza", "Naran",
        "Skardu", "Gilgit", "Muzaffarabad"
    )
    val types     = listOf("House", "Apartment", "Room", "Studio", "Villa", "Hostel")
    val amenities = listOf("WiFi", "Parking", "AC", "Generator", "Security", "Kitchen", "Furnished")

    Scaffold(
        containerColor = pageBg,
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
                    containerColor             = topBarBg,
                    titleContentColor          = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) F_DarkNavy else BackgroundWhite)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ── City filter ───────────────────────────────────────────────────
            FilterSectionTitle("City", isDark = isDark)
            Spacer(Modifier.height(10.dp))

            // FlowRow wraps chips naturally on any screen width
            FlowRow(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                cities.forEach { city ->
                    val isSelected = selectedCity == city
                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedCity = if (isSelected) "" else city },
                        label    = { Text(city, fontSize = 13.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipSelBg,
                            selectedLabelColor     = chipSelLabel,
                            containerColor         = chipBg,
                            labelColor             = chipLabel
                        )
                    )
                }
            }

            FilterDivider(color = dividerC)

            // ── Property type filter ──────────────────────────────────────────
            FilterSectionTitle("Property Type", isDark = isDark)
            Spacer(Modifier.height(10.dp))

            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                types.forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedType = if (isSelected) "" else type },
                        label    = { Text(type, fontSize = 13.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipSelBg,
                            selectedLabelColor     = chipSelLabel,
                            containerColor         = chipBg,
                            labelColor             = chipLabel
                        )
                    )
                }
            }

            FilterDivider(color = dividerC)

            // ── Price range ───────────────────────────────────────────────────
            FilterSectionTitle("Price Range (PKR/night)", isDark = isDark)
            Spacer(Modifier.height(8.dp))

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

            // Quick price preset buttons — responsive wrap on small screens
            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
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
                        shape  = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = goldC)
                    ) {
                        Text(label, fontSize = 12.sp)
                    }
                }
            }

            FilterDivider(color = dividerC)

            // ── Amenities (multi-select) ──────────────────────────────────────
            FilterSectionTitle("Amenities", isDark = isDark)
            Spacer(Modifier.height(10.dp))

            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                amenities.forEach { amenity ->
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

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Section title ─────────────────────────────────────────────────────────────
@Composable
private fun FilterSectionTitle(title: String, isDark: Boolean = false) {
    Text(
        text       = title,
        fontSize   = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color      = if (isDark) F_DarkTextPri else TextPrimary
    )
}

// ── Divider ───────────────────────────────────────────────────────────────────
@Composable
private fun FilterDivider(color: Color = BorderGray) {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = color, thickness = 1.dp)
    Spacer(Modifier.height(20.dp))
}