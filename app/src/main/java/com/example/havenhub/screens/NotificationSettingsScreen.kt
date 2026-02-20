package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit = {}) {
    var pushEnabled by remember { mutableStateOf(true) }
    var emailEnabled by remember { mutableStateOf(true) }
    var smsEnabled by remember { mutableStateOf(false) }
    var bookingAlerts by remember { mutableStateOf(true) }
    var paymentAlerts by remember { mutableStateOf(true) }
    var messageAlerts by remember { mutableStateOf(true) }
    var promoAlerts by remember { mutableStateOf(false) }
    var systemAlerts by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            SettingsGroup(title = "Notification Channels") {
                SwitchSettingItem(Icons.Default.Notifications, "Push Notifications", "App notifications on your device", pushEnabled) { pushEnabled = it }
                SwitchSettingItem(Icons.Default.Email, "Email Notifications", "Updates sent to your email", emailEnabled) { emailEnabled = it }
                SwitchSettingItem(Icons.Default.Sms, "SMS Notifications", "Text message updates", smsEnabled) { smsEnabled = it }
            }

            SettingsGroup(title = "Notification Types") {
                SwitchSettingItem(Icons.Default.CalendarToday, "Booking Alerts", "Confirmations, reminders, cancellations", bookingAlerts) { bookingAlerts = it }
                SwitchSettingItem(Icons.Default.Payment, "Payment Alerts", "Receipts and payment confirmations", paymentAlerts) { paymentAlerts = it }
                SwitchSettingItem(Icons.Default.Message, "Message Alerts", "New messages from hosts or tenants", messageAlerts) { messageAlerts = it }
                SwitchSettingItem(Icons.Default.LocalOffer, "Promotions", "Deals and special offers", promoAlerts) { promoAlerts = it }
                SwitchSettingItem(Icons.Default.Info, "System Updates", "App updates and announcements", systemAlerts) { systemAlerts = it }
            }
        }
    }
}

@Composable
fun SwitchSettingItem(icon: ImageVector, label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
