package com.example.havenhub.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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

    val isDark = isSystemInDarkTheme()

    val screenBg    = if (isDark) DarkBg           else Color(0xFFF5F7FA)
    val cardBg      = if (isDark) DarkSurface       else SurfaceVariantLight
    val textPrimary = if (isDark) DarkTextPrimary   else TextPrimary
    val textSecond  = if (isDark) DarkTextSecondary else TextSecondary
    val accentColor = if (isDark) DarkGoldPrimary   else AccentGold
    val primaryBlue = if (isDark) DarkBgSecondary   else PrimaryBlue
    val borderCol   = if (isDark) DarkBorder        else BorderGray
    val topBarBg    = if (isDark) DarkBgSecondary   else PrimaryBlue

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

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = { Text("Write a Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = topBarBg,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── 1. PROPERTY SECTION ──────────────────────────────
            if (isPropertyPreset) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint     = if (isDark) DarkGoldPrimary else PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                propertyTitle.ifEmpty { "Property" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp,
                                color      = textPrimary
                            )
                            Text(
                                "Booking ID: #${bookingId.take(8).uppercase()}",
                                fontSize = 12.sp,
                                color    = textSecond
                            )
                        }
                    }
                }

            } else {
                Column {
                    Text(
                        "Select Property",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        color      = textPrimary
                    )
                    Spacer(Modifier.height(8.dp))

                    if (selectedPropertyId.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isDark) DarkBgSecondary.copy(0.4f)
                                    else PrimaryBlue.copy(0.08f)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null,
                                    tint     = if (isDark) DarkGoldPrimary else PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    selectedPropertyTitle.ifEmpty { selectedPropertyId },
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
                                    "Change",
                                    fontSize = 12.sp,
                                    color    = if (isDark) DarkGoldPrimary else PrimaryBlue
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
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
                                Icon(Icons.Default.Search, null, tint = textSecond)
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
                            shape  = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = if (isDark) DarkGoldPrimary else PrimaryBlue,
                                unfocusedBorderColor = borderCol,
                                focusedTextColor     = textPrimary,
                                unfocusedTextColor   = textPrimary
                            ),
                            singleLine = true
                        )

                        AnimatedVisibility(
                            visible = isSearchExpanded && (isSearching || searchResults.isNotEmpty() || searchQuery.length >= 2),
                            enter   = expandVertically(),
                            exit    = shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp),
                                shape     = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                                colors    = CardDefaults.cardColors(containerColor = cardBg),
                                elevation = CardDefaults.cardElevation(4.dp)
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
                                                color       = if (isDark) DarkGoldPrimary else PrimaryBlue
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
                                                accentColor = accentColor,
                                                onSelect    = {
                                                    selectedPropertyId    = property.propertyId
                                                    selectedPropertyTitle = property.title
                                                    searchQuery           = ""
                                                    isSearchExpanded      = false
                                                    viewModel.clearPropertySearch()
                                                }
                                            )
                                            HorizontalDivider(color = borderCol.copy(0.3f), thickness = 0.5.dp)
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
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── 2. OVERALL RATING ────────────────────────────────
            Column {
                Text(
                    "Overall Rating",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector        = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint     = if (star <= rating) accentColor else textSecond,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    if (rating > 0) {
                        Text(
                            "$rating/5",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = accentColor
                        )
                    }
                }
            }

            // ── 3. CATEGORY RATINGS ──────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Category Ratings",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = textPrimary
                )
                Spacer(Modifier.height(4.dp))
                CategoryRating("Cleanliness",    cleanliness) { cleanliness = it }
                CategoryRating("Location",        location)    { location    = it }
                CategoryRating("Value for Money", value)       { value       = it }
            }

            // ── 4. REVIEW TEXT ───────────────────────────────────
            Column {
                Text(
                    "Your Review",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = textPrimary
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = reviewText,
                    onValueChange = { if (it.length <= 500) reviewText = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = {
                        Text("Share your experience...", color = textSecond)
                    },
                    shape    = RoundedCornerShape(12.dp),
                    maxLines = 6,
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = accentColor,
                        focusedLabelColor    = if (isDark) DarkGoldPrimary else PrimaryBlue,
                        unfocusedBorderColor = borderCol,
                        focusedTextColor     = textPrimary,
                        unfocusedTextColor   = textPrimary
                    )
                )
                Text(
                    "${reviewText.length}/500 characters",
                    fontSize = 11.sp,
                    color    = textSecond,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }

            // ── 5. SUBMIT BUTTON ─────────────────────────────────
            val canSubmit = rating > 0 &&
                    reviewText.isNotBlank() &&
                    selectedPropertyId.isNotEmpty() &&
                    !uiState.isLoading

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
                    .height(50.dp),
                shape   = RoundedCornerShape(12.dp),
                enabled = canSubmit,
                colors  = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit Review", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!isPropertyPreset && selectedPropertyId.isEmpty()) {
                Text(
                    "⚠ Please select a property before submitting",
                    fontSize = 12.sp,
                    color    = if (isDark) Color(0xFFFFB74D) else Color(0xFFF59E0B)
                )
            }

            uiState.errorMessage?.let { error ->
                Text(text = error, color = ErrorRed, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Category Rating Row ───────────────────────────────────────────────────────
@Composable
private fun CategoryRating(
    label    : String,
    rating   : Int,
    onRating : (Int) -> Unit
) {
    val isDark      = isSystemInDarkTheme()
    val textPrimary = if (isDark) DarkTextPrimary   else TextPrimary
    val textSecond  = if (isDark) DarkTextSecondary else TextSecondary
    val accentColor = if (isDark) DarkGoldPrimary   else AccentGold

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color    = textPrimary,
            modifier = Modifier.width(120.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (1..5).forEach { star ->
                IconButton(
                    onClick  = { onRating(star) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector        = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint     = if (star <= rating) accentColor else textSecond,
                        modifier = Modifier.size(22.dp)
                    )
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
    onSelect   : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Home, null,
                tint     = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                property.title,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                "${property.city} · ${property.propertyTypeEnum.displayName()}",
                fontSize = 11.sp,
                color    = textSecond,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (property.averageRating > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star, null,
                    tint     = accentColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "%.1f".format(property.averageRating),
                    fontSize = 11.sp,
                    color    = textSecond
                )
            }
        }
    }
}
