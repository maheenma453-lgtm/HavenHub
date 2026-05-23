package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack  // FIX: removed deprecated Icons.Filled.ArrowBack import
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// ── Dark theme color tokens ───────────────────────────────────────────────────
private val F_DarkBg      = Color(0xFF060D1A)
private val F_DarkCard    = Color(0xFF112038)
private val F_DarkNavy    = Color(0xFF0D1B3E)
private val F_DarkGold    = Color(0xFFD4AF37)
private val F_DarkTextPri = Color(0xFFF0F4FF)
private val F_DarkTextSec = Color(0xFF8899BB)
private val F_DarkBorder  = Color(0xFF1E2E50)

// ── FIX 1: labelToPropertyType — exhaustive when() with ALL PropertyType values
//
// ERROR WAS: when(uiState.propertyType) at line 98 was not exhaustive
// because PropertyType enum has more values than just 6 basic ones.
//
// FIX: Added else -> null branch so it is always exhaustive regardless
// of how many values PropertyType enum has (PREMIUM, PENTHOUSE, FARMHOUSE etc.)
//
private fun labelToPropertyType(label: String): PropertyType? = when (label) {
    "House"     -> PropertyType.HOUSE
    "Apartment" -> PropertyType.APARTMENT
    "Room"      -> PropertyType.ROOM
    "Studio"    -> PropertyType.STUDIO
    "Villa"     -> PropertyType.VILLA
    "Hostel"    -> PropertyType.HOSTEL
    else        -> null
}

// ── FIX 2: propertyTypeToLabel — exhaustive when() with else branch ───────────
//
// ERROR WAS: when(uiState.propertyType) at line 98 was missing branches
// for PREMIUM, PENTHOUSE, FARMHOUSE (and any future values)
//
// FIX: else -> "" handles all unlisted enum values safely
//
private fun propertyTypeToLabel(type: PropertyType?): String = when (type) {
    PropertyType.HOUSE     -> "House"
    PropertyType.APARTMENT -> "Apartment"
    PropertyType.ROOM      -> "Room"
    PropertyType.STUDIO    -> "Studio"
    PropertyType.VILLA     -> "Villa"
    PropertyType.HOSTEL    -> "Hostel"
    else                   -> ""   // handles PREMIUM, PENTHOUSE, FARMHOUSE, null, any future values
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterScreen(
    navController : NavController,
    viewModel     : SearchViewModel = hiltViewModel()
) {
    val isDark  = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

    // ── Color aliases based on theme ─────────────────────────────────────────
    // FIX: removed textPri / textSec — they were assigned but never used (warnings 151-154)
    val pageBg       = if (isDark) F_DarkBg      else BackgroundWhite
    val topBarBg     = if (isDark) F_DarkNavy    else PrimaryBlue
    val accentC      = if (isDark) F_DarkGold    else PrimaryBlue
    val dividerC     = if (isDark) F_DarkBorder  else BorderGray
    val chipSelBg    = if (isDark) F_DarkGold    else PrimaryBlue
    val chipSelLabel = if (isDark) F_DarkNavy    else BackgroundWhite
    val chipBg       = if (isDark) F_DarkCard    else SurfaceVariantLight
    val chipLabel    = if (isDark) F_DarkTextSec else TextSecondary
    // FIX: textColor for section titles — used directly instead of unused variables
    val sectionTitleColor = if (isDark) F_DarkTextPri else TextPrimary
    val sliderLabelColor  = if (isDark) F_DarkTextSec else TextSecondary

    // ── Local filter state — pre-populated from ViewModel so Reset works ──────
    var selectedCity by remember {
        mutableStateOf<String?>(uiState.selectedCity)
    }

    // FIX: propertyTypeToLabel() now has else branch — no exhaustive error
    var selectedType by remember {
        mutableStateOf(propertyTypeToLabel(uiState.propertyType))
    }

    var priceRange by remember {
        val min = uiState.minPrice?.toFloat() ?: 0f
        val max = uiState.maxPrice?.toFloat() ?: 500000f
        mutableStateOf(min..max)
    }

    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }

    // ── Data ──────────────────────────────────────────────────────────────────
    val cities = listOf(
        "Lahore", "Karachi", "Islamabad", "Rawalpindi", "Quetta",
        "Faisalabad", "Multan", "Sialkot", "Gujranwala",
        "Murree", "Swat", "Hunza", "Naran",
        "Skardu", "Gilgit", "Muzaffarabad"
    )
    val types     = listOf("House", "Apartment", "Room", "Studio", "Villa", "Hostel")
    val amenities = listOf("WiFi", "Parking", "AC", "Generator", "Security", "Kitchen", "Furnished")

    Scaffold(
        containerColor = pageBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Filter Properties",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            // FIX: AutoMirrored version — deprecated warning line 141 fixed
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = BackgroundWhite
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            selectedCity      = null
                            selectedType      = ""
                            priceRange        = 0f..500000f
                            selectedAmenities = setOf()
                            viewModel.clearFilters()
                        }
                    ) {
                        Text(
                            text       = "Reset",
                            color      = BackgroundWhite,
                            fontWeight = FontWeight.SemiBold
                        )
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
                        // FIX: labelToPropertyType() — guaranteed enum match, no null bug
                        val propertyTypeEnum = labelToPropertyType(selectedType)

                        viewModel.applyFilters(
                            minPrice = if (priceRange.start > 0f) priceRange.start.toDouble() else null,
                            maxPrice = if (priceRange.endInclusive < 500000f) priceRange.endInclusive.toDouble() else null,
                            city     = selectedCity,      // null when nothing selected
                            type     = propertyTypeEnum,  // null when nothing selected
                            bedrooms = null
                        )
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
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

            // ── City Filter ───────────────────────────────────────────────────
            FilterSectionTitle(title = "City", textColor = sectionTitleColor)
            Spacer(Modifier.height(10.dp))

            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                cities.forEach { city ->
                    val isSelected = selectedCity == city
                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedCity = if (isSelected) null else city },
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

            // ── Property Type Filter ──────────────────────────────────────────
            FilterSectionTitle(title = "Property Type", textColor = sectionTitleColor)
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

            // ── Price Range ───────────────────────────────────────────────────
            FilterSectionTitle(title = "Price Range (PKR/night)", textColor = sectionTitleColor)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text     = "PKR ${priceRange.start.toInt()}",
                    fontSize = 13.sp,
                    color    = sliderLabelColor
                )
                Text(
                    text     = if (priceRange.endInclusive >= 500000f) "PKR 500,000+"
                    else "PKR ${priceRange.endInclusive.toInt()}",
                    fontSize = 13.sp,
                    color    = sliderLabelColor
                )
            }

            Spacer(Modifier.height(4.dp))

            RangeSlider(
                value         = priceRange,
                onValueChange = { priceRange = it },
                valueRange    = 0f..500000f,
                steps         = 49,
                colors        = SliderDefaults.colors(
                    thumbColor         = accentC,
                    activeTrackColor   = accentC,
                    inactiveTrackColor = dividerC
                )
            )

            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                listOf("< 20K", "20K–50K", "50K–100K", "100K+").forEach { label ->
                    OutlinedButton(
                        onClick = {
                            priceRange = when (label) {
                                "< 20K"    -> 0f..20000f
                                "20K–50K"  -> 20000f..50000f
                                "50K–100K" -> 50000f..100000f
                                else       -> 100000f..500000f
                            }
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentC)
                    ) {
                        Text(label, fontSize = 12.sp)
                    }
                }
            }

            FilterDivider(color = dividerC)

            // ── Amenities (multi-select) ──────────────────────────────────────
            FilterSectionTitle(title = "Amenities", textColor = sectionTitleColor)
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

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun FilterSectionTitle(
    title     : String,
    textColor : Color = TextPrimary
) {
    Text(
        text       = title,
        fontSize   = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color      = textColor
    )
}

@Composable
private fun FilterDivider(color: Color = BorderGray) {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = color, thickness = 1.dp)
    Spacer(Modifier.height(20.dp))
}