package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun UserVerificationDetailScreen(
    userId: String,
    navController: NavController,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val detail by viewModel.userDetail.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    LaunchedEffect(userId) { viewModel.loadUserDetail(userId) }
    LaunchedEffect(actionState) { if (actionState.isSuccess) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Review") },
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
                            onClick = { viewModel.rejectUser(userId) },
                            modifier = Modifier.weight(1f),
                            enabled = !actionState.isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Reject") }
                        Button(
                            onClick = { viewModel.approveUser(userId) },
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
                val user = detail!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("User Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                HorizontalDivider()
                                InfoRow("Full Name", user.fullName)
                                InfoRow("Email", user.email)
                                InfoRow("Phone", user.phone)
                                InfoRow("Role", user.role)
                                InfoRow("National ID", user.nationalId)
                                InfoRow("Submitted", user.submittedDate)
                            }
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ID Document", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                HorizontalDivider()
                                Text(
                                    "Front and back of government-issued ID",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(onClick = { /* view front */ }, label = { Text("View Front") }, modifier = Modifier.weight(1f))
                                    AssistChip(onClick = { /* view back */ }, label = { Text("View Back") }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (actionState.error != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = actionState.error ?: "",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}


