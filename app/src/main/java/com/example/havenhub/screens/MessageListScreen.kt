package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.MessagingViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────
// HavenHub Color Constants (matching app theme)
// ─────────────────────────────────────────────────────────────
private val HavenNavy        = Color(0xFF1A2A4A)
private val HavenGold        = Color(0xFFB8922A)
private val HavenGoldLight   = Color(0xFFD4A843)
private val HavenWhite       = Color(0xFFFFFFFF)
private val HavenSurface     = Color(0xFFF5F7FA)
private val HavenDivider     = Color(0xFFE8ECF0)
private val HavenTextPrimary   = Color(0xFF1A2A4A)
private val HavenTextSecondary = Color(0xFF6B7A8D)
private val HavenUnreadBg    = Color(0xFFFFF8E7)

// ─────────────────────────────────────────────────────────────
// Per-conversation local state
// ─────────────────────────────────────────────────────────────
private data class ConvoState(
    val isPinned  : Boolean = false,
    val isMuted   : Boolean = false,
    val isBlocked : Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController : NavController,
    viewModel     : MessagingViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val currentUid  = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var userRole  by remember { mutableStateOf("") }
    val userNames  = remember { mutableStateMapOf<String, String>() }
    val convoStates = remember { mutableStateMapOf<String, ConvoState>() }

    var deleteTargetConvoId by remember { mutableStateOf<String?>(null) }
    var deleteTargetName    by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUid) {
        if (currentUid.isEmpty()) return@LaunchedEffect
        try {
            val directDoc = FirebaseFirestore.getInstance()
                .collection("users").document(currentUid).get().await()
            userRole = if (directDoc.exists()) {
                directDoc.getString("role")?.trim() ?: ""
            } else {
                val query = FirebaseFirestore.getInstance()
                    .collection("users").whereEqualTo("userId", currentUid).limit(1).get().await()
                query.documents.firstOrNull()?.getString("role")?.trim() ?: ""
            }
        } catch (_: Exception) { userRole = "" }
        viewModel.loadConversations(currentUid)
    }

    LaunchedEffect(uiState.conversations) {
        uiState.conversations.forEach { convo ->
            val participants = (convo["participants"] as? List<*>) ?: emptyList<Any>()
            val otherUserId  = participants.firstOrNull { it.toString() != currentUid }?.toString() ?: ""

            if (otherUserId.isNotEmpty() && !userNames.containsKey(otherUserId)) {
                try {
                    val directDoc = FirebaseFirestore.getInstance()
                        .collection("users").document(otherUserId).get().await()
                    val name = if (directDoc.exists()) {
                        directDoc.getString("name")
                            ?: directDoc.getString("fullName")
                            ?: directDoc.getString("displayName") ?: "User"
                    } else {
                        val query = FirebaseFirestore.getInstance()
                            .collection("users").whereEqualTo("userId", otherUserId).limit(1).get().await()
                        val doc = query.documents.firstOrNull()
                        doc?.getString("name") ?: doc?.getString("fullName") ?: doc?.getString("displayName") ?: "User"
                    }
                    userNames[otherUserId] = name
                } catch (_: Exception) { userNames[otherUserId] = "User" }
            }

            val convoId = (convo["conversationId"] as? String) ?: (convo["id"] as? String) ?: ""
            if (convoId.isNotEmpty() && !convoStates.containsKey(convoId)) {
                convoStates[convoId] = ConvoState()
            }
        }
    }

    // ── Delete Confirmation Dialog ────────────────────────────────────────────
    if (deleteTargetConvoId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetConvoId = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = HavenWhite,
            title = {
                Text(
                    "Delete Conversation?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HavenTextPrimary
                )
            },
            text = {
                Text(
                    "Your entire conversation with $deleteTargetName will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HavenTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        convoStates.remove(deleteTargetConvoId)
                        viewModel.deleteConversation(deleteTargetConvoId!!, currentUid)
                        deleteTargetConvoId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteTargetConvoId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = HavenTextSecondary)
                ) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = HavenSurface,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(HavenNavy, Color(0xFF243B6E))
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Messages",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = HavenWhite
                            )
                            if (uiState.unreadCount > 0) {
                                Text(
                                    "${uiState.unreadCount} unread conversation${if (uiState.unreadCount > 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color = HavenGoldLight
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = HavenWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(HavenSurface)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = HavenNavy, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading messages...", fontSize = 13.sp, color = HavenTextSecondary)
                    }
                }

                uiState.conversations.isEmpty() -> {
                    EmptyMessagesState(userRole = userRole)
                }

                else -> {
                    val sortedConvos = uiState.conversations.sortedByDescending { convo ->
                        val cid = (convo["conversationId"] as? String) ?: (convo["id"] as? String) ?: ""
                        convoStates[cid]?.isPinned == true
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = sortedConvos,
                            key   = { convo ->
                                (convo["conversationId"] as? String)
                                    ?: (convo["id"] as? String)
                                    ?: convo.hashCode().toString()
                            }
                        ) { convo ->
                            val participants = (convo["participants"] as? List<*>) ?: emptyList<Any>()
                            val otherUserId  = participants.firstOrNull { it.toString() != currentUid }?.toString() ?: ""
                            val convoId      = (convo["conversationId"] as? String) ?: (convo["id"] as? String) ?: ""
                            val lastMessage  = (convo["lastMessage"] as? String) ?: ""
                            val timestamp    = (convo["lastMessageTimestamp"] as? Long) ?: 0L
                            // val locale       = Locale.getDefault()
                            val locale = remember { Locale.getDefault() }
                            val timeStr = if (timestamp > 0L) {
                                val diff = System.currentTimeMillis() - timestamp
                                if (diff < 24 * 60 * 60 * 1000)
                                    SimpleDateFormat("hh:mm a", locale).format(Date(timestamp))
                                else
                                    SimpleDateFormat("MMM d", locale).format(Date(timestamp))
                            } else ""

                            val displayName  = userNames[otherUserId] ?: "Loading..."
                            val avatarLetter = displayName.firstOrNull { it.isLetter() }?.uppercase() ?: "?"
                            val thisState    = convoStates[convoId] ?: ConvoState()
                            val unreadCount  = ((convo["unreadCount_$currentUid"] as? Long)?.toInt() ?: 0)

                            ConversationItem(
                                avatarInitial = avatarLetter,
                                name          = displayName,
                                lastMessage   = lastMessage.ifEmpty { "Tap to open chat" },
                                timestamp     = timeStr,
                                unreadCount   = unreadCount,
                                isPinned      = thisState.isPinned,
                                isMuted       = thisState.isMuted,
                                isBlocked     = thisState.isBlocked,
                                onClick = {
                                    if (!thisState.isBlocked) {
                                        navController.navigate(
                                            Screen.Chat.createRoute(
                                                userId    = otherUserId,
                                                ownerName = displayName
                                            )
                                        )
                                    }
                                },
                                onPin    = { convoStates[convoId] = thisState.copy(isPinned  = !thisState.isPinned) },
                                onMute   = { convoStates[convoId] = thisState.copy(isMuted   = !thisState.isMuted) },
                                onBlock  = { convoStates[convoId] = thisState.copy(isBlocked = !thisState.isBlocked) },
                                onDelete = { deleteTargetConvoId = convoId; deleteTargetName = displayName }
                            )

                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 80.dp),
                                color     = HavenDivider,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ✦ FIXED ConversationItem — WhatsApp style badge (bottom-right)
// ─────────────────────────────────────────────────────────────
@Composable
private fun ConversationItem(
    avatarInitial : String,
    name          : String,
    lastMessage   : String,
    timestamp     : String,
    unreadCount   : Int,
    isPinned      : Boolean,
    isMuted       : Boolean,
    isBlocked     : Boolean,
    onClick       : () -> Unit,
    onPin         : () -> Unit,
    onMute        : () -> Unit,
    onBlock       : () -> Unit,
    onDelete      : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isUnread = unreadCount > 0

    val rowBackground = when {
        isPinned && isUnread -> HavenUnreadBg
        isPinned             -> HavenNavy.copy(alpha = 0.04f)
        isUnread             -> HavenUnreadBg
        else                 -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(rowBackground)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ── Avatar — SIMPLE, badge bilkul nahi ──────────────────────────
        AvatarCircle(
            letter    = avatarInitial,
            size      = 52,
            isBlocked = isBlocked
        )

        Spacer(modifier = Modifier.width(12.dp))

        // ── Name + Last Message ─────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPinned) {
                    Icon(
                        Icons.Default.PushPin, "Pinned",
                        tint     = HavenGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text       = name,
                    fontSize   = 15.sp,
                    fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color      = if (isBlocked) HavenTextSecondary else HavenTextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (isBlocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text     = "Blocked",
                        fontSize = 9.sp,
                        color    = HavenWhite,
                        modifier = Modifier
                            .background(Color.Gray, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text       = if (isMuted) "🔇 $lastMessage" else lastMessage,
                fontSize   = 13.sp,
                color      = if (isUnread) HavenTextPrimary.copy(alpha = 0.75f) else HavenTextSecondary,
                fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        // ── ✦ RIGHT SIDE: Timestamp upar, badge neeche (WhatsApp style) ──
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            // Timestamp
            Text(
                text       = timestamp,
                fontSize   = 11.sp,
                color      = if (isUnread) HavenGold else HavenTextSecondary,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
            )

            // ✦ Unread badge neeche right mein — WhatsApp style
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(if (unreadCount > 9) 22.dp else 20.dp)
                        .clip(CircleShape)
                        .background(HavenGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (unreadCount > 99) "99+" else "$unreadCount",
                        fontSize   = if (unreadCount > 9) 9.sp else 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = HavenWhite,
                        maxLines   = 1
                    )
                }
            } else {
                // Badge nahi hai toh space maintain karne ke liye invisible box
                Spacer(modifier = Modifier.size(20.dp))
            }

            // Three-dot menu
            Box {
                IconButton(
                    onClick  = { menuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert, "More options",
                        tint     = HavenTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded         = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor   = HavenWhite
                ) {
                    DropdownMenuItem(
                        text        = { Text(if (isPinned) "Unpin Chat" else "Pin Chat", color = HavenTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.PushPin, null, tint = HavenNavy) },
                        onClick     = { menuExpanded = false; onPin() }
                    )
                    DropdownMenuItem(
                        text        = { Text(if (isMuted) "Unmute" else "Mute", color = HavenTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = Color(0xFFF59E0B)) },
                        onClick     = { menuExpanded = false; onMute() }
                    )
                    DropdownMenuItem(
                        text        = { Text(if (isBlocked) "Unblock" else "Block", color = HavenTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Block, null, tint = Color(0xFFEF4444)) },
                        onClick     = { menuExpanded = false; onBlock() }
                    )
                    HorizontalDivider(color = HavenDivider)
                    DropdownMenuItem(
                        text        = { Text("Delete Chat", color = Color(0xFFDC2626)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626)) },
                        onClick     = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Reusable Avatar Circle — simple, no badge
// ─────────────────────────────────────────────────────────────

@Composable
private fun AvatarCircle(letter: String, size: Int, isBlocked: Boolean) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                brush = if (isBlocked)
                    Brush.linearGradient(colors = listOf(Color(0xFF9CA3AF), Color(0xFF9CA3AF)))
                else
                    Brush.linearGradient(colors = listOf(HavenNavy, Color(0xFF243B6E)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = letter,
            fontSize   = (size * 0.38f).sp,
            color      = HavenWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────
@Composable
private fun EmptyMessagesState(userRole: String) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(HavenNavy.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💬", fontSize = 44.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "No Messages Yet",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = HavenTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = if (userRole.equals("landlord", ignoreCase = true))
                "Tenants will appear here when they message you"
            else
                "Start a conversation by browsing properties",
            fontSize  = 14.sp,
            color     = HavenTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(HavenGold)
        )
    }
}