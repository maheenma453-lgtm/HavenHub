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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.MessagingViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await

// Local conversation state (pin/mute/block per conversation)
private data class ConvoState(
    val isPinned  : Boolean = false,
    val isMuted   : Boolean = false,
    val isBlocked : Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController: NavController,
    viewModel    : MessagingViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val currentUid  = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var userRole  by remember { mutableStateOf("") }
    val userNames  = remember { mutableStateMapOf<String, String>() }
    val convoStates = remember { mutableStateMapOf<String, ConvoState>() }

    var deleteTargetConvoId by remember { mutableStateOf<String?>(null) }
    var deleteTargetName    by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline          = MaterialTheme.colorScheme.outline

    // Load user role and conversations
    LaunchedEffect(currentUid) {
        if (currentUid.isEmpty()) return@LaunchedEffect
        try {
            val directDoc = FirebaseFirestore.getInstance().collection("users").document(currentUid).get().await()
            userRole = if (directDoc.exists()) {
                directDoc.getString("role")?.trim() ?: ""
            } else {
                val query = FirebaseFirestore.getInstance().collection("users").whereEqualTo("userId", currentUid).limit(1).get().await()
                query.documents.firstOrNull()?.getString("role")?.trim() ?: ""
            }
        } catch (_: Exception) { userRole = "" }
        viewModel.loadConversations(currentUid)
    }

    // Fetch display names for other participants
    LaunchedEffect(uiState.conversations) {
        uiState.conversations.forEach { convo ->
            val participants = (convo["participants"] as? List<*>) ?: emptyList<Any>()
            val otherUserId  = participants.firstOrNull { it.toString() != currentUid }?.toString() ?: ""
            if (otherUserId.isNotEmpty() && !userNames.containsKey(otherUserId)) {
                try {
                    val doc = FirebaseFirestore.getInstance().collection("users").document(otherUserId).get().await()
                    val name = if (doc.exists()) {
                        doc.getString("name") ?: doc.getString("fullName") ?: doc.getString("displayName") ?: "User"
                    } else {
                        val q = FirebaseFirestore.getInstance().collection("users").whereEqualTo("userId", otherUserId).limit(1).get().await()
                        q.documents.firstOrNull()?.let { it.getString("name") ?: it.getString("fullName") ?: "User" } ?: "User"
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

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (deleteTargetConvoId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetConvoId = null },
            shape            = RoundedCornerShape(16.dp),
            title            = { Text("Delete Conversation?", fontWeight = FontWeight.Bold, color = onSurface) },
            text             = { Text("Your entire conversation with $deleteTargetName will be permanently deleted.", color = onSurfaceVariant) },
            confirmButton    = {
                TextButton(
                    onClick = {
                        convoStates.remove(deleteTargetConvoId)
                        viewModel.deleteConversation(deleteTargetConvoId!!, currentUid)
                        deleteTargetConvoId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetConvoId = null }, colors = ButtonDefaults.textButtonColors(contentColor = onSurfaceVariant)) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = background,
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth().shadow(4.dp)
                    .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Messages", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onPrimary)
                            if (uiState.unreadCount > 0) {
                                Text(
                                    "${uiState.unreadCount} unread conversation${if (uiState.unreadCount > 1) "s" else ""}",
                                    fontSize = 11.sp, color = tertiary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(background)) {
            when {
                // Loading
                uiState.isLoading -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = primary, strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading messages...", fontSize = 13.sp, color = onSurfaceVariant)
                    }
                }

                // Empty state
                uiState.conversations.isEmpty() -> {
                    MLEmptyState(userRole = userRole, primary = primary, tertiary = tertiary, onSurface = onSurface, onSurfaceVariant = onSurfaceVariant)
                }

                // Conversation list (pinned first)
                else -> {
                    val sortedConvos = uiState.conversations.sortedByDescending { convo ->
                        val cid = (convo["conversationId"] as? String) ?: (convo["id"] as? String) ?: ""
                        convoStates[cid]?.isPinned == true
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = sortedConvos,
                            key   = { convo -> (convo["conversationId"] as? String) ?: (convo["id"] as? String) ?: convo.hashCode().toString() }
                        ) { convo ->
                            val participants = (convo["participants"] as? List<*>) ?: emptyList<Any>()
                            val otherUserId  = participants.firstOrNull { it.toString() != currentUid }?.toString() ?: ""
                            val convoId      = (convo["conversationId"] as? String) ?: (convo["id"] as? String) ?: ""
                            val lastMessage  = (convo["lastMessage"] as? String) ?: ""
                            val timestamp    = (convo["lastMessageTimestamp"] as? Long) ?: 0L
                            val locale       = remember { Locale.getDefault() }
                            val timeStr      = if (timestamp > 0L) {
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

                            MLConversationItem(
                                avatarInitial = avatarLetter,
                                name          = displayName,
                                lastMessage   = lastMessage.ifEmpty { "Tap to open chat" },
                                timestamp     = timeStr,
                                unreadCount   = unreadCount,
                                isPinned      = thisState.isPinned,
                                isMuted       = thisState.isMuted,
                                isBlocked     = thisState.isBlocked,
                                primary       = primary,
                                tertiary      = tertiary,
                                surface       = surface,
                                onSurface     = onSurface,
                                onSurfaceVariant = onSurfaceVariant,
                                outline       = outline,
                                onClick       = {
                                    if (!thisState.isBlocked)
                                        navController.navigate(Screen.Chat.createRoute(userId = otherUserId, ownerName = displayName))
                                },
                                onPin    = { convoStates[convoId] = thisState.copy(isPinned  = !thisState.isPinned) },
                                onMute   = { convoStates[convoId] = thisState.copy(isMuted   = !thisState.isMuted) },
                                onBlock  = { convoStates[convoId] = thisState.copy(isBlocked = !thisState.isBlocked) },
                                onDelete = { deleteTargetConvoId = convoId; deleteTargetName = displayName }
                            )

                            HorizontalDivider(modifier = Modifier.padding(start = 80.dp), color = outline.copy(0.3f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// ── Conversation item ─────────────────────────────────────────────────────────
@Composable
private fun MLConversationItem(
    avatarInitial   : String,
    name            : String,
    lastMessage     : String,
    timestamp       : String,
    unreadCount     : Int,
    isPinned        : Boolean,
    isMuted         : Boolean,
    isBlocked       : Boolean,
    primary         : Color,
    tertiary        : Color,
    surface         : Color,
    onSurface       : Color,
    onSurfaceVariant: Color,
    outline         : Color,
    onClick         : () -> Unit,
    onPin           : () -> Unit,
    onMute          : () -> Unit,
    onBlock         : () -> Unit,
    onDelete        : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isUnread = unreadCount > 0

    val rowBackground = when {
        isPinned && isUnread -> tertiary.copy(0.08f)
        isPinned             -> primary.copy(0.04f)
        isUnread             -> tertiary.copy(0.06f)
        else                 -> Color.Transparent
    }

    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick() }.background(rowBackground).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        MLAvatarCircle(letter = avatarInitial, size = 52, isBlocked = isBlocked, primary = primary, onPrimary = MaterialTheme.colorScheme.onPrimary)

        Spacer(Modifier.width(12.dp))

        // Name + last message
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPinned) {
                    Icon(Icons.Default.PushPin, "Pinned", tint = tertiary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text       = name,
                    fontSize   = 15.sp,
                    fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color      = if (isBlocked) onSurfaceVariant else onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (isBlocked) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Blocked",
                        fontSize = 9.sp,
                        color    = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.background(onSurfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text       = if (isMuted) "🔇 $lastMessage" else lastMessage,
                fontSize   = 13.sp,
                color      = if (isUnread) onSurface.copy(0.75f) else onSurfaceVariant,
                fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        // Right: timestamp + badge + menu
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
            Text(timestamp, fontSize = 11.sp, color = if (isUnread) tertiary else onSurfaceVariant, fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal)

            // Unread badge (WhatsApp style)
            if (isUnread) {
                Box(
                    modifier         = Modifier.size(if (unreadCount > 9) 22.dp else 20.dp).clip(CircleShape).background(tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (unreadCount > 99) "99+" else "$unreadCount",
                        fontSize   = if (unreadCount > 9) 9.sp else 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.onPrimary,
                        maxLines   = 1
                    )
                }
            } else {
                Spacer(Modifier.size(20.dp))
            }

            // Three-dot menu
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, "More options", tint = onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text        = { Text(if (isPinned) "Unpin Chat" else "Pin Chat", color = onSurface) },
                        leadingIcon = { Icon(Icons.Default.PushPin, null, tint = primary) },
                        onClick     = { menuExpanded = false; onPin() }
                    )
                    DropdownMenuItem(
                        text        = { Text(if (isMuted) "Unmute" else "Mute", color = onSurface) },
                        leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = Color(0xFFF59E0B)) },
                        onClick     = { menuExpanded = false; onMute() }
                    )
                    DropdownMenuItem(
                        text        = { Text(if (isBlocked) "Unblock" else "Block", color = onSurface) },
                        leadingIcon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
                        onClick     = { menuExpanded = false; onBlock() }
                    )
                    HorizontalDivider(color = outline.copy(0.3f))
                    DropdownMenuItem(
                        text        = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick     = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

// ── Avatar circle ─────────────────────────────────────────────────────────────
@Composable
private fun MLAvatarCircle(letter: String, size: Int, isBlocked: Boolean, primary: Color, onPrimary: Color) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(
            brush = if (isBlocked) Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF9CA3AF)))
            else Brush.linearGradient(listOf(primary, primary.copy(0.7f)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = letter, fontSize = (size * 0.38f).sp, color = onPrimary, fontWeight = FontWeight.Bold)
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun MLEmptyState(
    userRole        : String,
    primary         : Color,
    tertiary        : Color,
    onSurface       : Color,
    onSurfaceVariant: Color
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier.size(100.dp).clip(CircleShape).background(primary.copy(0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💬", fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("No Messages Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            text      = if (userRole.equals("landlord", ignoreCase = true))
                "Tenants will appear here when they message you"
            else
                "Start a conversation by browsing properties",
            fontSize  = 14.sp,
            color     = onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.width(48.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(tertiary))
    }
}