package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.havenhub.data.Property
import com.example.havenhub.navigation.Screen
import com.example.havenhub.utils.getPropertyImage
import com.example.havenhub.viewmodel.PropertyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListScreen(
    navController: NavController,
    viewModel    : PropertyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAllProperties()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Properties") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color    = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.allProperties.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No properties found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.allProperties) { property ->
                        AllPropertyCard(property) {
                            navController.navigate(
                                Screen.PropertyDetail.createRoute(property.propertyId)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Property Image Helper ─────────────────────────────────────────────────────
// ✅ FIX: Remote URL pehle check karo, phir drawableImageName,
//         phir resolvedDrawableName, aakhir mein propertyId fallback
@Composable
private fun PLPropertyImage(property: Property, modifier: Modifier = Modifier) {
    val remoteUrl = property.imageUrls.firstOrNull { it.isNotBlank() }
    when {
        // 1st: Firestore mein imageUrls hai → remote se load karo
        !remoteUrl.isNullOrBlank() -> {
            AsyncImage(
                model              = remoteUrl,
                contentDescription = property.title,
                modifier           = modifier,
                contentScale       = ContentScale.Crop
            )
        }
        // 2nd: drawableImageName Firestore mein set hai
        property.drawableImageName.isNotBlank() -> {
            Image(
                painter            = painterResource(
                    id = getPropertyImage(property.drawableImageName)
                ),
                contentDescription = property.title,
                modifier           = modifier,
                contentScale       = ContentScale.Crop
            )
        }
        // 3rd: resolvedDrawableName (city+type se compute hota hai)
        property.resolvedDrawableName.isNotBlank() -> {
            Image(
                painter            = painterResource(
                    id = getPropertyImage(property.resolvedDrawableName)
                ),
                contentDescription = property.title,
                modifier           = modifier,
                contentScale       = ContentScale.Crop
            )
        }
        // 4th: propertyId fallback (prop_001, prop_002, etc.)
        else -> {
            Image(
                painter            = painterResource(
                    id = getPropertyImage(property.propertyId)
                ),
                contentDescription = property.title,
                modifier           = modifier,
                contentScale       = ContentScale.Crop
            )
        }
    }
}

@Composable
fun AllPropertyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // ✅ FIX: PLPropertyImage use karo — 4-level fallback logic
            PLPropertyImage(
                property = property,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    property.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    property.formattedPrice + " / night",
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "📍 ${property.city}",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}