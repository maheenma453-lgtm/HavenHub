package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val email        by viewModel.email.collectAsState()
    val emailError   by viewModel.emailError.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut), label = "a")
    val slide by animateFloatAsState(targetValue = if (visible) 0f else 24f,
        animationSpec = tween(550, easing = EaseOutCubic), label = "s")

    val successScale by animateFloatAsState(
        targetValue = if (uiState.isPasswordResetSent) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ss"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundWhite).padding(padding)
        ) {
            if (uiState.isPasswordResetSent) {
                // Success state
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).alpha(successScale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(90.dp).clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) { Text("✓", fontSize = 38.sp, color = BackgroundWhite, fontWeight = FontWeight.Bold) }

                    Spacer(modifier = Modifier.height(22.dp))
                    Text("Email Sent!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(36.dp).height(3.dp).clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.5f)))))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Check your inbox at\n$email\nFollow the link to reset your password.",
                        fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) { Text("← Back to Sign In", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BackgroundWhite) }
                }
            } else {
                // Input state
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).alpha(alpha).offset(y = slide.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier.size(86.dp).clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlueDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(Brush.horizontalGradient(listOf(AccentGold, AccentGoldLight))))
                        Text("🔐", fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    Text("Reset Your Password", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(36.dp).height(3.dp).clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(AccentGold, AccentGoldLight))))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Enter your registered email.\nWe'll send you a reset link.",
                        fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = email, onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email Address") },
                        isError = emailError != null,
                        supportingText = { emailError?.let { Text(it, color = ErrorRed) } },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryBlue) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold, focusedLabelColor = PrimaryBlue,
                            unfocusedBorderColor = BorderGray, errorBorderColor = ErrorRed
                        )
                    )

                    uiState.errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(ErrorRed.copy(alpha = 0.08f)).padding(10.dp)
                        ) { Text(it, color = ErrorRed, fontSize = 12.sp) }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = { viewModel.sendPasswordResetEmail() },
                        enabled = email.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue,
                            disabledContainerColor = BorderGray)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = BackgroundWhite, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send Reset Link  →", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BackgroundWhite)
                        }
                    }
                }
            }
        }
    }
}






















