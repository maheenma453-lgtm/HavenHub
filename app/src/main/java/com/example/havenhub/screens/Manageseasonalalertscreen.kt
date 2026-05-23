package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.SeasonalAlert
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SeasonalAlertViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSeasonalAlertsScreen(
    navController: NavController,
    viewModel    : SeasonalAlertViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

    // ── Theme-aware colors ────────────────────────────────────────────────────
    val pageBg     = if (isDark) DarkBg          else Color(0xFFF4F6FA)
    val headerGrad = if (isDark)
        Brush.horizontalGradient(listOf(DarkBg, DarkBgSecondary))
    else
        Brush.horizontalGradient(listOf(PrimaryNavyDark, PrimaryNavy))
    val goldC      = if (isDark) DarkGoldPrimary  else GoldAccent
    val goldL      = if (isDark) DarkGoldLight     else GoldAccentLight
    val surfaceC   = if (isDark) DarkSurface       else Color.White
    val textPri    = if (isDark) DarkTextPrimary   else PrimaryNavyDark
    val textSec    = if (isDark) DarkTextSecondary else Color(0xFF6B7280)
    val borderC    = if (isDark) DarkBorder        else Color(0xFFE5E7EB)

    var showDeleteDialog   by remember { mutableStateOf(false) }
    var selectedAlertId    by remember { mutableStateOf<String?>(null) }
    var selectedAlertTitle by remember { mutableStateOf("") }
    val snackbarHostState  = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadAllAlertsForAdmin() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        uiState.errorMessage?.let   { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    // ── Delete Dialog ─────────────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = surfaceC,
            icon = {
                Box(
                    modifier         = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.error,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete Alert?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 18.sp,
                    color      = textPri
                )
            },
            text = {
                Text(
                    "\"$selectedAlertTitle\" will be permanently deleted.",
                    fontSize   = 14.sp,
                    lineHeight = 21.sp,
                    color      = textSec
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedAlertId?.let { viewModel.deleteAlert(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape   = RoundedCornerShape(10.dp),
                    border  = androidx.compose.foundation.BorderStroke(1.dp, borderC)
                ) { Text("Cancel", color = textPri) }
            }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = pageBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGrad)
                    .statusBarsPadding()
            ) {
                // Gold shimmer line at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, goldC, goldL, goldC, Color.Transparent)
                            )
                        )
                )
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = Color.White
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Seasonal Alerts",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 20.sp,
                            color      = Color.White
                        )
                        Text(
                            "Manage promotional alerts",
                            fontSize = 11.sp,
                            color    = Color.White.copy(alpha = 0.65f)
                        )
                    }
                    if (uiState.allAlerts.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(goldC.copy(alpha = 0.22f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${uiState.allAlerts.size} alerts",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = goldL
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { navController.navigate(Screen.CreateSeasonalAlert.route) },
                containerColor = if (isDark) DarkBgElevated else PrimaryNavyDark,
                shape          = RoundedCornerShape(16.dp),
                modifier       = Modifier.shadow(12.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = goldC)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "New Alert",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = goldL
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color       = goldC,
                            strokeWidth = 3.dp,
                            modifier    = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Loading alerts...", fontSize = 13.sp, color = textSec)
                    }
                }

                uiState.allAlerts.isEmpty() -> {
                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(goldC.copy(alpha = 0.18f), goldC.copy(alpha = 0.05f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Celebration,
                                contentDescription = null,
                                modifier           = Modifier.size(52.dp),
                                tint               = goldC.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "No Seasonal Alerts",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = textPri
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Create your first alert to notify\nlandlords and tenants",
                            fontSize   = 14.sp,
                            color      = textSec,
                            textAlign  = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(
                            start  = 16.dp,
                            end    = 16.dp,
                            top    = 16.dp,
                            bottom = 110.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val activeAlerts   = uiState.allAlerts.filter { it.isActive }
                        val inactiveAlerts = uiState.allAlerts.filter { !it.isActive }

                        if (activeAlerts.isNotEmpty()) {
                            item {
                                AlertSectionLabel(
                                    label  = "Active",
                                    count  = activeAlerts.size,
                                    color  = Color(0xFF22C55E),
                                    isDark = isDark
                                )
                            }
                            items(activeAlerts, key = { it.alertId }) { alert ->
                                SeasonalAlertAdminCard(
                                    alert    = alert,
                                    isDark   = isDark,
                                    goldC    = goldC,
                                    goldL    = goldL,
                                    textPri  = textPri,
                                    textSec  = textSec,
                                    surfaceC = surfaceC,
                                    onToggle = { viewModel.toggleAlertActive(alert.alertId, !alert.isActive) },
                                    onDelete = {
                                        selectedAlertId    = alert.alertId
                                        selectedAlertTitle = alert.title
                                        showDeleteDialog   = true
                                    }
                                )
                            }
                        }

                        if (inactiveAlerts.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                AlertSectionLabel(
                                    label  = "Inactive",
                                    count  = inactiveAlerts.size,
                                    color  = Color(0xFF9E9E9E),
                                    isDark = isDark
                                )
                            }
                            items(inactiveAlerts, key = { it.alertId }) { alert ->
                                SeasonalAlertAdminCard(
                                    alert    = alert,
                                    isDark   = isDark,
                                    goldC    = goldC,
                                    goldL    = goldL,
                                    textPri  = textPri,
                                    textSec  = textSec,
                                    surfaceC = surfaceC,
                                    onToggle = { viewModel.toggleAlertActive(alert.alertId, !alert.isActive) },
                                    onDelete = {
                                        selectedAlertId    = alert.alertId
                                        selectedAlertTitle = alert.title
                                        showDeleteDialog   = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AlertSectionLabel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlertSectionLabel(label: String, count: Int, color: Color, isDark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (isDark) DarkTextSecondary else Color(0xFF6B7280)
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.14f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text       = "$count",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SeasonalAlertAdminCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SeasonalAlertAdminCard(
    alert   : SeasonalAlert,
    isDark  : Boolean,
    goldC   : Color,
    goldL   : Color,
    textPri : Color,
    textSec : Color,
    surfaceC: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive    = alert.isActive
    val accentColor = if (isActive) goldC else Color(0xFF9E9E9E)
    val cardSurface = if (isActive) surfaceC
    else if (isDark) DarkBgSecondary else Color(0xFFF5F5F5)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isActive) 6.dp else 2.dp,
                shape     = RoundedCornerShape(16.dp),
                ambientColor = accentColor.copy(alpha = 0.15f),
                spotColor    = accentColor.copy(alpha = 0.10f)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.09f), accentColor.copy(alpha = 0.02f))
                    )
                )
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(accentColor)
            )

            Column(
                modifier = Modifier.padding(
                    start  = 16.dp,
                    end    = 12.dp,
                    top    = 14.dp,
                    bottom = 14.dp
                )
            ) {
                // ── Header Row ──────────────────────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = alert.iconEmoji.ifEmpty { "🎉" },
                            fontSize = 22.sp
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text       = alert.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 14.sp,
                            color      = if (isActive) textPri else Color(0xFF9E9E9E)
                        )
                        if (alert.season.isNotEmpty()) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text       = alert.season,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = accentColor
                                )
                            }
                        }
                    }

                    Switch(
                        checked         = isActive,
                        onCheckedChange = { onToggle() },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor   = Color.White,
                            checkedTrackColor   = Color(0xFF22C55E),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = if (isDark) DarkBorder else Color(0xFFD1D5DB)
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text       = alert.message,
                    fontSize   = 12.sp,
                    lineHeight = 18.sp,
                    color      = if (isActive) textSec else Color(0xFF9E9E9E),
                    maxLines   = 2
                )

                Spacer(Modifier.height(10.dp))

                // ── Footer Row ──────────────────────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (roleLabel, roleColor) = when (alert.targetRole.lowercase()) {
                        "landlord" -> "Landlords" to Color(0xFF6366F1)
                        "tenant"   -> "Tenants"   to Color(0xFF0EA5E9)
                        else       -> "All Users" to Color(0xFF22C55E)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(roleColor.copy(alpha = 0.10f))
                            .border(1.dp, roleColor.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PeopleAlt,
                                contentDescription = null,
                                tint               = roleColor,
                                modifier           = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text       = roleLabel,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = roleColor
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    TextButton(
                        onClick        = onDelete,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = "Delete",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}