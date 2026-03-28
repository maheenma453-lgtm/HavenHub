package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    navController: NavController,
    viewModel    : AuthViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val email         by viewModel.email.collectAsState()
    val password      by viewModel.password.collectAsState()
    val emailError    by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut), label = "a"
    )
    val slide by animateFloatAsState(
        targetValue   = if (visible) 0f else 24f,
        animationSpec = tween(550, easing = EaseOutCubic), label = "s"
    )

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            val dest = when (uiState.userRole) {
                "admin", "ADMIN" -> Screen.AdminDashboard.route
                else             -> Screen.Home.route
            }
            navController.navigate(dest) { popUpTo(0) { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status bar space only
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(16.dp))

            // ── Logo + Brand ───────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.alpha(alpha)
            ) {
                Image(
                    painter            = painterResource(id = R.drawable.havenhub),
                    contentDescription = "HavenHub",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .fillMaxWidth(0.65f)
                        .wrapContentHeight()
                        //.padding(vertical = (-24).dp)
                //cuts transparent padding from image
                        .padding(vertical = 0.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "HavenHub",
                    fontSize      = 26.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = PrimaryBlue,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Smart Rental & Vacation Stay",
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Form Card ──────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .offset(y = (slide * 1.2f).dp),
                shape     = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {

                    Text(
                        "Welcome Back",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = PrimaryBlue
                    )
                    Text(
                        "Sign in to your account",
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp).height(3.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(AccentGold, AccentGoldLight)))
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentGold,
                        focusedLabelColor    = PrimaryBlue,
                        unfocusedBorderColor = BorderGray,
                        errorBorderColor     = ErrorRed
                    )

                    OutlinedTextField(
                        value          = email,
                        onValueChange  = { viewModel.onEmailChange(it) },
                        label          = { Text("Email Address") },
                        isError        = emailError != null,
                        supportingText = { emailError?.let { Text(it, color = ErrorRed, fontSize = 11.sp) } },
                        leadingIcon    = { Icon(Icons.Default.Email, null, tint = PrimaryBlue.copy(alpha = 0.7f)) },
                        singleLine     = true,
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(14.dp),
                        colors         = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value                = password,
                        onValueChange        = { viewModel.onPasswordChange(it) },
                        label                = { Text("Password") },
                        isError              = passwordError != null,
                        supportingText       = { passwordError?.let { Text(it, color = ErrorRed, fontSize = 11.sp) } },
                        leadingIcon          = { Icon(Icons.Default.Lock, null, tint = PrimaryBlue.copy(alpha = 0.7f)) },
                        trailingIcon         = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    null, tint = TextSecondary
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

                    TextButton(
                        onClick  = { navController.navigate(Screen.ForgotPassword.route) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Forgot Password?",
                            color      = AccentGoldDark,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    uiState.errorMessage?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ErrorRed.copy(alpha = 0.08f))
                                .padding(11.dp)
                        ) { Text(it, color = ErrorRed, fontSize = 12.sp) }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick  = { viewModel.signIn() },
                        enabled  = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = PrimaryBlue,
                            disabledContainerColor = BorderGray
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
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // OR divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
                        Text("  or continue with  ", fontSize = 11.sp, color = TextSecondary)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Social buttons
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { /* TODO: Google Sign In */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("G", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4285F4))
                                Text("Google", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                        OutlinedButton(
                            onClick  = { /* TODO: Facebook Sign In */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
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
                                    Text("f", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                                Text("Facebook", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.alpha(alpha)
            ) {
                Text("New here? ", color = TextSecondary, fontSize = 13.sp)
                Text(
                    "Create Account",
                    color      = AccentGoldDark,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable {
                        navController.navigate(Screen.RoleSelection.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
