package com.example.havenhub.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.havenhub.R
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel    : SearchViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val focusRequester  = remember { FocusRequester() }
    val focusManager    = LocalFocusManager.current
    val isSearching     = uiState.searchQuery.isNotEmpty()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSearch()
    }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.verticalGradient(listOf(primary, primaryContainer)))
                    .statusBarsPadding()
                    .padding(bottom = 24.dp)
                    .animateContentSize()
            ) {
                Column {
                    Row(
                        modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = onPrimary)
                        }
                        Text(
                            "Find Your Stay",
                            color      = onPrimary,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(start = 8.dp)
                        )

                        if (uiState.hasActiveFilter) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tertiary.copy(alpha = 0.9f)
                            ) {
                                Row(
                                    modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.FilterAlt, null,
                                        tint     = onPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Filtered",
                                        color    = onPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close, null,
                                        tint     = onPrimary,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.clearFilters() }
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        TextField(
                            value         = uiState.searchQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            placeholder   = {
                                Text(
                                    "Search city, area or type...",
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon  = {
                                Icon(Icons.Default.Search, null, tint = tertiary)
                            },
                            trailingIcon = {
                                if (isSearching) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(
                                            Icons.Default.Close, null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.Tune, null,
                                        tint     = if (uiState.hasActiveFilter) tertiary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable {
                                            navController.navigate(Screen.Filter.route)
                                        }
                                    )
                                }
                            },
                            singleLine      = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                viewModel.addToHistory(uiState.searchQuery)
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor   = MaterialTheme.colorScheme.surface,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                                cursorColor             = tertiary,
                                focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color    = tertiary
                )
            }

            when {
                (isSearching || uiState.hasActiveFilter) && uiState.searchResults.isNotEmpty() -> {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${uiState.searchResults.size} properties found",
                                    fontSize   = 13.sp,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                if (uiState.hasActiveFilter) {
                                    Text(
                                        "Clear Filters",
                                        fontSize   = 13.sp,
                                        color      = tertiary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier   = Modifier.clickable { viewModel.clearFilters() }
                                    )
                                }
                            }
                        }
                        items(uiState.searchResults) { property ->
                            ModernSearchCard(property) {
                                viewModel.addToHistory(uiState.searchQuery)
                                navController.navigate(
                                    Screen.PropertyDetail.createRoute(property.propertyId)
                                )
                            }
                        }
                    }
                }

                (isSearching || uiState.hasActiveFilter) && !uiState.isLoading -> {
                    EmptySearchUI(
                        query         = uiState.searchQuery,
                        hasFilter     = uiState.hasActiveFilter,
                        onClearFilter = { viewModel.clearFilters() }
                    )
                }

                !isSearching && !uiState.hasActiveFilter -> {
                    Column(
                        modifier = Modifier
                            .padding(28.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Popular Regions",
                            fontWeight = FontWeight.Black,
                            fontSize   = 20.sp,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val regions = listOf("Lahore", "Islamabad", "Murree", "Hunza", "Skardu", "Karachi")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(regions) { city ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        viewModel.onQueryChange(city)
                                        viewModel.addToHistory(city)
                                        focusManager.clearFocus()
                                    },
                                    shape  = RoundedCornerShape(12.dp),
                                    color  = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        city,
                                        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Activity",
                                fontWeight = FontWeight.Black,
                                fontSize   = 20.sp,
                                color      = MaterialTheme.colorScheme.onBackground
                            )
                            if (uiState.recentSearches.isNotEmpty()) {
                                Text(
                                    text       = "Clear All",
                                    color      = tertiary,
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier   = Modifier.clickable { viewModel.clearHistory() }
                                )
                            }
                        }

                        if (uiState.recentSearches.isEmpty()) {
                            Text(
                                "Your search history is empty",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            uiState.recentSearches.forEach { search ->
                                Row(
                                    modifier          = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.onQueryChange(search)
                                                focusManager.clearFocus()
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.History, null,
                                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            search,
                                            color    = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 16.sp
                                        )
                                    }

                                    IconButton(
                                        onClick  = { viewModel.removeFromHistory(search) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier           = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ✅ FIXED: ModernSearchCard
//
// PURANA CODE (broken):
//   Image(painter = painterResource(id = getPropertyImage(property.propertyId)), ...)
//   → Sirf local drawables load karta tha (prop_001 .. prop_012)
//   → Auto-ID wali properties ka koi drawable nahi → HavenHub logo dikhta tha
//
// NAYA CODE (fixed):
//   Priority 1: property.imageUrls.first() → ImgBB URL (AsyncImage se load)
//   Priority 2: property.drawableImageName → drawable fallback
//   Priority 3: property.propertyId       → getPropertyImage() fallback
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ModernSearchCard(property: Property, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // ── Image Logic ──────────────────────────────────────────────────
            // Priority 1: ImgBB URL (app-added / auto-ID properties)
            if (property.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(property.imageUrls.first())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier           = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale  = ContentScale.Crop,
                    // Jab tak URL load ho, drawable fallback dikhao
                    placeholder   = painterResource(
                        id = getPropertyImage(
                            property.drawableImageName.ifBlank { property.propertyId }
                        )
                    ),
                    // URL fail ho jaye tab bhi drawable fallback
                    error         = painterResource(
                        id = getPropertyImage(
                            property.drawableImageName.ifBlank { property.propertyId }
                        )
                    )
                )
            } else {
                // Priority 2 & 3: drawableImageName ya propertyId se drawable
                androidx.compose.foundation.Image(
                    painter            = painterResource(
                        id = getPropertyImage(
                            property.drawableImageName.ifBlank { property.propertyId }
                        )
                    ),
                    contentDescription = null,
                    modifier           = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            // ── End Image Logic ──────────────────────────────────────────────

            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(
                    property.title,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        " ${property.city}",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    property.formattedPrice,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    fontSize   = 18.sp,
                    modifier   = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptySearchUI(
    query        : String,
    hasFilter    : Boolean   = false,
    onClearFilter: () -> Unit = {}
) {
    val tertiary = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff, null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (query.isNotEmpty()) "No results for \"$query\""
            else "No properties match your filters",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (hasFilter) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Try different filters or clear them",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClearFilter,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, tertiary)
            ) {
                Icon(
                    Icons.Default.FilterAltOff,
                    null,
                    tint = tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Clear Filters", color = tertiary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}