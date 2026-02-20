package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onMyBookings: () -> Unit = {},
    onMyProperties: () -> Unit = {},
    onSettings: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = onEditProfile) { Icon(Icons.Default.Edit, "Edit") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("A", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Ali Hassan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("ali.hassan@email.com", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                        Text("Tenant", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                }
            }

            // Stats
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("4", "Bookings")
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem("2", "Reviews")
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem("1", "Properties")
            }

            HorizontalDivider()

            // Menu Items
            Spacer(Modifier.height(8.dp))
            ProfileMenuItem(icon = Icons.Default.BookOnline, label = "My Bookings", onClick = onMyBookings)
            ProfileMenuItem(icon = Icons.Default.Home, label = "My Properties", onClick = onMyProperties)
            ProfileMenuItem(icon = Icons.Default.Settings, label = "Settings", onClick = onSettings)
            ProfileMenuItem(icon = Icons.Default.Help, label = "Help & Support", onClick = {})
            ProfileMenuItem(icon = Icons.Default.Info, label = "About", onClick = {})

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            ProfileMenuItem(
                icon = Icons.Default.Logout,
                label = "Logout",
                onClick = onLogout,
                tint = Color(0xFFE53935)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text(label, fontSize = 15.sp, color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
            }
        }
    }
}
