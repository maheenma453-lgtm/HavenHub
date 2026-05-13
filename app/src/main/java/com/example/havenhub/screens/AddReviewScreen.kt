package com.example.havenhub.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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

    // ── Dark / Light theme detection ──────────────────────────────
    val isDark = isSystemInDarkTheme()

    // Theme-aware color tokens
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

            // ── Property Info Card ─────────────────────────────────
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
                            propertyTitle,
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

            // ── Overall Rating ─────────────────────────────────────
            Text("Overall Rating", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        color      = accentColor,
                        modifier   = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            // ── Category Ratings ───────────────────────────────────
            Text("Category Ratings", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textPrimary)
            CategoryRating("Cleanliness",    cleanliness) { cleanliness = it }
            CategoryRating("Location",       location)    { location    = it }
            CategoryRating("Value for Money", value)      { value       = it }

            // ── Review Text ────────────────────────────────────────
            Text("Your Review", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textPrimary)
            OutlinedTextField(
                value         = reviewText,
                onValueChange = { if (it.length <= 500) reviewText = it },
                modifier      = Modifier.fillMaxWidth().height(140.dp),
                placeholder   = { Text("Share your experience...", color = textSecond) },
                shape         = RoundedCornerShape(12.dp),
                maxLines      = 6,
                colors        = OutlinedTextFieldDefaults.colors(
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
                modifier = Modifier.align(Alignment.End)
            )

            // ── Submit Button ──────────────────────────────────────
            Button(
                onClick = {
                    viewModel.addReview(
                        propertyId        = propertyId,
                        bookingId         = bookingId,
                        rating            = rating.toFloat(),
                        comment           = reviewText,
                        cleanlinessRating = cleanliness.toFloat(),
                        locationRating    = location.toFloat(),
                        valueRating       = value.toFloat()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                enabled  = rating > 0 && reviewText.isNotBlank() && !uiState.isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Review", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Error Message ──────────────────────────────────────
            uiState.errorMessage?.let { error ->
                Text(text = error, color = ErrorRed, fontSize = 14.sp)
            }
        }
    }
}

// ── Category Rating Row ────────────────────────────────────────────
@Composable
fun CategoryRating(label: String, value: Int, onSelect: (Int) -> Unit) {
    val isDark      = isSystemInDarkTheme()
    val textSecond  = if (isDark) DarkTextSecondary else TextSecondary
    val accentColor = if (isDark) DarkGoldPrimary   else AccentGold

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = textSecond, modifier = Modifier.width(130.dp))
        Row {
            (1..5).forEach { star ->
                IconButton(onClick = { onSelect(star) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector        = if (star <= value) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint     = if (star <= value) accentColor else textSecond,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}