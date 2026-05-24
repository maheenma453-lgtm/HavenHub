package com.example.havenhub.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.havenhub.viewmodel.PropertyViewModel

private val EDIT_PROPERTY_TYPES = PropertyType.entries.map { it.displayName() }
private val EDIT_AMENITIES_LIST = listOf(
    "WiFi", "Parking", "Generator", "Air Conditioning", "Heating",
    "Swimming Pool", "Gym", "Security Guard", "CCTV", "Elevator",
    "Laundry", "Garden", "Furnished", "Kitchen", "Balcony"
)

// ══════════════════════════════════════════════════════════════════════════════
// COLOR TOKENS — Logo navy + gold palette
// ══════════════════════════════════════════════════════════════════════════════
private object EPR {
    val NavyPrimary  = Color(0xFF1B2B5B)
    val NavyDark     = Color(0xFF0F1F45)
    val NavyMid      = Color(0xFF253672)
    val GoldAccent   = Color(0xFFD4AF37)   // logo gold — card borders, focus, accents
    val GoldLight    = Color(0xFFF0D060)
    val GoldDim      = Color(0xFFB8962E)

    // Light theme
    val LightBg      = Color(0xFFEEF1F8)
    val LightCard    = Color(0xFFFFFFFF)
    val LightHead    = Color(0xFFFAFBFE)   // card header bg
    val LightBorder  = Color(0xFFE2E6F0)
    val LightText    = Color(0xFF0F1933)
    val LightTextSec = Color(0xFF5A6480)
    val LightTextHnt = Color(0xFF9AA3BB)
    val LightIconBg  = Color(0xFF1B2B5B)   // navy icon bg
    val LightChipBg  = Color(0xFFF4F6FC)
    val LightInputBg = Color(0xFFF7F8FC)

    // Dark theme
    val DarkBg       = Color(0xFF060D1A)
    val DarkCard     = Color(0xFF0F1E3A)
    val DarkHead     = Color(0xFF0B1830)
    val DarkNavyBar  = Color(0xFF0D1B3E)
    val DarkBorder   = Color(0xFF1E2E50)
    val DarkTextPri  = Color(0xFFF0F4FF)
    val DarkTextSec  = Color(0xFF8899BB)
    val DarkIconBg   = Color(0xFF1B2B5B)
    val DarkChipBg   = Color(0xFF1A2A50)
    val DarkInputBg  = Color(0xFF0D1B3E)

    val ErrorRed     = Color(0xFFE74C3C)
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPropertyScreen(
    propertyId    : String,
    navController : NavController,
    viewModel     : PropertyViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()

    val pageBg   = if (isDark) EPR.DarkBg      else EPR.LightBg
    val cardBg   = if (isDark) EPR.DarkCard    else EPR.LightCard
    val topBarBg = if (isDark) EPR.DarkNavyBar  else EPR.NavyPrimary
    val accentC  = if (isDark) EPR.GoldAccent   else EPR.NavyPrimary
    val textSec  = if (isDark) EPR.DarkTextSec  else EPR.LightTextSec
    val borderC  = if (isDark) EPR.DarkBorder   else EPR.LightBorder

    val uiState           = viewModel.uiState.collectAsState()
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

    val listState   = rememberLazyListState()
    val showSaveBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 80
        }
    }

    LaunchedEffect(propertyId) { viewModel.loadPropertyDetail(propertyId) }

    LaunchedEffect(uiState.value.propertyDetail) {
        uiState.value.propertyDetail?.let { prop ->
            if (!isFormInitialized) {
                title             = prop.title
                description       = prop.description
                propertyType      = try {
                    PropertyType.valueOf(prop.propertyType).displayName()
                } catch (_: Exception) { "Apartment" }
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

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor   = cardBg,
            title = {
                Text(
                    "Discard Changes?",
                    color      = if (isDark) EPR.DarkTextPri else EPR.LightText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "You have unsaved changes. Are you sure you want to go back?",
                    color = textSec
                )
            },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Discard", color = EPR.ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing", color = accentC)
                }
            }
        )
    }

    val onSave: () -> Unit = {
        uiState.value.propertyDetail?.let { currentProp ->
            val propTypeString = PropertyType.entries
                .find { it.displayName() == propertyType }
                ?.name ?: currentProp.propertyType

            val updatedProperty = currentProp.copy(
                title         = title,
                description   = description,
                propertyType  = propTypeString,
                pricePerNight = pricePerNight.toDoubleOrNull() ?: currentProp.pricePerNight,
                bedrooms      = bedrooms.toIntOrNull()         ?: currentProp.bedrooms,
                bathrooms     = bathrooms.toIntOrNull()        ?: currentProp.bathrooms,
                areaSqFt      = area.toDoubleOrNull(),
                amenities     = selectedAmenities.toList(),
                city          = city,
                address       = address,
                imageUrls     = existingImageUrls.filterNot { it in removedImageUrls }
            )
            viewModel.updateProperty(updatedProperty, newImageUris)
        }
    }

    Scaffold(
        containerColor = pageBg,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                "Edit Property",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 17.sp,
                                color      = Color.White
                            )
                            AnimatedVisibility(
                                visible = hasUnsavedChanges,
                                enter   = fadeIn(),
                                exit    = fadeOut()
                            ) {
                                Text(
                                    "Unsaved changes",
                                    fontSize = 11.sp,
                                    color    = EPR.GoldLight.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        AnimatedVisibility(
                            visible = hasUnsavedChanges,
                            enter   = fadeIn() + scaleIn(),
                            exit    = fadeOut() + scaleOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = EPR.GoldAccent.copy(alpha = 0.2f),
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    "● Unsaved",
                                    modifier   = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = EPR.GoldLight,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showDiscardDialog = true
                        else navController.popBackStack()
                    }) {
                        Box(
                            modifier         = Modifier
                                .size(36.dp)
                                .background(
                                    EPR.GoldAccent.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    EPR.GoldAccent.copy(alpha = 0.3f),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint               = EPR.GoldAccent,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = topBarBg,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        // Gold accent line under topbar
        bottomBar = {
            Column {
                // Gold top-line on topbar bottom
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.dp)
                )
                AnimatedVisibility(
                    visible = showSaveBar,
                    enter   = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec  = tween(durationMillis = 320)
                    ),
                    exit    = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 260)
                    )
                ) {
                    EPRBottomSaveBar(
                        isLoading = uiState.value.isLoading,
                        isDark    = isDark,
                        onSave    = onSave
                    )
                }
            }
        }
    ) { paddingValues ->

        if (uiState.value.isLoading && !isFormInitialized) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EPR.GoldAccent)
            }
            return@Scaffold
        }

        // Gold line below topbar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(EPR.GoldAccent)
            )

            LazyColumn(
                state               = listState,
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(14.dp)) }

                // ── Basic Information ─────────────────────────────────────
                item {
                    EPRSectionCard("Basic Information", Icons.Default.Info, isDark) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            EPRTextField(
                                value         = title,
                                onValueChange = { title = it; hasUnsavedChanges = true },
                                label         = "PROPERTY TITLE",
                                isDark        = isDark
                            )
                            EPRTextField(
                                value         = description,
                                onValueChange = { description = it; hasUnsavedChanges = true },
                                label         = "DESCRIPTION",
                                isDark        = isDark,
                                minLines      = 3
                            )
                            Text(
                                "PROPERTY TYPE",
                                fontSize      = 10.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = if (isDark) EPR.DarkTextSec else EPR.LightTextHnt,
                                letterSpacing = 0.7.sp
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(EDIT_PROPERTY_TYPES) { type ->
                                    val isSelected = propertyType == type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick  = { propertyType = type; hasUnsavedChanges = true },
                                        label    = {
                                            Text(
                                                type,
                                                fontWeight = FontWeight.Bold,
                                                fontSize   = 12.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            // Selected: gold bg + navy text — premium look
                                            selectedContainerColor = EPR.GoldAccent,
                                            selectedLabelColor     = EPR.NavyDark,
                                            containerColor         = if (isDark) EPR.DarkChipBg else EPR.LightChipBg,
                                            labelColor             = if (isDark) EPR.DarkTextSec else EPR.LightTextSec
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled             = true,
                                            selected            = isSelected,
                                            selectedBorderColor = EPR.GoldDim,
                                            selectedBorderWidth = 1.5.dp,
                                            borderColor         = borderC,
                                            borderWidth         = 1.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Pricing & Details ─────────────────────────────────────
                item {
                    EPRSectionCard("Pricing & Details", Icons.Default.AttachMoney, isDark) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            EPRTextField(
                                value           = pricePerNight,
                                onValueChange   = { pricePerNight = it; hasUnsavedChanges = true },
                                label           = "PRICE / NIGHT (PKR)",
                                isDark          = isDark,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier              = Modifier.fillMaxWidth()
                            ) {
                                EPRTextField(
                                    value           = bedrooms,
                                    onValueChange   = { bedrooms = it; hasUnsavedChanges = true },
                                    label           = "BEDROOMS",
                                    isDark          = isDark,
                                    modifier        = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                EPRTextField(
                                    value           = bathrooms,
                                    onValueChange   = { bathrooms = it; hasUnsavedChanges = true },
                                    label           = "BATHROOMS",
                                    isDark          = isDark,
                                    modifier        = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            EPRTextField(
                                value           = area,
                                onValueChange   = { area = it; hasUnsavedChanges = true },
                                label           = "AREA (SQ FT)",
                                isDark          = isDark,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                // ── Location ──────────────────────────────────────────────
                item {
                    EPRSectionCard("Location", Icons.Default.LocationOn, isDark) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier              = Modifier.fillMaxWidth()
                            ) {
                                EPRTextField(
                                    value         = city,
                                    onValueChange = { city = it; hasUnsavedChanges = true },
                                    label         = "CITY",
                                    isDark        = isDark,
                                    modifier      = Modifier.weight(1f)
                                )
                                EPRTextField(
                                    value         = address,
                                    onValueChange = { address = it; hasUnsavedChanges = true },
                                    label         = "AREA / STREET",
                                    isDark        = isDark,
                                    modifier      = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ── Amenities ─────────────────────────────────────────────
                item {
                    EPRSectionCard("Amenities", Icons.Default.Checklist, isDark) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EDIT_AMENITIES_LIST.chunked(3).forEach { row ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier              = Modifier.fillMaxWidth()
                                ) {
                                    row.forEach { am ->
                                        val isSelected = am in selectedAmenities
                                        FilterChip(
                                            selected = isSelected,
                                            onClick  = {
                                                selectedAmenities =
                                                    if (isSelected) selectedAmenities - am
                                                    else selectedAmenities + am
                                                hasUnsavedChanges = true
                                            },
                                            label = {
                                                Text(
                                                    am,
                                                    fontSize   = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines   = 1
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors   = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EPR.NavyPrimary,
                                                selectedLabelColor     = Color.White,
                                                containerColor         = if (isDark) EPR.DarkChipBg else EPR.LightChipBg,
                                                labelColor             = if (isDark) EPR.DarkTextSec else EPR.LightTextSec
                                            ),
                                            border   = FilterChipDefaults.filterChipBorder(
                                                enabled             = true,
                                                selected            = isSelected,
                                                selectedBorderColor = EPR.GoldAccent,
                                                selectedBorderWidth = 1.5.dp,
                                                borderColor         = borderC,
                                                borderWidth         = 1.dp
                                            )
                                        )
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }

                // ── Photos ────────────────────────────────────────────────
                item {
                    EPRPhotoSection(
                        existing    = existingImageUrls,
                        removed     = removedImageUrls,
                        new         = newImageUris,
                        onRemoveOld = { removedImageUrls = removedImageUrls + it; hasUnsavedChanges = true },
                        onAddNew    = { newImageUris     = newImageUris     + it; hasUnsavedChanges = true },
                        onRemoveNew = { newImageUris     = newImageUris     - it; hasUnsavedChanges = true },
                        isDark      = isDark
                    )
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════

// ── Text Field ────────────────────────────────────────────────────────────────
@Composable
private fun EPRTextField(
    value          : String,
    onValueChange  : (String) -> Unit,
    label          : String,
    isDark         : Boolean         = false,
    readOnly       : Boolean         = false,
    minLines       : Int             = 1,
    modifier       : Modifier        = Modifier.fillMaxWidth(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        },
        modifier        = modifier,
        readOnly        = readOnly,
        minLines        = minLines,
        keyboardOptions = keyboardOptions,
        shape           = RoundedCornerShape(10.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = EPR.GoldAccent,
            unfocusedBorderColor    = if (isDark) EPR.DarkBorder  else EPR.LightBorder,
            focusedLabelColor       = EPR.GoldAccent,
            unfocusedLabelColor     = if (isDark) EPR.DarkTextSec else EPR.LightTextHnt,
            focusedTextColor        = if (isDark) EPR.DarkTextPri else EPR.LightText,
            unfocusedTextColor      = if (isDark) EPR.DarkTextPri else EPR.LightText,
            focusedContainerColor   = if (isDark) EPR.DarkInputBg else EPR.LightInputBg,
            unfocusedContainerColor = if (isDark) EPR.DarkInputBg else EPR.LightInputBg,
            cursorColor             = EPR.GoldAccent
        )
    )
}

// ── Section Card — gold outline border ────────────────────────────────────────
@Composable
private fun EPRSectionCard(
    title  : String,
    icon   : ImageVector,
    isDark : Boolean = false,
    content: @Composable () -> Unit
) {
    val cardBg  = if (isDark) EPR.DarkCard else EPR.LightCard
    val headBg  = if (isDark) EPR.DarkHead else EPR.LightHead
    val textPri = if (isDark) EPR.DarkTextPri else EPR.LightText

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            // ── GOLD OUTLINE ── this is the premium border on every card
            .border(
                width = 1.5.dp,
                color = EPR.GoldAccent.copy(alpha = if (isDark) 0.55f else 0.45f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // no shadow — border does the work
    ) {
        Column {
            // ── Card header with navy icon bg + gold icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(headBg)
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(34.dp)
                        .background(EPR.NavyPrimary, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint               = EPR.GoldAccent,   // gold icon on navy bg
                        modifier           = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 13.sp,
                    color         = textPri,
                    letterSpacing = 0.2.sp
                )
            }
            // ── Gold divider line between header and body
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EPR.GoldAccent.copy(alpha = if (isDark) 0.35f else 0.25f))
            )
            // ── Card body
            Column(Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

// ── Photo section ─────────────────────────────────────────────────────────────
@Composable
private fun EPRPhotoSection(
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

    EPRSectionCard("Photos", Icons.Default.PhotoLibrary, isDark) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(activeOld) { url -> EPRImageThumb(model = url) { onRemoveOld(url) } }
                items(new)       { uri -> EPRImageThumb(model = uri) { onRemoveNew(uri) } }
            }
            OutlinedButton(
                onClick  = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = EPR.NavyPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = EPR.GoldAccent.copy(alpha = 0.5f)   // gold border on add button too
                )
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint               = EPR.NavyPrimary,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add More Photos",
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 13.sp,
                    color         = if (isDark) EPR.DarkTextPri else EPR.NavyPrimary,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

// ── Image thumbnail ───────────────────────────────────────────────────────────
@Composable
private fun EPRImageThumb(model: Any?, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, EPR.GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
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
                .padding(3.dp)
                .background(EPR.NavyDark.copy(alpha = 0.7f), CircleShape)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint               = EPR.GoldAccent,
                modifier           = Modifier.size(12.dp)
            )
        }
    }
}

// ── Bottom Save Bar ───────────────────────────────────────────────────────────
@Composable
private fun EPRBottomSaveBar(
    isLoading: Boolean,
    isDark   : Boolean    = false,
    onSave   : () -> Unit
) {
    Surface(
        color           = if (isDark) EPR.DarkNavyBar else EPR.LightCard,
        tonalElevation  = 0.dp,
        shadowElevation = 16.dp
    ) {
        Column {
            // Gold accent line at very top of save bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(EPR.GoldAccent)
            )
            Button(
                onClick   = onSave,
                enabled   = !isLoading,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp),
                shape     = RoundedCornerShape(13.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor         = EPR.NavyPrimary,
                    contentColor           = Color.White,
                    disabledContainerColor = EPR.NavyPrimary.copy(alpha = 0.45f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        color       = EPR.GoldAccent,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        tint               = EPR.GoldAccent,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Save Changes",
                        fontSize      = 15.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }
            }
            Text(
                "Changes will be saved to your listing",
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                fontSize  = 10.sp,
                color     = if (isDark) EPR.DarkTextSec else EPR.LightTextHnt,
                fontWeight = FontWeight.Medium,
                textAlign  = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
