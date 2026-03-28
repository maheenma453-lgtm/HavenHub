package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.R
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel    : AuthViewModel = hiltViewModel()
) {
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

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOut), label = "a"
    )

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            val dest = when (uiState.userRole) {
                "admin", "ADMIN" -> Screen.AdminDashboard.route
                else             -> Screen.Home.route
            }
            navController.navigate(dest) { popUpTo(0) { inclusive = true } }
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Back arrow ─────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 4.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint               = PrimaryBlue
                )
            }
        }

        // ── Logo ───────────────────────────────────────────────────
        // fillMaxWidth(0.65f) — responsive width
        // NO aspectRatio — logo ki actual height use hogi, transparent padding nahi
        // wrapContentHeight — sirf content ki height lega
        Image(
            painter            = painterResource(id = R.drawable.havenhub),
            contentDescription = "HavenHub",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .fillMaxWidth(0.65f)
                .wrapContentHeight()
                .padding(vertical = 0.dp)
                //.padding(vertical = (-24).dp)
                .alpha(alpha)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Brand name ─────────────────────────────────────────────
        Text(
            "HavenHub",
            fontSize      = 22.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = PrimaryBlue,
            letterSpacing = (-0.5).sp,
            modifier      = Modifier.alpha(alpha)
        )
        Text(
            "Create your account",
            fontSize = 12.sp,
            color    = TextSecondary,
            modifier = Modifier
                .padding(top = 2.dp, bottom = 12.dp)
                .alpha(alpha)
        )

        // Role badge
        if (uiState.selectedRole.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentGold.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    "Role: ${uiState.selectedRole.replaceFirstChar { it.uppercase() }}",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = AccentGoldDark
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ── Form Card ──────────────────────────────────────────────
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .alpha(alpha),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {

                Text(
                    "Your Details",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = PrimaryBlue
                )
                Text(
                    "Fill in your information below",
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp).height(3.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(AccentGold, AccentGoldLight)))
                )
                Spacer(modifier = Modifier.height(18.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentGold,
                    focusedLabelColor    = PrimaryBlue,
                    unfocusedBorderColor = BorderGray,
                    errorBorderColor     = ErrorRed
                )

                OutlinedTextField(
                    value          = fullName,
                    onValueChange  = { viewModel.onFullNameChange(it) },
                    label          = { Text("Full Name") },
                    isError        = nameError != null,
                    supportingText = { nameError?.let { Text(it, color = ErrorRed, fontSize = 11.sp) } },
                    leadingIcon    = { Icon(Icons.Default.Person, null, tint = PrimaryBlue.copy(alpha = 0.7f)) },
                    singleLine     = true,
                    modifier       = Modifier.fillMaxWidth(),
                    shape          = RoundedCornerShape(14.dp),
                    colors         = fieldColors
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value           = email,
                    onValueChange   = { viewModel.onEmailChange(it) },
                    label           = { Text("Email Address") },
                    isError         = emailError != null,
                    supportingText  = { emailError?.let { Text(it, color = ErrorRed, fontSize = 11.sp) } },
                    leadingIcon     = { Icon(Icons.Default.Email, null, tint = PrimaryBlue.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(14.dp),
                    colors          = fieldColors
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value                = password,
                    onValueChange        = { viewModel.onPasswordChange(it) },
                    label                = { Text("Password") },
                    isError              = passwordError != null,
                    supportingText       = { passwordError?.let { Text(it, color = ErrorRed, fontSize = 11.sp) } },
                    leadingIcon          = { Icon(Icons.Default.Lock, null, tint = PrimaryBlue.copy(alpha = 0.7f)) },
                    trailingIcon         = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                null, tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine           = true,
                    modifier             = Modifier.fillMaxWidth(),
                    shape                = RoundedCornerShape(14.dp),
                    colors               = fieldColors
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value                = confirmPassword,
                    onValueChange        = { viewModel.onConfirmPasswordChange(it) },
                    label                = { Text("Confirm Password") },
                    isError              = confirmPassword.isNotEmpty() && password != confirmPassword,
                    supportingText       = {
                        if (confirmPassword.isNotEmpty() && password != confirmPassword)
                            Text("Passwords do not match", color = ErrorRed, fontSize = 11.sp)
                    },
                    leadingIcon          = { Icon(Icons.Default.LockOpen, null, tint = PrimaryBlue.copy(alpha = 0.7f)) },
                    trailingIcon         = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                if (confirmVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                null, tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (confirmVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine           = true,
                    modifier             = Modifier.fillMaxWidth(),
                    shape                = RoundedCornerShape(14.dp),
                    colors               = fieldColors
                )

                uiState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ErrorRed.copy(alpha = 0.08f))
                            .padding(11.dp)
                    ) { Text(it, color = ErrorRed, fontSize = 12.sp) }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick  = { viewModel.signUp() },
                    enabled  = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = PrimaryBlue,
                        disabledContainerColor = BorderGray
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
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.alpha(alpha)
        ) {
            Text("Already have an account? ", color = TextSecondary, fontSize = 13.sp)
            Text(
                "Sign In",
                color      = AccentGoldDark,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.clickable {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
