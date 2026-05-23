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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
// NotificationDetailScreen.kt  —  FIXED
//
// Fix: NEW_MESSAGE action button
//   Before: Screen.Chat.createRoute(notification.referenceId)
//           referenceId = conversationId — wrong! Chat expects userId
//   After:  Split conversationId → find otherUserId → fetch name → navigate
//
// Fix: PAYMENT_* action button
//   Before: empty block { }
//   After:  navigate to BookingDetails (referenceId = bookingId)
//
// Fix: USER_VERIFIED / USER_REJECTED action button
//   Before: no case
//   After:  navigate to Profile screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    navController : NavController,
    notificationId: String,
    viewModel     : NotificationViewModel = hiltViewModel()
) {
    val uiState   = viewModel.uiState.collectAsState().value
    val userId    = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val notification = uiState.notifications.find { it.notificationId == notificationId }

    // For async sender-name fetch before navigating to chat
    var isNavigatingToChat by remember { mutableStateOf(false) }
    val coroutineScope     = rememberCoroutineScope()

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

    // Loading overlay
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

        val enumType = try {
            NotificationType.valueOf(notification.type)
        } catch (e: Exception) {
            NotificationType.GENERAL
        }

        val notifColor = getNotificationColor(enumType)

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

            // ── Notification type icon ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
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
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
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
                text       = notification.title,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = onSurface
            )

            // ── Timestamp ─────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint     = onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text     = notification.createdAt?.toString() ?: "-",
                    fontSize = 13.sp,
                    color    = onSurfaceVariant
                )
            }

            HorizontalDivider(color = outline)

            // ── Message body ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text       = notification.body,
                    modifier   = Modifier.padding(16.dp),
                    fontSize   = 15.sp,
                    lineHeight = 24.sp,
                    color      = onSurface
                )
            }

            // ── Admin Note (if any) ───────────────────────────────────────────
            if (notification.adminNote.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = notifColor.copy(0.08f)
                    )
                ) {
                    Row(
                        modifier  = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint     = notifColor,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                "Admin Note",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = notifColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                notification.adminNote,
                                fontSize = 14.sp,
                                color    = notifColor
                            )
                        }
                    }
                }
            }

            // ── Action button ─────────────────────────────────────────────────
            // FIX: Every notification type now has a proper action
            NotificationActionButton(
                type    = enumType,
                onClick = {
                    when (enumType) {

                        NotificationType.BOOKING_REQUESTED,
                        NotificationType.BOOKING_CONFIRMED,
                        NotificationType.BOOKING_CANCELLED,
                        NotificationType.BOOKING_COMPLETED,
                        NotificationType.BOOKING_REMINDER -> {
                            if (notification.referenceId.isNotEmpty()) {
                                navController.navigate(
                                    Screen.BookingDetails.createRoute(notification.referenceId)
                                )
                            }
                        }

                        // FIX: Payment → BookingDetails (referenceId = bookingId)
                        NotificationType.PAYMENT_RECEIVED,
                        NotificationType.PAYMENT_FAILED,
                        NotificationType.REFUND_ISSUED -> {
                            if (notification.referenceId.isNotEmpty()) {
                                navController.navigate(
                                    Screen.BookingDetails.createRoute(notification.referenceId)
                                )
                            }
                        }

                        // FIX: NEW_MESSAGE → fetch name first, then navigate correctly
                        NotificationType.NEW_MESSAGE -> {
                            val conversationId = notification.referenceId
                            val currentUserId  = userId
                            if (conversationId.isNotEmpty() && currentUserId.isNotEmpty()) {
                                val parts       = conversationId.split("_")
                                val otherUserId = parts.firstOrNull { it != currentUserId } ?: ""

                                if (otherUserId.isNotEmpty()) {
                                    coroutineScope.launch {
                                        isNavigatingToChat = true
                                        try {
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
                                            } else "User"

                                            navController.navigate(
                                                Screen.Chat.createRoute(
                                                    userId    = otherUserId,
                                                    ownerName = senderName
                                                )
                                            )
                                        } catch (e: Exception) {
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
                                    navController.navigate(Screen.MessageList.route)
                                }
                            }
                        }

                        // FIX: Reviews
                        NotificationType.NEW_REVIEW,
                        NotificationType.REVIEW_REPLY -> {
                            if (notification.referenceId.isNotEmpty()) {
                                navController.navigate(
                                    Screen.ViewReviews.createRoute(notification.referenceId)
                                )
                            }
                        }

                        // FIX: User Verification → Profile
                        NotificationType.USER_VERIFIED,
                        NotificationType.USER_REJECTED,
                        NotificationType.ACCOUNT_VERIFIED -> {
                            navController.navigate(Screen.Profile.route)
                        }

                        NotificationType.USER_VERIFICATION_PENDING -> {
                            navController.navigate(Screen.VerifyUsers.route)
                        }

                        // FIX: Property Pending → VerifyProperties
                        NotificationType.PROPERTY_PENDING -> {
                            if (notification.referenceId.isNotEmpty()) {
                                navController.navigate(
                                    Screen.PropertyVerificationDetail.createRoute(
                                        notification.referenceId
                                    )
                                )
                            } else {
                                navController.navigate(Screen.VerifyProperties.route)
                            }
                        }

                        // Seasonal Alert → ManageSeasonalAlerts
                        NotificationType.SEASONAL_ALERT -> {
                            navController.navigate(Screen.ManageSeasonalAlerts.route)
                        }

                        else -> {
                            // GENERAL / unknown — bas popBackStack
                            navController.popBackStack()
                        }
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
    NotificationType.USER_VERIFIED,
    NotificationType.ACCOUNT_VERIFIED          -> Icons.Default.VerifiedUser
    NotificationType.USER_REJECTED             -> Icons.Default.PersonOff
    NotificationType.USER_VERIFICATION_PENDING -> Icons.Default.PersonSearch
    NotificationType.NEW_REVIEW,
    NotificationType.REVIEW_REPLY              -> Icons.Default.Star
    NotificationType.SEASONAL_ALERT            -> Icons.Default.Celebration
    NotificationType.ACCOUNT_SUSPENDED         -> Icons.Default.Block
    else                                       -> Icons.Default.Notifications
}

// ── Helper: get color for notification type ───────────────────────────────────
@Composable
fun getNotificationColor(type: NotificationType): Color = when (type) {
    NotificationType.BOOKING_REQUESTED         -> Color(0xFF1B2B5B)
    NotificationType.BOOKING_CONFIRMED         -> Color(0xFF2ECC71)
    NotificationType.BOOKING_CANCELLED         -> MaterialTheme.colorScheme.error
    NotificationType.BOOKING_COMPLETED         -> Color(0xFF00897B)
    NotificationType.BOOKING_REMINDER          -> Color(0xFF9B7D2E)
    NotificationType.PAYMENT_RECEIVED          -> Color(0xFF4CAF50)
    NotificationType.PAYMENT_FAILED            -> MaterialTheme.colorScheme.error
    NotificationType.REFUND_ISSUED             -> Color(0xFFFF6F00)
    NotificationType.NEW_MESSAGE               -> Color(0xFF2E4A9E)
    NotificationType.PROPERTY_APPROVED         -> Color(0xFF2ECC71)
    NotificationType.PROPERTY_REJECTED         -> MaterialTheme.colorScheme.error
    NotificationType.PROPERTY_PENDING          -> Color(0xFFFF6F00)
    NotificationType.USER_VERIFIED,
    NotificationType.ACCOUNT_VERIFIED          -> Color(0xFF2ECC71)
    NotificationType.USER_REJECTED             -> MaterialTheme.colorScheme.error
    NotificationType.USER_VERIFICATION_PENDING -> Color(0xFFFF6F00)
    NotificationType.NEW_REVIEW,
    NotificationType.REVIEW_REPLY              -> Color(0xFF9B7D2E)
    NotificationType.SEASONAL_ALERT            -> Color(0xFF9B7D2E)
    NotificationType.ACCOUNT_SUSPENDED         -> MaterialTheme.colorScheme.error
    else                                       -> MaterialTheme.colorScheme.onSurfaceVariant
}

// ── Action button based on notification type ──────────────────────────────────
@Composable
fun NotificationActionButton(type: NotificationType, onClick: () -> Unit) {
    val (label, icon) = when (type) {
        NotificationType.BOOKING_REQUESTED,
        NotificationType.BOOKING_CONFIRMED,
        NotificationType.BOOKING_CANCELLED,
        NotificationType.BOOKING_COMPLETED,
        NotificationType.BOOKING_REMINDER -> "View Booking" to Icons.Default.CalendarToday

        NotificationType.PAYMENT_RECEIVED,
        NotificationType.PAYMENT_FAILED,
        NotificationType.REFUND_ISSUED -> "View Payment" to Icons.Default.Payment

        NotificationType.NEW_MESSAGE -> "Open Chat" to Icons.AutoMirrored.Filled.Message
        NotificationType.NEW_REVIEW,
        NotificationType.REVIEW_REPLY -> "View Reviews" to Icons.Default.Star

        NotificationType.PROPERTY_APPROVED,
        NotificationType.PROPERTY_REJECTED -> "View My Properties" to Icons.Default.Home

        NotificationType.PROPERTY_PENDING -> "Review Property" to Icons.Default.HourglassEmpty
        NotificationType.USER_VERIFIED,
        NotificationType.USER_REJECTED,
        NotificationType.ACCOUNT_VERIFIED -> "View Profile" to Icons.Default.Person

        NotificationType.USER_VERIFICATION_PENDING -> "Review Users" to Icons.Default.PersonSearch
        NotificationType.SEASONAL_ALERT -> "View Alerts" to Icons.Default.Celebration
        else -> "Close" to Icons.AutoMirrored.Filled.OpenInNew
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}