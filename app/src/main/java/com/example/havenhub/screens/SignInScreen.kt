package com.example.havenhub.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.R
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

@Composable
fun SignInScreen(
    navController : NavController,
    viewModel     : AuthViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val email         by viewModel.email.collectAsState()
    val password      by viewModel.password.collectAsState()
    val emailError    by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    // ── Role-based navigation ──────────────────────────────────────
    LaunchedEffect(uiState.isLoggedIn, uiState.userRole) {
        if (uiState.isLoggedIn && uiState.userRole.isNotEmpty()) {
            val destination = when (uiState.userRole) {
                "admin"    -> Screen.AdminDashboard.route
                "landlord" -> Screen.Home.route
                else       -> Screen.Home.route
            }
            navController.navigate(destination) {
                popUpTo(Screen.SignIn.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── Top gradient header ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(PrimaryBlue, PrimaryBlueDark)
                    )
                )
        )

        // ── Gold bottom border on header ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .offset(y = 278.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(AccentGoldDark, AccentGold, AccentGoldLight, AccentGold, AccentGoldDark)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Logo ───────────────────────────────────────────────
            Image(
                painter            = painterResource(id = R.drawable.havenhub),
                contentDescription = "HavenHub Logo",
                modifier           = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Brand Name ─────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = BackgroundWhite, fontWeight = FontWeight.Bold)) {
                        append("HAVEN")
                    }
                    withStyle(SpanStyle(color = AccentGold, fontWeight = FontWeight.Bold)) {
                        append("HUB")
                    }
                },
                fontSize      = 28.sp,
                letterSpacing = 2.sp
            )
            Text(
                text          = "Smart Rental & Vacation Stay",
                fontSize      = 11.sp,
                color         = BackgroundWhite.copy(alpha = 0.75f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Card ───────────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape     = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                colors    = CardDefaults.cardColors(containerColor = BackgroundWhite)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        "Sign In",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryBlue
                    )
                    Text(
                        "Enter your credentials to continue",
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )

                    // ── Gold accent line ───────────────────────────
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AccentGold, AccentGoldLight)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Email ──────────────────────────────────────
                    OutlinedTextField(
                        value           = email,
                        onValueChange   = { viewModel.onEmailChange(it) },
                        label           = { Text("Email Address") },
                        isError         = emailError != null,
                        supportingText  = { emailError?.let { Text(it) } },
                        leadingIcon     = { Icon(Icons.Default.Email, null, tint = PrimaryBlue) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(10.dp),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentGold,
                            focusedLabelColor    = PrimaryBlue,
                            unfocusedBorderColor = BorderGray
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Password ───────────────────────────────────
                    OutlinedTextField(
                        value          = password,
                        onValueChange  = { viewModel.onPasswordChange(it) },
                        label          = { Text("Password") },
                        isError        = passwordError != null,
                        supportingText = { passwordError?.let { Text(it) } },
                        leadingIcon    = { Icon(Icons.Default.Lock, null, tint = PrimaryBlue) },
                        trailingIcon   = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(10.dp),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentGold,
                            focusedLabelColor    = PrimaryBlue,
                            unfocusedBorderColor = BorderGray
                        )
                    )

                    // ── Forgot Password ────────────────────────────
                    TextButton(
                        onClick  = { navController.navigate(Screen.ForgotPassword.route) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Forgot Password?",
                            color      = AccentGold,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // ── Error ──────────────────────────────────────
                    uiState.errorMessage?.let {
                        Text(
                            it,
                            color    = ErrorRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // ── Sign In Button ─────────────────────────────
                    Button(
                        onClick  = { viewModel.signIn() },
                        enabled  = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color       = BackgroundWhite,
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Sign In",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = BackgroundWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("→", fontSize = 16.sp, color = AccentGold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Sign Up Link ───────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account? ", color = TextSecondary, fontSize = 14.sp)
                Text(
                    "Sign Up",
                    color      = AccentGold,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable {
                        navController.navigate(Screen.SignUp.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


































