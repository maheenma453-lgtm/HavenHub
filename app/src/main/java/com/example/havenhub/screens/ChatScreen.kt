package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.MessagingViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// ChatScreen.kt
// PURPOSE : Real-time one-to-one chat between renter and owner.
//           Messages are loaded from Firebase Realtime Database.
//           Sent messages appear on right (blue), received on left (gray).
//           Supports text messages and image attachments.
// PARAMETERS: userId (the other person's UID)
// NAVIGATION: ChatScreen → (back to MessageListScreen)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController : NavController,
    userId        : String,                   // The other user's Firebase UID
    viewModel     : MessagingViewModel = hiltViewModel()
) {

    // ── Load messages & listen for real-time updates ───────────────
    LaunchedEffect(userId) {
        viewModel.loadChat(userId)
        viewModel.markAsRead(userId)           // Mark all messages as read
    }

    val messages    by viewModel.messages.collectAsState()
    val otherUser   by viewModel.otherUser.collectAsState()
    val isOnline    by viewModel.isOtherUserOnline.collectAsState()

    // ── Message input state ───────────────────────────────────────
    var messageText by remember { mutableStateOf("") }

    // Auto-scroll to bottom when new messages arrive
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // ── UI ────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Contact name + online status
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // Avatar circle
                        Box(
                            modifier         = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = otherUser?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color      = BackgroundWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text     = otherUser?.name ?: "Loading...",
                                fontSize = 15.sp,
                                color    = BackgroundWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text     = if (isOnline) "Online" else "Offline",
                                fontSize = 11.sp,
                                color    = if (isOnline)
                                    StatusAvailable
                                else
                                    BackgroundWhite.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = BackgroundWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = PrimaryBlue,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        },

        // ── Message Input Bar (sticky at bottom) ──────────────────
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color           = BackgroundWhite
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {

                    // Attachment button
                    IconButton(onClick = { /* open file picker */ }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach",
                            tint = TextSecondary
                        )
                    }

                    // Text input field
                    OutlinedTextField(
                        value         = messageText,
                        onValueChange = { messageText = it },
                        placeholder   = { Text("Type a message...", color = TextHint) },
                        modifier      = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape         = RoundedCornerShape(24.dp),
                        maxLines      = 4,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PrimaryBlue,
                            unfocusedBorderColor = BorderGray,
                            focusedContainerColor   = SurfaceGray,
                            unfocusedContainerColor = SurfaceGray
                        )
                    )

                    // Send button — enabled only when message is not blank
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(
                                    receiverId = userId,
                                    text       = messageText.trim()
                                )
                                messageText = ""   // Clear input after sending
                                // Scroll to latest message
                                scope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageText.isNotBlank()) PrimaryBlue else BorderGray
                            )
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Send,
                            contentDescription = "Send",
                            tint               = BackgroundWhite,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        // ── Messages List ─────────────────────────────────────────
        LazyColumn(
            state          = listState,
            modifier       = Modifier
                .fillMaxSize()
                .background(SurfaceGray)
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    text      = message.text,
                    isMine    = message.isSentByMe,    // true = sent by current user
                    timestamp = message.time,
                    isRead    = message.isRead
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// MessageBubble
// Individual message bubble — right-aligned if sent, left if received
// ─────────────────────────────────────────────────────────────────
@Composable
private fun MessageBubble(
    text      : String,
    isMine    : Boolean,
    timestamp : String,
    isRead    : Boolean
) {
    // Layout direction based on sender
    val arrangement  = if (isMine) Arrangement.End else Arrangement.Start
    val bubbleColor  = if (isMine) PrimaryBlue else BackgroundWhite
    val textColor    = if (isMine) BackgroundWhite else TextPrimary
    val bubbleShape  = if (isMine)
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = arrangement
    ) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {

            // Bubble
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .widthIn(max = 270.dp)
            ) {
                Text(
                    text      = text,
                    fontSize  = 14.sp,
                    color     = textColor,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Timestamp + read receipt
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(text = timestamp, fontSize = 10.sp, color = TextSecondary)
                // Double tick for sent messages
                if (isMine) {
                    Text(
                        text  = if (isRead) "✓✓" else "✓",
                        fontSize = 10.sp,
                        color = if (isRead) AccentCyan else TextSecondary
                    )
                }
            }
        }
    }
}


