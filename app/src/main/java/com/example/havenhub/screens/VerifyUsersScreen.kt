package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.havenhub.viewmodel.VerificationViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// ── Brand Colors ─────────────────────────────────────────────────────────────
private val NavyBlue    = Color(0xFF1B2A4A)
private val NavyLight   = Color(0xFF243658)
private val Gold        = Color(0xFFC9A227)
private val GoldDark    = Color(0xFFA07D10)
private val PageBg      = Color(0xFFF4F6FA)

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

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = PageBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NavyBlue, NavyLight)))
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
                            tint               = Gold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text          = "Verify Users",
                            color         = Color.White,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        if (!uiState.isLoading && uiState.pendingUsers.isNotEmpty()) {
                            Text(
                                text     = "${uiState.pendingUsers.size} pending review",
                                color    = Gold.copy(alpha = 0.85f),
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
                                listOf(Color.Transparent, Gold, Color.Transparent)
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
                        modifier  = Modifier.align(Alignment.Center),
                        color     = Gold,
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
                            color      = NavyBlue
                        )
                        Text(
                            "No users pending verification",
                            fontSize = 13.sp,
                            color    = NavyBlue.copy(alpha = 0.45f)
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
                                        Brush.horizontalGradient(
                                            listOf(NavyBlue, NavyLight)
                                        )
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.horizontalGradient(listOf(Gold, GoldDark)),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Gold)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "${uiState.pendingUsers.size} User${if (uiState.pendingUsers.size != 1) "s" else ""} Pending Verification",
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 13.sp,
                                        color      = Gold
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

    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 4.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = NavyBlue.copy(alpha = 0.08f),
                spotColor    = NavyBlue.copy(alpha = 0.12f)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // Top accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(NavyBlue, Gold)))
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
                    .background(NavyBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint               = Gold,
                    modifier           = Modifier.size(26.dp)
                )
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = user.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = NavyBlue
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = user.email,
                    fontSize = 12.sp,
                    color    = NavyBlue.copy(alpha = 0.5f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Role Badge
                    Surface(
                        color = NavyBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text     = roleDisplay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color    = NavyBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Date Badge
                    Surface(
                        color = Gold.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text     = submittedDate,
                            fontSize = 11.sp,
                            color    = GoldDark,
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
                    containerColor = NavyBlue,
                    contentColor   = Gold
                )
            ) {
                Text(
                    "Review",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}





