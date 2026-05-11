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
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.VacationViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationCalendarScreen(
    navController: NavController,
    propertyId   : String = "",
    viewModel    : VacationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(propertyId) {
        if (propertyId.isNotEmpty()) {
            viewModel.loadUnavailableDates(propertyId)
        }
    }

    val calendar     = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear  by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    var selectedCheckIn  by remember { mutableIntStateOf(-1) }
    var selectedCheckOut by remember { mutableIntStateOf(-1) }

    val monthNames  = listOf("January","February","March","April","May","June","July","August","September","October","November","December")
    val dayNames    = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
    val daysInMonth = getDaysInMonth(currentMonth, currentYear)
    val firstDayOfWeek = getFirstDayOfWeek(currentMonth, currentYear)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Availability Calendar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Month Navigation
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (currentMonth == 0) { currentMonth = 11; currentYear-- }
                    else currentMonth--
                }) {
                    Icon(
                        Icons.Default.ChevronLeft, "Prev",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "${monthNames[currentMonth]} $currentYear",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = {
                    if (currentMonth == 11) { currentMonth = 0; currentYear++ }
                    else currentMonth++
                }) {
                    Icon(
                        Icons.Default.ChevronRight, "Next",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                dayNames.forEach { day ->
                    Text(
                        day,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize  = 12.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val totalCells = firstDayOfWeek + daysInMonth
            val rows       = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val day       = cellIndex - firstDayOfWeek + 1

                            if (day < 1 || day > daysInMonth) {
                                Box(Modifier.weight(1f).height(40.dp))
                            } else {
                                val isBooked = checkIsDateBooked(day, currentMonth, currentYear, uiState.unavailableDates)
                                CalendarDay(
                                    day        = day,
                                    isBooked   = isBooked,
                                    isSelected = (day == selectedCheckIn || day == selectedCheckOut),
                                    isInRange  = selectedCheckIn != -1 && selectedCheckOut != -1
                                            && day > selectedCheckIn && day < selectedCheckOut,
                                    modifier   = Modifier.weight(1f),
                                    onClick    = {
                                        if (!isBooked) {
                                            if (selectedCheckIn == -1 || day <= selectedCheckIn) {
                                                selectedCheckIn  = day
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
                LegendItem(MaterialTheme.colorScheme.tertiary, "Available")
                LegendItem(MaterialTheme.colorScheme.error, "Booked")
                LegendItem(MaterialTheme.colorScheme.primary, "Selected")
            }

            Spacer(Modifier.weight(1f))

            // Selection Info & Action
            if (selectedCheckIn != -1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Stay Duration",
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (selectedCheckOut != -1)
                                    "$selectedCheckIn - $selectedCheckOut ${monthNames[currentMonth]}"
                                else
                                    "Starts $selectedCheckIn ${monthNames[currentMonth]}",
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (selectedCheckOut != -1) {
                            Button(
                                onClick = { navController.navigate(Screen.PreBooking.route) },
                                colors  = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
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
private fun CalendarDay(
    day       : Int,
    isBooked  : Boolean,
    isSelected: Boolean,
    isInRange : Boolean,
    modifier  : Modifier,
    onClick   : () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    val bgColor = when {
        isSelected -> primary
        isInRange  -> primary.copy(alpha = 0.1f)
        else       -> Color.Transparent
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isBooked   -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        else       -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = !isBooked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "$day",
            color      = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isBooked) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

private fun getDaysInMonth(month: Int, year: Int) =
    Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)

private fun getFirstDayOfWeek(month: Int, year: Int) =
    Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK) - 1

private fun checkIsDateBooked(day: Int, month: Int, year: Int, unavailableDates: List<Date>): Boolean {
    val cal = Calendar.getInstance()
    return unavailableDates.any {
        cal.time = it
        cal.get(Calendar.DAY_OF_MONTH) == day &&
                cal.get(Calendar.MONTH) == month &&
                cal.get(Calendar.YEAR) == year
    }
}

