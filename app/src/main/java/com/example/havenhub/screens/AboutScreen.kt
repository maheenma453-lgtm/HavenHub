package com.example.havenhub.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.havenhub.R
import com.example.havenhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {

    val isDark = isSystemInDarkTheme()
    val cs     = MaterialTheme.colorScheme

    // ── Brushes from Color.kt tokens ──────────────────────────────────
    val heroGradient = Brush.verticalGradient(
        listOf(PrimaryNavyDark, PrimaryNavy, SecondaryBlueDark)
    )
    val goldRingGradient = Brush.linearGradient(
        colors = listOf(GoldAccent, GoldAccentLight, GoldAccent),
        start  = Offset(0f, 0f),
        end    = Offset(200f, 200f)
    )
    val goldAccentLine = Brush.horizontalGradient(
        listOf(Color.Transparent, GoldAccent.copy(alpha = 0.75f), Color.Transparent)
    )

    Scaffold(
        containerColor = cs.background,
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
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ══════════════════════════════════════════════════════════
            // HERO BANNER
            // ══════════════════════════════════════════════════════════
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .background(heroGradient)
                    .padding(top = 44.dp, bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                // Decorative atmospheric circles
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 80.dp, y = (-60).dp)
                        .background(Color.White.copy(alpha = 0.025f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-55).dp, y = 55.dp)
                        .background(GoldAccent.copy(alpha = 0.07f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .align(Alignment.Center)
                        .background(Color.White.copy(alpha = 0.012f), CircleShape)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Logo with gold ring + dark gap
                    Box(
                        modifier         = Modifier
                            .size(94.dp)
                            .drawBehind {
                                drawCircle(
                                    brush  = goldRingGradient,
                                    radius = size.minDimension / 2f
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(86.dp)
                                .background(PrimaryNavyDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter            = painterResource(id = R.drawable.havenhub),
                                contentDescription = "HavenHub Logo",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        "HavenHub",
                        fontSize      = 28.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "Version 1.0.0",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.55f)
                    )

                    // Gold gradient pill badge
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        GoldAccent.copy(alpha = 0.18f),
                                        GoldAccentLight.copy(alpha = 0.26f),
                                        GoldAccent.copy(alpha = 0.18f)
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

            Spacer(Modifier.height(4.dp))

            // ══════════════════════════════════════════════════════════
            // DESCRIPTION CARD
            // ══════════════════════════════════════════════════════════
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .shadow(
                        elevation    = if (isDark) 0.dp else 4.dp,
                        shape        = RoundedCornerShape(16.dp),
                        ambientColor = PrimaryNavyDark.copy(alpha = 0.08f),
                        spotColor    = PrimaryNavyDark.copy(alpha = 0.13f)
                    ),
                shape          = RoundedCornerShape(16.dp),
                color          = cs.surface,
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
                    icon    = Icons.Default.Android,
                    label   = "Platform",
                    subtitle = "Android",
                    isDark  = isDark,
                    divider = false
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
                    showChevron = true
                )
                AboutItem(
                    icon        = Icons.Default.Gavel,
                    label       = "Terms of Service",
                    isDark      = isDark,
                    divider     = true,
                    showChevron = true
                )
                AboutItem(
                    icon        = Icons.Default.Copyright,
                    label       = "Licenses",
                    isDark      = isDark,
                    divider     = false,
                    showChevron = true
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

// ─────────────────────────────────────────────────────────────────────
// PRIVATE COMPOSABLES
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSectionHeader(title: String, isDark: Boolean) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
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
private fun AboutCard(
    isDark : Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation    = if (isDark) 0.dp else 4.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = PrimaryNavyDark.copy(alpha = 0.08f),
                spotColor    = PrimaryNavyDark.copy(alpha = 0.13f)
            ),
        shape          = RoundedCornerShape(16.dp),
        color          = cs.surface,
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
    subtitle   : String?  = null,
    isDark     : Boolean,
    divider    : Boolean,
    showChevron: Boolean  = false
) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
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
