package com.example.havenhub.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.ProfileViewModel

// ── Dark theme color tokens ───────────────────────────────────────────────────
private val EP_DarkBg        = Color(0xFF060D1A)
private val EP_DarkCard      = Color(0xFF112038)
private val EP_DarkCardAlt   = Color(0xFF0D1B3E)
private val EP_DarkGold      = Color(0xFFD4AF37)
private val EP_DarkGoldLight = Color(0xFFF5D060)
private val EP_DarkGoldFaint = Color(0x22D4AF37)
private val EP_DarkTextPri   = Color(0xFFF0F4FF)
private val EP_DarkTextSec   = Color(0xFF8899BB)
private val EP_DarkNavy      = Color(0xFF0D1B3E)
private val EP_DarkBorder    = Color(0xFF1E2E50)
private val EP_DarkGreen     = Color(0xFF3DCC7A)
private val EP_DarkError     = Color(0xFFCF6679)
private val EP_DarkElevated  = Color(0xFF1E3060)

// ── Light theme color tokens ──────────────────────────────────────────────────
private val EP_LightBg       = Color(0xFFF0F2F8)
private val EP_LightCard     = Color(0xFFFFFFFF)
private val EP_LightBlue     = Color(0xFF1B2B5B)
private val EP_LightGold     = Color(0xFFC9A84C)
private val EP_LightTextPri  = Color(0xFF1A1A2E)
private val EP_LightTextSec  = Color(0xFF888888)
private val EP_LightBorder   = Color(0xFFDDE1E7)
private val EP_LightGreen    = Color(0xFF2ECC71)
private val EP_LightError    = Color(0xFFBA1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel    : ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel    = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()

    // ── Semantic aliases ──────────────────────────────────────────────────────
    val pageBg    = if (isDark) EP_DarkBg       else EP_LightBg
    val cardBg    = if (isDark) EP_DarkCard     else EP_LightCard
    val cardAlt   = if (isDark) EP_DarkCardAlt  else Color(0xFFF7F9FF)
    val accentC   = if (isDark) EP_DarkGold     else EP_LightBlue
    val accentSub = if (isDark) EP_DarkGoldLight else EP_LightGold
    val textPri   = if (isDark) EP_DarkTextPri  else EP_LightTextPri
    val textSec   = if (isDark) EP_DarkTextSec  else EP_LightTextSec
    val borderC   = if (isDark) EP_DarkBorder   else EP_LightBorder
    val greenC    = if (isDark) EP_DarkGreen    else EP_LightGreen
    val redC      = if (isDark) EP_DarkError    else EP_LightError
    val navyTop   = if (isDark) EP_DarkNavy     else EP_LightBlue
    val elevated  = if (isDark) EP_DarkElevated else Color(0xFF2E4A9E)

    val uiState     by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    var name  by remember(uiState.user) { mutableStateOf(uiState.user?.fullName ?: "") }
    var phone by remember(uiState.user) { mutableStateOf(uiState.user?.phoneNumber ?: "") }
    var city  by remember(uiState.user) { mutableStateOf("") }

    var showRemovePhotoDialog by remember { mutableStateOf(false) }
    val currentProfileUrl = uiState.user?.profileImageUrl ?: ""

    val profileImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { authViewModel.updateProfileImage(it) } }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearMessages()
        }
    }

    // ── Remove Photo Dialog ───────────────────────────────────────────────────
    if (showRemovePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePhotoDialog = false },
            containerColor   = cardBg,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Remove Profile Photo",
                    color      = textPri,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove your profile photo?",
                    color    = textSec,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.removeProfileImage()
                        showRemovePhotoDialog = false
                    },
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = redC)
                ) {
                    Text("Remove", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRemovePhotoDialog = false },
                    shape   = RoundedCornerShape(10.dp),
                    border  = androidx.compose.foundation.BorderStroke(1.dp, borderC)
                ) {
                    Text("Cancel", color = textSec)
                }
            }
        )
    }

    Scaffold(
        containerColor = pageBg,
        topBar = {
            // ── Gradient Top App Bar ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(navyTop, elevated)
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        "Edit Profile",
                        modifier   = Modifier.weight(1f),
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                    TextButton(
                        onClick = {
                            viewModel.updateProfile(
                                fullName    = name,
                                phoneNumber = phone,
                                city        = city
                            )
                        }
                    ) {
                        Text(
                            "Save",
                            fontWeight = FontWeight.Bold,
                            color      = if (isDark) EP_DarkGold else Color.White,
                            fontSize   = 15.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Hero Avatar Section ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                elevated.copy(alpha = 0.6f),
                                pageBg
                            )
                        )
                    )
                    .padding(top = 32.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Avatar ring + photo
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentC.copy(alpha = 0.25f),
                                            accentC.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .border(
                                    width = 2.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(accentC, accentSub)
                                    ),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                                .clip(CircleShape)
                                .clickable { profileImageLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isDark) EP_DarkNavy else EP_LightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    authUiState.isLoading -> {
                                        CircularProgressIndicator(
                                            color       = accentSub,
                                            modifier    = Modifier.size(30.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                    currentProfileUrl.isNotEmpty() -> {
                                        AsyncImage(
                                            model              = currentProfileUrl,
                                            contentDescription = null,
                                            modifier           = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale       = ContentScale.Crop
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text       = uiState.user?.initials ?: "?",
                                            fontSize   = 38.sp,
                                            color      = accentSub,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Camera badge
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(colors = listOf(accentC, accentSub))
                                )
                                .clickable { profileImageLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                modifier = Modifier.size(18.dp),
                                tint     = if (isDark) EP_DarkBg else Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // User name display
                    Text(
                        text       = uiState.user?.fullName ?: "Your Name",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = textPri
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = uiState.user?.email ?: "",
                        fontSize = 13.sp,
                        color    = textSec
                    )

                    Spacer(Modifier.height(12.dp))

                    // Change / Remove photo buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { profileImageLauncher.launch("image/*") },
                            shape   = RoundedCornerShape(20.dp),
                            border  = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(colors = listOf(accentC, accentSub))
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt, null,
                                modifier = Modifier.size(15.dp),
                                tint     = accentC
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Change Photo", fontSize = 13.sp, color = accentC)
                        }

                        if (currentProfileUrl.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showRemovePhotoDialog = true },
                                shape   = RoundedCornerShape(20.dp),
                                border  = androidx.compose.foundation.BorderStroke(1.dp, redC.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline, null,
                                    modifier = Modifier.size(15.dp),
                                    tint     = redC
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Remove", fontSize = 13.sp, color = redC)
                            }
                        }
                    }

                    // Auth messages
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(visible = authUiState.successMessage != null) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(greenC.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint     = greenC,
                                modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(authUiState.successMessage ?: "", color = greenC, fontSize = 13.sp)
                        }
                    }
                    AnimatedVisibility(visible = authUiState.errorMessage != null) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(redC.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null,
                                tint     = redC,
                                modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(authUiState.errorMessage ?: "", color = redC, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Form Fields Card ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation    = if (isDark) 0.dp else 4.dp,
                        shape        = RoundedCornerShape(20.dp),
                        ambientColor = accentC.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(
                        width = 1.dp,
                        color = borderC,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Section label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(colors = listOf(accentC, accentSub))
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Personal Information",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentC,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                EnhancedProfileField(
                    label         = "Full Name",
                    value         = name,
                    onValueChange = { name = it },
                    icon          = Icons.Default.Person,
                    isDark        = isDark,
                    accentC       = accentC,
                    textPri       = textPri,
                    textSec       = textSec,
                    borderC       = borderC,
                    cardAlt       = cardAlt
                )

                EnhancedProfileField(
                    label         = "Email Address",
                    value         = uiState.user?.email ?: "",
                    onValueChange = {},
                    icon          = Icons.Default.Email,
                    readOnly      = true,
                    isDark        = isDark,
                    accentC       = accentC,
                    textPri       = textPri,
                    textSec       = textSec,
                    borderC       = borderC,
                    cardAlt       = cardAlt,
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentC.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Verified", fontSize = 10.sp, color = accentC, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )

                EnhancedProfileField(
                    label         = "Phone Number",
                    value         = phone,
                    onValueChange = { phone = it },
                    icon          = Icons.Default.Phone,
                    isDark        = isDark,
                    accentC       = accentC,
                    textPri       = textPri,
                    textSec       = textSec,
                    borderC       = borderC,
                    cardAlt       = cardAlt
                )

                EnhancedProfileField(
                    label         = "City",
                    value         = city,
                    onValueChange = { city = it },
                    icon          = Icons.Default.LocationOn,
                    isDark        = isDark,
                    accentC       = accentC,
                    textPri       = textPri,
                    textSec       = textSec,
                    borderC       = borderC,
                    cardAlt       = cardAlt
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Save Button ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (!uiState.isLoading)
                            Brush.horizontalGradient(colors = listOf(accentC, elevated))
                        else
                            Brush.horizontalGradient(colors = listOf(
                                accentC.copy(alpha = 0.5f),
                                elevated.copy(alpha = 0.5f)
                            ))
                    )
                    .clickable(enabled = !uiState.isLoading) {
                        viewModel.updateProfile(
                            fullName    = name,
                            phoneNumber = phone,
                            city        = city
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = Color.White.copy(alpha = 0.8f),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Saving...",
                            color      = Color.White.copy(alpha = 0.8f),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Save Changes",
                            color      = Color.White,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // ── Profile error message ─────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(redC.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null,
                        tint     = redC,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.errorMessage ?: "", color = redC, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Enhanced Profile Field Component ─────────────────────────────────────────
@Composable
fun EnhancedProfileField(
    label           : String,
    value           : String,
    onValueChange   : (String) -> Unit,
    icon            : ImageVector,
    readOnly        : Boolean = false,
    isDark          : Boolean = false,
    accentC         : Color,
    textPri         : Color,
    textSec         : Color,
    borderC         : Color,
    cardAlt         : Color,
    trailingContent : @Composable (() -> Unit)? = null
) {
    val focusedBorder = accentC
    val unfocusedBorder = borderC

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                icon, null,
                tint = accentC,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = trailingContent?.let { { it() } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        readOnly = readOnly,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = focusedBorder,
            unfocusedBorderColor = unfocusedBorder,
            disabledBorderColor = unfocusedBorder.copy(alpha = 0.5f),
            focusedLabelColor = accentC,
            unfocusedLabelColor = textSec,
            disabledLabelColor = textSec.copy(alpha = 0.6f),
            focusedTextColor = textPri,
            unfocusedTextColor = textPri,
            disabledTextColor = textSec,
            focusedContainerColor = if (isDark) cardAlt else Color.Transparent,
            unfocusedContainerColor = if (isDark) cardAlt else Color.Transparent,
            disabledContainerColor = if (isDark) cardAlt.copy(alpha = 0.5f) else Color(0xFFF5F5F5),
            cursorColor = accentC
        )
    )
}