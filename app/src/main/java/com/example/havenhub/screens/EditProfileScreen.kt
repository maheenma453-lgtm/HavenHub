package com.example.havenhub.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel    : ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel    = hiltViewModel()
) {
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

    // ✅ Remove Photo Confirmation Dialog
    if (showRemovePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePhotoDialog = false },
            title = { Text("Remove Profile Photo") },
            text  = { Text("Are you sure you want to remove your profile photo?") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.removeProfileImage()
                    showRemovePhotoDialog = false
                }) {
                    Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePhotoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
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
                    containerColor             = PrimaryBlue,
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
            // Profile Image
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                        .clickable { profileImageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        authUiState.isLoading -> {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        currentProfileUrl.isNotEmpty() -> {
                            AsyncImage(
                                model              = currentProfileUrl,
                                contentDescription = null,
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale       = ContentScale.Crop
                            )
                        }
                        else -> {
                            Text(
                                text       = uiState.user?.initials ?: "?",
                                fontSize   = 36.sp,
                                color      = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { profileImageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt, null,
                        modifier = Modifier.size(18.dp),
                        tint     = PrimaryBlue
                    )
                }
            }

            Text("Tap to change photo", fontSize = 12.sp, color = Color.Gray)

            // ✅ Remove Photo Button — sirf tab show hoga jab photo exist kare
            if (currentProfileUrl.isNotEmpty()) {
                TextButton(
                    onClick = { showRemovePhotoDialog = true },
                    colors  = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Remove Profile Photo", fontSize = 13.sp)
                }
            }

            authUiState.successMessage?.let {
                Text(it, color = Color(0xFF4CAF50), fontSize = 13.sp)
            }
            authUiState.errorMessage?.let {
                Text(it, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(Modifier.height(4.dp))

            ProfileField(
                label = "Full Name", value = name,
                onValueChange = { name = it }, icon = Icons.Default.Person
            )
            ProfileField(
                label = "Email Address", value = uiState.user?.email ?: "",
                onValueChange = {}, icon = Icons.Default.Email, readOnly = true
            )
            ProfileField(
                label = "Phone Number", value = phone,
                onValueChange = { phone = it }, icon = Icons.Default.Phone
            )
            ProfileField(
                label = "City", value = city,
                onValueChange = { city = it }, icon = Icons.Default.LocationOn
            )

            Spacer(Modifier.height(8.dp))

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
                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            uiState.errorMessage?.let {
                Text(text = it, color = ErrorRed, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ProfileField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    icon         : ImageVector,
    readOnly     : Boolean = false
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        leadingIcon   = { Icon(icon, null, tint = PrimaryBlue) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        readOnly      = readOnly,
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = BorderGray,
            disabledLabelColor  = TextSecondary
        )
    )
}