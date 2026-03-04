package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
// FIX: Correcting your specific project imports
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.VacationViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationCalendarScreen(
    navController: NavController,
    propertyId: String = "",
    viewModel: VacationViewModel = hiltViewModel()
) {
    // FIX: Sync with VacationViewModel uiState
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(propertyId) {
        if (propertyId.isNotEmpty()) {
            viewModel.loadUnavailableDates(propertyId)
        }
    }

    // Calendar Display Logic
    val calendar = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    var selectedCheckIn by remember { mutableIntStateOf(-1) }
    var selectedCheckOut by remember { mutableIntStateOf(-1) }

    val monthNames = listOf("January","February","March","April","May","June","July","August","September","October","November","December")
    val dayNames = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

    val daysInMonth = getDaysInMonth(currentMonth, currentYear)
    val firstDayOfWeek = getFirstDayOfWeek(currentMonth, currentYear)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Availability Calendar", color = Color.White) },
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
                .padding(16.dp)
        ) {
            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth-- }) {
                    Icon(Icons.Default.ChevronLeft, "Prev", tint = PrimaryBlue)
                }
                Text("${monthNames[currentMonth]} $currentYear", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++ }) {
                    Icon(Icons.Default.ChevronRight, "Next", tint = PrimaryBlue)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                dayNames.forEach { day ->
                    Text(day, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val day = cellIndex - firstDayOfWeek + 1

                            if (day < 1 || day > daysInMonth) {
                                Box(Modifier.weight(1f).height(40.dp))
                            } else {
                                // Logic to check if date is booked from ViewModel's unavailableDates
                                val isBooked = checkIsDateBooked(day, currentMonth, currentYear, uiState.unavailableDates)

                                CalendarDay(
                                    day = day,
                                    isBooked = isBooked,
                                    isSelected = (day == selectedCheckIn || day == selectedCheckOut),
                                    isInRange = selectedCheckIn != -1 && selectedCheckOut != -1 && day > selectedCheckIn && day < selectedCheckOut,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!isBooked) {
                                            if (selectedCheckIn == -1 || day <= selectedCheckIn) {
                                                selectedCheckIn = day
                                                selectedCheckOut = -1
                                            } else {
                                                selectedCheckOut = day
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legend
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(Color(0xFF4CAF50), "Available")
                LegendItem(Color(0xFFF44336), "Booked")
                LegendItem(PrimaryBlue, "Selected")
            }

            Spacer(Modifier.weight(1f))

            // Selection Info & Action
            if (selectedCheckIn != -1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Stay Duration", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                if (selectedCheckOut != -1) "$selectedCheckIn - $selectedCheckOut ${monthNames[currentMonth]}"
                                else "Starts $selectedCheckIn ${monthNames[currentMonth]}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (selectedCheckOut != -1) {
                            Button(
                                onClick = { navController.navigate(Screen.PreBooking.route) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("Continue")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(day: Int, isBooked: Boolean, isSelected: Boolean, isInRange: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bgColor = when {
        isSelected -> PrimaryBlue
        isInRange -> PrimaryBlue.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isSelected -> Color.White
        isBooked -> Color.Red.copy(alpha = 0.5f)
        else -> Color.Black
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = !isBooked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$day", color = contentColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isBooked) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp).background(Color.Red, CircleShape))
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

// Helpers
private fun getDaysInMonth(month: Int, year: Int) = Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)

private fun getFirstDayOfWeek(month: Int, year: Int) = Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK) - 1

private fun checkIsDateBooked(day: Int, month: Int, year: Int, unavailableDates: List<Date>): Boolean {
    val cal = Calendar.getInstance()
    return unavailableDates.any {
        cal.time = it
        cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
    }
}