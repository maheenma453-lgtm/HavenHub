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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.viewmodel.ManagementViewModel

// ── Semantic status colors — intentional, theme se bahar hain ─────────────────
private val StatusApproved    = Color(0xFF27AE60)
private val StatusPending     = Color(0xFFE67E22)
private val StatusRejected    = Color(0xFFE74C3C)
private val StatusUnderReview = Color(0xFF2980B9)
private val StatusInactive    = Color(0xFF95A5A6)
// ✅ NEW: BOOKED status color — teal to differentiate from occupied/unavailable
private val StatusBooked      = Color(0xFF00897B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePropertiesScreen(
    navController: NavController,
    viewModel    : ManagementViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    var showConfirmDialog    by remember { mutableStateOf(false) }
    var pendingAction        by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDialogTitle   by remember { mutableStateOf("") }
    var confirmDialogMessage by remember { mutableStateOf("") }

    val filteredProperties = remember(uiState.properties, searchQuery) {
        uiState.properties.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background

    // ── Confirmation dialog ───────────────────────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            shape            = RoundedCornerShape(16.dp),
            title = {
                Text(
                    confirmDialogTitle,
                    fontWeight = FontWeight.Bold,
                    color      = onSurface,
                    fontSize   = 18.sp
                )
            },
            text = {
                Text(
                    confirmDialogMessage,
                    color    = onSurface.copy(0.75f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingAction?.invoke()
                        showConfirmDialog = false
                        pendingAction    = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tertiary),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm", color = primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Cancel") }
            }
        )
    }

    Scaffold(
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = tertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            "Manage Properties",
                            color         = onPrimary,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            "${filteredProperties.size} properties",
                            color    = tertiary.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
                // Gold shimmer accent line at bottom of top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(background.copy(0f), tertiary, background.copy(0f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            // ── Search bar ────────────────────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            "Search properties...",
                            color    = onSurface.copy(0.4f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon   = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint               = tertiary
                        )
                    },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = tertiary,
                        unfocusedBorderColor    = onSurface.copy(0.25f),
                        focusedContainerColor   = surface,
                        unfocusedContainerColor = surface,
                        cursorColor             = tertiary,
                        focusedTextColor        = onSurface,
                        unfocusedTextColor      = onSurface
                    )
                )
            }

            // ── Property list ─────────────────────────────────────────────────
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = 4.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProperties) { property ->
                    MPPropertyCard(
                        property  = property,
                        primary   = primary,
                        tertiary  = tertiary,
                        surface   = surface,
                        onSurface = onSurface,
                        onApprove = {
                            confirmDialogTitle   = "Approve Property"
                            confirmDialogMessage = "Approve \"${property.title}\"?"
                            pendingAction        = {
                                viewModel.approveProperty(property.propertyId)
                            }
                            showConfirmDialog = true
                        },
                        onReject  = {
                            confirmDialogTitle   = "Reject Property"
                            confirmDialogMessage = "Reject \"${property.title}\"?"
                            pendingAction        = {
                                viewModel.removeProperty(property.propertyId)
                            }
                            showConfirmDialog = true
                        },
                        onDelete  = {
                            confirmDialogTitle   = "Delete Property"
                            confirmDialogMessage =
                                "Permanently delete \"${property.title}\"? This cannot be undone."
                            pendingAction        = {
                                viewModel.deleteProperty(property.propertyId)
                            }
                            showConfirmDialog = true
                        }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// PUBLIC WRAPPER
// Exposed for use from other screens (e.g. AdminDashboardScreen quick-action).
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PropertyManagementCard(
    property : Property,
    onApprove: () -> Unit,
    onReject : () -> Unit,
    onDelete : () -> Unit
) {
    val primary   = MaterialTheme.colorScheme.primary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val surface   = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    MPPropertyCard(property, primary, tertiary, surface, onSurface, onApprove, onReject, onDelete)
}

// ════════════════════════════════════════════════════════════════════════════
// CORE CARD COMPOSABLE
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun MPPropertyCard(
    property : Property,
    primary  : Color,
    tertiary : Color,
    surface  : Color,
    onSurface: Color,
    onApprove: () -> Unit,
    onReject : () -> Unit,
    onDelete : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // ════════════════════════════════════════════════════════════════════════
    // ✅ FIX: BOOKED branch added — 'when' must be exhaustive for sealed enum.
    //
    // Error was: "'when' expression must be exhaustive. Add the 'BOOKED'
    // branch or an 'else' branch." at ManagePropertiesScreen.kt:213
    //
    // Root cause: Property.kt mein BOOKED enum value add ki thi (previous fix)
    // lekin ManagePropertiesScreen ka 'when' expression update nahi hua tha.
    // Kotlin 'when' on sealed/enum classes requires ALL cases to be covered.
    // ════════════════════════════════════════════════════════════════════════
    val statusColor = when (property.propertyStatusEnum) {
        PropertyStatus.APPROVED     -> StatusApproved
        PropertyStatus.REJECTED     -> StatusRejected
        PropertyStatus.PENDING      -> StatusPending
        PropertyStatus.UNDER_REVIEW -> StatusUnderReview
        PropertyStatus.INACTIVE     -> StatusInactive
        PropertyStatus.BOOKED       -> StatusBooked    // ✅ NEW branch added
    }

    // ── IMAGE SOURCE RESOLUTION ───────────────────────────────────────────────
    // Priority 1: coverImageUrl  (first URL from imageUrls list — ImgBB URL)
    // Priority 2: resolvedDrawableName (city+type se auto-matched drawable)
    // Priority 3: fallback placeholder icon
    val imageModel: Any? = when {
        // ImgBB / remote URL available
        property.coverImageUrl.startsWith("http://") ||
                property.coverImageUrl.startsWith("https://") -> {
            ImageRequest.Builder(context)
                .data(property.coverImageUrl)
                .crossfade(true)
                .build()
        }
        // Drawable name available (manually seeded properties)
        property.resolvedDrawableName.isNotEmpty() -> {
            val resId = context.resources.getIdentifier(
                property.resolvedDrawableName, "drawable", context.packageName
            )
            if (resId != 0) resId else null
        }
        else -> null   // will show placeholder icon
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                4.dp,
                RoundedCornerShape(16.dp),
                ambientColor = primary.copy(0.08f),
                spotColor    = primary.copy(0.12f)
            ),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surface)
    ) {
        // Top gradient accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(primary, tertiary)))
        )

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Property thumbnail ────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primary),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    SubcomposeAsyncImage(
                        model              = imageModel,
                        contentDescription = property.title,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                        loading = {
                            // Spinner while image loads
                            Box(
                                modifier         = Modifier
                                    .fillMaxSize()
                                    .background(primary),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    color       = tertiary,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            // Fallback icon if URL fails to load
                            Icon(
                                imageVector        = Icons.Default.Home,
                                contentDescription = null,
                                tint               = tertiary,
                                modifier           = Modifier.size(32.dp)
                            )
                        }
                    )
                } else {
                    // No image source — show house icon placeholder
                    Icon(
                        imageVector        = Icons.Default.Home,
                        contentDescription = null,
                        tint               = tertiary,
                        modifier           = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Property info column ──────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    property.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = onSurface,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    property.address,
                    fontSize = 12.sp,
                    color    = onSurface.copy(0.55f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Status badge
                Surface(
                    color    = statusColor.copy(0.12f),
                    shape    = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(
                        1.dp,
                        statusColor.copy(0.35f),
                        RoundedCornerShape(6.dp)
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            property.propertyStatusEnum.displayName(),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = statusColor
                        )
                    }
                }
            }

            // ── Overflow menu ─────────────────────────────────────────────────
            Box {
                IconButton(
                    onClick  = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(primary.copy(0.06f))
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint               = onSurface,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded         = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier         = Modifier
                        .background(surface)
                        .width(160.dp)
                ) {
                    // Approve option
                    DropdownMenuItem(
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusApproved)
                            )
                        },
                        text    = {
                            Text(
                                "Approve",
                                color      = StatusApproved,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 14.sp
                            )
                        },
                        onClick = { menuExpanded = false; onApprove() }
                    )
                    HorizontalDivider(color = onSurface.copy(0.07f), thickness = 0.5.dp)

                    // Reject option
                    DropdownMenuItem(
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusPending)
                            )
                        },
                        text    = {
                            Text(
                                "Reject",
                                color      = StatusPending,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 14.sp
                            )
                        },
                        onClick = { menuExpanded = false; onReject() }
                    )
                    HorizontalDivider(color = onSurface.copy(0.07f), thickness = 0.5.dp)

                    // Delete option
                    DropdownMenuItem(
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusRejected)
                            )
                        },
                        text    = {
                            Text(
                                "Delete",
                                color      = StatusRejected,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 14.sp
                            )
                        },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}