package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.PackageDuration
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.viewmodel.PropertyViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Screen for landlords to create a rental package for one of their approved properties.
 * Navigated to from MyPropertiesScreen when landlord taps "Add Package" on a card.
 *
 * propertyId is passed via nav argument and is pre-filled in the package object.
 * After successful creation, the screen pops back to MyPropertiesScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRentalPackageScreen(
    navController: NavController,
    propertyId   : String,
    viewModel    : PropertyViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val scrollState  = rememberScrollState()

    val primary      = MaterialTheme.colorScheme.primary
    val onPrimary    = MaterialTheme.colorScheme.onPrimary
    val background   = MaterialTheme.colorScheme.background

    // ── Form fields ───────────────────────────────────────────────────────────
    var packageName        by remember { mutableStateOf("") }
    var description        by remember { mutableStateOf("") }
    var badgeLabel         by remember { mutableStateOf("") }
    var originalPrice      by remember { mutableStateOf("") }
    var discountedPrice    by remember { mutableStateOf("") }
    var discountPercentage by remember { mutableStateOf("") }
    var minNights          by remember { mutableStateOf("1") }
    var maxNights          by remember { mutableStateOf("") }
    var fixedNights        by remember { mutableStateOf("") }
    var totalSlots         by remember { mutableStateOf("") }
    var selectedDuration   by remember { mutableStateOf(PackageDuration.FLEXIBLE) }
    var inclusionInput     by remember { mutableStateOf("") }
    var inclusions         by remember { mutableStateOf(listOf<String>()) }
    var showDurationMenu   by remember { mutableStateOf(false) }
    var validationError    by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Navigate back on success ──────────────────────────────────────────────
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            snackbarHostState.showSnackbar("Package created successfully!")
            viewModel.clearMessages()
            navController.popBackStack()
        }
    }

    // ── Show error in snackbar ────────────────────────────────────────────────
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar       = {
            TopAppBar(
                title          = { Text("Add Rental Package", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = primary,
                    titleContentColor          = onPrimary,
                    navigationIconContentColor = onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Section: Basic Info ───────────────────────────────────────────
            PackageSectionHeader(title = "Package Info")

            OutlinedTextField(
                value         = packageName,
                onValueChange = { packageName = it },
                label         = { Text("Package Name *") },
                placeholder   = { Text("e.g. Summer Escape Deal") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            OutlinedTextField(
                value         = description,
                onValueChange = { description = it },
                label         = { Text("Description *") },
                placeholder   = { Text("Describe what makes this package special") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 3,
                maxLines      = 5
            )

            OutlinedTextField(
                value         = badgeLabel,
                onValueChange = { badgeLabel = it },
                label         = { Text("Badge Label") },
                placeholder   = { Text("e.g. 🔥 Summer Deal") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            // ── Section: Duration ─────────────────────────────────────────────
            PackageSectionHeader(title = "Duration Type")

            // Duration type dropdown
            ExposedDropdownMenuBox(
                expanded         = showDurationMenu,
                onExpandedChange = { showDurationMenu = it }
            ) {
                OutlinedTextField(
                    value         = selectedDuration.name,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Duration Type *") },
                    trailingIcon  = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDurationMenu)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded         = showDurationMenu,
                    onDismissRequest = { showDurationMenu = false }
                ) {
                    PackageDuration.entries.forEach { duration ->
                        DropdownMenuItem(
                            text    = { Text(duration.name) },
                            onClick = {
                                selectedDuration = duration
                                showDurationMenu = false
                            }
                        )
                    }
                }
            }

            // Show fixed nights field only when FIXED_NIGHTS duration is selected
            if (selectedDuration == PackageDuration.FIXED_NIGHTS) {
                OutlinedTextField(
                    value           = fixedNights,
                    onValueChange   = { fixedNights = it },
                    label           = { Text("Fixed Nights *") },
                    placeholder     = { Text("e.g. 3") },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Min and Max nights in one row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value           = minNights,
                    onValueChange   = { minNights = it },
                    label           = { Text("Min Nights") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value           = maxNights,
                    onValueChange   = { maxNights = it },
                    label           = { Text("Max Nights") },
                    placeholder     = { Text("Optional") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // ── Section: Pricing ──────────────────────────────────────────────
            PackageSectionHeader(title = "Pricing (PKR)")

            // Original and discounted price in one row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value           = originalPrice,
                    onValueChange   = { originalPrice = it },
                    label           = { Text("Original Price *") },
                    placeholder     = { Text("Per night") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value           = discountedPrice,
                    onValueChange   = { discountedPrice = it },
                    label           = { Text("Discounted Price *") },
                    placeholder     = { Text("Per night") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Optional discount percentage
            OutlinedTextField(
                value           = discountPercentage,
                onValueChange   = { discountPercentage = it },
                label           = { Text("Discount % (Optional)") },
                placeholder     = { Text("e.g. 25") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // ── Section: Availability ─────────────────────────────────────────
            PackageSectionHeader(title = "Availability")

            OutlinedTextField(
                value           = totalSlots,
                onValueChange   = { totalSlots = it },
                label           = { Text("Total Slots (Optional)") },
                placeholder     = { Text("Max number of bookings") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // ── Section: Inclusions ───────────────────────────────────────────
            PackageSectionHeader(title = "Inclusions")

            // Input row with add button
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = inclusionInput,
                    onValueChange = { inclusionInput = it },
                    label         = { Text("Add Inclusion") },
                    placeholder   = { Text("e.g. Free Breakfast") },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inclusionInput.isNotBlank()) {
                            inclusions     = inclusions + inclusionInput.trim()
                            inclusionInput = ""
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add inclusion",
                        tint               = primary
                    )
                }
            }

            // Display added inclusions as removable chips
            if (inclusions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    inclusions.forEach { item ->
                        InputChip(
                            selected     = false,
                            onClick      = {},
                            label        = { Text(item, fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(
                                    onClick  = { inclusions = inclusions - item },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove inclusion",
                                        modifier           = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // ── Validation error message ──────────────────────────────────────
            validationError?.let { err ->
                Text(
                    text     = err,
                    color    = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Submit button ─────────────────────────────────────────────────
            Button(
                onClick = {
                    // Validate required fields before saving
                    when {
                        packageName.isBlank() -> {
                            validationError = "Package name is required"
                            return@Button
                        }
                        description.isBlank() -> {
                            validationError = "Description is required"
                            return@Button
                        }
                        originalPrice.isBlank() || discountedPrice.isBlank() -> {
                            validationError = "Both original and discounted prices are required"
                            return@Button
                        }
                        (discountedPrice.toDoubleOrNull() ?: 0.0) >=
                                (originalPrice.toDoubleOrNull()   ?: 0.0) -> {
                            validationError = "Discounted price must be less than original price"
                            return@Button
                        }
                        selectedDuration == PackageDuration.FIXED_NIGHTS &&
                                fixedNights.isBlank() -> {
                            validationError = "Fixed nights is required for this duration type"
                            return@Button
                        }
                        else -> validationError = null
                    }

                    // Get current landlord uid from FirebaseAuth
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    // Build the RentalPackage object using form values
                    val newPackage = RentalPackage(
                        propertyId              = propertyId,
                        landlordId              = currentUserId,
                        packageName             = packageName.trim(),
                        description             = description.trim(),
                        badgeLabel              = badgeLabel.trim(),
                        durationType            = selectedDuration,
                        fixedNights             = fixedNights.toIntOrNull(),
                        minNights               = minNights.toIntOrNull() ?: 1,
                        maxNights               = maxNights.toIntOrNull(),
                        originalPricePerNight   = originalPrice.toDoubleOrNull()    ?: 0.0,
                        discountedPricePerNight = discountedPrice.toDoubleOrNull()  ?: 0.0,
                        discountPercentage      = discountPercentage.toFloatOrNull(),
                        inclusions              = inclusions,
                        totalSlots              = totalSlots.toIntOrNull()
                    )

                    viewModel.addRentalPackage(newPackage)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = primary),
                enabled  = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text       = "Create Package",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Section header helper composable ─────────────────────────────────────────
@Composable
private fun PackageSectionHeader(title: String) {
    Text(
        text       = title,
        fontSize   = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.primary
    )
    HorizontalDivider(
        color     = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        thickness = 1.dp
    )
}
