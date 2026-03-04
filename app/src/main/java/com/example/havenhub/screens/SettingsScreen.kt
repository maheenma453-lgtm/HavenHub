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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = PrimaryBlue,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // Account
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

            // Preferences
            SettingsGroup(title = "Preferences") {
                SettingsItem(
                    icon     = Icons.Default.Notifications,
                    label    = "Notification Settings",
                    subtitle = if (uiState.userPreferences?.hasAnyNotificationsEnabled == true)
                        "Enabled" else "Disabled",
                    onClick  = { navController.navigate(Screen.NotificationSettings.route) }
                )
                SettingsItem(
                    icon     = Icons.Default.DarkMode,
                    label    = "Dark Mode",
                    subtitle = if (uiState.userPreferences?.isDarkMode == true) "On" else "Off",
                    onClick  = {
                        viewModel.toggleDarkMode(
                            uiState.userPreferences?.isDarkMode?.not() ?: false
                        )
                    }
                )
            }

            // Support
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

            // Error Message
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

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = title,
            fontSize = 12.sp,
            color    = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
        ) {
            Column { content() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun SettingsItem(
    icon    : ImageVector,
    label   : String,
    subtitle: String? = null,
    onClick : () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = TextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}