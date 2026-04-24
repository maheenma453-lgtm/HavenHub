package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel

// ── Brand Colors ────────────────────────────────────────────────────────────
private val SI_NavyDark    = Color(0xFF0D1B3E)
private val SI_NavyPrimary = Color(0xFF1A2A6C)
private val SI_GoldPrimary = Color(0xFFC9A84C)
private val SI_GoldLight   = Color(0xFFE8C96A)
private val SI_GoldDark    = Color(0xFF9A7A30)
private val SI_White       = Color(0xFFFDFBF7)
private val SI_Surface     = Color(0xFFF4F6FB)
private val SI_TextPrimary = Color(0xFF0D1B3E)
private val SI_TextMuted   = Color(0xFF8A94A6)
private val SI_BorderGray  = Color(0xFFDDE2EF)
private val SI_ErrorRed    = Color(0xFFD94040)

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel    : AuthViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val email         by viewModel.email.collectAsState()
    val password      by viewModel.password.collectAsState()
    val emailError    by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var visible         by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    // ── Animations ──────────────────────────────────────────────────────────
    val heroAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut), label = "ha"
    )
    val cardAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 150, easing = EaseOut), label = "ca"
    )
    val cardSlide by animateFloatAsState(
        targetValue   = if (visible) 0f else 32f,
        animationSpec = tween(550, delayMillis = 150, easing = EaseOutCubic), label = "cs"
    )
    val bottomAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 300, easing = EaseOut), label = "boa"
    )

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            val dest = when (uiState.userRole.lowercase()) {
                "admin" -> Screen.AdminDashboard.route
                else    -> Screen.Home.route
            }
            navController.navigate(dest) { popUpTo(0) { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SI_Surface)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Hero Header ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(heroAlpha)
                    .background(
                        Brush.verticalGradient(
                            listOf(SI_NavyDark, SI_NavyPrimary),
                            startY = 0f, endY = 700f
                        )
                    )
            ) {
                // Decorative circle top-right
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 60.dp, y = (-40).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(SI_GoldPrimary.copy(alpha = 0.1f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 26.dp)
                        .padding(top = 20.dp, bottom = 36.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // FIX: Logo removed — clean HAVENHUB wordmark only
                    Row {
                        Text(
                            "HAVEN",
                            fontSize      = 22.sp,
                            fontWeight    = FontWeight.Black,
                            color         = Color.White,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            "HUB",
                            fontSize      = 22.sp,
                            fontWeight    = FontWeight.Black,
                            color         = SI_GoldPrimary,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        "Welcome Back",
                        fontSize      = 26.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color.White,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Sign in to continue your journey",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Gold accent
                    Box(
                        modifier = Modifier
                            .width(36.dp).height(3.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(SI_GoldDark, SI_GoldPrimary, SI_GoldLight))
                            )
                    )
                }

                // Wave bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
                        .background(SI_Surface)
                )
            }

            // ── Form Card ────────────────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-12).dp)
                    .alpha(cardAlpha)
                    .offset(y = cardSlide.dp),
                shape     = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors    = CardDefaults.cardColors(containerColor = SI_White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = SI_NavyPrimary,
                        focusedLabelColor    = SI_NavyPrimary,
                        unfocusedBorderColor = SI_BorderGray,
                        unfocusedLabelColor  = SI_TextMuted,
                        errorBorderColor     = SI_ErrorRed,
                        cursorColor          = SI_NavyPrimary
                    )

                    // Email
                    OutlinedTextField(
                        value          = email,
                        onValueChange  = { viewModel.onEmailChange(it) },
                        label          = { Text("Email Address", fontSize = 13.sp) },
                        isError        = emailError != null,
                        supportingText = {
                            emailError?.let {
                                Text(it, color = SI_ErrorRed, fontSize = 11.sp)
                            }
                        },
                        leadingIcon    = {
                            Icon(
                                Icons.Default.Email, null,
                                tint     = SI_NavyPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine     = true,
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(14.dp),
                        colors         = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value                = password,
                        onValueChange        = { viewModel.onPasswordChange(it) },
                        label                = { Text("Password", fontSize = 13.sp) },
                        isError              = passwordError != null,
                        supportingText       = {
                            passwordError?.let {
                                Text(it, color = SI_ErrorRed, fontSize = 11.sp)
                            }
                        },
                        leadingIcon          = {
                            Icon(
                                Icons.Default.Lock, null,
                                tint     = SI_NavyPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon         = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    null,
                                    tint     = SI_TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        singleLine           = true,
                        modifier             = Modifier.fillMaxWidth(),
                        shape                = RoundedCornerShape(14.dp),
                        colors               = fieldColors
                    )

                    // Forgot Password
                    TextButton(
                        onClick  = { navController.navigate(Screen.ForgotPassword.route) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Forgot Password?",
                            color      = SI_GoldDark,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Error box
                    uiState.errorMessage?.let { errMsg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SI_ErrorRed.copy(alpha = 0.07f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning, null,
                                    tint     = SI_ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(errMsg, color = SI_ErrorRed, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Sign In button
                    Button(
                        onClick   = { viewModel.signIn() },
                        enabled   = !uiState.isLoading,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape     = RoundedCornerShape(15.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor         = SI_NavyPrimary,
                            disabledContainerColor = SI_BorderGray
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Sign In",
                                fontSize      = 15.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = Color.White,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // OR divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SI_BorderGray)
                        Text(
                            "  or continue with  ",
                            fontSize = 11.sp,
                            color    = SI_TextMuted
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SI_BorderGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Social buttons
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Google
                        OutlinedButton(
                            onClick  = { /* TODO: Google */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(13.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, SI_BorderGray),
                            colors   = ButtonDefaults.outlinedButtonColors(containerColor = SI_White)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "G",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = Color(0xFF4285F4)
                                )
                                Text(
                                    "Google",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = SI_TextPrimary
                                )
                            }
                        }
                        // Facebook
                        OutlinedButton(
                            onClick  = { /* TODO: Facebook */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(13.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, SI_BorderGray),
                            colors   = ButtonDefaults.outlinedButtonColors(containerColor = SI_White)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1877F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "f",
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = Color.White
                                    )
                                }
                                Text(
                                    "Facebook",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = SI_TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ── Sign Up link ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .alpha(bottomAlpha)
                    .padding(top = 4.dp)
            ) {
                Text("New here? ", color = SI_TextMuted, fontSize = 13.sp)
                Text(
                    "Create Account",
                    color      = SI_GoldDark,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable {
                        navController.navigate(Screen.RoleSelection.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
