package com.example.havenhub.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.PropertyViewModel

// ── Status colors ─────────────────────────────────────────────────────────────
private val StatusApproved = Color(0xFF22C55E)
private val StatusPending  = Color(0xFFF59E0B)
private val StatusRejected = Color(0xFFEF4444)
private val StatusDefault  = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPropertiesScreen(
    navController: NavController,
    viewModel    : PropertyViewModel = hiltViewModel()
) {
    val uiState            by viewModel.uiState.collectAsState()
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var selectedPropertyId by remember { mutableStateOf<String?>(null) }

    // Load on first composition
    LaunchedEffect(Unit) {
        viewModel.loadMyProperties()
    }

    // Reload after delete/edit succeeds
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            viewModel.loadMyProperties()
            viewModel.clearMessages()
        }
    }

    val primary          = MaterialTheme.colorScheme.primary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val background       = MaterialTheme.colorScheme.background
    val onBackground     = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            // Premium gradient top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(primary, primary.copy(alpha = 0.85f))
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = onPrimary
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "My Properties",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 20.sp,
                            color      = onPrimary
                        )
                        Text(
                            "Manage your listings",
                            fontSize = 11.sp,
                            color    = onPrimary.copy(alpha = 0.65f)
                        )
                    }
                    // Property count badge
                    if (uiState.myProperties.isNotEmpty()) {
                        Box(
                            modifier         = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(tertiary.copy(alpha = 0.18f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${uiState.myProperties.size} listed",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = tertiary
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        },
        floatingActionButton = {
            // Premium FAB with shadow
            FloatingActionButton(
                onClick        = { navController.navigate(Screen.AddProperty.route) },
                containerColor = primary,
                shape          = RoundedCornerShape(16.dp),
                modifier       = Modifier.shadow(12.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier       = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Property", tint = onPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Add New",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = onPrimary
                    )
                }
            }
        }
    ) { padding ->

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh    = { viewModel.loadMyProperties() },
            modifier     = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
            ) {
                when {
                    // Loading state
                    uiState.isLoading && uiState.myProperties.isEmpty() -> {
                        Column(
                            modifier            = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color       = primary,
                                strokeWidth = 3.dp,
                                modifier    = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Loading properties...",
                                fontSize = 13.sp,
                                color    = onSurfaceVariant
                            )
                        }
                    }

                    // Empty state
                    uiState.myProperties.isEmpty() -> {
                        Column(
                            modifier            = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Decorative empty state circle
                            Box(
                                modifier         = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(primary.copy(0.12f), primary.copy(0.04f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Home,
                                    contentDescription = null,
                                    modifier           = Modifier.size(48.dp),
                                    tint               = primary.copy(alpha = 0.45f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text       = "No Properties Yet",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text      = "Add your first property to start\nreceiving bookings",
                                fontSize  = 14.sp,
                                color     = onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 21.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text     = "Pull down to refresh",
                                fontSize = 12.sp,
                                color    = onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                    }

                    // Property list
                    else -> {
                        LazyColumn(
                            contentPadding      = PaddingValues(
                                start  = 16.dp,
                                end    = 16.dp,
                                top    = 16.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section header
                            item {
                                Row(
                                    modifier          = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .width(3.dp)
                                            .height(16.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(primary)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Active Listings",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = onSurfaceVariant
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "${uiState.myProperties.size} total",
                                        fontSize = 12.sp,
                                        color    = onSurfaceVariant.copy(0.6f)
                                    )
                                }
                            }

                            items(
                                items = uiState.myProperties,
                                key   = { it.propertyId }
                            ) { property ->
                                MyPropertyCard(
                                    property     = property,
                                    onClick      = {
                                        navController.navigate(
                                            Screen.PropertyDetail.createRoute(property.propertyId)
                                        )
                                    },
                                    onEdit       = {
                                        navController.navigate(
                                            Screen.EditProperty.createRoute(property.propertyId)
                                        )
                                    },
                                    onDelete     = {
                                        selectedPropertyId = property.propertyId
                                        showDeleteDialog   = true
                                    },
                                    onAddPackage = {
                                        navController.navigate(
                                            Screen.AddRentalPackage.createRoute(property.propertyId)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape            = RoundedCornerShape(20.dp),
            icon             = {
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.error,
                        modifier           = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete Property",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 18.sp
                )
            },
            text  = {
                Text(
                    "This action cannot be undone. The property and all its data will be permanently removed.",
                    fontSize   = 14.sp,
                    lineHeight = 21.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedPropertyId?.let { viewModel.deleteProperty(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// MyPropertyCard — Premium redesign
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun MyPropertyCard(
    property    : Property,
    onClick     : () -> Unit,
    onEdit      : () -> Unit,
    onDelete    : () -> Unit,
    onAddPackage: () -> Unit
) {
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val error            = MaterialTheme.colorScheme.error
    val surface          = MaterialTheme.colorScheme.surface
    val background       = MaterialTheme.colorScheme.background

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(0.dp) // shadow handles elevation
    ) {
        Column {
            // ── Hero Image with gradient overlay ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val remoteUrl = property.imageUrls.firstOrNull { it.startsWith("http") }

                when {
                    !remoteUrl.isNullOrEmpty() -> {
                        AsyncImage(
                            model              = remoteUrl,
                            contentDescription = property.title,
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    }
                    property.drawableImageName.isNotBlank() -> {
                        Image(
                            painter            = painterResource(
                                id = getPropertyImage(property.drawableImageName)
                            ),
                            contentDescription = property.title,
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    }
                    else -> {
                        val fallbackName = property.resolvedDrawableName
                            .ifEmpty { property.propertyId }
                        Image(
                            painter            = painterResource(
                                id = getPropertyImage(fallbackName)
                            ),
                            contentDescription = property.title,
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    }
                }

                // Bottom gradient overlay for text readability
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f   to Color.Transparent,
                            0.6f to Color.Transparent,
                            1f   to primary.copy(0.75f)
                        )
                    )
                )

                // Status badge — top left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(getMyPropStatusColor(property.status))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text       = getMyPropStatusLabel(property.status),
                        fontSize   = 11.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Price overlay — bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(primary.copy(0.9f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text       = property.formattedPrice + "/night",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = tertiary
                    )
                }

                // PT-1 badge if present
                if (property.hasPt1Document) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusApproved.copy(0.9f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "PT-1",
                                fontSize   = 10.sp,
                                color      = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Details section ───────────────────────────────────────────────
            Column(modifier = Modifier.padding(16.dp)) {

                // Title
                Text(
                    text       = property.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 17.sp,
                    color      = onSurface
                )

                Spacer(Modifier.height(6.dp))

                // Location row
                if (property.city.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier         = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(primary.copy(0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier           = Modifier.size(12.dp),
                                tint               = primary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text     = property.city,
                            fontSize = 13.sp,
                            color    = onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Divider
                HorizontalDivider(
                    color     = background,
                    thickness = 1.dp
                )
                Spacer(Modifier.height(12.dp))

                // Action buttons row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Edit button
                    OutlinedButton(
                        onClick  = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier           = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Edit",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Delete button
                    OutlinedButton(
                        onClick  = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier           = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Delete",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Add Package button — APPROVED properties only
                if (property.status.uppercase() == "APPROVED") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick  = onAddPackage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor   = tertiary
                        ),
                        elevation = ButtonDefaults.buttonElevation(2.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier           = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            "Add Package",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun getMyPropStatusColor(status: String): Color = when (status.uppercase()) {
    "APPROVED" -> StatusApproved
    "PENDING"  -> StatusPending
    "REJECTED" -> StatusRejected
    else       -> StatusDefault
}

private fun getMyPropStatusLabel(status: String): String = when (status.uppercase()) {
    "APPROVED" -> "✓ Approved"
    "PENDING"  -> "⏳ Pending"
    "REJECTED" -> "✗ Rejected"
    else       -> status
}















