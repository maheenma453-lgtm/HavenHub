package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.MessagingViewModel

// ─────────────────────────────────────────────────────────────────
// MessageListScreen.kt
// PURPOSE : Shows all active conversations for the current user.
//           Each conversation shows: contact name, last message,
//           timestamp, and unread count badge.
//           Owners see chats from renters; users see chats from owners.
// NAVIGATION: MessageListScreen → ChatScreen (on conversation tap)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController : NavController,
    viewModel     : MessagingViewModel = hiltViewModel()
) {

    // ── Load conversations on screen open ─────────────────────────
    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    val conversations by viewModel.conversations.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()

    // ── UI ────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = BackgroundWhite)
                    }
                },
                // New message / compose button
                actions = {
                    IconButton(onClick = { /* open contact picker */ }) {
                        Icon(Icons.Default.Edit, "New Message", tint = BackgroundWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = PrimaryBlue,
                    titleContentColor          = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->

        if (isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (conversations.isEmpty()) {

            // ── Empty State ───────────────────────────────────────
            Box(
                modifier         = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💬", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text       = "No Messages Yet",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text     = "Start a conversation by messaging a property owner",
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                }
            }

        } else {

            // ── Conversations List ────────────────────────────────
            LazyColumn(
                modifier       = Modifier
                    .fillMaxSize()
                    .background(BackgroundWhite)
                    .padding(paddingValues)
            ) {
                items(conversations) { convo ->
                    ConversationItem(
                        avatarInitial = convo.otherUserName.first().uppercaseChar().toString(),
                        name          = convo.otherUserName,
                        lastMessage   = convo.lastMessage,
                        timestamp     = convo.lastMessageTime,
                        unreadCount   = convo.unreadCount,
                        isOnline      = convo.isOnline,
                        onClick       = {
                            navController.navigate(
                                Screen.Chat.createRoute(convo.otherUserId)
                            )
                        }
                    )
                    Divider(
                        modifier  = Modifier.padding(start = 80.dp),
                        color     = BorderGray,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// ConversationItem
// A single conversation row with avatar, name, last msg, time, badge
// ─────────────────────────────────────────────────────────────────
@Composable
private fun ConversationItem(
    avatarInitial : String,
    name          : String,
    lastMessage   : String,
    timestamp     : String,
    unreadCount   : Int,
    isOnline      : Boolean,
    onClick       : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ── Avatar with online indicator ──────────────────────────
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = avatarInitial,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = BackgroundWhite
                )
            }
            // Green online dot (bottom-right of avatar)
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(BackgroundWhite)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(StatusAvailable)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Name + Last message ───────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = name,
                fontSize   = 15.sp,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                color      = TextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text       = lastMessage,
                fontSize   = 13.sp,
                color      = if (unreadCount > 0) TextPrimary else TextSecondary,
                fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // ── Timestamp + Unread badge ──────────────────────────────
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text     = timestamp,
                fontSize = 11.sp,
                color    = if (unreadCount > 0) PrimaryBlue else TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (unreadCount > 0) {
                Box(
                    modifier         = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = if (unreadCount > 9) "9+" else "$unreadCount",
                        fontSize = 10.sp,
                        color    = BackgroundWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


