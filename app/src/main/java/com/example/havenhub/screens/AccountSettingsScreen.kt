package com.example.havenhub.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    viewModel    : AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Dark / Light theme detection ──────────────────────────────
    val isDark = isSystemInDarkTheme()

    // Theme-aware color tokens
    val screenBg    = if (isDark) DarkBg           else Color(0xFFF5F7FA)
    val cardBg      = if (isDark) DarkSurface       else SurfaceVariantLight
    val textPrimary = if (isDark) DarkTextPrimary   else TextPrimary
    val textSecond  = if (isDark) DarkTextSecondary else TextSecondary
    val accentColor = if (isDark) DarkGoldPrimary   else PrimaryBlue
    val topBarBg    = if (isDark) DarkBgSecondary   else PrimaryBlue

    var currentPassword  by remember { mutableStateOf("") }
    var newPassword      by remember { mutableStateOf("") }
    var confirmPassword  by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Track whether account was deleted to trigger navigation
    var accountDeleted by remember { mutableStateOf(false) }

    // Clear fields on success
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            currentPassword = ""
            newPassword     = ""
            confirmPassword = ""
            viewModel.clearSuccess()
        }
    }

    // Navigate to sign-in only after account deletion
    LaunchedEffect(uiState.isLoggedIn) {
        if (!uiState.isLoggedIn && accountDeleted) {
            navController.navigate(Screen.SignIn.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = { Text("Account Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = topBarBg,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Change Password Section ───────────────────────────
            Text(
                "Change Password",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = accentColor
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value                = currentPassword,
                        onValueChange        = { currentPassword = it },
                        label                = { Text("Current Password") },
                        modifier             = Modifier.fillMaxWidth(),
                        shape                = RoundedCornerShape(10.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine           = true,
                        colors               = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = accentColor,
                            focusedLabelColor    = accentColor,
                            focusedTextColor     = textPrimary,
                            unfocusedTextColor   = textPrimary
                        )
                    )
                    OutlinedTextField(
                        value                = newPassword,
                        onValueChange        = { newPassword = it },
                        label                = { Text("New Password") },
                        modifier             = Modifier.fillMaxWidth(),
                        shape                = RoundedCornerShape(10.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine           = true,
                        colors               = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = accentColor,
                            focusedLabelColor    = accentColor,
                            focusedTextColor     = textPrimary,
                            unfocusedTextColor   = textPrimary
                        )
                    )
                    OutlinedTextField(
                        value                = confirmPassword,
                        onValueChange        = { confirmPassword = it },
                        label                = { Text("Confirm New Password") },
                        modifier             = Modifier.fillMaxWidth(),
                        shape                = RoundedCornerShape(10.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine           = true,
                        colors               = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = accentColor,
                            focusedLabelColor    = accentColor,
                            focusedTextColor     = textPrimary,
                            unfocusedTextColor   = textPrimary
                        )
                    )

                    // Password mismatch warning
                    if (newPassword.isNotBlank() && confirmPassword.isNotBlank() && newPassword != confirmPassword) {
                        Text("Passwords do not match", color = ErrorRed, fontSize = 12.sp)
                    }

                    // Success message
                    uiState.successMessage?.let {
                        Text(it, color = SuccessGreen, fontSize = 12.sp)
                    }

                    Button(
                        onClick  = { viewModel.changePassword(currentPassword, newPassword) },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        enabled  = currentPassword.isNotBlank()
                                && newPassword.isNotBlank()
                                && newPassword == confirmPassword
                                && !uiState.isLoading,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Update Password")
                        }
                    }
                }
            }

            // ── Linked Accounts Section ───────────────────────────
            Text(
                "Linked Accounts",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = accentColor
            )
            SettingsGroup(title = "") {
                SettingsItem(
                    icon     = Icons.Default.Email,
                    label    = "Google",
                    subtitle = uiState.currentUser?.email ?: "-",
                    onClick  = {}
                )
                SettingsItem(
                    icon     = Icons.Default.Phone,
                    label    = "Phone Number",
                    subtitle = uiState.currentUser?.phoneNumber ?: "Not linked",
                    onClick  = {}
                )
            }

            // ── Danger Zone ───────────────────────────────────────
            Text(
                "Danger Zone",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = ErrorRed
            )
            OutlinedButton(
                onClick  = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete Account")
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Text(text = error, color = ErrorRed, fontSize = 14.sp)
            }
        }

        // ── Delete Confirmation Dialog ────────────────────────────
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor   = if (isDark) DarkSurface else Color.White,
                title   = {
                    Text(
                        "Delete Account",
                        color = if (isDark) DarkTextPrimary else TextPrimary
                    )
                },
                text    = {
                    Text(
                        "Are you sure you want to permanently delete your account? This action cannot be undone.",
                        color = if (isDark) DarkTextSecondary else TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            accountDeleted   = true
                            viewModel.deleteAccount()
                        }
                    ) {
                        Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = if (isDark) DarkTextSecondary else TextSecondary)
                    }
                }
            )
        }
    }
}
