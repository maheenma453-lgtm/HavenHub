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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController   : NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel   : AuthViewModel    = hiltViewModel()
) {
    val configuration = LocalConfiguration.current
    val screenWidth   = configuration.screenWidthDp       // Float
    val screenHeight  = configuration.screenHeightDp      // Float

    // ── Responsive values (all Float → then .dp / .sp) ──────────────────────
    val avatarSize        = (screenWidth * 0.22f).coerceIn(72f,  110f).dp
    val nameFontSize      = (screenWidth * 0.052f).coerceIn(16f, 22f).sp
    val horizontalPadding = (screenWidth * 0.04f).coerceIn(12f,  20f).dp
    val statFontSize      = (screenWidth * 0.045f).coerceIn(14f, 20f).sp
    val heroPadV          = (screenHeight * 0.035f).coerceIn(20f, 36f).dp

    val uiState     by profileViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authUiState.isLoggedIn, authUiState.isAuthReady) {
        if (authUiState.isAuthReady && !authUiState.isLoggedIn) {
            navController.navigate(Screen.SignIn.route) { popUpTo(0) }
        }
    }

    val userRole = when {
        uiState.user != null              -> uiState.user!!.normalizedRole
        authUiState.userRole.isNotEmpty() -> authUiState.userRole.lowercase().trim()
        else                              -> "tenant"
    }

    val isAdmin    = userRole == "admin"
    val isLandlord = userRole == "landlord"
    val roleText   = userRole.replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Profile", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF2F4F7))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Profile Header ───────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE4E8EF))
                    .padding(vertical = heroPadV),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier         = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(avatarSize * 0.35f),
                                strokeWidth = 3.dp
                            )
                        }
                        !uiState.user?.profileImageUrl.isNullOrEmpty() -> {
                            AsyncImage(
                                model              = uiState.user!!.profileImageUrl,
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
                                fontSize   = (avatarSize.value * 0.37f).sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text       = if (uiState.isLoading) "Loading..."
                    else uiState.user?.fullName ?: "—",
                    fontSize   = nameFontSize,
                    fontWeight = FontWeight.Bold,
                    color      = PrimaryBlue
                )

                Text(
                    text     = uiState.user?.email ?: "",
                    fontSize = (screenWidth * 0.032f).coerceIn(11f, 14f).sp,
                    color    = Color.Gray
                )

                Spacer(Modifier.height(8.dp))

                // Verification Badge
                val verStatus = uiState.user?.verificationStatus?.uppercase() ?: "PENDING"
                val (badgeColor, badgeText) = when (verStatus) {
                    "VERIFIED", "APPROVED" -> Color(0xFF4CAF50) to "Verified"
                    "REJECTED"             -> Color(0xFFE53935) to "Rejected"
                    else                   -> Color(0xFFFFA726) to "Pending Verification"
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text       = badgeText,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                        fontSize   = 12.sp,
                        color      = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Role Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isAdmin) Color(0xFFFFD700) else Color(0xFFCDD4DF)
                ) {
                    Text(
                        text       = roleText,
                        modifier   = Modifier.padding(horizontal = 24.dp, vertical = 7.dp),
                        fontSize   = 13.sp,
                        color      = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Stats Row ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                ProfileStat(
                    value    = when {
                        isAdmin    -> "N/A"
                        isLandlord -> "${uiState.user?.landlordReviewCount ?: 0}"
                        else       -> "0"
                    },
                    label    = "Reviews",
                    fontSize = statFontSize
                )
                VerticalDivider()
                ProfileStat(
                    value    = when {
                        isAdmin    -> "N/A"
                        isLandlord -> "%.1f".format(uiState.user?.landlordRating ?: 0f)
                        else       -> "0.0"
                    },
                    label    = "Rating",
                    fontSize = statFontSize
                )
                VerticalDivider()
                ProfileStat(value = roleText, label = "Role", fontSize = statFontSize)
            }

            Spacer(Modifier.height(8.dp))

            // ── Edit Profile Button ──────────────────────────────────────────
            OutlinedButton(
                onClick  = { navController.navigate(Screen.EditProfile.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .height(46.dp),
                shape  = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            // ── Admin Menu ───────────────────────────────────────────────────
            if (isAdmin) {
                SectionHeader("Administration")
                ProfileMenuItem(
                    icon    = Icons.Default.Dashboard,
                    label   = "Admin Dashboard",
                    onClick = { navController.navigate("admin_dashboard") }
                )
                ProfileMenuItem(
                    icon    = Icons.Default.VerifiedUser,
                    label   = "Verification Requests",
                    onClick = { navController.navigate("verify_users") }
                )
                ProfileMenuItem(
                    icon    = Icons.Default.People,
                    label   = "Manage Users",
                    onClick = { navController.navigate("manage_users") }
                )
            }

            // ── Account Settings ─────────────────────────────────────────────
            SectionHeader("Account Settings")

            if (!isAdmin) {
                ProfileMenuItem(
                    icon    = Icons.Default.BookOnline,
                    label   = "My Bookings",
                    onClick = { navController.navigate(Screen.MyBookings.route) }
                )
            }
            if (isLandlord) {
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
                icon    = Icons.AutoMirrored.Filled.HelpOutline,
                label   = "Help & Support",
                onClick = { navController.navigate(Screen.HelpAndSupport.route) }
            )

            Spacer(Modifier.height(24.dp))

            // ── Logout Button ────────────────────────────────────────────────
            Button(
                onClick  = { authViewModel.signOut() },
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Logout", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Helper Composables ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        fontSize   = 12.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.Gray
    )
}

@Composable
private fun ProfileStat(
    value   : String,
    label   : String,
    fontSize: TextUnit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontSize   = fontSize,
            fontWeight = FontWeight.Bold,
            color      = PrimaryBlue
        )
        Text(
            text     = label,
            fontSize = 12.sp,
            color    = Color.Gray
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color.LightGray)
    )
}

@Composable
private fun ProfileMenuItem(
    icon   : ImageVector,
    label  : String,
    onClick: () -> Unit
) {
    Surface(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        color    = Color.White
    ) {
        Column {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null,
                    tint     = PrimaryBlue,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text(label,
                    fontSize = 15.sp,
                    color    = Color.DarkGray,
                    modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null,
                    tint     = Color.LightGray,
                    modifier = Modifier.size(20.dp))
            }
            HorizontalDivider(color = Color(0xFFF1F1F1), thickness = 1.dp)
        }
    }
}