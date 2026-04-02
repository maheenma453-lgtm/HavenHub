package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.R
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()

    var phase          by remember { mutableStateOf(false) }
    var delayDone      by remember { mutableStateOf(false) }
    var navigationDone by remember { mutableStateOf(false) }

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
    val bottomAlpha by animateFloatAsState(
        targetValue   = if (phase) 1f else 0f,
        animationSpec = tween(400, delayMillis = 600, easing = EaseOut), label = "ba"
    )

    // 2.2 second minimum delay
    LaunchedEffect(Unit) {
        phase = true
        delay(2200)
        delayDone = true
    }

    // ✅ FIX: Dono conditions check karo:
    // 1. Minimum 2.2s delay ho jaaye
    // 2. isLoading = false ho (Firebase auth + role fetch complete)
    // Dono hone ke baad hi navigate karo
    LaunchedEffect(delayDone, uiState.isLoading, uiState.isLoggedIn, uiState.userRole) {
        if (!delayDone) return@LaunchedEffect           // 2.2s wait karo
        if (uiState.isLoading) return@LaunchedEffect    // role fetch ho rahi hai, wait karo
        if (navigationDone) return@LaunchedEffect       // ek baar hi navigate karo

        // ✅ Agar logged in hai lekin role abhi "" hai — thoda aur wait karo
        if (uiState.isLoggedIn && uiState.userRole.isEmpty()) return@LaunchedEffect

        navigationDone = true

        val destination = when {
            !uiState.isLoggedIn                         -> Screen.Onboarding.route
            uiState.userRole.lowercase() == "admin"     -> Screen.AdminDashboard.route
            uiState.userRole.lowercase() == "landlord"  -> Screen.Home.route
            else                                        -> Screen.Home.route
        }

        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter            = painterResource(id = R.drawable.havenhub),
            contentDescription = "HavenHub",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .size(600.dp)
                .scale(logoScale)
                .alpha(logoAlpha)
        )
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(bottomAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("from", fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.5f))
            Text(
                "Superior Group of Colleges",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = Color.Gray.copy(alpha = 0.65f)
            )
        }
    }
}