package com.example.havenhub.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel    : ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Pre-fill fields from loaded user
    var name  by remember(uiState.user) { mutableStateOf(uiState.user?.fullName ?: "") }
    var phone by remember(uiState.user) { mutableStateOf(uiState.user?.phoneNumber ?: "") }
    var city  by remember(uiState.user) { mutableStateOf("") }
    var bio   by remember { mutableStateOf("") }

    // Success hone par back navigate karo
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearMessages()
        }
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
                    TextButton(
                        onClick = {
                            viewModel.updateProfile(
                                fullName    = name,
                                phoneNumber = phone,
                                city        = city
                            )
                        }
                    ) {
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
            verticalArrangement     = Arrangement.spacedBy(16.dp),
            horizontalAlignment     = Alignment.CenterHorizontally
        ) {

            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier         = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = uiState.user?.initials ?: "?",
                        fontSize   = 36.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier         = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint     = PrimaryBlue
                    )
                }
            }

            Text("Change Photo", fontSize = 13.sp, color = PrimaryBlue)

            Spacer(Modifier.height(4.dp))

            ProfileField(
                label         = "Full Name",
                value         = name,
                onValueChange = { name = it },
                icon          = Icons.Default.Person
            )
            ProfileField(
                label         = "Email Address",
                value         = uiState.user?.email ?: "",
                onValueChange = {},
                icon          = Icons.Default.Email,
                readOnly      = true
            )
            ProfileField(
                label         = "Phone Number",
                value         = phone,
                onValueChange = { phone = it },
                icon          = Icons.Default.Phone
            )
            ProfileField(
                label         = "City",
                value         = city,
                onValueChange = { city = it },
                icon          = Icons.Default.LocationOn
            )

            OutlinedTextField(
                value         = bio,
                onValueChange = { bio = it },
                label         = { Text("Bio (optional)") },
                modifier      = Modifier.fillMaxWidth().height(110.dp),
                shape         = RoundedCornerShape(12.dp),
                placeholder   = { Text("Tell others about yourself...") },
                maxLines      = 4
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

            // Error Message
            uiState.errorMessage?.let { error ->
                Text(text = error, color = ErrorRed, fontSize = 14.sp)
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
        leadingIcon   = { Icon(icon, contentDescription = null, tint = PrimaryBlue) },
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