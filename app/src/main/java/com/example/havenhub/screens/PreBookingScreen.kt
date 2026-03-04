package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
// FIX: Correct Imports
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.VacationViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreBookingScreen(
    navController: NavController,
    viewModel: VacationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Form State
    var selectedDestination by remember { mutableStateOf("") }
    var checkInDate by remember { mutableStateOf("") }
    var checkOutDate by remember { mutableStateOf("") }
    var guestCount by remember { mutableStateOf(2) }
    var propertyType by remember { mutableStateOf("") }

    val totalAmount = 25000.0
    val depositAmount = totalAmount * 0.20 // 20% deposit

    val destinations = listOf("Murree", "Swat Valley", "Hunza", "Nathia Gali", "Chitral", "Kaghan")
    val propertyTypes = listOf("Cottage", "Hotel", "Guest House", "Cabin", "Chalet")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Book Vacation", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Advance Booking Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Advance Booking — Pay 20% Deposit", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 13.sp)
                        Text("Pay PKR ${"%,.0f".format(depositAmount)} now to secure booking.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel("Select Destination")
            destinations.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { dest ->
                        val isSelected = selectedDestination == dest
                        DestinationChip(dest, isSelected) { selectedDestination = dest }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel("Travel Dates")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VacDateField("Check-In", checkInDate, Modifier.weight(1f)) { checkInDate = "15 Oct 2024" /* Placeholder for Picker */ }
                VacDateField("Check-Out", checkOutDate, Modifier.weight(1f)) { checkOutDate = "20 Oct 2024" /* Placeholder for Picker */ }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel("Number of Guests")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CounterButton("−") { if (guestCount > 1) guestCount-- }
                Text("$guestCount", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                CounterButton("+", isPrimary = true) { if (guestCount < 15) guestCount++ }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Payment Summary", fontWeight = FontWeight.Bold)
                    PriceRow("Total Amount", "PKR ${"%,.0f".format(totalAmount)}")
                    PriceRow("Deposit (20%)", "PKR ${"%,.0f".format(depositAmount)}", Color(0xFFE67E22))
                    PriceRow("Due on Arrival", "PKR ${"%,.0f".format(totalAmount - depositAmount)}")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Note: Ensure createPreBooking exists in ViewModel or adjust to your needs
                    navController.navigate(Screen.Booking.route)
                },
                enabled = selectedDestination.isNotEmpty() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Pay Deposit PKR ${"%,.0f".format(depositAmount)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DestinationChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .width(150.dp) // Fixed width for symmetry
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryBlue else Color(0xFFF0F0F0))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(name, color = if (isSelected) Color.White else Color.Black, fontSize = 13.sp)
    }
}

@Composable
fun CounterButton(text: String, isPrimary: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPrimary) PrimaryBlue else Color(0xFFEEEEEE))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 20.sp, color = if (isPrimary) Color.White else Color.Black)
    }
}

@Composable
fun PriceRow(label: String, value: String, color: Color = Color.Black) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun VacDateField(label: String, date: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp), tint = PrimaryBlue)
                Spacer(Modifier.width(8.dp))
                Text(if (date.isEmpty()) "Select" else date, fontSize = 13.sp)
            }
        }
    }
}