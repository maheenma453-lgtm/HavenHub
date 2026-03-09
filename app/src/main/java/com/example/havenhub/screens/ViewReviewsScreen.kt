package com.example.havenhub.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Review
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.ReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReviewsScreen(
    navController: NavController,
    propertyId   : String,
    viewModel    : ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Property ki reviews load karo
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
                    containerColor             = PrimaryBlue,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Rating Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
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
                                color      = PrimaryBlue
                            )
                            Row {
                                (1..5).forEach { s ->
                                    Icon(
                                        imageVector        = if (s <= uiState.averageRating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint     = AccentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text("${uiState.reviews.size} reviews", fontSize = 12.sp, color = TextSecondary)
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
                Text("All Reviews", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
            }

            // Error Message
            if (uiState.errorMessage != null) {
                item {
                    Text(text = uiState.errorMessage!!, color = ErrorRed, fontSize = 14.sp)
                }
            }

            // Empty State
            if (uiState.reviews.isEmpty() && !uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No reviews yet.", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            items(uiState.reviews) { review ->
                ReviewCard(review)
            }
        }
    }
}

@Composable
fun RatingBar(star: Int, count: Int, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("$star", fontSize = 12.sp, color = TextSecondary)
        Icon(Icons.Default.Star, null, tint = AccentGold, modifier = Modifier.size(12.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(6.dp),
            color    = AccentGold
        )
        Text("$count", fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = PrimaryBlue, shape = CircleShape) {
                    Box(
                        modifier         = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            review.reviewerName.firstOrNull()?.toString() ?: "?",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.reviewerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Text(
                        review.createdAt?.toDate()?.toString() ?: "-",
                        fontSize = 11.sp,
                        color    = TextSecondary
                    )
                }
                Row {
                    (1..5).forEach { s ->
                        Icon(
                            imageVector        = if (s <= review.overallRating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint     = AccentGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(review.comment, fontSize = 13.sp, lineHeight = 20.sp, color = TextPrimary)
        }
    }
}