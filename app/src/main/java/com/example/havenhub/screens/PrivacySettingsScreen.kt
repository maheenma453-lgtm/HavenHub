package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit = {}) {
    var profileVisible by remember { mutableStateOf(true) }
    var showPhone by remember { mutableStateOf(false) }
    var showEmail by remember { mutableStateOf(false) }
    var locationTracking by remember { mutableStateOf(true) }
    var dataSharing by remember { mutableStateOf(false) }
    var activityStatus by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            SettingsGroup(title = "Profile Visibility") {
                SwitchSettingItem(Icons.Default.Person, "Public Profile", "Others can view your profile", profileVisible) { profileVisible = it }
                SwitchSettingItem(Icons.Default.Phone, "Show Phone Number", "Display phone on your profile", showPhone) { showPhone = it }
                SwitchSettingItem(Icons.Default.Email, "Show Email", "Display email on your profile", showEmail) { showEmail = it }
                SwitchSettingItem(Icons.Default.Circle, "Activity Status", "Show when you're online", activityStatus) { activityStatus = it }
            }

            SettingsGroup(title = "Data & Permissions") {
                SwitchSettingItem(Icons.Default.LocationOn, "Location Access", "Allow app to use your location", locationTracking) { locationTracking = it }
                SwitchSettingItem(Icons.Default.Share, "Data Sharing", "Share usage data to improve the app", dataSharing) { dataSharing = it }
            }

            SettingsGroup(title = "Legal") {
                SettingsItem(icon = Icons.Default.Policy, label = "Privacy Policy", onClick = {})
                SettingsItem(icon = Icons.Default.Gavel, label = "Terms of Service", onClick = {})
                SettingsItem(icon = Icons.Default.Cookie, label = "Cookie Policy", onClick = {})
            }
        }
    }
}
