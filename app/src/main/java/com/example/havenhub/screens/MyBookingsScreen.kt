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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController: NavController,
    userId: String,
    viewModel: BookingViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val userRole = authUiState.userRole

    LaunchedEffect(userId) {
        viewModel.loadBookings(
            userId = userId,
            role = userRole.ifEmpty { "tenant" }
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Confirmed", "Checked In", "Completed", "Cancelled")

    val filteredBookings = uiState.bookings.filter { booking ->
        when (selectedTab) {
            0 -> booking.bookingStatus == BookingStatus.PENDING
            1 -> booking.bookingStatus == BookingStatus.CONFIRMED
            2 -> booking.bookingStatus == BookingStatus.CHECKED_IN
            3 -> booking.bookingStatus == BookingStatus.COMPLETED
            4 -> booking.bookingStatus == BookingStatus.CANCELLED
            else -> true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Bookings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B3E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(paddingValues)
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF0D1B3E)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) Color(0xFF0D1B3E) else Color(0xFF8899AA),
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFD4AF37))
                    }
                }
                filteredBookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📋",
                                fontSize = 48.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No ${tabs[selectedTab]} Bookings",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color(0xFF0D1B3E)
                            )
                            Text(
                                text = "Your ${tabs[selectedTab].lowercase()} bookings will appear here",
                                color = Color(0xFF8899AA),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = filteredBookings, key = { it.bookingId }) { booking ->
                            BookingCard(
                                booking = booking,
                                onTap = {
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(booking.bookingId)
                                    )
                                },
                                onPayNow = {
                                    navController.navigate(
                                        Screen.Payment.createRoute(
                                            bookingId = booking.bookingId,
                                            payerId = booking.tenantId,
                                            payeeId = booking.landlordId,
                                            payerName = booking.tenantName,
                                            payeeName = booking.landlordName,
                                            amount = booking.totalAmount
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: Booking, onTap: () -> Unit, onPayNow: () -> Unit) {
    val bookingStatus = booking.bookingStatus
    val (statusColor, statusText) = when (bookingStatus) {
        BookingStatus.PENDING    -> Pair(Color(0xFFF59E0B), "Pending")
        BookingStatus.CONFIRMED  -> Pair(Color(0xFF10B981), "Confirmed")
        BookingStatus.CHECKED_IN -> Pair(Color(0xFF3B82F6), "Checked In")
        BookingStatus.COMPLETED  -> Pair(Color(0xFF6B7280), "Completed")
        BookingStatus.CANCELLED  -> Pair(Color(0xFFEF4444), "Cancelled")
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onTap() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.propertyTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0D1B3E),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = booking.propertyAddress,
                color = Color(0xFF8899AA),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Nights", color = Color(0xFF8899AA), fontSize = 11.sp)
                    Text("${booking.totalNights}", fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                }
                Column {
                    Text("Guests", color = Color(0xFF8899AA), fontSize = 11.sp)
                    Text("${booking.guestCount}", fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", color = Color(0xFF8899AA), fontSize = 11.sp)
                    Text(
                        booking.formattedTotal,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD4AF37),
                        fontSize = 15.sp
                    )
                }
            }

            if (bookingStatus == BookingStatus.PENDING) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPayNow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D1B3E)
                    )
                ) {
                    Text("Pay Now", fontSize = 14.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}