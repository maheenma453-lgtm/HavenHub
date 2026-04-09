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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryBlue)
            } else if (uiState.myProperties.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No properties found.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.myProperties) { property ->
                        MyPropertyCard(
                            property = property,
                            onClick = { navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId)) },
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedPropertyId?.let { viewModel.deleteProperty(it) }
                    showDeleteDialog = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun MyPropertyCard(property: Property, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            AsyncImage(
                model = property.coverImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentScale = ContentScale.Crop
            )
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(property.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                }
            }
        }
    }
}