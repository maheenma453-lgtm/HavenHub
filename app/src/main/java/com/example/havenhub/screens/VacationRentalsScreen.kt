package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.VacationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationRentalsScreen(
    navController: NavController,
    viewModel: VacationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadVacationProperties()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vacation Rentals", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(paddingValues)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .background(Brush.horizontalGradient(listOf(PrimaryBlue, Color(0xFF00ACC1))))
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.CenterStart)) {
                        Text("Explore The North 🏔️", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Pre-book your dream vacation home.", fontSize = 14.sp, color = Color.White.copy(0.8f))
                    }
                }
            }

            item {
                Text("Available Properties (${uiState.properties.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(50.dp), Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            } else {
                items(uiState.properties) { prop ->
                    // ✅ FIXED ARGUMENT TYPES HERE
                    VacationPropertyCard(
                        title = prop.title,
                        location = prop.city,
                        price = prop.pricePerNight.toDouble(),
                        rating = prop.averageRating.toDouble(),
                        onClick = {
                            navController.navigate(Screen.PropertyDetail.createRoute(prop.propertyId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VacationPropertyCard(title: String, location: String, price: Double, rating: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(70.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)), Alignment.Center) {
                Text("🏠", fontSize = 30.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(location, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    Text(" $rating", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("PKR ${"%,.0f".format(price)}", color = PrimaryBlue, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}