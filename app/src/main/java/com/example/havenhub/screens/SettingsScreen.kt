package com.example.havenhub.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel    : SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark  = isSystemInDarkTheme()
    val cs      = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(PrimaryNavyDark, PrimaryNavy))
                    )
                    .statusBarsPadding()
                    .height(58.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Gold shimmer line
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GoldAccent.copy(alpha = 0.75f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        "Settings",
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 20.sp,
                        color         = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = if (isDark) DarkGoldPrimary else GoldAccent
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(10.dp))

            // ── ACCOUNT ──────────────────────────────────────────────
            SettingsGroupEnhanced(title = "ACCOUNT", isDark = isDark) {
                SettingsItemEnhanced(
                    icon        = Icons.Default.ManageAccounts,
                    label       = "Account Settings",
                    showDivider = true,
                    isDark      = isDark,
                    onClick     = { navController.navigate(Screen.AccountSettings.route) }
                )
                SettingsItemEnhanced(
                    icon        = Icons.Default.Lock,
                    label       = "Privacy Settings",
                    showDivider = false,
                    isDark      = isDark,
                    onClick     = { navController.navigate(Screen.PrivacySettings.route) }
                )
            }

            // ── PREFERENCES ──────────────────────────────────────────
            SettingsGroupEnhanced(title = "PREFERENCES", isDark = isDark) {
                SettingsItemEnhanced(
                    icon        = Icons.Default.Notifications,
                    label       = "Notification Settings",
                    subtitle    = if (uiState.userPreferences?.hasAnyNotificationsEnabled == true)
                        "Enabled" else "Disabled",
                    showDivider = true,
                    isDark      = isDark,
                    onClick     = { navController.navigate(Screen.NotificationSettings.route) }
                )
                DarkModeToggleItemEnhanced(
                    isDarkMode = uiState.userPreferences?.isDarkMode == true,
                    isDark     = isDark,
                    onToggle   = { viewModel.toggleDarkMode(it) }
                )
            }

            // ── SUPPORT ──────────────────────────────────────────────
            SettingsGroupEnhanced(title = "SUPPORT", isDark = isDark) {
                SettingsItemEnhanced(
                    icon        = Icons.AutoMirrored.Filled.Help,
                    label       = "Help & Support",
                    showDivider = true,
                    isDark      = isDark,
                    onClick     = { navController.navigate(Screen.HelpAndSupport.route) }
                )
                SettingsItemEnhanced(
                    icon        = Icons.Default.Info,
                    label       = "About",
                    showDivider = false,
                    isDark      = isDark,
                    onClick     = { navController.navigate(Screen.About.route) }
                )
            }

            // ── Error ─────────────────────────────────────────────────
            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = cs.errorContainer
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint     = cs.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = cs.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Footer version badge ──────────────────────────────────
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    GoldAccent.copy(alpha = if (isDark) 0.20f else 0.13f),
                                    GoldAccentLight.copy(alpha = if (isDark) 0.25f else 0.16f),
                                    GoldAccent.copy(alpha = if (isDark) 0.20f else 0.13f)
                                )
                            ),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Text(
                        "HavenHub v1.0.0",
                        fontSize      = 11.sp,
                        color         = if (isDark) DarkGoldLight else GoldAccentDark,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Spacer(Modifier.height(88.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Section group — gold bar header + elevated card
// ─────────────────────────────────────────────────────────────────────
@Composable
fun SettingsGroupEnhanced(
    title  : String,
    isDark : Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier          = Modifier.padding(
                start  = 20.dp, end = 20.dp,
                top    = 16.dp, bottom = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gradient gold bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isDark) DarkGoldPrimary else GoldAccent,
                                if (isDark) DarkGoldDim     else GoldAccentDark
                            )
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text          = title,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = if (isDark) DarkGoldLight else GoldAccentDark,
                letterSpacing = 1.1.sp
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation    = if (isDark) 0.dp else 4.dp,
                    shape        = RoundedCornerShape(16.dp),
                    ambientColor = PrimaryNavyDark.copy(alpha = 0.09f),
                    spotColor    = PrimaryNavyDark.copy(alpha = 0.15f)
                ),
            shape          = RoundedCornerShape(16.dp),
            color          = cs.surface,
            tonalElevation = if (isDark) 3.dp else 0.dp
        ) {
            Column(content = content)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Settings row item
// ─────────────────────────────────────────────────────────────────────
@Composable
fun SettingsItemEnhanced(
    icon       : ImageVector,
    label      : String,
    subtitle   : String?  = null,
    showDivider: Boolean  = true,
    isDark     : Boolean  = isSystemInDarkTheme(),
    onClick    : () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gold icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isDark) DarkGoldFaint.copy(alpha = 0.8f)
                        else GoldAccent.copy(alpha = 0.11f),
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = if (isDark) DarkGoldPrimary else GoldAccent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = cs.onSurface
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = cs.onSurfaceVariant)
                }
            }
            // Circular chevron
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (isDark) DarkGoldFaint.copy(alpha = 0.6f)
                        else GoldAccent.copy(alpha = 0.09f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint     = if (isDark) DarkGoldPrimary.copy(alpha = 0.8f)
                    else GoldAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier  = Modifier.padding(start = 70.dp, end = 16.dp),
                color     = cs.outline.copy(alpha = 0.30f),
                thickness = 0.5.dp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Dark mode toggle with gold switch
// ─────────────────────────────────────────────────────────────────────
@Composable
fun DarkModeToggleItemEnhanced(
    isDarkMode: Boolean,
    isDark    : Boolean = isSystemInDarkTheme(),
    onToggle  : (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isDarkMode) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isDark) DarkGoldFaint.copy(alpha = 0.8f)
                    else GoldAccent.copy(alpha = 0.11f),
                    RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DarkMode,
                contentDescription = null,
                tint     = if (isDark) DarkGoldPrimary else GoldAccent,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Dark Mode",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = cs.onSurface
            )
            Text(
                if (isDarkMode) "On" else "Off",
                fontSize = 12.sp,
                color    = cs.onSurfaceVariant
            )
        }
        Switch(
            checked         = isDarkMode,
            onCheckedChange = { onToggle(it) },
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = if (isDark) DarkGoldPrimary else GoldAccent,
                uncheckedThumbColor = cs.outline,
                uncheckedTrackColor = cs.surfaceVariant
            )
        )
    }
}

// ── Legacy aliases — no logic removed ────────────────────────────────
@Composable
fun DarkModeToggleItem(isDarkMode: Boolean, onToggle: (Boolean) -> Unit) =
    DarkModeToggleItemEnhanced(isDarkMode = isDarkMode, onToggle = onToggle)

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val isDark = isSystemInDarkTheme()
    SettingsGroupEnhanced(title = title, isDark = isDark, content = content)
}

@Composable
fun SettingsItem(icon: ImageVector, label: String, subtitle: String? = null, onClick: () -> Unit) =
    SettingsItemEnhanced(icon = icon, label = label, subtitle = subtitle, onClick = onClick)















