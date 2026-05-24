package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.PropertyType
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SearchViewModel

// ─────────────────────────────────────────────────────────────────────────────
// labelToPropertyType — maps chip label string to PropertyType enum.
// Uses else -> null so it's exhaustive regardless of future enum additions.
// ─────────────────────────────────────────────────────────────────────────────
private fun labelToPropertyType(label: String): PropertyType? = when (label) {
    "House"     -> PropertyType.HOUSE
    "Apartment" -> PropertyType.APARTMENT
    "Room"      -> PropertyType.ROOM
    "Studio"    -> PropertyType.STUDIO
    "Villa"     -> PropertyType.VILLA
    "Hostel"    -> PropertyType.HOSTEL
    else        -> null
}

// ─────────────────────────────────────────────────────────────────────────────
// propertyTypeToLabel — reverse map from enum to display label.
// else -> "" handles PREMIUM, PENTHOUSE, FARMHOUSE, null, any future values.
// ─────────────────────────────────────────────────────────────────────────────
private fun propertyTypeToLabel(type: PropertyType?): String = when (type) {
    PropertyType.HOUSE     -> "House"
    PropertyType.APARTMENT -> "Apartment"
    PropertyType.ROOM      -> "Room"
    PropertyType.STUDIO    -> "Studio"
    PropertyType.VILLA     -> "Villa"
    PropertyType.HOSTEL    -> "Hostel"
    else                   -> ""
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterScreen(
    navController : NavController,
    viewModel     : SearchViewModel = hiltViewModel()
) {
    val isDark  = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

    // ── Theme-aware color aliases ─────────────────────────────────────────────
    val pageBg            = if (isDark) DarkBg             else BackgroundLight
    val topBarGradStart   = if (isDark) DarkBgSecondary    else PrimaryNavy
    val topBarGradEnd     = if (isDark) DarkBgTertiary     else PrimaryNavyLight
    val accentColor       = if (isDark) DarkGoldPrimary    else PrimaryNavy
    val dividerColor      = if (isDark) DarkBorder         else BorderGray
    val chipSelectedBg    = if (isDark) DarkGoldPrimary    else PrimaryNavy
    val chipSelectedLabel = if (isDark) DarkBg             else BackgroundWhite
    val chipUnselBg       = if (isDark) DarkSurfaceCard    else SurfaceVariantLight
    val chipUnselLabel    = if (isDark) DarkTextSecondary  else TextSecondary
    val sectionTitleColor = if (isDark) DarkTextPrimary    else TextPrimary
    val sliderLabelColor  = if (isDark) DarkTextSecondary  else TextSecondary
    val cardBg            = if (isDark) DarkSurface        else SurfaceWhite
    val bottomBarBg       = if (isDark) DarkBgSecondary    else SurfaceWhite
    val applyBtnBg        = if (isDark) DarkGoldPrimary    else PrimaryNavy
    val applyBtnLabel     = if (isDark) DarkBg             else Color.White
    val pricePresetBorder = if (isDark) DarkBorder         else BorderGray
    val pricePresetLabel  = if (isDark) DarkGoldLight      else PrimaryNavy

    // ── Local state — pre-populated from ViewModel so Reset restores defaults ─
    var selectedCity by remember { mutableStateOf<String?>(uiState.selectedCity) }
    var selectedType by remember { mutableStateOf(propertyTypeToLabel(uiState.propertyType)) }
    var priceRange   by remember {
        val min = uiState.minPrice?.toFloat() ?: 0f
        val max = uiState.maxPrice?.toFloat() ?: 500000f
        mutableStateOf(min..max)
    }

    // ── Data ──────────────────────────────────────────────────────────────────
    val cities = listOf(
        "Lahore", "Karachi", "Islamabad", "Rawalpindi", "Quetta",
        "Faisalabad", "Multan", "Sialkot", "Gujranwala",
        "Murree", "Swat", "Hunza", "Naran",
        "Skardu", "Gilgit", "Muzaffarabad"
    )
    val types = listOf("House", "Apartment", "Room", "Studio", "Villa", "Hostel")

    val pricePresets = listOf(
        "< 20K"    to (0f..20000f),
        "20K–50K"  to (20000f..50000f),
        "50K–100K" to (50000f..100000f),
        "100K+"    to (100000f..500000f)
    )

    // Count how many filter sections are active — for the summary chip
    val activeFilterCount = listOfNotNull(
        selectedCity,
        selectedType.ifBlank { null },
        if (priceRange.start > 0f || priceRange.endInclusive < 500000f) "price" else null
    ).size

    Scaffold(
        containerColor = pageBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(topBarGradStart, topBarGradEnd))
                    )
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text       = "Filter Properties",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = BackgroundWhite
                            )
                            // Shows active count sub-label e.g. "2 filters active"
                            if (activeFilterCount > 0) {
                                Text(
                                    text     = "$activeFilterCount filter${if (activeFilterCount > 1) "s" else ""} active",
                                    fontSize = 12.sp,
                                    color    = if (isDark) DarkGoldLight else GoldAccentLight
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint               = BackgroundWhite
                            )
                        }
                    },
                    actions = {
                        // Reset button
                        TextButton(
                            onClick = {
                                selectedCity = null
                                selectedType = ""
                                priceRange   = 0f..500000f
                                viewModel.clearFilters()
                            }
                        ) {
                            Text(
                                text       = "Reset",
                                color      = if (isDark) DarkGoldLight else GoldAccentLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 15.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            // Bottom apply bar with shadow separator line
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = bottomBarBg,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Summary row above button
                    if (activeFilterCount > 0) {
                        Text(
                            text     = buildString {
                                val parts = mutableListOf<String>()
                                selectedCity?.let { parts.add(it) }
                                if (selectedType.isNotBlank()) parts.add(selectedType)
                                if (priceRange.start > 0f || priceRange.endInclusive < 500000f)
                                    parts.add("PKR ${priceRange.start.toInt()}–${priceRange.endInclusive.toInt()}")
                                append(parts.joinToString("  •  "))
                            },
                            fontSize = 12.sp,
                            color    = sliderLabelColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            // Pass null when slider is at default bounds (no price filter intended)
                            val minP = if (priceRange.start > 0f) priceRange.start.toDouble() else null
                            val maxP = if (priceRange.endInclusive < 500000f) priceRange.endInclusive.toDouble() else null

                            viewModel.applyFilters(
                                minPrice = minP,
                                maxPrice = maxP,
                                city     = selectedCity,
                                type     = labelToPropertyType(selectedType),
                                bedrooms = null
                            )
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = applyBtnBg)
                    ) {
                        Text(
                            text       = if (activeFilterCount > 0) "Apply $activeFilterCount Filter${if (activeFilterCount > 1) "s" else ""}"
                            else "Apply Filters",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = applyBtnLabel
                        )
                    }
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
        ) {

            Spacer(Modifier.height(8.dp))

            // ── CITY SECTION ─────────────────────────────────────────────────
            FilterSectionCard(background = cardBg) {
                FilterSectionTitle(title = "City", textColor = sectionTitleColor)
                Spacer(Modifier.height(12.dp))

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
                            label    = {
                                Text(
                                    city,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            shape  = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipSelectedBg,
                                selectedLabelColor     = chipSelectedLabel,
                                containerColor         = chipUnselBg,
                                labelColor             = chipUnselLabel
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled              = true,
                                selected             = isSelected,
                                selectedBorderColor  = chipSelectedBg,
                                borderColor          = dividerColor,
                                selectedBorderWidth  = 1.5.dp,
                                borderWidth          = 1.dp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── PROPERTY TYPE SECTION ─────────────────────────────────────────
            FilterSectionCard(background = cardBg) {
                FilterSectionTitle(title = "Property Type", textColor = sectionTitleColor)
                Spacer(Modifier.height(12.dp))

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
                            label    = {
                                Text(
                                    type,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            shape  = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipSelectedBg,
                                selectedLabelColor     = chipSelectedLabel,
                                containerColor         = chipUnselBg,
                                labelColor             = chipUnselLabel
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = isSelected,
                                selectedBorderColor = chipSelectedBg,
                                borderColor         = dividerColor,
                                selectedBorderWidth = 1.5.dp,
                                borderWidth         = 1.dp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── PRICE RANGE SECTION ───────────────────────────────────────────
            FilterSectionCard(background = cardBg) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    FilterSectionTitle(title = "Price Range", textColor = sectionTitleColor)
                    // Live price display badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) DarkGoldFaint else PrimaryNavy.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text     = if (priceRange.endInclusive >= 500000f)
                                "PKR ${priceRange.start.toInt()} – 500K+"
                            else
                                "PKR ${priceRange.start.toInt()} – ${priceRange.endInclusive.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color    = if (isDark) DarkGoldLight else PrimaryNavy,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                RangeSlider(
                    value         = priceRange,
                    onValueChange = { priceRange = it },
                    valueRange    = 0f..500000f,
                    steps         = 49,
                    colors        = SliderDefaults.colors(
                        thumbColor         = accentColor,
                        activeTrackColor   = accentColor,
                        inactiveTrackColor = dividerColor
                    )
                )

                Spacer(Modifier.height(4.dp))

                // Min / Max labels
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PKR 0", fontSize = 11.sp, color = sliderLabelColor)
                    Text("PKR 500K+", fontSize = 11.sp, color = sliderLabelColor)
                }

                Spacer(Modifier.height(14.dp))

                // Quick preset buttons row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pricePresets.forEach { (label, range) ->
                        val isActive = priceRange == range
                        OutlinedButton(
                            onClick  = { priceRange = range },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isActive) accentColor else Color.Transparent,
                                contentColor   = if (isActive) applyBtnLabel else pricePresetLabel
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) accentColor else pricePresetBorder
                            )
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FilterSectionCard(
    background: Color,
    content   : @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape         = RoundedCornerShape(16.dp),
        color         = background,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            content  = content
        )
    }
}

@Composable
private fun FilterSectionTitle(
    title     : String,
    textColor : Color
) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        letterSpacing = 0.3.sp
    )
}