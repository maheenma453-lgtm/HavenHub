package com.example.havenhub.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController : NavController,
    viewModel     : SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // ── Search Header ──
        Surface(
            color = PrimaryBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }

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
                        .focusRequester(focusRequester)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { navController.navigate(Screen.Filter.route) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.2f))
                ) {
                    Icon(Icons.Default.FilterList, "Filters", tint = Color.White)
                }
            }
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
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
                            fontSize = 13.sp, color = TextSecondary,
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

@Composable
private fun PopularSearchesSection(onSearch: (String) -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text("Popular in Pakistan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        val popular = listOf("Lahore", "Karachi", "Islamabad", "Studio", "Villa")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popular) { term ->
                SuggestionChip(
                    onClick = { onSearch(term) },
                    label = { Text(term) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(property: Property, onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🏠", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(property.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)

                // ✅ FIX: displayName() ki jagah direct logic use kiya
                val typeDisplay = property.propertyType.lowercase().replaceFirstChar { it.uppercase() }
                Text("${property.city} • $typeDisplay", fontSize = 13.sp, color = TextSecondary)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("${property.formattedPrice}/night", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Star, null, tint = AccentGold, modifier = Modifier.size(14.dp))
                    Text(" ${property.averageRating}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = BorderGray)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = BorderGray.copy(0.5f))
    }
}

@Composable
private fun EmptySearchResult(query: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = BorderGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No results for \"$query\"", fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Try checking your spelling or use different keywords.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp, color = TextSecondary)
    }
}