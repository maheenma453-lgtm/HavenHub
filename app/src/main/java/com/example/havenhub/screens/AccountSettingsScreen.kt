package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(onBack: () -> Unit = {}) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Change Password
            Text("Change Password", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = { Text("Current Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("New Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm New Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), singleLine = true)
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), enabled = newPassword.isNotBlank() && newPassword == confirmPassword) {
                        Text("Update Password")
                    }
                }
            }

            // Linked Accounts
            Text("Linked Accounts", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            SettingsGroup(title = "") {
                SettingsItem(icon = Icons.Default.Email, label = "Google", subtitle = "ali.hassan@gmail.com", onClick = {})
                SettingsItem(icon = Icons.Default.Phone, label = "Phone Number", subtitle = "+92 300 1234567", onClick = {})
            }

            // Danger Zone
            Text("Danger Zone", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFFE53935))
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete Account")
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Account") },
                text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
