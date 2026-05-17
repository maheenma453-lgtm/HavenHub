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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Review
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

    // ✅ Role check — landlord ko hi reply button dikhao
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val isLandlord = authState.userRole == "landlord"

    LaunchedEffect(propertyId) {
        viewModel.loadPropertyReviews(propertyId)
    }

    // ── Reply dialog state ────────────────────────────────────────────────────
    var showReplyDialog   by remember { mutableStateOf(false) }
    var replyTargetReview by remember { mutableStateOf<Review?>(null) }
    var replyText         by remember { mutableStateOf("") }

    // Dialog dismiss when reply submitted
    LaunchedEffect(uiState.replySuccess) {
        if (uiState.replySuccess) {
            showReplyDialog   = false
            replyText         = ""
            replyTargetReview = null
            viewModel.clearReplySuccess()
        }
    }

    // ── Reply Dialog ──────────────────────────────────────────────────────────
    if (showReplyDialog && replyTargetReview != null) {
        Dialog(onDismissRequest = {
            showReplyDialog   = false
            replyText         = ""
            replyTargetReview = null
        }) {
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Reply,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Reply to Review",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "by ${replyTargetReview!!.reviewerName}",
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Original review preview
                    Card(
                        shape  = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text       = replyTargetReview!!.comment,
                            modifier   = Modifier.padding(12.dp),
                            fontSize   = 13.sp,
                            lineHeight = 19.sp,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines   = 3
                        )
                    }

                    // Reply text field
                    OutlinedTextField(
                        value         = replyText,
                        onValueChange = { if (it.length <= 300) replyText = it },
                        modifier      = Modifier.fillMaxWidth().height(120.dp),
                        placeholder   = { Text("Apna jawab likhein...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        shape         = RoundedCornerShape(12.dp),
                        maxLines      = 5,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Text(
                        "${replyText.length}/300",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )

                    // Buttons
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = {
                                showReplyDialog   = false
                                replyText         = ""
                                replyTargetReview = null
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick  = {
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
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            enabled  = replyText.isNotBlank() && !uiState.isReplyLoading,
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isReplyLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color       = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Submit", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
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

            uiState.errorMessage?.let { err ->
                item {
                    Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }

            if (uiState.reviews.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }

            items(uiState.reviews) { review ->
                ReviewCard(
                    review       = review,
                    isLandlord   = isLandlord,
                    onReplyClick = {
                        replyTargetReview = review
                        // Agar pehle se reply hai toh edit ke liye prefill karo
                        replyText         = if (review.hasLandlordReply) review.landlordReply else ""
                        showReplyDialog   = true
                    }
                )
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
        Text("$star", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(6.dp),
            color    = MaterialTheme.colorScheme.tertiary
        )
        Text("$count", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Review Card ────────────────────────────────────────────────────
@Composable
fun ReviewCard(
    review      : Review,
    isLandlord  : Boolean  = false,
    onReplyClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Reviewer Info Row ──────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
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

            // ── Landlord Reply ─────────────────────────────────────
            if (review.hasLandlordReply) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.8.dp)
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
                                        "${it.date} ${
                                            arrayOf("Jan","Feb","Mar","Apr","May","Jun",
                                                "Jul","Aug","Sep","Oct","Nov","Dec")[it.month]
                                        } ${it.year + 1900}"
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

            // ── Reply / Edit Button (sirf landlord ke liye) ────────
            // ✅ NEW
            if (isLandlord) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick  = onReplyClick,
                    modifier = Modifier.align(Alignment.End),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector        = if (review.hasLandlordReply) Icons.Default.Edit else Icons.Default.Reply,
                        contentDescription = null,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text       = if (review.hasLandlordReply) "Edit Reply" else "Reply",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
