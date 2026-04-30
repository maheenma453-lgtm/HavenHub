package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val userId  = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var showNoteDialog   by remember { mutableStateOf(false) }
    var dialogTitle      by remember { mutableStateOf("") }
    var dialogNote       by remember { mutableStateOf("") }
    var dialogIsApproved by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) viewModel.observeNotifications(userId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (dialogIsApproved) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        null,
                        tint     = if (dialogIsApproved) Color(0xFF4CAF50) else Color.Red,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(dialogTitle, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (dialogIsApproved)
                            "Mubarak! Aapki property admin ne approve kar di hai! 🎉"
                        else
                            "Aapki property admin ne reject kar di hai.",
                        fontSize = 14.sp
                    )
                    if (dialogNote.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Admin Note:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextSecondary)
                        Card(
                            shape  = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (dialogIsApproved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                        ) {
                            Text(
                                text     = dialogNote,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color    = if (dialogIsApproved) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNoteDialog = false },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (dialogIsApproved) Color(0xFF4CAF50) else Color.Red
                    )
                ) { Text("OK") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${uiState.unreadCount}",
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        null,
                        modifier = Modifier.size(72.dp),
                        tint     = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications yet", color = TextSecondary, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Aapki notifications yahan dikhengi",
                        color    = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = uiState.notifications,
                    // ✅ FIX: duplicate key crash fix — index fallback add kiya
                    key   = { notification ->
                        notification.notificationId
                            .ifEmpty { notification.hashCode().toString() }
                    }
                ) { notification ->
                    val enumType = try {
                        NotificationType.valueOf(notification.type)
                    } catch (e: Exception) {
                        NotificationType.GENERAL
                    }

                    NotificationCard(
                        item     = notification,
                        enumType = enumType,
                        onDelete = { viewModel.deleteNotification(notification.notificationId, userId) },
                        onClick  = {
                            viewModel.markAsRead(notification.notificationId, userId)
                            when (enumType) {
                                NotificationType.PROPERTY_APPROVED -> {
                                    dialogTitle      = "Property Approved ✓"
                                    dialogNote       = notification.adminNote.ifEmpty { notification.body }
                                    dialogIsApproved = true
                                    showNoteDialog   = true
                                }
                                NotificationType.PROPERTY_REJECTED -> {
                                    dialogTitle      = "Property Rejected"
                                    dialogNote       = notification.adminNote.ifEmpty { notification.body }
                                    dialogIsApproved = false
                                    showNoteDialog   = true
                                }
                                NotificationType.BOOKING_CONFIRMED,
                                NotificationType.BOOKING_CANCELLED,
                                NotificationType.BOOKING_REMINDER,
                                NotificationType.BOOKING_COMPLETED,
                                NotificationType.BOOKING_REQUESTED -> {
                                    if (notification.referenceId.isNotEmpty())
                                        navController.navigate(
                                            Screen.BookingDetails.createRoute(notification.referenceId)
                                        )
                                }
                                NotificationType.NEW_MESSAGE -> {
                                    if (notification.referenceId.isNotEmpty())
                                        navController.navigate(
                                            Screen.Chat.createRoute(notification.referenceId)
                                        )
                                }
                                else -> { }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item    : Notification,
    enumType: NotificationType,
    onClick : () -> Unit,
    onDelete: () -> Unit
) {
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
                .size(42.dp)
                .clip(CircleShape)
                .background(screenNotificationColor(enumType).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = screenNotificationIcon(enumType),
                contentDescription = null,
                tint               = screenNotificationColor(enumType),
                modifier           = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = item.title.ifEmpty { enumType.name.replace("_", " ") },
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Medium,
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    text     = item.createdAt?.toDate()?.let {
                        java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(it)
                    } ?: "-",
                    fontSize = 11.sp,
                    color    = TextSecondary
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text     = item.body,
                fontSize = 13.sp,
                color    = if (!item.isRead) TextPrimary else TextSecondary,
                maxLines = 2
            )
            if (item.adminNote.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info, null,
                        modifier = Modifier.size(12.dp),
                        tint     = when (enumType) {
                            NotificationType.PROPERTY_APPROVED -> Color(0xFF4CAF50)
                            NotificationType.PROPERTY_REJECTED -> Color.Red
                            else                               -> TextSecondary
                        }
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text     = item.adminNote,
                        fontSize = 12.sp,
                        color    = when (enumType) {
                            NotificationType.PROPERTY_APPROVED -> Color(0xFF4CAF50)
                            NotificationType.PROPERTY_REJECTED -> Color.Red
                            else                               -> TextSecondary
                        },
                        maxLines = 1
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                )
                Spacer(Modifier.height(8.dp))
            }
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close, "Delete",
                    tint     = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color    = BorderGray.copy(alpha = 0.4f)
    )
}

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
    NotificationType.PROPERTY_APPROVED -> Icons.Default.CheckCircle
    NotificationType.PROPERTY_REJECTED -> Icons.Default.Cancel
    NotificationType.PROPERTY_PENDING  -> Icons.Default.HourglassEmpty
    NotificationType.USER_VERIFIED     -> Icons.Default.VerifiedUser
    NotificationType.USER_REJECTED     -> Icons.Default.PersonOff
    NotificationType.USER_VERIFICATION_PENDING -> Icons.Default.PersonSearch
    else                               -> Icons.Default.Notifications
}

fun screenNotificationColor(type: NotificationType): Color = when (type) {
    NotificationType.BOOKING_REQUESTED,
    NotificationType.BOOKING_CONFIRMED  -> PrimaryBlue
    NotificationType.BOOKING_CANCELLED  -> Color.Red
    NotificationType.PAYMENT_RECEIVED   -> Color(0xFF4CAF50)
    NotificationType.PROPERTY_APPROVED  -> Color(0xFF4CAF50)
    NotificationType.PROPERTY_REJECTED  -> Color.Red
    NotificationType.PROPERTY_PENDING   -> Color(0xFFFF9800)
    NotificationType.USER_VERIFIED      -> Color(0xFF4CAF50)
    NotificationType.USER_REJECTED      -> Color.Red
    else                                -> Color.Gray
}