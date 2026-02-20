package com.example.havenhub.screens
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────
// SignUpScreen.kt
// PURPOSE : New user registration. Collects name, email, phone,
//           password. After signup, navigates to RoleSelectionScreen
//           so user can choose USER or PROPERTY_OWNER role.
// NAVIGATION: SignUpScreen → RoleSelectionScreen
// ─────────────────────────────────────────────────────────────────

@Composable
fun SignUpScreen(
    navController : NavController,
    viewModel     : AuthViewModel = hiltViewModel()
) {

    // ── State Variables ───────────────────────────────────────────
    var fullName        by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }

    val isLoading    by viewModel.isLoading.collectAsState()
    val signUpSuccess by viewModel.signUpSuccess.collectAsState()

    // ── Navigate after successful sign up ─────────────────────────
    LaunchedEffect(signUpSuccess) {
        if (signUpSuccess) {
            navController.navigate(Screen.RoleSelection.route) {
                popUpTo(Screen.SignUp.route) { inclusive = true }
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Blue Header ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryBlue, PrimaryDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = "Create Account",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = BackgroundWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text     = "Join thousands of happy renters",
                    fontSize = 14.sp,
                    color    = BackgroundWhite.copy(alpha = 0.8f)
                )
            }
        }

        // ── Form Card ─────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-20).dp),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors    = CardDefaults.cardColors(containerColor = BackgroundWhite)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // ── Full Name ─────────────────────────────────────
                OutlinedTextField(
                    value         = fullName,
                    onValueChange = { fullName = it },
                    label         = { Text("Full Name") },
                    leadingIcon   = {
                        Icon(Icons.Default.Person, "Name", tint = PrimaryBlue)
                    },
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth(),
                    shape      = RoundedCornerShape(10.dp),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderGray
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Email ─────────────────────────────────────────
                OutlinedTextField(
                    value           = email,
                    onValueChange   = { email = it },
                    label           = { Text("Email Address") },
                    leadingIcon     = {
                        Icon(Icons.Default.Email, "Email", tint = PrimaryBlue)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(10.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderGray
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Phone Number ──────────────────────────────────
                OutlinedTextField(
                    value           = phone,
                    onValueChange   = { phone = it },
                    label           = { Text("Phone Number") },
                    leadingIcon     = {
                        Icon(Icons.Default.Phone, "Phone", tint = PrimaryBlue)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder     = { Text("+92 3XX XXXXXXX") },
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(10.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderGray
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Password ──────────────────────────────────────
                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = { Text("Password") },
                    leadingIcon   = {
                        Icon(Icons.Default.Lock, "Password", tint = PrimaryBlue)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(10.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderGray
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Confirm Password ──────────────────────────────
                OutlinedTextField(
                    value         = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label         = { Text("Confirm Password") },
                    leadingIcon   = {
                        Icon(Icons.Default.LockOpen, "Confirm", tint = PrimaryBlue)
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible)
                                    Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (confirmVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    // Highlight red if passwords don't match
                    isError         = confirmPassword.isNotEmpty() && password != confirmPassword,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(10.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryBlue,
                        unfocusedBorderColor = BorderGray
                    )
                )

                // Passwords mismatch warning
                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text(
                        text     = "Passwords do not match",
                        color    = ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                // ── Error Message ─────────────────────────────────
                viewModel.errorMessage.collectAsState().value?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, color = ErrorRed, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Sign Up Button ────────────────────────────────
                Button(
                    onClick = {
                        // Basic validation before calling viewModel
                        if (password == confirmPassword) {
                            viewModel.signUp(
                                name  = fullName.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                password = password
                            )
                        }
                    },
                    enabled  = !isLoading && password == confirmPassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color       = BackgroundWhite,
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text       = "Create Account",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Already have account ──────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(text = "Already have an account? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                text       = "Sign In",
                color      = PrimaryBlue,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.clickable { navController.popBackStack() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}


