package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────
// ForgotPasswordScreen.kt
// PURPOSE : Allows user to request a password reset email via
//           Firebase Auth. Shows success confirmation after sending.
// NAVIGATION: ForgotPasswordScreen → (back to SignIn)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController : NavController,
    viewModel     : AuthViewModel = hiltViewModel()
) {

    // ── State ──────────────────────────────────────────────────────
    var email        by remember { mutableStateOf("") }
    val isLoading    by viewModel.isLoading.collectAsState()
    val resetSuccess by viewModel.resetEmailSent.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // ── UI ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password") },
                navigationIcon = {
                    // Back arrow to return to sign in
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor      = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            // ── Lock Illustration ──────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔐", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Title & Description ────────────────────────────────
            Text(
                text       = "Reset Your Password",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = "Enter your registered email address. We'll send you a link to reset your password.",
                fontSize  = 14.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Success State ─────────────────────────────────────
            if (resetSuccess) {
                // Show success card when email is sent
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = SuccessGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "✅", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text       = "Email Sent!",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text      = "Check your inbox at $email and follow the instructions to reset your password.",
                            fontSize  = 13.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Back to Sign In button
                OutlinedButton(
                    onClick  = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text  = "Back to Sign In",
                        color = PrimaryBlue
                    )
                }

            } else {

                // ── Email Input Form ──────────────────────────────
                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it },
                    label         = { Text("Email Address") },
                    leadingIcon   = {
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

                // ── Error Message ─────────────────────────────────
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text     = error,
                        color    = ErrorRed,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Send Reset Button ─────────────────────────────
                Button(
                    onClick  = { viewModel.sendPasswordReset(email.trim()) },
                    enabled  = email.isNotBlank() && !isLoading,
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
                            text       = "Send Reset Link",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}


