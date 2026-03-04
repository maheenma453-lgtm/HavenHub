package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.viewmodel.VerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserVerificationDetailScreen(
    userId: String,
    navController: NavController,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = remember(uiState.pendingUsers, userId) {
        uiState.pendingUsers.find { it.userId == userId } // ✅ Fixed: userId from your data class
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
                title = { Text("Verify User") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            user?.let {
                BottomActionBar(
                    onReject = { viewModel.rejectUser(userId) },
                    onApprove = { viewModel.approveUser(userId) },
                    isLoading = uiState.isLoading
                )
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("User Details", fontWeight = FontWeight.Bold)
                        HorizontalDivider()
                        user?.let {
                            DetailRow("Name", it.fullName)
                            DetailRow("Email", it.email)
                            DetailRow("Role", it.role.displayName())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BottomActionBar(onReject: () -> Unit, onApprove: () -> Unit, isLoading: Boolean) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("Ban/Reject") }
        Button(onClick = onApprove, modifier = Modifier.weight(1f), enabled = !isLoading) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Approve")
        }
    }
}