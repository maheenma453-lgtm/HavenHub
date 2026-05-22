package com.example.havenhub.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.AuthViewModel

// ════════════════════════════════════════════════════════════════════
// LIGHT THEME TOKENS
// ════════════════════════════════════════════════════════════════════
private val SU_NavyDark    = Color(0xFF0D1B3E)
private val SU_NavyPrimary = Color(0xFF1A2A6C)
private val SU_NavyMid     = Color(0xFF1E3A8A)
private val SU_GoldPrimary = Color(0xFFC9A84C)
private val SU_GoldLight   = Color(0xFFE8C96A)
private val SU_GoldDark    = Color(0xFF9A7A30)
private val SU_White       = Color(0xFFFDFBF7)
private val SU_Surface     = Color(0xFFF4F6FB)
private val SU_TextMuted   = Color(0xFF8A94A6)
private val SU_BorderGray  = Color(0xFFDDE2EF)
private val SU_ErrorRed    = Color(0xFFD94040)

// ════════════════════════════════════════════════════════════════════
// DARK THEME TOKENS
// ════════════════════════════════════════════════════════════════════
private val SUK_BgDeep      = Color(0xFF060D1A)
private val SUK_BgPrimary   = Color(0xFF0D1B3E)
private val SUK_BgCard      = Color(0xFF112038)
private val SUK_BgField     = Color(0xFF0F1D38)
private val SUK_GoldPrimary = Color(0xFFD4AF37)
private val SUK_GoldLight   = Color(0xFFF5D060)
private val SUK_GoldDark    = Color(0xFFB8962E)
private val SUK_TextPrimary = Color(0xFFF0F4FF)
private val SUK_TextMuted   = Color(0xFF6A7A9A)
private val SUK_Border      = Color(0xFF1E2E50)
private val SUK_ErrorRed    = Color(0xFFCF6679)

@Composable
fun SignUpScreen(
    navController: NavController,
    selectedRole : String        = "",
    viewModel    : AuthViewModel = hiltViewModel()
) {
    LaunchedEffect(selectedRole) {
        if (selectedRole.isNotEmpty()) viewModel.onRoleSelected(selectedRole)
    }

    val uiState by viewModel.uiState.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val cnicNumber by viewModel.cnicNumber.collectAsState()
    val cnicImageUri by viewModel.cnicImageUri.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val cnicError by viewModel.cnicError.collectAsState()
    // NEW: profile image error
    val profileImageError by viewModel.profileImageError.collectAsState()

    val role = uiState.selectedRole.lowercase()

    // CNIC label — teeno roles ke liye
    val cnicVerificationLabel = when (role) {
        "admin" -> "Required for admin verification"
        "landlord" -> "Required for landlord verification"
        else -> "Required for tenant verification"
    }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var btnPressed by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) { visible = true }

    val heroAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut), label = "ha"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 180, easing = EaseOut), label = "ca"
    )
    val cardSlide by animateFloatAsState(
        targetValue = if (visible) 0f else 32f,
        animationSpec = tween(550, delayMillis = 180, easing = EaseOutCubic), label = "cs"
    )

    val shineOffset by animateFloatAsState(
        targetValue = if (btnPressed) 1.5f else -0.5f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        finishedListener = { btnPressed = false },
        label = "shine"
    )

    // ── Theme tokens ──────────────────────────────────────────────
    val screenBg = if (isDark) SUK_BgDeep else SU_Surface
    val heroGrad = if (isDark)
        Brush.verticalGradient(listOf(SUK_BgDeep, SUK_BgPrimary))
    else
        Brush.verticalGradient(listOf(SU_NavyDark, SU_NavyPrimary))
    val cardBg = if (isDark) SUK_BgCard else SU_White
    val fieldBg = if (isDark) SUK_BgField else SU_White
    val goldP = if (isDark) SUK_GoldPrimary else SU_GoldPrimary
    val goldL = if (isDark) SUK_GoldLight else SU_GoldLight
    val goldDk = if (isDark) SUK_GoldDark else SU_GoldDark
    val textPrimary = if (isDark) SUK_TextPrimary else Color(0xFF0D1B3E)
    val textMuted = if (isDark) SUK_TextMuted else SU_TextMuted
    val border = if (isDark) SUK_Border else SU_BorderGray
    val borderFocus = if (isDark) SUK_GoldPrimary else SU_NavyPrimary
    val errorRed = if (isDark) SUK_ErrorRed else SU_ErrorRed

    val shineBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            SU_GoldLight.copy(alpha = 0.55f),
            SU_GoldPrimary.copy(alpha = 0.35f),
            Color.Transparent
        ),
        start = Offset(shineOffset * 900f - 250f, 0f),
        end = Offset(shineOffset * 900f + 150f, 180f)
    )

    val profileImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> viewModel.onProfileImageSelected(uri) }

    val cnicImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> viewModel.onCnicImageSelected(uri) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            val dest = when (uiState.userRole.lowercase()) {
                "admin" -> Screen.AdminDashboard.route
                else -> Screen.Home.route
            }
            navController.navigate(dest) { popUpTo(0) { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(screenBg)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Hero Header ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(heroAlpha)
                    .background(heroGrad)
            ) {
                Box(
                    Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    goldP.copy(0.9f),
                                    goldL.copy(0.4f),
                                    goldP.copy(0.9f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier.size(160.dp).align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-30).dp).clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    goldP.copy(0.08f),
                                    Color.Transparent
                                )
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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack, null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "HAVEN",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                "HUB",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = goldP,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Create Account",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Join thousands of happy renters",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(36.dp).height(3.dp).clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(goldDk, goldP, goldL)))
                        )
                        if (uiState.selectedRole.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(goldP.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    uiState.selectedRole.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = goldL
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
                        .background(screenBg)
                )
            }

            // ── Form Card ─────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-12).dp)
                    .alpha(cardAlpha)
                    .offset(y = cardSlide.dp),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 10.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                if (isDark) Box(
                    Modifier.fillMaxWidth().height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    goldP.copy(0.8f),
                                    goldL.copy(0.3f),
                                    goldP.copy(0.8f)
                                )
                            )
                        )
                )

                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        "Your Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) SUK_TextPrimary else SU_NavyPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Fill in your information to get started",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                    Spacer(Modifier.height(20.dp))

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderFocus,
                        focusedLabelColor = borderFocus,
                        unfocusedBorderColor = border,
                        unfocusedLabelColor = textMuted,
                        errorBorderColor = errorRed,
                        cursorColor = borderFocus,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                    )

                    // ══════════════════════════════════════════════
                    // PROFILE IMAGE — COMPULSORY FOR ALL ROLES
                    // ══════════════════════════════════════════════
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Profile Photo",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) SUK_TextPrimary else SU_NavyPrimary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    // Red asterisk — compulsory
                                    Text(
                                        "*",
                                        fontSize = 13.sp,
                                        color = errorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("Required for all users", fontSize = 11.sp, color = textMuted)
                            }

                            // Profile image circle
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (profileImageUri != null)
                                            Color.Transparent
                                        else if (isDark) SUK_Border else SU_BorderGray
                                    )
                                    .border(
                                        width = if (profileImageUri != null) 2.5.dp else if (profileImageError != null) 2.dp else 1.5.dp,
                                        color = when {
                                            profileImageUri != null -> goldP
                                            profileImageError != null -> errorRed
                                            else -> border
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable { profileImageLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (profileImageUri != null) {
                                    AsyncImage(
                                        model = profileImageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Edit overlay badge
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(goldP)
                                            .border(1.5.dp, cardBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            null,
                                            tint = SU_NavyDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt, null,
                                            tint = if (profileImageError != null) errorRed else textMuted,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "Add *",
                                            fontSize = 9.sp,
                                            color = if (profileImageError != null) errorRed else textMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Success row when image selected
                        if (profileImageUri != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    "Profile photo added",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50)
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "Change",
                                    fontSize = 11.sp,
                                    color = goldP,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { profileImageLauncher.launch("image/*") }
                                )
                            }
                        }

                        // Error message
                        if (profileImageError != null && profileImageUri == null) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    null,
                                    tint = errorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(profileImageError!!, fontSize = 11.sp, color = errorRed)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = border.copy(alpha = 0.5f))
                    Spacer(Modifier.height(20.dp))

                    // ── Full Name ─────────────────────────────────
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { viewModel.onFullNameChange(it) },
                        label = { Text("Full Name *", fontSize = 13.sp) },
                        isError = nameError != null,
                        supportingText = {
                            nameError?.let {
                                Text(
                                    it,
                                    color = errorRed,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                null,
                                tint = borderFocus.copy(0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(10.dp))

                    // ── Email ─────────────────────────────────────
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email Address *", fontSize = 13.sp) },
                        isError = emailError != null,
                        supportingText = {
                            emailError?.let {
                                Text(
                                    it,
                                    color = errorRed,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                null,
                                tint = borderFocus.copy(0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(10.dp))

                    // ── Password ──────────────────────────────────
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Password *", fontSize = 13.sp) },
                        isError = passwordError != null,
                        supportingText = {
                            passwordError?.let {
                                Text(
                                    it,
                                    color = errorRed,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                null,
                                tint = borderFocus.copy(0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(10.dp))

                    // ── Confirm Password ──────────────────────────
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChange(it) },
                        label = { Text("Confirm Password *", fontSize = 13.sp) },
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        supportingText = {
                            if (confirmPassword.isNotEmpty() && password != confirmPassword)
                                Text("Passwords do not match", color = errorRed, fontSize = 11.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LockOpen,
                                null,
                                tint = borderFocus.copy(0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )

                    // ══════════════════════════════════════════════
                    // CNIC SECTION — COMPULSORY FOR ALL 3 ROLES
                    // ══════════════════════════════════════════════
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = border)
                    Spacer(Modifier.height(16.dp))

                    // Section header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(goldP.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                null,
                                tint = goldP,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Identity Verification",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) SUK_TextPrimary else SU_NavyPrimary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "*",
                                    fontSize = 14.sp,
                                    color = errorRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(cnicVerificationLabel, fontSize = 11.sp, color = textMuted)
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // CNIC number field
                    OutlinedTextField(
                        value = cnicNumber,
                        onValueChange = { viewModel.onCnicNumberChange(it) },
                        label = { Text("CNIC Number *", fontSize = 13.sp) },
                        isError = cnicError != null,
                        supportingText = {
                            cnicError?.let {
                                Text(
                                    it,
                                    color = errorRed,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Badge,
                                null,
                                tint = borderFocus.copy(0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        placeholder = {
                            Text(
                                "e.g. 35201-1234567-1",
                                fontSize = 12.sp,
                                color = textMuted
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(14.dp))

                    Text(
                        "CNIC Image *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) SUK_TextPrimary else SU_NavyPrimary
                    )
                    Spacer(Modifier.height(8.dp))

                    // CNIC image — uploaded state
                    if (cnicImageUri != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = cnicImageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, goldP.copy(0.4f), RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.onCnicImageSelected(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(30.dp)
                                    .background(Color.Red.copy(0.85f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Text("CNIC image uploaded", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        }
                    } else {
                        // CNIC image — upload button
                        OutlinedButton(
                            onClick = { cnicImageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Brush.horizontalGradient(
                                    listOf(goldP.copy(0.5f), goldL.copy(0.3f), goldP.copy(0.5f))
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) goldP else SU_NavyPrimary,
                                containerColor = fieldBg
                            )
                        ) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Upload CNIC Photo (JPG/PNG) *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Info box
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1A1608) else Color(0xFFFFF8E1))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = if (isDark) goldP else Color(0xFFD4AF37),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val infoText = when (role) {
                            "admin" -> "Your CNIC will be verified before admin account activation."
                            "landlord" -> "Admin will verify your CNIC before you can list properties."
                            else -> "Admin will verify your CNIC before account activation."
                        }
                        Text(
                            infoText,
                            fontSize = 11.sp,
                            color = if (isDark) goldP.copy(0.7f) else Color(0xFF8A7040)
                        )
                    }

                    // ── Global Error box ──────────────────────────
                    uiState.errorMessage?.let { errMsg ->
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(errorRed.copy(alpha = 0.07f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = errorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(errMsg, color = errorRed, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Create Account Button ─────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (uiState.isLoading)
                                    Brush.horizontalGradient(
                                        listOf(
                                            SU_NavyDark.copy(0.5f),
                                            SU_NavyDark.copy(0.5f)
                                        )
                                    )
                                else if (isDark)
                                    Brush.horizontalGradient(
                                        listOf(
                                            SUK_BgPrimary,
                                            Color(0xFF1A2F70),
                                            SUK_BgPrimary
                                        )
                                    )
                                else
                                    Brush.horizontalGradient(
                                        listOf(
                                            SU_NavyDark,
                                            SU_NavyPrimary,
                                            SU_NavyMid,
                                            SU_NavyPrimary,
                                            SU_NavyDark
                                        )
                                    )
                            )
                            .clickable(enabled = !uiState.isLoading) {
                                btnPressed = true
                                viewModel.signUp()
                            }
                            .drawBehind {
                                if (!uiState.isLoading && btnPressed) drawRect(brush = shineBrush)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading)
                            CircularProgressIndicator(
                                color = SU_GoldLight,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        else
                            Text(
                                "Create Account",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SU_GoldLight,
                                letterSpacing = 0.5.sp
                            )
                    }
                }
            }

            // ── Sign in link ──────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = 36.dp)
            ) {
                Text("Already have an account? ", color = textMuted, fontSize = 13.sp)
                Text(
                    "Sign In",
                    color = goldDk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}