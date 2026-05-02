package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.ManagementViewModel

private const val SUPER_ADMIN_EMAIL = "admin@havenhub.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    navController: NavController,
    viewModel    : ManagementViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    var searchQuery  by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("All") }

    val filteredUsers = remember(uiState.users, searchQuery, selectedRole) {
        uiState.users.filter { user ->
            val matchesSearch = user.fullName.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true)
            val matchesRole = selectedRole == "All" ||
                    user.userRole.displayName() == selectedRole
            matchesSearch && matchesRole
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage Users",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy)
            )
        },
        containerColor = Color(0xFFF0F2F5)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Search + Filter Banner ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(PrimaryNavy, PrimaryNavyLight))
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder   = {
                            Text(
                                "Search by name or email...",
                                fontSize = 13.sp,
                                color    = Color.White.copy(0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = Color.White.copy(0.8f))
                        },
                        singleLine = true,
                        modifier   = Modifier.fillMaxWidth(),
                        shape      = RoundedCornerShape(12.dp),
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GoldAccent,
                            unfocusedBorderColor = Color.White.copy(0.3f),
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            cursorColor          = GoldAccent
                        )
                    )

                    val roles = listOf("All", "Tenant", "Landlord", "Admin")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(roles) { role ->
                            val selected = selectedRole == role
                            FilterChip(
                                selected = selected,
                                onClick  = { selectedRole = role },
                                label    = {
                                    Text(
                                        role,
                                        fontSize   = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent,
                                    selectedLabelColor     = Color.White,
                                    containerColor         = Color.White.copy(0.15f),
                                    labelColor             = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selected,
                                    selectedBorderColor = GoldAccent,
                                    borderColor         = Color.White.copy(0.3f),
                                    selectedBorderWidth = 1.5.dp,
                                    borderWidth         = 1.dp
                                )
                            )
                        }
                    }
                }
            }

            // ── Content ────────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = GoldAccent)
                    }
                }

                uiState.errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                            Text("Error: ${uiState.errorMessage}", color = ErrorRed, fontSize = 14.sp)
                            Button(
                                onClick = { viewModel.loadAllUsers() },
                                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                border  = BorderStroke(1.5.dp, GoldAccent)
                            ) { Text("Retry") }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryNavy.copy(0.1f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${filteredUsers.size} users found",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = PrimaryNavy
                                )
                            }
                        }

                        items(filteredUsers, key = { it.userId }) { user ->
                            val isSuperAdmin = user.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
                            ModernUserCard(
                                fullName     = user.fullName,
                                email        = user.email,
                                role         = user.userRole.displayName(),
                                isVerified   = user.isVerified,
                                isBanned     = user.isBanned,
                                isSuperAdmin = isSuperAdmin,
                                onBan        = { viewModel.banUser(user.userId) },
                                onUnban      = { viewModel.unbanUser(user.userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── User Card ──────────────────────────────────────────────────────────────────
@Composable
private fun ModernUserCard(
    fullName    : String,
    email       : String,
    role        : String,
    isVerified  : Boolean,
    isBanned    : Boolean,
    isSuperAdmin: Boolean,
    onBan       : () -> Unit,
    onUnban     : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val roleColor = when (role.lowercase()) {
        "admin"    -> Color(0xFF6A1B9A)
        "landlord" -> PrimaryNavy
        else       -> Color(0xFF00695C)
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSuperAdmin) Color(0xFFFFF8E1) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(
            width = 1.5.dp,
            color = if (isSuperAdmin) GoldAccent else GoldAccent.copy(0.7f)
        )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSuperAdmin) GoldAccent.copy(0.3f) else PrimaryNavy.copy(0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSuperAdmin) {
                    Icon(
                        Icons.Default.Shield,
                        null,
                        tint     = GoldAccent,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Text(
                        fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryNavy
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = Color(0xFF1A1A2E),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    if (isSuperAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(GoldAccent.copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Super Admin",
                                fontSize   = 9.sp,
                                color      = GoldAccentDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    email,
                    fontSize = 12.sp,
                    color    = Color(0xFF888888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Role badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(roleColor.copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            role,
                            fontSize   = 11.sp,
                            color      = roleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Verified / Unverified badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isVerified) SuccessGreen.copy(0.12f) else ErrorRed.copy(0.12f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (isVerified) "Verified" else "Unverified",
                            fontSize   = 11.sp,
                            color      = if (isVerified) SuccessGreen else ErrorRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Banned badge
                    if (isBanned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF37474F).copy(0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Banned",
                                fontSize   = 11.sp,
                                color      = Color(0xFF37474F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3-dots menu (not for super admin)
            if (!isSuperAdmin) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = Color(0xFF888888))
                    }
                    DropdownMenu(
                        expanded         = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor   = Color.White
                    ) {
                        if (isBanned) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Unban User",
                                        color      = SuccessGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                                },
                                onClick = { menuExpanded = false; onUnban() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Ban User",
                                        color      = ErrorRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Block, null, tint = ErrorRed) },
                                onClick = { menuExpanded = false; onBan() }
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
    }
}















