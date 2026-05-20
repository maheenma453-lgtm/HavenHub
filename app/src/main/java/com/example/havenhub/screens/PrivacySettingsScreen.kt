package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    navController: NavController,
    viewModel    : SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs   = uiState.userPreferences
    val isDark  = isSystemInDarkTheme()
    val cs      = MaterialTheme.colorScheme

    // ── Dialog state ──────────────────────────────────────────────────────────
    var dialogTitle   by remember { mutableStateOf("") }
    var dialogContent by remember { mutableStateOf("") }
    var showDialog    by remember { mutableStateOf(false) }

    // Legal content — AppSettings se URL milti hai, yahan hardcoded fallback
    val privacyPolicyUrl   = uiState.appSettings?.privacyPolicyUrl   ?: "https://havenhub.co.za/privacy"
    val termsOfServiceUrl  = uiState.appSettings?.termsOfServiceUrl  ?: "https://havenhub.co.za/terms"
    val supportEmail       = uiState.appSettings?.supportEmail        ?: "support@havenhub.co.za"

    // ── Legal dialog ──────────────────────────────────────────────────────────
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cs.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Dialog title with gold accent
                    Text(
                        text       = dialogTitle,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isDark) DarkGoldPrimary else GoldAccent
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = cs.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))

                    // Dialog body content
                    Text(
                        text      = dialogContent,
                        fontSize  = 13.sp,
                        color     = cs.onSurface,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    // Close button
                    Button(
                        onClick = { showDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) DarkGoldPrimary else GoldAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(PrimaryNavyDark, PrimaryNavy)))
                    .statusBarsPadding()
                    .height(58.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GoldAccent.copy(alpha = 0.75f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(horizontal = 4.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = Color.White
                        )
                    }
                    Text(
                        text          = "Privacy Settings",
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 20.sp,
                        color         = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        },
        containerColor = cs.background
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = if (isDark) DarkGoldPrimary else GoldAccent)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(10.dp))

            // ── PROFILE VISIBILITY ────────────────────────────────────────────
            PrivacyGroup(title = "PROFILE VISIBILITY", isDark = isDark) {
                PrivacySwitchRow(
                    icon        = Icons.Default.Person,
                    label       = "Public Profile",
                    subtitle    = "Others can view your profile",
                    checked     = prefs?.isProfilePublic ?: true,
                    isDark      = isDark,
                    showDivider = true,
                    onCheckedChange = { newValue ->
                        prefs?.takeIf { it.userId.isNotBlank() }?.let { p ->
                            viewModel.savePreferences(p.copy(isProfilePublic = newValue))
                        }
                    }
                )
                PrivacySwitchRow(
                    icon        = Icons.Default.Phone,
                    label       = "Show Phone Number",
                    subtitle    = "Display phone on your profile",
                    checked     = prefs?.showPhoneNumber ?: false,
                    isDark      = isDark,
                    showDivider = true,
                    onCheckedChange = { newValue ->
                        prefs?.takeIf { it.userId.isNotBlank() }?.let { p ->
                            viewModel.savePreferences(p.copy(showPhoneNumber = newValue))
                        }
                    }
                )
                PrivacySwitchRow(
                    icon        = Icons.Default.Email,
                    label       = "Show Email",
                    subtitle    = "Display email on your profile",
                    checked     = prefs?.showEmail ?: false,
                    isDark      = isDark,
                    showDivider = false,
                    onCheckedChange = { newValue ->
                        prefs?.takeIf { it.userId.isNotBlank() }?.let { p ->
                            viewModel.savePreferences(p.copy(showEmail = newValue))
                        }
                    }
                )
            }

            // ── DATA & PERMISSIONS ────────────────────────────────────────────
            PrivacyGroup(title = "DATA & PERMISSIONS", isDark = isDark) {
                PrivacySwitchRow(
                    icon        = Icons.Default.LocationOn,
                    label       = "Location Access",
                    subtitle    = "Allow app to use your location",
                    checked     = prefs?.locationAccess ?: true,
                    isDark      = isDark,
                    showDivider = true,
                    onCheckedChange = { newValue ->
                        prefs?.takeIf { it.userId.isNotBlank() }?.let { p ->
                            viewModel.savePreferences(p.copy(locationAccess = newValue))
                        }
                    }
                )
                PrivacySwitchRow(
                    icon        = Icons.Default.Share,
                    label       = "Data Sharing",
                    subtitle    = "Share usage data to improve the app",
                    checked     = prefs?.dataSharing ?: false,
                    isDark      = isDark,
                    showDivider = false,
                    onCheckedChange = { newValue ->
                        prefs?.takeIf { it.userId.isNotBlank() }?.let { p ->
                            viewModel.savePreferences(p.copy(dataSharing = newValue))
                        }
                    }
                )
            }

            // ── LEGAL ─────────────────────────────────────────────────────────
            PrivacyGroup(title = "LEGAL", isDark = isDark) {
                PrivacyNavRow(
                    icon        = Icons.Default.Policy,
                    label       = "Privacy Policy",
                    isDark      = isDark,
                    showDivider = true,
                    onClick     = {
                        dialogTitle   = "Privacy Policy"
                        dialogContent = """
HavenHub Privacy Policy

Last updated: March 2026

1. INFORMATION WE COLLECT
We collect information you provide directly to us, such as when you create an account, make a booking, or contact us for support. This includes your name, email address, phone number, and payment information.

2. HOW WE USE YOUR INFORMATION
We use the information we collect to:
- Provide, maintain, and improve our services
- Process transactions and send related information
- Send promotional communications (with your consent)
- Monitor and analyse trends and usage

3. INFORMATION SHARING
We do not sell, trade, or rent your personal information to third parties. We may share your information with service providers who assist us in operating our platform.

4. DATA SECURITY
We implement appropriate technical and organisational measures to protect your personal information against unauthorised access, alteration, disclosure, or destruction.

5. YOUR RIGHTS
You have the right to access, correct, or delete your personal information at any time through your account settings.

6. CONTACT US
For privacy-related questions, contact us at:
$supportEmail

URL: $privacyPolicyUrl
                        """.trimIndent()
                        showDialog = true
                    }
                )
                PrivacyNavRow(
                    icon        = Icons.Default.Gavel,
                    label       = "Terms of Service",
                    isDark      = isDark,
                    showDivider = true,
                    onClick     = {
                        dialogTitle   = "Terms of Service"
                        dialogContent = """
HavenHub Terms of Service

Last updated: March 2026

1. ACCEPTANCE OF TERMS
By accessing and using HavenHub, you accept and agree to be bound by the terms and provisions of this agreement.

2. USE OF SERVICE
HavenHub provides a platform connecting property owners (landlords) with prospective tenants. You agree to use this service only for lawful purposes.

3. USER ACCOUNTS
You are responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account.

4. PROPERTY LISTINGS
Landlords are responsible for ensuring that all property information is accurate, complete, and not misleading. HavenHub reserves the right to remove any listing that violates our policies.

5. BOOKINGS AND PAYMENTS
All bookings made through HavenHub are subject to our booking and cancellation policies. Payments are processed securely through our payment partners.

6. LIMITATION OF LIABILITY
HavenHub shall not be liable for any indirect, incidental, special, or consequential damages resulting from your use of our services.

7. TERMINATION
We reserve the right to terminate or suspend your account at any time for violations of these terms.

8. CONTACT US
For terms-related questions:
$supportEmail

URL: $termsOfServiceUrl
                        """.trimIndent()
                        showDialog = true
                    }
                )
                PrivacyNavRow(
                    icon        = Icons.Default.Description,
                    label       = "Cookie Policy",
                    isDark      = isDark,
                    showDivider = false,
                    onClick     = {
                        dialogTitle   = "Cookie Policy"
                        dialogContent = """
HavenHub Cookie Policy

Last updated: March 2026

1. WHAT ARE COOKIES
Cookies are small text files that are stored on your device when you visit our platform. They help us provide you with a better experience.

2. HOW WE USE COOKIES
We use cookies to:
- Keep you signed in to your account
- Remember your preferences and settings
- Understand how you use our platform
- Improve our services based on usage patterns

3. TYPES OF COOKIES WE USE

Essential Cookies:
These are necessary for the platform to function properly. They cannot be disabled.

Preference Cookies:
These remember your settings such as language and dark mode preferences.

Analytics Cookies:
These help us understand how users interact with our platform (only if you have enabled Data Sharing in your privacy settings).

4. MANAGING COOKIES
You can control cookie settings through your device settings. Note that disabling certain cookies may affect the functionality of our platform.

5. CONTACT US
For cookie-related questions:
$supportEmail
                        """.trimIndent()
                        showDialog = true
                    }
                )
            }

            // ── Error ─────────────────────────────────────────────────────────
            uiState.errorMessage?.let { errorText ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = cs.errorContainer
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint     = cs.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(errorText, color = cs.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(88.dp))
        }
    }
}

// ── PrivacyGroup ──────────────────────────────────────────────────────────────
@Composable
private fun PrivacyGroup(
    title  : String,
    isDark : Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isDark) DarkGoldPrimary else GoldAccent,
                                if (isDark) DarkGoldDim     else GoldAccentDark
                            )
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text          = title,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = if (isDark) DarkGoldLight else GoldAccentDark,
                letterSpacing = 1.1.sp
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation    = if (isDark) 0.dp else 4.dp,
                    shape        = RoundedCornerShape(16.dp),
                    ambientColor = PrimaryNavyDark.copy(alpha = 0.09f),
                    spotColor    = PrimaryNavyDark.copy(alpha = 0.15f)
                ),
            shape          = RoundedCornerShape(16.dp),
            color          = cs.surface,
            tonalElevation = if (isDark) 3.dp else 0.dp
        ) {
            Column(content = content)
        }
    }
}

// ── PrivacySwitchRow ──────────────────────────────────────────────────────────
@Composable
private fun PrivacySwitchRow(
    icon           : ImageVector,
    label          : String,
    subtitle       : String,
    checked        : Boolean,
    isDark         : Boolean,
    showDivider    : Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isDark) DarkGoldFaint.copy(alpha = 0.8f)
                        else GoldAccent.copy(alpha = 0.11f),
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint     = if (isDark) DarkGoldPrimary else GoldAccent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = label,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = cs.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = cs.onSurfaceVariant
                )
            }
            Switch(
                checked         = checked,
                onCheckedChange = { onCheckedChange(it) },
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = Color.White,
                    checkedTrackColor   = if (isDark) DarkGoldPrimary else GoldAccent,
                    uncheckedThumbColor = cs.outline,
                    uncheckedTrackColor = cs.surfaceVariant
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier  = Modifier.padding(start = 70.dp, end = 16.dp),
                color     = cs.outline.copy(alpha = 0.30f),
                thickness = 0.5.dp
            )
        }
    }
}

// ── PrivacyNavRow ─────────────────────────────────────────────────────────────
@Composable
private fun PrivacyNavRow(
    icon       : ImageVector,
    label      : String,
    isDark     : Boolean,
    showDivider: Boolean,
    onClick    : () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isDark) DarkGoldFaint.copy(alpha = 0.8f)
                        else GoldAccent.copy(alpha = 0.11f),
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint     = if (isDark) DarkGoldPrimary else GoldAccent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text       = label,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = cs.onSurface,
                modifier   = Modifier.weight(1f)
            )
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint     = if (isDark) DarkGoldPrimary.copy(alpha = 0.8f)
                else GoldAccent.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier  = Modifier.padding(start = 70.dp, end = 16.dp),
                color     = cs.outline.copy(alpha = 0.30f),
                thickness = 0.5.dp
            )
        }
    }
}