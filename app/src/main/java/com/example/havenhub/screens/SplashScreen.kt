package com.example.havenhub.screens

import androidx.compose.animation.core.*
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
import com.example.havenhub.ui.theme.BackgroundWhite
import com.example.havenhub.ui.theme.PrimaryBlue
import com.example.havenhub.ui.theme.PrimaryBlueDark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = EaseInOut),
        label = "splashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutBack),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        // ✅ Splash ke baad Onboarding par bhejna hai
        navController.navigate(Screen.Onboarding.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(PrimaryBlue, PrimaryBlueDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.scale(scaleAnim).alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(100.dp).background(color = BackgroundWhite.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏠", fontSize = 48.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "HavenHub", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = BackgroundWhite)
            Text(text = "Your Perfect Home, One Tap Away", fontSize = 14.sp, color = BackgroundWhite.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}