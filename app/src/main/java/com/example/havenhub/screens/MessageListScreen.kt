package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await

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

    // ✅ Other users ke naam store karo — uid -> name map
    val userNames = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            viewModel.loadConversations(currentUid)
        }
    }

    // ✅ Jab bhi conversations update hon — har otherUserId ka naam Firestore se fetch karo
    LaunchedEffect(uiState.conversations) {
        uiState.conversations.forEach { convo ->
            val participants = (convo["participants"] as? List<*>) ?: emptyList<Any>()
            val otherUserId  = participants
                .firstOrNull { it != currentUid }
                ?.toString() ?: ""

            // Sirf fetch karo agar pehle se map mein nahi hai
            if (otherUserId.isNotEmpty() && !userNames.containsKey(otherUserId)) {
                try {
                    val doc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(otherUserId)
                        .get()
                        .await()
                    // ✅ "name" ya "fullName" — jo bhi aapke User model mein hai
                    val name = doc.getString("name")
                        ?: doc.getString("fullName")
                        ?: doc.getString("displayName")
                        ?: "User"
                    userNames[otherUserId] = name
                } catch (e: Exception) {
                    userNames[otherUserId] = "User"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Messages", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                            val timeStr      = if (timestamp > 0L) {
                                SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(Date(timestamp))
                            } else ""

                            // ✅ Naam map se lo — load ho raha ho to "Loading..." dikhao
                            val displayName  = userNames[otherUserId] ?: "Loading..."
                            val avatarLetter = displayName
                                .firstOrNull { it.isLetter() }
                                ?.uppercase() ?: "?"

                            ConversationItem(
                                avatarInitial = avatarLetter,
                                name          = displayName,
                                lastMessage   = lastMessage.ifEmpty { "Start a conversation" },
                                timestamp     = timeStr,
                                unreadCount   = 0,
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

// ── Helper: await() extension for Tasks ──────────────────────────────────────
//private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
//    return kotlinx.coroutines.tasks.await(this)


// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyMessagesState() {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💬", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text       = "No Messages Yet",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text     = "Start a conversation with a landlord",
            fontSize = 13.sp,
            color    = TextSecondary
        )
    }
}

// ── Conversation Row ──────────────────────────────────────────────────────────
@Composable
private fun ConversationItem(
    avatarInitial : String,
    name          : String,
    lastMessage   : String,
    timestamp     : String,
    unreadCount   : Int,
    onClick       : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Avatar ──
        Box(
            modifier         = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = avatarInitial,
                fontSize   = 20.sp,
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Name + Last Message ──
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = name,
                fontSize   = 15.sp,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                color      = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text     = lastMessage,
                fontSize = 13.sp,
                color    = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── Timestamp + Badge ──
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text     = timestamp,
                fontSize = 11.sp,
                color    = if (unreadCount > 0) PrimaryBlue else TextSecondary
            )
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
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
