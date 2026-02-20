package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Review(val id: String, val author: String, val rating: Int, val comment: String, val date: String)

val dummyReviews = listOf(
    Review("R1", "Sara Khan", 5, "Absolutely loved the view and the cleanliness. Host was very responsive and helpful. Will definitely book again!", "Nov 2024"),
    Review("R2", "Umar Farooq", 4, "Great location and comfortable stay. Just minor issues with WiFi speed.", "Oct 2024"),
    Review("R3", "Hina Malik", 5, "Perfect for a family getaway. Everything was as shown in the pictures.", "Sep 2024"),
    Review("R4", "Ahmed Raza", 3, "Decent place but the kitchen could use some upgrades. Overall okay stay.", "Aug 2024"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReviewsScreen(onBack: () -> Unit = {}) {
    val avgRating = dummyReviews.map { it.rating }.average()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // Rating Overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(String.format("%.1f", avgRating), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row {
                                (1..5).forEach { s ->
                                    Icon(if (s <= avgRating.toInt()) Icons.Default.Star else Icons.Default.StarBorder, null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                                }
                            }
                            Text("${dummyReviews.size} reviews", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            (5 downTo 1).forEach { star ->
                                val count = dummyReviews.count { it.rating == star }
                                RatingBar(star = star, count = count, total = dummyReviews.size)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("All Reviews", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
            }
            items(dummyReviews) { review ->
                ReviewCard(review)
            }
        }
    }
}

@Composable
fun RatingBar(star: Int, count: Int, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$star", fontSize = 12.sp, color = Color.Gray)
        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f).height(6.dp), color = Color(0xFFFFC107))
        Text("$count", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Text(review.author.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.author, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(review.date, fontSize = 11.sp, color = Color.Gray)
                }
                Row {
                    (1..5).forEach { s ->
                        Icon(if (s <= review.rating) Icons.Default.Star else Icons.Default.StarBorder, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(review.comment, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}