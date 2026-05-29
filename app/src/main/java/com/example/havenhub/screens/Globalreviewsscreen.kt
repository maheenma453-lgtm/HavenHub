package com.example.havenhub.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Review
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.ReviewViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// DATE FIX EXPLANATION
// ─────────────────────────────────────────────────────────────────────────────
// Root cause: Firestore uses FieldValue.serverTimestamp() on new reviews.
// When a doc is written locally, Firestore sets createdAt = null until the
// server round-trip confirms the timestamp. This caused dates to be blank on
// freshly posted reviews.
//
// Fix applied here:
//   1. safeToDate() — null-safe extension on Timestamp? with try/catch
//   2. formatDate() — accepts Date? and returns "" safely (no "—" clutter)
//   3. In the card we call review.createdAt.safeToDate() → passes Date? to
//      formatDate(). If null (server timestamp pending), date hides cleanly.
//   4. Sort comparator already used safeToDate() — left unchanged.
// ─────────────────────────────────────────────────────────────────────────────

// Safe null-handled Firestore Timestamp → Date converter
// Returns null if Timestamp is null or conversion throws (e.g. pending server ts)
private fun Timestamp?.safeToDate(): Date? = try { this?.toDate() } catch (e: Exception) { null }

// Formats a nullable Date to "d MMM yyyy" (e.g. "28 May 2025")
// Returns empty string for null so UI hides the field cleanly — no "—" shown
private fun formatDate(date: Date?): String {
    if (date == null) return ""
    return try {
        SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(date)
    } catch (e: Exception) { "" }
}

// ─────────────────────────────────────────────────────────────────────────────
// GlobalReviewsScreen
//
// Roles & permissions:
//   • Tenant  → read-only; can write reviews via FAB; cannot delete
//   • Landlord → sees only reviews on their properties; can long-press delete
//   • Admin   → sees ALL reviews; long-press opens premium red moderation UI
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalReviewsScreen(
    navController : NavController,
    isLandlord    : Boolean         = false,
    isAdmin       : Boolean         = false,
    currentUserId : String          = "",
    viewModel     : ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val error            = MaterialTheme.colorScheme.error

    // Load correct dataset based on role
    LaunchedEffect(Unit) {
        when {
            isLandlord -> viewModel.loadLandlordReviews()
            else       -> viewModel.loadAllReviews() // admin + tenant both load all
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on delete error
    LaunchedEffect(uiState.deleteReviewError) {
        uiState.deleteReviewError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteReviewState()
        }
    }
    // Show snackbar on delete success
    LaunchedEffect(uiState.deleteReviewSuccess) {
        if (uiState.deleteReviewSuccess) {
            snackbarHostState.showSnackbar("Review deleted successfully")
            viewModel.clearDeleteReviewState()
        }
    }

    // ── Filter logic ──────────────────────────────────────────────────────────
    val starFiltered = when (uiState.selectedFilter) {
        "5★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 5 }
        "4★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 4 }
        "3★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 3 }
        "2★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 2 }
        "1★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 1 }
        else  -> uiState.allReviews
    }
    // ── Sort logic ────────────────────────────────────────────────────────────
    val filteredReviews = when (uiState.selectedSort) {
        "Highest" -> starFiltered.sortedByDescending { it.overallRating }
        "Lowest"  -> starFiltered.sortedBy { it.overallRating }
        // DATE FIX: safeToDate() used here so null timestamps sort to bottom
        else      -> starFiltered.sortedByDescending { it.createdAt.safeToDate()?.time ?: 0L }
    }

    Scaffold(
        containerColor = background,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Admin monitors content — hide Write Review FAB for them
            if (!isAdmin) {
                ExtendedFloatingActionButton(
                    onClick        = { navController.navigate(Screen.AddReview.createRoute("")) },
                    containerColor = tertiary,
                    contentColor   = primary,
                    shape          = RoundedCornerShape(18.dp),
                    modifier       = Modifier.shadow(12.dp, RoundedCornerShape(18.dp))
                ) {
                    Icon(Icons.Default.RateReview, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Write Review",
                        fontWeight    = FontWeight.ExtraBold,
                        fontSize      = 13.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Premium gradient header ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        // Admin gets a red-tinted header so the moderation context is clear
                        if (isAdmin)
                            Brush.verticalGradient(
                                listOf(
                                    error.copy(alpha = 0.85f),
                                    primary.copy(alpha = 0.92f)
                                )
                            )
                        else
                            Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.88f)))
                    )
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.weight(1f)
                        ) {
                            // Back button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { navController.navigateUp() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint               = onPrimary,
                                    modifier           = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                // Admin gets a shield icon next to the title
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isAdmin) {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = null,
                                            tint     = onPrimary.copy(alpha = 0.90f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        if (isAdmin) "Review Moderation" else "Community Reviews",
                                        fontSize      = 21.sp,
                                        fontWeight    = FontWeight.Black,
                                        color         = onPrimary,
                                        letterSpacing = (-0.3).sp
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    when {
                                        isAdmin    -> "Long-press any review to moderate"
                                        isLandlord -> "Hold a review to manage content"
                                        else       -> "Ratings & feedback from our community"
                                    },
                                    fontSize = 11.sp,
                                    color    = onPrimary.copy(alpha = 0.60f)
                                )
                            }
                        }

                        // Average rating badge (right side of header)
                        if (uiState.allReviews.isNotEmpty()) {
                            val avg = uiState.allReviews.map { it.overallRating.toDouble() }.average()
                            Box(
                                modifier = Modifier
                                    .shadow(8.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.22f),
                                                Color.White.copy(alpha = 0.10f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Star,
                                            null,
                                            tint     = tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "%.1f".format(avg),
                                            fontSize   = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color      = tertiary
                                        )
                                    }
                                    Text(
                                        "${uiState.allReviews.size} reviews",
                                        fontSize = 10.sp,
                                        color    = onPrimary.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                    }

                    // ── Admin moderation banner below header ──────────────────
                    // Gives admin a persistent visual cue that they are in mod mode
                    if (isAdmin) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.13f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint     = onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Admin Mode — you can delete any review. Use responsibly.",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // ── Body ──────────────────────────────────────────────────────────
            when {
                uiState.isLoadingAll -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color       = primary,
                                strokeWidth = 3.dp,
                                modifier    = Modifier.size(46.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Loading reviews...",
                                fontSize = 13.sp,
                                color    = onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.allReviewsError != null -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😕", fontSize = 46.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                uiState.allReviewsError!!,
                                color    = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                if (isLandlord) viewModel.loadLandlordReviews()
                                else            viewModel.loadAllReviews()
                            }) { Text("Retry") }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {

                        // ── Filter chips row ──────────────────────────────────
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surface)
                                    .padding(vertical = 14.dp)
                            ) {
                                LazyRow(
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val filters = listOf("All", "5★", "4★", "3★", "2★", "1★")
                                    items(filters) { filter ->
                                        val sel = uiState.selectedFilter == filter
                                        Box(
                                            modifier = Modifier
                                                .shadow(if (sel) 4.dp else 0.dp, RoundedCornerShape(20.dp))
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (sel) primary else background)
                                                .clickable { viewModel.setFilter(filter) }
                                                .padding(horizontal = 16.dp, vertical = 9.dp)
                                        ) {
                                            Text(
                                                filter,
                                                fontSize   = 12.sp,
                                                fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal,
                                                color      = if (sel) onPrimary else onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                LazyRow(
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val sorts = listOf("Newest", "Highest", "Lowest")
                                    items(sorts) { sort ->
                                        val sel = uiState.selectedSort == sort
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    if (sel) tertiary.copy(alpha = 0.15f)
                                                    else Color.Transparent
                                                )
                                                .clickable { viewModel.setSort(sort) }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (sel) {
                                                Icon(
                                                    Icons.Default.Sort,
                                                    null,
                                                    tint     = tertiary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(
                                                sort,
                                                fontSize   = 12.sp,
                                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                                color      = if (sel) tertiary else onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = background, thickness = 6.dp)
                        }

                        // ── Results count row ─────────────────────────────────
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surface)
                                    .padding(horizontal = 20.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .width(3.dp)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            // Admin accent bar is red
                                            if (isAdmin) error else primary
                                        )
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (uiState.selectedFilter == "All")
                                        "All Reviews (${filteredReviews.size})"
                                    else
                                        "${uiState.selectedFilter} Reviews (${filteredReviews.size})",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = onSurface
                                )
                            }
                            HorizontalDivider(color = background, thickness = 2.dp)
                        }

                        // ── Empty state ───────────────────────────────────────
                        if (filteredReviews.isEmpty()) {
                            item {
                                Box(
                                    modifier         = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📝", fontSize = 46.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            if (isLandlord) "No reviews on your properties yet"
                                            else            "No reviews yet",
                                            fontSize   = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = onSurface
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Tap 'Write Review' to share your experience!",
                                            fontSize = 13.sp,
                                            color    = onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // ── Review cards with animated entrance ───────────────
                        itemsIndexed(filteredReviews, key = { _, r -> r.reviewId }) { index, review ->

                            // Tenant sees "You" badge on their own reviews
                            val isOwnReview = !isLandlord && !isAdmin && review.reviewerId == currentUserId

                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(review.reviewId) { visible = true }

                            AnimatedVisibility(
                                visible = visible,
                                enter   = fadeIn(tween(220, delayMillis = (index * 40).coerceAtMost(300))) +
                                        slideInVertically(
                                            tween(220, delayMillis = (index * 40).coerceAtMost(300))
                                        ) { it / 4 }
                            ) {
                                GlobalReviewCard(
                                    review          = review,
                                    isLandlord      = isLandlord,
                                    isAdmin         = isAdmin,
                                    isOwnReview     = isOwnReview,
                                    onPropertyClick = { propId ->
                                        if (propId.isNotEmpty())
                                            navController.navigate(Screen.PropertyDetail.createRoute(propId))
                                    },
                                    // Only landlord and admin get the delete callback
                                    onDeleteReview  = if (isLandlord || isAdmin) {
                                        { reviewId, propertyId ->
                                            viewModel.deleteReview(reviewId, propertyId)
                                        }
                                    } else null
                                )
                            }

                            HorizontalDivider(
                                color     = background,
                                thickness = 1.dp,
                                modifier  = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GlobalReviewCard
//
// DATE FIX:
//   • review.createdAt is Timestamp? from Firestore
//   • We call review.createdAt.safeToDate() to get Date?
//   • Then pass Date? into formatDate() which returns "" if null
//   • This means brand-new reviews (server timestamp still null) show no date
//     instead of crashing or showing "—"
//
// ADMIN DELETE UI:
//   • Admin gets a distinct deep-red bottom sheet with a shield icon header
//   • "Admin Mode" badge shown inside sheet so it's clear who is acting
//   • Confirm dialog text and button label differ for admin vs landlord
//   • Card border glows red subtly when isAdmin so admin can visually
//     distinguish moderation mode from normal browse
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GlobalReviewCard(
    review          : Review,
    isLandlord      : Boolean                     = false,
    isAdmin         : Boolean                     = false,
    isOwnReview     : Boolean                     = false,
    onPropertyClick : (String) -> Unit,
    onDeleteReview  : ((String, String) -> Unit)? = null
) {
    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val background       = MaterialTheme.colorScheme.background
    val error            = MaterialTheme.colorScheme.error

    // true when this user has moderation powers
    val canModerate = isLandlord || isAdmin

    var showBottomSheet   by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // ── DATE FIX: resolve date once here, reuse in UI ────────────────────────
    // safeToDate() returns null if Firestore server timestamp hasn't confirmed yet
    val reviewDate: String = formatDate(review.createdAt.safeToDate())

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN CONFIRM DIALOG — premium red moderation style
    // ─────────────────────────────────────────────────────────────────────────
    if (showConfirmDialog && canModerate) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            shape            = RoundedCornerShape(24.dp),
            containerColor   = surface,
            icon = {
                // Admin gets a double-layered warning icon with red glow ring
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(error.copy(alpha = 0.07f))
                    )
                    Box(
                        modifier         = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(error.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            // Admin sees shield+warning, landlord sees plain warning
                            if (isAdmin) Icons.Default.GppBad else Icons.Default.Warning,
                            contentDescription = null,
                            tint               = error,
                            modifier           = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Admin mode badge inside dialog title area
                    if (isAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(error.copy(alpha = 0.10f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint     = error,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "ADMIN ACTION",
                                    fontSize      = 10.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    color         = error,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        "Delete This Review?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp,
                        color      = onSurface
                    )
                }
            },
            text = {
                Column {
                    // Reviewer name preview chip inside dialog
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(background)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier         = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    review.reviewerName.firstOrNull { it.isLetter() }
                                        ?.uppercaseChar()?.toString() ?: "?",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = primary
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    review.reviewerName.ifEmpty { "Anonymous" },
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = onSurface
                                )
                                // DATE FIX: use the already-resolved reviewDate
                                if (reviewDate.isNotEmpty()) {
                                    Text(
                                        reviewDate,
                                        fontSize = 11.sp,
                                        color    = onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isAdmin)
                            "This review will be permanently removed. Admin deletions are irreversible — only remove content that violates community guidelines."
                        else
                            "This review by \"${review.reviewerName}\" will be permanently removed. Only delete vulgar or abusive content.",
                        fontSize   = 13.sp,
                        lineHeight = 21.sp,
                        color      = onSurface.copy(alpha = 0.72f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onDeleteReview?.invoke(review.reviewId, review.propertyId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = error),
                    shape  = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isAdmin) "Delete Review" else "Delete",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showConfirmDialog = false },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, onSurfaceVariant.copy(alpha = 0.25f))
                ) { Text("Cancel", color = onSurfaceVariant) }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOTTOM SHEET — admin gets red-accented moderation sheet
    //                landlord gets the standard sheet
    // ─────────────────────────────────────────────────────────────────────────
    if (showBottomSheet && canModerate) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor   = surface,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 44.dp)
            ) {

                // ── Sheet header ──────────────────────────────────────────────
                if (isAdmin) {
                    // Admin gets a full-width red header banner inside the sheet
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        error.copy(alpha = 0.18f),
                                        error.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier         = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint     = error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Admin Moderation",
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = error
                                )
                                Text(
                                    "Full platform access — act responsibly",
                                    fontSize = 11.sp,
                                    color    = error.copy(alpha = 0.60f)
                                )
                            }
                        }
                    }
                } else {
                    // Landlord gets a simple title row
                    Text(
                        "Review Options",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = onSurface
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Review preview chip ───────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(background)
                        .padding(14.dp)
                ) {
                    Box(
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            review.reviewerName.firstOrNull { it.isLetter() }
                                ?.uppercaseChar()?.toString() ?: "?",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            review.reviewerName.ifEmpty { "Anonymous" },
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = onSurface
                        )
                        // DATE FIX: show resolved date (empty string if pending)
                        if (reviewDate.isNotEmpty()) {
                            Text(
                                reviewDate,
                                fontSize = 11.sp,
                                color    = onSurfaceVariant
                            )
                        }
                        Text(
                            review.comment.take(60) + if (review.comment.length > 60) "…" else "",
                            fontSize = 11.sp,
                            color    = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Star badge in the preview row
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint     = tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "%.1f".format(review.overallRating),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = primary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Delete row ────────────────────────────────────────────────
                // Admin gets a slightly deeper red, landlord uses standard error color
                val deleteRowBg   = if (isAdmin) error.copy(alpha = 0.12f) else error.copy(alpha = 0.08f)
                val deleteIconBg  = if (isAdmin) error.copy(alpha = 0.20f) else error.copy(alpha = 0.13f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(deleteRowBg)
                        .clickable { showBottomSheet = false; showConfirmDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(deleteIconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            // Admin gets GppBad (shield crossed), landlord gets DeleteForever
                            if (isAdmin) Icons.Default.GppBad else Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint               = error,
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            if (isAdmin) "Remove Review" else "Delete This Review",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = error
                        )
                        Text(
                            if (isAdmin)
                                "Moderate content that violates guidelines"
                            else
                                "Remove vulgar or abusive content",
                            fontSize = 12.sp,
                            color    = error.copy(alpha = 0.55f)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint     = error.copy(alpha = 0.40f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ── Cancel row ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(background)
                        .clickable { showBottomSheet = false }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(onSurfaceVariant.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint     = onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Cancel",
                        fontSize = 14.sp,
                        color    = onSurfaceVariant
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CARD UI
    // Admin cards get a very subtle red-left-border treatment to signal
    // moderation mode — does not interfere with normal tenant/landlord views
    // ─────────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
    ) {
        // Subtle left red accent stripe for admin moderation mode
        if (isAdmin) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.0f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.0f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick     = { /* no-op: tap does nothing on the card itself */ },
                    onLongClick = {
                        // Long-press opens moderation sheet for landlord AND admin
                        if (canModerate && onDeleteReview != null) showBottomSheet = true
                    }
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {

            // ── Reviewer info row ─────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(primary, MaterialTheme.colorScheme.primaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        review.reviewerName.firstOrNull { it.isLetter() }
                            ?.uppercaseChar()?.toString() ?: "?",
                        color      = tertiary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(Modifier.width(13.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            review.reviewerName.ifEmpty { "Anonymous" },
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            color      = onSurface
                        )
                        // "You" badge — only visible to the tenant who wrote this review
                        if (isOwnReview) {
                            Spacer(Modifier.width(7.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "You",
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = primary
                                )
                            }
                        }
                    }

                    // DATE FIX: use pre-resolved reviewDate (empty = hide; no "—")
                    // New reviews with pending server timestamp show nothing here
                    // until Firestore confirms and listener refreshes the list
                    if (reviewDate.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            reviewDate,
                            fontSize = 11.sp,
                            color    = onSurfaceVariant
                        )
                    }
                }

                // Overall rating badge
                Row(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(11.dp))
                        .clip(RoundedCornerShape(11.dp))
                        .background(primary)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint     = tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "%.1f".format(review.overallRating),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = onPrimary
                    )
                }
            }

            Spacer(Modifier.height(13.dp))

            // ── Property tag ──────────────────────────────────────────────────
            if (review.propertyId.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(tertiary.copy(alpha = 0.11f))
                        .clickable { onPropertyClick(review.propertyId) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Home,
                        null,
                        tint     = tertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "View Property →",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = tertiary
                    )
                }
                Spacer(Modifier.height(11.dp))
            }

            // ── Review comment body ───────────────────────────────────────────
            Text(
                text       = review.comment,
                fontSize   = 13.sp,
                lineHeight = 21.sp,
                color      = onSurface.copy(alpha = 0.85f),
                maxLines   = 5,
                overflow   = TextOverflow.Ellipsis
            )

            // ── Sub-ratings row ───────────────────────────────────────────────
            val hasSubRatings = review.cleanlinessRating > 0f ||
                    review.locationRating > 0f ||
                    review.valueRating > 0f
            if (hasSubRatings) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (review.cleanlinessRating > 0f)
                        SubRatingChip("🧹 ${review.cleanlinessRating.toInt()}", onSurfaceVariant, background)
                    if (review.locationRating > 0f)
                        SubRatingChip("📍 ${review.locationRating.toInt()}", onSurfaceVariant, background)
                    if (review.valueRating > 0f)
                        SubRatingChip("💰 ${review.valueRating.toInt()}", onSurfaceVariant, background)
                }
            }

            // ── Landlord reply block ──────────────────────────────────────────
            if (review.hasLandlordReply) {
                Spacer(Modifier.height(13.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(primary.copy(alpha = 0.05f))
                        .padding(13.dp)
                ) {
                    Box(
                        modifier         = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tertiary.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Home,
                            null,
                            tint     = tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Landlord Reply",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 12.sp,
                            color      = primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            review.landlordReply,
                            fontSize   = 12.sp,
                            lineHeight = 19.sp,
                            color      = onSurface.copy(alpha = 0.75f),
                            fontStyle  = FontStyle.Italic
                        )
                    }
                }
            }

            // ── Bottom hint text for moderators ───────────────────────────────
            if (canModerate) {
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        // Admin gets a distinct shield icon in the hint
                        if (isAdmin) Icons.Default.Shield else Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (isAdmin)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                        else
                            onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isAdmin) "Hold to moderate" else "Hold to manage",
                        fontSize = 10.sp,
                        color = if (isAdmin)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.40f)
                        else
                            onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

// ── Sub-rating pill chip ──────────────────────────────────────────────────────
@Composable
private fun SubRatingChip(label: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}