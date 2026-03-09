package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    navController : NavController,
    viewModel     : SearchViewModel = hiltViewModel()
) {
    // ── ViewModel State Observation ──
    // Ab hum sirf single uiState collect kar rahe hain
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Auto-focus on search bar when screen opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // ── Search Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryBlue)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = BackgroundWhite)
                }

                // Search TextField
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Search properties...", color = TextSecondary.copy(0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Clear, null, tint = TextSecondary)
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BackgroundWhite,
                        unfocusedContainerColor = BackgroundWhite,
                        focusedBorderColor = PrimaryBlueLight,
                        unfocusedBorderColor = BorderGray
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Filter Button
                IconButton(
                    onClick = { navController.navigate(Screen.Filter.route) },
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(BackgroundWhite.copy(0.2f))
                ) {
                    Icon(Icons.Default.FilterList, "Filters", tint = BackgroundWhite)
                }
            }
        }

        // ── Progress Bar ──
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
        }

        // ── Content Area ──
        when {
            // Case 1: Empty Search Query - Show Popular Searches
            uiState.searchQuery.isEmpty() -> {
                PopularSearchesSection { term -> viewModel.onQueryChange(term) }
            }

            // Case 2: Results Found
            uiState.searchResults.isNotEmpty() -> {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    item {
                        Text(
                            text = "${uiState.searchResults.size} properties found",
                            fontSize = 13.sp, color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(uiState.searchResults) { property ->
                        SearchResultItem(property) {
                            navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                        }
                    }
                }
            }

            // Case 3: No Results & Not Loading
            !uiState.isLoading -> {
                EmptySearchResult(uiState.searchQuery)
            }
        }
    }
}

@Composable
private fun PopularSearchesSection(onSearch: (String) -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text("Popular in Pakistan", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        val popular = listOf("Lahore", "Karachi", "Islamabad", "Studio", "Villa")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popular) { term ->
                SuggestionChip(
                    onClick = { onSearch(term) },
                    label = { Text(term) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(property: Property, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceVariantLight),
            contentAlignment = Alignment.Center
        ) {
            Text("🏠", fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(property.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("${property.city} • ${property.propertyType.displayName()}", fontSize = 12.sp, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⭐ ${property.averageRating}", fontSize = 12.sp, color = AccentGold)
                Spacer(modifier = Modifier.width(10.dp))
                Text("${property.formattedPrice}/night", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            }
        }
        Text("›", fontSize = 20.sp, color = TextSecondary)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderGray, thickness = 0.5.dp)
}

@Composable
private fun EmptySearchResult(query: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔍", fontSize = 56.sp)
        Text("No results for \"$query\"", fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("Try different keywords.", fontSize = 13.sp, color = TextSecondary)
    }
}