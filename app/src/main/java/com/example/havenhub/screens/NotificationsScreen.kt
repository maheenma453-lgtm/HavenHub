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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Notification
import com.example.havenhub.data.NotificationType
import com.example.havenhub.data.SeasonalAlert
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.SuccessGreen
import com.example.havenhub.ui.theme.WarningOrange
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.NotificationViewModel
import com.example.havenhub.viewmodel.SeasonalAlertViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
// NotificationsScreen.kt
//
// ✦ FIX — NEW_MESSAGE notification click:
//   referenceId = conversationId = "uid1_uid2" (sorted UIDs joined by "_")
//   generateChatId sorts [userId1, userId2] and joins with "_"
//
//   Fix steps:
//   1. Split conversationId by "_" to get both UIDs
//   2. Remove currentUserId to find otherUserId
//   3. Fetch otherUser's name from Firestore ("name" / "fullName" field)
//   4. Navigate to Screen.Chat with userId=otherUserId, ownerName=fetchedName
//
//   This opens the correct ChatScreen showing that conversation.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController         : NavController,
    viewModel             : NotificationViewModel  = hiltViewModel(),
    seasonalAlertViewModel: SeasonalAlertViewModel = hiltViewModel()
) {
    val uiState         by viewModel.uiState.collectAsState()
    val seasonalUiState by seasonalAlertViewModel.uiState.collectAsState()
    val currentUserId   = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val isAdmin   = authState.userRole == "admin"
    val userRole  = authState.userRole ?: ""

    var showNoteDialog   by remember { mutableStateOf(false) }
    var dialogTitle      by remember { mutableStateOf("") }
    var dialogNote       by remember { mutableStateOf("") }
    var dialogIsApproved by remember { mutableStateOf(true) }

    // ✦ NEW — For async sender-name fetch before navigating to chat
    var isNavigatingToChat by remember { mutableStateOf(false) }
    val coroutineScope     = rememberCoroutineScope()

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) viewModel.startListening(currentUserId)
    }

    LaunchedEffect(userRole) {
        if (userRole.isNotEmpty() && userRole != "admin" && userRole != "sub_admin") {
            seasonalAlertViewModel.loadAlertsForRole(userRole)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // ── Property approval/rejection dialog ───────────────────────────────────
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            if (dialogIsApproved) SuccessGreen.copy(0.12f)
                            else MaterialTheme.colorScheme.error.copy(0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (dialogIsApproved) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        null,
                        tint     = if (dialogIsApproved) SuccessGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (dialogIsApproved) "Congratulations! Your property has been approved by the admin."
                        else "Your property has been rejected by the admin.",
                        fontSize = 14.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dialogNote.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(0.2f))
                        Text(
                            "Admin Note:",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Card(
                            shape  = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (dialogIsApproved) SuccessGreen.copy(0.08f)
                                else MaterialTheme.colorScheme.error.copy(0.08f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (dialogIsApproved) SuccessGreen.copy(0.3f)
                                else MaterialTheme.colorScheme.error.copy(0.3f)
                            )
                        ) {
                            Text(
                                text     = dialogNote,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color    = if (dialogIsApproved) SuccessGreen
                                else MaterialTheme.colorScheme.error
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
                        containerColor = if (dialogIsApproved) SuccessGreen
                        else MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // ✦ Loading overlay while fetching sender name before chat navigation
    if (isNavigatingToChat) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color       = Color(0xFFC9973A),
                strokeWidth = 3.dp
            )
        }
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
                            color      = MaterialTheme.colorScheme.onPrimary
                        )
                        if (uiState.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "${uiState.unreadCount}",
                                    color      = MaterialTheme.colorScheme.primary,
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
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead(currentUserId) }) {
                            Text(
                                "Mark all read",
                                fontSize   = 12.sp,
                                color      = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        val hasSeasonalAlerts = seasonalUiState.alerts.isNotEmpty() && !isAdmin
        val hasRegularNotifs  = uiState.notifications.isNotEmpty()
        val hasAnything       = hasSeasonalAlerts || hasRegularNotifs

        if (!hasAnything) {
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
                            .background(MaterialTheme.colorScheme.primary.copy(0.08f))
                            .border(
                                1.5.dp,
                                MaterialTheme.colorScheme.tertiary.copy(0.4f),
                                RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint     = MaterialTheme.colorScheme.primary.copy(0.5f)
                        )
                    }
                    Text(
                        "No notifications yet",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Your notifications will appear here",
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ── Seasonal Alerts Section ───────────────────────────────────
                if (hasSeasonalAlerts) {
                    item {
                        SeasonalAlertsSection(alerts = seasonalUiState.alerts)
                    }
                }

                // ── Regular Notifications ─────────────────────────────────────
                if (hasRegularNotifs) {
                    if (hasSeasonalAlerts) {
                        item {
                            Text(
                                text       = "Recent Notifications",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier   = Modifier.padding(
                                    start  = 4.dp,
                                    top    = 8.dp,
                                    bottom = 4.dp
                                )
                            )
                        }
                    }

                    items(
                        items = uiState.notifications,
                        key   = { it.notificationId.ifEmpty { it.hashCode().toString() } }
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
                                viewModel.deleteNotification(notification.notificationId, currentUserId)
                            },
                            onClick  = {
                                viewModel.markAsRead(notification.notificationId, currentUserId)

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
                                        if (notification.referenceId.isNotEmpty()) {
                                            if (isAdmin) navController.navigate(Screen.ManageBookings.route)
                                            else navController.navigate(
                                                Screen.BookingDetails.createRoute(notification.referenceId)
                                            )
                                        }
                                    }

                                    // ══════════════════════════════════════════
                                    // ✦ FIX — NEW_MESSAGE notification click
                                    //
                                    // Problem tha:
                                    //   notification.referenceId = conversationId
                                    //   e.g. "abc123_xyz789"  (sorted UIDs joined by "_")
                                    //   Screen.Chat.createRoute() ko userId chahiye tha
                                    //   isliye "Owner" show ho raha tha aur wrong screen
                                    //
                                    // Fix:
                                    //   1. conversationId ko split karo "_" se
                                    //   2. currentUserId hata ke otherUserId nikalo
                                    //   3. Firestore se us user ka name fetch karo
                                    //   4. Screen.Chat pe navigate karo correct params ke saath
                                    //
                                    // Note: generateChatId() sorted UIDs ko "_" se join karta hai.
                                    // UIDs mein khud underscore nahi hota (Firebase UID format),
                                    // isliye simple split("_") safe hai.
                                    // ══════════════════════════════════════════
                                    NotificationType.NEW_MESSAGE -> {
                                        val conversationId = notification.referenceId
                                        if (conversationId.isNotEmpty() && currentUserId.isNotEmpty()) {

                                            // Step 1: Extract otherUserId from conversationId
                                            // conversationId = sorted(uid1, uid2).joinToString("_")
                                            val parts = conversationId.split("_")
                                            val otherUserId = parts.firstOrNull { it != currentUserId } ?: ""

                                            if (otherUserId.isNotEmpty()) {
                                                coroutineScope.launch {
                                                    isNavigatingToChat = true
                                                    try {
                                                        // Step 2: Fetch other user's display name
                                                        val firestore = FirebaseFirestore.getInstance()
                                                        val doc = firestore
                                                            .collection("users")
                                                            .document(otherUserId)
                                                            .get()
                                                            .await()

                                                        val senderName = if (doc.exists()) {
                                                            doc.getString("name")
                                                                ?: doc.getString("fullName")
                                                                ?: doc.getString("displayName")
                                                                ?: "User"
                                                        } else {
                                                            // Fallback: query by userId field
                                                            val q = firestore
                                                                .collection("users")
                                                                .whereEqualTo("userId", otherUserId)
                                                                .limit(1)
                                                                .get()
                                                                .await()
                                                            q.documents.firstOrNull()?.let {
                                                                it.getString("name")
                                                                    ?: it.getString("fullName")
                                                                    ?: "User"
                                                            } ?: "User"
                                                        }

                                                        // Step 3: Navigate to ChatScreen
                                                        navController.navigate(
                                                            Screen.Chat.createRoute(
                                                                userId    = otherUserId,
                                                                ownerName = senderName
                                                            )
                                                        )
                                                    } catch (e: Exception) {
                                                        // Fallback: navigate with just userId, no name
                                                        navController.navigate(
                                                            Screen.Chat.createRoute(
                                                                userId    = otherUserId,
                                                                ownerName = "User"
                                                            )
                                                        )
                                                    } finally {
                                                        isNavigatingToChat = false
                                                    }
                                                }
                                            } else {
                                                // conversationId se userId nahi mila —
                                                // fallback: MessageList screen pe jao
                                                navController.navigate(Screen.MessageList.route)
                                            }
                                        }
                                    }
                                    // ══════════════════════════════════════════

                                    NotificationType.NEW_REVIEW -> {
                                        if (isAdmin) navController.navigate(Screen.GlobalReviews.route)
                                        else if (notification.referenceId.isNotEmpty()) {
                                            navController.navigate(
                                                Screen.ViewReviews.createRoute(notification.referenceId)
                                            )
                                        }
                                    }

                                    NotificationType.REVIEW_REPLY -> {
                                        if (notification.referenceId.isNotEmpty()) {
                                            navController.navigate(
                                                Screen.ViewReviews.createRoute(notification.referenceId)
                                            )
                                        }
                                    }

                                    else -> { /* No navigation */ }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SeasonalAlertsSection — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SeasonalAlertsSection(alerts: List<SeasonalAlert>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier              = Modifier.padding(start = 4.dp, bottom = 2.dp)
        ) {
            Icon(
                Icons.Default.Celebration,
                contentDescription = null,
                tint               = Color(0xFF9B7D2E),
                modifier           = Modifier.size(16.dp)
            )
            Text(
                text       = "Seasonal Alerts",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF9B7D2E)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF9B7D2E).copy(0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text       = "${alerts.size}",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF9B7D2E)
                )
            }
        }
        alerts.forEach { alert ->
            SeasonalAlertCard(alert = alert)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SeasonalAlertCard — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SeasonalAlertCard(alert: SeasonalAlert) {
    val seasonalGold   = Color(0xFF9B7D2E)
    val seasonalGoldBg = Color(0xFFFFF8E1)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = seasonalGoldBg),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = BorderStroke(1.5.dp, seasonalGold.copy(0.4f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(seasonalGold)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(seasonalGold.copy(0.15f))
                    .border(1.dp, seasonalGold.copy(0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = alert.iconEmoji.ifEmpty { "🎉" }, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text       = alert.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = Color(0xFF4A3800),
                        modifier   = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    if (alert.season.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(seasonalGold.copy(0.2f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text       = alert.season,
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color      = seasonalGold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = alert.message,
                    fontSize   = 12.sp,
                    color      = Color(0xFF5C4800).copy(0.8f),
                    maxLines   = 3,
                    lineHeight = 17.sp
                )
                if (alert.targetRole == "both") {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.PeopleAlt,
                            null,
                            modifier = Modifier.size(10.dp),
                            tint     = seasonalGold.copy(0.7f)
                        )
                        Text(
                            text     = "For all users",
                            fontSize = 10.sp,
                            color    = seasonalGold.copy(0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NotificationCard — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NotificationCard(
    item    : Notification,
    enumType: NotificationType,
    onClick : () -> Unit,
    onDelete: () -> Unit
) {
    val notifColor       = screenNotificationColor(enumType)
    val notifIcon        = screenNotificationIcon(enumType)
    val isUnread         = !item.isRead
    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isUnread) surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (isUnread) 2.dp else 0.dp),
        border    = BorderStroke(
            width = if (isUnread) 1.5.dp else 1.dp,
            color = if (isUnread) primary.copy(0.25f) else tertiary.copy(0.25f)
        )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(primary)
                )
            } else {
                Spacer(Modifier.width(3.dp))
            }

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
                        color      = onSurface,
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
                        color    = onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text       = item.body,
                    fontSize   = 12.sp,
                    color      = if (isUnread) onSurface.copy(0.75f) else onSurfaceVariant,
                    maxLines   = 2,
                    lineHeight = 17.sp
                )
                if (item.adminNote.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    val noteColor = when (enumType) {
                        NotificationType.PROPERTY_APPROVED -> SuccessGreen
                        NotificationType.PROPERTY_REJECTED -> MaterialTheme.colorScheme.error
                        else                               -> MaterialTheme.colorScheme.tertiary
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(noteColor.copy(0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            modifier = Modifier.size(11.dp),
                            tint     = noteColor
                        )
                        Text(
                            text     = item.adminNote,
                            fontSize = 11.sp,
                            color    = noteColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tertiary)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                    Icon(
                        Icons.Default.Close,
                        "Delete",
                        tint     = onSurfaceVariant.copy(0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Icon + Color helpers — unchanged
// ─────────────────────────────────────────────────────────────────────────────
fun screenNotificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.BOOKING_REQUESTED,
    NotificationType.BOOKING_CONFIRMED,
    NotificationType.BOOKING_CANCELLED,
    NotificationType.BOOKING_COMPLETED,
    NotificationType.BOOKING_REMINDER          -> Icons.Default.CalendarToday
    NotificationType.PAYMENT_RECEIVED,
    NotificationType.PAYMENT_FAILED,
    NotificationType.REFUND_ISSUED             -> Icons.Default.Payment
    NotificationType.NEW_MESSAGE               -> Icons.AutoMirrored.Filled.Message
    NotificationType.PROPERTY_APPROVED         -> Icons.Default.CheckCircle
    NotificationType.PROPERTY_REJECTED         -> Icons.Default.Cancel
    NotificationType.PROPERTY_PENDING          -> Icons.Default.HourglassEmpty
    NotificationType.USER_VERIFIED             -> Icons.Default.VerifiedUser
    NotificationType.USER_REJECTED             -> Icons.Default.PersonOff
    NotificationType.USER_VERIFICATION_PENDING -> Icons.Default.PersonSearch
    NotificationType.NEW_REVIEW                -> Icons.Default.Star
    NotificationType.REVIEW_REPLY              -> Icons.Default.Reply
    else                                       -> Icons.Default.Notifications
}

fun screenNotificationColor(type: NotificationType): Color = when (type) {
    NotificationType.BOOKING_REQUESTED -> Color(0xFF1B2B5B)
    NotificationType.BOOKING_CONFIRMED -> Color(0xFF2ECC71)
    NotificationType.BOOKING_CANCELLED -> Color(0xFFBA1A1A)
    NotificationType.BOOKING_COMPLETED -> Color(0xFF00897B)
    NotificationType.BOOKING_REMINDER -> Color(0xFF9B7D2E)
    NotificationType.PAYMENT_RECEIVED -> Color(0xFF2ECC71)
    NotificationType.PAYMENT_FAILED -> Color(0xFFBA1A1A)
    NotificationType.REFUND_ISSUED -> Color(0xFFFF6F00)
    NotificationType.NEW_MESSAGE -> Color(0xFF2E4A9E)
    NotificationType.PROPERTY_APPROVED -> Color(0xFF2ECC71)
    NotificationType.PROPERTY_REJECTED -> Color(0xFFBA1A1A)
    NotificationType.PROPERTY_PENDING -> Color(0xFFFF6F00)
    NotificationType.USER_VERIFIED -> Color(0xFF2ECC71)
    NotificationType.USER_REJECTED -> Color(0xFFBA1A1A)
    NotificationType.NEW_REVIEW -> Color(0xFF9B7D2E)
    NotificationType.REVIEW_REPLY -> Color(0xFF2E4A9E)
    else -> Color(0xFF9B7D2E)
}