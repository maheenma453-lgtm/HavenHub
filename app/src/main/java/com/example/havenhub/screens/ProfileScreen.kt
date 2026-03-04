package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController  : NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel  : AuthViewModel = hiltViewModel()
) {
    val uiState     by profileViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    // Logout hone par login screen pe navigate karo
    LaunchedEffect(authUiState.isLoggedIn) {
        if (!authUiState.isLoggedIn) {
            navController.navigate(Screen.SignIn.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.EditProfile.route) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
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

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header ────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue.copy(alpha = 0.1f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    Spacer(Modifier.height(12.dp))
                    Text(
                        uiState.user?.fullName ?: "-",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        uiState.user?.email ?: "-",
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text     = uiState.user?.role?.displayName() ?: "-",
                            color    = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    // Verification badge
                    if (uiState.user?.isVerified == true) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text     = "Verified",
                                color    = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // ── Stats ──────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = uiState.user?.landlordReviewCount?.toString() ?: "0",
                    label = "Reviews"
                )
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem(
                    value = uiState.user?.landlordRating?.let { "%.1f".format(it) } ?: "0.0",
                    label = "Rating"
                )
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem(
                    value = uiState.user?.role?.displayName() ?: "-",
                    label = "Role"
                )
            }

            HorizontalDivider(color = BorderGray)

            // ── Menu Items ─────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            ProfileMenuItem(
                icon    = Icons.Default.BookOnline,
                label   = "My Bookings",
                onClick = { navController.navigate(Screen.MyBookings.route) }
            )
            if (uiState.user?.isLandlord == true) {
                ProfileMenuItem(
                    icon    = Icons.Default.Home,
                    label   = "My Properties",
                    onClick = { navController.navigate(Screen.MyProperties.route) }
                )
            }
            ProfileMenuItem(
                icon    = Icons.Default.Settings,
                label   = "Settings",
                onClick = { navController.navigate(Screen.Settings.route) }
            )
            ProfileMenuItem(
                icon    = Icons.AutoMirrored.Filled.Help,
                label   = "Help & Support",
                onClick = { }
            )
            ProfileMenuItem(
                icon    = Icons.Default.Info,
                label   = "About",
                onClick = { }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = BorderGray)

            ProfileMenuItem(
                icon    = Icons.AutoMirrored.Filled.Logout,
                label   = "Logout",
                onClick = { authViewModel.signOut() },
                tint    = ErrorRed
            )
            Spacer(Modifier.height(24.dp))

            // Error Message
            uiState.errorMessage?.let { error ->
                Text(text = error, color = ErrorRed, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
fun ProfileMenuItem(
    icon   : ImageVector,
    label  : String,
    onClick: () -> Unit,
    tint   : Color = PrimaryBlue
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick  = onClick,
            modifier = Modifier.fillMaxWidth(),
            color    = Color.Transparent,
            shape    = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text(label, fontSize = 15.sp, color = if (tint == ErrorRed) ErrorRed else TextPrimary, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}