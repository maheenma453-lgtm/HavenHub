package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.PropertyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPropertiesScreen(
    navController : NavController,
    viewModel     : PropertyViewModel = hiltViewModel()
) {
    // ── ViewModel State (UiState Pattern) ──
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteDialog   by remember { mutableStateOf(false) }
    var selectedPropertyId by remember { mutableStateOf<String?>(null) }

    // Load properties on screen open
    LaunchedEffect(Unit) {
        viewModel.loadMyProperties()
    }

    // Handle Delete Success/Error via Snackbar if needed
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionSuccess, uiState.errorMessage) {
        if (uiState.actionSuccess) {
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Properties", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddProperty.route) },
                containerColor = PrimaryBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            when {
                // ── Loading State ──
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryBlue
                    )
                }

                // ── Error State ──
                uiState.errorMessage != null && uiState.myProperties.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(64.dp))
                        Text(text = uiState.errorMessage ?: "Unknown Error", color = ErrorRed)
                        Button(onClick = { viewModel.loadMyProperties() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Retry")
                        }
                    }
                }

                // ── Success State (List or Empty) ──
                else -> {
                    if (uiState.myProperties.isEmpty()) {
                        MyPropertiesEmptyState { navController.navigate(Screen.AddProperty.route) }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item { MyPropertiesSummaryRow(properties = uiState.myProperties) }

                            items(uiState.myProperties, key = { it.propertyId }) { property ->
                                MyPropertyCard(
                                    property = property,
                                    onClick = { navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId)) },
                                    onEdit = { /* Navigate to Edit */ },
                                    onDelete = {
                                        selectedPropertyId = property.propertyId
                                        showDeleteDialog = true
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ──
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Property") },
            text = { Text("Are you sure you want to delete this property? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedPropertyId?.let { viewModel.deleteProperty(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Summary Row ──────────────────────────────────────────────────
@Composable
private fun MyPropertiesSummaryRow(properties: List<Property>) {
    val activeCount = properties.count { it.status == PropertyStatus.APPROVED && it.isAvailable }
    val pendingCount = properties.count { it.status == PropertyStatus.PENDING || it.status == PropertyStatus.UNDER_REVIEW }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryChip(Modifier.weight(1f), "Total", properties.size, PrimaryBlue)
        SummaryChip(Modifier.weight(1f), "Active", activeCount, SuccessGreen)
        SummaryChip(Modifier.weight(1f), "Pending", pendingCount, WarningOrange)
    }
}

@Composable
private fun SummaryChip(modifier: Modifier, label: String, count: Int, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

// ─── Property Card ────────────────────────────────────────────────
@Composable
private fun MyPropertyCard(property: Property, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                AsyncImage(
                    model = property.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                PropertyStatusBadge(
                    status = property.status,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                )

                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White)

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = ErrorRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(property.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${property.city}, Pakistan", fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(property.formattedPrice, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Status Badge ─────────────────────────────────────────────────
@Composable
private fun PropertyStatusBadge(status: PropertyStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        PropertyStatus.APPROVED -> "Active" to SuccessGreen
        PropertyStatus.PENDING -> "Pending" to WarningOrange
        PropertyStatus.UNDER_REVIEW -> "Reviewing" to PrimaryBlue
        PropertyStatus.REJECTED -> "Rejected" to ErrorRed
        PropertyStatus.INACTIVE -> "Inactive" to TextSecondary
    }

    Surface(modifier = modifier, shape = RoundedCornerShape(4.dp), color = color) {
        Text(label, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

// ─── Empty State ──────────────────────────────────────────────────
@Composable
private fun MyPropertiesEmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.HomeWork, null, modifier = Modifier.size(80.dp), tint = BorderGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Properties Yet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("List your first property to start earning.", textAlign = TextAlign.Center, color = TextSecondary)
        Button(onClick = onAdd, modifier = Modifier.padding(top = 24.dp).fillMaxWidth(), colors = ButtonDefaults.buttonColors(PrimaryBlue)) {
            Text("Add Property")
        }
    }
}