package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.SeasonalAlert
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SeasonalAlertViewModel

private val QuickSeasons = listOf(
    "Eid", "Summer", "Winter", "Holidays", "New Year", "Independence Day", "Ramadan"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSeasonalAlertScreen(
    navController: NavController,
    viewModel    : SeasonalAlertViewModel = hiltViewModel()
) {
    val isDark  = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

    // ── Theme-aware colors ────────────────────────────────────────────────────
    val pageBg      = if (isDark) DarkBg          else Color(0xFFF4F6FA)
    val headerGrad  = if (isDark)
        Brush.horizontalGradient(listOf(DarkBg, DarkBgSecondary))
    else
        Brush.horizontalGradient(listOf(PrimaryNavyDark, PrimaryNavy))
    val goldC       = if (isDark) DarkGoldPrimary  else GoldAccent
    val goldL       = if (isDark) DarkGoldLight     else GoldAccentLight
    val textPri     = if (isDark) DarkTextPrimary   else PrimaryNavyDark
    val textSec     = if (isDark) DarkTextSecondary else Color(0xFF6B7280)
    val inputBg     = if (isDark) DarkBgSecondary   else Color.White
    val inputBorder = if (isDark) DarkBorder        else Color(0xFFE5E7EB)
    val chipBg      = if (isDark) DarkBgTertiary    else Color(0xFFF0F2F5)
    val chipText    = if (isDark) DarkTextSecondary else Color(0xFF374151)

    var title      by remember { mutableStateOf("") }
    var message    by remember { mutableStateOf("") }
    var season     by remember { mutableStateOf("") }
    var iconEmoji  by remember { mutableStateOf("🎉") }
    var targetRole by remember { mutableStateOf("all") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            navController.popBackStack()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
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
                    Column {
                        Text(
                            "Create Alert",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 20.sp,
                            color      = Color.White
                        )
                        Text(
                            "New seasonal notification",
                            fontSize = 11.sp,
                            color    = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Alert Title ───────────────────────────────────────────────────
            AlertFormSection(goldC = goldC, textPri = textPri, label = "Alert Title *") {
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    placeholder   = {
                        Text("e.g., Eid ul Adha Special!", color = textSec, fontSize = 14.sp)
                    },
                    modifier   = Modifier.fillMaxWidth(),
                    shape      = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors     = alertTextFieldColors(inputBg, inputBorder, goldC, textPri)
                )
            }

            // ── Message ───────────────────────────────────────────────────────
            AlertFormSection(goldC = goldC, textPri = textPri, label = "Message *") {
                OutlinedTextField(
                    value         = message,
                    onValueChange = { message = it },
                    placeholder   = {
                        Text("Write the full alert message here...", color = textSec, fontSize = 14.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape    = RoundedCornerShape(14.dp),
                    maxLines = 6,
                    colors   = alertTextFieldColors(inputBg, inputBorder, goldC, textPri)
                )
            }

            // ── Season / Occasion ─────────────────────────────────────────────
            AlertFormSection(goldC = goldC, textPri = textPri, label = "Season / Occasion") {
                OutlinedTextField(
                    value         = season,
                    onValueChange = { season = it },
                    placeholder   = {
                        Text("e.g., Eid, Summer, Winter...", color = textSec, fontSize = 14.sp)
                    },
                    modifier   = Modifier.fillMaxWidth(),
                    shape      = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors     = alertTextFieldColors(inputBg, inputBorder, goldC, textPri)
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QuickSeasons) { chip ->
                        val selected = season == chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) goldC.copy(alpha = 0.18f) else chipBg)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) goldC else inputBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { season = chip }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                chip,
                                fontSize   = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (selected) goldC else chipText
                            )
                        }
                    }
                }
            }

            // ── Icon Emoji ────────────────────────────────────────────────────
            AlertFormSection(goldC = goldC, textPri = textPri, label = "Icon Emoji") {
                OutlinedTextField(
                    value         = iconEmoji,
                    onValueChange = { iconEmoji = it },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(14.dp),
                    singleLine    = true,
                    colors        = alertTextFieldColors(inputBg, inputBorder, goldC, textPri)
                )
            }

            // ── Target Audience ───────────────────────────────────────────────
            AlertFormSection(goldC = goldC, textPri = textPri, label = "Target Audience") {
                val roles = listOf(
                    Triple("all",      "All Users", Color(0xFF22C55E)),
                    Triple("tenant",   "Tenants",   Color(0xFF0EA5E9)),
                    Triple("landlord", "Landlords", Color(0xFF6366F1))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    roles.forEach { (value, label, roleColor) ->
                        val selected = targetRole == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) roleColor.copy(alpha = 0.15f) else chipBg
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) roleColor else inputBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { targetRole = value }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize   = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (selected) roleColor else chipText
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Create Button ─────────────────────────────────────────────────
            Button(
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        // ✅ FIX: SeasonalAlert object banao aur pass karo
                        val alert = SeasonalAlert(
                            title      = title.trim(),
                            message    = message.trim(),
                            season     = season.trim(),
                            iconEmoji  = iconEmoji.trim(),
                            targetRole = targetRole,
                            isActive   = true
                        )
                        viewModel.createAlert(alert)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) DarkBgElevated else PrimaryNavyDark
                ),
                enabled = title.isNotBlank() && message.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color       = goldC,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,  // ✅ FIX: AutoMirrored version
                        contentDescription = null,
                        tint               = goldC,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Create Alert",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 15.sp,
                        color      = goldL
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AlertFormSection
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlertFormSection(
    goldC  : Color,
    textPri: Color,
    label  : String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(goldC)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = textPri
            )
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// alertTextFieldColors
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun alertTextFieldColors(
    inputBg    : Color,
    inputBorder: Color,
    goldC      : Color,
    textPri    : Color
) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor   = inputBg,
    unfocusedContainerColor = inputBg,
    focusedBorderColor      = goldC,
    unfocusedBorderColor    = inputBorder,
    focusedTextColor        = textPri,
    unfocusedTextColor      = textPri,
    cursorColor             = goldC
)