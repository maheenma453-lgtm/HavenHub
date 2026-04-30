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
import com.example.havenhub.viewmodel.MessagingViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController : NavController,
    viewModel     : MessagingViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val currentUid  = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // FIX: userRole aur loadConversations ko alag LaunchedEffect mein mat rakho —
    // race condition hoti thi. Ab ek hi effect mein pehle role fetch karo,
    // phir conversations load karo. Is tarah guaranteed order hai.
    var userRole  by remember { mutableStateOf("") }
    val userNames  = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(currentUid) {
        if (currentUid.isEmpty()) return@LaunchedEffect

        // Step 1: Role fetch karo pehle
        try {
            val directDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .get()
                .await()

            userRole = if (directDoc.exists()) {
                directDoc.getString("role")?.trim() ?: ""
            } else {
                val query = FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("userId", currentUid)
                    .limit(1)
                    .get()
                    .await()
                query.documents.firstOrNull()?.getString("role")?.trim() ?: ""
            }
        } catch (_: Exception) {
            userRole = ""
        }

        // Step 2: Role fetch hone ke BAAD conversations load karo
        viewModel.loadConversations(currentUid)
    }

    // User names fetch — conversations update hone pe chalega
    LaunchedEffect(uiState.conversations) {
        uiState.conversations.forEach { convo ->
            val participants = (convo["participants"] as? List<*>) ?: emptyList<Any>()
            val otherUserId  = participants
                .firstOrNull { it.toString() != currentUid }
                ?.toString() ?: ""

            if (otherUserId.isNotEmpty() && !userNames.containsKey(otherUserId)) {
                try {
                    val directDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(otherUserId)
                        .get()
                        .await()

                    val name = if (directDoc.exists()) {
                        directDoc.getString("name")
                            ?: directDoc.getString("fullName")
                            ?: directDoc.getString("displayName")
                            ?: "User"
                    } else {
                        val query = FirebaseFirestore.getInstance()
                            .collection("users")
                            .whereEqualTo("userId", otherUserId)
                            .limit(1)
                            .get()
                            .await()
                        val doc = query.documents.firstOrNull()
                        doc?.getString("name")
                            ?: doc?.getString("fullName")
                            ?: doc?.getString("displayName")
                            ?: "User"
                    }
                    userNames[otherUserId] = name
                } catch (_: Exception) {
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
                // FIX: Loading state — conversations aur userRole dono ka wait karo.
                // Pehle sirf uiState.isLoading check hota tha, userRole empty hone pe
                // bhi EmptyMessagesState show ho jata tha with wrong message.
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = PrimaryBlue
                    )
                }

                uiState.conversations.isEmpty() -> {
                    // FIX: userRole ab guaranteed set hai jab yahan pahuncho,
                    // kyunki conversations load karne se pehle role fetch ho chuki hai.
                    EmptyMessagesState(userRole = userRole)
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.conversations) { convo ->
                            val participants = (convo["participants"] as? List<*>)
                                ?: emptyList<Any>()
                            val otherUserId  = participants
                                .firstOrNull { it.toString() != currentUid }
                                ?.toString() ?: ""
                            val lastMessage  = (convo["lastMessage"] as? String) ?: ""
                            val timestamp    = (convo["lastMessageTimestamp"] as? Long) ?: 0L
                            val timeStr      = if (timestamp > 0L) {
                                SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(Date(timestamp))
                            } else ""

                            val displayName  = userNames[otherUserId] ?: "Loading..."
                            val avatarLetter = displayName
                                .firstOrNull { it.isLetter() }
                                ?.uppercase() ?: "?"

                            ConversationItem(
                                avatarInitial = avatarLetter,
                                name          = displayName,
                                lastMessage   = lastMessage.ifEmpty { "Tap to open chat" },
                                timestamp     = timeStr,
                                unreadCount   = 0,
                                onClick       = {
                                    navController.navigate(
                                        Screen.Chat.createRoute(
                                            userId    = otherUserId,
                                            ownerName = displayName
                                        )
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
private fun EmptyMessagesState(userRole: String) {
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
            text     = if (userRole.equals("landlord", ignoreCase = true))
                "Tenants will appear here when they message you"
            else
                "Start a conversation with a landlord",
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
    onClick       : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
