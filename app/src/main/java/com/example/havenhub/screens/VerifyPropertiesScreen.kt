package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.viewmodel.VerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyPropertiesScreen(
    navController: NavController,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.propertiesUiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Properties") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.properties.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("All properties verified!", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${uiState.properties.size} pending",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(uiState.properties, key = { it.propertyId }) { property ->
                        PendingPropertyCard(
                            title = property.title,
                            ownerName = property.ownerName,
                            location = property.location,
                            submittedDate = property.submittedDate,
                            onClick = {
                                navController.navigate(
                                    Screen.PropertyVerificationDetail.createRoute(property.propertyId)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingPropertyCard(
    title: String,
    ownerName: String,
    location: String,
    submittedDate: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Owner: $ownerName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Submitted: $submittedDate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            FilterChip(selected = false, onClick = onClick, label = { Text("Review") })
        }
    }
}

