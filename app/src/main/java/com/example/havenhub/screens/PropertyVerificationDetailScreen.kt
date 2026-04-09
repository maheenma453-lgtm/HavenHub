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
import com.example.havenhub.data.PropertyStatus // Ensure this import

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

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.resetActionState()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }

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
                    onReject  = { showRejectDialog = true },
                    isLoading = uiState.isLoading
                )
            }
        }
    ) { pad ->
        if (property == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) CircularProgressIndicator()
                else Text("Property not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider()

                            DetailItem("Title", property.title)

                            // ✅ FIX: displayName() hata kar direct string logic use kiya
                            DetailItem("Type", property.propertyType.lowercase().replaceFirstChar { it.uppercase() })

                            DetailItem("Price", property.formattedPrice)
                            DetailItem("Address", property.address.ifEmpty { property.city })
                            DetailItem("City", property.city)
                            DetailItem("Bedrooms", property.bedrooms.toString())
                            DetailItem("Bathrooms", property.bathrooms.toString())
                            DetailItem("Max Guests", property.maxGuests.toString())

                            // ✅ FIX: Status display logic without displayName()
                            DetailItem("Status", property.status.lowercase().replaceFirstChar { it.uppercase() })

                            if (property.adminNote.isNotEmpty()) {
                                DetailItem("Admin Note", property.adminNote)
                            }
                        }
                    }
                }

                item {
                    Text("Property Media", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                items(property.imageUrls) { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PropertyBottomBar(onReject: () -> Unit, isLoading: Boolean) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), enabled = !isLoading) {
                Text("Reject")
            }
            Button(onClick = { /* Approve placeholder */ }, modifier = Modifier.weight(1f), enabled = false) {
                Text("Approve (Pending)")
            }
        }
    }
}