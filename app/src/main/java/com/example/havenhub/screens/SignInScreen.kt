package com.example.havenhub.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private const val GOOGLE_WEB_CLIENT_ID = "33851762861-hnaa7gs8o0qapaedmmts78fcp716tnpc.apps.googleusercontent.com"

// ════════════════════════════════════════════════════════════════════
// LIGHT THEME TOKENS
// ════════════════════════════════════════════════════════════════════
private val SI_NavyDark    = Color(0xFF0D1B3E)
private val SI_NavyPrimary = Color(0xFF1A2A6C)
private val SI_NavyMid     = Color(0xFF1E3A8A)
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
// DARK THEME TOKENS
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
private val DK_ErrorRed    = Color(0xFFCF6679)

// ════════════════════════════════════════════════════════════════════
// GOOGLE ICON
// ════════════════════════════════════════════════════════════════════
@Composable
private fun GoogleIcon(modifier: Modifier = Modifier) {
    val painter = rememberVectorPainter(
        ImageVector.Builder("Google", 24.dp, 24.dp, 48f, 48f).apply {
            path(fill = SolidColor(Color(0xFFEA4335))) {
                moveTo(24f, 9.5f); curveTo(28.24f, 9.5f, 31.07f, 11.27f, 32.67f, 12.77f)
                lineTo(38.73f, 6.87f); curveTo(35.03f, 3.47f, 30.07f, 1.5f, 24f, 1.5f)
                curveTo(14.87f, 1.5f, 7.07f, 6.9f, 3.47f, 14.6f); lineTo(10.47f, 20f)
                curveTo(12.27f, 14.13f, 17.67f, 9.5f, 24f, 9.5f); close()
            }
            path(fill = SolidColor(Color(0xFF4285F4))) {
                moveTo(46.1f, 24.55f); curveTo(46.1f, 22.55f, 45.93f, 21.09f, 45.57f, 19.57f)
                lineTo(24f, 19.57f); lineTo(24f, 28.5f); lineTo(36.67f, 28.5f)
                curveTo(36.4f, 30.57f, 34.93f, 33.67f, 31.93f, 35.77f); lineTo(38.8f, 41.07f)
                curveTo(43.23f, 36.97f, 46.1f, 31.27f, 46.1f, 24.55f); close()
            }
            path(fill = SolidColor(Color(0xFF34A853))) {
                moveTo(10.47f, 28f); curveTo(9.97f, 26.53f, 9.67f, 24.97f, 9.67f, 23.33f)
                curveTo(9.67f, 21.7f, 9.97f, 20.13f, 10.47f, 18.67f); lineTo(3.47f, 13.27f)
                curveTo(1.87f, 16.47f, 1f, 19.8f, 1f, 23.33f)
                curveTo(1f, 26.87f, 1.87f, 30.2f, 3.47f, 33.4f); lineTo(10.47f, 28f); close()
            }
            path(fill = SolidColor(Color(0xFFFBBC05))) {
                moveTo(24f, 45.17f); curveTo(30.07f, 45.17f, 35.17f, 43.17f, 38.8f, 41.07f)
                lineTo(31.93f, 35.77f); curveTo(30.03f, 37.07f, 27.37f, 37.97f, 24f, 37.97f)
                curveTo(17.67f, 37.97f, 12.27f, 33.33f, 10.47f, 27.47f); lineTo(3.47f, 32.87f)
                curveTo(7.07f, 40.57f, 14.87f, 45.17f, 24f, 45.17f); close()
            }
        }.build()
    )
    Image(painter = painter, contentDescription = "Google", modifier = modifier)
}

// ════════════════════════════════════════════════════════════════════
// SIGN IN SCREEN
// ════════════════════════════════════════════════════════════════════
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
    var btnPressed      by remember { mutableStateOf(false) }

    val isDark  = isSystemInDarkTheme()
    val context = LocalContext.current

    // ── Golden shine sweep on press ───────────────────────────────
    val shineOffset by animateFloatAsState(
        targetValue      = if (btnPressed) 1.5f else -0.5f,
        animationSpec    = tween(700, easing = FastOutSlowInEasing),
        finishedListener = { btnPressed = false },
        label            = "shine"
    )

    // ── Google Sign In client ─────────────────────────────────────
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (_: ApiException) { }
        }
    }

    // ── Entry animations ──────────────────────────────────────────
    LaunchedEffect(Unit) { visible = true }

    val heroAlpha   by animateFloatAsState(if (visible) 1f else 0f, tween(500, easing = EaseOut),     label = "ha")
    val cardAlpha   by animateFloatAsState(if (visible) 1f else 0f, tween(500, 150, EaseOut),         label = "ca")
    val cardSlide   by animateFloatAsState(if (visible) 0f else 32f, tween(550, 150, EaseOutCubic),   label = "cs")
    val bottomAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(500, 300, EaseOut),         label = "boa")

    // ── Theme tokens ──────────────────────────────────────────────
    val screenBg    = if (isDark) DK_BgDeep     else SI_Surface
    val heroGrad    = if (isDark)
        Brush.verticalGradient(listOf(DK_BgDeep, DK_BgPrimary))
    else
        Brush.verticalGradient(listOf(SI_NavyDark, SI_NavyPrimary), startY = 0f, endY = 700f)
    val cardBg      = if (isDark) DK_BgCard     else SI_White
    val fieldBg     = if (isDark) DK_BgField    else SI_White
    val goldP       = if (isDark) DK_GoldPrimary else SI_GoldPrimary
    val goldL       = if (isDark) DK_GoldLight   else SI_GoldLight
    val goldDk      = if (isDark) DK_GoldDark    else SI_GoldDark
    val textPrimary = if (isDark) DK_TextPrimary else SI_TextPrimary
    val textMuted   = if (isDark) DK_TextMuted   else SI_TextMuted
    val border      = if (isDark) DK_Border      else SI_BorderGray
    val borderFocus = if (isDark) DK_GoldPrimary else SI_NavyPrimary
    val errorRed    = if (isDark) DK_ErrorRed    else SI_ErrorRed

    // ── Navy blue button gradient ─────────────────────────────────
    val navyBtnGrad = if (isDark)
        Brush.horizontalGradient(listOf(DK_BgPrimary, Color(0xFF1A2F70), DK_BgPrimary))
    else
        Brush.horizontalGradient(listOf(SI_NavyDark, SI_NavyPrimary, SI_NavyMid, SI_NavyPrimary, SI_NavyDark))

    // ── Golden shine brush (sweeps left→right on press) ───────────
    val shineBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            SI_GoldLight.copy(alpha = 0.55f),
            SI_GoldPrimary.copy(alpha = 0.35f),
            Color.Transparent
        ),
        start = Offset(shineOffset * 900f - 250f, 0f),
        end   = Offset(shineOffset * 900f + 150f, 180f)
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

    Box(modifier = Modifier.fillMaxSize().background(screenBg)) {
        Column(
            modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Hero ─────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().alpha(heroAlpha).background(heroGrad)) {
                Box(
                    modifier = Modifier.size(200.dp).align(Alignment.TopEnd)
                        .offset(x = 60.dp, y = (-40).dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(goldP.copy(alpha = 0.1f), Color.Transparent)))
                )
                if (isDark) Box(
                    modifier = Modifier.size(120.dp).align(Alignment.BottomStart)
                        .offset(x = (-30).dp, y = 30.dp).clip(CircleShape)
                        .background(goldP.copy(0.04f))
                )
                Box(
                    Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
                        .background(Brush.horizontalGradient(listOf(goldP.copy(0.9f), goldL.copy(0.4f), goldP.copy(0.9f))))
                )
                Column(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 26.dp).padding(top = 20.dp, bottom = 36.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row {
                        Text("HAVEN", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
                        Text("HUB",   fontSize = 22.sp, fontWeight = FontWeight.Black, color = goldP, letterSpacing = 1.5.sp)
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("Welcome Back", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.3).sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Sign in to continue your journey", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.width(36.dp).height(3.dp).clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(goldDk, goldP, goldL)))
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(24.dp).align(Alignment.BottomCenter)
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
                if (isDark) Box(
                    Modifier.fillMaxWidth().height(1.5.dp)
                        .background(Brush.horizontalGradient(listOf(goldP.copy(0.8f), goldL.copy(0.3f), goldP.copy(0.8f))))
                )
                Column(modifier = Modifier.padding(24.dp)) {
                    if (isDark) {
                        Text("Sign In", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DK_TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("Enter your credentials to continue", fontSize = 12.sp, color = DK_TextMuted)
                        Spacer(Modifier.height(20.dp))
                    }

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

                    OutlinedTextField(
                        value          = email,
                        onValueChange  = { viewModel.onEmailChange(it) },
                        label          = { Text("Email Address", fontSize = 13.sp) },
                        isError        = emailError != null,
                        supportingText = { emailError?.let { Text(it, color = errorRed, fontSize = 11.sp) } },
                        leadingIcon    = { Icon(Icons.Default.Email, null, tint = borderFocus.copy(0.6f), modifier = Modifier.size(20.dp)) },
                        singleLine     = true,
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(14.dp),
                        colors         = fieldColors
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value          = password,
                        onValueChange  = { viewModel.onPasswordChange(it) },
                        label          = { Text("Password", fontSize = 13.sp) },
                        isError        = passwordError != null,
                        supportingText = { passwordError?.let { Text(it, color = errorRed, fontSize = 11.sp) } },
                        leadingIcon    = { Icon(Icons.Default.Lock, null, tint = borderFocus.copy(0.6f), modifier = Modifier.size(20.dp)) },
                        trailingIcon   = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, tint = textMuted, modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine     = true,
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(14.dp),
                        colors         = fieldColors
                    )

                    TextButton(
                        onClick  = { navController.navigate(Screen.ForgotPassword.route) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?", color = goldDk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    uiState.errorMessage?.let { errMsg ->
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(errorRed.copy(alpha = 0.07f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, null, tint = errorRed, modifier = Modifier.size(16.dp))
                                Text(errMsg, color = errorRed, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Sign In — Navy Blue bg + Golden text + Golden shine ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (uiState.isLoading)
                                    Brush.horizontalGradient(listOf(SI_NavyDark.copy(0.5f), SI_NavyDark.copy(0.5f)))
                                else navyBtnGrad
                            )
                            .clickable(enabled = !uiState.isLoading) {
                                btnPressed = true
                                viewModel.signIn()
                            }
                            .drawBehind {
                                if (!uiState.isLoading && btnPressed) drawRect(brush = shineBrush)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading)
                            CircularProgressIndicator(
                                color       = SI_GoldLight,
                                modifier    = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        else
                            Text(
                                "Sign In",
                                fontSize      = 16.sp,
                                fontWeight    = FontWeight.ExtraBold,
                                color         = SI_GoldLight,
                                letterSpacing = 1.sp
                            )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = border)
                        Text("  or continue with  ", fontSize = 11.sp, color = textMuted)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = border)
                    }
                    Spacer(Modifier.height(16.dp))

                    // ── Google Button — Full Width ─────────────────
                    OutlinedButton(
                        onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, border),
                        colors   = ButtonDefaults.outlinedButtonColors(containerColor = fieldBg),
                        enabled  = !uiState.isLoading
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            GoogleIcon(Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Continue with Google",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = textPrimary
                            )
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
            Spacer(Modifier.height(36.dp))
        }
    }
}
