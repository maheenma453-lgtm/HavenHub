package com.example.havenhub.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

// ─── Constants ────────────────────────────────────────────────────
private val PROPERTY_TYPES = PropertyType.entries.map { it.displayName() }
private val AMENITIES_LIST = listOf(
    "WiFi", "Parking", "Generator", "Air Conditioning", "Heating",
    "Swimming Pool", "Gym", "Security Guard", "CCTV", "Elevator",
    "Laundry", "Garden", "Furnished", "Kitchen", "Balcony"
)
private const val TOTAL_STEPS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    navController : NavController,
    viewModel     : PropertyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentStep by remember { mutableIntStateOf(1) }

    // Form States
    var title        by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Apartment") }
    var pricePerNight by remember { mutableStateOf("") }
    var bedrooms      by remember { mutableStateOf("1") }
    var bathrooms     by remember { mutableStateOf("1") }
    var area          by remember { mutableStateOf("") }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var city           by remember { mutableStateOf("") }
    var address        by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf(listOf<Uri>()) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var cityError  by remember { mutableStateOf<String?>(null) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add Property Listing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            AddPropertyBottomBar(
                currentStep = currentStep,
                totalSteps  = TOTAL_STEPS,
                isLoading   = uiState.isLoading,
                onNext = {
                    when (currentStep) {
                        1 -> {
                            if (title.isBlank()) titleError = "Required"
                            else { titleError = null; currentStep = 2 }
                        }
                        2 -> {
                            if (pricePerNight.isBlank()) priceError = "Required"
                            else { priceError = null; currentStep = 3 }
                        }
                        3 -> {
                            if (city.isBlank()) cityError = "Required"
                            else {
                                cityError = null
                                val typeEnum = PropertyType.entries.find { it.displayName() == selectedType } ?: PropertyType.APARTMENT
                                viewModel.addProperty(
                                    title = title,
                                    description = description,
                                    pricePerNight = pricePerNight.toDoubleOrNull() ?: 0.0,
                                    address = address,
                                    city = city,
                                    propertyType = typeEnum,
                                    bedrooms = bedrooms.toIntOrNull() ?: 1,
                                    bathrooms = bathrooms.toIntOrNull() ?: 1,
                                    areaSqFt = area.toDoubleOrNull(),
                                    amenities = selectedAmenities.toList(),
                                    images = selectedImages
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
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { StepProgressIndicator(currentStep, TOTAL_STEPS) }

            when (currentStep) {
                1 -> item { Step1BasicInfo(title, {title=it}, titleError, description, {description=it}, selectedType, {selectedType=it}) }
                2 -> item { Step2Details(pricePerNight, {pricePerNight=it}, priceError, bedrooms, {bedrooms=it}, bathrooms, {bathrooms=it}, area, {area=it}, selectedAmenities) {
                    selectedAmenities = if (it in selectedAmenities) selectedAmenities - it else selectedAmenities + it
                } }
                3 -> item { Step3LocationPhotos(city, {city=it}, cityError, address, {address=it}, selectedImages, {selectedImages = selectedImages + it}, {selectedImages = selectedImages - it}) }
            }
        }
    }
}

// ─── Progress Indicator Fixed (Line 253 Area) ───────────────────
@Composable
private fun StepProgressIndicator(current: Int, total: Int) {
    LinearProgressIndicator(
        // FIXED: Removed lambda, using direct Float calculation
        progress = current.toFloat() / total.toFloat(),
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = PrimaryBlue,
        trackColor = BorderGray
    )
}

// ─── Bottom Bar Fixed (Circular Progress Area) ──────────────────
@Composable
private fun AddPropertyBottomBar(currentStep: Int, totalSteps: Int, isLoading: Boolean, onNext: () -> Unit, onBack: () -> Unit) {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (currentStep > 1) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
        }
        Button(onClick = onNext, modifier = Modifier.weight(1f), enabled = !isLoading) {
            if (isLoading) {
                // FIXED: size parameter moved to modifier
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(if (currentStep == totalSteps) "Submit" else "Next")
            }
        }
    }
}

// ... Step1, Step2, Step3 Helper functions remain same as previous version ...
@Composable
private fun Step1BasicInfo(title: String, onTitle: (String) -> Unit, error: String?, desc: String, onDesc: (String) -> Unit, type: String, onType: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = title, onValueChange = onTitle, label = { Text("Title") }, isError = error != null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = desc, onValueChange = onDesc, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp))
        Text("Property Type", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PROPERTY_TYPES) { t -> FilterChip(selected = type == t, onClick = { onType(t) }, label = { Text(t) }) }
        }
    }
}

@Composable
private fun Step2Details(price: String, onPrice: (String) -> Unit, error: String?, beds: String, onBeds: (String) -> Unit, baths: String, onBaths: (String) -> Unit, area: String, onArea: (String) -> Unit, amenities: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = price, onValueChange = onPrice, label = { Text("Price/Night") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = error != null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = beds, onValueChange = onBeds, label = { Text("Beds") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = baths, onValueChange = onBaths, label = { Text("Baths") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
        }
        Text("Amenities", fontWeight = FontWeight.Bold)
        Column {
            AMENITIES_LIST.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { am -> FilterChip(selected = am in amenities, onClick = { onToggle(am) }, label = { Text(am, fontSize = 10.sp) }, modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Step3LocationPhotos(city: String, onCity: (String) -> Unit, error: String?, addr: String, onAddr: (String) -> Unit, images: List<Uri>, onAdd: (List<Uri>) -> Unit, onRemove: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { onAdd(it) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = city, onValueChange = onCity, label = { Text("City") }, isError = error != null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = addr, onValueChange = onAddr, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.AddAPhoto, null)
            Spacer(Modifier.width(8.dp))
            Text("Add Photos")
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(images) { uri ->
                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                    AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop)
                    IconButton(onClick = { onRemove(uri) }, modifier = Modifier.align(Alignment.TopEnd).background(Color.Red.copy(0.6f), CircleShape).size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}