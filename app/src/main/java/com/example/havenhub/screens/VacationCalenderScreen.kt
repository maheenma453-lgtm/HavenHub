package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.VacationViewModel

// ─────────────────────────────────────────────────────────────────
// VacationCalendarScreen.kt
// PURPOSE : Interactive availability calendar for vacation properties.
//           Shows which dates are booked (red), available (green),
//           or partially available (yellow).
//           User taps on an available date range to start booking.
// NAVIGATION: VacationCalendarScreen → PreBookingScreen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationCalendarScreen(
    navController : NavController,
    propertyId    : String = "",
    viewModel     : VacationViewModel = hiltViewModel()
) {

    LaunchedEffect(propertyId) {
        if (propertyId.isNotEmpty()) viewModel.loadCalendar(propertyId)
    }

    // ── Calendar State ─────────────────────────────────────────────
    // Tracks which month is currently displayed
    var currentMonth    by remember { mutableStateOf(3) }    // April (0-indexed March = 3 in display)
    var currentYear     by remember { mutableStateOf(2025) }
    var selectedCheckIn  by remember { mutableStateOf(-1) }  // Selected start day
    var selectedCheckOut by remember { mutableStateOf(-1) }  // Selected end day

    val bookedDates     by viewModel.bookedDates.collectAsState()

    val monthNames = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )

    val daysInMonth = getDaysInMonth(currentMonth, currentYear)
    val firstDayOfWeek = getFirstDayOfWeek(currentMonth, currentYear)  // 0=Sun .. 6=Sat
    val dayNames = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

    // ── UI ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Availability Calendar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = BackgroundWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = PrimaryBlue,
                    titleContentColor = BackgroundWhite,
                    navigationIconContentColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // ── Month Navigation Header ────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Previous month arrow
                IconButton(
                    onClick = {
                        if (currentMonth == 0) { currentMonth = 11; currentYear-- }
                        else currentMonth--
                    }
                ) {
                    Icon(Icons.Default.ChevronLeft, "Previous", tint = PrimaryBlue)
                }

                // Month & Year label
                Text(
                    text       = "${monthNames[currentMonth]} $currentYear",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )

                // Next month arrow
                IconButton(
                    onClick = {
                        if (currentMonth == 11) { currentMonth = 0; currentYear++ }
                        else currentMonth++
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, "Next", tint = PrimaryBlue)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Day name headers ───────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                dayNames.forEach { day ->
                    Text(
                        text      = day,
                        modifier  = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize  = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color     = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Calendar Grid ──────────────────────────────────────
            // Total cells = leading empty slots + days in month
            val totalCells = firstDayOfWeek + daysInMonth

            // Build grid rows
            val rows = (totalCells + 6) / 7   // ceil division

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - firstDayOfWeek + 1

                            if (day < 1 || day > daysInMonth) {
                                // Empty cell
                                Box(modifier = Modifier.weight(1f).height(40.dp))
                            } else {
                                val isBooked   = day in bookedDates
                                val isCheckIn  = day == selectedCheckIn
                                val isCheckOut = day == selectedCheckOut
                                val inRange    = selectedCheckIn != -1 && selectedCheckOut != -1
                                        && day > selectedCheckIn && day < selectedCheckOut

                                CalendarDay(
                                    day       = day,
                                    isBooked  = isBooked,
                                    isSelected = isCheckIn || isCheckOut,
                                    isInRange  = inRange,
                                    modifier   = Modifier.weight(1f),
                                    onClick    = {
                                        if (!isBooked) {
                                            // First tap = check-in, second tap = check-out
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

            Spacer(modifier = Modifier.height(20.dp))

            // ── Legend ─────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                LegendItem(color = SuccessGreen, label = "Available")
                LegendItem(color = ErrorRed,     label = "Booked")
                LegendItem(color = PrimaryBlue,  label = "Selected")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Selected range summary ─────────────────────────────
            if (selectedCheckIn != -1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = SurfaceGray)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Selected Dates", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = if (selectedCheckOut != -1)
                                    "$currentMonth/$selectedCheckIn → $currentMonth/$selectedCheckOut"
                                else
                                    "Check-in: $currentMonth/$selectedCheckIn",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary
                            )
                        }
                        if (selectedCheckOut != -1) {
                            Button(
                                onClick = { navController.navigate(Screen.PreBooking.route) },
                                shape   = RoundedCornerShape(8.dp),
                                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Book", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Single calendar day cell ──────────────────────────────────────
@Composable
private fun CalendarDay(
    day        : Int,
    isBooked   : Boolean,
    isSelected : Boolean,
    isInRange  : Boolean,
    modifier   : Modifier,
    onClick    : () -> Unit
) {
    val bgColor = when {
        isSelected -> PrimaryBlue
        isInRange  -> PrimaryBlue.copy(alpha = 0.15f)
        isBooked   -> ErrorRed.copy(alpha = 0.15f)
        else       -> BackgroundWhite
    }
    val textColor = when {
        isSelected -> BackgroundWhite
        isBooked   -> ErrorRed
        else       -> TextPrimary
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = !isBooked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$day", fontSize = 13.sp, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ── Calendar legend item ───────────────────────────────────────────
@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
    }
}

// ── Helper: days in month ──────────────────────────────────────────
private fun getDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        1 -> if (year % 4 == 0) 29 else 28   // February
        3, 5, 8, 10 -> 30                      // Apr, Jun, Sep, Nov
        else -> 31
    }
}

// ── Helper: first day of week (0=Sun) ─────────────────────────────
private fun getFirstDayOfWeek(month: Int, year: Int): Int {
    val cal = java.util.Calendar.getInstance()
    cal.set(year, month, 1)
    return cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
}


