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
    viewModel    : PropertyViewModel = hiltViewModel()
) {
    val uiState           = viewModel.uiState.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Dark / Light theme detection ──────────────────────────────
    val isDark = isSystemInDarkTheme()

    // Theme-aware color tokens
    val screenBg   = if (isDark) DarkBg          else Color(0xFFF5F7FA)
    val topBarBg   = if (isDark) DarkBgSecondary  else Color(0xFF0D1B3E)
    val goldAccent = if (isDark) DarkGoldPrimary  else Color(0xFFD4AF37)

    var currentStep by remember { mutableIntStateOf(1) }

    // Form field states
    var title             by remember { mutableStateOf("") }
    var description       by remember { mutableStateOf("") }
    var selectedType      by remember { mutableStateOf("Apartment") }
    var pricePerNight     by remember { mutableStateOf("") }
    var pricePerWeek      by remember { mutableStateOf("") }
    var pricePerMonth     by remember { mutableStateOf("") }
    var bedrooms          by remember { mutableStateOf("1") }
    var bathrooms         by remember { mutableStateOf("1") }
    var maxGuests         by remember { mutableStateOf("2") }
    var area              by remember { mutableStateOf("") }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var city              by remember { mutableStateOf("") }
    var address           by remember { mutableStateOf("") }
    var selectedImages    by remember { mutableStateOf(listOf<Uri>()) }
    var pt1DocumentUri    by remember { mutableStateOf<Uri?>(null) }

    // House rules
    var petsAllowed     by remember { mutableStateOf(false) }
    var smokingAllowed  by remember { mutableStateOf(false) }
    var partiesAllowed  by remember { mutableStateOf(false) }
    var checkInTime     by remember { mutableStateOf("14:00") }
    var checkOutTime    by remember { mutableStateOf("11:00") }

    // Validation errors
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
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            1    -> "Basic Info"
                            2    -> "Details & Amenities"
                            3    -> "Location & Photos"
                            4    -> "Rules & Verification"
                            else -> "Add Property"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = topBarBg,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
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
                                    title         = title,
                                    description   = description,
                                    pricePerNight = pricePerNight.toDoubleOrNull() ?: 0.0,
                                    address       = address,
                                    city          = city,
                                    propertyType  = typeEnum,
                                    bedrooms      = bedrooms.toIntOrNull() ?: 1,
                                    bathrooms     = bathrooms.toIntOrNull() ?: 1,
                                    areaSqFt      = area.toDoubleOrNull(),
                                    amenities     = selectedAmenities.toList(),
                                    images        = selectedImages,
                                    pt1DocumentUri = pt1DocumentUri
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
            modifier            = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(screenBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { StepProgressIndicator(currentStep, TOTAL_STEPS, isDark) }
            item {
                Text(
                    "Step $currentStep of $TOTAL_STEPS",
                    fontSize = 13.sp,
                    color    = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                )
            }

            when (currentStep) {
                1 -> item {
                    Step1BasicInfo(
                        title       = title,       onTitle       = { title       = it }, titleError = titleError,
                        description = description, onDescription = { description = it },
                        selectedType = selectedType, onType = { selectedType = it },
                        isDark = isDark
                    )
                }
                2 -> item {
                    Step2Details(
                        pricePerNight    = pricePerNight, onPrice      = { pricePerNight = it },
                        priceError       = priceError,
                        pricePerWeek     = pricePerWeek,  onWeekPrice  = { pricePerWeek  = it },
                        pricePerMonth    = pricePerMonth, onMonthPrice = { pricePerMonth = it },
                        bedrooms         = bedrooms,  onBedrooms  = { bedrooms  = it },
                        bathrooms        = bathrooms, onBathrooms = { bathrooms = it },
                        maxGuests        = maxGuests, onMaxGuests = { maxGuests = it },
                        area             = area,      onArea      = { area      = it },
                        selectedAmenities = selectedAmenities,
                        onToggleAmenity  = {
                            selectedAmenities = if (it in selectedAmenities)
                                selectedAmenities - it else selectedAmenities + it
                        },
                        isDark = isDark
                    )
                }
                3 -> item {
                    Step3LocationPhotos(
                        city          = city,    onCity    = { city    = it }, cityError = cityError,
                        address       = address, onAddress = { address = it },
                        selectedImages = selectedImages,
                        onAddImages   = { selectedImages = selectedImages + it },
                        onRemoveImage = { selectedImages = selectedImages - it },
                        isDark = isDark
                    )
                }
                4 -> item {
                    Step4RulesVerification(
                        petsAllowed    = petsAllowed,    onPets    = { petsAllowed    = it },
                        smokingAllowed = smokingAllowed, onSmoking = { smokingAllowed = it },
                        partiesAllowed = partiesAllowed, onParties = { partiesAllowed = it },
                        checkInTime    = checkInTime,    onCheckIn  = { checkInTime  = it },
                        checkOutTime   = checkOutTime,   onCheckOut = { checkOutTime = it },
                        pt1DocumentUri = pt1DocumentUri,
                        onPt1Selected  = { pt1DocumentUri = it },
                        pt1Error       = pt1Error,
                        isDark         = isDark
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ── Step Progress Indicator ────────────────────────────────────────
@Composable
private fun StepProgressIndicator(current: Int, total: Int, isDark: Boolean) {
    LinearProgressIndicator(
        progress   = current.toFloat() / total.toFloat(),
        modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
        color      = if (isDark) DarkGoldPrimary else Color(0xFFD4AF37),
        trackColor = if (isDark) DarkBgElevated  else Color(0xFFE0E0E0)
    )
}

// ── Bottom Navigation Bar ─────────────────────────────────────────
@Composable
private fun AddPropertyBottomBar(
    currentStep: Int,
    totalSteps : Int,
    isLoading  : Boolean,
    isDark     : Boolean,
    onNext     : () -> Unit,
    onBack     : () -> Unit
) {
    val barBg   = if (isDark) DarkSurface       else Color.White
    val btnBg   = if (isDark) DarkBgSecondary   else Color(0xFF0D1B3E)
    val btnText = if (isDark) DarkGoldPrimary   else Color.White

    Surface(shadowElevation = 8.dp, color = barBg) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick   = onBack,
                    modifier  = Modifier.weight(1f).height(50.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) DarkTextPrimary else Color(0xFF0D1B3E)
                    )
                ) { Text("Back") }
            }
            Button(
                onClick  = onNext,
                modifier = Modifier.weight(1f).height(50.dp),
                enabled  = !isLoading,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = btnBg)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = btnText,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (currentStep == totalSteps) "Submit Listing" else "Next",
                        fontWeight = FontWeight.Bold,
                        color      = btnText
                    )
                }
            }
        }
    }
}

// ── Step 1: Basic Info ────────────────────────────────────────────
@Composable
private fun Step1BasicInfo(
    title      : String, onTitle       : (String) -> Unit, titleError: String?,
    description: String, onDescription : (String) -> Unit,
    selectedType: String, onType        : (String) -> Unit,
    isDark     : Boolean
) {
    val cardBg    = if (isDark) DarkSurface      else Color.White
    val textColor = if (isDark) DarkTextPrimary  else Color(0xFF0D1B3E)
    val chipSelBg = if (isDark) DarkBgElevated   else Color(0xFF0D1B3E)
    val chipSelLb = if (isDark) DarkGoldPrimary  else Color(0xFFD4AF37)

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Property Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
            OutlinedTextField(
                value         = title,
                onValueChange = onTitle,
                label         = { Text("Property Title *") },
                isError       = titleError != null,
                supportingText = { titleError?.let { Text(it, color = Color.Red) } },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor   = textColor,
                    unfocusedTextColor = textColor
                )
            )
            OutlinedTextField(
                value         = description,
                onValueChange = onDescription,
                label         = { Text("Description") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 3,
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor   = textColor,
                    unfocusedTextColor = textColor
                )
            )
            Text("Property Type *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PROPERTY_TYPES) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick  = { onType(type) },
                        label    = { Text(type, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipSelBg,
                            selectedLabelColor     = chipSelLb
                        )
                    )
                }
            }
        }
    }
}

// ── Step 2: Details & Amenities ───────────────────────────────────
@Composable
private fun Step2Details(
    pricePerNight    : String, onPrice      : (String) -> Unit, priceError: String?,
    pricePerWeek     : String, onWeekPrice  : (String) -> Unit,
    pricePerMonth    : String, onMonthPrice : (String) -> Unit,
    bedrooms         : String, onBedrooms   : (String) -> Unit,
    bathrooms        : String, onBathrooms  : (String) -> Unit,
    maxGuests        : String, onMaxGuests  : (String) -> Unit,
    area             : String, onArea       : (String) -> Unit,
    selectedAmenities: Set<String>, onToggleAmenity: (String) -> Unit,
    isDark           : Boolean
) {
    val cardBg    = if (isDark) DarkSurface      else Color.White
    val textColor = if (isDark) DarkTextPrimary  else Color(0xFF0D1B3E)
    val divColor  = if (isDark) DarkBorder       else Color(0xFFEEEEEE)
    val hintColor = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val chipSelBg = if (isDark) DarkBgElevated   else Color(0xFF0D1B3E)
    val chipSelLb = if (isDark) DarkGoldPrimary  else Color(0xFFD4AF37)

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Pricing", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
            OutlinedTextField(
                value           = pricePerNight,
                onValueChange   = onPrice,
                label           = { Text("Price per Night (PKR) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError         = priceError != null,
                supportingText  = { priceError?.let { Text(it, color = Color.Red) } },
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(12.dp),
                leadingIcon     = { Text("₨", color = hintColor) },
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedTextColor   = textColor,
                    unfocusedTextColor = textColor
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = pricePerWeek,
                    onValueChange   = onWeekPrice,
                    label           = { Text("Per Week") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                OutlinedTextField(
                    value           = pricePerMonth,
                    onValueChange   = onMonthPrice,
                    label           = { Text("Per Month") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            }
            HorizontalDivider(color = divColor)
            Text("Property Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = bedrooms,
                    onValueChange   = onBedrooms,
                    label           = { Text("Bedrooms") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                OutlinedTextField(
                    value           = bathrooms,
                    onValueChange   = onBathrooms,
                    label           = { Text("Bathrooms") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = maxGuests,
                    onValueChange   = onMaxGuests,
                    label           = { Text("Max Guests") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                OutlinedTextField(
                    value           = area,
                    onValueChange   = onArea,
                    label           = { Text("Area (sqft)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(12.dp),
                    colors          = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            }
            HorizontalDivider(color = divColor)
            Text("Amenities", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AMENITIES_LIST.chunked(3).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { amenity ->
                            FilterChip(
                                selected = amenity in selectedAmenities,
                                onClick  = { onToggleAmenity(amenity) },
                                label    = { Text(amenity, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipSelBg,
                                    selectedLabelColor     = chipSelLb
                                )
                            )
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

// ── Step 3: Location & Photos ─────────────────────────────────────
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

    val cardBg    = if (isDark) DarkSurface      else Color.White
    val textColor = if (isDark) DarkTextPrimary  else Color(0xFF0D1B3E)
    val hintColor = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val divColor  = if (isDark) DarkBorder       else Color(0xFFEEEEEE)
    val goldColor = if (isDark) DarkGoldPrimary  else Color(0xFFD4AF37)
    val btnBg     = if (isDark) DarkBgElevated   else Color(0xFF0D1B3E)
    val btnText   = if (isDark) DarkGoldPrimary  else Color.White

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
            OutlinedTextField(
                value          = city,
                onValueChange  = onCity,
                label          = { Text("City *") },
                isError        = cityError != null,
                supportingText = { cityError?.let { Text(it, color = Color.Red) } },
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(12.dp),
                leadingIcon    = { Icon(Icons.Default.LocationOn, null, tint = goldColor) },
                colors         = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
            )
            OutlinedTextField(
                value         = address,
                onValueChange = onAddress,
                label         = { Text("Full Address") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                leadingIcon   = { Icon(Icons.Default.Home, null, tint = hintColor) },
                colors        = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
            )
            HorizontalDivider(color = divColor)
            Text("Property Photos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
            Text("Add at least 3 photos of your property", fontSize = 13.sp, color = hintColor)
            Button(
                onClick  = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = btnBg)
            ) {
                Icon(Icons.Default.AddAPhoto, null, tint = btnText)
                Spacer(Modifier.width(8.dp))
                Text("Add Photos (${selectedImages.size} selected)", color = btnText)
            }
            if (selectedImages.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImages) { uri ->
                        Box(Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                            AsyncImage(
                                model              = uri,
                                contentDescription = null,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop
                            )
                            IconButton(
                                onClick  = { onRemoveImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red.copy(0.7f), CircleShape)
                                    .size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Step 4: Rules & Verification ─────────────────────────────────
@Composable
private fun Step4RulesVerification(
    petsAllowed    : Boolean, onPets    : (Boolean) -> Unit,
    smokingAllowed : Boolean, onSmoking : (Boolean) -> Unit,
    partiesAllowed : Boolean, onParties : (Boolean) -> Unit,
    checkInTime    : String,  onCheckIn  : (String) -> Unit,
    checkOutTime   : String,  onCheckOut : (String) -> Unit,
    pt1DocumentUri : Uri?,    onPt1Selected: (Uri?) -> Unit,
    pt1Error       : String?,
    isDark         : Boolean
) {
    // Only image uploads allowed for PT-1 (imgbb requires image format)
    val pt1Launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onPt1Selected(it) } }

    val cardBg    = if (isDark) DarkSurface      else Color.White
    val textColor = if (isDark) DarkTextPrimary  else Color(0xFF0D1B3E)
    val hintColor = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
    val goldColor = if (isDark) DarkGoldPrimary  else Color(0xFFD4AF37)
    val infoBg    = if (isDark) DarkGoldFaint    else Color(0xFFFFF8E1)
    val successBg = if (isDark) Color(0xFF0D2A1A) else Color(0xFFE8F5E9)
    val btnBg     = if (isDark) DarkBgElevated   else Color(0xFF0D1B3E)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ── House Rules Card ──────────────────────────────────────
        Card(
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("House Rules", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value         = checkInTime,
                        onValueChange = onCheckIn,
                        label         = { Text("Check-in Time") },
                        modifier      = Modifier.weight(1f),
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    OutlinedTextField(
                        value         = checkOutTime,
                        onValueChange = onCheckOut,
                        label         = { Text("Check-out Time") },
                        modifier      = Modifier.weight(1f),
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                }
                RuleToggle("Pets Allowed",    petsAllowed,    onPets,    isDark)
                RuleToggle("Smoking Allowed", smokingAllowed, onSmoking, isDark)
                RuleToggle("Parties Allowed", partiesAllowed, onParties, isDark)
            }
        }

        // ── Verification Document Card ────────────────────────────
        Card(
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Verification Document", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)

                // Info banner
                Row(
                    modifier          = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(infoBg).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = goldColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "PT-1 (Property Tax) document ki PHOTO upload karein (JPG/PNG). Admin review karega.",
                        fontSize = 12.sp,
                        color    = hintColor
                    )
                }

                if (pt1DocumentUri != null) {
                    // Preview uploaded image
                    AsyncImage(
                        model              = pt1DocumentUri,
                        contentDescription = "PT-1 Preview",
                        modifier           = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale       = ContentScale.Crop
                    )
                    Row(
                        modifier          = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(successBg).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "PT-1 document uploaded ✓",
                            fontSize   = 13.sp,
                            color      = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { onPt1Selected(null) }) {
                            Text("Remove", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick  = { pt1Launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (pt1Error != null) Color.Red else textColor
                        ),
                        border   = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (pt1Error != null) Color.Red else textColor
                        )
                    ) {
                        Icon(Icons.Default.UploadFile, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload PT-1 Photo (JPG/PNG) *")
                    }
                    pt1Error?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                }
            }
        }
    }
}

// ── Rule Toggle Row ───────────────────────────────────────────────
@Composable
private fun RuleToggle(label: String, value: Boolean, onToggle: (Boolean) -> Unit, isDark: Boolean) {
    val textColor  = if (isDark) DarkTextPrimary  else Color(0xFF0D1B3E)
    val trackColor = if (isDark) DarkBgElevated   else Color(0xFF0D1B3E)

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = textColor)
        Switch(
            checked         = value,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = trackColor
            )
        )
    }
}