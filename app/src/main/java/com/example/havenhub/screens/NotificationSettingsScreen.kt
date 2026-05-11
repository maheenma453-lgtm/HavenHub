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
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel    : SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs   = uiState.userPreferences

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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {

            // ── Push notification toggle ──────────────────────────────────────
            SettingsGroup(title = "Notification Channels") {
                SwitchSettingItem(
                    icon            = Icons.Default.Notifications,
                    label           = "Push Notifications",
                    subtitle        = "App notifications on your device",
                    checked         = prefs?.hasAnyNotificationsEnabled ?: true,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
            }

            // ── Notification types ────────────────────────────────────────────
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

            // ── Error message ─────────────────────────────────────────────────
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
