package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.MessagingViewModel
// ✅ FIX: FirebaseAuth import — currentUserId fallback ke liye
import com.google.firebase.auth.FirebaseAuth

private val SurfaceGray = Color(0xFFF2F2F2)
private val TextHint    = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController : NavController,
    userId        : String = "",         // landlord/tenant ka ID (other user)
    currentUserId : String = "",         // current logged-in user ka ID
    chatId        : String = "",         // optional — abhi use nahi ho raha
    viewModel     : MessagingViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    var messageText  by remember { mutableStateOf("") }
    val listState    = rememberLazyListState()

    // ✅ FIX: Agar NavGraph se currentUserId empty aaye to FirebaseAuth se lo
    val resolvedCurrentUserId = remember(currentUserId) {
        currentUserId.ifEmpty {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
    }

    // ✅ FIX: initUserId + loadChat — dono resolvedCurrentUserId se chalein
    LaunchedEffect(userId, resolvedCurrentUserId) {
        if (resolvedCurrentUserId.isNotEmpty() && userId.isNotEmpty()) {
            viewModel.initUserId(resolvedCurrentUserId)
            viewModel.loadChat(userId)
        }
    }

    // ✅ Auto-scroll to bottom jab naye messages aayein
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // ✅ Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Chat", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Attachment logic */ }) {
                        Icon(Icons.Default.AttachFile, null, tint = Color.Gray)
                    }

                    OutlinedTextField(
                        value         = messageText,
                        onValueChange = { messageText = it },
                        modifier      = Modifier.weight(1f),
                        placeholder   = { Text("Type here...", color = TextHint) },
                        shape         = RoundedCornerShape(24.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = SurfaceGray,
                            unfocusedContainerColor = SurfaceGray
                        )
                    )

                    // ✅ FIX: userId (otherUserId) ko receiverId pass karo
                    IconButton(onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(
                                receiverId = userId,
                                content    = messageText.trim()
                            )
                            messageText = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = PrimaryBlue)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceGray)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = PrimaryBlue
                    )
                }
                uiState.messages.isEmpty() -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No messages yet.\nSay hello! 👋", color = TextHint)
                    }
                }
                else -> {
                    LazyColumn(
                        state           = listState,
                        modifier        = Modifier.fillMaxSize(),
                        contentPadding  = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages) { message ->
                            // ✅ FIX: resolvedCurrentUserId se compare karo
                            val isMe = message.senderId == resolvedCurrentUserId

                            Column(
                                modifier            = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart    = 12.dp,
                                        topEnd      = 12.dp,
                                        bottomStart = if (isMe) 12.dp else 0.dp,
                                        bottomEnd   = if (isMe) 0.dp else 12.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) PrimaryBlue else Color.White
                                    )
                                ) {
                                    Text(
                                        text     = message.content,
                                        modifier = Modifier.padding(12.dp),
                                        color    = if (isMe) Color.White else Color.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}