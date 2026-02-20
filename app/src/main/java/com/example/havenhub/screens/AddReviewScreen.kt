package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(onBack: () -> Unit = {}, onSubmit: () -> Unit = {}) {
    var rating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    var cleanliness by remember { mutableIntStateOf(0) }
    var location by remember { mutableIntStateOf(0) }
    var value by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write a Review", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Property Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Luxury Sea View Apartment", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Stayed: Nov 10 – Nov 14, 2024", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // Overall Rating
            Text("Overall Rating", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { rating = star }) {
                        Icon(
                            if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (star <= rating) Color(0xFFFFC107) else Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                if (rating > 0) {
                    Text("$rating/5", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC107), modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            // Category Ratings
            Text("Category Ratings", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            CategoryRating("Cleanliness", cleanliness) { cleanliness = it }
            CategoryRating("Location", location) { location = it }
            CategoryRating("Value for Money", value) { value = it }

            // Review Text
            Text("Your Review", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            OutlinedTextField(
                value = reviewText,
                onValueChange = { reviewText = it },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                placeholder = { Text("Share your experience...") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )
            Text("${reviewText.length}/500 characters", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = rating > 0 && reviewText.isNotBlank()
            ) {
                Text("Submit Review", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun CategoryRating(label: String, value: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.width(130.dp))
        Row {
            (1..5).forEach { star ->
                IconButton(onClick = { onSelect(star) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (star <= value) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (star <= value) Color(0xFFFFC107) else Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
