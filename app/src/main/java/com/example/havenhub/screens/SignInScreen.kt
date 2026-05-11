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

// ════════════════════════════════════════════════════════════════════
// LIGHT THEME TOKENS — unchanged
// ════════════════════════════════════════════════════════════════════
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

// ════════════════════════════════════════════════════════════════════
// DARK THEME TOKENS — deep navy + logo gold
// ════════════════════════════════════════════════════════════════════
private val DK_BgDeep      = Color(0xFF060D1A)
private val DK_BgPrimary   = Color(0xFF0D1B3E)
private val DK_BgCard      = Color(0xFF112038)
private val DK_BgField     = Color(0xFF0F1D38)
private val DK_GoldPrimary = Color(0xFFD4AF37)
private val DK_GoldLight   = Color(0xFFF5D060)
private val DK_GoldDark    = Color(0xFFB8962E)
private val DK_TextPrimary = Color(0xFFF0F4FF)
private val DK_TextMuted   = Color(0xFF6A7A9A)
private val DK_Border      = Color(0xFF1E2E50)
private val DK_BorderGold  = Color(0xFF2A3A60)
private val DK_ErrorRed    = Color(0xFFCF6679)

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

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(Unit) { visible = true }

    // ── Animations ───────────────────────────────────────────────
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

    // Theme-aware values
    val screenBg     = if (isDark) DK_BgDeep      else SI_Surface
    val heroGrad     = if (isDark)
        Brush.verticalGradient(listOf(DK_BgDeep, DK_BgPrimary))
    else
        Brush.verticalGradient(listOf(SI_NavyDark, SI_NavyPrimary), startY = 0f, endY = 700f)
    val cardBg       = if (isDark) DK_BgCard      else SI_White
    val fieldBg      = if (isDark) DK_BgField     else SI_White
    val goldP        = if (isDark) DK_GoldPrimary  else SI_GoldPrimary
    val goldL        = if (isDark) DK_GoldLight    else SI_GoldLight
    val goldDk       = if (isDark) DK_GoldDark     else SI_GoldDark
    val textPrimary  = if (isDark) DK_TextPrimary  else SI_TextPrimary
    val textMuted    = if (isDark) DK_TextMuted    else SI_TextMuted
    val border       = if (isDark) DK_Border       else SI_BorderGray
    val borderFocus  = if (isDark) DK_GoldPrimary  else SI_NavyPrimary
    val errorRed     = if (isDark) DK_ErrorRed     else SI_ErrorRed
    val navyP        = if (isDark) DK_BgPrimary    else SI_NavyPrimary

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            val dest = when (uiState.userRole.lowercase()) {
                "admin" -> Screen.AdminDashboard.route
                else    -> Screen.Home.route
            }
            navController.navigate(dest) { popUpTo(0) { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(screenBg)) {
        Column(
            modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Hero Header ──────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().alpha(heroAlpha).background(heroGrad)) {
                // Decorative circles
                Box(
                    modifier = Modifier.size(200.dp).align(Alignment.TopEnd)
                        .offset(x = 60.dp, y = (-40).dp).clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(goldP.copy(alpha = 0.1f), Color.Transparent))
                        )
                )
                if (isDark) {
                    // Extra dark theme decorative circle
                    Box(
                        modifier = Modifier.size(120.dp).align(Alignment.BottomStart)
                            .offset(x = (-30).dp, y = 30.dp).clip(CircleShape)
                            .background(goldP.copy(0.04f))
                    )
                }
                // Gold bottom line
                Box(
                    Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(listOf(goldP.copy(0.9f), goldL.copy(0.4f), goldP.copy(0.9f)))
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 26.dp).padding(top = 20.dp, bottom = 36.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // App name
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
                            color         = goldP,
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
                    // Gold accent bar
                    Box(
                        modifier = Modifier.width(36.dp).height(3.dp).clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(goldDk, goldP, goldL)))
                    )
                }
                // Wave bottom
                Box(
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
                        .background(screenBg)
                )
            }

            // ── Form Card ─────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .offset(y = (-12).dp).alpha(cardAlpha).offset(y = cardSlide.dp),
                shape     = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 10.dp),
                colors    = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                if (isDark) {
                    // Gold top border for dark card
                    Box(
                        Modifier.fillMaxWidth().height(1.5.dp)
                            .background(Brush.horizontalGradient(listOf(goldP.copy(0.8f), goldL.copy(0.3f), goldP.copy(0.8f))))
                    )
                }
                Column(modifier = Modifier.padding(24.dp)) {

                    // Section title for dark theme
                    if (isDark) {
                        Text(
                            "Sign In",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = DK_TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Enter your credentials to continue",
                            fontSize = 12.sp,
                            color    = DK_TextMuted
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    // Field colors — dark or light
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = borderFocus,
                        focusedLabelColor       = borderFocus,
                        unfocusedBorderColor    = border,
                        unfocusedLabelColor     = textMuted,
                        errorBorderColor        = errorRed,
                        cursorColor             = borderFocus,
                        focusedTextColor        = textPrimary,
                        unfocusedTextColor      = textPrimary,
                        focusedContainerColor   = fieldBg,
                        unfocusedContainerColor = fieldBg,
                    )

                    // Email field
                    OutlinedTextField(
                        value          = email,
                        onValueChange  = { viewModel.onEmailChange(it) },
                        label          = { Text("Email Address", fontSize = 13.sp) },
                        isError        = emailError != null,
                        supportingText = { emailError?.let { Text(it, color = errorRed, fontSize = 11.sp) } },
                        leadingIcon    = {
                            Icon(
                                Icons.Default.Email, null,
                                tint     = borderFocus.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier   = Modifier.fillMaxWidth(),
                        shape      = RoundedCornerShape(14.dp),
                        colors     = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Password field
                    OutlinedTextField(
                        value                = password,
                        onValueChange        = { viewModel.onPasswordChange(it) },
                        label                = { Text("Password", fontSize = 13.sp) },
                        isError              = passwordError != null,
                        supportingText       = { passwordError?.let { Text(it, color = errorRed, fontSize = 11.sp) } },
                        leadingIcon          = {
                            Icon(
                                Icons.Default.Lock, null,
                                tint     = borderFocus.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon         = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint     = textMuted,
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

                    // Forgot password
                    TextButton(
                        onClick  = { navController.navigate(Screen.ForgotPassword.route) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Forgot Password?",
                            color      = goldDk,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Error box
                    uiState.errorMessage?.let { errMsg ->
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(errorRed.copy(alpha = 0.07f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, null, tint = errorRed, modifier = Modifier.size(16.dp))
                                Text(errMsg, color = errorRed, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Sign in button
                    Box(
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (isDark && !uiState.isLoading)
                                    Brush.horizontalGradient(listOf(DK_GoldPrimary, DK_GoldLight, DK_GoldPrimary))
                                else if (isDark)
                                    Brush.linearGradient(listOf(DK_Border, DK_Border))
                                else
                                    Brush.linearGradient(listOf(navyP, navyP))
                            )
                            .clickable(enabled = !uiState.isLoading) { viewModel.signIn() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color       = if (isDark) DK_GoldPrimary else Color.White,
                                modifier    = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Sign In",
                                fontSize      = 15.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = if (isDark) DK_BgDeep else Color.White,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // OR divider
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = border)
                        Text("  or continue with  ", fontSize = 11.sp, color = textMuted)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = border)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Social buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Google
                        OutlinedButton(
                            onClick  = { /* TODO: Google */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(13.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, border),
                            colors   = ButtonDefaults.outlinedButtonColors(containerColor = fieldBg)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("G", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4285F4))
                                Text("Google", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            }
                        }
                        // Facebook
                        OutlinedButton(
                            onClick  = { /* TODO: Facebook */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(13.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, border),
                            colors   = ButtonDefaults.outlinedButtonColors(containerColor = fieldBg)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF1877F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("f", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                                Text("Facebook", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            }
                        }
                    }
                }
            }

            // ── Sign up link ──────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.alpha(bottomAlpha).padding(top = 4.dp)
            ) {
                Text("New here? ", color = textMuted, fontSize = 13.sp)
                Text(
                    "Create Account",
                    color      = goldDk,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable { navController.navigate(Screen.RoleSelection.route) }
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
