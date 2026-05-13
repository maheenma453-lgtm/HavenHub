package com.example.havenhub.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// Semantic status colors — intentional, not themed
private val StatusApproved = Color(0xFF4CAF50)
private val StatusPending  = Color(0xFFFF9800)
private val StatusRejected = Color(0xFFF44336)
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

    LaunchedEffect(Unit) { viewModel.loadMyProperties() }

    val primary          = MaterialTheme.colorScheme.primary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val background       = MaterialTheme.colorScheme.background
    val onBackground     = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Properties", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = primary,
                    titleContentColor          = onPrimary,
                    navigationIconContentColor = onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { navController.navigate(Screen.AddProperty.route) },
                containerColor = primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = onPrimary)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(background)
        ) {
            when {
                // ── Loading ───────────────────────────────────────────────────
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = primary
                    )
                }

                // ── Empty state ───────────────────────────────────────────────
                uiState.myProperties.isEmpty() -> {
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier           = Modifier.size(80.dp),
                            tint               = onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No properties yet",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = onBackground
                        )
                    }
                }

                // ── Property list ─────────────────────────────────────────────
                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items = uiState.myProperties, key = { it.propertyId }) { property ->
                            MyPropertyCard(
                                property = property,
                                onClick  = {
                                    navController.navigate(
                                        Screen.PropertyDetail.createRoute(property.propertyId)
                                    )
                                },
                                onEdit   = {
                                    navController.navigate(
                                        Screen.EditProperty.createRoute(property.propertyId)
                                    )
                                },
                                onDelete = {
                                    selectedPropertyId = property.propertyId
                                    showDeleteDialog   = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Delete Property") },
            text             = { Text("Are you sure you want to delete this property?") },
            confirmButton    = {
                TextButton(onClick = {
                    selectedPropertyId?.let { viewModel.deleteProperty(it) }
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Property card composable ──────────────────────────────────────────────────
@Composable
fun MyPropertyCard(
    property: Property,
    onClick : () -> Unit,
    onEdit  : () -> Unit,
    onDelete: () -> Unit
) {
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary          = MaterialTheme.colorScheme.primary
    val error            = MaterialTheme.colorScheme.error

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // ✦ FIX: Pehle remote URL check karo, agar nahi toh drawable fallback
                // Exactly wahi logic jo HomeScreen aur PropertyDetailScreen mein use hota hai
                val remoteUrl = property.imageUrls.firstOrNull { it.isNotBlank() }

                when {
                    // Case 1: ImgBB ya koi bhi remote URL available hai → Coil se load karo
                    !remoteUrl.isNullOrEmpty() -> {
                        AsyncImage(
                            model              = remoteUrl,
                            contentDescription = property.title,
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    }

                    // Case 2: Remote URL nahi, drawable name ya propertyId se local image load karo
                    // drawableImageName → resolvedDrawableName → propertyId — isi order mein try karo
                    else -> {
                        val drawableRes = getPropertyImage(
                            property.drawableImageName.ifEmpty {
                                property.resolvedDrawableName.ifEmpty {
                                    property.propertyId
                                }
                            }
                        )
                        Image(
                            painter            = painterResource(id = drawableRes),
                            contentDescription = property.title,
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    }
                }

                // Status badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(getMyPropStatusColor(property.status).copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = getMyPropStatusLabel(property.status),
                        fontSize   = 11.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Title
                Text(
                    text       = property.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = onSurface
                )

                // City
                if (property.city.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier           = Modifier.size(14.dp),
                            tint               = onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text     = property.city,
                            fontSize = 12.sp,
                            color    = onSurfaceVariant
                        )
                    }
                }

                // Price
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = property.formattedPrice + "/night",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = primary
                )

                // PT-1 document status
                if (property.hasPt1Document) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier           = Modifier.size(13.dp),
                            tint               = StatusApproved
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text     = "PT-1 Uploaded",
                            fontSize = 11.sp,
                            color    = StatusApproved
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                // Edit / Delete buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = onEdit,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick  = onDelete,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Status color helper ───────────────────────────────────────────────────────
private fun getMyPropStatusColor(status: String): Color = when (status.uppercase()) {
    "APPROVED" -> StatusApproved
    "PENDING"  -> StatusPending
    "REJECTED" -> StatusRejected
    else       -> StatusDefault
}

// ── Status label helper ───────────────────────────────────────────────────────
private fun getMyPropStatusLabel(status: String): String = when (status.uppercase()) {
    "APPROVED" -> "✓ Approved"
    "PENDING"  -> "⏳ Pending"
    "REJECTED" -> "✗ Rejected"
    else       -> status
}