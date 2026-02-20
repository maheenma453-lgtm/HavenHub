package com.example.havenhub.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FaqItem(val question: String, val answer: String)

val faqs = listOf(
    FaqItem("How do I book a property?", "Browse properties, select one you like, choose your dates, and proceed to payment. Your booking will be confirmed instantly."),
    FaqItem("Can I cancel my booking?", "Yes, you can cancel from 'My Bookings'. Cancellation policies vary per property. Check the property details for refund info."),
    FaqItem("How do I contact the host?", "Once your booking is confirmed, you can message the host directly through the app's chat feature."),
    FaqItem("Is my payment secure?", "Yes, all payments are encrypted and processed through trusted payment gateways like JazzCash and EasyPaisa."),
    FaqItem("How do I list my property?", "Go to your Profile, tap 'My Properties', then 'Add Property'. Fill in the details and submit for verification."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportScreen(onBack: () -> Unit = {}) {
    var searchQuery by remember { mutableStateOf("") }
    val expandedFaq = remember { mutableStateMapOf<Int, Boolean>() }

    val filteredFaqs = faqs.filter {
        searchQuery.isBlank() || it.question.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for help...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Contact Options
            Text("Contact Us", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContactCard(icon = Icons.Default.Chat, label = "Live Chat", sub = "Available 9AM–6PM", modifier = Modifier.weight(1f))
                ContactCard(icon = Icons.Default.Email, label = "Email Us", sub = "support@rentease.pk", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContactCard(icon = Icons.Default.Phone, label = "Call Us", sub = "0800-12345", modifier = Modifier.weight(1f))
                ContactCard(icon = Icons.Default.Language, label = "Visit Website", sub = "rentease.pk", modifier = Modifier.weight(1f))
            }

            // FAQs
            Text("Frequently Asked Questions", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)

            filteredFaqs.forEachIndexed { index, faq ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.clickable { expandedFaq[index] = !(expandedFaq[index] ?: false) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(faq.question, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(
                                if (expandedFaq[index] == true) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = Color.Gray
                            )
                        }
                        if (expandedFaq[index] == true) {
                            HorizontalDivider()
                            Text(faq.answer, fontSize = 13.sp, color = Color.Gray, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ContactCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sub: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, fontSize = 11.sp, color = Color.Gray)
        }
    }
}
