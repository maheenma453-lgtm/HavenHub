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
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    navController   : NavController,
    notificationId  : String,
    viewModel       : NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId  = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Notification dhundo list mein se
    val notification = uiState.notifications.find { it.notificationId == notificationId }

    // Mark as read jab screen khule
    LaunchedEffect(notificationId) {
        if (notificationId.isNotEmpty()) {
            viewModel.markAsRead(notificationId, userId)
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

        if (notification == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Notification not found.", color = TextSecondary)
            }
            return@Scaffold
        }

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

            // Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(notificationColor(notification.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = notificationIcon(notification.type),
                    contentDescription = null,
                    tint     = notificationColor(notification.type),
                    modifier = Modifier.size(44.dp)
                )
            }

            // Type Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(notificationColor(notification.type).copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = notification.type.displayName(),
                    color      = notificationColor(notification.type),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Title
            Text(
                text       = notification.title,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = TextPrimary
            )

            // Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                Spacer(Modifier.width(4.dp))
                Text(
                    text     = notification.createdAt?.toDate()?.toString() ?: "-",
                    fontSize = 13.sp,
                    color    = TextSecondary
                )
            }

            HorizontalDivider(color = BorderGray)

            // Message Body
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Text(
                    text      = notification.body,
                    modifier  = Modifier.padding(16.dp),
                    fontSize  = 15.sp,
                    lineHeight = 24.sp,
                    color     = TextPrimary
                )
            }

            // Action Button
            NotificationActionButton(
                type    = notification.type,
                onClick = {
                    when (notification.type) {
                        NotificationType.BOOKING_REQUESTED,
                        NotificationType.BOOKING_CONFIRMED,
                        NotificationType.BOOKING_CANCELLED,
                        NotificationType.BOOKING_COMPLETED,
                        NotificationType.BOOKING_REMINDER -> {
                            if (notification.referenceId.isNotEmpty()) {
                                navController.navigate(Screen.BookingDetails.createRoute(notification.referenceId))
                            }
                        }
                        NotificationType.PAYMENT_RECEIVED,
                        NotificationType.PAYMENT_FAILED,
                        NotificationType.REFUND_ISSUED -> {
                            if (notification.referenceId.isNotEmpty()) {
                                navController.navigate(Screen.PaymentSuccess.createRoute(notification.referenceId))
                            }
                        }
                        NotificationType.NEW_MESSAGE -> {
                            if (notification.referenceId.isNotEmpty()) {
                                navController.navigate(Screen.Chat.createRoute(notification.referenceId))
                            }
                        }
                        else -> { }
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

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
        colors   = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}