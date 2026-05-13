package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

// ── Light theme brand colors ──────────────────────────────────────────────────
private val FP_NavyDark     = Color(0xFF1A2B5E)
private val FP_NavyPrimary  = Color(0xFF243A80)
private val FP_GoldPrimary  = Color(0xFFC9A84C)
private val FP_GoldLight    = Color(0xFFE8C96A)
private val FP_BgTop        = Color(0xFF404040)
private val FP_BgMid        = Color(0xFF1E3570)
private val FP_BgBottom     = Color(0xFF162954)
private val FP_ErrorRed     = Color(0xFFE53935)
private val FP_SuccessGreen = Color(0xFF2E7D32)
private val FP_White        = Color.White

// ── Dark theme overrides — deeper navy + brighter gold ────────────────────────
private val FP_D_BgTop      = Color(0xFF060D1A)   // deepest dark bg
private val FP_D_BgMid      = Color(0xFF0D1B3E)   // mid dark
private val FP_D_BgBottom   = Color(0xFF071020)   // bottom dark
private val FP_D_Gold       = Color(0xFFD4AF37)   // brighter gold in dark
private val FP_D_GoldLight  = Color(0xFFF5D060)   // lighter gold
private val FP_D_NavyDark   = Color(0xFF060D1A)   // text on gold button

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel    : AuthViewModel = hiltViewModel()
) {
    // ── Dark theme detection ──────────────────────────────────────────────────
    val isDark = isSystemInDarkTheme()

    // ── Theme-aware aliases ───────────────────────────────────────────────────
    val bgTop     = if (isDark) FP_D_BgTop    else FP_BgTop
    val bgMid     = if (isDark) FP_D_BgMid    else FP_BgMid
    val bgBottom  = if (isDark) FP_D_BgBottom else FP_BgBottom
    val goldP     = if (isDark) FP_D_Gold     else FP_GoldPrimary
    val goldL     = if (isDark) FP_D_GoldLight else FP_GoldLight
    val navyDark  = if (isDark) FP_D_NavyDark else FP_NavyDark

    val uiState    by viewModel.uiState.collectAsState()
    val email      by viewModel.email.collectAsState()
    val emailError by viewModel.emailError.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOut), label = "alpha"
    )
    val slide by animateFloatAsState(
        targetValue   = if (visible) 0f else 32f,
        animationSpec = tween(650, easing = EaseOutCubic), label = "slide"
    )
    val successScale by animateFloatAsState(
        targetValue   = if (uiState.isPasswordResetSent) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ss"
    )
    val shimmer by rememberInfiniteTransition(label = "sh").animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label         = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(bgTop, bgMid, bgBottom),   // dark: deeper navy, light: original
                    startY = 0f, endY = 1800f
                )
            )
    ) {
        // Decorative gold glow circles — brighter in dark
        Box(
            modifier = Modifier
                .size(280.dp).align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-80).dp).clip(CircleShape)
                .background(Brush.radialGradient(
                    listOf(goldP.copy(alpha = if (isDark) 0.22f else 0.15f), Color.Transparent)
                ))
        )
        Box(
            modifier = Modifier
                .size(200.dp).align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 50.dp).clip(CircleShape)
                .background(Brush.radialGradient(
                    listOf(goldP.copy(alpha = if (isDark) 0.16f else 0.10f), Color.Transparent)
                ))
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Custom Top Bar ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick  = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.CenterStart).size(40.dp).clip(CircleShape)
                        .background(FP_White.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FP_White)
                }
                Text(
                    text       = "Forgot Password",
                    modifier   = Modifier.align(Alignment.Center),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FP_White
                )
            }

            // Gold shimmer accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f).height(1.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            goldP.copy(alpha = 0.4f + 0.4f * shimmer),
                            goldL,
                            goldP.copy(alpha = 0.4f + 0.4f * (1f - shimmer)),
                            Color.Transparent
                        )
                    ))
            )

            Spacer(Modifier.height(32.dp))

            if (uiState.isPasswordResetSent) {
                // ── SUCCESS STATE ─────────────────────────────────────────────
                Column(
                    modifier            = Modifier
                        .fillMaxSize().padding(horizontal = 32.dp).scale(successScale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp).clip(CircleShape)
                            .background(Brush.radialGradient(
                                listOf(FP_SuccessGreen, FP_SuccessGreen.copy(alpha = 0.7f))
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 48.sp, color = FP_White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(28.dp))
                    Text("Email Sent!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = goldP)
                    Spacer(Modifier.height(8.dp))

                    // Gold divider line
                    Box(
                        modifier = Modifier.width(48.dp).height(3.dp).clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(goldP, goldL, goldP)))
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Check your inbox at", fontSize = 13.sp, color = FP_White.copy(alpha = 0.65f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(email, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = goldP, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text("Follow the link to reset your password.", fontSize = 13.sp, color = FP_White.copy(alpha = 0.65f), textAlign = TextAlign.Center, lineHeight = 20.sp)

                    Spacer(Modifier.height(36.dp))

                    Button(
                        onClick   = { navController.popBackStack() },
                        modifier  = Modifier.fillMaxWidth().height(56.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = ButtonDefaults.buttonColors(containerColor = goldP),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("Back to Sign In", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = navyDark)
                    }
                }

            } else {
                // ── INPUT STATE ───────────────────────────────────────────────
                Column(
                    modifier            = Modifier
                        .fillMaxSize().padding(horizontal = 28.dp)
                        .alpha(alpha).offset(y = slide.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lock icon card
                    Box(
                        modifier = Modifier
                            .size(96.dp).clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(
                                listOf(FP_White.copy(alpha = 0.18f), FP_White.copy(alpha = 0.08f))
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        // Gold top stripe on icon card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().height(3.dp).align(Alignment.TopCenter)
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                .background(Brush.horizontalGradient(listOf(goldP, goldL, goldP)))
                        )
                        Text(text = "\uD83D\uDD10", fontSize = 40.sp)
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Reset Your Password", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = FP_White)
                    Spacer(Modifier.height(8.dp))

                    // Gold divider
                    Box(
                        modifier = Modifier.width(48.dp).height(3.dp).clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(goldP, goldL, goldP)))
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Enter your registered email address.\nWe'll send you a password reset link.",
                        fontSize   = 13.sp,
                        color      = FP_White.copy(alpha = 0.65f),
                        textAlign  = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(32.dp))

                    // Email field — glassmorphism card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(FP_White.copy(alpha = if (isDark) 0.06f else 0.10f))
                            .padding(2.dp)
                    ) {
                        OutlinedTextField(
                            value          = email,
                            onValueChange  = { viewModel.onEmailChange(it) },
                            label          = { Text("Email Address", color = FP_White.copy(alpha = 0.75f)) },
                            isError        = emailError != null,
                            supportingText = { emailError?.let { Text(it, color = FP_ErrorRed, fontSize = 11.sp) } },
                            leadingIcon    = { Icon(Icons.Default.Email, null, tint = goldP) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine     = true,
                            modifier       = Modifier.fillMaxWidth(),
                            shape          = RoundedCornerShape(14.dp),
                            colors         = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = goldP,
                                unfocusedBorderColor    = FP_White.copy(alpha = if (isDark) 0.18f else 0.25f),
                                errorBorderColor        = FP_ErrorRed,
                                focusedLabelColor       = goldP,
                                unfocusedLabelColor     = FP_White.copy(alpha = 0.6f),
                                focusedTextColor        = FP_White,
                                unfocusedTextColor      = FP_White,
                                cursorColor             = goldP,
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    // Error message box
                    uiState.errorMessage?.let { errMsg ->
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(FP_ErrorRed.copy(alpha = if (isDark) 0.20f else 0.15f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text      = errMsg,
                                color     = FP_ErrorRed,
                                fontSize  = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Send Reset Link Button
                    Button(
                        onClick   = { viewModel.sendPasswordResetEmail() },
                        enabled   = email.isNotBlank() && !uiState.isLoading,
                        modifier  = Modifier.fillMaxWidth().height(56.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor         = goldP,
                            disabledContainerColor = FP_White.copy(alpha = 0.15f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = navyDark, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send Reset Link", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = navyDark, letterSpacing = 0.5.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(
                            "Back to Sign In",
                            fontSize   = 13.sp,
                            color      = FP_White.copy(alpha = if (isDark) 0.70f else 0.60f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}