package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.NotificationType
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    navController : NavController,
    notificationId: String,
    viewModel     : NotificationViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val userId  = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Find notification from state
    val notification = uiState.notifications.find { it.notificationId == notificationId }

    // Mark as read when screen opens
    LaunchedEffect(notificationId) {
        if (notificationId.isNotEmpty()) viewModel.markAsRead(notificationId, userId)
    }

    val primary          = MaterialTheme.colorScheme.primary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val background       = MaterialTheme.colorScheme.background
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline          = MaterialTheme.colorScheme.outline

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = primary,
                    titleContentColor          = onPrimary,
                    navigationIconContentColor = onPrimary
                )
            )
        },
        containerColor = background
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primary)
            }
            return@Scaffold
        }

        if (notification == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Notification not found.", color = onSurfaceVariant)
            }
            return@Scaffold
        }

        // Safely convert string type to enum
        val enumType = try {
            NotificationType.valueOf(notification.type)
        } catch (e: Exception) {
            NotificationType.BOOKING_REQUESTED
        }

        val notifColor = getNotificationColor(enumType)

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Notification type icon ────────────────────────────────────────
            Box(
                modifier = Modifier.size(90.dp).clip(CircleShape)
                    .background(notifColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = getNotificationIcon(enumType),
                    contentDescription = null,
                    tint               = notifColor,
                    modifier           = Modifier.size(44.dp)
                )
            }

            // ── Type label chip ───────────────────────────────────────────────
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(notifColor.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = enumType.name.replace("_", " "),
                    color      = notifColor,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text      = notification.title,
                fontSize  = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = onSurface
            )

            // ── Timestamp ─────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(text = notification.createdAt?.toString() ?: "-", fontSize = 13.sp, color = onSurfaceVariant)
            }

            HorizontalDivider(color = outline)

            // ── Message body ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text       = notification.body,
                    modifier   = Modifier.padding(16.dp),
                    fontSize   = 15.sp,
                    lineHeight = 24.sp,
                    color      = onSurface
                )
            }

            // ── Action button ─────────────────────────────────────────────────
            NotificationActionButton(
                type    = enumType,
                onClick = {
                    when (enumType) {
                        NotificationType.BOOKING_REQUESTED,
                        NotificationType.BOOKING_CONFIRMED,
                        NotificationType.BOOKING_CANCELLED,
                        NotificationType.BOOKING_COMPLETED,
                        NotificationType.BOOKING_REMINDER -> {
                            if (notification.referenceId.isNotEmpty())
                                navController.navigate(Screen.BookingDetails.createRoute(notification.referenceId))
                        }
                        NotificationType.PAYMENT_RECEIVED,
                        NotificationType.PAYMENT_FAILED,
                        NotificationType.REFUND_ISSUED    -> { /* navigate to payment detail */ }
                        NotificationType.NEW_MESSAGE      -> {
                            if (notification.referenceId.isNotEmpty())
                                navController.navigate(Screen.Chat.createRoute(notification.referenceId))
                        }
                        else -> { }
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Helper: get icon for notification type ────────────────────────────────────
@Composable
fun getNotificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.BOOKING_REQUESTED -> Icons.Default.EventNote
    NotificationType.NEW_MESSAGE       -> Icons.AutoMirrored.Filled.Message
    NotificationType.PAYMENT_RECEIVED  -> Icons.Default.Payments
    NotificationType.ACCOUNT_VERIFIED  -> Icons.Default.VerifiedUser
    else                               -> Icons.Default.Notifications
}

// ── Helper: get color for notification type ───────────────────────────────────
@Composable
fun getNotificationColor(type: NotificationType): Color = when (type) {
    NotificationType.BOOKING_REQUESTED,
    NotificationType.BOOKING_CONFIRMED -> MaterialTheme.colorScheme.primary
    NotificationType.BOOKING_CANCELLED -> MaterialTheme.colorScheme.error
    NotificationType.PAYMENT_RECEIVED  -> Color(0xFF4CAF50)
    else                               -> MaterialTheme.colorScheme.onSurfaceVariant
}

// ── Action button based on notification type ──────────────────────────────────
@Composable
fun NotificationActionButton(type: NotificationType, onClick: () -> Unit) {
    val (label, icon) = when (type) {
        NotificationType.BOOKING_REQUESTED,
        NotificationType.BOOKING_CONFIRMED,
        NotificationType.BOOKING_CANCELLED,
        NotificationType.BOOKING_COMPLETED,
        NotificationType.BOOKING_REMINDER  -> "View Booking"  to Icons.Default.CalendarToday
        NotificationType.PAYMENT_RECEIVED,
        NotificationType.PAYMENT_FAILED,
        NotificationType.REFUND_ISSUED     -> "View Payment"  to Icons.Default.Payment
        NotificationType.NEW_MESSAGE       -> "Open Chat"     to Icons.AutoMirrored.Filled.Message
        else                               -> "View Details"  to Icons.AutoMirrored.Filled.OpenInNew
    }

    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}