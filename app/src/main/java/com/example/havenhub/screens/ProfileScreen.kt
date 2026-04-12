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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    // Auth Redirect Logic
    LaunchedEffect(authUiState.isLoggedIn) {
        if (!authUiState.isLoggedIn && !authUiState.isLoading) {
            navController.navigate(Screen.SignIn.route) {
                popUpTo(0)
            }
        }
    }

    // Role Logic
    val userRole = uiState.user?.role?.lowercase() ?: "user"
    val isAdmin = userRole == "admin"
    val isLandlord = userRole == "landlord"

    val roleText = userRole.replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF2F4F7))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // SECTION 1: Header (Avatar & Name)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE4E8EF))
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.user?.initials ?: "?",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = uiState.user?.fullName ?: "Loading...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )

                Text(
                    text = uiState.user?.email ?: "",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isAdmin) Color(0xFFFFD700) else Color(0xFFCDD4DF) // Admin ko gold badge
                ) {
                    Text(
                        text = roleText,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 7.dp),
                        fontSize = 13.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // SECTION 2: Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Agar Admin hai toh platform stats ya simple placeholders
                ProfileStat(value = if(isAdmin) "N/A" else "0", label = "Reviews")
                VerticalDivider()
                ProfileStat(value = if(isAdmin) "N/A" else "0.0", label = "Rating")
                VerticalDivider()
                ProfileStat(value = roleText, label = "Role")
            }

            Spacer(Modifier.height(8.dp))

            // SECTION 3: Menu Items

            // --- ADMIN ONLY SECTION ---
            if (isAdmin) {
                Text(
                    text = "Administration",
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                ProfileMenuItem(
                    icon = Icons.Default.Dashboard,
                    label = "Admin Dashboard",
                    onClick = { navController.navigate("admin_dashboard") }
                )
                ProfileMenuItem(
                    icon = Icons.Default.VerifiedUser,
                    label = "Verification Requests",
                    onClick = { navController.navigate("verify_users") }
                )
                ProfileMenuItem(
                    icon = Icons.Default.People,
                    label = "Manage Users",
                    onClick = { navController.navigate("manage_users") }
                )
            }

            // --- COMMON & ROLE SPECIFIC ---
            Text(
                text = "Account Settings",
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            if (!isAdmin) {
                ProfileMenuItem(
                    icon = Icons.Default.BookOnline,
                    label = "My Bookings",
                    onClick = { navController.navigate(Screen.MyBookings.route) }
                )
            }

            if (isLandlord) {
                ProfileMenuItem(
                    icon = Icons.Default.Home,
                    label = "My Properties",
                    onClick = { navController.navigate(Screen.MyProperties.route) }
                )
            }

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = { navController.navigate(Screen.Settings.route) }
            )

            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                label = "Help & Support",
                onClick = { navController.navigate(Screen.HelpAndSupport.route) }
            )

            Spacer(Modifier.height(24.dp))

            // SECTION 4: Logout
            Button(
                onClick = { authViewModel.signOut() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Logout", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.LightGray))
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text(text = label, fontSize = 15.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
            HorizontalDivider(color = Color(0xFFF1F1F1), thickness = 1.dp)
        }
    }
}