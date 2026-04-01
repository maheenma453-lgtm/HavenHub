package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

// ── Colors ────────────────────────────────────────────────────────
private val RSWhite     = Color(0xFFFFFFFF)
private val RSNavy      = Color(0xFF1A2744)
private val RSNavyL     = Color(0xFF243258)
private val RSGold      = Color(0xFFC9A84C)
private val RSGoldL     = Color(0xFFE8C97A)
private val RSGoldD     = Color(0xFF9A7A30)
private val RSGrayText  = Color(0xFF8A8F9E)
private val RSGrayBrd   = Color(0xFFE2E6F0)
private val RSBgLight   = Color(0xFFF4F7FB)
private val RSRedError  = Color(0xFFE53935)

// Role accent colors — matching prototype
private val RSTenantBg      = Color(0xFFE8F4FD)
private val RSTenantBorder  = Color(0xFF4A9BB8)
private val RSOwnerBg       = Color(0xFFE8F5E9)
private val RSOwnerBorder   = Color(0xFF2E7D32)
private val RSAdminBg       = Color(0xFFF3E5F5)
private val RSAdminBorder   = Color(0xFF7B1FA2)
private val RSBtnTeal       = Color(0xFF1A8A8A)

@Composable
fun RoleSelectionScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var localSelectedRole by remember { mutableStateOf("") }

    // Enter animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut), label = "alpha"
    )
    val enterSlide by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(550, easing = EaseOutCubic), label = "slide"
    )

    // ── ROOT ──────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RSBgLight)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            // ── HEADER ────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(enterAlpha)
                    .offset(y = enterSlide.dp)
            ) {
                // Wave emoji like prototype
                Text("👋", fontSize = 32.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "How will you use HavenHub?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RSNavy,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select your role to get a personalized\nexperience on HavenHub",
                    fontSize = 13.sp,
                    color = RSGrayText,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── ROLE CARDS ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(enterAlpha)
                    .offset(y = (enterSlide * 1.2f).dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Tenant / Traveler
                RSRoleCard(
                    emoji = "🧳",
                    title = "Tenant / Traveler",
                    subtitle = "Search & book rental properties",
                    isSelected = localSelectedRole == "tenant",
                    selectedBg = RSTenantBg,
                    selectedBorder = RSTenantBorder,
                    onClick = {
                        localSelectedRole = "tenant"
                        viewModel.onRoleSelected("tenant")
                    }
                )

                // Property Owner
                RSRoleCard(
                    emoji = "🏠",
                    title = "Property Owner",
                    subtitle = "List & manage your properties",
                    isSelected = localSelectedRole == "landlord",
                    selectedBg = RSOwnerBg,
                    selectedBorder = RSOwnerBorder,
                    onClick = {
                        localSelectedRole = "landlord"
                        viewModel.onRoleSelected("landlord")
                    }
                )

                // Admin
                RSRoleCard(
                    emoji = "🛡️",
                    title = "Admin",
                    subtitle = "Verify & moderate platform",
                    isSelected = localSelectedRole == "admin",
                    selectedBg = RSAdminBg,
                    selectedBorder = RSAdminBorder,
                    onClick = {
                        localSelectedRole = "admin"
                        viewModel.onRoleSelected("admin")
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error
            uiState.errorMessage?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RSRedError.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Text(
                        it,
                        color = RSRedError,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── CONTINUE BUTTON ───────────────────────────────────
            Button(
                onClick = { navController.navigate(Screen.SignUp.route) },
                enabled = localSelectedRole.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .alpha(enterAlpha),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (localSelectedRole) {
                        "landlord" -> RSOwnerBorder
                        "admin"    -> RSAdminBorder
                        else       -> RSBtnTeal
                    },
                    disabledContainerColor = RSGrayBrd
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Continue  →",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RSWhite,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Already have account
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(enterAlpha)
            ) {
                Text("Already have an account? ", color = RSGrayText, fontSize = 13.sp)
                Text(
                    "Sign In",
                    color = RSBtnTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── Reusable Role Card — exactly like prototype ───────────────────
@Composable
private fun RSRoleCard(
    emoji: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    selectedBg: Color,
    selectedBorder: Color,
    onClick: () -> Unit
) {
    val bgColor     = if (isSelected) selectedBg     else RSWhite
    val borderColor = if (isSelected) selectedBorder else RSGrayBrd
    val borderWidth = if (isSelected) 2.dp           else 1.dp

    // Slight scale up on select
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(180), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Emoji icon circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) selectedBorder.copy(alpha = 0.12f)
                        else Color(0xFFF0F4F8)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) selectedBorder else RSNavy
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = RSGrayText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) selectedBorder
                        else RSGrayBrd.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = RSWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
















