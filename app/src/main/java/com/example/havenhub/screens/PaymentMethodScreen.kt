package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PaymentMethod(val id: String, val name: String, val detail: String, val icon: ImageVector, val isDefault: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(onBack: () -> Unit = {}, onAddNew: () -> Unit = {}) {
    val methods = remember {
        mutableStateListOf(
            PaymentMethod("1", "JazzCash", "**** 4521", Icons.Default.Payment, true),
            PaymentMethod("2", "EasyPaisa", "**** 8832", Icons.Default.Payment),
            PaymentMethod("3", "Visa Card", "**** 1234", Icons.Default.CreditCard),
        )
    }
    var defaultId by remember { mutableStateOf("1") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onAddNew) { Icon(Icons.Default.Add, "Add") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Saved Methods", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
            }
            items(methods) { method ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (defaultId == method.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(method.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(method.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(method.detail, fontSize = 12.sp, color = Color.Gray)
                            if (defaultId == method.id) {
                                Text("Default", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (defaultId != method.id) {
                            TextButton(onClick = { defaultId = method.id }) { Text("Set Default", fontSize = 12.sp) }
                        }
                        IconButton(onClick = { methods.remove(method) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAddNew,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Payment Method")
                }
            }
        }
    }
}