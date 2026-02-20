package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────
// RoleSelectionScreen.kt
// PURPOSE : After signup, user selects their role:
//             • Regular User   → looking for properties to rent
//             • Property Owner → wants to list their properties
//           Role is saved in Firebase and drives entire app experience.
// NAVIGATION: RoleSelectionScreen → HomeScreen
// ─────────────────────────────────────────────────────────────────

@Composable
fun RoleSelectionScreen(
    navController : NavController,
    viewModel     : AuthViewModel = hiltViewModel()
) {

    // ── State: tracks which role card is selected ──────────────────
    // "USER" or "PROPERTY_OWNER"
    var selectedRole by remember { mutableStateOf("") }
    val isLoading    by viewModel.isLoading.collectAsState()

    // ── UI ─────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            // ── Header ────────────────────────────────────────────
            Text(
                text       = "I am a...",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = "Select your role to personalize your experience",
                fontSize  = 14.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Role Card: Regular User ───────────────────────────
            RoleCard(
                emoji       = "🏡",
                title       = "Regular User",
                description = "I'm looking for a property to rent for daily, weekly, or monthly stay.",
                isSelected  = selectedRole == "USER",
                onClick     = { selectedRole = "USER" }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Role Card: Property Owner ─────────────────────────
            RoleCard(
                emoji       = "🏗️",
                title       = "Property Owner",
                description = "I want to list my properties and manage bookings from renters.",
                isSelected  = selectedRole == "PROPERTY_OWNER",
                onClick     = { selectedRole = "PROPERTY_OWNER" }
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Continue Button ───────────────────────────────────
            Button(
                onClick = {
                    if (selectedRole.isNotEmpty()) {
                        // Save role to Firebase via ViewModel, then navigate
                        viewModel.setUserRole(selectedRole) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.RoleSelection.route) { inclusive = true }
                            }
                        }
                    }
                },
                enabled  = selectedRole.isNotEmpty() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = PrimaryBlue,
                    disabledContainerColor = BorderGray
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = BackgroundWhite,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text       = "Continue",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// RoleCard
// Reusable card composable for each role option
// Shows selected state with blue border and background tint
// ─────────────────────────────────────────────────────────────────
@Composable
private fun RoleCard(
    emoji       : String,
    title       : String,
    description : String,
    isSelected  : Boolean,
    onClick     : () -> Unit
) {
    // Dynamic colors based on selection state
    val borderColor     = if (isSelected) PrimaryBlue else BorderGray
    val backgroundColor = if (isSelected) PrimaryBlue.copy(alpha = 0.06f) else SurfaceGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // ── Emoji Icon ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) PrimaryBlue.copy(alpha = 0.12f)
                        else BackgroundWhite
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // ── Text Content ──────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isSelected) PrimaryBlue else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text      = description,
                    fontSize  = 13.sp,
                    color     = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── Selection Indicator ───────────────────────────────
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✓", color = BackgroundWhite, fontSize = 12.sp)
                }
            }
        }
    }
}


