package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.DashboardViewModel

// ─────────────────────────────────────────────────────────────────
// AdminDashboardScreen.kt
// PURPOSE : Central admin control panel.
//           Shows key platform stats: total users, properties,
//           bookings, revenue. Quick access to all admin sections.
//           Only accessible after admin login (role = ADMIN).
// NAVIGATION:
//   → VerifyPropertiesScreen
//   → VerifyUsersScreen
//   → ManageUsersScreen
//   → ManagePropertiesScreen
//   → ManageBookingsScreen
//   → ReportsScreen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController : NavController,
    viewModel     : DashboardViewModel = hiltViewModel()
) {

    LaunchedEffect(Unit) { viewModel.loadDashboardStats() }

    val stats     by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // ── UI ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        Icon(Icons.Default.Notifications, null, tint = BackgroundWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = PrimaryBlue,
                    titleContentColor = BackgroundWhite,
                    actionIconContentColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .background(SurfaceGray)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Header Greeting ────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(PrimaryBlue, PrimaryDark)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text       = "Welcome, Admin 👋",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = BackgroundWhite
                        )
                        Text(
                            text     = "Here's your platform overview",
                            fontSize = 13.sp,
                            color    = BackgroundWhite.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ── Stats Grid (2×2) ───────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text       = "Platform Overview",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminStatCard("👥", "Total Users",      "${stats?.totalUsers ?: 0}",      "+12%",  PrimaryBlue,   Modifier.weight(1f))
                        AdminStatCard("🏠", "Properties",       "${stats?.totalProperties ?: 0}", "+8%",   AccentCyan,    Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminStatCard("📅", "Total Bookings",   "${stats?.totalBookings ?: 0}",   "+20%",  SuccessGreen,  Modifier.weight(1f))
                        AdminStatCard("💰", "Revenue (PKR)",    "${stats?.totalRevenue ?: 0}",    "+15%",  StarGold,      Modifier.weight(1f))
                    }
                }
            }

            // ── Pending Actions Card ───────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                if ((stats?.pendingVerifications ?: 0) > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = WarningAmber.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier          = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = "${stats?.pendingVerifications} Pending Verifications",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = WarningAmber
                                )
                                Text(
                                    text     = "Properties and users awaiting review",
                                    fontSize = 12.sp,
                                    color    = TextSecondary
                                )
                            }
                            TextButton(onClick = { navController.navigate(Screen.VerifyProperties.route) }) {
                                Text("Review", color = WarningAmber, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Quick Actions Grid ─────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text       = "Quick Actions",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminActionCard("🏠", "Verify\nProperties", Modifier.weight(1f)) {
                            navController.navigate(Screen.VerifyProperties.route)
                        }
                        AdminActionCard("👤", "Verify\nUsers", Modifier.weight(1f)) {
                            navController.navigate(Screen.VerifyUsers.route)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminActionCard("👥", "Manage\nUsers", Modifier.weight(1f)) {
                            navController.navigate(Screen.ManageUsers.route)
                        }
                        AdminActionCard("📋", "Manage\nBookings", Modifier.weight(1f)) {
                            navController.navigate(Screen.ManageBookings.route)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminActionCard("📊", "Reports", Modifier.weight(1f)) {
                            navController.navigate(Screen.Reports.route)
                        }
                        AdminActionCard("🏗️", "All\nProperties", Modifier.weight(1f)) {
                            navController.navigate(Screen.ManageProperties.route)
                        }
                    }
                }
            }
        }
    }
}

// ── Admin stat card ───────────────────────────────────────────────
@Composable
private fun AdminStatCard(
    emoji    : String,
    label    : String,
    value    : String,
    change   : String,
    color    : androidx.compose.ui.graphics.Color,
    modifier : Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(emoji, fontSize = 22.sp)
                Text(text = change, fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ── Admin quick action card ───────────────────────────────────────
@Composable
private fun AdminActionCard(emoji: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier  = modifier.height(80.dp).clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(emoji, fontSize = 24.sp)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, lineHeight = 18.sp)
        }
    }
}


