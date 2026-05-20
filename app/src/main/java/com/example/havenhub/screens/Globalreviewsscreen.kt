package com.example.havenhub.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import java.text.SimpleDateFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// formatDate — Firestore Timestamp → "5 Jan 2025"
// ─────────────────────────────────────────────────────────────────────────────
private fun formatDate(date: java.util.Date?): String {
    if (date == null) return "—"
    return try {
        SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(date)
    } catch (e: Exception) { "—" }
}

// ─────────────────────────────────────────────────────────────────────────────
// GlobalReviewsScreen
//
// TENANT DELETE FLOW — HOW IT WORKS:
//   1. currentUserId is passed in from the NavGraph / parent composable.
//      It should be FirebaseAuth.getInstance().currentUser?.uid ?: ""
//
//   2. For every review card we check:
//        isOwnReview = !isLandlord && review.reviewerId == currentUserId
//      This is a pure UI guard — Firestore security rules are the real guard.
//
//   3. When isOwnReview == true:
//      • A red "Delete" OutlinedButton appears at the bottom of that card.
//      • Tapping it shows a confirmation AlertDialog.
//      • On confirm → viewModel.deleteOwnReview(reviewId) is called.
//      • ViewModel calls Firestore; on success snackbar shows "Review deleted".
//
//   4. Landlord flow is unchanged — long press → bottom sheet → confirm → delete.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalReviewsScreen(
    navController : NavController,
    isLandlord    : Boolean         = false,
    // ⬇️  Pass FirebaseAuth.getInstance().currentUser?.uid ?: "" from NavGraph
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

    // ── Load reviews on entry ─────────────────────────────────────
    LaunchedEffect(Unit) {
        if (isLandlord) viewModel.loadLandlordReviews()
        else            viewModel.loadAllReviews()
    }

    // ── Snackbar: delete success / error ──────────────────────────
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

    // ── Filter + Sort ─────────────────────────────────────────────
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
        else      -> starFiltered.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }

    Scaffold(
        containerColor = background,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = { navController.navigate(Screen.AddReview.createRoute("")) },
                containerColor = tertiary,
                contentColor   = primary,
                shape          = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.RateReview, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Write Review", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Header ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primary)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
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
                        IconButton(
                            onClick  = { navController.navigateUp() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint     = onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                "Community Reviews",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Black,
                                color      = onPrimary
                            )
                            Text(
                                when {
                                    isLandlord -> "Hold a review to delete vulgar content"
                                    else       -> "Ratings & feedback from our community"
                                },
                                fontSize = 12.sp,
                                color    = onPrimary.copy(0.65f)
                            )
                        }
                    }
                    // Average rating badge
                    if (uiState.allReviews.isNotEmpty()) {
                        val avg = uiState.allReviews.map { it.overallRating.toDouble() }.average()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(tertiary.copy(0.15f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = tertiary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("%.1f".format(avg), fontSize = 16.sp, fontWeight = FontWeight.Black, color = tertiary)
                                }
                                Text("${uiState.allReviews.size} reviews", fontSize = 10.sp, color = onPrimary.copy(0.55f))
                            }
                        }
                    }
                }
            }

            // ── Body ──────────────────────────────────────────────
            when {
                uiState.isLoadingAll -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = primary)
                    }
                }
                uiState.allReviewsError != null -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😕", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(uiState.allReviewsError!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
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
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {

                        // ── Filter chips ──────────────────────────
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surface)
                                    .padding(vertical = 12.dp)
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
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (sel) primary else background)
                                                .clickable { viewModel.setFilter(filter) }
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                filter,
                                                fontSize   = 12.sp,
                                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                color      = if (sel) onPrimary else onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
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
                                                .background(if (sel) tertiary.copy(0.15f) else Color.Transparent)
                                                .clickable { viewModel.setSort(sort) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (sel) {
                                                Icon(Icons.Default.Sort, null, tint = tertiary, modifier = Modifier.size(12.dp))
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

                        // ── Results count ─────────────────────────
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surface)
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    if (uiState.selectedFilter == "All") "All Reviews (${filteredReviews.size})"
                                    else "${uiState.selectedFilter} Reviews (${filteredReviews.size})",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = onSurface
                                )
                            }
                            HorizontalDivider(color = background, thickness = 2.dp)
                        }

                        // ── Empty state ───────────────────────────
                        if (filteredReviews.isEmpty()) {
                            item {
                                Box(
                                    modifier         = Modifier.fillMaxWidth().padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📝", fontSize = 40.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            if (isLandlord) "No reviews on your properties yet"
                                            else            "No reviews yet",
                                            fontSize   = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Tap 'Write Review' to share your experience!",
                                            fontSize = 13.sp,
                                            color    = onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // ── Review cards ──────────────────────────
                        items(filteredReviews, key = { it.reviewId }) { review ->

                            // ── CORE GUARD: kya yeh review is tenant ka apna hai? ──
                            // Sirf tab true hoga jab:
                            //   • logged-in user landlord NAHI hai
                            //   • review ka reviewerId == currentUserId (Firebase UID match)
                            // Agar dono match nahi, isOwnReview = false → delete button hidden
                            val isOwnReview = !isLandlord && review.reviewerId == currentUserId

                            GlobalReviewCard(
                                review          = review,
                                isLandlord      = isLandlord,
                                isOwnReview     = isOwnReview,
                                onPropertyClick = { propId ->
                                    if (propId.isNotEmpty())
                                        navController.navigate(Screen.PropertyDetail.createRoute(propId))
                                },
                                // Landlord: (reviewId, propertyId) dono chahiye
                                onDeleteReview = if (isLandlord) {
                                    { reviewId, propertyId -> viewModel.deleteReview(reviewId, propertyId) }
                                } else null,
                                // Tenant apna review: sirf reviewId
                                onDeleteOwnReview = if (isOwnReview) {
                                    { reviewId -> viewModel.deleteOwnReview(reviewId) }
                                } else null
                            )
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
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GlobalReviewCard(
    review            : Review,
    isLandlord        : Boolean                     = false,
    isOwnReview       : Boolean                     = false,
    onPropertyClick   : (String) -> Unit,
    onDeleteReview    : ((String, String) -> Unit)? = null,
    onDeleteOwnReview : ((String) -> Unit)?         = null
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
    var showTenantConfirm by remember { mutableStateOf(false) }

    // ── Landlord confirm dialog ───────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon  = { Icon(Icons.Default.Warning, null, tint = error) },
            title = { Text("Delete This Review?", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "\"${review.reviewerName}\" ka review permanently delete ho jaye ga. " +
                            "Sirf vulgar/abusive content hi delete karein.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onDeleteReview?.invoke(review.reviewId, review.propertyId)
                }) { Text("Delete", color = error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Tenant: apna review delete karne ka confirm dialog ────────
    // Yahan user se double-confirm liya ja raha hai — real apps mein
    // yeh important step hai taake accidental delete na ho
    if (showTenantConfirm) {
        AlertDialog(
            onDismissRequest = { showTenantConfirm = false },
            icon  = { Icon(Icons.Default.Delete, null, tint = error) },
            title = { Text("Apna Review Delete Karein?", fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text(
                        "Aapka review permanently remove ho jaye ga aur wapas nahi aa sake ga.",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    // Review preview — user ko confirm krwata hai k sahi review delete ho rha hai
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(error.copy(0.06f))
                            .padding(10.dp)
                    ) {
                        Text(
                            "\"${review.comment.take(80)}${if (review.comment.length > 80) "..." else ""}\"",
                            fontSize  = 12.sp,
                            color     = onSurface.copy(0.7f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTenantConfirm = false
                    // ViewModel ko reviewId bhejo — woh Firestore se delete karega
                    // Firestore security rule ensure karti hai k sirf owner delete kar sake
                    onDeleteOwnReview?.invoke(review.reviewId)
                }) { Text("Haan, Delete Karein", color = error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTenantConfirm = false }) {
                    Text("Nahi, Rakho", fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    // ── Landlord long-press bottom sheet ─────────────────────────
    if (showBottomSheet && isLandlord) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor   = surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Review Options", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface, modifier = Modifier.padding(bottom = 16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(background)
                        .padding(12.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(36.dp).clip(CircleShape).background(primary.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            review.reviewerName.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "?",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(review.reviewerName.ifEmpty { "Anonymous" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
                        Text(review.comment.take(60) + if (review.comment.length > 60) "..." else "", fontSize = 11.sp, color = onSurfaceVariant, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(error.copy(0.08f))
                        .clickable { showBottomSheet = false; showConfirmDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = error, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Delete This Review", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = error)
                        Text("Remove vulgar or abusive content", fontSize = 12.sp, color = error.copy(0.6f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(background)
                        .clickable { showBottomSheet = false }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Cancel", fontSize = 14.sp, color = onSurfaceVariant)
                }
            }
        }
    }

    // ── Card UI ───────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
            .combinedClickable(
                onClick     = { /* no action */ },
                onLongClick = {
                    if (isLandlord && onDeleteReview != null) showBottomSheet = true
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {

        // Reviewer info
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(primary, MaterialTheme.colorScheme.primaryContainer))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    review.reviewerName.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "?",
                    color = tertiary, fontSize = 18.sp, fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(review.reviewerName.ifEmpty { "Anonymous" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = onSurface)
                    // "You" badge — sirf apne review par dikhta hai
                    if (isOwnReview && !isLandlord) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(primary.copy(0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("You", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = primary)
                        }
                    }
                }
                Text(formatDate(review.createdAt?.toDate()), fontSize = 11.sp, color = onSurfaceVariant)
            }
            // Star rating badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(primary)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, null, tint = tertiary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text("%.1f".format(review.overallRating), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onPrimary)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Property tag
        if (review.propertyId.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tertiary.copy(0.10f))
                    .clickable { onPropertyClick(review.propertyId) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Home, null, tint = tertiary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
                Text("View Property →", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tertiary)
            }
            Spacer(Modifier.height(8.dp))
        }

        // Review comment
        Text(text = review.comment, fontSize = 13.sp, lineHeight = 20.sp, color = onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis)

        // Sub-ratings
        val hasSubRatings = review.cleanlinessRating > 0f || review.locationRating > 0f || review.valueRating > 0f
        if (hasSubRatings) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (review.cleanlinessRating > 0f) SubRatingChip("🧹 ${review.cleanlinessRating.toInt()}", onSurfaceVariant, background)
                if (review.locationRating    > 0f) SubRatingChip("📍 ${review.locationRating.toInt()}",    onSurfaceVariant, background)
                if (review.valueRating       > 0f) SubRatingChip("💰 ${review.valueRating.toInt()}",       onSurfaceVariant, background)
            }
        }

        // Landlord reply
        if (review.hasLandlordReply) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(primary.copy(0.05f))
                    .padding(10.dp)
            ) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(tertiary.copy(0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Home, null, tint = tertiary, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Landlord Reply", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = primary)
                    Spacer(Modifier.height(3.dp))
                    Text(review.landlordReply, fontSize = 12.sp, lineHeight = 18.sp, color = onSurface, fontStyle = FontStyle.Italic)
                }
            }
        }

        // ─────────────────────────────────────────────────────────
        // TENANT DELETE BUTTON
        // Conditions (sab zaroori hain):
        //   ✅ isLandlord == false      → landlord ko button nahi dikhega
        //   ✅ isOwnReview == true      → sirf apne review par dikhega
        //   ✅ onDeleteOwnReview != null → callback available hai
        // ─────────────────────────────────────────────────────────
        if (!isLandlord && isOwnReview && onDeleteOwnReview != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // "Your review" label — remind karta hai k yeh apna hai
                Text(
                    "Aapka review",
                    fontSize = 10.sp,
                    color    = onSurfaceVariant.copy(0.6f),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick        = { showTenantConfirm = true },  // dialog open karo
                    shape          = RoundedCornerShape(8.dp),
                    border         = BorderStroke(1.dp, error.copy(0.5f)),
                    colors         = ButtonDefaults.outlinedButtonColors(contentColor = error),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete my review", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Landlord hint
        if (isLandlord) {
            Spacer(Modifier.height(6.dp))
            Text("Hold to manage", fontSize = 10.sp, color = onSurfaceVariant.copy(0.5f), modifier = Modifier.align(Alignment.End))
        }
    }
}

// ── Sub-rating chip ───────────────────────────────────────────────────────────
@Composable
private fun SubRatingChip(label: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { Text(label, fontSize = 11.sp, color = textColor) }
}
