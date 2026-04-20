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

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()

    var phase          by remember { mutableStateOf(false) }
    var navigationDone by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue   = if (phase) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ), label = "scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(400, easing = EaseOut),
        label = "alpha"
    )
    val buttonsAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(400, delayMillis = 400, easing = EaseOut),
        label = "ba"
    )

    // ✅ Sirf animation ke liye - navigate NAHI karta
    LaunchedEffect(Unit) {
        phase = true
    }

    // ✅ Sirf tab navigate karo jab already logged in ho
    // Logged out user ke liye buttons se navigate hoga
    LaunchedEffect(uiState.isLoading, uiState.isLoggedIn, uiState.userRole) {
        if (uiState.isLoading) return@LaunchedEffect
        if (navigationDone) return@LaunchedEffect
        if (!uiState.isLoggedIn) return@LaunchedEffect  // ✅ Logged out = kuch mat karo
        if (uiState.userRole.isEmpty()) return@LaunchedEffect

        // ✅ Sirf logged IN user ko navigate karo
        navigationDone = true
        val destination = when (uiState.userRole.lowercase()) {
            "admin" -> Screen.AdminDashboard.route
            else    -> Screen.Home.route
        }
        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    // ✅ Responsive - scrollable column
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ Logo - fillMaxWidth se responsive
            Image(
                painter            = painterResource(id = R.drawable.havenhub),
                contentDescription = "HavenHub",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxWidth(0.75f)   // ✅ Screen width ka 75%
                    .aspectRatio(1f)       // ✅ Square ratio maintain kare
                    .scale(logoScale)
                    .alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // HavenHub text
            Row(modifier = Modifier.alpha(contentAlpha)) {
                Text(
                    "Haven",
                    fontSize      = 32.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color(0xFF1A3A6B),
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Hub",
                    fontSize      = 32.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color(0xFFC9A84C),
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SMART RENTAL subtitle
            Text(
                "SMART RENTAL & VACATION STAY",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Medium,
                color         = Color(0xFF7A8BA6),
                letterSpacing = 1.5.sp,
                modifier      = Modifier.alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Gold accent line
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .alpha(contentAlpha)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFFC9A84C),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Description text
            Text(
                "Find verified rental properties for students,\nfamilies, job holders & vacation travelers",
                fontSize   = 13.sp,
                color      = Color(0xFF7A8BA6),
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
                modifier   = Modifier.alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ✅ Get Started button
            Button(
                onClick   = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .alpha(buttonsAlpha),
                shape     = RoundedCornerShape(16.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A3A6B)
                ),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Text(
                    "Get Started →",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Already have account button
            OutlinedButton(
                onClick  = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .alpha(buttonsAlpha),
                shape    = RoundedCornerShape(16.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    1.5.dp, Color(0xFF1A3A6B)
                ),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    "I Already Have Account",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1A3A6B)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom credit
            Column(
                modifier            = Modifier.alpha(buttonsAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "from",
                    fontSize = 11.sp,
                    color    = Color.Gray.copy(alpha = 0.5f)
                )
                Text(
                    "Superior Group of Colleges",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Color.Gray.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}



















