package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// UnverifiedAccessScreen
//
// Shown when an unverified user (Tenant or Landlord) tries to access a
// feature that requires verification:
//   - Tenant tries to open BookingScreen
//   - Landlord tries to open AddPropertyScreen
//
// The user can only go back — no action is allowed until admin verifies them.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun UnverifiedAccessScreen(
    message : String,          // Custom message based on which feature was blocked
    onBack  : () -> Unit       // Navigate back to previous screen
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(20.dp)
        ) {

            // Lock icon in a rounded box
            Box(
                modifier         = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Lock,
                    contentDescription = "Access Restricted",
                    tint               = Color(0xFFE53935),
                    modifier           = Modifier.size(48.dp)
                )
            }

            // Title
            Text(
                "Verification Required",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1A1A2E),
                textAlign  = TextAlign.Center
            )

            // Custom message passed from NavGraph based on context
            Text(
                message,
                fontSize  = 14.sp,
                color     = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            // Info card explaining what the user should do
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("ℹ️", fontSize = 18.sp)
                    Text(
                        "Admin will verify your account and documents. " +
                                "You will be notified once your account is approved.",
                        fontSize   = 13.sp,
                        color      = Color(0xFF7B6200),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Go Back button
            Button(
                onClick  = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D1B3E),
                    contentColor   = Color.White
                )
            ) {
                Text(
                    "Go Back",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}