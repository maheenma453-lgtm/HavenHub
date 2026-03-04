package com.example.havenhub.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // App Logo
            Box(
                modifier         = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }

            Text("HavenHub", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Version 1.0.0", fontSize = 13.sp, color = TextSecondary)
            Text(
                "HavenHub is a modern property rental platform connecting tenants with property owners across Pakistan.",
                fontSize   = 14.sp,
                color      = TextSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )

            HorizontalDivider(color = BorderGray)

            // App Info
            SettingsGroup(title = "App Info") {
                SettingsItem(icon = Icons.Default.Code,    label = "Version",      subtitle = "1.0.0 (Build 100)", onClick = {})
                SettingsItem(icon = Icons.Default.Update,  label = "Last Updated", subtitle = "November 2024",     onClick = {})
                SettingsItem(icon = Icons.Default.Android, label = "Platform",     subtitle = "Android",           onClick = {})
            }

            // Legal
            SettingsGroup(title = "Legal") {
                SettingsItem(icon = Icons.Default.Policy,    label = "Privacy Policy",  onClick = {})
                SettingsItem(icon = Icons.Default.Gavel,     label = "Terms of Service",onClick = {})
                SettingsItem(icon = Icons.Default.Copyright, label = "Licenses",        onClick = {})
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "© 2024 HavenHub. All rights reserved.",
                fontSize  = 12.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}