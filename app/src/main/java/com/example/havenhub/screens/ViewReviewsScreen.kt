package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Review
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.ReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReviewsScreen(
    navController: NavController,
    propertyId   : String,
    viewModel    : ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark   = isSystemInDarkTheme()

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState  by authViewModel.uiState.collectAsState()
    val isLandlord  = authState.userRole == "landlord"

    // ── Theme tokens ──────────────────────────────────────────────────────────
    val screenBg  = if (isDark) DarkBg          else BackgroundLight
    val gold      = if (isDark) DarkGoldPrimary  else GoldAccent
    val navy      = if (isDark) DarkBgSecondary  else PrimaryNavy
    val onNavy    = if (isDark) DarkTextPrimary  else Color.White
    val cardBg    = if (isDark) DarkSurface      else SurfaceWhite
    val borderCol = if (isDark) DarkBorder       else Color(0xFFE8EAF0)

    LaunchedEffect(propertyId) {
        viewModel.loadPropertyReviews(propertyId)
    }

    // ── Dialog state ──────────────────────────────────────────────────────────
    var showReplyDialog   by remember { mutableStateOf(false) }
    var replyTargetReview by remember { mutableStateOf<Review?>(null) }
    var replyText         by remember { mutableStateOf("") }

    var showDeleteDialog   by remember { mutableStateOf(false) }
    var deleteTargetReview by remember { mutableStateOf<Review?>(null) }

    LaunchedEffect(uiState.replySuccess) {
        if (uiState.replySuccess) {
            showReplyDialog   = false
            replyText         = ""
            replyTargetReview = null
            viewModel.clearReplySuccess()
        }
    }

    // ── Delete Confirmation Dialog ────────────────────────────────────────────
    if (showDeleteDialog && deleteTargetReview != null) {
        Dialog(onDismissRequest = { showDeleteDialog = false; deleteTargetReview = null }) {
            Card(
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.border(1.dp, borderCol, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(DarkError.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            null,
                            tint     = DarkError,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        "Delete Review?",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = if (isDark) DarkTextPrimary else PrimaryNavy
                    )
                    Text(
                        "Review by \"${deleteTargetReview!!.reviewerName}\" will be permanently removed.",
                        fontSize   = 13.sp,
                        lineHeight = 20.sp,
                        color      = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showDeleteDialog = false; deleteTargetReview = null },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
                        ) {
                            Text(
                                "Cancel",
                                color = if (isDark) DarkTextSecondary else PrimaryNavy
                            )
                        }
                        Button(
                            onClick = {
                                deleteTargetReview?.let {
                                    viewModel.deleteReview(reviewId = it.reviewId, propertyId = propertyId)
                                }
                                showDeleteDialog = false; deleteTargetReview = null
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = DarkError)
                        ) {
                            Icon(Icons.Default.Delete, null,
                                tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // ── Reply Dialog ──────────────────────────────────────────────────────────
    if (showReplyDialog && replyTargetReview != null) {
        Dialog(onDismissRequest = {
            showReplyDialog = false; replyText = ""; replyTargetReview = null
        }) {
            Card(
                shape    = RoundedCornerShape(24.dp),
                colors   = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.border(1.dp, borderCol, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier         = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(gold.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Reply, null,
                                tint = gold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Reply to Review",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp,
                                color      = if (isDark) DarkTextPrimary else PrimaryNavy
                            )
                            Text(
                                "by ${replyTargetReview!!.reviewerName}",
                                fontSize = 12.sp,
                                color    = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                            )
                        }
                    }

                    // Original review preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDark) DarkBgSecondary else Color(0xFFF5F6FA)
                            )
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            replyTargetReview!!.comment,
                            fontSize   = 13.sp,
                            lineHeight = 19.sp,
                            color      = if (isDark) DarkTextSecondary else Color(0xFF8899AA),
                            maxLines   = 3
                        )
                    }

                    // Reply field
                    OutlinedTextField(
                        value         = replyText,
                        onValueChange = { if (it.length <= 300) replyText = it },
                        modifier      = Modifier.fillMaxWidth().height(110.dp),
                        placeholder   = {
                            Text("Write your response...",
                                color = if (isDark) DarkTextMuted else Color(0xFFAAAAAA))
                        },
                        shape   = RoundedCornerShape(14.dp),
                        maxLines = 5,
                        colors  = OutlinedTextFieldDefaults.colors(
                            focusedTextColor        = if (isDark) DarkTextPrimary else PrimaryNavy,
                            unfocusedTextColor      = if (isDark) DarkTextPrimary else PrimaryNavy,
                            focusedBorderColor      = gold,
                            unfocusedBorderColor    = borderCol,
                            focusedContainerColor   = if (isDark) DarkBgSecondary else Color(0xFFF8F9FC),
                            unfocusedContainerColor = if (isDark) DarkBgSecondary else Color(0xFFF8F9FC),
                            cursorColor             = gold
                        )
                    )
                    Text(
                        "${replyText.length}/300",
                        fontSize = 11.sp,
                        color    = if (isDark) DarkTextMuted else Color(0xFFAAAAAA),
                        modifier = Modifier.align(Alignment.End)
                    )

                    // Buttons
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = {
                                showReplyDialog = false; replyText = ""; replyTargetReview = null
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, gold),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = gold)
                        ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }

                        Button(
                            onClick = {
                                replyTargetReview?.let { review ->
                                    viewModel.replyToReview(
                                        reviewId     = review.reviewId,
                                        propertyId   = propertyId,
                                        reply        = replyText,
                                        tenantId     = review.reviewerId,
                                        reviewerName = review.reviewerName
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape    = RoundedCornerShape(12.dp),
                            enabled  = replyText.isNotBlank() && !uiState.isReplyLoading,
                            colors   = ButtonDefaults.buttonColors(containerColor = navy)
                        ) {
                            if (uiState.isReplyLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color       = gold
                                )
                            } else {
                                Text("Submit", fontWeight = FontWeight.Bold, color = gold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Main Scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        containerColor = screenBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (isDark)
                                listOf(DarkBg, DarkBgSecondary, DarkBgTertiary)
                            else
                                listOf(PrimaryNavyDark, PrimaryNavy, PrimaryNavyLight)
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Reviews",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = onNavy
                            )
                            if (uiState.reviews.isNotEmpty()) {
                                Text(
                                    "${uiState.reviews.size} guest review${if (uiState.reviews.size != 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color    = gold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = onNavy)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = gold, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding).background(screenBg),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Rating summary card ───────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isDark) 1.dp else 0.5.dp,
                            color = borderCol,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Big rating number + stars
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.weight(1f)
                        ) {
                            Text(
                                String.format("%.1f", uiState.averageRating),
                                fontSize   = 52.sp,
                                fontWeight = FontWeight.Black,
                                color      = gold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                (1..5).forEach { s ->
                                    Icon(
                                        imageVector = if (s <= uiState.averageRating.toInt())
                                            Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint     = gold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${uiState.reviews.size} review${if (uiState.reviews.size != 1) "s" else ""}",
                                fontSize = 12.sp,
                                color    = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                            )
                        }

                        // Vertical divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(80.dp)
                                .background(borderCol)
                        )
                        Spacer(Modifier.width(16.dp))

                        // Bar breakdown
                        Column(
                            modifier            = Modifier.weight(2f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            (5 downTo 1).forEach { star ->
                                val count = uiState.reviews.count { it.overallRating.toInt() == star }
                                PremiumRatingBar(
                                    star  = star,
                                    count = count,
                                    total = uiState.reviews.size,
                                    gold  = gold,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
            }

            // "All Reviews" label
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(gold)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "All Reviews",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = if (isDark) DarkTextPrimary else PrimaryNavy
                    )
                }
            }

            uiState.errorMessage?.let { err ->
                item {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkError.copy(0.1f))
                            .border(1.dp, DarkError.copy(0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null,
                            tint = DarkError, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = DarkError, fontSize = 13.sp)
                    }
                }
            }

            // Empty state
            if (uiState.reviews.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(gold.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RateReview, null,
                                tint = gold, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            "No reviews yet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp,
                            color      = if (isDark) DarkTextPrimary else PrimaryNavy
                        )
                        Text(
                            "Be the first to share your experience",
                            fontSize = 13.sp,
                            color    = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                        )
                    }
                }
            }

            // Review cards
            items(uiState.reviews) { review ->
                PremiumReviewCard(
                    review       = review,
                    isLandlord   = isLandlord,
                    isDark       = isDark,
                    gold         = gold,
                    navy         = navy,
                    onNavy       = onNavy,
                    cardBg       = cardBg,
                    borderCol    = borderCol,
                    onReplyClick = {
                        replyTargetReview = review
                        replyText         = if (review.hasLandlordReply) review.landlordReply else ""
                        showReplyDialog   = true
                    },
                    onDeleteClick = {
                        deleteTargetReview = review
                        showDeleteDialog   = true
                    }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM RATING BAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PremiumRatingBar(star: Int, count: Int, total: Int, gold: Color, isDark: Boolean) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            "$star",
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (isDark) DarkTextSecondary else Color(0xFF8899AA),
            modifier   = Modifier.width(10.dp)
        )
        Icon(Icons.Default.Star, null,
            tint = gold, modifier = Modifier.size(11.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isDark) DarkBgElevated else Color(0xFFE8EAF0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(50))
                    .background(gold)
            )
        }
        Text(
            "$count",
            fontSize = 10.sp,
            color    = if (isDark) DarkTextMuted else Color(0xFFAAAAAA),
            modifier = Modifier.width(14.dp)
        )
    }
}

// Legacy alias — keeps any other call sites compiling
@Composable
fun RatingBar(star: Int, count: Int, total: Int) {
    PremiumRatingBar(
        star   = star,
        count  = count,
        total  = total,
        gold   = GoldAccent,
        isDark = false
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM REVIEW CARD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PremiumReviewCard(
    review      : Review,
    isLandlord  : Boolean = false,
    isDark      : Boolean = false,
    gold        : Color   = GoldAccent,
    navy        : Color   = PrimaryNavy,
    onNavy      : Color   = Color.White,
    cardBg      : Color   = SurfaceWhite,
    borderCol   : Color   = Color(0xFFE8EAF0),
    onReplyClick : () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .border(
                width = if (isDark) 1.dp else 0.5.dp,
                color = borderCol,
                shape = RoundedCornerShape(16.dp)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Reviewer header ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isDark) listOf(DarkBgElevated, DarkBgTertiary)
                                else        listOf(PrimaryNavy, PrimaryNavyLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = review.reviewerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color      = gold,
                        fontWeight = FontWeight.Black,
                        fontSize   = 18.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        review.reviewerName.ifEmpty { "Anonymous" },
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = if (isDark) DarkTextPrimary else PrimaryNavy
                    )
                    Text(
                        review.createdAt?.toDate()?.let {
                            "${it.date} ${arrayOf("Jan","Feb","Mar","Apr","May","Jun",
                                "Jul","Aug","Sep","Oct","Nov","Dec")[it.month]} ${it.year + 1900}"
                        } ?: "-",
                        fontSize = 11.sp,
                        color    = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                    )
                }
                // Star rating badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(gold.copy(0.12f))
                        .border(1.dp, gold.copy(0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Star, null,
                            tint = gold, modifier = Modifier.size(12.dp))
                        Text(
                            "${review.overallRating.toInt()}",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = gold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Star row ──────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (1..5).forEach { s ->
                    Icon(
                        imageVector = if (s <= review.overallRating.toInt())
                            Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint     = gold,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Comment ───────────────────────────────────────────────────────
            Text(
                review.comment,
                fontSize   = 13.sp,
                lineHeight = 21.sp,
                color      = if (isDark) DarkTextPrimary else Color(0xFF333355)
            )

            // ── Landlord Reply ────────────────────────────────────────────────
            if (review.hasLandlordReply) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    color     = if (isDark) DarkBorder else Color(0xFFEEEEEE),
                    thickness = 0.8.dp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDark) DarkBgSecondary
                            else        PrimaryNavy.copy(0.04f)
                        )
                        .border(
                            1.dp,
                            if (isDark) DarkBorder else PrimaryNavy.copy(0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    // Host badge
                    Box(
                        modifier         = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(gold.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Home, null,
                            tint = gold, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Host Reply",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 12.sp,
                                color      = gold
                            )
                            review.landlordRepliedAt?.let { ts ->
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "· ${ts.toDate().let {
                                        "${it.date} ${arrayOf("Jan","Feb","Mar","Apr","May","Jun",
                                            "Jul","Aug","Sep","Oct","Nov","Dec")[it.month]}"
                                    }}",
                                    fontSize = 10.sp,
                                    color    = if (isDark) DarkTextMuted else Color(0xFFAAAAAA)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            review.landlordReply,
                            fontSize   = 12.sp,
                            lineHeight = 18.sp,
                            color      = if (isDark) DarkTextSecondary else Color(0xFF555577),
                            fontStyle  = FontStyle.Italic
                        )
                    }
                }
            }

            // ── Landlord action buttons ───────────────────────────────────────
            if (isLandlord) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Reply / Edit button
                    OutlinedButton(
                        onClick  = onReplyClick,
                        shape    = RoundedCornerShape(10.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, navy),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = navy),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = if (review.hasLandlordReply) Icons.Default.Edit
                            else Icons.Default.Reply,
                            null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (review.hasLandlordReply) "Edit Reply" else "Reply",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Delete button
                    OutlinedButton(
                        onClick  = onDeleteClick,
                        shape    = RoundedCornerShape(10.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, DarkError.copy(0.6f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = DarkError),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Legacy alias — keeps existing call sites compiling
@Composable
fun ReviewCard(
    review       : Review,
    isLandlord   : Boolean  = false,
    onReplyClick : () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    PremiumReviewCard(
        review = review,
        isLandlord = isLandlord,
        onReplyClick = onReplyClick,
        onDeleteClick = onDeleteClick
    )
}