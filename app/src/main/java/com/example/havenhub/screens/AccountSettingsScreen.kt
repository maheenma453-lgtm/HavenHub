package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

// ── Brand constants ────────────────────────────────────────────────────
private val NavyDeep    = Color(0xFF0F1D35)
private val NavyPrimary = Color(0xFF1B2A4A)
private val NavyMedium  = Color(0xFF243358)
private val GoldPrimary = Color(0xFFC9A84C)
private val GoldLight   = Color(0xFFE2C47A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    viewModel    : AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark  = isSystemInDarkTheme()

    // ── Theme-aware tokens ─────────────────────────────────────────────
    val screenBg         = if (isDark) DarkBg            else Color(0xFFF4F6F9)
    val cardBg           = if (isDark) DarkSurface        else Color.White
    val textPrimary      = if (isDark) DarkTextPrimary    else TextPrimary
    val textSecond       = if (isDark) DarkTextSecondary  else TextSecondary
    val dividerCol       = if (isDark) DarkBorder         else Color(0xFFE0E4EE)
    val sectionLabelCol  = if (isDark) DarkTextSecondary  else Color(0xFF6B7A99)
    val fieldBorderUnfocused = if (isDark) DarkBorder     else Color(0xFFCDD1DB)

    // ── State ──────────────────────────────────────────────────────────
    var currentPassword   by remember { mutableStateOf("") }
    var newPassword       by remember { mutableStateOf("") }
    var confirmPassword   by remember { mutableStateOf("") }
    var showCurrentPw     by remember { mutableStateOf(false) }
    var showNewPw         by remember { mutableStateOf(false) }
    var showConfirmPw     by remember { mutableStateOf(false) }
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var accountDeleted    by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            currentPassword = ""
            newPassword     = ""
            confirmPassword = ""
            viewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (!uiState.isLoggedIn && accountDeleted) {
            navController.navigate(Screen.SignIn.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val heroGradient = Brush.verticalGradient(
        colors = listOf(NavyDeep, NavyMedium)
    )

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Account Settings",
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = NavyPrimary,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {

            // ══════════════════════════════════════════════════════════
            // HERO BANNER
            // ══════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroGradient)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier
                            .size(46.dp)
                            .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ManageAccounts,
                            contentDescription = null,
                            tint     = GoldLight,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Account Settings",
                            fontSize      = 16.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = Color.White,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            "Manage your password & account",
                            fontSize = 12.sp,
                            color    = Color.White.copy(alpha = 0.58f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════
            // CHANGE PASSWORD SECTION
            // ══════════════════════════════════════════════════════════
            ASSectionHeader(
                title = "Change Password",
                icon  = Icons.Default.Lock,
                color = sectionLabelCol
            )

            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape           = RoundedCornerShape(14.dp),
                color           = cardBg,
                shadowElevation = if (isDark) 0.dp else 2.dp,
                tonalElevation  = if (isDark) 2.dp  else 0.dp
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Current Password
                    ASPasswordField(
                        value         = currentPassword,
                        onValueChange = { currentPassword = it },
                        label         = "Current Password",
                        showPassword  = showCurrentPw,
                        onToggle      = { showCurrentPw = !showCurrentPw },
                        accentColor   = GoldPrimary,
                        textPrimary   = textPrimary,
                        borderUnfocused = fieldBorderUnfocused
                    )
                    // New Password
                    ASPasswordField(
                        value         = newPassword,
                        onValueChange = { newPassword = it },
                        label         = "New Password",
                        showPassword  = showNewPw,
                        onToggle      = { showNewPw = !showNewPw },
                        accentColor   = GoldPrimary,
                        textPrimary   = textPrimary,
                        borderUnfocused = fieldBorderUnfocused
                    )
                    // Confirm Password
                    ASPasswordField(
                        value         = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label         = "Confirm New Password",
                        showPassword  = showConfirmPw,
                        onToggle      = { showConfirmPw = !showConfirmPw },
                        accentColor   = GoldPrimary,
                        textPrimary   = textPrimary,
                        borderUnfocused = fieldBorderUnfocused,
                        isError       = newPassword.isNotBlank()
                                && confirmPassword.isNotBlank()
                                && newPassword != confirmPassword
                    )

                    // Mismatch warning
                    if (newPassword.isNotBlank() && confirmPassword.isNotBlank()
                        && newPassword != confirmPassword
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint     = ErrorRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("Passwords do not match", color = ErrorRed, fontSize = 12.sp)
                        }
                    }

                    // Success message
                    uiState.successMessage?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint     = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(it, color = SuccessGreen, fontSize = 12.sp)
                        }
                    }

                    val canUpdate = currentPassword.isNotBlank()
                            && newPassword.isNotBlank()
                            && newPassword == confirmPassword
                            && !uiState.isLoading

                    Button(
                        onClick  = { viewModel.changePassword(currentPassword, newPassword) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape    = RoundedCornerShape(10.dp),
                        enabled  = canUpdate,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = NavyPrimary,
                            disabledContainerColor = if (isDark)
                                NavyMedium.copy(alpha = 0.4f)
                            else
                                Color(0xFFCDD1DB)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = GoldLight,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.LockReset,
                                contentDescription = null,
                                tint     = if (canUpdate) GoldLight else Color.White.copy(0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Update Password",
                                fontWeight    = FontWeight.SemiBold,
                                color         = if (canUpdate) GoldLight else Color.White.copy(0.5f),
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════
            // LINKED ACCOUNTS SECTION
            // ══════════════════════════════════════════════════════════
            ASSectionHeader(
                title = "Linked Accounts",
                icon  = Icons.Default.Link,
                color = sectionLabelCol
            )

            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape           = RoundedCornerShape(14.dp),
                color           = cardBg,
                shadowElevation = if (isDark) 0.dp else 2.dp,
                tonalElevation  = if (isDark) 2.dp  else 0.dp
            ) {
                Column {
                    ASLinkedItem(
                        icon       = Icons.Default.Email,
                        label      = "Google",
                        subtitle   = uiState.currentUser?.email ?: "—",
                        textPrimary = textPrimary,
                        textSecond  = textSecond,
                        divider     = true,
                        dividerCol  = dividerCol
                    )
                    ASLinkedItem(
                        icon       = Icons.Default.Phone,
                        label      = "Phone Number",
                        subtitle   = uiState.currentUser?.phoneNumber ?: "Not linked",
                        textPrimary = textPrimary,
                        textSecond  = textSecond,
                        divider     = false,
                        dividerCol  = dividerCol
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════
            // DANGER ZONE SECTION
            // ══════════════════════════════════════════════════════════
            ASSectionHeader(
                title      = "Danger Zone",
                icon       = Icons.Default.Warning,
                color      = ErrorRed,
                iconTint   = ErrorRed
            )

            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape           = RoundedCornerShape(14.dp),
                color           = ErrorRed.copy(alpha = if (isDark) 0.08f else 0.04f),
                border          = androidx.compose.foundation.BorderStroke(
                    1.dp, ErrorRed.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Permanently delete your account and all associated data. This action cannot be undone.",
                        fontSize   = 13.sp,
                        color      = textSecond,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick  = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border   = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorRed)
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Delete Account",
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Row(
                    modifier          = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint     = ErrorRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(error, color = ErrorRed, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(88.dp))
        }

        // ══════════════════════════════════════════════════════════════
        // DELETE CONFIRMATION DIALOG
        // ══════════════════════════════════════════════════════════════
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor   = if (isDark) DarkSurface else Color.White,
                icon = {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint     = ErrorRed,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        "Delete Account?",
                        fontWeight = FontWeight.Bold,
                        color      = if (isDark) DarkTextPrimary else TextPrimary
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to permanently delete your account? All your data will be lost and this action cannot be undone.",
                        color      = if (isDark) DarkTextSecondary else TextSecondary,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            accountDeleted   = true
                            viewModel.deleteAccount()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape  = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteDialog = false },
                        shape   = RoundedCornerShape(8.dp),
                        colors  = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) DarkTextSecondary else TextSecondary
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// PRIVATE COMPOSABLES
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ASSectionHeader(
    title    : String,
    icon     : androidx.compose.ui.graphics.vector.ImageVector,
    color    : Color,
    iconTint : Color = GoldPrimary
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(28.dp)
                .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text          = title,
            fontSize      = 13.sp,
            fontWeight    = FontWeight.Bold,
            color         = color,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun ASPasswordField(
    value           : String,
    onValueChange   : (String) -> Unit,
    label           : String,
    showPassword    : Boolean,
    onToggle        : () -> Unit,
    accentColor     : Color,
    textPrimary     : Color,
    borderUnfocused : Color,
    isError         : Boolean = false
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, fontSize = 13.sp) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        singleLine    = true,
        isError       = isError,
        visualTransformation = if (showPassword) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon  = {
            IconButton(onClick = onToggle) {
                Icon(
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Hide" else "Show",
                    tint     = accentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = accentColor,
            unfocusedBorderColor = borderUnfocused,
            focusedLabelColor    = accentColor,
            unfocusedLabelColor  = textPrimary.copy(alpha = 0.55f),
            focusedTextColor     = textPrimary,
            unfocusedTextColor   = textPrimary,
            errorBorderColor     = ErrorRed,
            cursorColor          = accentColor
        )
    )
}

@Composable
private fun ASLinkedItem(
    icon        : ImageVector,
    label       : String,
    subtitle    : String,
    textPrimary : Color,
    textSecond  : Color,
    divider     : Boolean,
    dividerCol  : Color
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .background(GoldPrimary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = textSecond)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint     = textSecond.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
    if (divider) {
        HorizontalDivider(
            modifier  = Modifier.padding(horizontal = 16.dp),
            color     = dividerCol.copy(alpha = 0.6f),
            thickness = 0.5.dp
        )
    }
}











