package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    Scaffold(
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
                    onReject = { viewModel.rejectProperty(propertyId, "Does not meet criteria") },
                    onApprove = { viewModel.approveProperty(propertyId) },
                    isLoading = uiState.isLoading
                )
            }
        }
    ) { pad ->
        if (property == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) CircularProgressIndicator() else Text("Property not found")
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
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider()

                            // ✅ Fix: Manual check for fields
                            DetailItem("Title", property.title)
                            DetailItem("Type", property.propertyType.displayName())
                            DetailItem("Price", property.formattedPrice)
                            DetailItem("Location", property.location.address)
                        }
                    }
                }

                // ✅ Fix for "images" error:
                // Agar aapki list ka naam images nahi hai, to property. par Ctrl+Space daba kar sahi naam check karein.
                // Filhal main isay safe bana raha hoon.
                item {
                    Text("Property Media", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                // Agar images ka variable mil jaye to ye chalega
                /* items(property.images) { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                */
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PropertyBottomBar(onReject: () -> Unit, onApprove: () -> Unit, isLoading: Boolean) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), enabled = !isLoading) {
                Text("Reject")
            }
            Button(onClick = onApprove, modifier = Modifier.weight(1f), enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Approve")
            }
        }
    }
}