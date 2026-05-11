package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.viewmodel.VerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserVerificationDetailScreen(
    userId       : String,
    navController: NavController,
    viewModel    : VerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val user = remember(uiState.pendingUsers, uiState.selectedUser, userId) {
        uiState.pendingUsers.find { it.userId == userId }
            ?: uiState.selectedUser?.takeIf { it.userId == userId }
    }

    LaunchedEffect(userId) {
        viewModel.loadUserById(userId)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.resetActionState()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }

    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason     by remember { mutableStateOf("") }

    if (showRejectDialog && user != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title            = { Text("Reject User") },
            text             = {
                OutlinedTextField(
                    value         = rejectReason,
                    onValueChange = { rejectReason = it },
                    label         = { Text("Reason for rejection") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectUser(
                            user   = user,
                            reason = rejectReason.ifEmpty { "Does not meet criteria" }
                        )
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Confirm Reject") }
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
                title = { Text("Verify User", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (user != null) {
                Surface(tonalElevation = 2.dp, shadowElevation = 4.dp) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showRejectDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled  = !uiState.isLoading,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Reject", fontWeight = FontWeight.SemiBold) }

                        Button(
                            onClick  = { viewModel.verifyUser(user) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled  = !uiState.isLoading,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color       = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Approve", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            when {
                uiState.isLoading && user == null -> {
                    CircularProgressIndicator(
                        modifier    = Modifier.align(Alignment.Center),
                        color       = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }

                !uiState.isLoading && user == null -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "User not found",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "User ID: $userId",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── User Details Card ────────────────────────────────
                        item {
                            Card(
                                modifier  = Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier            = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "User Details",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 16.sp,
                                        color      = MaterialTheme.colorScheme.onSurface
                                    )
                                    HorizontalDivider(thickness = 0.5.dp)
                                    UserDetailRow("Name",  user!!.fullName)
                                    UserDetailRow("Email", user.email)
                                    UserDetailRow(
                                        "Role",
                                        user.role.toString()
                                            .lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    )
                                    UserDetailRow(
                                        "Status",
                                        if (user.isVerified) "Verified" else "Pending"
                                    )
                                    if (user.cnicNumber.isNotEmpty()) {
                                        UserDetailRow("CNIC", user.cnicNumber)
                                    }
                                }
                            }
                        }

                        // ── CNIC Image Card ──────────────────────────────────
                        if (user!!.cnicImageUrl.isNotEmpty()) {
                            item {
                                Card(
                                    modifier  = Modifier.fillMaxWidth(),
                                    shape     = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(
                                        modifier            = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "CNIC Document",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize   = 16.sp,
                                            color      = MaterialTheme.colorScheme.onSurface
                                        )
                                        HorizontalDivider(thickness = 0.5.dp)
                                        Text(
                                            "Verify the CNIC image below:",
                                            fontSize = 13.sp,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        AsyncImage(
                                            model              = user.cnicImageUrl,
                                            contentDescription = "CNIC Image",
                                            modifier           = Modifier
                                                .fillMaxWidth()
                                                .height(220.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale       = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }

                        // ── Info Banner ──────────────────────────────────────
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (user.isVerified)
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        else
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                    )
                                    .padding(14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (user.isVerified)
                                        "✅ This user is already verified."
                                    else
                                        "⏳ Awaiting admin verification. Review CNIC above.",
                                    fontSize = 13.sp,
                                    color    = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Detail Row ────────────────────────────────────────────────────────────────
@Composable
private fun UserDetailRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            fontWeight = FontWeight.Bold,
            style      = MaterialTheme.typography.bodyMedium,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

