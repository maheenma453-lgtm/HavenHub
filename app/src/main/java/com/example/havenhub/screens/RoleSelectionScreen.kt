package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel

@Composable
fun RoleSelectionScreen(
    navController : NavController,
    viewModel     : AuthViewModel = hiltViewModel()
) {
    // ── ViewModel State ───────────────────────────────────────────
    // AuthViewModel ab uiState (AuthUiState) use kar raha hai
    val uiState by viewModel.uiState.collectAsState()

    // UI Local State for visual selection
    var localSelectedRole by remember { mutableStateOf("") }

    // ── Navigation Logic ──────────────────────────────────────────
    // Jab isLoggedIn true ho jaye (SignUp success ke baad)
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.RoleSelection.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "I am a...",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Role Card: Regular User (Tenant)
            RoleCard(
                emoji = "🏡",
                title = "Regular User",
                description = "I'm looking for a property to rent for daily or monthly stay.",
                isSelected = localSelectedRole == "tenant",
                onClick = {
                    localSelectedRole = "tenant"
                    viewModel.onRoleSelected("tenant")
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Role Card: Property Owner (Landlord)
            RoleCard(
                emoji = "🏗️",
                title = "Property Owner",
                description = "I want to list my properties and manage bookings.",
                isSelected = localSelectedRole == "landlord",
                onClick = {
                    localSelectedRole = "landlord"
                    viewModel.onRoleSelected("landlord")
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Error Display
            uiState.errorMessage?.let {
                Text(text = it, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            // ── Continue Button ───────────────────────────────────
            Button(
                onClick = {
                    // Yahan aap signUp() call karenge kyunki aapke VM mein signUp role use karta hai
                    viewModel.signUp()
                },
                enabled = localSelectedRole.isNotEmpty() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = BorderGray
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = BackgroundWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(text = "Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoleCard(
    emoji: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryBlue else BorderGray
    val backgroundColor = if (isSelected) PrimaryBlue.copy(alpha = 0.06f) else SurfaceVariantLight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else BackgroundWhite),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryBlue else TextPrimary)
                Text(text = description, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }

            if (isSelected) {
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(PrimaryBlue), contentAlignment = Alignment.Center) {
                    Text(text = "✓", color = BackgroundWhite, fontSize = 12.sp)
                }
            }
        }
    }
}