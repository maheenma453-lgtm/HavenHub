package com.example.havenhub.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.havenhub.R
import com.example.havenhub.ui.theme.*

private enum class LegalDialog { NONE, PRIVACY, TERMS, LICENSES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {

    val isDark = isSystemInDarkTheme()
    val cs     = MaterialTheme.colorScheme

    var activeDialog by remember { mutableStateOf(LegalDialog.NONE) }

    val goldAccentLine = Brush.horizontalGradient(
        listOf(Color.Transparent, GoldAccent.copy(alpha = 0.75f), Color.Transparent)
    )

    if (activeDialog != LegalDialog.NONE) {
        LegalContentDialog(
            title = when (activeDialog) {
                LegalDialog.PRIVACY  -> "Privacy Policy"
                LegalDialog.TERMS    -> "Terms of Service"
                LegalDialog.LICENSES -> "Licenses"
                LegalDialog.NONE     -> ""
            },
            content = when (activeDialog) {
                LegalDialog.PRIVACY  -> privacyPolicyText()
                LegalDialog.TERMS    -> termsOfServiceText()
                LegalDialog.LICENSES -> licensesText()
                LegalDialog.NONE     -> ""
            },
            isDark    = isDark,
            onDismiss = { activeDialog = LegalDialog.NONE }
        )
    }

    Scaffold(
        containerColor = if (isDark) cs.background else Color(0xFFF8F9FB),
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
                        .background(goldAccentLine)
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
                        "About",
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 20.sp,
                        color         = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ══════════════════════════════════════════════════════════
            // HERO SECTION
            // ══════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(PrimaryNavyDark, PrimaryNavy, SecondaryBlueDark)
                        )
                    )
                    .padding(top = 36.dp, bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // ✅ FIX: Logo circle bg = pure white
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color.White, CircleShape), // ✅ solid white
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter            = painterResource(id = R.drawable.havenhub),
                            contentDescription = "HavenHub Logo",
                            contentScale       = ContentScale.Fit,  // Fit so logo doesn't get cut
                            modifier           = Modifier
                                .size(90.dp)   // slightly smaller so white ring is visible
                                .clip(CircleShape)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "HavenHub",
                        fontSize      = 30.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "Version 1.0.0",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.65f)
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        GoldAccent.copy(alpha = 0.25f),
                                        GoldAccentLight.copy(alpha = 0.35f),
                                        GoldAccent.copy(alpha = 0.25f)
                                    )
                                ),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 22.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "Property Rental Platform",
                            fontSize      = 12.sp,
                            color         = GoldAccentLight,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════════════════════════
            // DESCRIPTION CARD
            // ══════════════════════════════════════════════════════════
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation    = if (isDark) 0.dp else 3.dp,
                        shape        = RoundedCornerShape(16.dp),
                        ambientColor = PrimaryNavyDark.copy(alpha = 0.08f),
                        spotColor    = PrimaryNavyDark.copy(alpha = 0.13f)
                    ),
                shape          = RoundedCornerShape(16.dp),
                color          = if (isDark) cs.surface else Color.White,
                tonalElevation = if (isDark) 3.dp else 0.dp
            ) {
                Row(
                    modifier          = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isDark) DarkGoldFaint.copy(alpha = 0.8f)
                                else GoldAccent.copy(alpha = 0.11f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint     = if (isDark) DarkGoldPrimary else GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "HavenHub is a modern property rental platform connecting tenants with property owners across Pakistan.",
                        fontSize   = 14.sp,
                        color      = cs.onSurfaceVariant,
                        lineHeight = 22.sp,
                        modifier   = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ══════════════════════════════════════════════════════════
            // APP INFO
            // ══════════════════════════════════════════════════════════
            AboutSectionHeader(title = "APP INFO", isDark = isDark)

            AboutCard(isDark = isDark) {
                AboutItem(
                    icon     = Icons.Default.Code,
                    label    = "Version",
                    subtitle = "1.0.0 (Build 100)",
                    isDark   = isDark,
                    divider  = true
                )
                AboutItem(
                    icon     = Icons.Default.Update,
                    label    = "Last Updated",
                    subtitle = "November 2024",
                    isDark   = isDark,
                    divider  = true
                )
                AboutItem(
                    icon     = Icons.Default.Android,
                    label    = "Platform",
                    subtitle = "Android",
                    isDark   = isDark,
                    divider  = false
                )
            }

            // ══════════════════════════════════════════════════════════
            // LEGAL
            // ══════════════════════════════════════════════════════════
            AboutSectionHeader(title = "LEGAL", isDark = isDark)

            AboutCard(isDark = isDark) {
                AboutItem(
                    icon        = Icons.Default.Policy,
                    label       = "Privacy Policy",
                    isDark      = isDark,
                    divider     = true,
                    showChevron = true,
                    onClick     = { activeDialog = LegalDialog.PRIVACY }
                )
                AboutItem(
                    icon        = Icons.Default.Gavel,
                    label       = "Terms of Service",
                    isDark      = isDark,
                    divider     = true,
                    showChevron = true,
                    onClick     = { activeDialog = LegalDialog.TERMS }
                )
                AboutItem(
                    icon        = Icons.Default.Copyright,
                    label       = "Licenses",
                    isDark      = isDark,
                    divider     = false,
                    showChevron = true,
                    onClick     = { activeDialog = LegalDialog.LICENSES }
                )
            }

            Spacer(Modifier.height(28.dp))

            // ══════════════════════════════════════════════════════════
            // FOOTER
            // ══════════════════════════════════════════════════════════
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GoldAccent.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(1.dp)
                        )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "© 2024 HavenHub. All rights reserved.",
                    fontSize  = 12.sp,
                    color     = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Made with ❤️ in Pakistan",
                    fontSize  = 11.sp,
                    color     = cs.onSurfaceVariant.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(88.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LEGAL CONTENT DIALOG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LegalContentDialog(
    title    : String,
    content  : String,
    isDark   : Boolean,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier        = Modifier
                .fillMaxWidth(0.93f)
                .fillMaxHeight(0.82f),
            shape           = RoundedCornerShape(20.dp),
            color           = if (isDark) cs.surface else Color.White,
            tonalElevation  = if (isDark) 4.dp else 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryNavyDark, PrimaryNavy)),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            title,
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint               = Color.White.copy(alpha = 0.85f),
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        content,
                        fontSize   = 13.sp,
                        color      = cs.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick  = onDismiss,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) DarkGoldPrimary else GoldAccent
                        ),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(
                            "Close",
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isDark) PrimaryNavyDark else Color.White
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LEGAL TEXT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun privacyPolicyText() = """
PRIVACY POLICY
Last updated: November 2024

1. INFORMATION WE COLLECT
HavenHub collects information you provide directly to us, such as your name, email address, phone number, CNIC details, and profile picture when you register for an account.

2. HOW WE USE YOUR INFORMATION
We use the information we collect to:
- Provide, maintain, and improve our services
- Process transactions and send related information
- Send notifications about bookings, payments, and messages
- Verify your identity and prevent fraud

3. INFORMATION SHARING
We do not sell, trade, or rent your personal information to third parties. We may share your information with:
- Property owners/tenants to facilitate bookings
- Service providers who assist in our operations
- Law enforcement when required by law

4. DATA SECURITY
We implement appropriate security measures to protect your personal information against unauthorized access, alteration, disclosure, or destruction.

5. FCM NOTIFICATIONS
We use Firebase Cloud Messaging (FCM) to send push notifications to your device. Your FCM token is stored securely and used only to deliver relevant notifications.

6. YOUR RIGHTS
You have the right to:
- Access your personal information
- Correct inaccurate data
- Request deletion of your data
- Opt out of marketing communications

7. CONTACT US
If you have questions about this Privacy Policy, please contact us at support@havenhub.com
""".trimIndent()

@Composable
private fun termsOfServiceText() = """
TERMS OF SERVICE
Last updated: November 2024

1. ACCEPTANCE OF TERMS
By accessing or using HavenHub, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use our platform.

2. USER ACCOUNTS
- You must be at least 18 years old to use HavenHub
- You are responsible for maintaining the confidentiality of your account
- You must provide accurate and complete information during registration
- CNIC verification is required for tenants and landlords

3. PROPERTY LISTINGS
- Landlords must ensure all property information is accurate
- Properties are subject to admin verification before going live
- HavenHub reserves the right to remove listings that violate our policies

4. BOOKINGS AND PAYMENTS
- All bookings are subject to landlord approval
- Payments must be made through the platform
- Cancellation policies apply as stated in each listing
- A platform fee of 5% applies to all transactions

5. PROHIBITED ACTIVITIES
Users may not:
- Post false or misleading information
- Engage in fraudulent transactions
- Harass or abuse other users
- Violate any applicable laws or regulations

6. TERMINATION
HavenHub reserves the right to suspend or terminate accounts that violate these terms without prior notice.

7. LIMITATION OF LIABILITY
HavenHub is not liable for any indirect, incidental, or consequential damages arising from your use of the platform.

8. CONTACT
For questions about these Terms, contact us at legal@havenhub.com
""".trimIndent()

@Composable
private fun licensesText() = """
OPEN SOURCE LICENSES

HavenHub is built using the following open source libraries:

JETPACK COMPOSE
Copyright 2021 The Android Open Source Project
Apache License, Version 2.0
https://developer.android.com/jetpack/compose

FIREBASE ANDROID SDK
Copyright 2021 Google LLC
Apache License, Version 2.0
https://firebase.google.com

DAGGER HILT
Copyright 2020 The Dagger Authors
Apache License, Version 2.0
https://dagger.dev/hilt

KOTLIN COROUTINES
Copyright 2016 JetBrains s.r.o
Apache License, Version 2.0
https://github.com/Kotlin/kotlinx.coroutines

COIL
Copyright 2021 Coil Contributors
Apache License, Version 2.0
https://coil-kt.github.io/coil

OKHTTP
Copyright 2019 Square, Inc.
Apache License, Version 2.0
https://square.github.io/okhttp

KOTLINX DATETIME
Copyright 2019 JetBrains s.r.o
Apache License, Version 2.0
https://github.com/Kotlin/kotlinx-datetime

MATERIAL ICONS EXTENDED
Copyright 2021 The Android Open Source Project
Apache License, Version 2.0
https://fonts.google.com/icons

---
Apache License, Version 2.0

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
""".trimIndent()

// ─────────────────────────────────────────────────────────────────────────────
// PRIVATE COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AboutSectionHeader(title: String, isDark: Boolean) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
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
}

@Composable
private fun AboutCard(isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation    = if (isDark) 0.dp else 3.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = PrimaryNavyDark.copy(alpha = 0.08f),
                spotColor    = PrimaryNavyDark.copy(alpha = 0.13f)
            ),
        shape          = RoundedCornerShape(16.dp),
        color          = if (isDark) cs.surface else Color.White,
        tonalElevation = if (isDark) 3.dp else 0.dp
    ) {
        Column(content = content)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun AboutItem(
    icon       : ImageVector,
    label      : String,
    subtitle   : String?       = null,
    isDark     : Boolean,
    divider    : Boolean,
    showChevron: Boolean       = false,
    onClick    : (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
                    icon,
                    contentDescription = null,
                    tint     = if (isDark) DarkGoldPrimary else GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = cs.onSurface
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = cs.onSurfaceVariant)
                }
            }
            if (showChevron) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (isDark) DarkGoldFaint.copy(alpha = 0.6f)
                            else GoldAccent.copy(alpha = 0.09f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint     = if (isDark) DarkGoldPrimary.copy(alpha = 0.8f)
                        else GoldAccent.copy(alpha = 0.8f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        if (divider) {
            HorizontalDivider(
                modifier  = Modifier.padding(start = 70.dp, end = 16.dp),
                color     = cs.outline.copy(alpha = 0.30f),
                thickness = 0.5.dp
            )
        }
    }
}