package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel

// Role accent colors — yeh semantic hain, theme se bahar hain
private val RS_TenantAccent   = Color(0xFF1A6FA8)
private val RS_LandlordAccent = Color(0xFF2E7D52)
private val RS_AdminAccent    = Color(0xFF6A3AAF)

@Composable
fun RoleSelectionScreen(
    navController: NavController,
    viewModel    : AuthViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    var localSelectedRole by remember { mutableStateOf("") }
    var visible           by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val fadeAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut),
        label         = "fadeAlpha"
    )
    val slideY by animateFloatAsState(
        targetValue   = if (visible) 0f else 30f,
        animationSpec = tween(550, easing = EaseOutCubic),
        label         = "slideY"
    )

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val background       = MaterialTheme.colorScheme.background
    val onBackground     = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline          = MaterialTheme.colorScheme.outline
    val error            = MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(primary, primaryContainer),
                            startY = 0f, endY = 500f
                        )
                    )
            ) {
                // Decorative ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(tertiary.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 32.dp)
                        .graphicsLayer { alpha = fadeAlpha }
                        .offset(y = slideY.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row {
                        Text(
                            "HAVEN",
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Black,
                            color         = onPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            "HUB",
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Black,
                            color         = tertiary,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(tertiary, tertiary.copy(0.6f)))
                            )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "How will you use\nHavenHub?",
                        fontSize      = 24.sp,
                        fontWeight    = FontWeight.Black,
                        color         = onPrimary,
                        textAlign     = TextAlign.Center,
                        lineHeight    = 32.sp,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Select your role to get a personalized experience",
                        fontSize   = 13.sp,
                        color      = onPrimary.copy(alpha = 0.55f),
                        textAlign  = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.BottomCenter)
                        .background(background)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Role Cards ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer { alpha = fadeAlpha }
                    .offset(y = slideY.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RSRoleCard(
                    emoji       = "🧳",
                    title       = "Tenant / Traveler",
                    subtitle    = "Search & book verified rental properties",
                    isSelected  = localSelectedRole == "tenant",
                    accentColor = RS_TenantAccent,
                    surfaceColor = MaterialTheme.colorScheme.surface,
                    borderColor  = outline,
                    subtitleColor = onSurfaceVariant,
                    onClick     = {
                        localSelectedRole = "tenant"
                        viewModel.onRoleSelected("tenant")
                    }
                )
                RSRoleCard(
                    emoji       = "🏠",
                    title       = "Landlord",
                    subtitle    = "List & manage your rental properties",
                    isSelected  = localSelectedRole == "landlord",
                    accentColor = RS_LandlordAccent,
                    surfaceColor = MaterialTheme.colorScheme.surface,
                    borderColor  = outline,
                    subtitleColor = onSurfaceVariant,
                    onClick     = {
                        localSelectedRole = "landlord"
                        viewModel.onRoleSelected("landlord")
                    }
                )
                RSRoleCard(
                    emoji       = "🛡️",
                    title       = "Admin",
                    subtitle    = "Verify & moderate the platform",
                    isSelected  = localSelectedRole == "admin",
                    accentColor = RS_AdminAccent,
                    surfaceColor = MaterialTheme.colorScheme.surface,
                    borderColor  = outline,
                    subtitleColor = onSurfaceVariant,
                    onClick     = {
                        localSelectedRole = "admin"
                        viewModel.onRoleSelected("admin")
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error message
            uiState.errorMessage?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(error.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Text(
                        it,
                        color     = error,
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── Continue Button ───────────────────────────────────────────────
            Button(
                onClick   = {
                    navController.navigate(Screen.SignUp.createRoute(localSelectedRole))
                },
                enabled   = localSelectedRole.isNotEmpty(),
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(54.dp)
                    .graphicsLayer { alpha = fadeAlpha },
                shape     = RoundedCornerShape(15.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor         = when (localSelectedRole) {
                        "landlord" -> RS_LandlordAccent
                        "admin"    -> RS_AdminAccent
                        else       -> primary
                    },
                    disabledContainerColor = outline
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text          = "Continue",
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = onPrimary,
                    letterSpacing = 0.4.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint               = onPrimary,
                    modifier           = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.graphicsLayer { alpha = fadeAlpha }
            ) {
                Text("Already have an account? ", color = onSurfaceVariant, fontSize = 13.sp)
                Text(
                    "Sign In",
                    color      = tertiary,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable {
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

// ── Role Card Component ───────────────────────────────────────────────────────
@Composable
private fun RSRoleCard(
    emoji        : String,
    title        : String,
    subtitle     : String,
    isSelected   : Boolean,
    accentColor  : Color,
    surfaceColor : Color,
    borderColor  : Color,
    subtitleColor: Color,
    onClick      : () -> Unit
) {
    val bgColor     = if (isSelected) accentColor.copy(alpha = 0.07f) else surfaceColor
    val borderClr   = if (isSelected) accentColor else borderColor
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(borderWidth, borderClr, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text       = subtitle,
                    fontSize   = 12.sp,
                    color      = subtitleColor,
                    lineHeight = 17.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) accentColor
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimary,
                        modifier           = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}