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

// ✅ Missing colors defined locally for safety
private val SurfaceGray = Color(0xFFF2F2F2)
private val TextHint = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    userId: String = "",          // ✅ Default value added to fix NavGraph error
    currentUserId: String = "",   // ✅ Default value added
    chatId: String = "",          // ✅ Default value added
    viewModel: MessagingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Initialize chat logic
    LaunchedEffect(chatId, currentUserId) {
        if (chatId.isNotEmpty() && currentUserId.isNotEmpty()) {
            viewModel.initUserId(currentUserId)
            viewModel.listenToMessages(chatId, currentUserId)
        }
    }

    Scaffold(
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
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Handle attachment */ }) {
                        Icon(Icons.Default.AttachFile, null, tint = Color.Gray)
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type here...", color = TextHint) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceGray,
                            unfocusedContainerColor = SurfaceGray
                        )
                    )

                    IconButton(onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(userId, messageText.trim())
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
            if (uiState.messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages yet", color = TextHint)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        // Basic message bubble (Aap ise mazeed customize kar sakte hain)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.senderId == currentUserId) PrimaryBlue else Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp).align(Alignment.Center)
                        ) {
                            Text(
                                text = message.content,
                                modifier = Modifier.padding(12.dp),
                                color = if (message.senderId == currentUserId) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}