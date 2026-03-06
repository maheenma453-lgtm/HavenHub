package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState         by viewModel.uiState.collectAsState()
    val fullName        by viewModel.fullName.collectAsState()
    val email           by viewModel.email.collectAsState()
    val password        by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val nameError       by viewModel.nameError.collectAsState()
    val emailError      by viewModel.emailError.collectAsState()
    val passwordError   by viewModel.passwordError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }

    // Navigate to Home when account is created
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.SignUp.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Gradient Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Brush.verticalGradient(listOf(PrimaryBlue, PrimaryBlueDark))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Create Account", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = BackgroundWhite)
                Text("Join thousands of happy renters", fontSize = 14.sp, color = BackgroundWhite.copy(0.8f))
            }
        }

        // ── Main Card ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-20).dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // ── Full Name ──
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { viewModel.onFullNameChange(it) },
                    label = { Text("Full Name") },
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(14.dp))

                // ── Email ──
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text("Email Address") },
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryBlue) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(14.dp))

                // ── Password ──
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = { Text("Password") },
                    isError = passwordError != null,
                    supportingText = { passwordError?.let { Text(it) } },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryBlue) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(14.dp))

                // ── Confirm Password ──
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.onConfirmPasswordChange(it) },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = PrimaryBlue) },
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null
                            )
                        }
                    },
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && password != confirmPassword)
                            Text("Passwords do not match", color = ErrorRed)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(24.dp))

                // ════════════════════════════════════════
                // ── ROLE SELECTION (NAYA SECTION) ──
                // ════════════════════════════════════════
                Text(
                    text = "I am a...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoleCard(
                        role = "Tenant",
                        icon = Icons.Default.Person,
                        description = "Looking for a place to rent",
                        isSelected = uiState.selectedRole == "tenant",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onRoleSelected("tenant") }
                    )

                    RoleCard(
                        role = "Landlord",
                        icon = Icons.Default.Home,
                        description = "I want to list my property",
                        isSelected = uiState.selectedRole == "landlord",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onRoleSelected("landlord") }
                    )
                }
                // ════════════════════════════════════════

                // ── Global Error (role error bhi yahan dikhega) ──
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Create Account Button ──
                Button(
                    onClick = { viewModel.signUp() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = BackgroundWhite,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                text = "Sign In",
                color = PrimaryBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════
// ── RoleCard Component ──
// ════════════════════════════════════════
@Composable
fun RoleCard(
    role: String,
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PrimaryBlue else Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else BackgroundWhite,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = role,
            tint = if (isSelected) PrimaryBlue else Color.Gray,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = role,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isSelected) PrimaryBlue else TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}