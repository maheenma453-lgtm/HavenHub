package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notificationId: String = "N001",
    onBack: () -> Unit = {},
    onActionClick: () -> Unit = {}
) {
    // In real app: fetch from ViewModel using notificationId
    val notification = dummyNotifications.find { it.id == notificationId }
        ?: dummyNotifications.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Spacer(Modifier.height(12.dp))

            // ── Icon ──
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(notificationColor(notification.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notificationIcon(notification.type),
                    contentDescription = null,
                    tint = notificationColor(notification.type),
                    modifier = Modifier.size(44.dp)
                )
            }

            // ── Type Tag ──
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(notificationColor(notification.type).copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = notification.type.replaceFirstChar { it.uppercase() },
                    color = notificationColor(notification.type),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Title ──
            Text(
                text = notification.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // ── Time ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(text = notification.time, fontSize = 13.sp, color = Color.Gray)
            }

            HorizontalDivider()

            // ── Message ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = notification.message,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Action Button (context-aware) ──
            NotificationActionButton(type = notification.type, onClick = onActionClick)

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Context-Aware Action Button ────────────────────────────────────
@Composable
fun NotificationActionButton(type: String, onClick: () -> Unit) {
    val (label, icon) = when (type) {
        "booking" -> "View Booking"  to Icons.Default.CalendarToday
        "payment" -> "View Payment"  to Icons.Default.Payment
        "message" -> "Open Chat"     to Icons.Default.Message
        else      -> "View Details"  to Icons.Default.OpenInNew
    }

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}