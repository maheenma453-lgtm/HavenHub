package com.example.havenhub.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.havenhub.ui.theme.*

// ── FAQ data model — unchanged ────────────────────────────────────────
data class FaqItem(val question: String, val answer: String)

val faqs = listOf(
    FaqItem("How do I book a property?",  "Browse properties, select one you like, choose your dates, and proceed to payment. Your booking will be confirmed instantly."),
    FaqItem("Can I cancel my booking?",   "Yes, you can cancel from 'My Bookings'. Cancellation policies vary per property. Check the property details for refund info."),
    FaqItem("How do I contact the host?", "Once your booking is confirmed, you can message the host directly through the app's chat feature."),
    FaqItem("Is my payment secure?",      "Yes, all payments are encrypted and processed through trusted payment gateways like JazzCash and EasyPaisa."),
    FaqItem("How do I list my property?", "Go to your Profile, tap 'My Properties', then 'Add Property'. Fill in the details and submit for verification.")
)

// ── Backward-compat dark tokens (kept, no logic removed) ─────────────
private val HAS_DarkBg      = DarkBg
private val HAS_DarkCard    = DarkSurfaceCard
private val HAS_DarkNavy    = DarkSurface
private val HAS_DarkGold    = DarkGoldPrimary
private val HAS_DarkTextPri = DarkTextPrimary
private val HAS_DarkTextSec = DarkTextSecondary
private val HAS_DarkBorder  = DarkBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportScreen(navController: NavController) {
    val isDark = isSystemInDarkTheme()
    val cs     = MaterialTheme.colorScheme

    var searchQuery by remember { mutableStateOf("") }
    val expandedFaq  = remember { mutableStateMapOf<Int, Boolean>() }

    val filteredFaqs = faqs.filter {
        searchQuery.isBlank() || it.question.contains(searchQuery, ignoreCase = true)
    }

    // ── Computed tokens from Color.kt / MaterialTheme ─────────────────
    val goldC        = if (isDark) DarkGoldPrimary else GoldAccent
    val sectionLabel = if (isDark) DarkGoldLight   else GoldAccentDark

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(PrimaryNavyDark, PrimaryNavy))
                    )
                    .statusBarsPadding()
                    .height(58.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GoldAccent.copy(alpha = 0.75f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        "Help & Support",
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 20.sp,
                        color         = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Search field ──────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation    = if (isDark) 0.dp else 4.dp,
                        shape        = RoundedCornerShape(14.dp),
                        ambientColor = PrimaryNavyDark.copy(alpha = 0.08f),
                        spotColor    = PrimaryNavyDark.copy(alpha = 0.13f)
                    ),
                shape          = RoundedCornerShape(14.dp),
                color          = cs.surface,
                tonalElevation = if (isDark) 3.dp else 0.dp
            ) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text("Search for help...", color = cs.onSurfaceVariant, fontSize = 14.sp)
                    },
                    leadingIcon   = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint     = goldC,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape      = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = goldC,
                        unfocusedBorderColor    = Color.Transparent,
                        focusedTextColor        = cs.onSurface,
                        unfocusedTextColor      = cs.onSurface,
                        cursorColor             = goldC,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            // ── CONTACT US ────────────────────────────────────────────
            HasSectionHeader(title = "CONTACT US", isDark = isDark)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HASContactCard(
                    icon     = Icons.Default.Chat,
                    label    = "Live Chat",
                    sub      = "Available 9AM-6PM",
                    isDark   = isDark,
                    modifier = Modifier.weight(1f)
                )
                HASContactCard(
                    icon     = Icons.Default.Email,
                    label    = "Email Us",
                    sub      = "support@havenhub.pk",
                    isDark   = isDark,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HASContactCard(
                    icon     = Icons.Default.Phone,
                    label    = "Call Us",
                    sub      = "0800-12345",
                    isDark   = isDark,
                    modifier = Modifier.weight(1f)
                )
                HASContactCard(
                    icon     = Icons.Default.Language,
                    label    = "Visit Website",
                    sub      = "havenhub.pk",
                    isDark   = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── FAQ ───────────────────────────────────────────────────
            HasSectionHeader(title = "FREQUENTLY ASKED QUESTIONS", isDark = isDark)

            filteredFaqs.forEachIndexed { index, faq ->
                val isExpanded = expandedFaq[index] == true

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation    = if (isDark) 0.dp else 2.dp,
                            shape        = RoundedCornerShape(14.dp),
                            ambientColor = PrimaryNavyDark.copy(alpha = 0.07f),
                            spotColor    = PrimaryNavyDark.copy(alpha = 0.11f)
                        ),
                    shape          = RoundedCornerShape(14.dp),
                    color          = cs.surface,
                    tonalElevation = if (isDark) 3.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.clickable {
                            expandedFaq[index] = !isExpanded
                        }
                    ) {
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Number badge
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        if (isDark) DarkGoldFaint.copy(alpha = 0.8f)
                                        else GoldAccent.copy(alpha = 0.11f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = goldC
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                faq.question,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = cs.onSurface,
                                modifier   = Modifier.weight(1f),
                                lineHeight = 20.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (isExpanded)
                                            if (isDark) DarkGoldFaint.copy(alpha = 0.8f)
                                            else GoldAccent.copy(alpha = 0.12f)
                                        else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = if (isExpanded) Icons.Default.ExpandLess
                                    else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint               = goldC,
                                    modifier           = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter   = expandVertically() + fadeIn(),
                            exit    = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(
                                    modifier  = Modifier.padding(horizontal = 16.dp),
                                    color     = cs.outline.copy(alpha = 0.30f),
                                    thickness = 0.5.dp
                                )
                                Row(
                                    modifier          = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Gold left accent bar
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(14.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        if (isDark) DarkGoldPrimary else GoldAccent,
                                                        if (isDark) DarkGoldDim     else GoldAccentDark
                                                    )
                                                ),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        faq.answer,
                                        fontSize   = 13.sp,
                                        color      = cs.onSurfaceVariant,
                                        lineHeight = 21.sp,
                                        modifier   = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────
@Composable
private fun HasSectionHeader(title: String, isDark: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isDark) DarkGoldPrimary else GoldAccent,
                            if (isDark) DarkGoldDim     else GoldAccentDark
                        )
                    ),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text          = title,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Bold,
            color         = if (isDark) DarkGoldLight else GoldAccentDark,
            letterSpacing = 1.1.sp
        )
    }
}

// ── Enhanced Contact Card ─────────────────────────────────────────────
@Composable
fun HASContactCard(
    icon    : ImageVector,
    label   : String,
    sub     : String,
    isDark  : Boolean  = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val cs    = MaterialTheme.colorScheme
    val goldC = if (isDark) DarkGoldPrimary else GoldAccent

    Surface(
        modifier = modifier
            .shadow(
                elevation    = if (isDark) 0.dp else 3.dp,
                shape        = RoundedCornerShape(14.dp),
                ambientColor = PrimaryNavyDark.copy(alpha = 0.08f),
                spotColor    = PrimaryNavyDark.copy(alpha = 0.12f)
            ),
        shape          = RoundedCornerShape(14.dp),
        color          = cs.surface,
        tonalElevation = if (isDark) 3.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .clickable { }
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (isDark) DarkGoldFaint.copy(alpha = 0.8f)
                        else GoldAccent.copy(alpha = 0.11f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = goldC,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = cs.onSurface
            )
            Text(
                sub,
                fontSize = 11.sp,
                color    = cs.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

// ── Backward-compat alias — no logic removed ──────────────────────────
@Composable
fun ContactCard(icon: ImageVector, label: String, sub: String, modifier: Modifier = Modifier) =
    HASContactCard(icon = icon, label = label, sub = sub, modifier = modifier)















