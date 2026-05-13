package com.example.havenhub.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// FAQ data model — unchanged
data class FaqItem(val question: String, val answer: String)

val faqs = listOf(
    FaqItem("How do I book a property?",   "Browse properties, select one you like, choose your dates, and proceed to payment. Your booking will be confirmed instantly."),
    FaqItem("Can I cancel my booking?",    "Yes, you can cancel from 'My Bookings'. Cancellation policies vary per property. Check the property details for refund info."),
    FaqItem("How do I contact the host?",  "Once your booking is confirmed, you can message the host directly through the app's chat feature."),
    FaqItem("Is my payment secure?",       "Yes, all payments are encrypted and processed through trusted payment gateways like JazzCash and EasyPaisa."),
    FaqItem("How do I list my property?",  "Go to your Profile, tap 'My Properties', then 'Add Property'. Fill in the details and submit for verification.")
)

// ── Dark theme tokens for HelpAndSupport ─────────────────────────────────────
private val HAS_DarkBg      = Color(0xFF060D1A)   // page background
private val HAS_DarkCard    = Color(0xFF112038)   // card surface
private val HAS_DarkNavy    = Color(0xFF0D1B3E)   // top bar
private val HAS_DarkGold    = Color(0xFFD4AF37)   // primary accent gold
private val HAS_DarkTextPri = Color(0xFFF0F4FF)   // primary text
private val HAS_DarkTextSec = Color(0xFF8899BB)   // secondary text
private val HAS_DarkBorder  = Color(0xFF1E2E50)   // divider color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportScreen(navController: NavController) {
    // ── Dark theme detection ──────────────────────────────────────────────────
    val isDark = isSystemInDarkTheme()

    // ── Theme-aware aliases ───────────────────────────────────────────────────
    val pageBg    = if (isDark) HAS_DarkBg      else MaterialTheme.colorScheme.background
    val topBarBg  = if (isDark) HAS_DarkNavy    else MaterialTheme.colorScheme.primary
    val cardBg    = if (isDark) HAS_DarkCard    else MaterialTheme.colorScheme.surfaceVariant
    val goldC     = if (isDark) HAS_DarkGold    else MaterialTheme.colorScheme.primary
    val textPri   = if (isDark) HAS_DarkTextPri else MaterialTheme.colorScheme.onSurface
    val textSec   = if (isDark) HAS_DarkTextSec else MaterialTheme.colorScheme.onSurfaceVariant
    val borderC   = if (isDark) HAS_DarkBorder  else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val iconTint  = if (isDark) HAS_DarkGold    else MaterialTheme.colorScheme.primary

    var searchQuery by remember { mutableStateOf("") }
    val expandedFaq = remember { mutableStateMapOf<Int, Boolean>() }

    val filteredFaqs = faqs.filter {
        searchQuery.isBlank() || it.question.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = pageBg,                                // dark: deep navy
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = topBarBg,      // dark: deep navy, light: material primary
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search field — dark aware
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("Search for help...", color = textSec) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = goldC) },
                shape         = RoundedCornerShape(12.dp),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = goldC,
                    unfocusedBorderColor    = if (isDark) HAS_DarkBorder else MaterialTheme.colorScheme.outline,
                    focusedTextColor        = if (isDark) HAS_DarkTextPri else MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor      = if (isDark) HAS_DarkTextPri else MaterialTheme.colorScheme.onSurface,
                    cursorColor             = goldC,
                    focusedContainerColor   = if (isDark) HAS_DarkCard else Color.Transparent,
                    unfocusedContainerColor = if (isDark) HAS_DarkCard else Color.Transparent
                )
            )

            // Contact section header
            Text(
                "Contact Us",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = goldC
            )

            // Contact cards grid
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HASContactCard(icon = Icons.Default.Chat,  label = "Live Chat",   sub = "Available 9AM-6PM",   isDark = isDark, modifier = Modifier.weight(1f))
                HASContactCard(icon = Icons.Default.Email, label = "Email Us",    sub = "support@havenhub.pk", isDark = isDark, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HASContactCard(icon = Icons.Default.Phone,    label = "Call Us",       sub = "0800-12345",   isDark = isDark, modifier = Modifier.weight(1f))
                HASContactCard(icon = Icons.Default.Language, label = "Visit Website", sub = "havenhub.pk",  isDark = isDark, modifier = Modifier.weight(1f))
            }

            // FAQ section header
            Text(
                "Frequently Asked Questions",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = goldC
            )

            // FAQ cards — dark aware
            filteredFaqs.forEachIndexed { index, faq ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier.clickable {
                            expandedFaq[index] = !(expandedFaq[index] ?: false)
                        }
                    ) {
                        // Question row
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                faq.question,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = textPri,
                                modifier   = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector        = if (expandedFaq[index] == true) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint               = if (isDark) HAS_DarkGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Answer — expanded
                        if (expandedFaq[index] == true) {
                            HorizontalDivider(color = borderC)
                            Text(
                                faq.answer,
                                fontSize   = 13.sp,
                                color      = textSec,
                                lineHeight = 20.sp,
                                modifier   = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Contact Card — dark theme aware ──────────────────────────────────────────
@Composable
fun HASContactCard(
    icon    : ImageVector,
    label   : String,
    sub     : String,
    isDark  : Boolean  = false,
    modifier: Modifier = Modifier
) {
    val cardBg  = if (isDark) HAS_DarkCard    else MaterialTheme.colorScheme.surfaceVariant
    val iconTnt = if (isDark) HAS_DarkGold    else MaterialTheme.colorScheme.primary
    val textPri = if (isDark) HAS_DarkTextPri else MaterialTheme.colorScheme.onSurface
    val textSec = if (isDark) HAS_DarkTextSec else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconTnt, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPri)
            Text(sub,   fontSize = 11.sp, color = textSec)
        }
    }
}

// Backward-compatible alias — no logic changed
@Composable
fun ContactCard(icon: ImageVector, label: String, sub: String, modifier: Modifier = Modifier) =
    HASContactCard(icon = icon, label = label, sub = sub, modifier = modifier)