package com.example.havenhub.screens

// ─────────────────────────────────────────────────────────────────
// SplashScreen.kt
//
// PURPOSE    : App launch par sabse pehle dikhne wali screen.
//              Logo aur tagline fade-in animation ke saath show hoti hai,
//              phir 2.5 seconds baad Onboarding screen par navigate karti hai.
//
// NAVIGATION : SplashScreen → OnboardingScreen
// ─────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.AccentNavy
import com.example.havenhub.ui.theme.BackgroundWhite
import com.example.havenhub.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    // ── Animation State ───────────────────────────────────────────
    // startAnimation false se true hone par animations trigger hoti hain
    var startAnimation by remember { mutableStateOf(false) }

    // Logo aur tagline ka fade-in effect (0f → 1f)
    val alphaAnim by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = EaseInOut),
        label         = "splashAlpha"
    )

    // Logo ka scale-up effect (0.8f → 1f) with bounce
    val scaleAnim by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutBack),
        label         = "splashScale"
    )

    // ── Side Effect: Animation start karo, phir navigate karo ────
    LaunchedEffect(Unit) {
        startAnimation = true   // Animations trigger karo
        delay(2500)             // 2.5 seconds ruko
        navController.navigate(Screen.Onboarding.route) {
            // Splash ko back stack se hata do taaki user wapas na ja sake
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    // ── Root Container ────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryBlue, AccentNavy)
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── Center Content: Logo + App Name + Tagline ─────────────
        Column(
            modifier = Modifier
                .scale(scaleAnim)
                .alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── App Logo ──────────────────────────────────────────
            // TODO: Replace emoji with actual logo:
            //       Image(painter = painterResource(R.drawable.ic_splash_logo), ...)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = BackgroundWhite.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = "🏠",
                    fontSize = 48.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── App Name ──────────────────────────────────────────
            Text(
                text       = "HavenHub",
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = BackgroundWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tagline ───────────────────────────────────────────
            Text(
                text      = "Your Perfect Home, One Tap Away",
                fontSize  = 14.sp,
                color     = BackgroundWhite.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        // ── Bottom: Version Number ────────────────────────────────
        Text(
            text     = "v1.0.0",
            fontSize = 12.sp,
            color    = BackgroundWhite.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}