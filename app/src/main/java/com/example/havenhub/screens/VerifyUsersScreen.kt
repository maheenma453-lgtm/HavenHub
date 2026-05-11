package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.User
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.SuccessGreen
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

    LaunchedEffect(Unit) { viewModel.loadPendingUsers() }

    val primary   = MaterialTheme.colorScheme.primary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(primary, primaryContainer))
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = tertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text          = "Verify Users",
                            color         = MaterialTheme.colorScheme.onPrimary,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        if (!uiState.isLoading && uiState.pendingUsers.isNotEmpty()) {
                            Text(
                                text     = "${uiState.pendingUsers.size} pending review",
                                color    = tertiary.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                // Gold shimmer line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0f),
                                    tertiary,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0f)
                                )
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // ── Loading ───────────────────────────────────────────────────
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier    = Modifier.align(Alignment.Center),
                        color       = tertiary,
                        strokeWidth = 3.dp
                    )
                }

                // ── Empty State ───────────────────────────────────────────────
                uiState.pendingUsers.isEmpty() -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(SuccessGreen.copy(0.18f), SuccessGreen.copy(0.04f))
                                    )
                                )
                                .border(1.5.dp, SuccessGreen.copy(0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier           = Modifier.size(42.dp),
                                tint               = SuccessGreen
                            )
                        }
                        Text(
                            "All caught up!",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "No users pending verification",
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                        )
                    }
                }

                // ── List ──────────────────────────────────────────────────────
                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Pending Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(primary, primaryContainer))
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(tertiary, tertiary.copy(alpha = 0.6f))
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(tertiary)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "${uiState.pendingUsers.size} User${if (uiState.pendingUsers.size != 1) "s" else ""} Pending Verification",
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 13.sp,
                                        color      = tertiary
                                    )
                                }
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

    val roleDisplay = user.role
        .toString()
        .lowercase()
        .replaceFirstChar { it.uppercase() }

    val primary  = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 4.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = primary.copy(alpha = 0.08f),
                spotColor    = primary.copy(alpha = 0.12f)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // Top accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(primary, tertiary)))
        )

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint               = tertiary,
                    modifier           = Modifier.size(26.dp)
                )
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = user.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = user.email,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Role Badge
                    Surface(
                        color = primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text       = roleDisplay,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = primary,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Date Badge
                    Surface(
                        color = tertiary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text     = submittedDate,
                            fontSize = 11.sp,
                            color    = tertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Review Button
            Button(
                onClick        = onClick,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier       = Modifier.height(38.dp),
                shape          = RoundedCornerShape(10.dp),
                colors         = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor   = tertiary
                )
            ) {
                Text(
                    "Review",
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
