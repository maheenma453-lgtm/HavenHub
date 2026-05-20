package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.ProfileViewModel

// ── Brand colors matching logo (deep navy + gold) ──────────────────────
private val NavyDeep    = Color(0xFF0F1D35)
private val NavyPrimary = Color(0xFF1B2A4A)
private val NavyMedium  = Color(0xFF243358)
private val GoldPrimary = Color(0xFFC9A84C)
private val GoldLight   = Color(0xFFE2C47A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController   : NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel   : AuthViewModel    = hiltViewModel()
) {
    val configuration     = LocalConfiguration.current
    val screenWidth       = configuration.screenWidthDp
    val screenHeight      = configuration.screenHeightDp

    val avatarSize        = (screenWidth * 0.24f).coerceIn(76f, 116f).dp
    val nameFontSize      = (screenWidth * 0.054f).coerceIn(17f, 23f).sp
    val horizontalPadding = (screenWidth * 0.045f).coerceIn(14f, 22f).dp
    val statFontSize      = (screenWidth * 0.047f).coerceIn(15f, 21f).sp
    val heroPadV          = (screenHeight * 0.038f).coerceIn(22f, 40f).dp

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

    val background       = MaterialTheme.colorScheme.background
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val heroGradient = Brush.verticalGradient(
        colors = listOf(NavyDeep, NavyMedium)
    )
    val avatarRingGradient = Brush.linearGradient(
        colors = listOf(GoldPrimary, GoldLight, GoldPrimary),
        start  = Offset(0f, 0f),
        end    = Offset(300f, 300f)
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = NavyPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .background(background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ══════════════════════════════════════════════════════════
            // HERO — navy gradient
            // ══════════════════════════════════════════════════════════
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .background(heroGradient),
                contentAlignment = Alignment.TopCenter
            ) {
                // decorative circles
                Box(
                    modifier = Modifier
                        .size((screenWidth * 0.55f).dp)
                        .align(Alignment.TopEnd)
                        .offset(
                            x = (screenWidth * 0.15f).dp,
                            y = -(screenWidth * 0.1f).dp
                        )
                        .background(
                            color = Color.White.copy(alpha = 0.03f),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size((screenWidth * 0.38f).dp)
                        .align(Alignment.BottomStart)
                        .offset(
                            x = -(screenWidth * 0.1f).dp,
                            y = (screenWidth * 0.08f).dp
                        )
                        .background(
                            color = GoldPrimary.copy(alpha = 0.05f),
                            shape = CircleShape
                        )
                )

                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(vertical = heroPadV),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ── Avatar with gold shimmer ring + pencil icon ────
                    Box(
                        modifier         = Modifier
                            .size(avatarSize + 6.dp)
                            .drawBehind {
                                drawCircle(
                                    brush  = avatarRingGradient,
                                    radius = size.minDimension / 2f
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Avatar circle
                        Box(
                            modifier         = Modifier
                                .size(avatarSize)
                                .clip(CircleShape)
                                .background(NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                uiState.isLoading -> {
                                    CircularProgressIndicator(
                                        color       = GoldPrimary,
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
                                        color      = GoldLight
                                    )
                                }
                            }
                        }

                        // ✦ Pencil edit icon — bottom-right of avatar
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                                .clickable {
                                    navController.navigate(Screen.EditProfile.route)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint               = NavyDeep,
                                modifier           = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text          = if (uiState.isLoading) "Loading..." else uiState.user?.fullName ?: "—",
                        fontSize      = nameFontSize,
                        fontWeight    = FontWeight.Bold,
                        color         = Color.White,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text     = uiState.user?.email ?: "",
                        fontSize = (screenWidth * 0.033f).coerceIn(11f, 14f).sp,
                        color    = Color.White.copy(alpha = 0.62f)
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Verification badge ─────────────────────────────
                    val verStatus = uiState.user?.verificationStatus?.uppercase() ?: "PENDING"
                    val (badgeColor, badgeText) = when (verStatus) {
                        "VERIFIED", "APPROVED" -> Color(0xFF4CAF50) to "✓  Verified"
                        "REJECTED"             -> Color(0xFFE53935) to "✗  Rejected"
                        else                   -> Color(0xFFFFA726) to "⏳  Pending Verification"
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text          = badgeText,
                            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                            fontSize      = 12.sp,
                            color         = badgeColor,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Role badge ─────────────────────────────────────
                    val roleBadgeBg = when {
                        isAdmin    -> GoldPrimary
                        isLandlord -> Color.White.copy(alpha = 0.15f)
                        else       -> Color.White.copy(alpha = 0.10f)
                    }
                    val roleBadgeTextColor = when {
                        isAdmin -> NavyDeep
                        else    -> GoldLight
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = roleBadgeBg
                    ) {
                        Text(
                            text          = roleText,
                            modifier      = Modifier.padding(horizontal = 28.dp, vertical = 7.dp),
                            fontSize      = 13.sp,
                            color         = roleBadgeTextColor,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════
            // STATS ROW
            // ══════════════════════════════════════════════════════════
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                color           = surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ProfileStat(
                        value = when {
                            isAdmin    -> "N/A"
                            isLandlord -> "${uiState.user?.landlordReviewCount ?: 0}"
                            else       -> "0"
                        },
                        label    = "Reviews",
                        fontSize = statFontSize,
                        color    = GoldPrimary
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        GoldPrimary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    ProfileStat(
                        value = when {
                            isAdmin    -> "N/A"
                            isLandlord -> "%.1f".format(uiState.user?.landlordRating ?: 0f)
                            else       -> "0.0"
                        },
                        label    = "Rating",
                        fontSize = statFontSize,
                        color    = GoldPrimary
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        GoldPrimary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    ProfileStat(
                        value    = roleText,
                        label    = "Role",
                        fontSize = statFontSize,
                        color    = GoldPrimary
                    )
                }
            }

            // ══════════════════════════════════════════════════════════
            // EDIT PROFILE BUTTON — REMOVED
            // ══════════════════════════════════════════════════════════

            Spacer(Modifier.height(12.dp))

            // ══════════════════════════════════════════════════════════
            // ADMIN SECTION
            // ══════════════════════════════════════════════════════════
            if (isAdmin) {
                PSectionHeader(
                    title            = "Administration",
                    accentColor      = GoldPrimary,
                    onSurfaceVariant = onSurfaceVariant
                )
                PProfileMenuItem(
                    icon           = Icons.Default.Dashboard,
                    label          = "Admin Dashboard",
                    surfaceColor   = surface,
                    onSurfaceColor = onSurface,
                    onClick        = { navController.navigate("admin_dashboard") }
                )
                PProfileMenuItem(
                    icon           = Icons.Default.VerifiedUser,
                    label          = "Verification Requests",
                    surfaceColor   = surface,
                    onSurfaceColor = onSurface,
                    onClick        = { navController.navigate("verify_users") }
                )
                PProfileMenuItem(
                    icon           = Icons.Default.People,
                    label          = "Manage Users",
                    surfaceColor   = surface,
                    onSurfaceColor = onSurface,
                    onClick        = { navController.navigate("manage_users") }
                )
            }

            // ══════════════════════════════════════════════════════════
            // ACCOUNT SETTINGS SECTION
            // ══════════════════════════════════════════════════════════
            PSectionHeader(
                title            = "Account Settings",
                accentColor      = GoldPrimary,
                onSurfaceVariant = onSurfaceVariant
            )

            if (!isAdmin) {
                PProfileMenuItem(
                    icon           = Icons.Default.BookOnline,
                    label          = "My Bookings",
                    surfaceColor   = surface,
                    onSurfaceColor = onSurface,
                    onClick        = { navController.navigate(Screen.MyBookings.route) }
                )
            }
            if (isLandlord) {
                PProfileMenuItem(
                    icon           = Icons.Default.Home,
                    label          = "My Properties",
                    surfaceColor   = surface,
                    onSurfaceColor = onSurface,
                    onClick        = { navController.navigate(Screen.MyProperties.route) }
                )
            }
            PProfileMenuItem(
                icon           = Icons.Default.Settings,
                label          = "Settings",
                surfaceColor   = surface,
                onSurfaceColor = onSurface,
                onClick        = { navController.navigate(Screen.Settings.route) }
            )
            PProfileMenuItem(
                icon           = Icons.AutoMirrored.Filled.HelpOutline,
                label          = "Help & Support",
                surfaceColor   = surface,
                onSurfaceColor = onSurface,
                onClick        = { navController.navigate(Screen.HelpAndSupport.route) }
            )

            Spacer(Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════
            // LOGOUT BUTTON
            // ══════════════════════════════════════════════════════════
            Button(
                onClick   = { authViewModel.signOut() },
                colors    = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .height(52.dp),
                shape     = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    null,
                    tint     = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Logout",
                    color         = Color.White,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(88.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// PRIVATE COMPOSABLES
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun PSectionHeader(
    title            : String,
    accentColor      : Color,
    onSurfaceVariant : Color
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text          = title,
            fontSize      = 12.sp,
            fontWeight    = FontWeight.Bold,
            color         = onSurfaceVariant,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun ProfileStat(
    value   : String,
    label   : String,
    fontSize: TextUnit,
    color   : Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontSize   = fontSize,
            fontWeight = FontWeight.Bold,
            color      = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = label,
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PProfileMenuItem(
    icon          : ImageVector,
    label         : String,
    surfaceColor  : Color,
    onSurfaceColor: Color,
    onClick       : () -> Unit
) {
    Surface(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        color    = surfaceColor
    ) {
        Column {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .background(GoldPrimary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint     = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    label,
                    fontSize   = 15.sp,
                    color      = onSurfaceColor,
                    modifier   = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint     = onSurfaceColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                thickness = 0.5.dp
            )
        }
    }
}
