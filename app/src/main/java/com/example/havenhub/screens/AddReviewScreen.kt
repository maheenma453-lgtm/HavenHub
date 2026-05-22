package com.example.havenhub.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.ReviewViewModel

// ── Local design tokens ───────────────────────────────────────────────────────
private val NavyDeep      = Color(0xFF071020)
private val NavyPrimary   = Color(0xFF0D1B3E)
private val NavyMedium    = Color(0xFF1A3A6B)
private val GoldPrime     = Color(0xFFD4AF37)
private val GoldLight     = Color(0xFFF5D060)
private val GoldFaint     = Color(0xFFFFF8E1)
private val GoldDim       = Color(0xFFB8962E)

private val D_BgDeep      = Color(0xFF060D1A)
private val D_BgPrimary   = Color(0xFF0D1B3E)
private val D_BgCard      = Color(0xFF112038)
private val D_BgSecondary = Color(0xFF122040)
private val D_GoldPrime   = Color(0xFFD4AF37)
private val D_GoldLight   = Color(0xFFF5D060)
private val D_GoldFaint   = Color(0xFF1A1608)
private val D_TextPrim    = Color(0xFFF0F4FF)
private val D_TextSec     = Color(0xFF8899BB)
private val D_Border      = Color(0xFF1E2E50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(
    navController : NavController,
    propertyId    : String,
    bookingId     : String,
    propertyTitle : String,
    viewModel     : ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark  = isSystemInDarkTheme()

    // ── Theme tokens ──────────────────────────────────────────────
    val pageBg      = if (isDark) D_BgDeep    else Color(0xFFF0F4FA)
    val cardBg      = if (isDark) D_BgCard    else Color.White
    val headerGrad  = if (isDark)
        Brush.verticalGradient(listOf(D_BgDeep, D_BgPrimary))
    else
        Brush.verticalGradient(listOf(NavyDeep, NavyMedium))
    val goldBorder  = Brush.horizontalGradient(
        listOf(
            if (isDark) D_GoldPrime.copy(0.9f) else GoldPrime.copy(0.9f),
            if (isDark) D_GoldLight.copy(0.4f) else GoldLight.copy(0.5f),
            if (isDark) D_GoldPrime.copy(0.9f) else GoldPrime.copy(0.9f)
        )
    )
    val goldP       = if (isDark) D_GoldPrime  else GoldPrime
    val goldF       = if (isDark) D_GoldFaint  else GoldFaint
    val textPrimary = if (isDark) D_TextPrim   else NavyPrimary
    val textSecond  = if (isDark) D_TextSec    else Color(0xFF8899AA)
    val borderCol   = if (isDark) D_Border     else Color(0xFFE8ECF4)

    // ── State ─────────────────────────────────────────────────────
    var rating      by remember { mutableIntStateOf(0) }
    var reviewText  by remember { mutableStateOf("") }
    var cleanliness by remember { mutableIntStateOf(0) }
    var location    by remember { mutableIntStateOf(0) }
    var value       by remember { mutableIntStateOf(0) }

    val isPropertyPreset = propertyId.isNotEmpty()

    var selectedPropertyId    by remember { mutableStateOf(propertyId) }
    var selectedPropertyTitle by remember { mutableStateOf(propertyTitle) }
    var searchQuery           by remember { mutableStateOf("") }
    var isSearchExpanded      by remember { mutableStateOf(false) }

    val searchResults = uiState.propertySearchResults
    val isSearching   = uiState.isSearchingProperties

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2 && !isPropertyPreset) {
            viewModel.searchProperties(searchQuery)
        }
    }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearMessages()
        }
    }

    val canSubmit = rating > 0 &&
            reviewText.isNotBlank() &&
            selectedPropertyId.isNotEmpty() &&
            !uiState.isLoading

    Scaffold(
        containerColor = pageBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGrad)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(goldBorder)
                )
                TopAppBar(
                    title = {
                        Text(
                            "Write a Review",
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 18.sp,
                            color         = Color.White,
                            letterSpacing = 0.3.sp,
                            maxLines      = 1,
                            overflow      = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint               = goldP
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ══════════════════════════════════════════════════════
            // 1. PROPERTY CARD / SEARCH
            // ══════════════════════════════════════════════════════
            if (isPropertyPreset) {
                PremiumCard(cardBg = cardBg, goldBorder = goldBorder, isDark = isDark) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(goldP.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint               = goldP,
                                modifier           = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = propertyTitle.ifEmpty { "Property" },
                                fontWeight = FontWeight.Bold,
                                fontSize   = 14.sp,
                                color      = textPrimary,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.BookOnline,
                                    null,
                                    tint     = textSecond,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text     = "Booking #${bookingId.take(8).uppercase()}",
                                    fontSize = 11.sp,
                                    color    = textSecond
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(goldP.copy(0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text       = "Selected",
                                fontSize   = 10.sp,
                                color      = goldP,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // ── Search section ────────────────────────────────
                SectionLabel("Select Property", goldP)

                if (selectedPropertyId.isNotEmpty()) {
                    PremiumCard(cardBg = cardBg, goldBorder = goldBorder, isDark = isDark) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(goldP.copy(0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        null,
                                        tint     = goldP,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text       = selectedPropertyTitle.ifEmpty { selectedPropertyId },
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = textPrimary,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick        = {
                                    selectedPropertyId    = ""
                                    selectedPropertyTitle = ""
                                    searchQuery           = ""
                                    isSearchExpanded      = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text      = "Change",
                                    fontSize  = 12.sp,
                                    color     = goldP,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (selectedPropertyId.isEmpty()) {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = {
                            searchQuery      = it
                            isSearchExpanded = it.isNotEmpty()
                        },
                        modifier    = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Search property name or city...",
                                color    = textSecond,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = goldP)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery      = ""
                                    isSearchExpanded = false
                                    viewModel.clearPropertySearch()
                                }) {
                                    Icon(Icons.Default.Close, null, tint = textSecond)
                                }
                            }
                        },
                        shape  = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = goldP,
                            unfocusedBorderColor = borderCol,
                            focusedTextColor     = textPrimary,
                            unfocusedTextColor   = textPrimary,
                            cursorColor          = goldP
                        ),
                        singleLine = true
                    )

                    AnimatedVisibility(
                        visible = isSearchExpanded && (isSearching || searchResults.isNotEmpty() || searchQuery.length >= 2),
                        enter   = expandVertically(),
                        exit    = shrinkVertically()
                    ) {
                        Card(
                            modifier  = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            shape     = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
                            colors    = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (isSearching) {
                                    Row(
                                        modifier              = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier    = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color       = goldP
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Searching...", fontSize = 13.sp, color = textSecond)
                                    }
                                } else if (searchResults.isEmpty() && searchQuery.length >= 2) {
                                    Box(
                                        modifier         = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No properties found", fontSize = 13.sp, color = textSecond)
                                    }
                                } else {
                                    searchResults.forEach { property ->
                                        PropertySearchItem(
                                            property    = property,
                                            textPrimary = textPrimary,
                                            textSecond  = textSecond,
                                            accentColor = goldP,
                                            cardBg      = cardBg,
                                            onSelect    = {
                                                selectedPropertyId    = property.propertyId
                                                selectedPropertyTitle = property.title
                                                searchQuery           = ""
                                                isSearchExpanded      = false
                                                viewModel.clearPropertySearch()
                                            }
                                        )
                                        HorizontalDivider(
                                            color     = borderCol.copy(0.3f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (searchQuery.length < 2 && selectedPropertyId.isEmpty()) {
                        Text(
                            "Type at least 2 characters to search",
                            fontSize = 11.sp,
                            color    = textSecond,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════
            // 2. OVERALL RATING
            // ══════════════════════════════════════════════════════
            PremiumCard(cardBg = cardBg, goldBorder = goldBorder, isDark = isDark) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SectionLabel("Overall Rating", goldP)
                    Spacer(Modifier.height(12.dp))

                    // ── RESPONSIVE: use fillMaxWidth + SpaceEvenly so all 5 stars
                    //    fit on any screen size without clipping the last one ──────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (star <= rating) goldP.copy(0.14f)
                                        else Color.Transparent
                                    )
                                    .clickable { rating = star },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star stars",
                                    tint               = if (star <= rating) goldP else textSecond,
                                    modifier           = Modifier.size(30.dp)    // consistent size for all stars
                                )
                            }
                        }
                    }

                    if (rating > 0) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text       = ratingLabel(rating),
                                fontSize   = 13.sp,
                                color      = goldP,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(goldP.copy(0.14f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text       = "$rating / 5",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = goldP
                                )
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════
            // 3. CATEGORY RATINGS  (responsive row layout)
            // ══════════════════════════════════════════════════════
            PremiumCard(cardBg = cardBg, goldBorder = goldBorder, isDark = isDark) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SectionLabel("Category Ratings", goldP)
                    Spacer(Modifier.height(12.dp))
                    CategoryRatingRow(
                        label       = "Cleanliness",
                        emoji       = "🧹",
                        rating      = cleanliness,
                        goldP       = goldP,
                        textPrimary = textPrimary,
                        textSecond  = textSecond,
                        onRating    = { cleanliness = it }
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = 10.dp),
                        color     = if (isDark) D_Border else Color(0xFFE8ECF4),
                        thickness = 0.5.dp
                    )
                    CategoryRatingRow(
                        label       = "Location",
                        emoji       = "📍",
                        rating      = location,
                        goldP       = goldP,
                        textPrimary = textPrimary,
                        textSecond  = textSecond,
                        onRating    = { location = it }
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = 10.dp),
                        color     = if (isDark) D_Border else Color(0xFFE8ECF4),
                        thickness = 0.5.dp
                    )
                    CategoryRatingRow(
                        label       = "Value",
                        emoji       = "💰",
                        rating      = value,
                        goldP       = goldP,
                        textPrimary = textPrimary,
                        textSecond  = textSecond,
                        onRating    = { value = it }
                    )
                }
            }

            // ══════════════════════════════════════════════════════
            // 4. REVIEW TEXT
            // ══════════════════════════════════════════════════════
            PremiumCard(cardBg = cardBg, goldBorder = goldBorder, isDark = isDark) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SectionLabel("Your Review", goldP)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = reviewText,
                        onValueChange = { if (it.length <= 500) reviewText = it },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        placeholder = {
                            Text(
                                "Share your experience with this property...",
                                color    = textSecond,
                                fontSize = 13.sp
                            )
                        },
                        shape    = RoundedCornerShape(12.dp),
                        maxLines = 6,
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = goldP,
                            unfocusedBorderColor = if (isDark) D_Border else Color(0xFFE8ECF4),
                            focusedTextColor     = textPrimary,
                            unfocusedTextColor   = textPrimary,
                            cursorColor          = goldP
                        )
                    )
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        if (reviewText.isNotEmpty()) {
                            Text(
                                text     = if (reviewText.length < 20) "Add more details" else "Looking good!",
                                fontSize = 11.sp,
                                color    = if (reviewText.length < 20) textSecond else goldP,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Text(
                            text     = "${reviewText.length}/500",
                            fontSize = 11.sp,
                            color    = if (reviewText.length > 450) Color(0xFFE53935) else textSecond
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════
            // 5. SUBMIT BUTTON
            // ══════════════════════════════════════════════════════
            Button(
                onClick = {
                    viewModel.addReview(
                        propertyId        = selectedPropertyId,
                        bookingId         = bookingId,
                        rating            = rating.toFloat(),
                        comment           = reviewText,
                        cleanlinessRating = cleanliness.toFloat(),
                        locationRating    = location.toFloat(),
                        valueRating       = value.toFloat()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(if (canSubmit) 8.dp else 0.dp, RoundedCornerShape(14.dp)),
                shape   = RoundedCornerShape(14.dp),
                enabled = canSubmit,
                colors  = ButtonDefaults.buttonColors(
                    containerColor         = NavyPrimary,
                    disabledContainerColor = if (isDark) D_BgCard else Color(0xFFE0E4EF)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = goldP,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.RateReview,
                        contentDescription = null,
                        tint     = if (canSubmit) goldP else textSecond,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "Submit Review",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (canSubmit) goldP else textSecond,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            // ── Warnings / Errors ─────────────────────────────────
            if (!isPropertyPreset && selectedPropertyId.isEmpty()) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF3CD))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint     = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Please select a property before submitting",
                        fontSize = 12.sp,
                        color    = Color(0xFF92400E)
                    )
                }
            }

            uiState.errorMessage?.let { error ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        tint     = Color(0xFFE53935),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(error, fontSize = 12.sp, color = Color(0xFFB71C1C))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun ratingLabel(rating: Int) = when (rating) {
    1    -> "😞  Poor"
    2    -> "😐  Fair"
    3    -> "🙂  Good"
    4    -> "😊  Very Good"
    5    -> "🤩  Excellent!"
    else -> ""
}

@Composable
private fun SectionLabel(text: String, goldP: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(goldP)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text          = text,
            fontWeight    = FontWeight.Bold,
            fontSize      = 15.sp,
            color         = goldP,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun PremiumCard(
    cardBg     : Color,
    goldBorder : Brush,
    isDark     : Boolean,
    content    : @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDark) 0.dp else 4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                brush = goldBorder,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(goldBorder)
        )
        content()
    }
}

// ── RESPONSIVE Category Rating Row ───────────────────────────────────────────
// Label is flexible (weight), stars use SpaceEvenly in a fixed Box so all
// 5 stars always render at the same size regardless of screen width.
@Composable
private fun CategoryRatingRow(
    label       : String,
    emoji       : String,
    rating      : Int,
    goldP       : Color,
    textPrimary : Color,
    textSecond  : Color,
    onRating    : (Int) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji + label — flexible
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text       = label,
            fontSize   = 12.sp,
            color      = textPrimary,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.weight(1f)       // takes remaining space
        )
        Spacer(Modifier.width(6.dp))

        // Stars — fixed 160dp box, SpaceEvenly ensures uniform sizing
        Box(modifier = Modifier.width(160.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (star <= rating) goldP.copy(0.10f)
                                else Color.Transparent
                            )
                            .clickable { onRating(star) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star stars",
                            tint               = if (star <= rating) goldP else textSecond,
                            modifier           = Modifier.size(20.dp)   // uniform for all 5
                        )
                    }
                }
            }
        }
    }
}

// ── Property Search Result Item ───────────────────────────────────────────────
@Composable
private fun PropertySearchItem(
    property   : Property,
    textPrimary: Color,
    textSecond : Color,
    accentColor: Color,
    cardBg     : Color,
    onSelect   : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Home, null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                property.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${property.city} · ${property.propertyTypeEnum.displayName()}",
                fontSize = 11.sp,
                color = textSecond,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (property.averageRating > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(0.10f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    Icons.Default.Star, null,
                    tint = accentColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "%.1f".format(property.averageRating),
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}