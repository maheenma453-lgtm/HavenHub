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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
// FIX: Correct Imports for your project structure
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // FIX: Sync with your ViewModel's UiState
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.stats

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", color = Color.White) },
                actions = {
                    IconButton(onClick = { /* Handle Notifications */ }) {
                        Icon(Icons.Default.Notifications, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)) // SurfaceGray
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header Greeting
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(PrimaryBlue, Color(0xFF003366))))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("Welcome, Admin 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Platform overview and management.", fontSize = 13.sp, color = Color.White.copy(0.8f))
                        }
                    }
                }

                // Stats Grid
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Platform Overview", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(10.dp))

                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminStatCard("🏠", "Properties", "${stats.totalProperties}", PrimaryBlue, Modifier.weight(1f))
                            AdminStatCard("👥", "Total Bookings", "${stats.totalBookings}", Color(0xFF00ACC1), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminStatCard("⏳", "Pending", "${stats.pendingBookings}", Color(0xFFE67E22), Modifier.weight(1f))
                            AdminStatCard("💰", "Revenue", "PKR ${"%,.0f".format(stats.totalEarnings)}", Color(0xFF2ECC71), Modifier.weight(1f))
                        }
                    }
                }

                // Quick Actions
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Quick Actions", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(10.dp))

                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionRow("Verify Properties", Icons.Default.CheckCircle, "verify_props") {
                            // navController.navigate(Screen.ManageProperties.route) // Adjust based on your routes
                        }
                        ActionRow("Manage Users", Icons.Default.People, "manage_users") {
                            // navController.navigate(Screen.Dashboard.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(emoji: String, label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ActionRow(label: String, icon: ImageVector, tag: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}