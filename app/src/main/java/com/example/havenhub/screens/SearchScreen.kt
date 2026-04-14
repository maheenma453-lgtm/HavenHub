package com.example.havenhub.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0D1B3E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Search city, property type...", color = Color.White.copy(0.6f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(0.7f)) },
                        trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, null, tint = Color.White.copy(0.7f))
                                }
                            }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(0.15f),
                            unfocusedContainerColor = Color.White.copy(0.15f),
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { navController.navigate(Screen.Filter.route) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.2f))
                    ) {
                        Icon(Icons.Default.FilterList, "Filters", tint = Color(0xFFD4AF37))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F7FA))
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFD4AF37),
                    trackColor = Color(0xFF0D1B3E)
                )
            }

            when {
                uiState.searchQuery.isEmpty() -> {
                    PopularSearchesSection { term -> viewModel.onQueryChange(term) }
                }
                uiState.searchResults.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "${uiState.searchResults.size} properties found",
                                fontSize = 13.sp,
                                color = Color(0xFF8899AA),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                        items(uiState.searchResults, key = { it.propertyId }) { property ->
                            SearchResultItem(property) {
                                navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                            }
                        }
                    }
                }
                !uiState.isLoading -> {
                    EmptySearchResult(uiState.searchQuery)
                }
            }
        }
    }
}

@Composable
private fun PopularSearchesSection(onSearch: (String) -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            "Popular in Pakistan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D1B3E)
        )
        Spacer(modifier = Modifier.height(12.dp))
        val popular = listOf("Lahore", "Karachi", "Islamabad", "Murree", "Hunza", "Swat", "Studio", "Villa")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popular) { term ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable { onSearch(term) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(term, fontSize = 13.sp, color = Color(0xFF0D1B3E), fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Recent Searches",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D1B3E)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your recent searches will appear here",
            fontSize = 13.sp,
            color = Color(0xFF8899AA)
        )
    }
}

@Composable
private fun SearchResultItem(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = getPropertyImage(property.propertyId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    property.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = Color(0xFF0D1B3E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        " ${property.city} • ${property.propertyType.toString()}",
                        fontSize = 12.sp,
                        color = Color(0xFF8899AA)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${property.formattedPrice}/night",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B3E)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                        Text(" ${property.averageRating}", fontSize = 12.sp, color = Color(0xFF0D1B3E), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResult(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFE0E0E0)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No results for \"$query\"",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF0D1B3E)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Try checking your spelling or use different keywords",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = Color(0xFF8899AA)
        )
    }
}