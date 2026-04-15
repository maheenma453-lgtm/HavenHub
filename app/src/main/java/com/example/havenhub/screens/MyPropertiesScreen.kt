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
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.PropertyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPropertiesScreen(
    navController: NavController,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedPropertyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMyProperties()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Properties", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        // ✅ Updated to AutoMirrored to fix deprecation warning
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B3E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddProperty.route) },
                containerColor = Color(0xFF0D1B3E)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F7FA))) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF0D1B3E)
                    )
                }
                uiState.myProperties.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No properties yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1B3E)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.myProperties,
                            key = { it.propertyId } // ✅ Key added for better performance
                        ) { property ->
                            MyPropertyCard(
                                property = property,
                                onClick = {
                                    navController.navigate(
                                        Screen.PropertyDetail.createRoute(property.propertyId)
                                    )
                                },
                                onEdit = {
                                    navController.navigate(
                                        Screen.EditProperty.createRoute(property.propertyId)
                                    )
                                },
                                onDelete = {
                                    selectedPropertyId = property.propertyId
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Property") },
            text = { Text("Are you sure you want to delete this property?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedPropertyId?.let { viewModel.deleteProperty(it) }
                    showDeleteDialog = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MyPropertyCard(
    property: Property,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                Image(
                    painter = painterResource(id = getPropertyImage(property.propertyId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // ✅ Status Badge (Fixed Ambiguity)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(getLocalStatusColor(property.status).copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = getLocalStatusLabel(property.status),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = property.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ✅ FIXED: Renamed functions to avoid conflict with other files
private fun getLocalStatusColor(status: String): Color = when (status.uppercase()) {
    "APPROVED" -> Color(0xFF4CAF50)
    "PENDING"  -> Color(0xFFFF9800)
    "REJECTED" -> Color(0xFFF44336)
    else       -> Color(0xFF9E9E9E)
}

private fun getLocalStatusLabel(status: String): String = when (status.uppercase()) {
    "APPROVED" -> "✓ Approved"
    "PENDING"  -> "⏳ Pending"
    "REJECTED" -> "✗ Rejected"
    else       -> status
}