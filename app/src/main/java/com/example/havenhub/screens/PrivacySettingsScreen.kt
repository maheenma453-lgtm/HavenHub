package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    navController: NavController,
    viewModel    : SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs   = uiState.userPreferences

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings", fontWeight = FontWeight.Bold) },
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
            // Profile Visibility
            SettingsGroup(title = "Profile Visibility") {
                SwitchSettingItem(
                    icon            = Icons.Default.Person,
                    label           = "Public Profile",
                    subtitle        = "Others can view your profile",
                    checked         = prefs?.isProfilePublic ?: true,
                    onCheckedChange = {
                        prefs?.let { p -> viewModel.savePreferences(p.copy(isProfilePublic = it)) }
                    }
                )
                SwitchSettingItem(
                    icon            = Icons.Default.Phone,
                    label           = "Show Phone Number",
                    subtitle        = "Display phone on your profile",
                    checked         = prefs?.showPhoneNumber ?: false,
                    onCheckedChange = {
                        prefs?.let { p -> viewModel.savePreferences(p.copy(showPhoneNumber = it)) }
                    }
                )
                SwitchSettingItem(
                    icon            = Icons.Default.Email,
                    label           = "Show Email",
                    subtitle        = "Display email on your profile",
                    checked         = prefs?.showEmail ?: false,
                    onCheckedChange = {
                        prefs?.let { p -> viewModel.savePreferences(p.copy(showEmail = it)) }
                    }
                )
            }

            // Data & Permissions
            SettingsGroup(title = "Data & Permissions") {
                SwitchSettingItem(
                    icon            = Icons.Default.LocationOn,
                    label           = "Location Access",
                    subtitle        = "Allow app to use your location",
                    checked         = true,
                    onCheckedChange = { }
                )
                SwitchSettingItem(
                    icon            = Icons.Default.Share,
                    label           = "Data Sharing",
                    subtitle        = "Share usage data to improve the app",
                    checked         = false,
                    onCheckedChange = { }
                )
            }

            // Legal
            SettingsGroup(title = "Legal") {
                SettingsItem(icon = Icons.Default.Policy, label = "Privacy Policy",   onClick = {})
                SettingsItem(icon = Icons.Default.Gavel,  label = "Terms of Service", onClick = {})
                SettingsItem(icon = Icons.Default.Cookie, label = "Cookie Policy",    onClick = {})
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text     = error,
                    color    = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// ── Switch Setting Item ───────────────────────────────────────────────────────
@Composable
fun SwitchSettingItem(
    icon           : androidx.compose.ui.graphics.vector.ImageVector,
    label          : String,
    subtitle       : String,
    checked        : Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}