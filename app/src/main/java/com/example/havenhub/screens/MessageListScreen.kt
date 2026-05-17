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

// ── HavenHub Brand Colors ─────────────────────────────────────────────────────
private val HavenNavy      = Color(0xFF1A2744)
private val HavenNavyLight = Color(0xFF2D3F6B)
private val HavenGold      = Color(0xFFC9973A)
private val HavenGoldLight = Color(0xFFE8B84B)
private val OnlineGreen    = Color(0xFF22C55E)

// ── Local per-conversation state ──────────────────────────────────────────────
private data class ConvoState(
    val isPinned : Boolean = false,
    val isMuted  : Boolean = false,
    val isBlocked: Boolean = false
)

// ── User presence data fetched from Firestore ─────────────────────────────────
private data class UserPresence(
    val isOnline : Boolean = false,
    val lastSeen : Long    = 0L      // epoch ms
)

/**
 * Formats lastSeen timestamp to a WhatsApp-style string:
 * "Last seen today at 10:30 AM", "Last seen yesterday at ...", "Last seen MMM d at ..."
 */
private fun formatLastSeen(lastSeen: Long): String {
    if (lastSeen == 0L) return "Last seen recently"
    val now        = System.currentTimeMillis()
    val diff       = now - lastSeen
    val timeFmt    = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFmt    = SimpleDateFormat("MMM d", Locale.getDefault())
    val calNow     = Calendar.getInstance()
    val calSeen    = Calendar.getInstance().apply { timeInMillis = lastSeen }
    val calYestr   = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

    val isSameDay = calNow.get(Calendar.YEAR) == calSeen.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calSeen.get(Calendar.DAY_OF_YEAR)
    val isYesterday = calYestr.get(Calendar.YEAR) == calSeen.get(Calendar.YEAR) &&
            calYestr.get(Calendar.DAY_OF_YEAR) == calSeen.get(Calendar.DAY_OF_YEAR)

    return when {
        diff < 60_000          -> "Last seen just now"
        isSameDay              -> "Last seen today at ${timeFmt.format(Date(lastSeen))}"
        isYesterday            -> "Last seen yesterday at ${timeFmt.format(Date(lastSeen))}"
        else                   -> "Last seen ${dateFmt.format(Date(lastSeen))} at ${timeFmt.format(Date(lastSeen))}"
    }
}

/**
 * Formats the conversation list timestamp — time if today, date if older.
 */
private fun formatConvoTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff   = System.currentTimeMillis() - timestamp
    val locale = Locale.getDefault()
    return if (diff < 24 * 60 * 60 * 1000L)
        SimpleDateFormat("hh:mm a", locale).format(Date(timestamp))
    else if (diff < 7 * 24 * 60 * 60 * 1000L)
        SimpleDateFormat("EEE", locale).format(Date(timestamp))  // Mon, Tue...
    else
        SimpleDateFormat("MMM d", locale).format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController: NavController,
    viewModel    : MessagingViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val currentUid  = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var userRole    by remember { mutableStateOf("") }
    val userNames   = remember { mutableStateMapOf<String, String>() }
    val userPresence = remember { mutableStateMapOf<String, UserPresence>() }  // ✦ Online/LastSeen
    val convoStates = remember { mutableStateMapOf<String, ConvoState>() }

    var deleteTargetConvoId by remember { mutableStateOf<String?>(null) }
    var deleteTargetName    by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Load role + conversations ─────────────────────────────────────────────
    LaunchedEffect(currentUid) {
        if (currentUid.isEmpty()) return@LaunchedEffect
        try {
            val doc = FirebaseFirestore.getInstance().collection("users").document(currentUid).get().await()
            userRole = if (doc.exists()) {
                doc.getString("role")?.trim() ?: ""
            } else {
                val q = FirebaseFirestore.getInstance().collection("users")
                    .whereEqualTo("userId", currentUid).limit(1).get().await()
                q.documents.firstOrNull()?.getString("role")?.trim() ?: ""
            }
        } catch (_: Exception) { userRole = "" }
        viewModel.loadConversations(currentUid)
    }

    // ── Fetch names + presence for other participants ─────────────────────────
    LaunchedEffect(uiState.conversations) {
        uiState.conversations.forEach { convo ->
            val participants = (convo["participants"] as? List<*>) ?: emptyList<Any>()
            val otherUserId  = participants.firstOrNull { it.toString() != currentUid }?.toString() ?: ""

            if (otherUserId.isNotEmpty() && !userNames.containsKey(otherUserId)) {
                try {
                    val doc  = FirebaseFirestore.getInstance().collection("users").document(otherUserId).get().await()
                    val name = if (doc.exists()) {
                        doc.getString("name") ?: doc.getString("fullName") ?: doc.getString("displayName") ?: "User"
                    } else {
                        val q = FirebaseFirestore.getInstance().collection("users")
                            .whereEqualTo("userId", otherUserId).limit(1).get().await()
                        q.documents.firstOrNull()?.let {
                            it.getString("name") ?: it.getString("fullName") ?: "User"
                        } ?: "User"
                    }
                    userNames[otherUserId] = name

                    // ✦ Fetch presence: isOnline + lastSeen
                    val isOnline = doc.getBoolean("isOnline") ?: false
                    val lastSeen = when (val raw = doc.get("lastSeen")) {
                        is Long                               -> raw
                        is com.google.firebase.Timestamp      -> raw.toDate().time
                        else                                  -> 0L
                    }
                    userPresence[otherUserId] = UserPresence(isOnline = isOnline, lastSeen = lastSeen)

                } catch (_: Exception) {
                    userNames[otherUserId]    = "User"
                    userPresence[otherUserId] = UserPresence()
                }
            }

            val convoId = (convo["conversationId"] as? String) ?: (convo["id"] as? String) ?: ""
            if (convoId.isNotEmpty() && !convoStates.containsKey(convoId)) {
                convoStates[convoId] = ConvoState()
            }
        }
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────
    if (deleteTargetConvoId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetConvoId = null },
            shape            = RoundedCornerShape(20.dp),
            title            = { Text("Delete Conversation?", fontWeight = FontWeight.Bold) },
            text             = { Text("$deleteTargetName ke saath saari conversation permanently delete ho jaegi.") },
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
                TextButton(onClick = { deleteTargetConvoId = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // ── HavenHub branded TopBar ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp)
                    .background(Brush.horizontalGradient(listOf(HavenNavy, HavenNavyLight)))
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Messages",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 20.sp,
                                color      = Color.White
                            )
                            if (uiState.unreadCount > 0) {
                                Text(
                                    "${uiState.unreadCount} unread conversation${if (uiState.unreadCount > 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color    = HavenGoldLight
                                )
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = HavenGold, strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading messages...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                uiState.conversations.isEmpty() -> {
                    MLEmptyState(userRole = userRole)
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
                            val timeStr      = formatConvoTime(timestamp)
                            val displayName  = userNames[otherUserId] ?: "Loading..."
                            val avatarLetter = displayName.firstOrNull { it.isLetter() }?.uppercase() ?: "?"
                            val thisState    = convoStates[convoId] ?: ConvoState()
                            val unreadCount  = ((convo["unreadCount_$currentUid"] as? Long)?.toInt() ?: 0)
                            val presence     = userPresence[otherUserId] ?: UserPresence()

                            MLConversationItem(
                                avatarInitial = avatarLetter,
                                name          = displayName,
                                lastMessage   = lastMessage.ifEmpty { "Tap to open chat" },
                                timestamp     = timeStr,
                                unreadCount   = unreadCount,
                                isPinned      = thisState.isPinned,
                                isMuted       = thisState.isMuted,
                                isBlocked     = thisState.isBlocked,
                                presence      = presence,
                                onClick       = {
                                    if (!thisState.isBlocked)
                                        navController.navigate(
                                            Screen.Chat.createRoute(userId = otherUserId, ownerName = displayName)
                                        )
                                },
                                onPin    = { convoStates[convoId] = thisState.copy(isPinned  = !thisState.isPinned) },
                                onMute   = { convoStates[convoId] = thisState.copy(isMuted   = !thisState.isMuted) },
                                onBlock  = { convoStates[convoId] = thisState.copy(isBlocked = !thisState.isBlocked) },
                                onDelete = { deleteTargetConvoId = convoId; deleteTargetName = displayName }
                            )

                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 80.dp),
                                color     = MaterialTheme.colorScheme.outline.copy(0.2f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Conversation Item ─────────────────────────────────────────────────────────
@Composable
private fun MLConversationItem(
    avatarInitial: String,
    name         : String,
    lastMessage  : String,
    timestamp    : String,
    unreadCount  : Int,
    isPinned     : Boolean,
    isMuted      : Boolean,
    isBlocked    : Boolean,
    presence     : UserPresence,
    onClick      : () -> Unit,
    onPin        : () -> Unit,
    onMute       : () -> Unit,
    onBlock      : () -> Unit,
    onDelete     : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isUnread = unreadCount > 0

    val rowBackground = when {
        isPinned && isUnread -> HavenGold.copy(0.08f)
        isPinned             -> HavenNavy.copy(0.04f)
        isUnread             -> HavenGold.copy(0.05f)
        else                 -> Color.Transparent
    }

    // ✦ Status string: Online / Last seen
    val statusText = when {
        isBlocked          -> "Blocked"
        presence.isOnline  -> "Online"
        presence.lastSeen > 0L -> formatLastSeen(presence.lastSeen)
        else               -> ""
    }
    val statusColor = when {
        isBlocked         -> MaterialTheme.colorScheme.error
        presence.isOnline -> OnlineGreen
        else              -> MaterialTheme.colorScheme.onSurfaceVariant.copy(0.65f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(rowBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online dot
        Box {
            MLAvatarCircle(
                letter    = avatarInitial,
                isBlocked = isBlocked
            )
            // ✦ Online green dot
            if (presence.isOnline && !isBlocked) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(OnlineGreen)
                        .align(Alignment.BottomEnd)
                        .padding(1.dp)
                )
            }
        }

        Spacer(Modifier.width(13.dp))

        // Name + last message + status
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPinned) {
                    Icon(Icons.Default.PushPin, null, tint = HavenGold, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                }
                Text(
                    text       = name,
                    fontSize   = 15.sp,
                    fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color      = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            // ✦ Online / Last seen status line
            if (statusText.isNotEmpty()) {
                Text(
                    text     = statusText,
                    fontSize = 11.sp,
                    color    = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (presence.isOnline) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text       = if (isMuted) "🔇 $lastMessage" else lastMessage,
                fontSize   = 13.sp,
                color      = if (isUnread)
                    MaterialTheme.colorScheme.onSurface.copy(0.80f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        // Right: time + badge + menu
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text       = timestamp,
                fontSize   = 11.sp,
                color      = if (isUnread) HavenGold else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
            )

            // Unread badge — HavenHub Gold
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
                        color      = Color.White,
                        maxLines   = 1
                    )
                }
            } else {
                Spacer(Modifier.size(20.dp))
            }

            // Three-dot menu
            Box {
                IconButton(
                    onClick  = { menuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        "More options",
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text        = { Text(if (isPinned) "Unpin Chat" else "Pin Chat") },
                        leadingIcon = { Icon(Icons.Default.PushPin, null, tint = HavenGold) },
                        onClick     = { menuExpanded = false; onPin() }
                    )
                    DropdownMenuItem(
                        text        = { Text(if (isMuted) "Unmute" else "Mute") },
                        leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = Color(0xFFF59E0B)) },
                        onClick     = { menuExpanded = false; onMute() }
                    )
                    DropdownMenuItem(
                        text        = { Text(if (isBlocked) "Unblock" else "Block") },
                        leadingIcon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
                        onClick     = { menuExpanded = false; onBlock() }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
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

// ── Avatar Circle — HavenHub Navy gradient ────────────────────────────────────
@Composable
private fun MLAvatarCircle(letter: String, isBlocked: Boolean) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                brush = if (isBlocked)
                    Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF9CA3AF)))
                else
                    Brush.linearGradient(listOf(HavenNavy, HavenNavyLight))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = letter,
            fontSize   = 20.sp,
            color      = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun MLEmptyState(userRole: String) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(HavenNavy.copy(0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💬", fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "No Messages Yet",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = if (userRole.equals("landlord", ignoreCase = true))
                "Tenants will appear here when they message you"
            else
                "Browse properties and start a conversation",
            fontSize   = 14.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(HavenGold)
        )
    }
}
