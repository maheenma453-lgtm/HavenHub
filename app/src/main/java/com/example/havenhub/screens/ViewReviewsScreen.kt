package com.example.havenhub.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Review
import com.example.havenhub.viewmodel.ReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReviewsScreen(
    navController: NavController,
    propertyId   : String,
    viewModel    : ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(propertyId) {
        viewModel.loadPropertyReviews(propertyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // ── Rating Overview Card ───────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
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
                                String.format("%.1f", uiState.averageRating),
                                fontSize   = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                (1..5).forEach { s ->
                                    Icon(
                                        imageVector        = if (s <= uiState.averageRating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint     = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                "${uiState.reviews.size} reviews",
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(
                            modifier            = Modifier.weight(2f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (5 downTo 1).forEach { star ->
                                val count = uiState.reviews.count { it.overallRating.toInt() == star }
                                RatingBar(star = star, count = count, total = uiState.reviews.size)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "All Reviews",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
            }

            if (uiState.errorMessage != null) {
                item {
                    Text(
                        text  = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }

            if (uiState.reviews.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No reviews yet.",
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            items(uiState.reviews) { review ->
                ReviewCard(review)
            }
        }
    }
}

// ── Rating Bar ─────────────────────────────────────────────────────
@Composable
fun RatingBar(star: Int, count: Int, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "$star",
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            Icons.Default.Star,
            null,
            tint     = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(12.dp)
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(6.dp),
            color    = MaterialTheme.colorScheme.tertiary
        )
        Text(
            "$count",
            fontSize = 11.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Review Card ────────────────────────────────────────────────────
@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Reviewer Info Row ──────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color    = MaterialTheme.colorScheme.primary,
                    shape    = CircleShape
                ) {
                    Box(
                        modifier         = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = review.reviewerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color      = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = review.reviewerName.ifEmpty { "Anonymous" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text     = review.createdAt?.toDate()?.toString() ?: "-",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    (1..5).forEach { s ->
                        Icon(
                            imageVector        = if (s <= review.overallRating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Review Comment ─────────────────────────────────────
            Text(
                review.comment,
                fontSize   = 13.sp,
                lineHeight = 20.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )

            // ── Landlord Reply Section ──────────────────────────────
            if (review.hasLandlordReply) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline,
                    thickness = 0.8.dp
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    // Landlord Avatar
                    Surface(
                        color    = MaterialTheme.colorScheme.tertiary,
                        shape    = CircleShape,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector        = Icons.Default.Home,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Landlord Reply",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 12.sp,
                                color      = MaterialTheme.colorScheme.primary
                            )
                            review.landlordRepliedAt?.let { ts ->
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "• ${ts.toDate().let {
                                        "${it.date} ${it.month.let { m ->
                                            arrayOf("Jan","Feb","Mar","Apr","May","Jun",
                                                "Jul","Aug","Sep","Oct","Nov","Dec")[m]
                                        }} ${it.year + 1900}"
                                    }}",
                                    fontSize = 10.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            review.landlordReply,
                            fontSize   = 12.sp,
                            lineHeight = 18.sp,
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontStyle  = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}
