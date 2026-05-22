package com.example.havenhub.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.data.PropertyType
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.PropertyViewModel

private val PROPERTY_TYPES = PropertyType.entries.map { it.displayName() }
private val AMENITIES_LIST = listOf(
    "WiFi", "Parking", "Generator", "Air Conditioning", "Heating",
    "Swimming Pool", "Gym", "Security Guard", "CCTV", "Elevator",
    "Laundry", "Garden", "Furnished", "Kitchen", "Balcony"
)
private const val TOTAL_STEPS = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    navController: NavController,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    val uiState           = viewModel.uiState.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark            = isSystemInDarkTheme()

    // ── Theme tokens ──────────────────────────────────────────────────────────
    val screenBg    = if (isDark) DarkBg          else BackgroundLight
    val topBarBg    = if (isDark) DarkBgSecondary  else PrimaryNavy
    val goldAccent  = if (isDark) DarkGoldPrimary  else GoldAccent
    val onTopBar    = if (isDark) DarkTextPrimary  else Color.White

    // ── Step state ────────────────────────────────────────────────────────────
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1
    var title        by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Apartment") }

    // Step 2
    var pricePerNight     by remember { mutableStateOf("") }
    var pricePerWeek      by remember { mutableStateOf("") }
    var pricePerMonth     by remember { mutableStateOf("") }
    var bedrooms          by remember { mutableStateOf("1") }
    var bathrooms         by remember { mutableStateOf("1") }
    var maxGuests         by remember { mutableStateOf("2") }
    var area              by remember { mutableStateOf("") }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }

    // Step 3
    var city           by remember { mutableStateOf("") }
    var address        by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf(listOf<Uri>()) }

    // Step 4
    var petsAllowed    by remember { mutableStateOf(false) }
    var smokingAllowed by remember { mutableStateOf(false) }
    var partiesAllowed by remember { mutableStateOf(false) }
    var checkInTime    by remember { mutableStateOf("14:00") }
    var checkOutTime   by remember { mutableStateOf("11:00") }
    var pt1DocumentUri by remember { mutableStateOf<Uri?>(null) }

    // Validation
    var titleError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var cityError  by remember { mutableStateOf<String?>(null) }
    var pt1Error   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.actionSuccess, uiState.errorMessage) {
        if (uiState.actionSuccess) {
            viewModel.clearMessages()
            navController.popBackStack()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = screenBg,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (isDark)
                                listOf(DarkBg, DarkBgSecondary, DarkBgTertiary)
                            else
                                listOf(PrimaryNavyDark, PrimaryNavy, PrimaryNavyLight)
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (currentStep) {
                                    1    -> "Basic Info"
                                    2    -> "Details & Amenities"
                                    3    -> "Location & Photos"
                                    4    -> "Rules & Verification"
                                    else -> "Add Property"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = onTopBar
                            )
                            Text(
                                text     = "Step $currentStep of $TOTAL_STEPS",
                                fontSize = 11.sp,
                                color    = goldAccent
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (currentStep > 1) currentStep-- else navController.popBackStack()
                        }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint               = onTopBar
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
            AddPropertyBottomBar(
                currentStep = currentStep,
                totalSteps  = TOTAL_STEPS,
                isLoading   = uiState.isLoading,
                isDark      = isDark,
                onNext = {
                    when (currentStep) {
                        1 -> {
                            if (title.isBlank()) titleError = "Title is required"
                            else { titleError = null; currentStep = 2 }
                        }
                        2 -> {
                            if (pricePerNight.isBlank()) priceError = "Price is required"
                            else { priceError = null; currentStep = 3 }
                        }
                        3 -> {
                            if (city.isBlank()) cityError = "City is required"
                            else { cityError = null; currentStep = 4 }
                        }
                        4 -> {
                            if (pt1DocumentUri == null) {
                                pt1Error = "PT-1 document is required"
                            } else {
                                pt1Error = null
                                val typeEnum = PropertyType.entries.find {
                                    it.displayName() == selectedType
                                } ?: PropertyType.APARTMENT
                                viewModel.addProperty(
                                    title          = title,
                                    description    = description,
                                    pricePerNight  = pricePerNight.toDoubleOrNull() ?: 0.0,
                                    pricePerWeek   = pricePerWeek.toDoubleOrNull(),
                                    pricePerMonth  = pricePerMonth.toDoubleOrNull(),
                                    address        = address,
                                    city           = city,
                                    propertyType   = typeEnum,
                                    bedrooms       = bedrooms.toIntOrNull() ?: 1,
                                    bathrooms      = bathrooms.toIntOrNull() ?: 1,
                                    maxGuests      = maxGuests.toIntOrNull() ?: 2,
                                    areaSqFt       = area.toDoubleOrNull(),
                                    amenities      = selectedAmenities.toList(),
                                    images         = selectedImages,
                                    pt1DocumentUri = pt1DocumentUri,
                                    isPremium      = selectedType == "Premium",
                                    petsAllowed    = petsAllowed,
                                    smokingAllowed = smokingAllowed,
                                    partiesAllowed = partiesAllowed,
                                    checkInTime    = checkInTime,
                                    checkOutTime   = checkOutTime
                                )
                            }
                        }
                    }
                },
                onBack = { if (currentStep > 1) currentStep-- }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(screenBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            // Premium step progress bar
            item { PremiumStepProgress(currentStep, TOTAL_STEPS, isDark) }

            when (currentStep) {
                1 -> item {
                    Step1BasicInfo(
                        title        = title,       onTitle        = { title        = it },
                        titleError   = titleError,
                        description  = description, onDescription  = { description  = it },
                        selectedType = selectedType, onType        = { selectedType = it },
                        isDark       = isDark
                    )
                }
                2 -> item {
                    Step2Details(
                        pricePerNight     = pricePerNight, onPrice      = { pricePerNight = it },
                        priceError        = priceError,
                        pricePerWeek      = pricePerWeek,  onWeekPrice  = { pricePerWeek  = it },
                        pricePerMonth     = pricePerMonth, onMonthPrice = { pricePerMonth = it },
                        bedrooms          = bedrooms,  onBedrooms   = { bedrooms   = it },
                        bathrooms         = bathrooms, onBathrooms  = { bathrooms  = it },
                        maxGuests         = maxGuests, onMaxGuests  = { maxGuests  = it },
                        area              = area,      onArea       = { area       = it },
                        selectedAmenities = selectedAmenities,
                        onToggleAmenity   = {
                            selectedAmenities =
                                if (it in selectedAmenities) selectedAmenities - it
                                else selectedAmenities + it
                        },
                        isDark = isDark
                    )
                }
                3 -> item {
                    Step3LocationPhotos(
                        city           = city,    onCity    = { city    = it },
                        cityError      = cityError,
                        address        = address,  onAddress = { address = it },
                        selectedImages = selectedImages,
                        onAddImages    = { selectedImages = selectedImages + it },
                        onRemoveImage  = { selectedImages = selectedImages - it },
                        isDark         = isDark
                    )
                }
                4 -> item {
                    Step4RulesVerification(
                        petsAllowed    = petsAllowed,    onPets     = { petsAllowed    = it },
                        smokingAllowed = smokingAllowed, onSmoking  = { smokingAllowed = it },
                        partiesAllowed = partiesAllowed, onParties  = { partiesAllowed = it },
                        checkInTime    = checkInTime,    onCheckIn  = { checkInTime    = it },
                        checkOutTime   = checkOutTime,   onCheckOut = { checkOutTime   = it },
                        pt1DocumentUri = pt1DocumentUri,
                        onPt1Selected  = { pt1DocumentUri = it },
                        pt1Error       = pt1Error,
                        isDark         = isDark
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM STEP PROGRESS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumStepProgress(current: Int, total: Int, isDark: Boolean) {
    val gold      = if (isDark) DarkGoldPrimary else GoldAccent
    val goldFaint = if (isDark) DarkGoldFaint   else Color(0xFFF5E6C4)
    val trackBg   = if (isDark) DarkBgElevated  else Color(0xFFE0E4EF)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Step dots row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            (1..total).forEach { step ->
                val isActive   = step == current
                val isComplete = step < current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isActive) 6.dp else 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isComplete -> gold
                                isActive   -> gold
                                else       -> trackBg
                            }
                        )
                )
            }
        }
        // Step label chips row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Basic", "Details", "Location", "Rules").forEachIndexed { index, label ->
                val step      = index + 1
                val isActive  = step == current
                val isDone    = step < current
                Text(
                    text       = if (isDone) "✓ $label" else label,
                    fontSize   = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color      = when {
                        isDone   -> gold
                        isActive -> gold
                        else     -> if (isDark) DarkTextMuted else Color(0xFFAAAAAA)
                    }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BOTTOM BAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddPropertyBottomBar(
    currentStep: Int,
    totalSteps : Int,
    isLoading  : Boolean,
    isDark     : Boolean,
    onNext     : () -> Unit,
    onBack     : () -> Unit
) {
    val barBg  = if (isDark) DarkSurface    else SurfaceWhite
    val gold   = if (isDark) DarkGoldPrimary else GoldAccent
    val navy   = if (isDark) DarkBgSecondary else PrimaryNavy
    val onNavy = if (isDark) DarkTextPrimary else Color.White

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color           = barBg,
        tonalElevation  = if (isDark) 4.dp else 0.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick  = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.5.dp, gold),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = gold
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back", fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick  = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                enabled  = !isLoading,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = navy,
                    contentColor   = onNavy
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = gold,
                        strokeWidth = 2.dp
                    )
                } else {
                    if (currentStep == totalSteps) {
                        Icon(Icons.Default.Check, null, tint = gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text       = if (currentStep == totalSteps) "Submit Listing" else "Next",
                        fontWeight = FontWeight.Bold,
                        color      = if (currentStep == totalSteps) gold else onNavy
                    )
                    if (currentStep < totalSteps) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM SECTION CARD wrapper
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumCard(
    isDark  : Boolean,
    content : @Composable ColumnScope.() -> Unit
) {
    val cardBg     = if (isDark) DarkSurface    else SurfaceWhite
    val borderColor = if (isDark) DarkBorder    else Color(0xFFE8EAF0)

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDark) 0.dp else 2.dp
        ),
        modifier  = Modifier
            .fillMaxWidth()
            .border(
                width = if (isDark) 1.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content             = content
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM SECTION HEADER inside card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title  : String,
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    isDark : Boolean
) {
    val gold      = if (isDark) DarkGoldPrimary else GoldAccent
    val textColor = if (isDark) DarkTextPrimary else PrimaryNavy

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(gold.copy(alpha = if (isDark) 0.18f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = gold, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text       = title,
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp,
            color      = textColor
        )
    }
}

// ── Shared field colors helper ────────────────────────────────────────────────

@Composable
private fun premiumFieldColors(isDark: Boolean): TextFieldColors {
    val gold      = if (isDark) DarkGoldPrimary else GoldAccent
    val textColor = if (isDark) DarkTextPrimary else PrimaryNavy
    val hint      = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val container = if (isDark) DarkBgSecondary   else Color(0xFFF8F9FC)
    val border    = if (isDark) DarkBorder         else Color(0xFFDDE1E7)

    return OutlinedTextFieldDefaults.colors(
        focusedTextColor        = textColor,
        unfocusedTextColor      = textColor,
        focusedBorderColor      = gold,
        unfocusedBorderColor    = border,
        focusedLabelColor       = gold,
        unfocusedLabelColor     = hint,
        focusedContainerColor   = container,
        unfocusedContainerColor = container,
        cursorColor             = gold
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// STEP 1 — BASIC INFO
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun Step1BasicInfo(
    title        : String, onTitle       : (String) -> Unit, titleError: String?,
    description  : String, onDescription : (String) -> Unit,
    selectedType : String, onType        : (String) -> Unit,
    isDark       : Boolean
) {
    val gold      = if (isDark) DarkGoldPrimary  else GoldAccent
    val navy      = if (isDark) DarkBgElevated   else PrimaryNavy
    val goldLabel = if (isDark) DarkGoldLight     else GoldAccentDark
    val textColor = if (isDark) DarkTextPrimary   else PrimaryNavy
    val chipUnsel = if (isDark) DarkBgTertiary    else Color(0xFFF0F2F8)
    val chipText  = if (isDark) DarkTextSecondary else Color(0xFF555577)

    PremiumCard(isDark = isDark) {
        SectionHeader("Property Information", Icons.Default.Home, isDark)

        OutlinedTextField(
            value          = title,
            onValueChange  = onTitle,
            label          = { Text("Property Title *") },
            isError        = titleError != null,
            supportingText = { titleError?.let { Text(it, color = DarkError) } },
            modifier       = Modifier.fillMaxWidth(),
            shape          = RoundedCornerShape(14.dp),
            colors         = premiumFieldColors(isDark)
        )

        OutlinedTextField(
            value         = description,
            onValueChange = onDescription,
            label         = { Text("Description") },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = 3,
            shape         = RoundedCornerShape(14.dp),
            colors        = premiumFieldColors(isDark)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Property Type",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp,
                color      = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PROPERTY_TYPES) { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) navy else chipUnsel)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) gold else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onType(type) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text       = type,
                            fontSize   = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) gold else chipText
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// STEP 2 — DETAILS & AMENITIES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun Step2Details(
    pricePerNight     : String, onPrice       : (String) -> Unit, priceError: String?,
    pricePerWeek      : String, onWeekPrice   : (String) -> Unit,
    pricePerMonth     : String, onMonthPrice  : (String) -> Unit,
    bedrooms          : String, onBedrooms    : (String) -> Unit,
    bathrooms         : String, onBathrooms   : (String) -> Unit,
    maxGuests         : String, onMaxGuests   : (String) -> Unit,
    area              : String, onArea        : (String) -> Unit,
    selectedAmenities : Set<String>, onToggleAmenity: (String) -> Unit,
    isDark            : Boolean
) {
    val gold      = if (isDark) DarkGoldPrimary  else GoldAccent
    val navy      = if (isDark) DarkBgElevated   else PrimaryNavy
    val divColor  = if (isDark) DarkBorder       else Color(0xFFEEEEEE)
    val chipUnsel = if (isDark) DarkBgTertiary   else Color(0xFFF0F2F8)
    val chipText  = if (isDark) DarkTextSecondary else Color(0xFF555577)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Pricing card
        PremiumCard(isDark = isDark) {
            SectionHeader("Pricing", Icons.Default.CurrencyRupee, isDark)

            OutlinedTextField(
                value           = pricePerNight,
                onValueChange   = onPrice,
                label           = { Text("Price per Night (PKR) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError         = priceError != null,
                supportingText  = { priceError?.let { Text(it, color = DarkError) } },
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(14.dp),
                leadingIcon     = { Text("₨", color = gold, fontWeight = FontWeight.Bold) },
                colors          = premiumFieldColors(isDark)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = pricePerWeek,
                    onValueChange   = onWeekPrice,
                    label           = { Text("Per Week") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(14.dp),
                    colors          = premiumFieldColors(isDark)
                )
                OutlinedTextField(
                    value           = pricePerMonth,
                    onValueChange   = onMonthPrice,
                    label           = { Text("Per Month") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(14.dp),
                    colors          = premiumFieldColors(isDark)
                )
            }
        }

        // Property details card
        PremiumCard(isDark = isDark) {
            SectionHeader("Property Details", Icons.Default.Apartment, isDark)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = bedrooms,
                    onValueChange   = onBedrooms,
                    label           = { Text("Bedrooms") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(14.dp),
                    leadingIcon     = { Text("🛏️", fontSize = 14.sp) },
                    colors          = premiumFieldColors(isDark)
                )
                OutlinedTextField(
                    value           = bathrooms,
                    onValueChange   = onBathrooms,
                    label           = { Text("Bathrooms") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(14.dp),
                    leadingIcon     = { Text("🚿", fontSize = 14.sp) },
                    colors          = premiumFieldColors(isDark)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = maxGuests,
                    onValueChange   = onMaxGuests,
                    label           = { Text("Max Guests") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(14.dp),
                    leadingIcon     = { Text("👥", fontSize = 14.sp) },
                    colors          = premiumFieldColors(isDark)
                )
                OutlinedTextField(
                    value           = area,
                    onValueChange   = onArea,
                    label           = { Text("Area (sqft)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(14.dp),
                    leadingIcon     = { Text("📐", fontSize = 14.sp) },
                    colors          = premiumFieldColors(isDark)
                )
            }
        }

        // Amenities card
        PremiumCard(isDark = isDark) {
            SectionHeader("Amenities", Icons.Default.Checklist, isDark)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AMENITIES_LIST.chunked(3).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { amenity ->
                            val isSelected = amenity in selectedAmenities
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) navy else chipUnsel)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) gold else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onToggleAmenity(amenity) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = amenity,
                                    fontSize   = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (isSelected) gold else chipText
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// STEP 3 — LOCATION & PHOTOS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun Step3LocationPhotos(
    city          : String, onCity    : (String) -> Unit, cityError: String?,
    address       : String, onAddress : (String) -> Unit,
    selectedImages: List<Uri>,
    onAddImages   : (List<Uri>) -> Unit,
    onRemoveImage : (Uri) -> Unit,
    isDark        : Boolean
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { onAddImages(it) }

    val gold  = if (isDark) DarkGoldPrimary  else GoldAccent
    val navy  = if (isDark) DarkBgSecondary  else PrimaryNavy
    val white = if (isDark) DarkTextPrimary  else Color.White

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        PremiumCard(isDark = isDark) {
            SectionHeader("Location", Icons.Default.LocationOn, isDark)

            OutlinedTextField(
                value          = city,
                onValueChange  = onCity,
                label          = { Text("City *") },
                placeholder    = { Text("e.g. Lahore, Islamabad, Skardu") },
                isError        = cityError != null,
                supportingText = { cityError?.let { Text(it, color = DarkError) } },
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(14.dp),
                leadingIcon    = {
                    Icon(Icons.Default.LocationOn, null, tint = gold)
                },
                colors         = premiumFieldColors(isDark)
            )

            OutlinedTextField(
                value         = address,
                onValueChange = onAddress,
                label         = { Text("Full Address") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                leadingIcon   = {
                    Icon(Icons.Default.Home, null,
                        tint = if (isDark) DarkTextSecondary else Color(0xFF8899AA))
                },
                colors        = premiumFieldColors(isDark)
            )
        }

        PremiumCard(isDark = isDark) {
            SectionHeader("Property Photos", Icons.Default.PhotoCamera, isDark)

            Text(
                "Add at least 3 photos of your property",
                fontSize = 13.sp,
                color    = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
            )

            // Upload button — gradient style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (isDark) listOf(DarkBgTertiary, DarkBgElevated)
                            else        listOf(PrimaryNavy, PrimaryNavyLight)
                        )
                    )
                    .border(1.dp, gold, RoundedCornerShape(14.dp))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddAPhoto, null, tint = gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Add Photos (${selectedImages.size} selected)",
                        color      = gold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp
                    )
                }
            }

            if (selectedImages.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selectedImages) { uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, gold.copy(0.4f), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model              = uri,
                                contentDescription = null,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xCCEF4444))
                                    .clickable { onRemoveImage(uri) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// STEP 4 — RULES & VERIFICATION
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun Step4RulesVerification(
    petsAllowed    : Boolean, onPets      : (Boolean) -> Unit,
    smokingAllowed : Boolean, onSmoking   : (Boolean) -> Unit,
    partiesAllowed : Boolean, onParties   : (Boolean) -> Unit,
    checkInTime    : String,  onCheckIn   : (String)  -> Unit,
    checkOutTime   : String,  onCheckOut  : (String)  -> Unit,
    pt1DocumentUri : Uri?,    onPt1Selected: (Uri?)   -> Unit,
    pt1Error       : String?,
    isDark         : Boolean
) {
    val pt1Launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onPt1Selected(it) } }

    val gold      = if (isDark) DarkGoldPrimary  else GoldAccent
    val navy      = if (isDark) DarkBgSecondary  else PrimaryNavy
    val infoBg    = if (isDark) DarkGoldFaint    else Color(0xFFFFF8E1)
    val infoText  = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val successBg = if (isDark) Color(0xFF0D2A1A) else Color(0xFFE8F5E9)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // House rules card
        PremiumCard(isDark = isDark) {
            SectionHeader("House Rules", Icons.Default.Rule, isDark)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = checkInTime,
                    onValueChange = onCheckIn,
                    label         = { Text("Check-in") },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(14.dp),
                    leadingIcon   = { Text("🕐", fontSize = 14.sp) },
                    colors        = premiumFieldColors(isDark)
                )
                OutlinedTextField(
                    value         = checkOutTime,
                    onValueChange = onCheckOut,
                    label         = { Text("Check-out") },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(14.dp),
                    leadingIcon   = { Text("🕑", fontSize = 14.sp) },
                    colors        = premiumFieldColors(isDark)
                )
            }

            HorizontalDivider(
                color     = if (isDark) DarkBorder else Color(0xFFEEEEEE),
                thickness = 1.dp
            )

            PremiumRuleToggle("🐾  Pets Allowed",    petsAllowed,    onPets,    gold, isDark)
            PremiumRuleToggle("🚬  Smoking Allowed", smokingAllowed, onSmoking, gold, isDark)
            PremiumRuleToggle("🎉  Parties Allowed", partiesAllowed, onParties, gold, isDark)
        }

        // Verification card
        PremiumCard(isDark = isDark) {
            SectionHeader("Verification Document", Icons.Default.VerifiedUser, isDark)

            // Info box
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(infoBg)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint     = gold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Upload PT-1 (Property Tax) document photo. Admin will verify before approval.",
                    fontSize   = 12.sp,
                    color      = infoText,
                    lineHeight = 18.sp
                )
            }

            if (pt1DocumentUri != null) {
                AsyncImage(
                    model              = pt1DocumentUri,
                    contentDescription = "PT-1 Preview",
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, gold.copy(0.4f), RoundedCornerShape(12.dp)),
                    contentScale       = ContentScale.Crop
                )
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(successBg)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PT-1 document uploaded", fontSize = 13.sp,
                        color = SuccessGreen, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { onPt1Selected(null) }) {
                        Text("Remove", color = DarkError, fontSize = 12.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (pt1Error != null) DarkError.copy(0.06f)
                            else if (isDark) DarkBgTertiary else Color(0xFFF8F9FC)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (pt1Error != null) DarkError
                            else if (isDark) DarkBorder else Color(0xFFDDE1E7),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { pt1Launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.UploadFile, null,
                            tint     = if (pt1Error != null) DarkError else gold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Upload PT-1 Photo (JPG/PNG) *",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp,
                            color      = if (pt1Error != null) DarkError
                            else if (isDark) DarkTextSecondary else PrimaryNavy
                        )
                    }
                }
                pt1Error?.let {
                    Text(it, color = DarkError, fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Premium Rule Toggle ───────────────────────────────────────────────────────

@Composable
private fun PremiumRuleToggle(
    label    : String,
    value    : Boolean,
    onToggle : (Boolean) -> Unit,
    gold     : Color,
    isDark   : Boolean
) {
    val textColor = if (isDark) DarkTextPrimary else PrimaryNavy
    val navy = if (isDark) DarkBgElevated else PrimaryNavy

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (value) navy.copy(alpha = if (isDark) 0.6f else 1f)
                else if (isDark) DarkBgSecondary else Color(0xFFF8F9FC)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (value) FontWeight.SemiBold else FontWeight.Normal,
            color = if (value) gold else textColor
        )
        Switch(
            checked = value,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = gold,
                checkedTrackColor = navy,
                uncheckedThumbColor = if (isDark) DarkTextMuted else Color(0xFFBBBBBB),
                uncheckedTrackColor = if (isDark) DarkBgTertiary else Color(0xFFE0E0E0)
            )
        )
    }
}