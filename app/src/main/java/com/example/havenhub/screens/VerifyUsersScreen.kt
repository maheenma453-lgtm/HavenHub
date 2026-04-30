package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.User
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.PrimaryBlue
import com.example.havenhub.ui.theme.SuccessGreen
import com.example.havenhub.ui.theme.WarningOrange
import com.example.havenhub.viewmodel.VerificationViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyUsersScreen(
    navController: NavController,
    viewModel    : VerificationViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ FIX: errorMessage aaye to snackbar dikhao, phir reset karo
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }

    // ✅ FIX: actionSuccess true ho to successMessage ke saath snackbar dikhao
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            val msg = uiState.successMessage ?: "Action completed successfully"
            snackbarHostState.showSnackbar(msg)
            viewModel.resetActionState()
        }
    }

    // ✅ Screen open hone pe pending users load karo
    LaunchedEffect(Unit) {
        viewModel.loadPendingUsers()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Verify Users",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        containerColor = Color(0xFFF4F6FB)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = PrimaryBlue
                    )
                }

                uiState.pendingUsers.isEmpty() -> {
                    // ── Empty State ─────────────────────────────
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SuccessGreen.copy(.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier           = Modifier.size(40.dp),
                                tint               = SuccessGreen
                            )
                        }
                        Text(
                            "All caught up!",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF1A1A2E)
                        )
                        Text(
                            "No users pending verification",
                            fontSize = 13.sp,
                            color    = Color(0xFF888888)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // ── Pending Banner ──────────────────
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarningOrange.copy(.12f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "${uiState.pendingUsers.size} Users Pending Verification",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 13.sp,
                                    color      = WarningOrange
                                )
                            }
                        }

                        items(uiState.pendingUsers, key = { it.userId }) { user ->
                            AdminPendingUserCard(
                                user    = user,
                                onClick = {
                                    navController.navigate(
                                        Screen.UserVerificationDetail.createRoute(user.userId)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── User Card ─────────────────────────────────────────────────────────────────
@Composable
private fun AdminPendingUserCard(
    user   : User,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val submittedDate = user.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "—"

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(
                modifier         = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue.copy(.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint               = PrimaryBlue,
                    modifier           = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = user.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = Color(0xFF1A1A2E)
                )
                Text(
                    text     = user.email,
                    fontSize = 12.sp,
                    color    = Color(0xFF888888)
                )

                // ✅ FIX: role.toString() use karo — enum ka name lata hai
                val roleDisplay = user.role
                    .toString()
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }

                Text(
                    text     = "Role: $roleDisplay",
                    fontSize = 12.sp,
                    color    = Color(0xFF666666)
                )
                Text(
                    text     = "Joined: $submittedDate",
                    fontSize = 11.sp,
                    color    = Color(0xFFAAAAAA)
                )
            }

            // Review button
            Button(
                onClick         = onClick,
                contentPadding  = PaddingValues(horizontal = 14.dp),
                modifier        = Modifier.height(34.dp),
                colors          = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Review", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}