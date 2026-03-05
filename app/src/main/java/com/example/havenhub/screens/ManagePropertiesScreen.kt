package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.viewmodel.ManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePropertiesScreen(
    navController: NavController,
    viewModel: ManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("All") }

    val filteredProperties = remember(uiState.properties, searchQuery, selectedStatus) {
        uiState.properties.filter { property ->
            val matchesSearch = property.title.contains(searchQuery, ignoreCase = true)
            // Fixed: Use 'status' instead of 'isApproved' or 'verificationStatus'
            val matchesStatus = when (selectedStatus) {
                "All" -> true
                "Approved" -> property.status == PropertyStatus.APPROVED
                "Pending" -> property.status == PropertyStatus.PENDING
                "Rejected" -> property.status == PropertyStatus.REJECTED
                else -> true
            }
            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Properties") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search properties...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            // LazyColumn... items use property.status.displayName() and property.address
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredProperties) { property ->
                    PropertyManagementCard(
                        title = property.title,
                        address = property.address,
                        status = property.status.displayName(),
                        onRemove = { viewModel.removeProperty(property.propertyId) }
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyManagementCard(title: String, address: String, status: String, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Home, null)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(address, style = MaterialTheme.typography.bodySmall)
                Text("Status: $status", color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.MoreVert, null) }
        }
    }
}