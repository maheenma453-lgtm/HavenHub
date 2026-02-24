package com.example.havenhub.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.havenhub.ui.viewmodel.PropertyViewModel
import com.havenhub.utils.Resource

// ─── Constants ────────────────────────────────────────────────────────────────

private val PROPERTY_TYPES = listOf("Apartment", "House", "Villa", "Studio", "Room", "Commercial")
private val AMENITIES_LIST = listOf(
    "WiFi", "Parking", "Generator", "Air Conditioning", "Heating",
    "Swimming Pool", "Gym", "Security Guard", "CCTV", "Elevator",
    "Laundry", "Garden", "Furnished", "Kitchen", "Balcony"
)
private const val TOTAL_STEPS = 3

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    navController: NavController,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableIntStateOf(1) }

    // Form State — Step 1: Basic Info
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }

    // Form State — Step 2: Details & Amenities
    var pricePerMonth by remember { mutableStateOf("") }
    var bedrooms by remember { mutableStateOf("") }
    var bathrooms by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }

    // Form State — Step 3: Location & Photos
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf(listOf<Uri>()) }

    // Validation
    var titleError by remember { mutableStateOf<String?>(null) }
    var descError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }

    val addPropertyState by viewModel.addPropertyState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle success/error
    LaunchedEffect(addPropertyState) {
        when (val state = addPropertyState) {
            is Resource.Success -> {
                navController.popBackStack()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(state.message ?: "Failed to add property")
                viewModel.resetAddPropertyState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Property",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AddPropertyBottomBar(
                currentStep = currentStep,
                totalSteps = TOTAL_STEPS,
                isLoading = addPropertyState is Resource.Loading,
                onNext = {
                    when (currentStep) {
                        1 -> {
                            var valid = true
                            if (title.isBlank()) { titleError = "Title is required"; valid = false } else titleError = null
                            if (description.isBlank()) { descError = "Description is required"; valid = false } else descError = null
                            if (valid) currentStep = 2
                        }
                        2 -> {
                            var valid = true
                            if (pricePerMonth.isBlank()) { priceError = "Price is required"; valid = false } else priceError = null
                            if (valid) currentStep = 3
                        }
                        3 -> {
                            var valid = true
                            if (city.isBlank()) { cityError = "City is required"; valid = false } else cityError = null
                            if (valid) {
                                viewModel.addProperty(
                                    title = title,
                                    description = description,
                                    type = selectedType,
                                    pricePerMonth = pricePerMonth.toDoubleOrNull() ?: 0.0,
                                    bedrooms = bedrooms.toIntOrNull() ?: 0,
                                    bathrooms = bathrooms.toIntOrNull() ?: 0,
                                    area = area.toDoubleOrNull() ?: 0.0,
                                    amenities = selectedAmenities.toList(),
                                    city = city,
                                    country = country,
                                    address = address,
                                    imageUris = selectedImages
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                StepProgressIndicator(currentStep = currentStep, totalSteps = TOTAL_STEPS)
            }

            when (currentStep) {
                1 -> {
                    item { StepHeader(step = 1, title = "Basic Information") }
                    item {
                        Step1BasicInfo(
                            title = title, onTitleChange = { title = it }, titleError = titleError,
                            description = description, onDescriptionChange = { description = it }, descError = descError,
                            selectedType = selectedType, onTypeSelect = { selectedType = it }
                        )
                    }
                }
                2 -> {
                    item { StepHeader(step = 2, title = "Property Details") }
                    item {
                        Step2Details(
                            pricePerMonth = pricePerMonth, onPriceChange = { pricePerMonth = it }, priceError = priceError,
                            bedrooms = bedrooms, onBedroomsChange = { bedrooms = it },
                            bathrooms = bathrooms, onBathroomsChange = { bathrooms = it },
                            area = area, onAreaChange = { area = it },
                            selectedAmenities = selectedAmenities,
                            onAmenityToggle = { amenity ->
                                selectedAmenities = if (amenity in selectedAmenities)
                                    selectedAmenities - amenity
                                else
                                    selectedAmenities + amenity
                            }
                        )
                    }
                }
                3 -> {
                    item { StepHeader(step = 3, title = "Location & Photos") }
                    item {
                        Step3LocationPhotos(
                            city = city, onCityChange = { city = it }, cityError = cityError,
                            country = country, onCountryChange = { country = it },
                            address = address, onAddressChange = { address = it },
                            selectedImages = selectedImages,
                            onImagesSelected = { uris -> selectedImages = selectedImages + uris },
                            onImageRemove = { uri -> selectedImages = selectedImages - uri }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ─── Step Progress Indicator ──────────────────────────────────────────────────

@Composable
private fun StepProgressIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val stepNumber = index + 1
            val isCompleted = stepNumber < currentStep
            val isCurrent = stepNumber == currentStep

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            color = if (stepNumber < currentStep)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun StepHeader(step: Int, title: String) {
    Column {
        Text(
            text = "Step $step of $TOTAL_STEPS",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Step 1: Basic Info ───────────────────────────────────────────────────────

@Composable
private fun Step1BasicInfo(
    title: String, onTitleChange: (String) -> Unit, titleError: String?,
    description: String, onDescriptionChange: (String) -> Unit, descError: String?,
    selectedType: String, onTypeSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Property Title *") },
            placeholder = { Text("e.g. Modern 2-Bedroom Apartment") },
            isError = titleError != null,
            supportingText = titleError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description *") },
            placeholder = { Text("Describe your property...") },
            isError = descError != null,
            supportingText = descError?.let { { Text(it) } },
            minLines = 4,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text("Property Type", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PROPERTY_TYPES) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelect(type) },
                    label = { Text(type) },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

// ─── Step 2: Details & Amenities ─────────────────────────────────────────────

@Composable
private fun Step2Details(
    pricePerMonth: String, onPriceChange: (String) -> Unit, priceError: String?,
    bedrooms: String, onBedroomsChange: (String) -> Unit,
    bathrooms: String, onBathroomsChange: (String) -> Unit,
    area: String, onAreaChange: (String) -> Unit,
    selectedAmenities: Set<String>,
    onAmenityToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = pricePerMonth,
            onValueChange = onPriceChange,
            label = { Text("Monthly Rent (PKR) *") },
            isError = priceError != null,
            supportingText = priceError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Text("₨", modifier = Modifier.padding(start = 8.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = bedrooms,
                onValueChange = onBedroomsChange,
                label = { Text("Bedrooms") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = bathrooms,
                onValueChange = onBathroomsChange,
                label = { Text("Bathrooms") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = area,
            onValueChange = onAreaChange,
            label = { Text("Area (sq ft)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text("Amenities", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        FlowRowAmenities(
            amenities = AMENITIES_LIST,
            selectedAmenities = selectedAmenities,
            onToggle = onAmenityToggle
        )
    }
}

@Composable
private fun FlowRowAmenities(
    amenities: List<String>,
    selectedAmenities: Set<String>,
    onToggle: (String) -> Unit
) {
    Column {
        amenities.chunked(3).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                rowItems.forEach { amenity ->
                    FilterChip(
                        selected = amenity in selectedAmenities,
                        onClick = { onToggle(amenity) },
                        label = { Text(amenity, fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining slots with empty weight
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
