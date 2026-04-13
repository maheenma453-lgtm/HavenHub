package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.MessagingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController : NavController,
    viewModel     : MessagingViewModel = hiltViewModel(),
    authViewModel : AuthViewModel      = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUid  = authUiState.currentUser?.uid ?: ""

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            viewModel.loadConversations(currentUid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Edit, "New Message")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = PrimaryBlue,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor     = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundWhite)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = PrimaryBlue
                    )
                }
                uiState.conversations.isEmpty() -> {
                    EmptyMessagesState()
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.conversations) { convo ->
                            val participants = (convo["participants"] as? List<*>)
                                ?: emptyList<Any>()
                            val otherUserId  = participants
                                .firstOrNull { it != currentUid }
                                ?.toString() ?: ""
                            val lastMessage  = (convo["lastMessage"] as? String) ?: ""
                            val timestamp    = (convo["lastMessageTimestamp"] as? Long) ?: 0L
                            val timeStr      = if (timestamp > 0) {
                                SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(Date(timestamp))
                            } else ""

                            ConversationItem(
                                avatarInitial = otherUserId.take(1).uppercase(),
                                name          = otherUserId.take(8) + "...",
                                lastMessage   = lastMessage.ifEmpty { "No messages yet" },
                                timestamp     = timeStr,
                                unreadCount   = 0,
                                isOnline      = false,
                                onClick       = {
                                    navController.navigate(
                                        Screen.Chat.createRoute(otherUserId)
                                    )
                                }
                            )
                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 80.dp),
                                color     = BorderGray,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessagesState() {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "💬", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text       = "No Messages Yet",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary
        )
        Text(
            text     = "Start a conversation with a landlord",
            fontSize = 13.sp,
            color    = TextSecondary
        )
    }
}

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
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    avatarInitial,
                    fontSize   = 20.sp,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isOnline) {
                Surface(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd),
                    shape  = CircleShape,
                    color  = Color.White,
                    border = null
                ) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontSize   = 15.sp,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                lastMessage,
                fontSize = 13.sp,
                color    = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                timestamp,
                fontSize = 11.sp,
                color    = if (unreadCount > 0) PrimaryBlue else TextSecondary
            )
            if (unreadCount > 0) {
                Badge(
                    containerColor = PrimaryBlue,
                    contentColor   = Color.White
                ) {
                    Text("$unreadCount")
                }
            }
        }
    }
}