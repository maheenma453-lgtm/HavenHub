package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.viewmodel.SettingsViewModel

// ── Theme colors ──────────────────────────────────────────────────────
private val NavyBlue   = Color(0xFF1A2A5E)
private val GoldColor  = Color(0xFFB8922A)
private val GoldTrack  = Color(0xFFC49A2A)
private val IconBg     = Color(0xFFF5F0E8)
private val CardBg     = Color(0xFFFFFFFF)
private val PageBg     = Color(0xFFF0F2F5)
private val Divider    = Color(0xFFF0F0F0)

// ── Section Label with gold left bar ─────────────────────────────────
@Composable
private fun SectionLabel(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GoldColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = title.uppercase(),
            color      = GoldColor,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ── Icon Box (beige background like Privacy screen) ───────────────────
@Composable
private fun IconBox(icon: ImageVector) {
    Box(
        modifier          = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(IconBg),
        contentAlignment  = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = GoldColor,
            modifier           = Modifier.size(22.dp)
        )
    }
}

// ── Gold Toggle Switch ────────────────────────────────────────────────
@Composable
private fun GoldSwitch(
    checked        : Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked         = checked,
        onCheckedChange = onCheckedChange,
        colors          = SwitchDefaults.colors(
            checkedThumbColor   = Color.White,
            checkedTrackColor   = GoldTrack,
            checkedBorderColor  = GoldTrack,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFDDDDDD),
            uncheckedBorderColor = Color(0xFFDDDDDD)
        )
    )
}

// ── Single Setting Row ────────────────────────────────────────────────
@Composable
private fun SettingRow(
    icon           : ImageVector,
    label          : String,
    subtitle       : String,
    checked        : Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider    : Boolean = true
) {
    Column {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(icon = icon)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = label,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1A1A2E)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = Color(0xFF888888)
                )
            }
            GoldSwitch(
                checked         = checked,
                onCheckedChange = onCheckedChange
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier  = Modifier.padding(horizontal = 16.dp),
                thickness = 0.8.dp,
                color     = Divider
            )
        }
    }
}

// ── Settings Card Container ───────────────────────────────────────────
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(content = content)
    }
}

// ── Main Screen ───────────────────────────────────────────────────────
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
                title = {
                    Text(
                        text       = "Notification Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = NavyBlue,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = PageBg
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldColor)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Push Notifications ────────────────────────────────────
            SectionLabel(title = "Notification Channels")
            Spacer(Modifier.height(10.dp))

            SettingsCard {
                SettingRow(
                    icon = Icons.Default.Notifications,
                    label = "Push Notifications",
                    subtitle = "App notifications on your device",
                    checked = prefs?.hasAnyNotificationsEnabled ?: true,
                    onCheckedChange = { viewModel.toggleNotifications(it) },
                    showDivider = false
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Notification Types ────────────────────────────────────
            SectionLabel(title = "Notification Types")
            Spacer(Modifier.height(10.dp))

            SettingsCard {
                SettingRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Booking Alerts",
                    subtitle = "Confirmations, reminders, cancellations",
                    checked = prefs?.notifyBookingUpdates ?: true,
                    onCheckedChange = {
                        viewModel.updateNotificationChannel(
                            "notifyBookingUpdates",
                            it
                        )
                    }
                )
                SettingRow(
                    icon = Icons.Default.Payment,
                    label = "Payment Alerts",
                    subtitle = "Receipts and payment confirmations",
                    checked = prefs?.notifyPayments ?: true,
                    onCheckedChange = { viewModel.updateNotificationChannel("notifyPayments", it) }
                )
                SettingRow(
                    icon = Icons.AutoMirrored.Filled.Message,
                    label = "Message Alerts",
                    subtitle = "New messages from hosts or tenants",
                    checked = prefs?.notifyMessages ?: true,
                    onCheckedChange = { viewModel.updateNotificationChannel("notifyMessages", it) }
                )
                SettingRow(
                    icon = Icons.Default.LocalOffer,
                    label = "Promotions",
                    subtitle = "Deals and special offers",
                    checked = prefs?.notifyPromotions ?: false,
                    onCheckedChange = {
                        viewModel.updateNotificationChannel(
                            "notifyPromotions",
                            it
                        )
                    }
                )
                SettingRow(
                    icon = Icons.Default.Info,
                    label = "System Updates",
                    subtitle = "App updates and announcements",
                    checked = prefs?.notifyAdminAlerts ?: true,
                    onCheckedChange = {
                        viewModel.updateNotificationChannel(
                            "notifyAdminAlerts",
                            it
                        )
                    },
                    showDivider = false
                )
            }

            // ── Error Message ─────────────────────────────────────────
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
    }