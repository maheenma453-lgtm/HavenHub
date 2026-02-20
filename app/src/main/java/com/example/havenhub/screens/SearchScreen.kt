package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.SearchViewModel

// ─────────────────────────────────────────────────────────────────
// SearchScreen.kt
// PURPOSE : Real-time property search with live results.
//           Users type to filter properties by title, city, or type.
//           Filter button opens FilterScreen for advanced options.
//           Shows recent searches when search bar is empty.
// NAVIGATION:
//   → FilterScreen (advanced filters)
//   → PropertyDetailScreen (from result tap)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController : NavController,
    viewModel     : SearchViewModel = hiltViewModel()
) {

    // ── State ──────────────────────────────────────────────────────
    var searchQuery    by remember { mutableStateOf("") }
    val focusRequester =  remember { FocusRequester() }

    val searchResults  by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val isSearching    by viewModel.isLoading.collectAsState()

    // Auto-focus keyboard when screen opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // ── UI ─────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {

        // ── Search Header ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryBlue)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Back button
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector        = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint               = BackgroundWhite
                    )
                }

                // ── Search TextField ──────────────────────────────
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        // Trigger live search as user types
                        viewModel.search(query)
                    },
                    placeholder = { Text("Search properties...", color = TextHint) },
                    leadingIcon = {
                        Icon(
                            imageVector        = Icons.Default.Search,
                            contentDescription = "Search",
                            tint               = TextSecondary
                        )
                    },
                    // Clear button appears when query is not empty
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.search("")
                            }) {
                                Icon(
                                    imageVector        = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint               = TextSecondary
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier   = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape      = RoundedCornerShape(10.dp),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor   = BackgroundWhite,
                        unfocusedContainerColor = BackgroundWhite,
                        focusedBorderColor      = PrimaryLight,
                        unfocusedBorderColor    = BorderGray
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // ── Filter Button ─────────────────────────────────
                IconButton(
                    onClick = { navController.navigate(Screen.Filter.route) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundWhite.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector        = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint               = BackgroundWhite
                    )
                }
            }
        }

        // ── Loading Indicator ──────────────────────────────────────
        if (isSearching) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color    = PrimaryBlue
            )
        }

        // ── Content Area ───────────────────────────────────────────
        if (searchQuery.isEmpty()) {
            // Show recent searches when no query
            RecentSearchesSection(
                searches  = recentSearches,
                onSearch  = { term ->
                    searchQuery = term
                    viewModel.search(term)
                },
                onClear   = { viewModel.clearRecentSearches() }
            )
        } else if (searchResults.isEmpty() && !isSearching) {
            // No results found state
            EmptySearchResult(query = searchQuery)
        } else {
            // ── Search Results List ───────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {

                // Results count header
                item {
                    Text(
                        text     = "${searchResults.size} properties found",
                        fontSize = 13.sp,
                        color    = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // Individual result items
                items(searchResults) { property ->
                    SearchResultItem(
                        title    = property.title,
                        city     = property.city,
                        type     = property.type,
                        price    = property.price,
                        rating   = property.rating,
                        onClick  = {
                            // Save to recent searches before navigating
                            viewModel.saveRecentSearch(searchQuery)
                            navController.navigate(Screen.PropertyDetail.createRoute(property.id))
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// RecentSearchesSection
// Shows chips of recent search terms
// ─────────────────────────────────────────────────────────────────
@Composable
private fun RecentSearchesSection(
    searches : List<String>,
    onSearch : (String) -> Unit,
    onClear  : () -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {

        if (searches.isNotEmpty()) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Recent Searches",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
                TextButton(onClick = onClear) {
                    Text(text = "Clear All", color = ErrorRed, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recent search chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searches) { term ->
                    SuggestionChip(
                        onClick = { onSearch(term) },
                        label   = { Text(term, fontSize = 13.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Popular Searches ──────────────────────────────────────
        Text(
            text       = "Popular in Pakistan",
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        val popularSearches = listOf(
            "🏙️ Lahore", "🌆 Karachi", "🏛️ Islamabad",
            "🏠 1 BHK", "🏢 Studio", "🏔️ Murree"
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popularSearches) { term ->
                SuggestionChip(
                    onClick = { onSearch(term.substringAfter(" ")) },
                    label   = { Text(term, fontSize = 13.sp) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// SearchResultItem
// Single property row in search results
// ─────────────────────────────────────────────────────────────────
@Composable
private fun SearchResultItem(
    title   : String,
    city    : String,
    type    : String,
    price   : Long,
    rating  : Float,
    onClick : () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceGray),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🏠", fontSize = 26.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "$city • $type", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⭐ $rating", fontSize = 12.sp, color = StarGold)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text       = "PKR $price/mo",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = PrimaryBlue
                )
            }
        }

        // Arrow indicator
        Text(text = "›", fontSize = 20.sp, color = TextSecondary)
    }

    // Divider between items
    Divider(
        modifier  = Modifier.padding(horizontal = 20.dp),
        color     = BorderGray,
        thickness = 0.5.dp
    )
}

// ─────────────────────────────────────────────────────────────────
// EmptySearchResult
// Shows when search query returns no results
// ─────────────────────────────────────────────────────────────────
@Composable
private fun EmptySearchResult(query: String) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🔍", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text       = "No results for \"$query\"",
            fontSize   = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = "Try searching with different keywords or check your spelling.",
            fontSize  = 13.sp,
            color     = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}


