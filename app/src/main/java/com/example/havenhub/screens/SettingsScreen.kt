package com.example.havenhub.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.ErrorRed
import com.example.havenhub.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel    : SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                // ✅ MaterialTheme use — auto dark/light
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        // ✅ Scaffold background auto dark/light
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Account ──────────────────────────────────────────────────────
            SettingsGroup(title = "Account") {
                SettingsItem(
                    icon    = Icons.Default.ManageAccounts,
                    label   = "Account Settings",
                    onClick = { navController.navigate(Screen.AccountSettings.route) }
                )
                SettingsItem(
                    icon    = Icons.Default.Lock,
                    label   = "Privacy Settings",
                    onClick = { navController.navigate(Screen.PrivacySettings.route) }
                )
            }

            // ── Preferences ──────────────────────────────────────────────────
            SettingsGroup(title = "Preferences") {
                SettingsItem(
                    icon     = Icons.Default.Notifications,
                    label    = "Notification Settings",
                    subtitle = if (uiState.userPreferences?.hasAnyNotificationsEnabled == true)
                        "Enabled" else "Disabled",
                    onClick  = { navController.navigate(Screen.NotificationSettings.route) }
                )

                // ✅ Dark Mode — WhatsApp style Switch
                DarkModeToggleItem(
                    isDarkMode = uiState.userPreferences?.isDarkMode == true,
                    onToggle   = { viewModel.toggleDarkMode(it) }
                )
            }

            // ── Support ──────────────────────────────────────────────────────
            SettingsGroup(title = "Support") {
                SettingsItem(
                    icon    = Icons.AutoMirrored.Filled.Help,
                    label   = "Help & Support",
                    onClick = { navController.navigate(Screen.HelpAndSupport.route) }
                )
                SettingsItem(
                    icon    = Icons.Default.Info,
                    label   = "About",
                    onClick = { navController.navigate(Screen.About.route) }
                )
            }

            // ── Error ────────────────────────────────────────────────────────
            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = error,
                    color    = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ✅ WhatsApp-style Dark Mode toggle — Switch dikhta hai, click pe bhi toggle hota hai
@Composable
fun DarkModeToggleItem(
    isDarkMode: Boolean,
    onToggle  : (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isDarkMode) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector      = Icons.Default.DarkMode,
            contentDescription = null,
            tint             = MaterialTheme.colorScheme.primary,
            modifier         = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = "Dark Mode",
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text     = if (isDarkMode) "On" else "Off",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        // ✅ WhatsApp-style Switch
        Switch(
            checked         = isDarkMode,
            onCheckedChange = { onToggle(it) },
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// ✅ SettingsGroup — MaterialTheme colors
@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = title,
            fontSize   = 12.sp,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                // ✅ Dark mode mein dark card, light mein light card
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column { content() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ✅ SettingsItem — MaterialTheme colors
@Composable
fun SettingsItem(
    icon    : ImageVector,
    label   : String,
    subtitle: String? = null,
    onClick : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}









