package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.viewmodel.VerificationViewModel

// Semantic colors — intentional, theme se bahar hain
private val GreenOk = Color(0xFF27AE60)
private val RedErr  = Color(0xFFE74C3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyVerificationDetailScreen(
    propertyId   : String,
    navController: NavController,
    viewModel    : VerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ════════════════════════════════════════════════════════════════════════
    // ✅ FIX: Property do jagah se dhundo:
    //   1. pendingProperties list (jab list screen se aaye)
    //   2. selectedProperty (jab direct Firestore fetch hua ho)
    //
    // Pehle list check karo, phir selectedProperty fallback.
    // ════════════════════════════════════════════════════════════════════════
    val property = remember(uiState.pendingProperties, uiState.selectedProperty, propertyId) {
        uiState.pendingProperties.find { it.propertyId == propertyId }
            ?: uiState.selectedProperty?.takeIf { it.propertyId == propertyId }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Load property if not already found ───────────────────────────────────
    // Jab screen open hoti hai aur property abhi tak null hai (direct navigation),
    // ViewModel se Firestore fetch trigger karo.
    LaunchedEffect(propertyId) {
        if (property == null && !uiState.isLoading) {
            viewModel.loadPropertyById(propertyId)
        }
    }

    // ── Re-check after loading ────────────────────────────────────────────────
    // Agar load ke baad bhi property null hai toh error dikhao.
    // Ye LaunchedEffect isLoading false hone par fire hoga.
    LaunchedEffect(uiState.isLoading, uiState.selectedProperty) {
        // Nothing to do — property recompose mein automatically update hogi
    }

    // ── Navigate back on approve/reject success ───────────────────────────────
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.resetActionState()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }

    var showRejectDialog  by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var rejectReason      by remember { mutableStateOf("") }
    var adminNote         by remember { mutableStateOf("") }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onBackground     = MaterialTheme.colorScheme.onBackground

    // ── Reject Dialog ─────────────────────────────────────────────────────────
    if (showRejectDialog && property != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(RedErr.copy(0.12f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Cancel, null, tint = RedErr, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text(
                    "Reject Property",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 17.sp,
                    color      = onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Please provide a reason for rejection.",
                        fontSize = 13.sp,
                        color    = onSurface.copy(0.55f)
                    )
                    OutlinedTextField(
                        value         = rejectReason,
                        onValueChange = { rejectReason = it },
                        label         = { Text("Reason for rejection") },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2,
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = tertiary,
                            unfocusedBorderColor = onSurface.copy(0.25f),
                            cursorColor          = tertiary,
                            focusedLabelColor    = tertiary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectProperty(
                            property  = property,
                            adminNote = rejectReason.ifEmpty { "Does not meet criteria" }
                        )
                        showRejectDialog = false
                    },
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = RedErr),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Reject", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showRejectDialog = false },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel") }
            }
        )
    }

    // ── Approve Dialog ────────────────────────────────────────────────────────
    if (showApproveDialog && property != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(GreenOk.copy(0.12f)),
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint     = GreenOk,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    "Approve Property",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 17.sp,
                    color      = onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Kya aap yeh property approve karna chahte hain?",
                        fontSize = 13.sp,
                        color    = onSurface.copy(0.55f)
                    )
                    OutlinedTextField(
                        value         = adminNote,
                        onValueChange = { adminNote = it },
                        label         = { Text("Admin note (optional)") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = tertiary,
                            unfocusedBorderColor = onSurface.copy(0.25f),
                            cursorColor          = tertiary,
                            focusedLabelColor    = tertiary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.approveProperty(property = property, adminNote = adminNote)
                        showApproveDialog = false
                    },
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor   = tertiary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Approve", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showApproveDialog = false },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = background,
        topBar = {
            // ── Gradient top bar ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tertiary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Property Review",
                            color         = onPrimary,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        // Show property title when loaded
                        property?.let {
                            Text(
                                it.title,
                                color    = tertiary.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                // Gold gradient bottom border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    background.copy(0f),
                                    tertiary,
                                    background.copy(0f)
                                )
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        },
        bottomBar = {
            // ── Approve / Reject buttons — only when property is loaded ────────
            if (property != null) {
                Surface(color = surface, tonalElevation = 2.dp) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showRejectDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled  = !uiState.isLoading,
                            shape    = RoundedCornerShape(12.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, RedErr),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedErr)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reject", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Button(
                            onClick  = { showApproveDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled  = !uiState.isLoading,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = primary,
                                contentColor   = tertiary
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color       = tertiary
                                )
                            } else {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Approve", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->

        // ════════════════════════════════════════════════════════════════════
        // MAIN CONTENT — three possible states:
        //   1. Loading  → spinner
        //   2. property == null after loading → "Property not found"
        //   3. property != null → full detail view
        // ════════════════════════════════════════════════════════════════════

        when {
            // ── State 1: Loading ──────────────────────────────────────────────
            uiState.isLoading && property == null -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(pad),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = tertiary, strokeWidth = 3.dp)
                        Text(
                            "Loading property...",
                            color    = onBackground.copy(0.55f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // ── State 2: Not found after loading attempt ───────────────────────
            !uiState.isLoading && property == null -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(pad),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            null,
                            tint     = RedErr,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Property not found",
                            color      = onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "ID: $propertyId",
                            color    = onBackground.copy(0.45f),
                            fontSize = 11.sp
                        )
                        // Retry button
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadPropertyById(propertyId) },
                            shape   = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            // ── State 3: Property loaded — show full detail ────────────────────
            else -> {
                val prop = property!! // safe because of when condition

                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(pad),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // ── Basic Information card ────────────────────────────────
                    item {
                        PVDPremiumDetailCard(
                            icon     = Icons.Default.Home,
                            title    = "Basic Information",
                            primary  = primary,
                            tertiary = tertiary,
                            surface  = surface,
                            onSurface = onSurface
                        ) {
                            PVDDetailRow("Title",      prop.title, onSurface)
                            PVDDetailRow(
                                "Type",
                                prop.propertyType.lowercase().replaceFirstChar { it.uppercase() },
                                onSurface
                            )
                            PVDDetailRow("Price",      prop.formattedPrice, onSurface)
                            PVDDetailRow(
                                "Address",
                                prop.address.ifEmpty { prop.city },
                                onSurface
                            )
                            PVDDetailRow("City",       prop.city, onSurface)
                            PVDDetailRow("Bedrooms",   prop.bedrooms.toString(), onSurface)
                            PVDDetailRow("Bathrooms",  prop.bathrooms.toString(), onSurface)
                            PVDDetailRow("Max Guests", prop.maxGuests.toString(), onSurface)
                            PVDDetailRow(
                                "Status",
                                prop.status.lowercase().replaceFirstChar { it.uppercase() },
                                onSurface
                            )
                            if (prop.adminNote.isNotEmpty()) {
                                PVDDetailRow("Admin Note", prop.adminNote, onSurface)
                            }
                        }
                    }

                    // ── Owner Information card ────────────────────────────────
                    item {
                        PVDPremiumDetailCard(
                            icon      = Icons.Default.Person,
                            title     = "Owner Information",
                            primary   = primary,
                            tertiary  = tertiary,
                            surface   = surface,
                            onSurface = onSurface
                        ) {
                            PVDDetailRow(
                                "Owner Name",
                                prop.ownerName.ifEmpty { "N/A" },
                                onSurface
                            )
                            PVDDetailRow("Owner ID", prop.ownerId, onSurface)
                        }
                    }

                    // ── Description card ──────────────────────────────────────
                    if (prop.description.isNotEmpty()) {
                        item {
                            PVDPremiumDetailCard(
                                icon      = Icons.Default.Description,
                                title     = "Description",
                                primary   = primary,
                                tertiary  = tertiary,
                                surface   = surface,
                                onSurface = onSurface
                            ) {
                                Text(
                                    prop.description,
                                    fontSize   = 13.sp,
                                    color      = onSurface.copy(alpha = 0.7f),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // ── Amenities card ────────────────────────────────────────
                    if (prop.amenities.isNotEmpty()) {
                        item {
                            PVDPremiumDetailCard(
                                icon      = Icons.Default.Checklist,
                                title     = "Amenities",
                                primary   = primary,
                                tertiary  = tertiary,
                                surface   = surface,
                                onSurface = onSurface
                            ) {
                                prop.amenities.chunked(3).forEach { rowItems ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier              = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        rowItems.forEach { amenity ->
                                            Surface(
                                                color    = tertiary.copy(0.10f),
                                                shape    = RoundedCornerShape(20.dp),
                                                modifier = Modifier.border(
                                                    1.dp,
                                                    tertiary.copy(0.25f),
                                                    RoundedCornerShape(20.dp)
                                                )
                                            ) {
                                                Text(
                                                    amenity,
                                                    fontSize   = 11.sp,
                                                    color      = tertiary,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier   = Modifier.padding(
                                                        horizontal = 9.dp,
                                                        vertical   = 4.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Property Photos section header ────────────────────────
                    item {
                        Row(
                            modifier          = Modifier.padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(tertiary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Property Photos",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = onBackground
                            )
                            Spacer(Modifier.weight(1f))
                            if (prop.imageUrls.isNotEmpty()) {
                                Surface(
                                    color = primary.copy(0.08f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        "${prop.imageUrls.size} photos",
                                        fontSize   = 11.sp,
                                        color      = primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier   = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical   = 4.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // ── Photos list or empty state ────────────────────────────
                    if (prop.imageUrls.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(surface)
                                    .border(1.dp, onSurface.copy(0.08f), RoundedCornerShape(14.dp))
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        null,
                                        tint     = onSurface.copy(0.3f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        "No photos uploaded",
                                        fontSize = 13.sp,
                                        color    = onSurface.copy(0.4f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Each photo rendered as a full-width card
                        items(prop.imageUrls) { imageUrl ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(3.dp, RoundedCornerShape(14.dp))
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model              = imageUrl,
                                    contentDescription = null,
                                    modifier           = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    contentScale       = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // ── PT-1 Document section header ──────────────────────────
                    item {
                        Row(
                            modifier          = Modifier.padding(
                                horizontal = 2.dp,
                                vertical   = 4.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(tertiary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "PT-1 Verification Document",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = onBackground
                            )
                        }
                    }

                    // ── PT-1 document card ────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    4.dp,
                                    RoundedCornerShape(16.dp),
                                    ambientColor = primary.copy(0.08f)
                                )
                        ) {
                            Card(
                                modifier  = Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(16.dp),
                                colors    = CardDefaults.cardColors(containerColor = surface),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                // Status indicator bar at top
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                if (prop.hasPt1Document)
                                                    listOf(primary, tertiary)
                                                else
                                                    listOf(RedErr, RedErr.copy(0.6f))
                                            )
                                        )
                                )
                                Column(
                                    modifier            = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (prop.hasPt1Document) primary
                                                    else RedErr.copy(0.12f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.VerifiedUser,
                                                null,
                                                tint     = if (prop.hasPt1Document) tertiary else RedErr,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                if (prop.hasPt1Document)
                                                    "PT-1 Document Uploaded"
                                                else
                                                    "PT-1 Document Not Uploaded",
                                                fontWeight = FontWeight.Bold,
                                                fontSize   = 14.sp,
                                                color      = if (prop.hasPt1Document) onSurface else RedErr
                                            )
                                            Text(
                                                if (prop.hasPt1Document)
                                                    "Document is available for review"
                                                else
                                                    "No document has been submitted",
                                                fontSize = 11.sp,
                                                color    = onSurface.copy(0.45f)
                                            )
                                        }
                                    }

                                    // Show preview only if document exists
                                    if (prop.hasPt1Document) {
                                        HorizontalDivider(color = onSurface.copy(0.07f))
                                        Text(
                                            "Document Preview",
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = onSurface.copy(0.6f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    1.dp,
                                                    onSurface.copy(0.1f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                        ) {
                                            AsyncImage(
                                                model              = prop.pt1DocumentUrl,
                                                contentDescription = "PT-1 Document",
                                                modifier           = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 200.dp, max = 400.dp),
                                                contentScale       = ContentScale.FillWidth
                                            )
                                        }
                                        Surface(
                                            color = tertiary.copy(0.08f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier              = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical   = 7.dp
                                                ),
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    null,
                                                    tint     = tertiary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    "Agar document PDF hai to image preview nahi aayegi",
                                                    fontSize = 11.sp,
                                                    color    = tertiary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// PREMIUM DETAIL CARD
// Consistent card with gradient top bar, icon circle, and titled content slot.
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun PVDPremiumDetailCard(
    icon     : ImageVector,
    title    : String,
    primary  : Color,
    tertiary : Color,
    surface  : Color,
    onSurface: Color,
    content  : @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                4.dp,
                RoundedCornerShape(16.dp),
                ambientColor = primary.copy(0.08f),
                spotColor    = primary.copy(0.10f)
            )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Gradient top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(primary, tertiary)))
            )
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header row with icon and title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            null,
                            tint     = tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        title,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = onSurface
                    )
                }
                HorizontalDivider(color = onSurface.copy(alpha = 0.07f))
                content()
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// DETAIL ROW
// Label + value pair used inside detail cards.
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun PVDDetailRow(label: String, value: String, onSurface: Color) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            color      = onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.weight(0.4f)
        )
        Text(
            text       = value,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp,
            color      = onSurface,
            modifier   = Modifier.weight(0.6f)
        )
    }
}
