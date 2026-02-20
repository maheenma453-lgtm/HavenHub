package com.example.havenhub.screens



import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────
// SplashScreen.kt
// PURPOSE : First screen shown on app launch.
//           Displays logo + tagline with fade-in animation,
//           then navigates to Onboarding after 2.5 seconds.
// NAVIGATION: SplashScreen → OnboardingScreen
// ─────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(navController: NavController) {

    // ── Animation States ──────────────────────────────────────────
    // Controls fade-in effect for logo and tagline
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue    = if (startAnimation) 1f else 0f,
        animationSpec  = tween(durationMillis = 1200, easing = EaseInOut),
        label          = "splashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutBack),
        label         = "splashScale"
    )

    // ── Side Effect: Start anim then navigate ─────────────────────
    LaunchedEffect(Unit) {
        startAnimation = true          // Trigger animation
        delay(2500)                    // Wait 2.5 seconds
        navController.navigate(Screen.Onboarding.route) {
            // Remove splash from back stack so user can't go back to it
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    // ── UI ────────────────────────────────────────────────────────
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

        Column(
            modifier            = Modifier
                .scale(scaleAnim)
                .alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── App Logo Icon (placeholder box) ───────────────────
            // Replace with actual Image(painter = painterResource(R.drawable.ic_splash_logo))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = BackgroundWhite.copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = "🏠",
                    fontSize  = 48.sp,
                    textAlign = TextAlign.Center
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

        // ── Bottom Version Text ───────────────────────────────────
        Text(
            text      = "v1.0.0",
            fontSize  = 12.sp,
            color     = BackgroundWhite.copy(alpha = 0.5f),
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
