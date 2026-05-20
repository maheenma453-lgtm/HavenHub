package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.PackageDuration
import com.example.havenhub.data.RentalPackage
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.PropertyViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRentalPackageScreen(
    navController: NavController,
    propertyId   : String,
    viewModel    : PropertyViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val isDark      = isSystemInDarkTheme()

    // ── Theme tokens ──────────────────────────────────────────────────────────
    val screenBg = if (isDark) DarkBg         else BackgroundLight
    val gold     = if (isDark) DarkGoldPrimary else GoldAccent
    val navy     = if (isDark) DarkBgSecondary else PrimaryNavy

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

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            snackbarHostState.showSnackbar("Package created successfully!")
            viewModel.clearMessages()
            navController.popBackStack()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = screenBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp)
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
                                "Add Rental Package",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = if (isDark) DarkTextPrimary else Color.White
                            )
                            Text(
                                "Create a special deal for your property",
                                fontSize = 11.sp,
                                color    = gold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                tint = if (isDark) DarkTextPrimary else Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(screenBg)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Package Info ──────────────────────────────────────────────────
            PkgCard(isDark = isDark) {
                PkgSectionHeader("Package Info", Icons.Default.LocalOffer, isDark)

                PkgField(
                    value         = packageName,
                    onValueChange = { packageName = it },
                    label         = "Package Name *",
                    placeholder   = "e.g. Summer Escape Deal",
                    isDark        = isDark
                )
                PkgField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = "Description *",
                    placeholder   = "Describe what makes this package special",
                    isDark        = isDark,
                    minLines      = 3,
                    maxLines      = 5
                )

                // Badge label with gold preview
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PkgField(
                        value         = badgeLabel,
                        onValueChange = { badgeLabel = it },
                        label         = "Badge Label",
                        placeholder   = "e.g. 🔥 Summer Deal",
                        isDark        = isDark
                    )
                    if (badgeLabel.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(gold.copy(0.15f))
                                .border(1.dp, gold.copy(0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                badgeLabel,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = gold
                            )
                        }
                    }
                }
            }

            // ── Duration ─────────────────────────────────────────────────────
            PkgCard(isDark = isDark) {
                PkgSectionHeader("Duration Type", Icons.Default.Schedule, isDark)

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
                            .menuAnchor(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = pkgFieldColors(isDark)
                    )
                    ExposedDropdownMenu(
                        expanded         = showDurationMenu,
                        onDismissRequest = { showDurationMenu = false },
                        containerColor   = if (isDark) DarkSurface else SurfaceWhite
                    ) {
                        PackageDuration.entries.forEach { duration ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        duration.name,
                                        color = if (isDark) DarkTextPrimary else PrimaryNavy
                                    )
                                },
                                onClick = {
                                    selectedDuration = duration
                                    showDurationMenu = false
                                }
                            )
                        }
                    }
                }

                if (selectedDuration == PackageDuration.FIXED_NIGHTS) {
                    PkgField(
                        value           = fixedNights,
                        onValueChange   = { fixedNights = it },
                        label           = "Fixed Nights *",
                        placeholder     = "e.g. 3",
                        isDark          = isDark,
                        keyboardType    = KeyboardType.Number
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PkgField(
                        value        = minNights,
                        onValueChange = { minNights = it },
                        label        = "Min Nights",
                        isDark       = isDark,
                        keyboardType = KeyboardType.Number,
                        modifier     = Modifier.weight(1f)
                    )
                    PkgField(
                        value        = maxNights,
                        onValueChange = { maxNights = it },
                        label        = "Max Nights",
                        placeholder  = "Optional",
                        isDark       = isDark,
                        keyboardType = KeyboardType.Number,
                        modifier     = Modifier.weight(1f)
                    )
                }
            }

            // ── Pricing ───────────────────────────────────────────────────────
            PkgCard(isDark = isDark) {
                PkgSectionHeader("Pricing (PKR)", Icons.Default.CurrencyRupee, isDark)

                // Discount summary badge (live preview)
                val orig = originalPrice.toDoubleOrNull() ?: 0.0
                val disc = discountedPrice.toDoubleOrNull() ?: 0.0
                if (orig > 0 && disc > 0 && disc < orig) {
                    val saving = ((orig - disc) / orig * 100).toInt()
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDark) DarkGoldFaint else Color(0xFFFFF8E1)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingDown,
                            null,
                            tint     = if (isDark) DarkGoldPrimary else GoldAccentDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Saving $saving% · PKR ${"%,.0f".format(orig - disc)} off per night",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isDark) DarkGoldLight else GoldAccentDark
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PkgField(
                        value        = originalPrice,
                        onValueChange = { originalPrice = it },
                        label        = "Original Price *",
                        placeholder  = "Per night",
                        isDark       = isDark,
                        keyboardType = KeyboardType.Number,
                        modifier     = Modifier.weight(1f)
                    )
                    PkgField(
                        value        = discountedPrice,
                        onValueChange = { discountedPrice = it },
                        label        = "Discounted Price *",
                        placeholder  = "Per night",
                        isDark       = isDark,
                        keyboardType = KeyboardType.Number,
                        modifier     = Modifier.weight(1f)
                    )
                }

                PkgField(
                    value        = discountPercentage,
                    onValueChange = { discountPercentage = it },
                    label        = "Discount % (Optional)",
                    placeholder  = "e.g. 25",
                    isDark       = isDark,
                    keyboardType = KeyboardType.Number
                )
            }

            // ── Availability ──────────────────────────────────────────────────
            PkgCard(isDark = isDark) {
                PkgSectionHeader("Availability", Icons.Default.EventAvailable, isDark)

                PkgField(
                    value        = totalSlots,
                    onValueChange = { totalSlots = it },
                    label        = "Total Slots (Optional)",
                    placeholder  = "Max number of bookings",
                    isDark       = isDark,
                    keyboardType = KeyboardType.Number
                )
            }

            // ── Inclusions ────────────────────────────────────────────────────
            PkgCard(isDark = isDark) {
                PkgSectionHeader("Inclusions", Icons.Default.Checklist, isDark)

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
                        singleLine    = true,
                        shape         = RoundedCornerShape(14.dp),
                        colors        = pkgFieldColors(isDark)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDark) DarkBgElevated else PrimaryNavy
                            )
                            .clickable {
                                if (inclusionInput.isNotBlank()) {
                                    inclusions     = inclusions + inclusionInput.trim()
                                    inclusionInput = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint     = if (isDark) DarkGoldPrimary else GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (inclusions.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        inclusions.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isDark) DarkBgElevated else PrimaryNavy.copy(0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDark) DarkGoldPrimary.copy(0.3f) else GoldAccent.copy(0.3f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint     = if (isDark) DarkGoldPrimary else GoldAccentDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    item,
                                    fontSize = 12.sp,
                                    color    = if (isDark) DarkTextPrimary else PrimaryNavy
                                )
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint     = if (isDark) DarkTextSecondary else Color(0xFF8899AA),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { inclusions = inclusions - item }
                                )
                            }
                        }
                    }
                }
            }

            // ── Validation error ──────────────────────────────────────────────
            validationError?.let { err ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkError.copy(0.1f))
                        .border(1.dp, DarkError.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, null,
                        tint = DarkError, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(err, color = DarkError, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Submit button ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (uiState.isLoading)
                            Brush.horizontalGradient(listOf(
                                if (isDark) DarkBgElevated else PrimaryNavy.copy(0.5f),
                                if (isDark) DarkBgTertiary else PrimaryNavyLight.copy(0.5f)
                            ))
                        else
                            Brush.horizontalGradient(listOf(
                                if (isDark) DarkBgSecondary else PrimaryNavyDark,
                                if (isDark) DarkBgElevated  else PrimaryNavy,
                                if (isDark) DarkBgTertiary  else PrimaryNavyLight
                            ))
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isDark) DarkGoldPrimary.copy(0.6f) else GoldAccent.copy(0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = !uiState.isLoading) {
                        when {
                            packageName.isBlank() -> {
                                validationError = "Package name is required"
                            }
                            description.isBlank() -> {
                                validationError = "Description is required"
                            }
                            originalPrice.isBlank() || discountedPrice.isBlank() -> {
                                validationError = "Both original and discounted prices are required"
                            }
                            (discountedPrice.toDoubleOrNull() ?: 0.0) >=
                                    (originalPrice.toDoubleOrNull() ?: 0.0) -> {
                                validationError = "Discounted price must be less than original"
                            }
                            selectedDuration == PackageDuration.FIXED_NIGHTS &&
                                    fixedNights.isBlank() -> {
                                validationError = "Fixed nights required for this duration type"
                            }
                            else -> {
                                validationError = null
                                val currentUserId =
                                    FirebaseAuth.getInstance().currentUser?.uid ?: ""
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
                                    originalPricePerNight   = originalPrice.toDoubleOrNull()   ?: 0.0,
                                    discountedPricePerNight = discountedPrice.toDoubleOrNull() ?: 0.0,
                                    discountPercentage      = discountPercentage.toFloatOrNull(),
                                    inclusions              = inclusions,
                                    totalSlots              = totalSlots.toIntOrNull()
                                )
                                viewModel.addRentalPackage(newPackage)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        color       = if (isDark) DarkGoldPrimary else GoldAccent,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalOffer,
                            null,
                            tint     = if (isDark) DarkGoldPrimary else GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Create Package",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (isDark) DarkGoldPrimary else GoldAccent
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SHARED HELPERS for AddRentalPackageScreen
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PkgCard(isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val cardBg  = if (isDark) DarkSurface else SurfaceWhite
    val border  = if (isDark) DarkBorder  else Color(0xFFE8EAF0)

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .border(
                width = if (isDark) 1.dp else 0.5.dp,
                color = border,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content             = content
        )
    }
}

@Composable
private fun PkgSectionHeader(title: String, icon: ImageVector, isDark: Boolean) {
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
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
    }
}

@Composable
private fun pkgFieldColors(isDark: Boolean): TextFieldColors {
    val gold      = if (isDark) DarkGoldPrimary   else GoldAccent
    val textColor = if (isDark) DarkTextPrimary   else PrimaryNavy
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

@Composable
private fun PkgField(
    value         : String,
    onValueChange : (String) -> Unit,
    label         : String,
    placeholder   : String   = "",
    isDark        : Boolean,
    keyboardType  : KeyboardType = KeyboardType.Text,
    minLines      : Int      = 1,
    maxLines      : Int      = 1,
    modifier      : Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label) },
        placeholder     = if (placeholder.isNotEmpty()) {
            { Text(placeholder, fontSize = 13.sp) }
        } else null,
        modifier        = modifier,
        singleLine      = maxLines == 1,
        minLines        = minLines,
        maxLines        = maxLines,
        shape           = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors          = pkgFieldColors(isDark)
    )
}





















