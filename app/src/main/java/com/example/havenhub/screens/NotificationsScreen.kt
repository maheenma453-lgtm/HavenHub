package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.havenhub.data.Notification
import com.example.havenhub.data.NotificationType
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel    : NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) viewModel.loadNotifications(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                        if (uiState.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text       = "${uiState.unreadCount}",
                                    color      = Color.White,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead(userId) }) {
                            Text("Mark all read", fontSize = 12.sp, color = Color.White)
                        }
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

        if (uiState.notifications.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint     = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications yet", color = TextSecondary, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.notifications, key = { it.notificationId }) { notification ->
                    val enumType = try {
                        NotificationType.valueOf(notification.type)
                    } catch (e: Exception) {
                        NotificationType.BOOKING_REQUESTED
                    }

                    NotificationCard(
                        item    = notification,
                        enumType = enumType,
                        onClick = {
                            viewModel.markAsRead(notification.notificationId, userId)

                            when (enumType) {
                                NotificationType.BOOKING_CONFIRMED,
                                NotificationType.BOOKING_CANCELLED,
                                NotificationType.BOOKING_REMINDER,
                                NotificationType.BOOKING_COMPLETED,
                                NotificationType.BOOKING_REQUESTED -> {
                                    if (notification.referenceId.isNotEmpty()) {
                                        navController.navigate(Screen.BookingDetails.createRoute(notification.referenceId))
                                    }
                                }
                                NotificationType.NEW_MESSAGE -> {
                                    if (notification.referenceId.isNotEmpty()) {
                                        navController.navigate(Screen.Chat.createRoute(notification.referenceId))
                                    }
                                }
                                else -> {
                                    navController.navigate("notification_detail/${notification.notificationId}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: Notification, enumType: NotificationType, onClick: () -> Unit) {
    val bgColor = if (item.isRead) Color.Transparent else PrimaryBlue.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                // ✅ Changed to call screenNotificationColor
                .background(screenNotificationColor(enumType).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                // ✅ Changed to call screenNotificationIcon
                imageVector = screenNotificationIcon(enumType),
                contentDescription = null,
                tint = screenNotificationColor(enumType),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = enumType.name.replace("_", " "),
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.createdAt?.toString()?.take(10) ?: "-",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.body,
                fontSize = 13.sp,
                color = if (!item.isRead) TextPrimary else TextSecondary,
                maxLines = 2
            )
        }

        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderGray.copy(alpha = 0.5f))
}

// ✅ Renamed these functions to prevent conflicts with DetailScreen
fun screenNotificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.BOOKING_REQUESTED,
    NotificationType.BOOKING_CONFIRMED,
    NotificationType.BOOKING_CANCELLED,
    NotificationType.BOOKING_COMPLETED,
    NotificationType.BOOKING_REMINDER  -> Icons.Default.CalendarToday
    NotificationType.PAYMENT_RECEIVED,
    NotificationType.PAYMENT_FAILED,
    NotificationType.REFUND_ISSUED     -> Icons.Default.Payment
    NotificationType.NEW_MESSAGE       -> Icons.AutoMirrored.Filled.Message
    else                               -> Icons.Default.Notifications
}

fun screenNotificationColor(type: NotificationType): Color = when (type) {
    NotificationType.BOOKING_REQUESTED,
    NotificationType.BOOKING_CONFIRMED -> PrimaryBlue
    NotificationType.BOOKING_CANCELLED -> Color.Red
    NotificationType.PAYMENT_RECEIVED  -> Color(0xFF4CAF50)
    else                               -> Color.Gray
}