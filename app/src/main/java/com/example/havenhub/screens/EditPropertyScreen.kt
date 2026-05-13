package com.example.havenhub.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
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

private val EDIT_PROPERTY_TYPES = PropertyType.entries.map { it.displayName() }
private val EDIT_AMENITIES_LIST = listOf(
    "WiFi", "Parking", "Generator", "Air Conditioning", "Heating",
    "Swimming Pool", "Gym", "Security Guard", "CCTV", "Elevator",
    "Laundry", "Garden", "Furnished", "Kitchen", "Balcony"
)

// ── Dark theme tokens for EditProperty ───────────────────────────────────────
private val EPR_DarkBg      = Color(0xFF060D1A)   // page background
private val EPR_DarkCard    = Color(0xFF112038)   // card surface
private val EPR_DarkNavy    = Color(0xFF0D1B3E)   // top bar / icon bg
private val EPR_DarkGold    = Color(0xFFD4AF37)   // primary accent
private val EPR_DarkTextPri = Color(0xFFF0F4FF)   // primary text
private val EPR_DarkTextSec = Color(0xFF8899BB)   // secondary text
private val EPR_DarkBorder  = Color(0xFF1E2E50)   // subtle borders
private val EPR_DarkError   = Color(0xFFE74C3C)   // error red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPropertyScreen(
    propertyId    : String,
    navController : NavController,
    viewModel     : PropertyViewModel = hiltViewModel()
) {
    // ── Dark theme detection ──────────────────────────────────────────────────
    val isDark = isSystemInDarkTheme()

    // ── Theme-aware aliases ───────────────────────────────────────────────────
    val pageBg   = if (isDark) EPR_DarkBg    else Color(0xFFF4F6FA)
    val cardBg   = if (isDark) EPR_DarkCard  else BackgroundWhite
    val topBarBg = if (isDark) EPR_DarkNavy  else PrimaryBlue
    val goldC    = if (isDark) EPR_DarkGold  else PrimaryBlue
    val textPri  = if (isDark) EPR_DarkTextPri else Color.Black
    val textSec  = if (isDark) EPR_DarkTextSec else Color.Gray
    val borderC  = if (isDark) EPR_DarkBorder  else Color(0xFFBDBDBD)
    val redC     = if (isDark) EPR_DarkError   else ErrorRed

    val uiState = viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    var existingImageUrls by remember { mutableStateOf(listOf<String>()) }
    var newImageUris      by remember { mutableStateOf(listOf<Uri>()) }
    var removedImageUrls  by remember { mutableStateOf(setOf<String>()) }

    var isFormInitialized by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
    }

    LaunchedEffect(uiState.value.propertyDetail) {
        uiState.value.propertyDetail?.let { prop ->
            if (!isFormInitialized) {
                title             = prop.title
                description       = prop.description
                propertyType      = try {
                    PropertyType.valueOf(prop.propertyType).displayName()
                } catch (e: Exception) { "Apartment" }
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

    LaunchedEffect(uiState.value.actionSuccess, uiState.value.errorMessage) {
        if (uiState.value.actionSuccess) {
            viewModel.clearMessages()
            navController.popBackStack()
        }
        uiState.value.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Discard changes dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor   = cardBg,
            title = { Text("Discard Changes?", color = textPri) },
            text  = { Text("You have unsaved changes. Are you sure you want to go back?", color = textSec) },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Discard", color = redC)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel", color = if (isDark) EPR_DarkTextSec else Color.Gray)
                }
            }
        )
    }

    Scaffold(
        containerColor = pageBg,                                // dark: deep navy bg
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Property", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showDiscardDialog = true else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = topBarBg,      // dark: deep navy
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            EditPropertyBottomBar(
                isLoading = uiState.value.isLoading,
                isDark    = isDark
            ) {
                uiState.value.propertyDetail?.let { currentProp ->
                    val propTypeString = PropertyType.entries
                        .find { it.displayName() == propertyType }
                        ?.name ?: currentProp.propertyType

                    val updatedProperty = currentProp.copy(
                        title         = title,
                        description   = description,
                        propertyType  = propTypeString,
                        pricePerNight = pricePerNight.toDoubleOrNull() ?: currentProp.pricePerNight,
                        bedrooms      = bedrooms.toIntOrNull() ?: currentProp.bedrooms,
                        bathrooms     = bathrooms.toIntOrNull() ?: currentProp.bathrooms,
                        areaSqFt      = area.toDoubleOrNull(),
                        amenities     = selectedAmenities.toList(),
                        city          = city,
                        address       = address,
                        imageUrls     = existingImageUrls.filterNot { it in removedImageUrls }
                    )
                    viewModel.updateProperty(updatedProperty, newImageUris)
                }
            }
        }
    ) { paddingValues ->
        if (uiState.value.isLoading && !isFormInitialized) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = goldC)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                // Basic Information section
                item {
                    EditSectionCard(
                        title  = "Basic Information",
                        icon   = Icons.Default.Info,
                        isDark = isDark
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DarkAwareTextField(
                                value         = title,
                                onValueChange = { title = it; hasUnsavedChanges = true },
                                label         = "Title",
                                isDark        = isDark
                            )
                            DarkAwareTextField(
                                value         = description,
                                onValueChange = { description = it; hasUnsavedChanges = true },
                                label         = "Description",
                                isDark        = isDark,
                                minLines      = 3
                            )
                            Text(
                                "Property Type",
                                fontWeight = FontWeight.Medium,
                                color      = textPri
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(EDIT_PROPERTY_TYPES) { type ->
                                    FilterChip(
                                        selected = propertyType == type,
                                        onClick  = { propertyType = type; hasUnsavedChanges = true },
                                        label    = { Text(type) },
                                        colors   = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isDark) EPR_DarkGold else PrimaryBlue,
                                            selectedLabelColor     = if (isDark) EPR_DarkNavy else Color.White,
                                            containerColor         = if (isDark) EPR_DarkCard else Color(0xFFEEEEEE),
                                            labelColor             = if (isDark) EPR_DarkTextSec else Color.Gray
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Pricing & Details section
                item {
                    EditSectionCard(
                        title  = "Pricing & Details",
                        icon   = Icons.Default.AttachMoney,
                        isDark = isDark
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DarkAwareTextField(
                                value           = pricePerNight,
                                onValueChange   = { pricePerNight = it; hasUnsavedChanges = true },
                                label           = "Price/Night",
                                isDark          = isDark,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DarkAwareTextField(
                                    value         = bedrooms,
                                    onValueChange = { bedrooms = it; hasUnsavedChanges = true },
                                    label         = "Beds",
                                    isDark        = isDark,
                                    modifier      = Modifier.weight(1f)
                                )
                                DarkAwareTextField(
                                    value         = bathrooms,
                                    onValueChange = { bathrooms = it; hasUnsavedChanges = true },
                                    label         = "Baths",
                                    isDark        = isDark,
                                    modifier      = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Amenities section
                item {
                    EditSectionCard(
                        title  = "Amenities",
                        icon   = Icons.Default.Checklist,
                        isDark = isDark
                    ) {
                        Column {
                            EDIT_AMENITIES_LIST.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { am ->
                                        FilterChip(
                                            selected = am in selectedAmenities,
                                            onClick  = {
                                                selectedAmenities = if (am in selectedAmenities)
                                                    selectedAmenities - am
                                                else
                                                    selectedAmenities + am
                                                hasUnsavedChanges = true
                                            },
                                            label    = { Text(am, fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            colors   = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (isDark) EPR_DarkGold else PrimaryBlue,
                                                selectedLabelColor     = if (isDark) EPR_DarkNavy else Color.White,
                                                containerColor         = if (isDark) EPR_DarkCard else Color(0xFFEEEEEE),
                                                labelColor             = if (isDark) EPR_DarkTextSec else Color.Gray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Photos section
                item {
                    EditPhotoSection(
                        existing    = existingImageUrls,
                        removed     = removedImageUrls,
                        new         = newImageUris,
                        onRemoveOld = { removedImageUrls = removedImageUrls + it; hasUnsavedChanges = true },
                        onAddNew    = { newImageUris = newImageUris + it; hasUnsavedChanges = true },
                        onRemoveNew = { newImageUris = newImageUris - it; hasUnsavedChanges = true },
                        isDark      = isDark
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ── Dark-aware OutlinedTextField helper ───────────────────────────────────────
@Composable
private fun DarkAwareTextField(
    value          : String,
    onValueChange  : (String) -> Unit,
    label          : String,
    isDark         : Boolean        = false,
    readOnly       : Boolean        = false,
    minLines       : Int            = 1,
    modifier       : Modifier       = Modifier.fillMaxWidth(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label) },
        modifier        = modifier,
        readOnly        = readOnly,
        minLines        = minLines,
        keyboardOptions = keyboardOptions,
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = if (isDark) EPR_DarkGold    else PrimaryBlue,
            unfocusedBorderColor    = if (isDark) EPR_DarkBorder  else Color(0xFFBDBDBD),
            focusedLabelColor       = if (isDark) EPR_DarkGold    else PrimaryBlue,
            unfocusedLabelColor     = if (isDark) EPR_DarkTextSec else Color.Gray,
            focusedTextColor        = if (isDark) EPR_DarkTextPri else Color.Black,
            unfocusedTextColor      = if (isDark) EPR_DarkTextPri else Color.Black,
            focusedContainerColor   = if (isDark) EPR_DarkCard    else Color.Transparent,
            unfocusedContainerColor = if (isDark) EPR_DarkCard    else Color.Transparent,
            cursorColor             = if (isDark) EPR_DarkGold    else PrimaryBlue
        )
    )
}

// ── Section Card — dark theme aware ──────────────────────────────────────────
@Composable
private fun EditSectionCard(
    title  : String,
    icon   : ImageVector,
    isDark : Boolean = false,
    content: @Composable () -> Unit
) {
    val cardBg  = if (isDark) EPR_DarkCard  else BackgroundWhite
    val goldC   = if (isDark) EPR_DarkGold  else PrimaryBlue
    val textPri = if (isDark) EPR_DarkTextPri else Color.Black

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(icon, null, tint = goldC, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, color = textPri)
            }
            content()
        }
    }
}

// ── Photo Section — dark theme aware ─────────────────────────────────────────
@Composable
private fun EditPhotoSection(
    existing   : List<String>,
    removed    : Set<String>,
    new        : List<Uri>,
    onRemoveOld: (String) -> Unit,
    onAddNew   : (List<Uri>) -> Unit,
    onRemoveNew: (Uri) -> Unit,
    isDark     : Boolean = false
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { onAddNew(it) }
    val activeOld = existing.filterNot { it in removed }

    EditSectionCard("Photos", Icons.Default.PhotoLibrary, isDark = isDark) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activeOld) { url -> EditableImageItem(url) { onRemoveOld(url) } }
                items(new)       { uri -> EditableImageItem(uri) { onRemoveNew(uri) } }
            }
            OutlinedButton(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) EPR_DarkGold else PrimaryBlue
                )
            ) {
                Icon(Icons.Default.AddPhotoAlternate, null)
                Text(" Add More Photos")
            }
        }
    }
}

// ── Editable image thumbnail ──────────────────────────────────────────────────
@Composable
private fun EditableImageItem(model: Any?, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp))) {
        AsyncImage(
            model              = model,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )
        IconButton(
            onClick  = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(24.dp)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Bottom Save Bar — dark theme aware ───────────────────────────────────────
@Composable
private fun EditPropertyBottomBar(
    isLoading: Boolean,
    isDark   : Boolean = false,
    onSave   : () -> Unit
) {
    val barBg  = if (isDark) Color(0xFF0D1B3E) else BackgroundWhite
    val goldC  = if (isDark) EPR_DarkGold      else PrimaryBlue
    val textC  = if (isDark) Color(0xFF060D1A) else Color.White

    Surface(tonalElevation = 8.dp, color = barBg) {
        Button(
            onClick  = onSave,
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(50.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = goldC)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = textC)
            } else {
                Icon(Icons.Default.Save, null, tint = textC)
                Spacer(Modifier.width(8.dp))
                Text("Save Changes", color = textC)
            }
        }
    }
}