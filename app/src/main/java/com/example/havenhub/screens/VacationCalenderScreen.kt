package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

    val monthNames  = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )
    val dayNames    = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
    val daysInMonth = getDaysInMonth(currentMonth, currentYear)
    val firstDayOfWeek = getFirstDayOfWeek(currentMonth, currentYear)

    val primary          = MaterialTheme.colorScheme.primary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor       = MaterialTheme.colorScheme.error

    // Computed values for display
    val nightsSelected = if (selectedCheckIn != -1 && selectedCheckOut != -1)
        selectedCheckOut - selectedCheckIn else 0

    Scaffold(
        topBar = {
            // Premium gradient top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(primary, primary.copy(alpha = 0.88f))
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Frosted back button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.15f))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = onPrimary,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Availability Calendar",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 19.sp,
                            color      = onPrimary
                        )
                        Text(
                            "Select your stay dates",
                            fontSize = 11.sp,
                            color    = onPrimary.copy(0.60f)
                        )
                    }
                    // Month/Year badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(tertiary.copy(0.18f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${monthNames[currentMonth].take(3)} $currentYear",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = tertiary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .background(background)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Month Navigation ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // Month nav row
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            // Prev month button
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(primary.copy(0.08f))
                                    .clickable {
                                        if (currentMonth == 0) {
                                            currentMonth = 11; currentYear--
                                        } else currentMonth--
                                        selectedCheckIn  = -1
                                        selectedCheckOut = -1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    "Prev",
                                    tint     = primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    monthNames[currentMonth],
                                    fontSize   = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color      = onSurface
                                )
                                Text(
                                    "$currentYear",
                                    fontSize = 12.sp,
                                    color    = onSurfaceVariant
                                )
                            }

                            // Next month button
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(primary.copy(0.08f))
                                    .clickable {
                                        if (currentMonth == 11) {
                                            currentMonth = 0; currentYear++
                                        } else currentMonth++
                                        selectedCheckIn  = -1
                                        selectedCheckOut = -1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    "Next",
                                    tint     = primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Day name headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            dayNames.forEach { day ->
                                Text(
                                    day,
                                    modifier   = Modifier.weight(1f),
                                    textAlign  = TextAlign.Center,
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (day == "Sun") errorColor.copy(0.7f) else onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Calendar grid
                        val totalCells = firstDayOfWeek + daysInMonth
                        val rows       = (totalCells + 6) / 7

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (r in 0 until rows) {
                                Row(Modifier.fillMaxWidth()) {
                                    for (c in 0 until 7) {
                                        val cellIndex = r * 7 + c
                                        val day       = cellIndex - firstDayOfWeek + 1

                                        if (day < 1 || day > daysInMonth) {
                                            Box(Modifier.weight(1f).height(44.dp))
                                        } else {
                                            val isBooked = checkIsDateBooked(
                                                day, currentMonth, currentYear, uiState.unavailableDates
                                            )
                                            val isCheckInDay  = day == selectedCheckIn
                                            val isCheckOutDay = day == selectedCheckOut
                                            val isInRange     = selectedCheckIn != -1 &&
                                                    selectedCheckOut != -1 &&
                                                    day > selectedCheckIn &&
                                                    day < selectedCheckOut

                                            // Today highlight
                                            val todayCal = Calendar.getInstance()
                                            val isToday  = todayCal.get(Calendar.DAY_OF_MONTH) == day &&
                                                    todayCal.get(Calendar.MONTH) == currentMonth &&
                                                    todayCal.get(Calendar.YEAR) == currentYear

                                            CalendarDay(
                                                day        = day,
                                                isBooked   = isBooked,
                                                isSelected = isCheckInDay || isCheckOutDay,
                                                isCheckIn  = isCheckInDay,
                                                isCheckOut = isCheckOutDay,
                                                isInRange  = isInRange,
                                                isToday    = isToday,
                                                isSunday   = c == 0,
                                                modifier   = Modifier.weight(1f),
                                                primary    = primary,
                                                tertiary   = tertiary,
                                                onPrimary  = onPrimary,
                                                onSurface  = onSurface,
                                                errorColor = errorColor,
                                                background = background,
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
                    }
                }
            }

            // ── Legend ───────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(3.dp, RoundedCornerShape(16.dp)),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        PremiumLegendItem(tertiary,   "Available",   onSurfaceVariant)
                        // Vertical divider
                        Box(Modifier.width(1.dp).height(28.dp).background(background))
                        PremiumLegendItem(errorColor, "Booked",      onSurfaceVariant)
                        Box(Modifier.width(1.dp).height(28.dp).background(background))
                        PremiumLegendItem(primary,    "Selected",    onSurfaceVariant)
                        Box(Modifier.width(1.dp).height(28.dp).background(background))
                        PremiumLegendItem(primary.copy(0.20f), "Range", onSurfaceVariant)
                    }
                }
            }

            // ── Selection summary card ────────────────────────────────────────
            if (selectedCheckIn != -1) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape  = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            // Header row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier         = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(tertiary.copy(0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        null,
                                        tint     = tertiary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Stay Summary",
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = onSurface
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Check-in / Check-out tiles
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Check-in tile
                                Column(
                                    modifier            = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(primary.copy(0.07f))
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🛬", fontSize = 20.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Check-in",
                                        fontSize = 10.sp,
                                        color    = onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "$selectedCheckIn\n${monthNames[currentMonth].take(3)}",
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color      = primary,
                                        textAlign  = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }

                                // Check-out tile (or placeholder)
                                Column(
                                    modifier            = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selectedCheckOut != -1) tertiary.copy(0.10f)
                                            else background
                                        )
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🛫", fontSize = 20.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Check-out",
                                        fontSize = 10.sp,
                                        color    = onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    if (selectedCheckOut != -1) {
                                        Text(
                                            "$selectedCheckOut\n${monthNames[currentMonth].take(3)}",
                                            fontSize   = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color      = Color(0xFFB8860B),
                                            textAlign  = TextAlign.Center,
                                            lineHeight = 20.sp
                                        )
                                    } else {
                                        Text(
                                            "Select\ndate",
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = onSurfaceVariant.copy(0.5f),
                                            textAlign  = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            // Nights count if both selected
                            if (selectedCheckOut != -1 && nightsSelected > 0) {
                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(background)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌙", fontSize = 18.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "$nightsSelected night${if (nightsSelected > 1) "s" else ""} selected",
                                        fontSize  = 13.sp,
                                        color     = onSurface,
                                        modifier  = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tertiary.copy(0.12f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            "$nightsSelected nights",
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = Color(0xFFB8860B)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Continue button
                                Button(
                                    onClick   = { navController.navigate(Screen.PreBooking.route) },
                                    modifier  = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .shadow(8.dp, RoundedCornerShape(14.dp)),
                                    shape     = RoundedCornerShape(14.dp),
                                    colors    = ButtonDefaults.buttonColors(
                                        containerColor = primary,
                                        contentColor   = onPrimary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                ) {
                                    Text(
                                        "Continue with $nightsSelected nights →",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 14.sp
                                    )
                                }
                            }

                            // Hint if only check-in selected
                            if (selectedCheckOut == -1) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(primary.copy(0.05f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        null,
                                        tint     = primary.copy(0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Now tap a check-out date on the calendar",
                                        fontSize = 12.sp,
                                        color    = onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Premium Calendar Day cell ─────────────────────────────────────────────────
@Composable
private fun CalendarDay(
    day       : Int,
    isBooked  : Boolean,
    isSelected: Boolean,
    isCheckIn : Boolean,
    isCheckOut: Boolean,
    isInRange : Boolean,
    isToday   : Boolean,
    isSunday  : Boolean,
    modifier  : Modifier,
    primary   : Color,
    tertiary  : Color,
    onPrimary : Color,
    onSurface : Color,
    errorColor: Color,
    background: Color,
    onClick   : () -> Unit
) {
    val bgColor = when {
        isCheckIn || isCheckOut -> primary
        isInRange               -> primary.copy(alpha = 0.12f)
        isBooked                -> errorColor.copy(alpha = 0.08f)
        else                    -> Color.Transparent
    }

    val textColor = when {
        isCheckIn || isCheckOut -> onPrimary
        isBooked                -> errorColor.copy(alpha = 0.45f)
        isSunday                -> errorColor.copy(alpha = 0.75f)
        else                    -> onSurface
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .padding(horizontal = 1.dp)
            .clip(
                when {
                    isCheckIn  -> RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp)
                    isCheckOut -> RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp)
                    isInRange  -> RoundedCornerShape(0.dp)
                    else       -> CircleShape
                }
            )
            .background(bgColor)
            .clickable(enabled = !isBooked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = "$day",
                color      = textColor,
                fontSize   = 13.sp,
                fontWeight = when {
                    isSelected -> FontWeight.ExtraBold
                    isToday    -> FontWeight.Bold
                    else       -> FontWeight.Normal
                }
            )
            // Today dot
            if (isToday && !isSelected) {
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(primary)
                )
            }
            // Booked dot
            if (isBooked) {
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(errorColor.copy(0.7f))
                )
            }
        }
    }
}

// ── Premium Legend item ───────────────────────────────────────────────────────
@Composable
private fun PremiumLegendItem(color: Color, label: String, textColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            fontSize = 10.sp,
            color    = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Pure utility functions — logic unchanged ──────────────────────────────────

private fun getDaysInMonth(month: Int, year: Int) =
    Calendar.getInstance().apply { set(year, month, 1) }
        .getActualMaximum(Calendar.DAY_OF_MONTH)

private fun getFirstDayOfWeek(month: Int, year: Int) =
    Calendar.getInstance().apply { set(year, month, 1) }
        .get(Calendar.DAY_OF_WEEK) - 1

private fun checkIsDateBooked(
    day              : Int,
    month            : Int,
    year             : Int,
    unavailableDates : List<Date>
): Boolean {
    val cal = Calendar.getInstance()
    return unavailableDates.any {
        cal.time = it
        cal.get(Calendar.DAY_OF_MONTH) == day &&
                cal.get(Calendar.MONTH)       == month &&
                cal.get(Calendar.YEAR)        == year
    }
}
