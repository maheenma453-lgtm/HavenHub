package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.havenhub.R
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    var phase by remember { mutableStateOf(false) }

    // Logo — WhatsApp style spring pop
    val logoScale by animateFloatAsState(
        targetValue   = if (phase) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ), label = "scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(400, easing = EaseOut), label = "alpha"
    )

    // Bottom "from" text
    val bottomAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(400, delayMillis = 600, easing = EaseOut), label = "ba"
    )

    LaunchedEffect(Unit) {
        phase = true
        delay(2200)
        navController.navigate(Screen.Onboarding.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    // ── Pure white background — exactly WhatsApp style ────────────
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Just the logo — big, centered, spring pop
        Image(
            painter            = painterResource(id = R.drawable.havenhub),
            contentDescription = "HavenHub",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .size(600.dp)
                .scale(logoScale)
                .alpha(logoAlpha)
        )

        // "from Superior..." bottom — exactly WhatsApp "from Meta"
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(bottomAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "from",
                fontSize  = 11.sp,
                color     = Color.Gray.copy(alpha = 0.5f)
            )
            Text(
                "Superior Group of Colleges",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = Color.Gray.copy(alpha = 0.65f)
            )
        }
    }
}




































