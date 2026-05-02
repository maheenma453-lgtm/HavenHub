package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.havenhub.ui.theme.GoldAccent
import com.example.havenhub.ui.theme.GoldAccentDark
import com.example.havenhub.ui.theme.PrimaryNavy
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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            val msg = uiState.successMessage ?: "Action completed successfully"
            snackbarHostState.showSnackbar(msg)
            viewModel.resetActionState()
        }
    }

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
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy)
            )
        },
        containerColor = Color(0xFFF0F2F5)
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
                        color    = GoldAccent
                    )
                }

                uiState.pendingUsers.isEmpty() -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
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
                            // Pending Banner with gold outline
                            Card(
                                modifier  = Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(12.dp),
                                colors    = CardDefaults.cardColors(containerColor = GoldAccent.copy(0.08f)),
                                border    = BorderStroke(1.5.dp, GoldAccent)
                            ) {
                                Text(
                                    "${uiState.pendingUsers.size} Users Pending Verification",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 13.sp,
                                    color      = GoldAccentDark,
                                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.5.dp, GoldAccent)
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryNavy.copy(.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint               = PrimaryNavy,
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

            // Review button — gold outlined
            OutlinedButton(
                onClick        = onClick,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier       = Modifier.height(36.dp),
                shape          = RoundedCornerShape(10.dp),
                border         = BorderStroke(1.5.dp, GoldAccent),
                colors         = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccentDark)
            ) {
                Text("Review", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
