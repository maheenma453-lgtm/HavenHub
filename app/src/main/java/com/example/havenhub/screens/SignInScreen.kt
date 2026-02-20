package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────
// SignInScreen.kt
// PURPOSE : Login screen for all user roles (User, Owner, Admin).
//           Email + password auth via Firebase.
//           Role-based navigation after login.
// NAVIGATION:
//   → Admin  : AdminDashboardScreen
//   → Owner  : HomeScreen (owner view)
//   → User   : HomeScreen (user view)
// ─────────────────────────────────────────────────────────────────

@Composable
fun SignInScreen(
    navController : NavController,
    viewModel     : AuthViewModel = hiltViewModel()
) {

    // ── State Variables ───────────────────────────────────────────
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Observe ViewModel UI state
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // ── Handle Navigation after successful login ───────────────────
    LaunchedEffect(authState) {
        authState?.let { state ->
            when (state.role) {
                "ADMIN"          -> navController.navigate(Screen.AdminDashboard.route) {
                    popUpTo(Screen.SignIn.route) { inclusive = true }
                }
                "PROPERTY_OWNER" -> navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.SignIn.route) { inclusive = true }
                }
                else             -> navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.SignIn.route) { inclusive = true }
                }
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {

        // Blue gradient header background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryBlue, PrimaryDark)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header Section ────────────────────────────────────
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text       = "🏠 HavenHub",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = BackgroundWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text     = "Welcome back!",
                fontSize = 15.sp,
                color    = BackgroundWhite.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── White Card Form ───────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape     = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors    = CardDefaults.cardColors(containerColor = BackgroundWhite)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    Text(
                        text       = "Sign In",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text     = "Enter your credentials to continue",
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Email Field ───────────────────────────────
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = { Text("Email Address") },
                        leadingIcon   = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = PrimaryBlue
                            )
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Password Field ────────────────────────────
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = { Text("Password") },
                        leadingIcon   = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = PrimaryBlue
                            )
                        },
                        // Toggle password visibility
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(10.dp),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PrimaryBlue,
                            unfocusedBorderColor = BorderGray
                        )
                    )

                    // ── Forgot Password Link ──────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { navController.navigate(Screen.ForgotPassword.route) }
                        ) {
                            Text(
                                text  = "Forgot Password?",
                                color = PrimaryBlue,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // ── Error Message ─────────────────────────────
                    viewModel.errorMessage.collectAsState().value?.let { error ->
                        Text(
                            text      = error,
                            color     = ErrorRed,
                            fontSize  = 13.sp,
                            modifier  = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Sign In Button ────────────────────────────
                    Button(
                        onClick = { viewModel.signIn(email.trim(), password) },
                        enabled  = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isLoading) {
                            // Show spinner while authenticating
                            CircularProgressIndicator(
                                color     = BackgroundWhite,
                                modifier  = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text       = "Sign In",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Sign Up Navigation ────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = "Don't have an account? ",
                    color    = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text     = "Sign Up",
                    color    = PrimaryBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.SignUp.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

