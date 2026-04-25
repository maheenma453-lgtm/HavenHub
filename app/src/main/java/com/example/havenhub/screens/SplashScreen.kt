package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

// ── Brand Colors ──────────────────────────────────────────────────────────────
private val NavyDark    = Color(0xFF1A2B5E)
private val GoldPrimary = Color(0xFFC9A84C)
private val GoldLight   = Color(0xFFE8C96A)

// LIGHTER BLUE — properly visible
private val BgTop    = Color(0xFF3D5A99)   // much lighter
private val BgMid    = Color(0xFF2E4A8A)
private val BgBottom = Color(0xFF1E3570)

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState        by authViewModel.uiState.collectAsState()
    var phase          by remember { mutableStateOf(false) }
    var navigationDone by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue   = if (phase) 1f else 0.65f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ), label = "scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut), label = "la"
    )
    val textAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200, easing = EaseOut), label = "ta"
    )
    val buttonsAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(500, delayMillis = 450, easing = EaseOut), label = "ba"
    )
    val buttonsSlide by animateFloatAsState(
        targetValue   = if (phase) 0f else 30f,
        animationSpec = tween(550, delayMillis = 450, easing = EaseOutCubic), label = "bs"
    )
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
                    listOf(BgTop, BgMid, BgBottom),
                    startY = 0f, endY = 1800f
                )
            )
    ) {
        // Decorative gold glow top-right
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-80).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(GoldPrimary.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
        )
        // Decorative gold glow bottom-left
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(GoldPrimary.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 52.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── TOP BLOCK ─────────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Gold accent bar
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .alpha(textAlpha)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent, GoldPrimary,
                                    GoldLight, GoldPrimary, Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(22.dp))

                // LOGO — large and clear
                Image(
                    painter            = painterResource(id = R.drawable.havenhub),
                    contentDescription = "HavenHub Logo",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(1f)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.alpha(textAlpha)) {
                    Text(
                        text          = "HAVEN",
                        fontSize      = 36.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color.White,
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

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text          = "SMART RENTAL & VACATION STAY",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = Color.White.copy(alpha = 0.62f),
                    letterSpacing = 2.8.sp,
                    modifier      = Modifier.alpha(textAlpha)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Gold shimmer divider
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

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text       = "Verified properties for students,\nfamilies, job holders & travelers",
                    fontSize   = 13.5.sp,
                    color      = Color.White.copy(alpha = 0.75f),
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier   = Modifier.alpha(textAlpha)
                )
            }

            // ── BOTTOM BLOCK ──────────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .alpha(buttonsAlpha)
                    .offset(y = buttonsSlide.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Get Started — plain text, NO emoji
                Button(
                    onClick   = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    modifier  = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text          = "Get Started",
                        fontSize      = 15.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = NavyDark,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick  = {
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape    = RoundedCornerShape(16.dp),
                    border   = androidx.compose.foundation.BorderStroke(
                        1.5.dp, Color.White.copy(alpha = 0.35f)
                    ),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.10f)
                    )
                ) {
                    Text(
                        text          = "I Already Have an Account",
                        fontSize      = 15.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp)
                        .background(GoldPrimary.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text          = "A Project by",
                    fontSize      = 10.sp,
                    color         = Color.White.copy(alpha = 0.45f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text          = "Superior Group of Colleges",
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = GoldPrimary,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}