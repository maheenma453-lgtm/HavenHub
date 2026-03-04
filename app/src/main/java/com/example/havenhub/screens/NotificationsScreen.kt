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
                                    .background(PrimaryBlue)
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
                    NotificationCard(
                        item    = notification,
                        onClick = {
                            viewModel.markAsRead(notification.notificationId, userId)
                            // Deep link navigation based on type
                            when (notification.type) {
                                NotificationType.BOOKING_CONFIRMED,
                                NotificationType.BOOKING_CANCELLED,
                                NotificationType.BOOKING_REMINDER,
                                NotificationType.BOOKING_COMPLETED,
                                NotificationType.BOOKING_REQUESTED -> {
                                    if (notification.referenceId.isNotEmpty()) {
                                        navController.navigate(
                                            Screen.BookingDetails.createRoute(notification.referenceId)
                                        )
                                    }
                                }
                                NotificationType.PAYMENT_RECEIVED,
                                NotificationType.PAYMENT_FAILED,
                                NotificationType.REFUND_ISSUED -> {
                                    if (notification.referenceId.isNotEmpty()) {
                                        navController.navigate(
                                            Screen.PaymentSuccess.createRoute(notification.referenceId)
                                        )
                                    }
                                }
                                NotificationType.NEW_MESSAGE -> {
                                    if (notification.referenceId.isNotEmpty()) {
                                        navController.navigate(
                                            Screen.Chat.createRoute(notification.referenceId)
                                        )
                                    }
                                }
                                else -> { }
                            }
                        }
                    )
                }
            }
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

@Composable
fun NotificationCard(item: Notification, onClick: () -> Unit) {
    val bgColor = if (item.isRead) MaterialTheme.colorScheme.surface
    else PrimaryBlue.copy(alpha = 0.07f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(notificationColor(item.type).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = notificationIcon(item.type),
                contentDescription = null,
                tint     = notificationColor(item.type),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = item.type.displayName(),
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal,
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    text     = item.createdAt?.toDate()?.toString() ?: "-",
                    fontSize = 11.sp,
                    color    = TextSecondary
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(text = item.body, fontSize = 13.sp, color = TextSecondary, maxLines = 2)
        }

        if (!item.isRead) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .align(Alignment.CenterVertically)
            )
        }
    }
    HorizontalDivider(color = BorderGray.copy(alpha = 0.4f))
}

fun notificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.BOOKING_REQUESTED,
    NotificationType.BOOKING_CONFIRMED,
    NotificationType.BOOKING_CANCELLED,
    NotificationType.BOOKING_COMPLETED,
    NotificationType.BOOKING_REMINDER  -> Icons.Default.CalendarToday
    NotificationType.PAYMENT_RECEIVED,
    NotificationType.PAYMENT_FAILED,
    NotificationType.REFUND_ISSUED     -> Icons.Default.Payment
    NotificationType.NEW_MESSAGE       -> Icons.AutoMirrored.Filled.Message
    NotificationType.NEW_REVIEW,
    NotificationType.REVIEW_REPLY      -> Icons.Default.Star
    NotificationType.PROPERTY_APPROVED,
    NotificationType.PROPERTY_REJECTED -> Icons.Default.Home
    else                               -> Icons.Default.Info
}

fun notificationColor(type: NotificationType): Color = when (type) {
    NotificationType.BOOKING_REQUESTED,
    NotificationType.BOOKING_CONFIRMED,
    NotificationType.BOOKING_CANCELLED,
    NotificationType.BOOKING_COMPLETED,
    NotificationType.BOOKING_REMINDER  -> PrimaryBlue
    NotificationType.PAYMENT_RECEIVED,
    NotificationType.REFUND_ISSUED     -> SuccessGreen
    NotificationType.PAYMENT_FAILED    -> ErrorRed
    NotificationType.NEW_MESSAGE       -> Color(0xFF6A1B9A)
    else                               -> WarningOrange
}