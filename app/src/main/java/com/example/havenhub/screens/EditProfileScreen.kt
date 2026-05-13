package com.example.havenhub.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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

// ── Dark theme color tokens for EditProfile ───────────────────────────────────
private val EP_DarkBg        = Color(0xFF060D1A)   // page background
private val EP_DarkCard      = Color(0xFF112038)   // card / field surface
private val EP_DarkGold      = Color(0xFFD4AF37)   // primary accent gold
private val EP_DarkTextPri   = Color(0xFFF0F4FF)   // primary text
private val EP_DarkTextSec   = Color(0xFF8899BB)   // secondary / hint text
private val EP_DarkNavy      = Color(0xFF0D1B3E)   // avatar background
private val EP_DarkBorder    = Color(0xFF1E2E50)   // field border
private val EP_DarkGreen     = Color(0xFF3DCC7A)   // success message
private val EP_DarkError     = Color(0xFFE74C3C)   // error / remove button

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel    : ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel    = hiltViewModel()
) {
    // ── Dark theme detection ──────────────────────────────────────────────────
    val isDark = isSystemInDarkTheme()

    // ── Theme-aware color aliases ─────────────────────────────────────────────
    val pageBg    = if (isDark) EP_DarkBg      else Color(0xFFF4F6FA)
    val cardBg    = if (isDark) EP_DarkCard    else Color.White
    val goldC     = if (isDark) EP_DarkGold    else PrimaryBlue
    val textPri   = if (isDark) EP_DarkTextPri else Color.Black
    val textSec   = if (isDark) EP_DarkTextSec else Color.Gray
    val greenC    = if (isDark) EP_DarkGreen   else Color(0xFF4CAF50)
    val redC      = if (isDark) EP_DarkError   else Color.Red
    val topBarBg  = if (isDark) EP_DarkNavy    else PrimaryBlue

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

    // ── Remove Photo Confirmation Dialog ──────────────────────────────────────
    if (showRemovePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePhotoDialog = false },
            containerColor   = cardBg,                          // dark: dark card, light: white
            title = { Text("Remove Profile Photo", color = textPri) },
            text  = { Text("Are you sure you want to remove your profile photo?", color = textSec) },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.removeProfileImage()
                    showRemovePhotoDialog = false
                }) {
                    Text("Remove", color = redC, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePhotoDialog = false }) {
                    Text("Cancel", color = if (isDark) EP_DarkTextSec else Color.Gray)
                }
            }
        )
    }

    Scaffold(
        containerColor = pageBg,                                // dark: deep navy, light: off-white
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.updateProfile(
                            fullName    = name,
                            phoneNumber = phone,
                            city        = city
                        )
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = topBarBg,      // dark: deep navy, light: primary blue
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image circle
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(if (isDark) EP_DarkNavy else PrimaryBlue)
                        .clickable { profileImageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        authUiState.isLoading -> {
                            CircularProgressIndicator(
                                color       = if (isDark) EP_DarkGold else Color.White,
                                modifier    = Modifier.size(28.dp),
                                strokeWidth = 2.dp
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
                                fontSize   = 36.sp,
                                color      = if (isDark) EP_DarkGold else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // Camera icon overlay
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) EP_DarkCard else Color.White)
                        .clickable { profileImageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt, null,
                        modifier = Modifier.size(18.dp),
                        tint     = if (isDark) EP_DarkGold else PrimaryBlue
                    )
                }
            }

            Text(
                "Tap to change photo",
                fontSize = 12.sp,
                color    = textSec
            )

            // Remove photo button — visible only when photo exists
            if (currentProfileUrl.isNotEmpty()) {
                TextButton(
                    onClick = { showRemovePhotoDialog = true },
                    colors  = ButtonDefaults.textButtonColors(contentColor = redC)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove Profile Photo", fontSize = 13.sp)
                }
            }

            // Auth success/error messages
            authUiState.successMessage?.let {
                Text(it, color = greenC, fontSize = 13.sp)
            }
            authUiState.errorMessage?.let {
                Text(it, color = redC, fontSize = 13.sp)
            }

            Spacer(Modifier.height(4.dp))

            // Profile fields
            ProfileField(
                label        = "Full Name",
                value        = name,
                onValueChange = { name = it },
                icon         = Icons.Default.Person,
                isDark       = isDark
            )
            ProfileField(
                label        = "Email Address",
                value        = uiState.user?.email ?: "",
                onValueChange = {},
                icon         = Icons.Default.Email,
                readOnly     = true,
                isDark       = isDark
            )
            ProfileField(
                label        = "Phone Number",
                value        = phone,
                onValueChange = { phone = it },
                icon         = Icons.Default.Phone,
                isDark       = isDark
            )
            ProfileField(
                label        = "City",
                value        = city,
                onValueChange = { city = it },
                icon         = Icons.Default.LocationOn,
                isDark       = isDark
            )

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    viewModel.updateProfile(
                        fullName    = name,
                        phoneNumber = phone,
                        city        = city
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                enabled  = !uiState.isLoading,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) EP_DarkGold else PrimaryBlue
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = if (isDark) EP_DarkNavy else Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Save Changes",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isDark) EP_DarkNavy else Color.White
                    )
                }
            }

            uiState.errorMessage?.let {
                Text(text = it, color = redC, fontSize = 14.sp)
            }
        }
    }
}

// ── Profile Field — dark theme aware ─────────────────────────────────────────
@Composable
fun ProfileField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    icon         : ImageVector,
    readOnly     : Boolean = false,
    isDark       : Boolean = false
) {
    val goldC     = if (isDark) Color(0xFFD4AF37) else PrimaryBlue
    val textC     = if (isDark) Color(0xFFF0F4FF) else Color.Unspecified
    val borderFoc = if (isDark) Color(0xFFD4AF37) else PrimaryBlue
    val borderUnf = if (isDark) Color(0xFF1E2E50) else Color(0xFFBDBDBD)
    val labelFoc  = if (isDark) Color(0xFFD4AF37) else PrimaryBlue
    val labelUnf  = if (isDark) Color(0xFF8899BB) else Color.Gray
    val containerC = if (isDark) Color(0xFF112038) else Color.Transparent

    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        leadingIcon   = { Icon(icon, null, tint = goldC) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        readOnly      = readOnly,
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = borderFoc,
            unfocusedBorderColor    = borderUnf,
            disabledBorderColor     = borderUnf,
            focusedLabelColor       = labelFoc,
            unfocusedLabelColor     = labelUnf,
            disabledLabelColor      = labelUnf,
            focusedTextColor        = textC,
            unfocusedTextColor      = textC,
            disabledTextColor       = if (isDark) Color(0xFF8899BB) else Color.Gray,
            focusedContainerColor   = containerC,
            unfocusedContainerColor = containerC,
            disabledContainerColor  = containerC
        )
    )
}