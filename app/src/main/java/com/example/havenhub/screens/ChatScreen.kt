package com.example.havenhub.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.components.MessageBubble
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.MessagingViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

// ✅ FIX 1: SurfaceGray — proper light gray (was almost transparent before)
private val SurfaceGray      = Color(0xFFF0F0F0)   // solid light gray background

// ✅ FIX 2: TextField background — slightly white so text is clearly visible
private val TextFieldBg      = Color(0xFFFFFFFF)   // white input field

// ✅ FIX 3: Text colors — dark on light background
private val TextDark         = Color(0xFF1A1A1A)   // typed text color
private val TextHint         = Color(0xFF9E9E9E)   // placeholder color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController : NavController,
    userId        : String = "",
    ownerName     : String = "Owner",
    propertyId    : String = "",
    currentUserId : String = "",
    chatId        : String = "",
    viewModel     : MessagingViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState   = rememberLazyListState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    val resolvedCurrentUserId = remember(currentUserId) {
        currentUserId.ifEmpty {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
    }

    LaunchedEffect(userId, resolvedCurrentUserId) {
        if (resolvedCurrentUserId.isNotEmpty() && userId.isNotEmpty()) {
            viewModel.initUserId(resolvedCurrentUserId)
            viewModel.loadChat(otherUserId = userId, propertyId = propertyId)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            snackbarHostState.showSnackbar("Messages delete ho gaye ✓")
            viewModel.resetDeleteSuccess()
        }
    }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text  = "Messages Delete Karen?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                val count = uiState.selectedMessageIds.size
                Text(
                    text  = "$count message${if (count > 1) "s" else ""} delete ho jaenge.\nSirf apne bheje huye messages delete honge.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSelectedMessages()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text  = "${uiState.selectedMessageIds.size} selected",
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "Cancel selection",
                                tint               = Color.White
                            )
                        }
                    },
                    actions = {
                        val allSelected = uiState.messages.isNotEmpty() &&
                                uiState.selectedMessageIds.size == uiState.messages.size

                        TextButton(
                            onClick = {
                                if (allSelected) viewModel.clearSelection()
                                else viewModel.selectAllMessages()
                            }
                        ) {
                            Text(
                                text       = if (allSelected) "Deselect All" else "Select All",
                                color      = Color.White,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        AnimatedVisibility(
                            visible = uiState.selectedMessageIds.isNotEmpty(),
                            enter   = fadeIn(),
                            exit    = fadeOut()
                        ) {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !uiState.isDeleting
                            ) {
                                if (uiState.isDeleting) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(20.dp),
                                        color       = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector        = Icons.Default.Delete,
                                        contentDescription = "Delete selected messages",
                                        tint               = Color.White
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text  = ownerName.ifEmpty { "Chat" },
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
                )
            }
        },
        bottomBar = {
            if (!uiState.isSelectionMode) {
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
                            // ✅ FIX: Text color explicitly set to dark so it's visible
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor     = TextFieldBg,
                                unfocusedContainerColor   = TextFieldBg,
                                focusedTextColor          = TextDark,       // ← typed text dark
                                unfocusedTextColor        = TextDark,       // ← typed text dark
                                focusedBorderColor        = PrimaryBlue,
                                unfocusedBorderColor      = Color(0xFFCCCCCC),
                                cursorColor               = PrimaryBlue
                            )
                        )

                        IconButton(onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(
                                    receiverId = userId,
                                    content    = messageText.trim(),
                                    propertyId = propertyId
                                )
                                messageText = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = PrimaryBlue)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // ✅ FIX: Proper gray background — was transparent before
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
                        state               = listState,
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key   = { it.id }
                        ) { message ->
                            val isMe = message.senderId == resolvedCurrentUserId

                            MessageBubble(
                                message         = message.content,
                                timestamp       = formatTimestamp(message.timestamp),
                                isSentByMe      = isMe,
                                isRead          = message.isRead,
                                isSelected      = message.isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onLongPress     = { viewModel.onMessageLongPress(message.id) },
                                onTap           = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.onMessageTap(message.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
}