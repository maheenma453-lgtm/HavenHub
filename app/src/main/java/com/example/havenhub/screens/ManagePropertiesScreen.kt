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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
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

    // Confirmation dialog state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDialogTitle by remember { mutableStateOf("") }
    var confirmDialogMessage by remember { mutableStateOf("") }

    val filteredProperties = remember(uiState.properties, searchQuery, selectedStatus) {
        uiState.properties.filter { property ->
            val matchesSearch = property.title.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (selectedStatus) {
                "All"      -> true
                "Approved" -> property.propertyStatusEnum == PropertyStatus.APPROVED
                "Pending"  -> property.propertyStatusEnum == PropertyStatus.PENDING
                "Rejected" -> property.propertyStatusEnum == PropertyStatus.REJECTED
                else       -> true
            }
            matchesSearch && matchesStatus
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(confirmDialogTitle) },
            text  = { Text(confirmDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction?.invoke()
                        showConfirmDialog = false
                        pendingAction = null
                    }
                ) {
                    Text("Confirm", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

            // Search Bar
            OutlinedTextField(
                value       = searchQuery,
                onValueChange = { searchQuery = it },
                modifier    = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search properties...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine  = true
            )

            // Properties List
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredProperties) { property ->
                    PropertyManagementCard(
                        property = property,
                        onApprove = {
                            confirmDialogTitle   = "Approve Property"
                            confirmDialogMessage = "Are you sure you want to approve \"${property.title}\"?"
                            pendingAction        = { viewModel.approveProperty(property.propertyId) }
                            showConfirmDialog     = true
                        },
                        onReject = {
                            confirmDialogTitle   = "Reject Property"
                            confirmDialogMessage = "Are you sure you want to reject \"${property.title}\"?"
                            pendingAction        = { viewModel.removeProperty(property.propertyId) }
                            showConfirmDialog     = true
                        },
                        onDelete = {
                            confirmDialogTitle   = "Delete Property"
                            confirmDialogMessage = "Are you sure you want to permanently delete \"${property.title}\"? This cannot be undone."
                            pendingAction        = { viewModel.deleteProperty(property.propertyId) }
                            showConfirmDialog     = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyManagementCard(
    property : Property,
    onApprove: () -> Unit,
    onReject : () -> Unit,
    onDelete : () -> Unit
) {
    // Dropdown menu open/close state — har card ka apna alag state
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text  = property.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text  = property.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Status badge with color
                val statusColor = when (property.propertyStatusEnum) {
                    PropertyStatus.APPROVED     -> Color(0xFF2ECC71)
                    PropertyStatus.REJECTED     -> Color(0xFFE74C3C)
                    PropertyStatus.PENDING      -> Color(0xFFE67E22)
                    PropertyStatus.UNDER_REVIEW -> Color(0xFF3498DB)
                    PropertyStatus.INACTIVE     -> Color.Gray
                }
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text     = property.propertyStatusEnum.displayName(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = statusColor
                    )
                }
            }

            // 3 dots — dropdown menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded        = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text    = { Text("Approve", color = Color(0xFF2ECC71)) },
                        onClick = {
                            menuExpanded = false
                            onApprove()
                        }
                    )
                    DropdownMenuItem(
                        text    = { Text("Reject", color = Color(0xFFE67E22)) },
                        onClick = {
                            menuExpanded = false
                            onReject()
                        }
                    )
                    DropdownMenuItem(
                        text    = { Text("Delete", color = Color(0xFFE74C3C)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}