package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyVerificationDetailScreen(
    propertyId    : String,
    navController : NavController,
    viewModel     : VerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ✅ Property object find karo list se — yahi object functions ko pass hoga
    val property = remember(uiState.pendingProperties, propertyId) {
        uiState.pendingProperties.find { it.propertyId == propertyId }
    }

    val snackbarHostState = remember { SnackbarHostState() }

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

    // ── Reject Dialog ─────────────────────────────────────────────────────────
    if (showRejectDialog && property != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title            = { Text("Reject Property") },
            text             = {
                OutlinedTextField(
                    value         = rejectReason,
                    onValueChange = { rejectReason = it },
                    label         = { Text("Reason for rejection") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2
                )
            },
            confirmButton = {
                Button(onClick = {
                    // ✅ FIX: String ki jagah Property object pass karo
                    viewModel.rejectProperty(
                        property  = property,
                        adminNote = rejectReason.ifEmpty { "Does not meet criteria" }
                    )
                    showRejectDialog = false
                }) { Text("Confirm Reject") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Approve Dialog ────────────────────────────────────────────────────────
    if (showApproveDialog && property != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title            = { Text("Approve Property") },
            text             = {
                Column {
                    Text("Kya aap yeh property approve karna chahte hain?")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = adminNote,
                        onValueChange = { adminNote = it },
                        label         = { Text("Admin note (optional)") },
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // ✅ FIX: String ki jagah Property object pass karo
                    viewModel.approveProperty(
                        property  = property,
                        adminNote = adminNote
                    )
                    showApproveDialog = false
                }) { Text("Approve") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showApproveDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title          = { Text("Property Review") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (property != null) {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showRejectDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled  = !uiState.isLoading
                        ) { Text("Reject") }

                        Button(
                            onClick  = { showApproveDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled  = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Approve")
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        if (property == null) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) CircularProgressIndicator()
                else Text("Property not found")
            }
        } else {
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                // ── Basic Info ────────────────────────────────────────────────
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Home,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Basic Information",
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider()
                            DetailItem("Title",      property.title)
                            DetailItem("Type",       property.propertyType.lowercase().replaceFirstChar { it.uppercase() })
                            DetailItem("Price",      property.formattedPrice)
                            DetailItem("Address",    property.address.ifEmpty { property.city })
                            DetailItem("City",       property.city)
                            DetailItem("Bedrooms",   property.bedrooms.toString())
                            DetailItem("Bathrooms",  property.bathrooms.toString())
                            DetailItem("Max Guests", property.maxGuests.toString())
                            DetailItem("Status",     property.status.lowercase().replaceFirstChar { it.uppercase() })
                            if (property.adminNote.isNotEmpty()) {
                                DetailItem("Admin Note", property.adminNote)
                            }
                        }
                    }
                }

                // ── Owner Info ────────────────────────────────────────────────
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Owner Information",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider()
                            DetailItem("Owner Name", property.ownerName.ifEmpty { "N/A" })
                            DetailItem("Owner ID",   property.ownerId)
                        }
                    }
                }

                // ── Description ───────────────────────────────────────────────
                if (property.description.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Description",
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    property.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // ── Amenities ─────────────────────────────────────────────────
                if (property.amenities.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Amenities",
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(property.amenities.joinToString(", "))
                            }
                        }
                    }
                }

                // ── Property Photos ───────────────────────────────────────────
                item {
                    Text(
                        "Property Photos",
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium
                    )
                }

                if (property.imageUrls.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No photos uploaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(property.imageUrls) { imageUrl ->
                        Card {
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

                // ── PT-1 Document ─────────────────────────────────────────────
                item {
                    Text(
                        "PT-1 Verification Document",
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    null,
                                    tint = if (property.hasPt1Document)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (property.hasPt1Document)
                                        "PT-1 Document Uploaded"
                                    else
                                        "PT-1 Document Not Uploaded",
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (property.hasPt1Document)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }

                            if (property.hasPt1Document) {
                                HorizontalDivider()
                                Text(
                                    "Document Preview",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    AsyncImage(
                                        model              = property.pt1DocumentUrl,
                                        contentDescription = "PT-1 Document",
                                        modifier           = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 200.dp, max = 400.dp),
                                        contentScale       = ContentScale.FillWidth
                                    )
                                }
                                Text(
                                    "* Agar document PDF hai to image preview nahi aayegi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Detail Row ────────────────────────────────────────────────────────────────
@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            fontWeight = FontWeight.SemiBold,
            style      = MaterialTheme.typography.bodySmall
        )
    }
}
