package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.R
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

private val NavyDark    = Color(0xFF1A2B5E)
private val GoldPrimary = Color(0xFFC9A84C)
private val GoldLight   = Color(0xFFE8C96A)

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    var phase   by remember { mutableStateOf(false) }

    var splashDone      by remember { mutableStateOf(false) }
    var navigationDone  by remember { mutableStateOf(false) }

    val logoScale    by animateFloatAsState(
        targetValue   = if (phase) 1f else 0.80f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label         = "sc"
    )
    val logoAlpha    by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(600),
        label         = "la"
    )
    val textAlpha    by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(600, delayMillis = 200),
        label         = "ta"
    )
    val buttonsAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(600, delayMillis = 400),
        label         = "ba"
    )
    val buttonsSlide by animateFloatAsState(
        targetValue   = if (phase) 0f else 30f,
        animationSpec = tween(600, delayMillis = 400),
        label         = "bs"
    )
    val shimmer by rememberInfiniteTransition(label = "sh").animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label         = "sh2"
    )

    LaunchedEffect(Unit) {
        phase = true
        delay(2500L)
        splashDone = true
    }

    LaunchedEffect(splashDone, uiState.isAuthReady) {
        if (!splashDone) return@LaunchedEffect
        if (!uiState.isAuthReady) return@LaunchedEffect
        if (navigationDone) return@LaunchedEffect

        if (uiState.isLoggedIn && uiState.userRole.isNotEmpty()) {
            navigationDone = true
            val dest = when (uiState.userRole.lowercase()) {
                "admin" -> Screen.AdminDashboard.route
                else    -> Screen.Home.route
            }
            navController.navigate(dest) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    val showButtons = splashDone && uiState.isAuthReady && !uiState.isLoggedIn

    // ══ UI ═══════════════════════════════════════════════════════════════════
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // ── LOGO ─────────────────────────────────────────────────────────────
        Image(
            painter            = painterResource(id = R.drawable.havenhub),
            contentDescription = "HavenHub Logo",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .scale(logoScale)
                .alpha(logoAlpha)
        )

        Spacer(Modifier.height(4.dp))

        // ── BRAND NAME ───────────────────────────────────────────────────────
        Row(modifier = Modifier.alpha(textAlpha)) {
            Text(
                text          = "HAVEN",
                fontSize      = 36.sp,
                fontWeight    = FontWeight.Black,
                color         = NavyDark,
                letterSpacing = 2.sp
            )
            Text(
                text          = "HUB",
                fontSize      = 36.sp,
                fontWeight    = FontWeight.Black,
                color         = GoldPrimary,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        // ── TAGLINE ──────────────────────────────────────────────────────────
        Text(
            text          = "SMART RENTAL & VACATION STAY",
            fontSize      = 9.5.sp,
            fontWeight    = FontWeight.Medium,
            color         = NavyDark.copy(alpha = 0.45f),
            letterSpacing = 2.6.sp,
            modifier      = Modifier.alpha(textAlpha)
        )

        Spacer(Modifier.height(10.dp))

        // ── SHIMMER DIVIDER ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(1.dp)
                .alpha(textAlpha)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            GoldPrimary.copy(alpha = 0.2f + 0.8f * shimmer),
                            GoldLight,
                            GoldPrimary.copy(alpha = 0.2f + 0.8f * (1f - shimmer)),
                            Color.Transparent
                        )
                    )
                )
        )

        Spacer(Modifier.height(10.dp))

        // ── DESCRIPTION ──────────────────────────────────────────────────────
        Text(
            text       = "Verified properties for students,\nfamilies, job holders & travelers",
            fontSize   = 14.sp,
            color      = NavyDark.copy(alpha = 0.60f),
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp,
            modifier   = Modifier.alpha(textAlpha)
        )

        Spacer(Modifier.height(40.dp))

        // ── BUTTONS ──────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .alpha(if (showButtons) buttonsAlpha else 0f)
                .offset(y = buttonsSlide.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = NavyDark),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text          = "Get Started",
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = GoldPrimary,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    1.5.dp, NavyDark.copy(alpha = 0.35f)
                ),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    text          = "I Already Have an Account",
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = NavyDark,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── FOOTER ───────────────────────────────────────────────────────
            Box(
                Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(GoldPrimary.copy(alpha = 0.45f))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "A Project by",
                fontSize = 10.sp,
                color    = NavyDark.copy(alpha = 0.40f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = "Superior Group of Colleges",
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = GoldPrimary
            )
        }
    }
}
