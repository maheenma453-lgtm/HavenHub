package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

// ─── Dummy Data Model ───────────────────────────────────────────────
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val type: String,        // "booking", "payment", "message", "system"
    val isRead: Boolean
)

val dummyNotifications = listOf(
    NotificationItem("N001", "Booking Confirmed!", "Your booking for Luxury Sea View Apartment has been confirmed.", "2 min ago", "booking", false),
    NotificationItem("N002", "Payment Received", "Payment of Rs. 34,000 received via JazzCash.", "1 hr ago", "payment", false),
    NotificationItem("N003", "New Message", "Ali Raza sent you a message: 'Welcome! Check-in is at 2 PM.'", "3 hrs ago", "message", false),
    NotificationItem("N004", "Booking Reminder", "Your check-in at Clifton Apartment is tomorrow.", "Yesterday", "booking", true),
    NotificationItem("N005", "System Update", "We've updated our privacy policy. Please review the changes.", "2 days ago", "system", true),
    NotificationItem("N006", "Review Request", "How was your stay at DHA Residency? Leave a review!", "3 days ago", "system", true),
    NotificationItem("N007", "Booking Cancelled", "Your booking BK-20241030-0031 has been cancelled.", "5 days ago", "booking", true),
)

// ─── Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onNotificationClick: (String) -> Unit = {}
) {
    var notifications by remember { mutableStateOf(dummyNotifications) }
    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = "$unreadCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = {
                            notifications = notifications.map { it.copy(isRead = true) }
                        }) {
                            Text("Mark all read", fontSize = 12.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications yet", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        item = notification,
                        onClick = {
                            notifications = notifications.map {
                                if (it.id == notification.id) it.copy(isRead = true) else it
                            }
                            onNotificationClick(notification.id)
                        }
                    )
                }
            }
        }
    }
}

// ─── Notification Card ───────────────────────────────────────────────
@Composable
fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    val bgColor = if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(notificationColor(item.type).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notificationIcon(item.type),
                contentDescription = null,
                tint = notificationColor(item.type),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(text = item.time, fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(2.dp))
            Text(text = item.message, fontSize = 13.sp, color = Color.Gray, maxLines = 2)
        }

        if (!item.isRead) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.CenterVertically)
            )
        }
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
}

// ─── Helpers ─────────────────────────────────────────────────────────
fun notificationIcon(type: String): ImageVector = when (type) {
    "booking" -> Icons.Default.CalendarToday
    "payment" -> Icons.Default.Payment
    "message" -> Icons.Default.Message
    else      -> Icons.Default.Info
}

fun notificationColor(type: String): Color = when (type) {
    "booking" -> Color(0xFF1565C0)
    "payment" -> Color(0xFF2E7D32)
    "message" -> Color(0xFF6A1B9A)
    else      -> Color(0xFFE65100)
}
