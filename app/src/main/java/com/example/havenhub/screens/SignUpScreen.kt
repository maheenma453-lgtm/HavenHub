package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel

// ── Brand Colors ────────────────────────────────────────────────────────────
private val SU_NavyDark    = Color(0xFF0D1B3E)
private val SU_NavyPrimary = Color(0xFF1A2A6C)
private val SU_GoldPrimary = Color(0xFFC9A84C)
private val SU_GoldLight   = Color(0xFFE8C96A)
private val SU_GoldDark    = Color(0xFF9A7A30)
private val SU_White       = Color(0xFFFDFBF7)
private val SU_Surface     = Color(0xFFF4F6FB)
private val SU_TextPrimary = Color(0xFF0D1B3E)
private val SU_TextMuted   = Color(0xFF8A94A6)
private val SU_BorderGray  = Color(0xFFDDE2EF)
private val SU_ErrorRed    = Color(0xFFD94040)

@Composable
fun SignUpScreen(
    navController: NavController,
    selectedRole : String        = "",
    viewModel    : AuthViewModel = hiltViewModel()
) {
    LaunchedEffect(selectedRole) {
        if (selectedRole.isNotEmpty()) viewModel.onRoleSelected(selectedRole)
    }

    val uiState         by viewModel.uiState.collectAsState()
    val fullName        by viewModel.fullName.collectAsState()
    val email           by viewModel.email.collectAsState()
    val password        by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val nameError       by viewModel.nameError.collectAsState()
    val emailError      by viewModel.emailError.collectAsState()
    val passwordError   by viewModel.passwordError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }
    var visible         by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val heroAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut), label = "ha"
    )
    val cardAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 180, easing = EaseOut), label = "ca"
    )
    val cardSlide by animateFloatAsState(
        targetValue   = if (visible) 0f else 32f,
        animationSpec = tween(550, delayMillis = 180, easing = EaseOutCubic), label = "cs"
    )

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            val dest = when (uiState.userRole.lowercase()) {
                "admin" -> Screen.AdminDashboard.route
                else    -> Screen.Home.route
            }
            navController.navigate(dest) { popUpTo(0) { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SU_Surface)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Hero Header ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(heroAlpha)
                    .background(
                        Brush.verticalGradient(
                            listOf(SU_NavyDark, SU_NavyPrimary),
                            startY = 0f, endY = 700f
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(SU_GoldPrimary.copy(alpha = 0.1f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 36.dp)
                ) {
                    // FIX: Back button + clean HAVENHUB wordmark (no logo image)
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack, null,
                                tint     = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // FIX: Logo image removed — only text wordmark
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "HAVEN",
                                fontSize      = 18.sp,
                                fontWeight    = FontWeight.Black,
                                color         = Color.White,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                "HUB",
                                fontSize      = 18.sp,
                                fontWeight    = FontWeight.Black,
                                color         = SU_GoldPrimary,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Create Account",
                        fontSize      = 26.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color.White,
                        letterSpacing = (-0.3).sp,
                        modifier      = Modifier.padding(start = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Join thousands of happy renters",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Role badge + gold bar
                    Row(
                        modifier          = Modifier.padding(start = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp).height(3.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SU_GoldDark, SU_GoldPrimary, SU_GoldLight)
                                    )
                                )
                        )
                        if (uiState.selectedRole.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SU_GoldPrimary.copy(alpha = 0.18f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text       = uiState.selectedRole.replaceFirstChar { it.uppercase() },
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = SU_GoldLight
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.BottomCenter)
                        .background(SU_Surface)
                )
            }

            // ── Form Card ────────────────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-12).dp)
                    .alpha(cardAlpha)
                    .offset(y = cardSlide.dp),
                shape     = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors    = CardDefaults.cardColors(containerColor = SU_White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        "Your Details",
                        fontSize      = 18.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = SU_NavyPrimary,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Fill in your information to get started",
                        fontSize = 12.sp,
                        color    = SU_TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp).height(3.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(SU_GoldDark, SU_GoldPrimary, SU_GoldLight)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = SU_NavyPrimary,
                        focusedLabelColor    = SU_NavyPrimary,
                        unfocusedBorderColor = SU_BorderGray,
                        unfocusedLabelColor  = SU_TextMuted,
                        errorBorderColor     = SU_ErrorRed,
                        cursorColor          = SU_NavyPrimary
                    )

                    // Full Name
                    OutlinedTextField(
                        value          = fullName,
                        onValueChange  = { viewModel.onFullNameChange(it) },
                        label          = { Text("Full Name", fontSize = 13.sp) },
                        isError        = nameError != null,
                        supportingText = { nameError?.let { Text(it, color = SU_ErrorRed, fontSize = 11.sp) } },
                        leadingIcon    = {
                            Icon(Icons.Default.Person, null, tint = SU_NavyPrimary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        },
                        singleLine     = true,
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(14.dp),
                        colors         = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Email
                    OutlinedTextField(
                        value           = email,
                        onValueChange   = { viewModel.onEmailChange(it) },
                        label           = { Text("Email Address", fontSize = 13.sp) },
                        isError         = emailError != null,
                        supportingText  = { emailError?.let { Text(it, color = SU_ErrorRed, fontSize = 11.sp) } },
                        leadingIcon     = {
                            Icon(Icons.Default.Email, null, tint = SU_NavyPrimary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(14.dp),
                        colors          = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Password
                    OutlinedTextField(
                        value                = password,
                        onValueChange        = { viewModel.onPasswordChange(it) },
                        label                = { Text("Password", fontSize = 13.sp) },
                        isError              = passwordError != null,
                        supportingText       = { passwordError?.let { Text(it, color = SU_ErrorRed, fontSize = 11.sp) } },
                        leadingIcon          = {
                            Icon(Icons.Default.Lock, null, tint = SU_NavyPrimary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        },
                        trailingIcon         = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, tint = SU_TextMuted, modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine           = true,
                        modifier             = Modifier.fillMaxWidth(),
                        shape                = RoundedCornerShape(14.dp),
                        colors               = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value                = confirmPassword,
                        onValueChange        = { viewModel.onConfirmPasswordChange(it) },
                        label                = { Text("Confirm Password", fontSize = 13.sp) },
                        isError              = confirmPassword.isNotEmpty() && password != confirmPassword,
                        supportingText       = {
                            if (confirmPassword.isNotEmpty() && password != confirmPassword)
                                Text("Passwords do not match", color = SU_ErrorRed, fontSize = 11.sp)
                        },
                        leadingIcon          = {
                            Icon(Icons.Default.LockOpen, null, tint = SU_NavyPrimary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        },
                        trailingIcon         = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, tint = SU_TextMuted, modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine           = true,
                        modifier             = Modifier.fillMaxWidth(),
                        shape                = RoundedCornerShape(14.dp),
                        colors               = fieldColors
                    )

                    // Error box
                    uiState.errorMessage?.let { errMsg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SU_ErrorRed.copy(alpha = 0.07f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning, null,
                                    tint     = SU_ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(errMsg, color = SU_ErrorRed, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Create Account button
                    Button(
                        onClick   = { viewModel.signUp() },
                        enabled   = !uiState.isLoading,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape     = RoundedCornerShape(15.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor         = SU_NavyPrimary,
                            disabledContainerColor = SU_BorderGray
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Create Account",
                                fontSize      = 15.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = Color.White,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }
            }

            // ── Sign In link ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(top = 4.dp, bottom = 36.dp)
            ) {
                Text("Already have an account? ", color = SU_TextMuted, fontSize = 13.sp)
                Text(
                    "Sign In",
                    color      = SU_GoldDark,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable {
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}







