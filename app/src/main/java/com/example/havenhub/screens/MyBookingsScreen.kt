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
    navController : NavController,
    userId        : String,
    viewModel     : BookingViewModel = hiltViewModel(),
    authViewModel : AuthViewModel    = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val userRole    = authUiState.userRole

    LaunchedEffect(userId) {
        viewModel.loadBookings(userId = userId, role = userRole.ifEmpty { "tenant" })
    }

    val uiState     by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Confirmed", "Checked In", "Completed", "Cancelled")

    val filteredBookings = uiState.bookings.filter { booking ->
        when (selectedTab) {
            0 -> booking.status == BookingStatus.PENDING.name
            1 -> booking.status == BookingStatus.CONFIRMED.name
            2 -> booking.status == BookingStatus.CHECKED_IN.name
            3 -> booking.status == BookingStatus.COMPLETED.name
            4 -> booking.status == BookingStatus.CANCELLED.name
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
                title = { Text("My Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = BackgroundWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = PrimaryBlue,
                    titleContentColor          = BackgroundWhite,
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
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = BackgroundWhite,
                contentColor     = PrimaryBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text     = {
                            Text(
                                text     = title,
                                color    = if (selectedTab == index) PrimaryBlue else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                filteredBookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "No ${tabs[selectedTab]} Bookings", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = filteredBookings, key = { it.bookingId }) { booking ->
                            BookingCard(
                                booking  = booking,
                                onTap    = {
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(booking.bookingId)
                                    )
                                },
                                // ✅ Pay Now — payment screen pe navigate karo
                                onPayNow = {
                                    navController.navigate(
                                        Screen.Payment.createRoute(
                                            bookingId = booking.bookingId,
                                            payerId   = booking.tenantId,
                                            payeeId   = booking.landlordId,
                                            payerName = booking.tenantName,
                                            payeeName = booking.landlordName,
                                            amount    = booking.totalAmount
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
        BookingStatus.PENDING    -> Pair(MaterialTheme.colorScheme.tertiary, "Pending")
        BookingStatus.CONFIRMED  -> Pair(SuccessGreen,  "Confirmed")
        BookingStatus.CHECKED_IN -> Pair(PrimaryBlue,   "Checked In")
        BookingStatus.COMPLETED  -> Pair(TextSecondary, "Completed")
        BookingStatus.CANCELLED  -> Pair(ErrorRed,      "Cancelled")
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onTap() },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = booking.propertyTitle,
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp
            )
            Text(
                text     = booking.propertyAddress,
                color    = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text     = statusText,
                    color    = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text       = booking.formattedTotal,
                    color      = PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }

            // ✅ Sirf PENDING booking pe Pay Now button dikhe
            if (bookingStatus == BookingStatus.PENDING) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick  = onPayNow,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Pay Now", fontSize = 13.sp)
                }
            }
        }
    }
}