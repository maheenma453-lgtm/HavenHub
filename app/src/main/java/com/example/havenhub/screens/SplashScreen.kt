package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

// ── Brand Colors ────────────────────────────────────────────────────────────
private val NavyDark    = Color(0xFF1A2B5E)
private val NavyPrimary = Color(0xFF243A80)
private val NavyMid     = Color(0xFF2A4496)
private val GoldPrimary = Color(0xFFC9A84C)
private val GoldLight   = Color(0xFFE8C96A)
private val GoldDark    = Color(0xFF9A7A30)
// CREAM BG colors
private val CreamLight  = Color(0xFFFDF8F0)
private val CreamMid    = Color(0xFFF5EDD8)
private val CreamDark   = Color(0xFFEDE0C4)
private val TextMuted   = Color(0xFF8A94A6)

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState        by authViewModel.uiState.collectAsState()
    var phase          by remember { mutableStateOf(false) }
    var navigationDone by remember { mutableStateOf(false) }

    // ── Animations ──────────────────────────────────────────────────────────
    val logoScale by animateFloatAsState(
        targetValue   = if (phase) 1f else 0.65f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ), label = "scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut),
        label = "la"
    )
    val textAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200, easing = EaseOut),
        label = "ta"
    )
    val buttonsAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(500, delayMillis = 450, easing = EaseOut),
        label = "ba"
    )
    val buttonsSlide by animateFloatAsState(
        targetValue   = if (phase) 0f else 30f,
        animationSpec = tween(550, delayMillis = 450, easing = EaseOutCubic),
        label = "bs"
    )

    // Infinite shimmer on gold line
    val shimmer by rememberInfiniteTransition(label = "sh").animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label         = "shimmer"
    )

    LaunchedEffect(Unit) { phase = true }

    LaunchedEffect(uiState.isLoading, uiState.isLoggedIn, uiState.userRole) {
        if (uiState.isLoading || navigationDone || !uiState.isLoggedIn) return@LaunchedEffect
        if (uiState.userRole.isEmpty()) return@LaunchedEffect
        navigationDone = true
        val dest = when (uiState.userRole.lowercase()) {
            "admin" -> Screen.AdminDashboard.route
            else    -> Screen.Home.route
        }
        navController.navigate(dest) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CreamLight, CreamMid, CreamDark),  // CREAM background
                    startY = 0f, endY = 1800f
                )
            )
    ) {
        // ── Decorative background rings ──────────────────────────────────────
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-60).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(GoldPrimary.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(GoldPrimary.copy(alpha = 0.13f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(GoldPrimary.copy(alpha = 0.12f))
        )

        // ── Main Content ─────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Gold top accent line
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .alpha(textAlpha)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GoldPrimary, GoldLight, GoldPrimary, Color.Transparent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // FIX: Logo size increased from 0.68f to 0.85f so it's clearly visible
            Image(
                painter            = painterResource(id = R.drawable.havenhub),
                contentDescription = "HavenHub Logo",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxWidth(0.85f)   // FIX: was 0.68f — now much bigger
                    .aspectRatio(1f)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // HavenHub wordmark
            Row(modifier = Modifier.alpha(textAlpha)) {
                Text(
                    "HAVEN",
                    fontSize      = 34.sp,
                    fontWeight    = FontWeight.Black,
                    color         = NavyDark,   // Navy on cream bg
                    letterSpacing = 2.sp
                )
                Text(
                    "HUB",
                    fontSize      = 34.sp,
                    fontWeight    = FontWeight.Black,
                    color         = GoldPrimary,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                "SMART RENTAL & VACATION STAY",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                color         = NavyDark.copy(alpha = 0.55f),  // Navy on cream
                letterSpacing = 2.8.sp,
                modifier      = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Animated gold shimmer divider
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(1.dp)
                    .alpha(textAlpha)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GoldPrimary.copy(alpha = 0.3f + 0.7f * shimmer),
                                GoldLight,
                                GoldPrimary.copy(alpha = 0.3f + 0.7f * (1f - shimmer)),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description
            Text(
                "Verified properties for students,\nfamilies, job holders & travelers",
                fontSize   = 13.5.sp,
                color      = NavyDark.copy(alpha = 0.65f),  // Navy on cream
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp,
                modifier   = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(44.dp))

            // ── Get Started Button ───────────────────────────────────────────
            Button(
                onClick   = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(buttonsAlpha)
                    .offset(y = buttonsSlide.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                // FIX: Removed stray  character — clean "Get Started" text only
                Text(
                    "Get Started",
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = NavyDark,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("→", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Already Have Account Button ──────────────────────────────────
            OutlinedButton(
                onClick  = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(buttonsAlpha)
                    .offset(y = buttonsSlide.dp),
                shape    = RoundedCornerShape(16.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    1.5.dp, NavyDark.copy(alpha = 0.25f)  // Navy border on cream
                ),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = NavyDark.copy(alpha = 0.08f)
                )
            ) {
                Text(
                    "I Already Have an Account",
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = NavyDark,  // Navy text on cream
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Bottom credit
            Column(
                modifier            = Modifier.alpha(buttonsAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp)
                        .background(GoldPrimary.copy(alpha = 0.35f))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "A Project by",
                    fontSize      = 10.sp,
                    color         = NavyDark.copy(alpha = 0.45f),  // Navy on cream
                    letterSpacing = 0.5.sp
                )
                // FIX: "Superior Group of Colleges" ab Golden color mein
                Text(
                    "Superior Group of Colleges",
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = GoldPrimary,   // FIX: was White.copy(0.42f), now Gold!
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}



