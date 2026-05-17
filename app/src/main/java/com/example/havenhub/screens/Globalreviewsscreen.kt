package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

// ── Global Reviews Screen ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalReviewsScreen(
    navController: NavController,
    viewModel    : ReviewViewModel = hiltViewModel()
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
        viewModel.loadAllReviews()
    }

    val filteredReviews = remember(
        uiState.allReviews,
        uiState.selectedFilter,
        uiState.selectedSort
    ) {
        viewModel.getFilteredReviews(uiState)
    }

    Scaffold(
        containerColor = background,
        // ── Write Review FAB ──────────────────────────────────────
        // Tap karo → AddReviewScreen khulega, wahan se user property
        // search karke select karega aur review submit karega
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick           = {
                    navController.navigate(
                        Screen.AddReview.createRoute("") // blank propertyId → search mode
                    )
                },
                containerColor    = tertiary,
                contentColor      = primary,
                shape             = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.RateReview, null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Write Review",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Top Header ────────────────────────────────────────
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
                    Column {
                        Text(
                            "Community Reviews",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Black,
                            color      = onPrimary
                        )
                        Text(
                            "What tenants are saying",
                            fontSize = 12.sp,
                            color    = onPrimary.copy(0.65f)
                        )
                    }
                    // Overall stats badge
                    if (uiState.allReviews.isNotEmpty()) {
                        val avg = uiState.allReviews
                            .map { it.overallRating.toDouble() }.average()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(tertiary.copy(0.15f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star, null,
                                        tint     = tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "%.1f".format(avg),
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color      = tertiary
                                    )
                                }
                                Text(
                                    "${uiState.allReviews.size} reviews",
                                    fontSize = 10.sp,
                                    color    = onPrimary.copy(0.55f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Body ──────────────────────────────────────────────
            when {
                uiState.isLoadingAll -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primary)
                    }
                }

                uiState.allReviewsError != null -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😕", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                uiState.allReviewsError!!,
                                color    = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadAllReviews() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(bottom = 88.dp), // FAB ke upar space
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {

                        // ── Filter + Sort Row ─────────────────────
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surface)
                                    .padding(vertical = 12.dp)
                            ) {
                                // Star filter chips
                                LazyRow(
                                    modifier              = Modifier.fillMaxWidth(),
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val filters = listOf("All", "5★", "4★", "3★", "2★", "1★")
                                    items(filters) { filter ->
                                        val isSelected = uiState.selectedFilter == filter
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    if (isSelected) primary else background
                                                )
                                                .clickable { viewModel.setFilter(filter) }
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                filter,
                                                fontSize   = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color      = if (isSelected) onPrimary else onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Sort chips
                                LazyRow(
                                    modifier              = Modifier.fillMaxWidth(),
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val sorts = listOf("Newest", "Highest", "Lowest")
                                    items(sorts) { sort ->
                                        val isSelected = uiState.selectedSort == sort
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    if (isSelected) tertiary.copy(0.15f)
                                                    else Color.Transparent
                                                )
                                                .clickable { viewModel.setSort(sort) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Sort, null,
                                                    tint     = tertiary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(
                                                sort,
                                                fontSize   = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color      = if (isSelected) tertiary else onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = background, thickness = 6.dp)
                        }

                        // ── Count Row ─────────────────────────────
                        item {
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .background(surface)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                        // ── Empty State ───────────────────────────
                        if (filteredReviews.isEmpty()) {
                            item {
                                Box(
                                    modifier         = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📝", fontSize = 40.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "No reviews yet",
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

                        // ── Review Cards ──────────────────────────
                        items(filteredReviews, key = { it.reviewId }) { review ->
                            GlobalReviewCard(
                                review          = review,
                                onPropertyClick = { propId ->
                                    if (propId.isNotEmpty()) {
                                        navController.navigate(
                                            Screen.PropertyDetail.createRoute(propId)
                                        )
                                    }
                                }
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

// ── Global Review Card ────────────────────────────────────────────────────────
@Composable
fun GlobalReviewCard(
    review         : Review,
    onPropertyClick: (String) -> Unit
) {
    val primary          = MaterialTheme.colorScheme.primary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val background       = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // ── Reviewer Info Row ─────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(primary, MaterialTheme.colorScheme.primaryContainer)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = review.reviewerName.firstOrNull { it.isLetter() }
                        ?.uppercaseChar()?.toString() ?: "?",
                    color      = tertiary,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = review.reviewerName.ifEmpty { "Anonymous" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = onSurface
                )
                Text(
                    text = review.createdAt?.toDate()?.let { d ->
                        "${d.date} ${
                            arrayOf("Jan","Feb","Mar","Apr","May","Jun",
                                "Jul","Aug","Sep","Oct","Nov","Dec")[d.month]
                        } ${d.year + 1900}"
                    } ?: "—",
                    fontSize = 11.sp,
                    color    = onSurfaceVariant
                )
            }

            // Star badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(primary)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star, null,
                    tint     = tertiary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "%.1f".format(review.overallRating),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = onPrimary
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Property Tag — Clickable ──────────────────────────────
        if (review.propertyId.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tertiary.copy(0.10f))
                    .clickable { onPropertyClick(review.propertyId) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Home, null,
                    tint     = tertiary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "View Property →",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = tertiary
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Review Comment ────────────────────────────────────────
        Text(
            text       = review.comment,
            fontSize   = 13.sp,
            lineHeight = 20.sp,
            color      = onSurface,
            maxLines   = 4,
            overflow   = TextOverflow.Ellipsis
        )

        // ── Sub-ratings ───────────────────────────────────────────
        val hasSubRatings = review.cleanlinessRating > 0f ||
                review.locationRating > 0f ||
                review.valueRating > 0f

        if (hasSubRatings) {
            Spacer(Modifier.height(10.dp))
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

        // ── Landlord Reply ────────────────────────────────────────
        if (review.hasLandlordReply) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(primary.copy(0.05f))
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(tertiary.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home, null,
                        tint     = tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Landlord Reply",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 12.sp,
                        color      = primary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        review.landlordReply,
                        fontSize   = 12.sp,
                        lineHeight = 18.sp,
                        color      = onSurface,
                        fontStyle  = FontStyle.Italic
                    )
                }
            }
        }
    }
}

// ── Sub Rating Chip ───────────────────────────────────────────────────────────
@Composable
private fun SubRatingChip(label: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = textColor)
    }
}
