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
import com.havenhub.data.model.Property
import com.havenhub.ui.viewmodel.PropertyViewModel
import com.havenhub.utils.Resource

private val EDIT_PROPERTY_TYPES = listOf("Apartment", "House", "Villa", "Studio", "Room", "Commercial")
private val EDIT_AMENITIES_LIST = listOf(
    "WiFi", "Parking", "Generator", "Air Conditioning", "Heating",
    "Swimming Pool", "Gym", "Security Guard", "CCTV", "Elevator",
    "Laundry", "Garden", "Furnished", "Kitchen", "Balcony"
)

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPropertyScreen(
    propertyId: String,
    navController: NavController,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    val propertyDetailState by viewModel.propertyDetailState.collectAsState()
    val updatePropertyState by viewModel.updatePropertyState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form Fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("") }
    var pricePerMonth by remember { mutableStateOf("") }
    var bedrooms by remember { mutableStateOf("") }
    var bathrooms by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    // Images: existing URLs + newly added URIs
    var existingImageUrls by remember { mutableStateOf(listOf<String>()) }
    var newImageUris by remember { mutableStateOf(listOf<Uri>()) }
    var removedImageUrls by remember { mutableStateOf(setOf<String>()) }

    var isFormInitialized by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Validation errors
    var titleError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }

    // Load property on launch
    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
    }

    // Populate form when property data arrives
    LaunchedEffect(propertyDetailState) {
        if (!isFormInitialized && propertyDetailState is Resource.Success) {
            val property = (propertyDetailState as Resource.Success<Property>).data
            property?.let {
                title = it.title
                description = it.description
                propertyType = it.type
                pricePerMonth = it.pricePerMonth.toString()
                bedrooms = it.bedrooms.toString()
                bathrooms = it.bathrooms.toString()
                area = it.area.toString()
                selectedAmenities = it.amenities.toSet()
                city = it.location.city
                country = it.location.country
                address = it.location.address ?: ""
                isActive = it.isActive
                existingImageUrls = it.imageUrls
                isFormInitialized = true
            }
        }
    }

    // Handle update result
    LaunchedEffect(updatePropertyState) {
        when (val state = updatePropertyState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Property updated successfully!")
                navController.popBackStack()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(state.message ?: "Update failed")
                viewModel.resetUpdatePropertyState()
            }
            else -> Unit
        }
    }

    // Discard Changes Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Property",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showDiscardDialog = true
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Active toggle in app bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isActive) "Active" else "Inactive",
                            fontSize = 12.sp,
                            color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it; hasUnsavedChanges = true },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            EditPropertyBottomBar(
                isLoading = updatePropertyState is Resource.Loading,
                onSave = {
                    var valid = true
                    if (title.isBlank()) { titleError = "Title is required"; valid = false } else titleError = null
                    if (pricePerMonth.isBlank()) { priceError = "Price is required"; valid = false } else priceError = null
                    if (city.isBlank()) { cityError = "City is required"; valid = false } else cityError = null

                    if (valid) {
                        viewModel.updateProperty(
                            propertyId = propertyId,
                            title = title,
                            description = description,
                            type = propertyType,
                            pricePerMonth = pricePerMonth.toDoubleOrNull() ?: 0.0,
                            bedrooms = bedrooms.toIntOrNull() ?: 0,
                            bathrooms = bathrooms.toIntOrNull() ?: 0,
                            area = area.toDoubleOrNull() ?: 0.0,
                            amenities = selectedAmenities.toList(),
                            city = city,
                            country = country,
                            address = address,
                            isActive = isActive,
                            existingImageUrls = existingImageUrls.filterNot { it in removedImageUrls },
                            newImageUris = newImageUris
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when (val state = propertyDetailState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is Resource.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message ?: "Failed to load property", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPropertyDetail(propertyId) }) { Text("Retry") }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // ── Section: Basic Info ──────────────────────────────────
                    item {
                        EditSectionCard(title = "Basic Information", icon = Icons.Default.Info) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it; hasUnsavedChanges = true },
                                    label = { Text("Property Title *") },
                                    isError = titleError != null,
                                    supportingText = titleError?.let { { Text(it) } },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it; hasUnsavedChanges = true },
                                    label = { Text("Description") },
                                    minLines = 3,
                                    maxLines = 6,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Text("Property Type", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(EDIT_PROPERTY_TYPES) { type ->
                                        FilterChip(
                                            selected = propertyType == type,
                                            onClick = { propertyType = type; hasUnsavedChanges = true },
                                            label = { Text(type) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Section: Pricing & Specs ─────────────────────────────
                    item {
                        EditSectionCard(title = "Pricing & Specifications", icon = Icons.Default.AttachMoney) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = pricePerMonth,
                                    onValueChange = { pricePerMonth = it; hasUnsavedChanges = true },
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
                                        onValueChange = { bedrooms = it; hasUnsavedChanges = true },
                                        label = { Text("Bedrooms") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = bathrooms,
                                        onValueChange = { bathrooms = it; hasUnsavedChanges = true },
                                        label = { Text("Bathrooms") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                OutlinedTextField(
                                    value = area,
                                    onValueChange = { area = it; hasUnsavedChanges = true },
                                    label = { Text("Area (sq ft)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // ── Section: Amenities ───────────────────────────────────
                    item {
                        EditSectionCard(title = "Amenities", icon = Icons.Default.Checklist) {
                            Column {
                                EDIT_AMENITIES_LIST.chunked(3).forEach { rowItems ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        rowItems.forEach { amenity ->
                                            FilterChip(
                                                selected = amenity in selectedAmenities,
                                                onClick = {
                                                    selectedAmenities = if (amenity in selectedAmenities)
                                                        selectedAmenities - amenity
                                                    else
                                                        selectedAmenities + amenity
                                                    hasUnsavedChanges = true
                                                },
                                                label = { Text(amenity, fontSize = 12.sp) },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }

                    // ── Section: Location ────────────────────────────────────
                    item {
                        EditSectionCard(title = "Location", icon = Icons.Default.LocationOn) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = city,
                                        onValueChange = { city = it; hasUnsavedChanges = true },
                                        label = { Text("City *") },
                                        isError = cityError != null,
                                        supportingText = cityError?.let { { Text(it) } },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = country,
                                        onValueChange = { country = it; hasUnsavedChanges = true },
                                        label = { Text("Country") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it; hasUnsavedChanges = true },
                                    label = { Text("Full Address") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // ── Section: Photos ──────────────────────────────────────
                    item {
                        EditPhotoSection(
                            existingImageUrls = existingImageUrls,
                            removedImageUrls = removedImageUrls,
                            newImageUris = newImageUris,
                            onRemoveExisting = { url ->
                                removedImageUrls = removedImageUrls + url
                                hasUnsavedChanges = true
                            },
                            onAddNew = { uris ->
                                newImageUris = newImageUris + uris
                                hasUnsavedChanges = true
                            },
                            onRemoveNew = { uri ->
                                newImageUris = newImageUris - uri
                                hasUnsavedChanges = true
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ─── Section Card Wrapper ─────────────────────────────────────────────────────

@Composable
private fun EditSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
            content()
        }
    }
}

// ─── Photo Edit Section ───────────────────────────────────────────────────────

@Composable
private fun EditPhotoSection(
    existingImageUrls: List<String>,
    removedImageUrls: Set<String>,
    newImageUris: List<Uri>,
    onRemoveExisting: (String) -> Unit,
    onAddNew: (List<Uri>) -> Unit,
    onRemoveNew: (Uri) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) onAddNew(uris) }

    val activeExisting = existingImageUrls.filterNot { it in removedImageUrls }
    val totalImages = activeExisting.size + newImageUris.size

    EditSectionCard(title = "Property Photos", icon = Icons.Default.PhotoLibrary) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "$totalImages photo(s) selected",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Existing Images
            AnimatedVisibility(visible = activeExisting.isNotEmpty()) {
                Column {
                    Text(
                        text = "Current Photos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activeExisting) { url ->
                            EditableImageItem(
                                model = url,
                                onRemove = { onRemoveExisting(url) }
                            )
                        }
                    }
                }
            }

            // New Images
            AnimatedVisibility(visible = newImageUris.isNotEmpty()) {
                Column {
                    Text(
                        text = "New Photos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(newImageUris) { uri ->
                            EditableImageItem(
                                model = uri,
                                onRemove = { onRemoveNew(uri) }
                            )
                        }
                    }
                }
            }

            // Upload Button
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add More Photos")
            }
        }
    }
}

@Composable
private fun EditableImageItem(model: Any?, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(90.dp)) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50)
                )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────────

@Composable
private fun EditPropertyBottomBar(isLoading: Boolean, onSave: () -> Unit) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Button(
            onClick = onSave,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
