package com.example.havenhub.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.havenhub.data.PaymentStatus
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.AuthViewModel
import com.example.havenhub.viewmodel.BookingViewModel
import com.example.havenhub.viewmodel.PropertyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController     : NavController,
    propertyId        : String,
    viewModel         : BookingViewModel  = hiltViewModel(),
    propertyViewModel : PropertyViewModel = hiltViewModel(),
    authViewModel     : AuthViewModel     = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val propUiState by propertyViewModel.uiState.collectAsState()

    val currentUid  = authUiState.currentUser?.uid         ?: ""
    val currentName = authUiState.currentUser?.displayName ?: ""

    // ✅ Property load karo
    LaunchedEffect(propertyId) {
        propertyViewModel.loadPropertyDetail(propertyId)
    }

    val property = propUiState.propertyDetail

    // ✅ Booking success hone par confirmation screen pe jao
    LaunchedEffect(uiState.actionSuccess, uiState.createdBookingId) {
        if (uiState.actionSuccess && !uiState.createdBookingId.isNullOrEmpty()) {
            navController.navigate(
                Screen.BookingConfirmation.createRoute(uiState.createdBookingId!!)
            ) {
                popUpTo(Screen.Booking.route) { inclusive = true }
            }
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Booking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = PrimaryBlue,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                // ✅ Loading
                propUiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(32.dp),
                        color    = PrimaryBlue
                    )
                }

                // ✅ Property nahi mili
                property == null -> {
                    Card(
                        colors   = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text     = "Property load nahi ho rahi. Wapas jao aur dobara try karo.",
                            color    = Color(0xFFB71C1C),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // ✅ Property approved nahi
                property.status != "APPROVED" -> {
                    Card(
                        colors   = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text     = "Yeh property abhi admin se approve nahi hui — booking nahi ho sakti.",
                            color    = Color(0xFFB71C1C),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // ✅ Sab theek — booking form dikhao
                else -> {

                    // ── Order Summary ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text       = "Order Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            SummaryRow("Property", property.title)
                            SummaryRow("Location", "${property.address}, ${property.city}")
                            SummaryRow("Type",     property.propertyType)
                            SummaryRow("Bedrooms", "${property.bedrooms}")

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            SummaryRow(
                                label = "Price per night",
                                value = property.formattedPrice,
                                bold  = true
                            )
                            SummaryRow(
                                label = "Status",
                                value = "Pending confirmation"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Tenant Info ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text       = "Tenant Info",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SummaryRow("Name",    currentName.ifEmpty { "N/A" })
                            SummaryRow("User ID", currentUid.take(12) + "...")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Confirm Button ──
                    Button(
                        onClick = {
                            val booking = Booking(
                                propertyId       = propertyId,
                                propertyTitle    = property.title,
                                landlordId       = property.ownerId,
                                landlordName     = property.ownerName,
                                tenantId         = currentUid,
                                tenantName       = currentName,
                                pricePerNight    = property.pricePerNight,
                                totalAmount      = property.pricePerNight,
                                status           = BookingStatus.PENDING.name,
                                paymentStatus    = PaymentStatus.PENDING.name,
                                propertyAddress  = "${property.address}, ${property.city}",
                                propertyCoverUrl = property.coverImageUrl
                            )
                            viewModel.createBooking(booking)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !uiState.isLoading,
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Confirm Booking",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ── Error Message ──
                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors   = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text     = error,
                                color    = Color(0xFFB71C1C),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text     = label,
            fontSize = 14.sp,
            color    = TextSecondary
        )
        Text(
            text       = value,
            fontSize   = 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color      = if (bold) PrimaryBlue else TextPrimary
        )
    }
}