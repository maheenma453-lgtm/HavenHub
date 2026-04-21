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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.SearchViewModel

private val HavenDeepBlue = Color(0xFF0D1B3E)
private val HavenGold = Color(0xFFD4AF37)
private val HavenBg = Color(0xFFF1F5F9)
private val MutedText = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val isSearching = uiState.searchQuery.isNotEmpty()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = HavenBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.verticalGradient(listOf(HavenDeepBlue, Color(0xFF1E3A8A))))
                    .statusBarsPadding()
                    .padding(bottom = 24.dp)
                    .animateContentSize()
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Text("Find Your Stay", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White
                    ) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            placeholder = { Text("Search city, area or type...", color = Color.Gray, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = HavenGold) },
                            trailingIcon = {
                                if (isSearching) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                                    }
                                } else {
                                    Icon(Icons.Default.Tune, null, tint = HavenGold,
                                        modifier = Modifier.clickable { navController.navigate(Screen.Filter.route) })
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                viewModel.addToHistory(uiState.searchQuery)
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = HavenGold,
                                focusedTextColor = HavenDeepBlue
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = HavenGold)
            }

            when {
                uiState.searchQuery.isEmpty() -> {
                    Column(modifier = Modifier.padding(28.dp).verticalScroll(rememberScrollState())) {
                        Text("Popular Regions", fontWeight = FontWeight.Black, fontSize = 20.sp, color = HavenDeepBlue)
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(city, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HavenDeepBlue)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // ✅ Header Row with Clear All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Activity", fontWeight = FontWeight.Black, fontSize = 20.sp, color = HavenDeepBlue)
                            if (uiState.recentSearches.isNotEmpty()) {
                                Text(
                                    text = "Clear All",
                                    color = HavenGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.clearHistory() }
                                )
                            }
                        }

                        if (uiState.recentSearches.isEmpty()) {
                            Text("Your search history is empty", color = MutedText, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            uiState.recentSearches.forEach { search ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Clickable area for Search Text
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
                                        Icon(Icons.Default.History, null, tint = MutedText, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(search, color = HavenDeepBlue, fontSize = 16.sp)
                                    }

                                    // ✅ Individual Delete Icon
                                    IconButton(
                                        onClick = { viewModel.removeFromHistory(search) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.Gray.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color.LightGray.copy(0.3f))
                            }
                        }
                    }
                }
                uiState.searchResults.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
                        items(uiState.searchResults) { property ->
                            ModernSearchCard(property) {
                                viewModel.addToHistory(uiState.searchQuery)
                                navController.navigate(Screen.PropertyDetail.createRoute(property.propertyId))
                            }
                        }
                    }
                }
                else -> { EmptySearchUI(uiState.searchQuery) }
            }
        }
    }
}

@Composable
fun ModernSearchCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = getPropertyImage(property.propertyId)),
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(property.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = HavenDeepBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = MutedText, modifier = Modifier.size(12.dp))
                    Text(" ${property.city}", fontSize = 13.sp, color = MutedText)
                }
                Text(property.formattedPrice, color = HavenDeepBlue, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun EmptySearchUI(query: String) {
    Column(modifier = Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Search, null, modifier = Modifier.size(60.dp), tint = Color.LightGray)
        Spacer(Modifier.height(20.dp))
        Text("No results for \"$query\"", fontWeight = FontWeight.Bold, color = HavenDeepBlue)
    }
}