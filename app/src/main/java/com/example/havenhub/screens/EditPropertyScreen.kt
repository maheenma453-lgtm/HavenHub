package com.example.havenhub.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyType
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.PropertyViewModel

private val EDIT_PROPERTY_TYPES = PropertyType.entries.map { it.displayName() }
private val EDIT_AMENITIES_LIST = listOf(
    "WiFi", "Parking", "Generator", "Air Conditioning", "Heating",
    "Swimming Pool", "Gym", "Security Guard", "CCTV", "Elevator",
    "Laundry", "Garden", "Furnished", "Kitchen", "Balcony"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPropertyScreen(
    propertyId    : String,
    navController : NavController,
    viewModel     : PropertyViewModel = hiltViewModel()
) {
    // ── ViewModel State Observation ──
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Form Fields ──
    var title             by remember { mutableStateOf("") }
    var description       by remember { mutableStateOf("") }
    var propertyType      by remember { mutableStateOf("Apartment") }
    var pricePerNight     by remember { mutableStateOf("") }
    var bedrooms          by remember { mutableStateOf("") }
    var bathrooms         by remember { mutableStateOf("") }
    var area              by remember { mutableStateOf("") }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var city              by remember { mutableStateOf("") }
    var address           by remember { mutableStateOf("") }

    // ── Image State ──
    var existingImageUrls by remember { mutableStateOf(listOf<String>()) }
    var newImageUris      by remember { mutableStateOf(listOf<Uri>()) }
    var removedImageUrls  by remember { mutableStateOf(setOf<String>()) }

    var isFormInitialized  by remember { mutableStateOf(false) }
    var hasUnsavedChanges  by remember { mutableStateOf(false) }
    var showDiscardDialog  by remember { mutableStateOf(false) }

    // ── Load property on Start ──
    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
    }

    // ── Populate form from uiState ──
    LaunchedEffect(uiState.propertyDetail) {
        uiState.propertyDetail?.let { prop ->
            if (!isFormInitialized) {
                title             = prop.title
                description       = prop.description
                propertyType      = prop.propertyType.displayName()
                pricePerNight     = prop.pricePerNight.toString()
                bedrooms          = prop.bedrooms.toString()
                bathrooms         = prop.bathrooms.toString()
                area              = prop.areaSqFt?.toString() ?: ""
                selectedAmenities = prop.amenities.toSet()
                city              = prop.city
                address           = prop.address
                existingImageUrls = prop.imageUrls
                isFormInitialized = true
            }
        }
    }

    // ── Handle Result ──
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

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) { Text("Discard", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Property", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showDiscardDialog = true else navController.popBackStack()
                    }) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            EditPropertyBottomBar(isLoading = uiState.isLoading) {
                uiState.propertyDetail?.let { currentProp ->
                    val propTypeEnum = PropertyType.entries.find { it.displayName() == propertyType } ?: currentProp.propertyType

                    val updatedProperty = currentProp.copy(
                        title = title,
                        description = description,
                        propertyType = propTypeEnum,
                        pricePerNight = pricePerNight.toDoubleOrNull() ?: currentProp.pricePerNight,
                        bedrooms = bedrooms.toIntOrNull() ?: currentProp.bedrooms,
                        bathrooms = bathrooms.toIntOrNull() ?: currentProp.bathrooms,
                        areaSqFt = area.toDoubleOrNull(),
                        amenities = selectedAmenities.toList(),
                        city = city,
                        address = address,
                        imageUrls = existingImageUrls.filterNot { it in removedImageUrls }
                    )
                    viewModel.updateProperty(updatedProperty, newImageUris)
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading && !isFormInitialized) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                // --- Form Sections ---
                item {
                    EditSectionCard("Basic Information", Icons.Default.Info) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(value = title, onValueChange = { title = it; hasUnsavedChanges = true }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = description, onValueChange = { description = it; hasUnsavedChanges = true }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                            Text("Property Type", fontWeight = FontWeight.Medium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(EDIT_PROPERTY_TYPES) { type ->
                                    FilterChip(selected = propertyType == type, onClick = { propertyType = type; hasUnsavedChanges = true }, label = { Text(type) })
                                }
                            }
                        }
                    }
                }

                item {
                    EditSectionCard("Pricing & Details", Icons.Default.AttachMoney) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(value = pricePerNight, onValueChange = { pricePerNight = it; hasUnsavedChanges = true }, label = { Text("Price/Night") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(value = bedrooms, onValueChange = { bedrooms = it; hasUnsavedChanges = true }, label = { Text("Beds") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = bathrooms, onValueChange = { bathrooms = it; hasUnsavedChanges = true }, label = { Text("Baths") }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    EditSectionCard("Amenities", Icons.Default.Checklist) {
                        Column {
                            EDIT_AMENITIES_LIST.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { am ->
                                        FilterChip(selected = am in selectedAmenities, onClick = {
                                            selectedAmenities = if (am in selectedAmenities) selectedAmenities - am else selectedAmenities + am
                                            hasUnsavedChanges = true
                                        }, label = { Text(am, fontSize = 10.sp) }, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    EditPhotoSection(existingImageUrls, removedImageUrls, newImageUris,
                        { removedImageUrls = removedImageUrls + it; hasUnsavedChanges = true },
                        { newImageUris = newImageUris + it; hasUnsavedChanges = true },
                        { newImageUris = newImageUris - it; hasUnsavedChanges = true })
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun EditSectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(BackgroundWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun EditPhotoSection(existing: List<String>, removed: Set<String>, new: List<Uri>, onRemoveOld: (String) -> Unit, onAddNew: (List<Uri>) -> Unit, onRemoveNew: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { onAddNew(it) }
    val activeOld = existing.filterNot { it in removed }

    EditSectionCard("Photos", Icons.Default.PhotoLibrary) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activeOld) { url -> EditableImageItem(url) { onRemoveOld(url) } }
                items(new) { uri -> EditableImageItem(uri) { onRemoveNew(uri) } }
            }
            OutlinedButton(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AddPhotoAlternate, null)
                Text(" Add More Photos")
            }
        }
    }
}

@Composable
private fun EditableImageItem(model: Any?, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp))) {
        AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(0.5f), CircleShape).size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun EditPropertyBottomBar(isLoading: Boolean, onSave: () -> Unit) {
    Surface(tonalElevation = 8.dp, color = BackgroundWhite) {
        Button(
            onClick = onSave,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(PrimaryBlue)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            else {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Changes")
            }
        }
    }
}