package com.example.havenhub.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.components.MessageBubble
import com.example.havenhub.components.MessageDateDivider
import com.example.havenhub.data.Message
import com.example.havenhub.viewmodel.MessagingViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

// ── HavenHub Brand Colors ─────────────────────────────────────────────────────
private val HavenNavy      = Color(0xFF1A2744)
private val HavenNavyLight = Color(0xFF2D3F6B)
private val HavenGold      = Color(0xFFC9973A)
private val HavenGoldLight = Color(0xFFE8B84B)

// ── Date grouping helper ──────────────────────────────────────────────────────
private data class ChatListItem(
    val type   : Type,
    val message: Message? = null,
    val label  : String   = ""
) {
    enum class Type { DATE_HEADER, MESSAGE }
}

private fun groupMessagesWithDateHeaders(messages: List<Message>): List<ChatListItem> {
    val result   = mutableListOf<ChatListItem>()
    var lastDate = ""
    val todayCal = Calendar.getInstance()
    val yestrCal = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
    val dateFmt  = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val dispFmt  = SimpleDateFormat("MMM d", Locale.getDefault())

    val todayKey = dateFmt.format(todayCal.time)
    val yestrKey = dateFmt.format(yestrCal.time)

    for (msg in messages) {
        val msgDate = if (msg.timestamp > 0L) dateFmt.format(Date(msg.timestamp)) else ""
        if (msgDate.isNotEmpty() && msgDate != lastDate) {
            val label = when (msgDate) {
                todayKey -> "Today"
                yestrKey -> "Yesterday"
                else     -> if (msg.timestamp > 0L) dispFmt.format(Date(msg.timestamp)) else ""
            }
            if (label.isNotEmpty()) {
                result.add(ChatListItem(type = ChatListItem.Type.DATE_HEADER, label = label))
            }
            lastDate = msgDate
        }
        result.add(ChatListItem(type = ChatListItem.Type.MESSAGE, message = msg))
    }
    return result
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

// ── Last Seen format helper ───────────────────────────────────────────────────
private fun formatLastSeen(lastSeen: Long): String {
    if (lastSeen == 0L) return ""
    val now     = System.currentTimeMillis()
    val diff    = now - lastSeen
    val minutes = diff / 60_000
    val hours   = diff / 3_600_000

    return when {
        diff < 60_000          -> "last seen just now"
        minutes < 60           -> "last seen $minutes min ago"
        hours < 24             -> "last seen at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastSeen))}"
        else                   -> "last seen ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(lastSeen))}"
    }
}

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
    val isDark      = isSystemInDarkTheme()

    // Theme-aware colors — HavenHub Navy+Gold palette
    val screenBg    = if (isDark) Color(0xFF0F1828) else Color(0xFFF5F5F5)
    val bottomBarBg = if (isDark) Color(0xFF1A2133) else Color.White
    val fieldBg     = if (isDark) Color(0xFF243042) else Color.White
    val typedText   = if (isDark) Color(0xFFECECEC) else Color(0xFF1A1A1A)
    val hintText    = if (isDark) Color(0xFF6B7A99) else Color(0xFF9E9E9E)
    val attachTint  = if (isDark) Color(0xFF6B7A99) else Color(0xFF9E9E9E)
    val emptyMsgCol = if (isDark) Color(0xFF6B7A99) else Color(0xFF9E9E9E)

    var showDeleteDialog by remember { mutableStateOf(false) }

    val resolvedCurrentUserId = remember(currentUserId) {
        currentUserId.ifEmpty { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    }

    LaunchedEffect(userId, resolvedCurrentUserId) {
        if (resolvedCurrentUserId.isNotEmpty() && userId.isNotEmpty()) {
            viewModel.initUserId(resolvedCurrentUserId)
            viewModel.loadChat(otherUserId = userId, propertyId = propertyId)
        }
    }

    // ✦ Other user ki info uiState se
    val otherUser        = uiState.otherUserProfile
    val isOtherOnline    = uiState.isOtherUserOnline
    val otherLastSeen    = uiState.otherUserLastSeen
    val otherUserRole    = otherUser?.role ?: ""

    // ✦ My own role (current logged-in user ka role)
    // Isse determine hoga ke meri bubbles ka color kya hoga
    // Note: Tumhare pass currentUser ka role bhi chahiye hoga
    // Agar AuthViewModel se milta hai toh wahan se lena
    // Abhi hum otherUser ke opposite role assume karein ge:
    //   agar other = landlord → main = tenant, vice versa
    val myRole: String = remember(otherUserRole) {
        when (otherUserRole.lowercase().trim()) {
            "landlord" -> "tenant"
            "tenant"   -> "landlord"
            else       -> "tenant"
        }
    }

    val chatItems = remember(uiState.messages) { groupMessagesWithDateHeaders(uiState.messages) }
    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty()) listState.animateScrollToItem(chatItems.size - 1)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            snackbarHostState.showSnackbar("Messages delete ho gaye ✓")
            viewModel.resetDeleteSuccess()
        }
    }

    BackHandler(enabled = uiState.isSelectionMode) { viewModel.clearSelection() }

    // ── Delete Confirmation Dialog ─────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = if (isDark) Color(0xFF1E2A3A) else Color.White,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Delete Messages?", fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Unspecified)
            },
            text = {
                val count = uiState.selectedMessageIds.size
                Text(
                    "$count message${if (count > 1) "s" else ""} permanently delete ho jaenge.\nSirf apne messages delete ho sakte hain.",
                    color = if (isDark) Color(0xFFAFB8CC) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.deleteSelectedMessages() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                // Selection mode TopBar
                TopAppBar(
                    title = {
                        Text(
                            "${uiState.selectedMessageIds.size} selected",
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    },
                    actions = {
                        val allSelected = uiState.messages.isNotEmpty() &&
                                uiState.selectedMessageIds.size == uiState.messages.size
                        TextButton(onClick = {
                            if (allSelected) viewModel.clearSelection()
                            else viewModel.selectAllMessages()
                        }) {
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
                                if (uiState.isDeleting)
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(20.dp),
                                        color       = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                else Icon(Icons.Default.Delete, null, tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                )
            } else {
                // ✦ Normal TopBar — Profile pic + Name + Online/Last seen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp)
                        .background(Brush.horizontalGradient(listOf(HavenNavy, HavenNavyLight)))
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment    = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // ✦ Profile Picture
                                Box(
                                    modifier        = Modifier.size(38.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    val profileUrl = otherUser?.profileImageUrl ?: ""
                                    if (profileUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model               = profileUrl,
                                            contentDescription  = "Profile",
                                            contentScale        = ContentScale.Crop,
                                            modifier            = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(HavenNavyLight)
                                        )
                                    } else {
                                        // Placeholder initials avatar
                                        Box(
                                            modifier        = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(HavenGold, HavenGoldLight)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val initials = otherUser?.initials
                                                ?: ownerName.trim().split(" ")
                                                    .filter { it.isNotEmpty() }
                                                    .take(2)
                                                    .joinToString("") { it.first().uppercaseChar().toString() }
                                            if (initials.isNotEmpty()) {
                                                Text(
                                                    text       = initials,
                                                    color      = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize   = 14.sp
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint               = Color.White,
                                                    modifier           = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    // ✦ Online green dot
                                    if (isOtherOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))  // green
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }

                                // ✦ Name + Online/Last seen
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text       = ownerName.ifEmpty { otherUser?.fullName ?: "Chat" },
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 16.sp,
                                        maxLines   = 1
                                    )
                                    // Online status line
                                    val statusText = when {
                                        isOtherOnline            -> "Online"
                                        otherLastSeen > 0L       -> formatLastSeen(otherLastSeen)
                                        else                     -> ""
                                    }
                                    if (statusText.isNotEmpty()) {
                                        Text(
                                            text     = statusText,
                                            color    = if (isOtherOnline) Color(0xFF81C784) // light green
                                            else Color.White.copy(alpha = 0.60f),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        },
        bottomBar = {
            if (!uiState.isSelectionMode) {
                Surface(shadowElevation = 10.dp, color = bottomBarBg) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Attachment */ }) {
                            Icon(Icons.Default.AttachFile, null, tint = attachTint)
                        }

                        OutlinedTextField(
                            value         = messageText,
                            onValueChange = { messageText = it },
                            modifier      = Modifier.weight(1f),
                            placeholder   = {
                                Text("Type a message...", color = hintText, fontSize = 14.sp)
                            },
                            shape    = RoundedCornerShape(26.dp),
                            maxLines = 4,
                            colors   = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor   = fieldBg,
                                unfocusedContainerColor = fieldBg,
                                focusedTextColor        = typedText,
                                unfocusedTextColor      = typedText,
                                focusedBorderColor      = HavenGold,
                                unfocusedBorderColor    = if (isDark) Color(0xFF2D3F6B) else Color(0xFFDDDDDD),
                                cursorColor             = HavenGold
                            )
                        )

                        Spacer(Modifier.width(4.dp))

                        // Send button — Gold circle
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    brush = if (messageText.isNotBlank())
                                        Brush.linearGradient(listOf(HavenGold, HavenGoldLight))
                                    else
                                        Brush.linearGradient(listOf(Color(0xFF4A4A4A), Color(0xFF4A4A4A))),
                                    shape = RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendMessage(
                                            receiverId = userId,
                                            content    = messageText.trim(),
                                            propertyId = propertyId
                                        )
                                        messageText = ""
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                .background(screenBg)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier    = Modifier.align(Alignment.Center),
                        color       = HavenGold,
                        strokeWidth = 3.dp
                    )
                }

                uiState.messages.isEmpty() -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👋", fontSize = 44.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No messages yet",
                            color      = emptyMsgCol,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 16.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Say hello to ${ownerName.ifEmpty { "them" }}!",
                            color    = emptyMsgCol,
                            fontSize = 13.sp
                        )
                    }
                }

                else -> {
                    // ── Messages with date dividers ────────────────────────────
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(
                            items = chatItems,
                            key   = { item ->
                                when (item.type) {
                                    ChatListItem.Type.DATE_HEADER -> "header_${item.label}"
                                    ChatListItem.Type.MESSAGE     -> item.message?.id ?: UUID.randomUUID().toString()
                                }
                            }
                        ) { item ->
                            when (item.type) {
                                ChatListItem.Type.DATE_HEADER -> {
                                    MessageDateDivider(date = item.label)
                                }
                                ChatListItem.Type.MESSAGE -> {
                                    val message = item.message ?: return@items
                                    val isMe = message.senderId == resolvedCurrentUserId

                                    // ✦ Role determine karo:
                                    //   agar isMe → myRole use karo
                                    //   agar other → otherUserRole use karo
                                    val bubbleSenderRole = if (isMe) myRole else otherUserRole

                                    MessageBubble(
                                        message         = message.content,
                                        timestamp       = formatTimestamp(message.timestamp),
                                        isSentByMe      = isMe,
                                        senderRole      = bubbleSenderRole,  // ✦ NEW
                                        isRead          = message.isRead,
                                        isSelected      = message.isSelected,
                                        isSelectionMode = uiState.isSelectionMode,
                                        onLongPress     = { viewModel.onMessageLongPress(message.id) },
                                        onTap           = {
                                            if (uiState.isSelectionMode)
                                                viewModel.onMessageTap(message.id)
                                        }
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











