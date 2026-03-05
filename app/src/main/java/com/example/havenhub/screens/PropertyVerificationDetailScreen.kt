package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.viewmodel.VerificationViewModel

// ─────────────────────────────────────────────────────────────────
// PropertyVerificationDetailScreen.kt
// FIX: approveProperty() ViewModel mein nahi —
//      AdminRepository mein bhi nahi hai
//      Approve button ko disable + note show kiya
// FIX: property.location.address → property.address direct field
// FIX: property.images → property.imageUrls (Property.kt ka sahi field)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyVerificationDetailScreen(
    propertyId: String,
    navController: NavController,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val property = remember(uiState.pendingProperties, propertyId) {
        uiState.pendingProperties.find { it.propertyId == propertyId }
    }

    // Navigate back after successful action
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.resetActionState()
        }
    }

    // Show error in snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }

    // Reject reason dialog state
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Property") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectProperty(propertyId, rejectReason.ifEmpty { "Does not meet criteria" })
                        showRejectDialog = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Property Review") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (property != null) {
                PropertyBottomBar(
                    // FIX: approveProperty nahi — reject only available hai
                    onReject  = { showRejectDialog = true },
                    isLoading = uiState.isLoading
                )
            }
        }
    ) { pad ->
        if (property == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) CircularProgressIndicator()
                else Text("Property not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {

                // ── Basic Info Card ────────────────────────────────
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Basic Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider()
                            DetailItem("Title",    property.title)
                            DetailItem("Type",     property.propertyType.displayName())
                            DetailItem("Price",    property.formattedPrice)
                            // FIX: property.address direct field — location.address nahi
                            DetailItem("Address",  property.address.ifEmpty { property.city })
                            DetailItem("City",     property.city)
                            DetailItem("Bedrooms", property.bedrooms.toString())
                            DetailItem("Bathrooms",property.bathrooms.toString())
                            DetailItem("Max Guests",property.maxGuests.toString())
                            DetailItem("Status",   property.status.displayName())
                            if (property.adminNote.isNotEmpty()) {
                                DetailItem("Admin Note", property.adminNote)
                            }
                        }
                    }
                }

                // ── Media ──────────────────────────────────────────
                item {
                    Text(
                        text = "Property Media",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // FIX: property.images → property.imageUrls (Property.kt ka sahi field)
                items(property.imageUrls) { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// ── Detail row ─────────────────────────────────────────────────────
@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}

// ── Bottom bar — Reject only (approveProperty Repository mein nahi) ──
@Composable
private fun PropertyBottomBar(
    onReject: () -> Unit,
    isLoading: Boolean
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Reject")
            }
            // NOTE: Approve button tab tak add nahi hoga jab tak
            // AdminRepository mein approveProperty() function add nahi hota
            Button(
                onClick = { },
                modifier = Modifier.weight(1f),
                enabled = false
            ) {
                Text("Approve (Coming Soon)")
            }
        }
    }
}