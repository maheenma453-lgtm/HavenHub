package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.viewmodel.VerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyVerificationDetailScreen(
    propertyId: String,
    navController: NavController,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val detail by viewModel.propertyDetail.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    LaunchedEffect(propertyId) {
        viewModel.loadPropertyDetail(propertyId)
    }

    // Navigate back after successful action
    LaunchedEffect(actionState) {
        if (actionState.isSuccess) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Property Review") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (detail != null) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.rejectProperty(propertyId) },
                            modifier = Modifier.weight(1f),
                            enabled = !actionState.isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reject")
                        }
                        Button(
                            onClick = { viewModel.approveProperty(propertyId) },
                            modifier = Modifier.weight(1f),
                            enabled = !actionState.isLoading
                        ) {
                            if (actionState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Approve")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            detail == null -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val prop = detail!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Property Info
                    item {
                        SectionCard(title = "Property Information") {
                            LabelValue("Title", prop.title)
                            LabelValue("Type", prop.propertyType)
                            LabelValue("Location", prop.location)
                            LabelValue("Price/night", prop.pricePerNight)
                            LabelValue("Bedrooms", prop.bedrooms.toString())
                            LabelValue("Owner", prop.ownerName)
                            LabelValue("Owner Email", prop.ownerEmail)
                            LabelValue("Submitted", prop.submittedDate)
                        }
                    }

                    // Description
                    item {
                        SectionCard(title = "Description") {
                            Text(prop.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Documents
                    item {
                        Text("Submitted Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }

                    items(prop.documents) { doc ->
                        DocumentItem(name = doc.name, type = doc.type)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DocumentItem(name: String, type: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = { /* view doc */ }, label = { Text("View") })
        }
    }
}


