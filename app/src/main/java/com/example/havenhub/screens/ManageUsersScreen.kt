package com.example.havenhub.screens

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
import com.example.havenhub.data.User
import com.example.havenhub.viewmodel.ManagementViewModel

private val GreenOk = Color(0xFF27AE60)
private val RedErr  = Color(0xFFE74C3C)

private const val SUPER_ADMIN_EMAIL = "admin@havenhub.com"

// ✅ Role ko lowercase trim karke normalize karo
// Firebase mein "TENANT", "tenant", "Tenant" — sab handle hoga
private fun normalizeRole(role: String?): String {
    if (role.isNullOrBlank()) return "user"
    return role.trim().lowercase()
}

private fun roleDisplayName(role: String?): String {
    return when (normalizeRole(role)) {
        "tenant"   -> "Tenant"
        "landlord" -> "Landlord"
        "admin"    -> "Admin"
        else       -> if (!role.isNullOrBlank()) role.trim() else "User"
    }
}

private fun getUserRole(user: User): String {
    return roleDisplayName(user.role)
}

// ✅ FIX: Stable unique key banao har user ke liye
// Pehle hashCode() use ho raha tha jo recomposition pe change hota tha → LazyColumn crash
private fun stableUserKey(user: User, index: Int): String {
    return when {
        user.userId.isNotBlank()  -> "uid_${user.userId}"
        user.email.isNotBlank()   -> "email_${user.email}"
        else                      -> "idx_$index"  // index-based fallback — stable per session
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    navController: NavController,
    viewModel    : ManagementViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    var searchQuery  by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("All") }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background

    // ✅ FIX: Users ki list index ke saath zip karo taake stable key mil sake
    val indexedUsers = remember(uiState.users) {
        uiState.users.mapIndexed { index, user -> Pair(index, user) }
    }

    // ✅ FIX: Filter stable indexedUsers se karo
    val filteredIndexedUsers = remember(indexedUsers, searchQuery, selectedRole) {
        indexedUsers.filter { (_, user) ->
            val name  = user.fullName.orEmpty()
            val email = user.email.orEmpty()

            val matchesSearch = searchQuery.isBlank() ||
                    name.contains(searchQuery, ignoreCase = true) ||
                    email.contains(searchQuery, ignoreCase = true)

            val matchesRole = selectedRole == "All" ||
                    normalizeRole(user.role) == selectedRole.lowercase()

            matchesSearch && matchesRole
        }
    }

    Scaffold(
        containerColor = background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tertiary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Manage Users",
                            color         = onPrimary,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            "${uiState.users.size} total users",
                            color    = tertiary.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(background.copy(0f), tertiary, background.copy(0f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(primary, primaryContainer)))
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
                                color    = onPrimary.copy(0.5f)
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = tertiary) },
                        singleLine  = true,
                        modifier    = Modifier.fillMaxWidth(),
                        shape       = RoundedCornerShape(14.dp),
                        colors      = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = tertiary,
                            unfocusedBorderColor    = onPrimary.copy(0.25f),
                            focusedContainerColor   = onPrimary.copy(0.08f),
                            unfocusedContainerColor = onPrimary.copy(0.05f),
                            focusedTextColor        = onPrimary,
                            unfocusedTextColor      = onPrimary,
                            cursorColor             = tertiary
                        )
                    )

                    // ✅ FIX: FilterChip mein border parameter bilkul nahi dena
                    // compose-bom 2024.x mein FilterChipDefaults.filterChipBorder()
                    // ka signature change ho gaya — click par INSTANT CRASH deta tha
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
                                    selectedContainerColor = tertiary,
                                    selectedLabelColor     = onPrimary,
                                    containerColor         = onPrimary.copy(alpha = 0.12f),
                                    labelColor             = onPrimary
                                )
                                // ❌ border = ... — mat dena — crash karega
                            )
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = tertiary, strokeWidth = 3.dp)
                    }
                }

                uiState.errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline, null,
                                tint     = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Error: ${uiState.errorMessage}",
                                color    = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { viewModel.loadAllUsers() },
                                colors  = ButtonDefaults.buttonColors(containerColor = primary),
                                shape   = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry", color = tertiary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                filteredIndexedUsers.isEmpty() && uiState.users.isNotEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff, null,
                                tint     = tertiary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                if (searchQuery.isBlank())
                                    "No \"$selectedRole\" users found"
                                else
                                    "No users match your search",
                                color    = onSurface.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                            if (selectedRole != "All") {
                                TextButton(onClick = { selectedRole = "All" }) {
                                    Text("Show all users", color = tertiary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(primary, primaryContainer))
                                    )
                                    .border(
                                        1.dp,
                                        Brush.horizontalGradient(
                                            listOf(tertiary.copy(0.6f), tertiary.copy(0.4f))
                                        ),
                                        RoundedCornerShape(10.dp)
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
                                            .background(tertiary)
                                    )
                                    Text(
                                        "${filteredIndexedUsers.size} user${if (filteredIndexedUsers.size != 1) "s" else ""} found",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = tertiary
                                    )
                                }
                            }
                        }

                        // ✅ FIX: key = stableUserKey() — hashCode() ki jagah stable string key
                        // hashCode() recomposition par change hota tha → LazyColumn item mismatch → CRASH
                        items(
                            items = filteredIndexedUsers,
                            key   = { (index, user) -> stableUserKey(user, index) }
                        ) { (index, user) ->
                            val isSuperAdmin = user.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
                            MUPremiumUserCard(
                                fullName     = user.fullName.ifBlank { "Unknown User" },
                                email        = user.email.ifBlank { "No email" },
                                role         = getUserRole(user),
                                isVerified   = user.isVerified,
                                isBanned     = user.isBanned,
                                isSuperAdmin = isSuperAdmin,
                                primary      = primary,
                                tertiary     = tertiary,
                                surface      = surface,
                                onSurface    = onSurface,
                                onBan        = {
                                    if (user.userId.isNotBlank()) viewModel.banUser(user.userId)
                                },
                                onUnban      = {
                                    if (user.userId.isNotBlank()) viewModel.unbanUser(user.userId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MUPremiumUserCard(
    fullName    : String,
    email       : String,
    role        : String,
    isVerified  : Boolean,
    isBanned    : Boolean,
    isSuperAdmin: Boolean,
    primary     : Color,
    tertiary    : Color,
    surface     : Color,
    onSurface   : Color,
    onBan       : () -> Unit,
    onUnban     : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val roleColor = when (role.lowercase()) {
        "admin"    -> Color(0xFF6A1B9A)
        "landlord" -> primary
        "tenant"   -> Color(0xFF00796B)
        else       -> Color(0xFF546E7A)
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(
                4.dp, RoundedCornerShape(16.dp),
                ambientColor = primary.copy(0.08f),
                spotColor    = primary.copy(0.12f)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSuperAdmin) tertiary.copy(0.05f) else surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    if (isSuperAdmin)
                        Brush.horizontalGradient(listOf(tertiary, tertiary.copy(0.6f)))
                    else
                        Brush.horizontalGradient(listOf(primary, tertiary))
                )
        )

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSuperAdmin)
                            Brush.radialGradient(listOf(tertiary, tertiary.copy(0.7f)))
                        else
                            Brush.radialGradient(listOf(primary, primary.copy(0.7f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSuperAdmin) {
                    Icon(
                        Icons.Default.Shield, null,
                        tint     = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        fullName.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "?",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = tertiary
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
                        fontSize   = 15.sp,
                        color      = onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    if (isSuperAdmin) {
                        Surface(
                            color    = tertiary.copy(alpha = 0.15f),
                            shape    = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, tertiary.copy(0.4f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                "Super Admin",
                                fontSize   = 9.sp,
                                color      = tertiary,
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
                    color    = onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color    = roleColor.copy(0.10f),
                        shape    = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, roleColor.copy(0.3f), RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            role.ifBlank { "User" },
                            fontSize   = 11.sp,
                            color      = roleColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }

                    val verColor = if (isVerified) GreenOk else RedErr
                    Surface(
                        color    = verColor.copy(0.10f),
                        shape    = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, verColor.copy(0.3f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(verColor)
                            )
                            Text(
                                if (isVerified) "Verified" else "Unverified",
                                fontSize   = 11.sp,
                                color      = verColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isBanned) {
                        val bannedColor = Color(0xFF546E7A)
                        Surface(
                            color    = bannedColor.copy(0.10f),
                            shape    = RoundedCornerShape(20.dp),
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

            if (!isSuperAdmin) {
                Box {
                    IconButton(
                        onClick  = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primary.copy(0.06f))
                    ) {
                        Icon(
                            Icons.Default.MoreVert, null,
                            tint     = onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded         = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier         = Modifier
                            .background(surface)
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
                                text    = {
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
                                text    = {
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









