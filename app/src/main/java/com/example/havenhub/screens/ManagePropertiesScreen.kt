package com.example.havenhub.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.viewmodel.ManagementViewModel

// ── Brand Colors ────────────────────────────────────────────────────────────
private val NavyBlue   = Color(0xFF1B2A4A)
private val NavyLight  = Color(0xFF243658)
private val Gold       = Color(0xFFC9A227)
private val GoldLight  = Color(0xFFE4BE5A)
private val NavySurface = Color(0xFFF4F6FA)   // light off-white tinted navy

// ── Status Colors ────────────────────────────────────────────────────────────
private val StatusApproved    = Color(0xFF27AE60)
private val StatusPending     = Color(0xFFE67E22)
private val StatusRejected    = Color(0xFFE74C3C)
private val StatusUnderReview = Color(0xFF2980B9)
private val StatusInactive    = Color(0xFF95A5A6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePropertiesScreen(
    navController: NavController,
    viewModel: ManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Confirmation dialog state
    var showConfirmDialog   by remember { mutableStateOf(false) }
    var pendingAction       by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDialogTitle  by remember { mutableStateOf("") }
    var confirmDialogMessage by remember { mutableStateOf("") }

    val filteredProperties = remember(uiState.properties, searchQuery) {
        uiState.properties.filter { property ->
            property.title.contains(searchQuery, ignoreCase = true)
        }
    }

    // ── Confirmation Dialog ──────────────────────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            shape            = RoundedCornerShape(16.dp),
            containerColor   = Color.White,
            title = {
                Text(
                    confirmDialogTitle,
                    fontWeight = FontWeight.Bold,
                    color      = NavyBlue,
                    fontSize   = 18.sp
                )
            },
            text = {
                Text(
                    confirmDialogMessage,
                    color    = NavyBlue.copy(alpha = 0.75f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingAction?.invoke()
                        showConfirmDialog = false
                        pendingAction    = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    border  = androidx.compose.foundation.BorderStroke(1.dp, NavyBlue.copy(alpha = 0.3f)),
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = NavyBlue)
                }
            }
        )
    }

    Scaffold(
        containerColor = NavySurface,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(NavyBlue, NavyLight))
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = Gold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text       = "Manage Properties",
                            color      = Color.White,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text  = "${filteredProperties.size} properties",
                            color = Gold.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
                // Gold accent line at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Gold, Color.Transparent)
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            // ── Search Bar ───────────────────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            "Search properties...",
                            color    = NavyBlue.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint               = Gold
                        )
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(14.dp),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Gold,
                        unfocusedBorderColor = NavyBlue.copy(alpha = 0.25f),
                        focusedContainerColor   = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor          = Gold,
                        focusedTextColor     = NavyBlue,
                        unfocusedTextColor   = NavyBlue
                    )
                )
            }

            // ── Property List ────────────────────────────────────────────────
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = 4.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProperties) { property ->
                    PropertyManagementCard(
                        property  = property,
                        onApprove = {
                            confirmDialogTitle   = "Approve Property"
                            confirmDialogMessage = "Approve \"${property.title}\"?"
                            pendingAction        = { viewModel.approveProperty(property.propertyId) }
                            showConfirmDialog    = true
                        },
                        onReject  = {
                            confirmDialogTitle   = "Reject Property"
                            confirmDialogMessage = "Reject \"${property.title}\"?"
                            pendingAction        = { viewModel.removeProperty(property.propertyId) }
                            showConfirmDialog    = true
                        },
                        onDelete  = {
                            confirmDialogTitle   = "Delete Property"
                            confirmDialogMessage = "Permanently delete \"${property.title}\"? This cannot be undone."
                            pendingAction        = { viewModel.deleteProperty(property.propertyId) }
                            showConfirmDialog    = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyManagementCard(
    property : Property,
    onApprove: () -> Unit,
    onReject : () -> Unit,
    onDelete : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val statusColor = when (property.propertyStatusEnum) {
        PropertyStatus.APPROVED     -> StatusApproved
        PropertyStatus.REJECTED     -> StatusRejected
        PropertyStatus.PENDING      -> StatusPending
        PropertyStatus.UNDER_REVIEW -> StatusUnderReview
        PropertyStatus.INACTIVE     -> StatusInactive
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(
                elevation       = 4.dp,
                shape           = RoundedCornerShape(16.dp),
                ambientColor    = NavyBlue.copy(alpha = 0.08f),
                spotColor       = NavyBlue.copy(alpha = 0.12f)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        // Gold top accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(listOf(NavyBlue, Gold))
                )
        )

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Circle
            Box(
                modifier        = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NavyBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint               = Gold,
                    modifier           = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = property.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = NavyBlue,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text     = property.address,
                    fontSize = 12.sp,
                    color    = NavyBlue.copy(alpha = 0.55f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = statusColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(6.dp)
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text     = property.propertyStatusEnum.displayName(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color    = statusColor
                        )
                    }
                }
            }

            // ── Overflow Menu ────────────────────────────────────────────────
            Box {
                IconButton(
                    onClick  = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NavyBlue.copy(alpha = 0.06f))
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint               = NavyBlue,
                        modifier           = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded         = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier         = Modifier
                        .background(Color.White)
                        .width(160.dp)
                ) {
                    // Approve
                    DropdownMenuItem(
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusApproved)
                            )
                        },
                        text    = {
                            Text(
                                "Approve",
                                color      = StatusApproved,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 14.sp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onApprove()
                        }
                    )
                    Divider(color = NavyBlue.copy(alpha = 0.07f), thickness = 0.5.dp)

                    // Reject
                    DropdownMenuItem(
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusPending)
                            )
                        },
                        text    = {
                            Text(
                                "Reject",
                                color      = StatusPending,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 14.sp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onReject()
                        }
                    )
                    Divider(color = NavyBlue.copy(alpha = 0.07f), thickness = 0.5.dp)

                    // Delete
                    DropdownMenuItem(
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusRejected)
                            )
                        },
                        text    = {
                            Text(
                                "Delete",
                                color      = StatusRejected,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 14.sp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}



