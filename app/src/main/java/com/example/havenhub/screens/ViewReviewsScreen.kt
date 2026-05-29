package com.example.havenhub.screens

import android.util.Log
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
import com.example.havenhub.viewmodel.ReviewViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// DATE FORMATTING HELPERS
//
// ROOT CAUSE OF MISSING DATES (fully fixed):
//
//   Problem 1 — serverTimestamp pending:
//     When a review is written, Firestore sets createdAt via FieldValue.serverTimestamp().
//     The local write returns null until the server confirms. So createdAt was null
//     for brand-new reviews.
//
//   Problem 2 — both createdAt AND updatedAt null:
//     Old code fell back to updatedAt, but for brand-new reviews BOTH fields are
//     FieldValue.serverTimestamp() — both pending simultaneously, so both null.
//
//   Fix in parseReview() (FirebaseDataManager.kt):
//     Falls back to Timestamp.now() as last resort so createdAt is NEVER null
//     in the Review object. The date shown will be "today" for new reviews
//     (correct!) and the real timestamp replaces it on the next Firestore read.
//
//   Fix in UI (here):
//     safeToDate() and formatReviewDate() are kept as null-safe guards,
//     but they should now always receive a non-null Timestamp.
// =============================================================================

/** Safely converts a Firestore Timestamp to Date. Returns null only on exception. */
private fun Timestamp?.safeToDate(): Date? =
    try { this?.toDate() } catch (e: Exception) { null }

/**
 * Formats a nullable Date to "d MMM yyyy" (e.g. "29 May 2026").
 * Returns empty string when date is null — UI simply hides the field.
 */
private fun formatReviewDate(date: Date?): String {
    if (date == null) return ""
    return try {
        SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(date)
    } catch (e: Exception) { "" }
}

// =============================================================================
// ViewReviewsScreen
//
// Used by:
//   • Landlord (propertyId = "") — shows all reviews on their properties
//   • Tenant / visitor (propertyId != "") — shows reviews for one property
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReviewsScreen(
    navController: NavController,
    propertyId: String,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    // -------------------------------------------------------------------------
    // Role detection — read directly from Firestore
    //
    // Why not use AuthViewModel here?
    // hiltViewModel() in a child screen can return a different instance whose
    // userRole has not been populated yet (still ""), so isLandlord was always
    // false and the Delete button never appeared. We fetch the role ourselves.
    // -------------------------------------------------------------------------
    var currentUserRole by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .get()
                    .await()
                currentUserRole = doc.getString("role")?.trim()?.lowercase() ?: ""
                Log.d("REVIEW_ROLE", "Role fetched: '$currentUserRole'")
            } catch (e: Exception) {
                Log.e("REVIEW_ROLE", "Failed to fetch role: ${e.localizedMessage}")
                currentUserRole = ""
            }
        }
    }

    val isLandlord = currentUserRole.equals("landlord", ignoreCase = true)
    Log.d("REVIEW_ROLE", "isLandlord=$isLandlord role='$currentUserRole'")

    val isLandlordMode = propertyId.isBlank() && isLandlord

    val reviews      = if (isLandlordMode) uiState.allReviews else uiState.reviews
    val isLoading    = if (isLandlordMode) uiState.isLoadingAll else uiState.isLoading
    val errorMessage = if (isLandlordMode) uiState.allReviewsError else uiState.errorMessage
    val avgRating    = if (reviews.isNotEmpty())
        reviews.sumOf { it.overallRating.toDouble() } / reviews.size
    else 0.0

    // Theme tokens
    val screenBg  = if (isDark) DarkBg else BackgroundLight
    val gold      = if (isDark) DarkGoldPrimary else GoldAccent
    val navy      = if (isDark) DarkBgSecondary else PrimaryNavy
    val onNavy    = if (isDark) DarkTextPrimary else Color.White
    val cardBg    = if (isDark) DarkSurface else SurfaceWhite
    val borderCol = if (isDark) DarkBorder else Color(0xFFE8EAF0)

    // Load data
    LaunchedEffect(propertyId, isLandlordMode) {
        when {
            isLandlordMode          -> viewModel.loadLandlordReviews()
            propertyId.isNotBlank() -> viewModel.loadPropertyReviews(propertyId)
        }
    }

    // Dialog state
    var showReplyDialog    by remember { mutableStateOf(false) }
    var replyTargetReview  by remember { mutableStateOf<Review?>(null) }
    var replyText          by remember { mutableStateOf("") }
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var deleteTargetReview by remember { mutableStateOf<Review?>(null) }

    // Handle reply success
    LaunchedEffect(uiState.replySuccess) {
        if (uiState.replySuccess) {
            showReplyDialog = false
            replyText = ""
            replyTargetReview = null
            viewModel.clearReplySuccess()
        }
    }

    // Handle delete success
    LaunchedEffect(uiState.deleteReviewSuccess) {
        if (uiState.deleteReviewSuccess) {
            Log.d("REVIEW_DELETE", "Delete SUCCESS — review removed from UI")
            viewModel.clearDeleteReviewState()
        }
    }

    // Handle delete error
    LaunchedEffect(uiState.deleteReviewError) {
        uiState.deleteReviewError?.let { error ->
            Log.e("REVIEW_DELETE", "Delete FAILED: $error")
            viewModel.clearDeleteReviewState()
        }
    }

    // -------------------------------------------------------------------------
    // DELETE CONFIRMATION DIALOG
    // -------------------------------------------------------------------------
    if (showDeleteDialog && deleteTargetReview != null) {
        Dialog(onDismissRequest = {
            showDeleteDialog = false
            deleteTargetReview = null
        }) {
            Card(
                shape    = RoundedCornerShape(24.dp),
                colors   = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.border(1.dp, borderCol, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier         = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(DarkError.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteForever, null, tint = DarkError, modifier = Modifier.size(28.dp))
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
                            Text("Cancel", color = if (isDark) DarkTextSecondary else PrimaryNavy)
                        }
                        Button(
                            onClick = {
                                Log.d("REVIEW_DELETE", "Confirm delete: reviewId=${deleteTargetReview?.reviewId}")
                                deleteTargetReview?.let {
                                    viewModel.deleteReview(reviewId = it.reviewId, propertyId = it.propertyId)
                                }
                                showDeleteDialog = false
                                deleteTargetReview = null
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = DarkError)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // REPLY DIALOG
    // -------------------------------------------------------------------------
    if (showReplyDialog && replyTargetReview != null) {
        Dialog(onDismissRequest = {
            showReplyDialog = false
            replyText = ""
            replyTargetReview = null
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier         = Modifier.size(40.dp).clip(CircleShape).background(gold.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Reply, null, tint = gold, modifier = Modifier.size(20.dp))
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

                    // Original comment preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) DarkBgSecondary else Color(0xFFF5F6FA))
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            replyTargetReview!!.comment,
                            fontSize   = 13.sp,
                            lineHeight = 19.sp,
                            maxLines   = 3,
                            color      = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                        )
                    }

                    OutlinedTextField(
                        value         = replyText,
                        onValueChange = { if (it.length <= 300) replyText = it },
                        modifier      = Modifier.fillMaxWidth().height(110.dp),
                        placeholder   = {
                            Text("Write your response...", color = if (isDark) DarkTextMuted else Color(0xFFAAAAAA))
                        },
                        shape    = RoundedCornerShape(14.dp),
                        maxLines = 5,
                        colors   = OutlinedTextFieldDefaults.colors(
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

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showReplyDialog = false; replyText = ""; replyTargetReview = null },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, gold),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = gold)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                replyTargetReview?.let { review ->
                                    viewModel.replyToReview(
                                        reviewId     = review.reviewId,
                                        propertyId   = review.propertyId,
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
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = gold)
                            } else {
                                Text("Submit", fontWeight = FontWeight.Bold, color = gold)
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // MAIN SCAFFOLD
    // -------------------------------------------------------------------------
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
                            Text("Reviews", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = onNavy)
                            if (reviews.isNotEmpty()) {
                                Text(
                                    "${reviews.size} guest review${if (reviews.size != 1) "s" else ""}",
                                    fontSize = 11.sp, color = gold
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

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = gold, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding).background(screenBg),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Rating summary card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isDark) 1.dp else 0.5.dp,
                            color = borderCol,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.weight(1f)
                        ) {
                            Text(
                                String.format("%.1f", avgRating),
                                fontSize   = 52.sp,
                                fontWeight = FontWeight.Black,
                                color      = gold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                (1..5).forEach { s ->
                                    Icon(
                                        imageVector        = if (s <= avgRating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint               = gold,
                                        modifier           = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${reviews.size} review${if (reviews.size != 1) "s" else ""}",
                                fontSize = 12.sp,
                                color    = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                            )
                        }

                        Box(Modifier.width(1.dp).height(80.dp).background(borderCol))
                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            (5 downTo 1).forEach { star ->
                                val count = reviews.count { it.overallRating.toInt() == star }
                                PremiumRatingBar(star = star, count = count, total = reviews.size, gold = gold, isDark = isDark)
                            }
                        }
                    }
                }
            }

            // Section label
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(modifier = Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(gold))
                    Spacer(Modifier.width(8.dp))
                    Text("All Reviews", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDark) DarkTextPrimary else PrimaryNavy)
                }
            }

            // Error banner
            errorMessage?.let { err ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkError.copy(0.1f))
                            .border(1.dp, DarkError.copy(0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = DarkError, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = DarkError, fontSize = 13.sp)
                    }
                }
            }

            // Empty state
            if (reviews.isEmpty() && !isLoading) {
                item {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier         = Modifier.size(72.dp).clip(CircleShape).background(gold.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RateReview, null, tint = gold, modifier = Modifier.size(32.dp))
                        }
                        Text("No reviews yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = if (isDark) DarkTextPrimary else PrimaryNavy)
                        Text("Be the first to share your experience", fontSize = 13.sp, color = if (isDark) DarkTextSecondary else Color(0xFF8899AA))
                    }
                }
            }

            // Review cards
            items(reviews) { review ->
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
                        replyText = if (review.hasLandlordReply) review.landlordReply else ""
                        showReplyDialog = true
                    },
                    onDeleteClick = {
                        Log.d("REVIEW_DELETE", "Delete tapped: reviewId=${review.reviewId}")
                        deleteTargetReview = review
                        showDeleteDialog = true
                    }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// =============================================================================
// RATING BAR
// =============================================================================
@Composable
fun PremiumRatingBar(star: Int, count: Int, total: Int, gold: Color, isDark: Boolean) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("$star", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) DarkTextSecondary else Color(0xFF8899AA), modifier = Modifier.width(10.dp))
        Icon(Icons.Default.Star, null, tint = gold, modifier = Modifier.size(11.dp))
        Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)).background(if (isDark) DarkBgElevated else Color(0xFFE8EAF0))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(50)).background(gold))
        }
        Text("$count", fontSize = 10.sp, color = if (isDark) DarkTextMuted else Color(0xFFAAAAAA), modifier = Modifier.width(14.dp))
    }
}

/** Legacy alias — keeps existing call sites compiling */
@Composable
fun RatingBar(star: Int, count: Int, total: Int) {
    PremiumRatingBar(star = star, count = count, total = total, gold = GoldAccent, isDark = false)
}

// =============================================================================
// REVIEW CARD
//
// DATE FIX:
//   parseReview() in FirebaseDataManager now guarantees createdAt is NEVER null
//   (falls back to Timestamp.now() for brand-new reviews).
//   safeToDate() + formatReviewDate() here are kept as safety nets.
//   Result: date ALWAYS shows — "today" for new reviews, real date otherwise.
// =============================================================================
@Composable
fun PremiumReviewCard(
    review       : Review,
    isLandlord   : Boolean   = false,
    isDark       : Boolean   = false,
    gold         : Color     = GoldAccent,
    navy         : Color     = PrimaryNavy,
    onNavy       : Color     = Color.White,
    cardBg       : Color     = SurfaceWhite,
    borderCol    : Color     = Color(0xFFE8EAF0),
    onReplyClick : () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    // Resolve dates once — reused in the card body
    val reviewDateStr = formatReviewDate(review.createdAt.safeToDate())
    val replyDateStr  = formatReviewDate(review.landlordRepliedAt.safeToDate())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (isDark) 1.dp else 0.5.dp, color = borderCol, shape = RoundedCornerShape(16.dp)),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Reviewer header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isDark) listOf(DarkBgElevated, DarkBgTertiary)
                                else listOf(PrimaryNavy, PrimaryNavyLight)
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
                    // DATE: always shows because parseReview guarantees non-null createdAt
                    if (reviewDateStr.isNotEmpty()) {
                        Text(
                            reviewDateStr,
                            fontSize = 11.sp,
                            color    = if (isDark) DarkTextSecondary else Color(0xFF8899AA)
                        )
                    }
                }
                // Rating badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(gold.copy(0.12f))
                        .border(1.dp, gold.copy(0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Star, null, tint = gold, modifier = Modifier.size(12.dp))
                        Text("${review.overallRating.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = gold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Star row
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (1..5).forEach { s ->
                    Icon(
                        imageVector        = if (s <= review.overallRating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint               = gold,
                        modifier           = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Review comment
            Text(review.comment, fontSize = 13.sp, lineHeight = 21.sp, color = if (isDark) DarkTextPrimary else Color(0xFF333355))

            // Landlord reply block
            if (review.hasLandlordReply) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFEEEEEE), thickness = 0.8.dp)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) DarkBgSecondary else PrimaryNavy.copy(0.04f))
                        .border(1.dp, if (isDark) DarkBorder else PrimaryNavy.copy(0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(32.dp).clip(CircleShape).background(gold.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Home, null, tint = gold, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Host Reply", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = gold)
                            // Reply date — shows when landlord replied
                            if (replyDateStr.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "· $replyDateStr",
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

            // Landlord action buttons
            if (isLandlord) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick        = onReplyClick,
                        shape          = RoundedCornerShape(10.dp),
                        border         = androidx.compose.foundation.BorderStroke(1.dp, navy),
                        colors         = ButtonDefaults.outlinedButtonColors(contentColor = navy),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier       = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector        = if (review.hasLandlordReply) Icons.Default.Edit else Icons.Default.Reply,
                            contentDescription = null,
                            modifier           = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (review.hasLandlordReply) "Edit Reply" else "Reply", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick        = onDeleteClick,
                        shape          = RoundedCornerShape(10.dp),
                        border         = androidx.compose.foundation.BorderStroke(1.dp, DarkError.copy(0.6f)),
                        colors         = ButtonDefaults.outlinedButtonColors(contentColor = DarkError),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier       = Modifier.height(36.dp)
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

/** Legacy alias — keeps existing call sites compiling */
@Composable
fun ReviewCard(
    review       : Review,
    isLandlord   : Boolean   = false,
    onReplyClick : () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    PremiumReviewCard(review = review, isLandlord = isLandlord, onReplyClick = onReplyClick, onDeleteClick = onDeleteClick)
}
