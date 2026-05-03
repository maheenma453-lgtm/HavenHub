package com.example.havenhub.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.ManagementViewModel

// ── Brand Colors ──────────────────────────────────────────────────────────────
private val NavyBlue   = Color(0xFF1B2A4A)
private val NavyLight  = Color(0xFF243658)
private val Gold       = Color(0xFFC9A227)
private val GoldDark   = Color(0xFFA07D10)
private val PageBg     = Color(0xFFF4F6FA)
private val GreenOk    = Color(0xFF27AE60)
private val RedErr     = Color(0xFFE74C3C)

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
        containerColor = PageBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NavyBlue, NavyLight)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Gold)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Manage Users",
                            color         = Color.White,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            "${uiState.users.size} total users",
                            color    = Gold.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
                // Gold shimmer line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
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
                    .background(Brush.verticalGradient(listOf(NavyBlue, NavyLight)))
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
                                color    = Color.White.copy(0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = Gold)
                        },
                        singleLine = true,
                        modifier   = Modifier.fillMaxWidth(),
                        shape      = RoundedCornerShape(14.dp),
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = Gold,
                            unfocusedBorderColor    = Color.White.copy(0.25f),
                            focusedContainerColor   = Color.White.copy(0.08f),
                            unfocusedContainerColor = Color.White.copy(0.05f),
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            cursorColor             = Gold
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
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Gold,
                                    selectedLabelColor     = Color.White,
                                    containerColor         = Color.White.copy(0.12f),
                                    labelColor             = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selected,
                                    selectedBorderColor = GoldDark,
                                    borderColor         = Color.White.copy(0.25f),
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
                        CircularProgressIndicator(color = Gold, strokeWidth = 3.dp)
                    }
                }

                uiState.errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = RedErr, modifier = Modifier.size(48.dp))
                            Text("Error: ${uiState.errorMessage}", color = RedErr, fontSize = 14.sp)
                            Button(
                                onClick = { viewModel.loadAllUsers() },
                                colors  = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                                shape   = RoundedCornerShape(12.dp)
                            ) { Text("Retry", color = Gold, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Count Banner — same style as other screens
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.horizontalGradient(listOf(NavyBlue, NavyLight)))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(listOf(Gold.copy(0.6f), GoldDark.copy(0.4f))),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Gold)
                                    )
                                    Text(
                                        "${filteredUsers.size} user${if (filteredUsers.size != 1) "s" else ""} found",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Gold
                                    )
                                }
                            }
                        }

                        items(filteredUsers, key = { it.userId }) { user ->
                            val isSuperAdmin = user.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
                            PremiumUserCard(
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

// ── Premium User Card ──────────────────────────────────────────────────────────
@Composable
private fun PremiumUserCard(
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
        "landlord" -> NavyBlue
        else       -> Color(0xFF00796B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 4.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = NavyBlue.copy(alpha = 0.08f),
                spotColor    = NavyBlue.copy(alpha = 0.12f)
            ),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuperAdmin) Color(0xFFFFFBF0) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // Top accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    if (isSuperAdmin)
                        Brush.horizontalGradient(listOf(Gold, GoldDark))
                    else
                        Brush.horizontalGradient(listOf(NavyBlue, Gold))
                )
        )

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSuperAdmin)
                            Brush.radialGradient(listOf(Gold, GoldDark))
                        else
                            Brush.radialGradient(listOf(NavyBlue, NavyLight))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSuperAdmin) {
                    Icon(
                        Icons.Default.Shield,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Gold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Name + Super Admin badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = NavyBlue,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    if (isSuperAdmin) {
                        Surface(
                            color = Gold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, Gold.copy(0.4f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                "Super Admin",
                                fontSize   = 9.sp,
                                color      = GoldDark,
                                fontWeight = FontWeight.ExtraBold,
                                modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Text(
                    email,
                    fontSize = 12.sp,
                    color    = NavyBlue.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                // Badges row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Role
                    Surface(
                        color = roleColor.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, roleColor.copy(0.3f), RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            role,
                            fontSize   = 11.sp,
                            color      = roleColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }

                    // Verified
                    val verifiedColor = if (isVerified) GreenOk else RedErr
                    Surface(
                        color = verifiedColor.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, verifiedColor.copy(0.3f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(verifiedColor)
                            )
                            Text(
                                if (isVerified) "Verified" else "Unverified",
                                fontSize   = 11.sp,
                                color      = verifiedColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Banned
                    if (isBanned) {
                        val bannedColor = Color(0xFF546E7A)
                        Surface(
                            color = bannedColor.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, bannedColor.copy(0.3f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                "Banned",
                                fontSize   = 11.sp,
                                color      = bannedColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 3-dot menu
            if (!isSuperAdmin) {
                Box {
                    IconButton(
                        onClick  = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NavyBlue.copy(alpha = 0.06f))
                    ) {
                        Icon(
                            Icons.Default.MoreVert, null,
                            tint     = NavyBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded         = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier         = Modifier
                            .background(Color.White)
                            .width(160.dp)
                    ) {
                        if (isBanned) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GreenOk)
                                    )
                                },
                                text = {
                                    Text(
                                        "Unban User",
                                        color      = GreenOk,
                                        fontWeight = FontWeight.Medium,
                                        fontSize   = 14.sp
                                    )
                                },
                                onClick = { menuExpanded = false; onUnban() }
                            )
                        } else {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(RedErr)
                                    )
                                },
                                text = {
                                    Text(
                                        "Ban User",
                                        color      = RedErr,
                                        fontWeight = FontWeight.Medium,
                                        fontSize   = 14.sp
                                    )
                                },
                                onClick = { menuExpanded = false; onBan() }
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.size(36.dp))
            }
        }
    }
}
