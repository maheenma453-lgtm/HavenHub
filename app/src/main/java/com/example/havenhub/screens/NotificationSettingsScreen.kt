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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel    : SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.userPreferences

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
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

            // Notification Channels
            SettingsGroup(title = "Notification Channels") {
                SwitchSettingItem(
                    icon             = Icons.Default.Notifications,
                    label            = "Push Notifications",
                    subtitle         = "App notifications on your device",
                    checked          = prefs?.hasAnyNotificationsEnabled ?: true,
                    onCheckedChange  = { viewModel.toggleNotifications(it) }
                )
            }

            // Notification Types
            SettingsGroup(title = "Notification Types") {
                SwitchSettingItem(
                    icon            = Icons.Default.CalendarToday,
                    label           = "Booking Alerts",
                    subtitle        = "Confirmations, reminders, cancellations",
                    checked         = prefs?.notifyBookingUpdates ?: true,
                    onCheckedChange = { viewModel.updateNotificationChannel("notifyBookingUpdates", it) }
                )
                SwitchSettingItem(
                    icon            = Icons.Default.Payment,
                    label           = "Payment Alerts",
                    subtitle        = "Receipts and payment confirmations",
                    checked         = prefs?.notifyPayments ?: true,
                    onCheckedChange = { viewModel.updateNotificationChannel("notifyPayments", it) }
                )
                SwitchSettingItem(
                    icon            = Icons.Default.Message,
                    label           = "Message Alerts",
                    subtitle        = "New messages from hosts or tenants",
                    checked         = prefs?.notifyMessages ?: true,
                    onCheckedChange = { viewModel.updateNotificationChannel("notifyMessages", it) }
                )
                SwitchSettingItem(
                    icon            = Icons.Default.LocalOffer,
                    label           = "Promotions",
                    subtitle        = "Deals and special offers",
                    checked         = prefs?.notifyPromotions ?: false,
                    onCheckedChange = { viewModel.updateNotificationChannel("notifyPromotions", it) }
                )
                SwitchSettingItem(
                    icon            = Icons.Default.Info,
                    label           = "System Updates",
                    subtitle        = "App updates and announcements",
                    checked         = prefs?.notifyAdminAlerts ?: true,
                    onCheckedChange = { viewModel.updateNotificationChannel("notifyAdminAlerts", it) }
                )
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Text(
                    text     = error,
                    color    = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SwitchSettingItem(
    icon           : ImageVector,
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
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
        )
    }
}