package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.havenhub.data.AdminPermissions
import com.example.havenhub.data.User
import com.example.havenhub.viewmodel.ManagementViewModel

private val GreenOk = Color(0xFF27AE60)
private val RedErr  = Color(0xFFE74C3C)

private const val SUPER_ADMIN_EMAIL = "admin@havenhub.com"

private fun normalizeRole(role: String?): String {
    if (role.isNullOrBlank()) return "user"
    return role.trim().lowercase()
}

private fun roleDisplayName(role: String?): String {
    return when (normalizeRole(role)) {
        "tenant"    -> "Tenant"
        "landlord"  -> "Landlord"
        "admin"     -> "Admin"
        "sub_admin" -> "Sub Admin"
        else        -> if (!role.isNullOrBlank()) role.trim() else "User"
    }
}

private fun getUserRole(user: User): String = roleDisplayName(user.role)

private fun stableUserKey(user: User, index: Int): String {
    return when {
        user.userId.isNotBlank() -> "uid_${user.userId}"
        user.email.isNotBlank()  -> "email_${user.email}"
        else                     -> "idx_$index"
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

    val indexedUsers = remember(uiState.users) {
        uiState.users.mapIndexed { index, user -> Pair(index, user) }
    }

    // ✅ FIX: "Admin" filter mein sub_admin bhi include karo
    val filteredIndexedUsers = remember(indexedUsers, searchQuery, selectedRole) {
        indexedUsers.filter { (_, user) ->
            val matchesSearch = searchQuery.isBlank() ||
                    user.fullName.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true)
            val matchesRole = selectedRole == "All" ||
                    normalizeRole(user.role) == selectedRole.lowercase() ||
                    (selectedRole == "Admin" && normalizeRole(user.role) == "sub_admin") // ← FIX
            matchesSearch && matchesRole
        }
    }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var targetUser           by remember { mutableStateOf<User?>(null) }

    var permManageUsers      by remember { mutableStateOf(false) }
    var permVerifyUsers      by remember { mutableStateOf(false) }
    var permVerifyProperties by remember { mutableStateOf(false) }
    var permManageProperties by remember { mutableStateOf(false) }
    var permManageBookings   by remember { mutableStateOf(false) }
    var permViewReports      by remember { mutableStateOf(false) }

    var showRemoveAdminDialog by remember { mutableStateOf(false) }
    var removeAdminTarget     by remember { mutableStateOf<User?>(null) }

    if (showPermissionDialog && targetUser != null) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor   = surface,
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = primary, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Grant Admin Access",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        targetUser!!.fullName.ifBlank { targetUser!!.email },
                        fontSize = 13.sp,
                        color    = onSurface.copy(alpha = 0.55f)
                    )
                }
            },
            text = {
                Column(
                    modifier            = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Select permissions for this sub-admin:",
                        fontSize = 13.sp,
                        color    = onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    PermissionToggleRow("Manage Users (ban only)", permManageUsers, primary) { permManageUsers = it }
                    PermissionToggleRow("Verify Users",            permVerifyUsers, primary) { permVerifyUsers = it }
                    PermissionToggleRow("Verify Properties",       permVerifyProperties, primary) { permVerifyProperties = it }
                    PermissionToggleRow("Manage Properties",       permManageProperties, primary) { permManageProperties = it }
                    PermissionToggleRow("Manage Bookings",         permManageBookings, primary) { permManageBookings = it }
                    PermissionToggleRow("View Reports",            permViewReports, primary) { permViewReports = it }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        targetUser?.let { user ->
                            viewModel.makeSubAdmin(
                                userId = user.userId,
                                permissions = AdminPermissions(
                                    canManageUsers      = permManageUsers,
                                    canVerifyUsers      = permVerifyUsers,
                                    canVerifyProperties = permVerifyProperties,
                                    canManageProperties = permManageProperties,
                                    canManageBookings   = permManageBookings,
                                    canViewReports      = permViewReports
                                )
                            )
                        }
                        showPermissionDialog = false
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = primary),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Access", color = onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showPermissionDialog = false },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = onSurface.copy(alpha = 0.7f))
                }
            }
        )
    }

    if (showRemoveAdminDialog && removeAdminTarget != null) {
        AlertDialog(
            onDismissRequest = { showRemoveAdminDialog = false },
            containerColor   = surface,
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(RedErr.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonRemove, null, tint = RedErr, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Revoke Admin Access?",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp,
                    color      = onSurface
                )
            },
            text = {
                Text(
                    "\"${removeAdminTarget!!.fullName.ifBlank { removeAdminTarget!!.email }}\" will lose all admin permissions and be restored to their previous role.",
                    fontSize = 14.sp,
                    color    = onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        removeAdminTarget?.let { viewModel.removeSubAdmin(it.userId) }
                        showRemoveAdminDialog = false
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = RedErr),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, Revoke Access", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showRemoveAdminDialog = false },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = onSurface.copy(alpha = 0.7f))
                }
            }
        )
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
                                if (searchQuery.isBlank()) "No \"$selectedRole\" users found"
                                else "No users match your search",
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

                        items(
                            items = filteredIndexedUsers,
                            key   = { (index, user) -> stableUserKey(user, index) }
                        ) { (index, user) ->
                            val isSuperAdmin = user.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
                            val isSubAdmin   = user.isSubAdmin && !isSuperAdmin  // ✅ FIX: user.isSubAdmin use karo

                            MUPremiumUserCard(
                                fullName     = user.fullName.ifBlank { "Unknown User" },
                                email        = user.email.ifBlank { "No email" },
                                role         = getUserRole(user),
                                isVerified   = user.isVerified,
                                isBanned     = user.isBanned,
                                isSuperAdmin = isSuperAdmin,
                                isSubAdmin   = isSubAdmin,
                                canGrantAdmin = uiState.isSuperAdmin && !isSuperAdmin,
                                canDelete    = uiState.isSuperAdmin,
                                primary      = primary,
                                tertiary     = tertiary,
                                surface      = surface,
                                onSurface    = onSurface,
                                onBan        = {
                                    if (user.userId.isNotBlank()) viewModel.banUser(user.userId)
                                },
                                onUnban      = {
                                    if (user.userId.isNotBlank()) viewModel.unbanUser(user.userId)
                                },
                                onDelete     = {
                                    if (user.userId.isNotBlank()) viewModel.deleteUser(user.userId)
                                },
                                onMakeAdmin  = {
                                    targetUser           = user
                                    permManageUsers      = false
                                    permVerifyUsers      = false
                                    permVerifyProperties = false
                                    permManageProperties = false
                                    permManageBookings   = false
                                    permViewReports      = false
                                    showPermissionDialog = true
                                },
                                onRemoveAdmin = {
                                    removeAdminTarget     = user
                                    showRemoveAdminDialog = true
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
private fun PermissionToggleRow(
    label          : String,
    checked        : Boolean,
    primary        : Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize  = 13.sp,
            color     = MaterialTheme.colorScheme.onSurface,
            modifier  = Modifier.weight(1f)
        )
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = primary
            )
        )
    }
}

@Composable
private fun MUPremiumUserCard(
    fullName     : String,
    email        : String,
    role         : String,
    isVerified   : Boolean,
    isBanned     : Boolean,
    isSuperAdmin : Boolean,
    isSubAdmin   : Boolean,
    canGrantAdmin: Boolean,
    canDelete    : Boolean,
    primary      : Color,
    tertiary     : Color,
    surface      : Color,
    onSurface    : Color,
    onBan        : () -> Unit,
    onUnban      : () -> Unit,
    onDelete     : () -> Unit,
    onMakeAdmin  : () -> Unit,
    onRemoveAdmin: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val roleColor = when (role.lowercase()) {
        "admin" -> Color(0xFF6A1B9A)
        "sub admin" -> Color(0xFF6A1B9A)
        "landlord" -> primary
        "tenant" -> Color(0xFF00796B)
        else -> Color(0xFF546E7A)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                4.dp, RoundedCornerShape(16.dp),
                ambientColor = primary.copy(0.08f),
                spotColor = primary.copy(0.12f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        fullName.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tertiary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSuperAdmin) {
                        Surface(
                            color = tertiary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(
                                1.dp,
                                tertiary.copy(0.4f),
                                RoundedCornerShape(20.dp)
                            )
                        ) {
                            Text(
                                "Super Admin",
                                fontSize = 9.sp,
                                color = tertiary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (isSubAdmin) {
                        Surface(
                            color = Color(0xFF6A1B9A).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(
                                1.dp,
                                Color(0xFF6A1B9A).copy(0.35f),
                                RoundedCornerShape(20.dp)
                            )
                        ) {
                            Text(
                                "Sub-Admin",
                                fontSize = 9.sp,
                                color = Color(0xFF6A1B9A),
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Text(
                    email,
                    fontSize = 12.sp,
                    color = onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = roleColor.copy(0.10f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(
                            1.dp,
                            roleColor.copy(0.3f),
                            RoundedCornerShape(20.dp)
                        )
                    ) {
                        Text(
                            role.ifBlank { "User" },
                            fontSize = 11.sp,
                            color = roleColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }

                    val verColor = if (isVerified) GreenOk else RedErr
                    Surface(
                        color = verColor.copy(0.10f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(
                            1.dp,
                            verColor.copy(0.3f),
                            RoundedCornerShape(20.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(verColor))
                            Text(
                                if (isVerified) "Verified" else "Unverified",
                                fontSize = 11.sp,
                                color = verColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isBanned) {
                        val bannedColor = Color(0xFF546E7A)
                        Surface(
                            color = bannedColor.copy(0.10f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(
                                1.dp,
                                bannedColor.copy(0.3f),
                                RoundedCornerShape(20.dp)
                            )
                        ) {
                            Text(
                                "Banned",
                                fontSize = 11.sp,
                                color = bannedColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (!isSuperAdmin) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primary.copy(0.06f))
                    ) {
                        Icon(
                            Icons.Default.MoreVert, null,
                            tint = onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(surface)
                            .width(180.dp)
                    ) {
                        // Ban / Unban
                        if (isBanned) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(GreenOk))
                                },
                                text = {
                                    Text(
                                        "Unban User",
                                        color = GreenOk,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = { menuExpanded = false; onUnban() }
                            )
                        } else {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(RedErr))
                                },
                                text = {
                                    Text(
                                        "Ban User",
                                        color = RedErr,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = { menuExpanded = false; onBan() }
                            )
                        }

                        // Make Admin / Remove Admin — sirf Super Admin
                        if (canGrantAdmin) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = onSurface.copy(alpha = 0.10f)
                            )
                            if (isSubAdmin) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PersonRemove,
                                            null,
                                            tint = RedErr,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Remove Admin",
                                            color = RedErr,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = { menuExpanded = false; onRemoveAdmin() }
                                )
                            } else {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            null,
                                            tint = Color(0xFF6A1B9A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Make Admin",
                                            color = Color(0xFF6A1B9A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = { menuExpanded = false; onMakeAdmin() }
                                )
                            }
                        }

                        // Delete — sirf Super Admin
                        if (canDelete) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = RedErr.copy(alpha = 0.2f)
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteForever,
                                        null,
                                        tint = RedErr,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        "Delete User",
                                        color = RedErr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = { menuExpanded = false; onDelete() }
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