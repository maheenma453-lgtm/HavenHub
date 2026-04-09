package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authUiState.isLoggedIn) {
        if (!authUiState.isLoggedIn && !authUiState.isLoading) {
            navController.navigate(Screen.SignIn.route) { popUpTo(0) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(100.dp).clip(CircleShape).background(PrimaryBlue), contentAlignment = Alignment.Center) {
                Text(uiState.user?.initials ?: "?", fontSize = 40.sp, color = Color.White)
            }
            Text(uiState.user?.fullName ?: "User", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            // ✅ Fix: Direct logic for Role Display
            val roleText = uiState.user?.role?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "User"
            Text(roleText, color = PrimaryBlue)

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { authViewModel.signOut() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}