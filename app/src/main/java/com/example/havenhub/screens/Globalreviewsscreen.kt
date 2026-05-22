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
// formatDate — safe null-handled date formatter
// Handles: Firestore Timestamp → Date, plain Date, null (shows "—")
// ─────────────────────────────────────────────────────────────────────────────
private fun formatDate(date: Date?): String {
    if (date == null) return ""          // return empty so we don't show "—" clutter
    return try {
        SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(date)
    } catch (e: Exception) { "" }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper to safely resolve Date from any Timestamp-like object
// ─────────────────────────────────────────────────────────────────────────────
private fun Timestamp?.safeToDate(): Date? = try { this?.toDate() } catch (e: Exception) { null }

// ─────────────────────────────────────────────────────────────────────────────
// GlobalReviewsScreen — Premium UI
//
// CHANGES vs previous version:
//   • Tenant delete button REMOVED — tenants can no longer delete their reviews
//     from the global list. Only landlords retain the long-press delete flow.
//   • Date now shows even for manually added reviews:
//       - If createdAt is non-null → format normally
//       - If createdAt is null    → show nothing (no ugly "—")
//   • Cleaner card layout, tighter spacing, animated list items
//   • Average rating pill uses a sharper gradient
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalReviewsScreen(
    navController : NavController,
    isLandlord    : Boolean         = false,
    currentUserId : String          = "",          // kept for future use / "You" badge
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

    LaunchedEffect(Unit) {
        if (isLandlord) viewModel.loadLandlordReviews()
        else            viewModel.loadAllReviews()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.deleteReviewError) {
        uiState.deleteReviewError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteReviewState()
        }
    }
    LaunchedEffect(uiState.deleteReviewSuccess) {
        if (uiState.deleteReviewSuccess) {
            snackbarHostState.showSnackbar("Review deleted successfully")
            viewModel.clearDeleteReviewState()
        }
    }

    // ── Filter + Sort ─────────────────────────────────────────────────────────
    val starFiltered = when (uiState.selectedFilter) {
        "5★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 5 }
        "4★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 4 }
        "3★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 3 }
        "2★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 2 }
        "1★" -> uiState.allReviews.filter { Math.round(it.overallRating).toInt() == 1 }
        else  -> uiState.allReviews
    }
    val filteredReviews = when (uiState.selectedSort) {
        "Highest" -> starFiltered.sortedByDescending { it.overallRating }
        "Lowest"  -> starFiltered.sortedBy { it.overallRating }
        else      -> starFiltered.sortedByDescending { it.createdAt.safeToDate()?.time ?: 0L }
    }

    Scaffold(
        containerColor = background,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
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
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 13.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Premium Header ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(primary, primary.copy(alpha = 0.88f))
                        )
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
                                Text(
                                    "Community Reviews",
                                    fontSize      = 21.sp,
                                    fontWeight    = FontWeight.Black,
                                    color         = onPrimary,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (isLandlord) "Hold a review to manage content"
                                    else            "Ratings & feedback from our community",
                                    fontSize = 11.sp,
                                    color    = onPrimary.copy(alpha = 0.60f)
                                )
                            }
                        }

                        // Average rating badge
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

                        // ── Filter chips ──────────────────────────────────────
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surface)
                                    .padding(vertical = 14.dp)
                            ) {
                                // Star filters
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

                                // Sort chips
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

                        // ── Results count ─────────────────────────────────────
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
                                        .background(primary)
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

                        // ── Review cards — animated entrance ─────────────────
                        itemsIndexed(filteredReviews, key = { _, r -> r.reviewId }) { index, review ->

                            // "You" badge logic — only for display, no delete button
                            val isOwnReview = !isLandlord && review.reviewerId == currentUserId

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
                                    isOwnReview     = isOwnReview,
                                    onPropertyClick = { propId ->
                                        if (propId.isNotEmpty())
                                            navController.navigate(Screen.PropertyDetail.createRoute(propId))
                                    },
                                    onDeleteReview  = if (isLandlord) {
                                        { reviewId, propertyId -> viewModel.deleteReview(reviewId, propertyId) }
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
// GlobalReviewCard — Premium redesign
//
// KEY CHANGES:
//   • onDeleteOwnReview parameter REMOVED — tenants cannot delete from here
//   • Date shows when available, hides gracefully when null (no "—")
//   • Cleaner avatar gradient, tighter typography
//   • "You" badge still shows so the user knows which review is theirs
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GlobalReviewCard(
    review          : Review,
    isLandlord      : Boolean                     = false,
    isOwnReview     : Boolean                     = false,
    onPropertyClick : (String) -> Unit,
    onDeleteReview  : ((String, String) -> Unit)? = null   // landlord only
) {
    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val background       = MaterialTheme.colorScheme.background
    val error            = MaterialTheme.colorScheme.error

    var showBottomSheet   by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // ── Landlord confirm dialog ───────────────────────────────────────────────
    if (showConfirmDialog && isLandlord) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            shape            = RoundedCornerShape(22.dp),
            icon             = {
                Box(
                    modifier         = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(error.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint     = error,
                        modifier = Modifier.size(27.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete This Review?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 18.sp
                )
            },
            text  = {
                Text(
                    "This review by \"${review.reviewerName}\" will be permanently removed. Only delete vulgar or abusive content.",
                    fontSize   = 14.sp,
                    lineHeight = 22.sp,
                    color      = onSurface.copy(alpha = 0.75f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onDeleteReview?.invoke(review.reviewId, review.propertyId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = error),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }
            }
        )
    }

    // ── Landlord long-press bottom sheet ─────────────────────────────────────
    if (showBottomSheet && isLandlord) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor   = surface,
            shape            = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    "Review Options",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = onSurface,
                    modifier   = Modifier.padding(bottom = 18.dp)
                )

                // Review preview
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
                    Column {
                        Text(
                            review.reviewerName.ifEmpty { "Anonymous" },
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = onSurface
                        )
                        Text(
                            review.comment.take(55) + if (review.comment.length > 55) "…" else "",
                            fontSize = 11.sp,
                            color    = onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Delete row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(error.copy(alpha = 0.08f))
                        .clickable { showBottomSheet = false; showConfirmDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(error.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint               = error,
                            modifier           = Modifier.size(21.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Delete This Review",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = error
                        )
                        Text(
                            "Remove vulgar or abusive content",
                            fontSize = 12.sp,
                            color    = error.copy(alpha = 0.55f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Cancel row
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(onSurfaceVariant.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint               = onSurfaceVariant,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Cancel", fontSize = 14.sp, color = onSurfaceVariant)
                }
            }
        }
    }

    // ── Card UI ───────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
            .combinedClickable(
                onClick     = { /* no-op */ },
                onLongClick = {
                    if (isLandlord && onDeleteReview != null) showBottomSheet = true
                }
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {

        // ── Reviewer info row ─────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

            // Avatar
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
                    // "You" badge — only for the tenant's own review (no delete functionality)
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
                // Date — only shown when non-null and non-empty
                val dateText = formatDate(review.createdAt.safeToDate())
                if (dateText.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dateText,
                        fontSize = 11.sp,
                        color    = onSurfaceVariant
                    )
                }
            }

            // Star rating badge
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

        // ── Property tag ──────────────────────────────────────────────────────
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

        // ── Comment ───────────────────────────────────────────────────────────
        Text(
            text       = review.comment,
            fontSize   = 13.sp,
            lineHeight = 21.sp,
            color      = onSurface.copy(alpha = 0.85f),
            maxLines   = 5,
            overflow   = TextOverflow.Ellipsis
        )

        // ── Sub-ratings ───────────────────────────────────────────────────────
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

        // ── Landlord reply ────────────────────────────────────────────────────
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

        // ── Landlord hint ─────────────────────────────────────────────────────
        if (isLandlord) {
            Spacer(Modifier.height(7.dp))
            Text(
                "Hold to manage",
                fontSize = 10.sp,
                color    = onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.End)
            )
        }

        // NOTE: Tenant delete button is intentionally REMOVED.
        // Tenants see the "You" badge to identify their review but cannot delete it
        // from the global screen. Deletion can be offered only from the user's
        // personal profile / "My Reviews" screen if needed in the future.
    }
}

// ── Sub-rating chip ───────────────────────────────────────────────────────────
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