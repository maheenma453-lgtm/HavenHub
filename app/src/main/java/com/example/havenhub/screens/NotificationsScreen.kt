package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
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

    // ── Property Note Dialog ───────────────────────────────────────────────────
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            if (dialogIsApproved) SuccessGreen.copy(0.12f)
                            else ErrorRed.copy(0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (dialogIsApproved) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        null,
                        tint     = if (dialogIsApproved) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp,
                    color      = PrimaryNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (dialogIsApproved)
                            "Congratulations! Your property has been approved by the admin."
                        else
                            "Your property has been rejected by the admin.",
                        fontSize = 14.sp,
                        color    = TextSecondary
                    )
                    if (dialogNote.isNotEmpty()) {
                        HorizontalDivider(color = GoldAccent.copy(0.2f))
                        Text(
                            "Admin Note:",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            color      = TextSecondary
                        )
                        Card(
                            shape  = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (dialogIsApproved)
                                    SuccessGreen.copy(0.08f)
                                else
                                    ErrorRed.copy(0.08f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (dialogIsApproved) SuccessGreen.copy(0.3f)
                                else ErrorRed.copy(0.3f)
                            )
                        ) {
                            Text(
                                text     = dialogNote,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color    = if (dialogIsApproved) SuccessGreen else ErrorRed
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { showNoteDialog = false },
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (dialogIsApproved) SuccessGreen else ErrorRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = Color.White
                        )
                        if (uiState.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(GoldAccent)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "${uiState.unreadCount}",
                                    color      = PrimaryNavy,
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead(userId) }) {
                            Text(
                                "Mark all read",
                                fontSize   = 12.sp,
                                color      = GoldAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy)
            )
        },
        containerColor = Color(0xFFF0F2F8)
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryNavy)
            }
            return@Scaffold
        }

        if (uiState.notifications.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PrimaryNavy.copy(0.08f))
                            .border(1.5.dp, GoldAccent.copy(0.4f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint     = PrimaryNavy.copy(0.5f)
                        )
                    }
                    Text(
                        "No notifications yet",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = PrimaryNavy
                    )
                    Text(
                        "Your notifications will appear here",
                        color    = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.notifications,
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
                        onDelete = {
                            viewModel.deleteNotification(notification.notificationId, userId)
                        },
                        onClick  = {
                            viewModel.markAsRead(notification.notificationId, userId)
                            when (enumType) {
                                NotificationType.PROPERTY_APPROVED -> {
                                    dialogTitle      = "Property Approved"
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

// ══════════════════════════════════════════════════════════════════════════════
// NOTIFICATION CARD — Premium Navy + Gold UI
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun NotificationCard(
    item    : Notification,
    enumType: NotificationType,
    onClick : () -> Unit,
    onDelete: () -> Unit
) {
    val notifColor = screenNotificationColor(enumType)
    val notifIcon  = screenNotificationIcon(enumType)
    val isUnread   = !item.isRead

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isUnread) Color.White else Color(0xFFF8F9FC)
        ),
        elevation = CardDefaults.cardElevation(if (isUnread) 2.dp else 0.dp),
        border    = BorderStroke(
            width = if (isUnread) 1.5.dp else 1.dp,
            color = if (isUnread) PrimaryNavy.copy(0.25f) else GoldAccent.copy(0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Left: Unread indicator bar ─────────────────────────────────
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PrimaryNavy)
                )
            } else {
                Spacer(Modifier.width(3.dp))
            }

            // ── Icon ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(notifColor.copy(0.12f))
                    .border(1.dp, notifColor.copy(0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = notifIcon,
                    contentDescription = null,
                    tint               = notifColor,
                    modifier           = Modifier.size(22.dp)
                )
            }

            // ── Content ────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text       = item.title.ifEmpty { enumType.name.replace("_", " ") },
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                        fontSize   = 13.sp,
                        color      = PrimaryNavy,
                        modifier   = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = item.createdAt?.toDate()?.let {
                            java.text.SimpleDateFormat(
                                "MMM dd",
                                java.util.Locale.getDefault()
                            ).format(it)
                        } ?: "-",
                        fontSize = 10.sp,
                        color    = TextSecondary
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = item.body,
                    fontSize = 12.sp,
                    color    = if (isUnread) TextPrimary else TextSecondary,
                    maxLines = 2,
                    lineHeight = 17.sp
                )
                if (item.adminNote.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (enumType) {
                                    NotificationType.PROPERTY_APPROVED -> SuccessGreen.copy(0.08f)
                                    NotificationType.PROPERTY_REJECTED  -> ErrorRed.copy(0.08f)
                                    else                                -> GoldAccent.copy(0.08f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Info, null,
                            modifier = Modifier.size(11.dp),
                            tint     = when (enumType) {
                                NotificationType.PROPERTY_APPROVED -> SuccessGreen
                                NotificationType.PROPERTY_REJECTED  -> ErrorRed
                                else                                -> GoldAccentDark
                            }
                        )
                        Text(
                            text     = item.adminNote,
                            fontSize = 11.sp,
                            color    = when (enumType) {
                                NotificationType.PROPERTY_APPROVED -> SuccessGreen
                                NotificationType.PROPERTY_REJECTED  -> ErrorRed
                                else                                -> GoldAccentDark
                            },
                            maxLines = 1
                        )
                    }
                }
            }

            // ── Right: Unread dot + Delete ─────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GoldAccent)
                    )
                }
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Delete",
                        tint     = TextSecondary.copy(0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Icon + Color helpers ───────────────────────────────────────────────────────
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
    NotificationType.BOOKING_REQUESTED  -> PrimaryNavy
    NotificationType.BOOKING_CONFIRMED  -> SuccessGreen
    NotificationType.BOOKING_CANCELLED  -> ErrorRed
    NotificationType.BOOKING_COMPLETED  -> Color(0xFF00897B)
    NotificationType.BOOKING_REMINDER   -> GoldAccentDark
    NotificationType.PAYMENT_RECEIVED   -> SuccessGreen
    NotificationType.PAYMENT_FAILED     -> ErrorRed
    NotificationType.REFUND_ISSUED      -> WarningOrange
    NotificationType.NEW_MESSAGE        -> PrimaryNavyLight
    NotificationType.PROPERTY_APPROVED  -> SuccessGreen
    NotificationType.PROPERTY_REJECTED  -> ErrorRed
    NotificationType.PROPERTY_PENDING   -> WarningOrange
    NotificationType.USER_VERIFIED      -> SuccessGreen
    NotificationType.USER_REJECTED      -> ErrorRed
    else                                -> GoldAccentDark
}



























