package com.example.havenhub.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Dummy Data Model ───────────────────────────────────────────────
data class BookingDetail(
    val bookingId: String,
    val propertyName: String,
    val propertyAddress: String,
    val checkIn: String,
    val checkOut: String,
    val guests: Int,
    val nights: Int,
    val pricePerNight: Double,
    val totalAmount: Double,
    val status: String,
    val hostName: String,
    val hostPhone: String,
    val paymentMethod: String,
    val bookingDate: String
)

val dummyBookingDetail = BookingDetail(
    bookingId = "BK-20241105-0042",
    propertyName = "Luxury Sea View Apartment",
    propertyAddress = "Block 4, Clifton, Karachi",
    checkIn = "Nov 10, 2024",
    checkOut = "Nov 14, 2024",
    guests = 2,
    nights = 4,
    pricePerNight = 8500.0,
    totalAmount = 34000.0,
    status = "Confirmed",
    hostName = "Ali Raza",
    hostPhone = "+92 300 1234567",
    paymentMethod = "JazzCash",
    bookingDate = "Nov 5, 2024"
)

// ─── Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    onBack: () -> Unit = {},
    onCancelBooking: () -> Unit = {},
    onContactHost: () -> Unit = {}
) {
    val booking = dummyBookingDetail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusBadge(status = booking.status)

            SectionCard(title = "Property") {
                InfoRow(label = "Name", value = booking.propertyName)
                InfoRow(label = "Address", value = booking.propertyAddress)
            }

            SectionCard(title = "Stay Details") {
                InfoRow(label = "Check-In", value = booking.checkIn)
                InfoRow(label = "Check-Out", value = booking.checkOut)
                InfoRow(label = "Guests", value = "${booking.guests} Guest(s)")
                InfoRow(label = "Duration", value = "${booking.nights} Night(s)")
            }

            SectionCard(title = "Payment Summary") {
                InfoRow(label = "Price/Night", value = "Rs. ${booking.pricePerNight.toInt()}")
                InfoRow(label = "Nights", value = "${booking.nights}")
                InfoRow(label = "Payment Method", value = booking.paymentMethod)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(
                    label = "Total Amount",
                    value = "Rs. ${booking.totalAmount.toInt()}",
                    valueColor = MaterialTheme.colorScheme.primary,
                    bold = true
                )
            }

            SectionCard(title = "Host Information") {
                InfoRow(label = "Host", value = booking.hostName)
                InfoRow(label = "Phone", value = booking.hostPhone)
            }

            SectionCard(title = "Booking Info") {
                InfoRow(label = "Booking ID", value = booking.bookingId)
                InfoRow(label = "Booked On", value = booking.bookingDate)
            }

            if (booking.status != "Cancelled") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onContactHost,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Contact Host")
                    }
                    Button(
                        onClick = onCancelBooking,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancel")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Shared Components (move to a common UI package if needed) ───────

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Confirmed" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "Pending"   -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        "Cancelled" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else        -> Color(0xFFF5F5F5) to Color.Gray
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (status) {
                "Confirmed" -> Icons.Default.CheckCircle
                "Cancelled" -> Icons.Default.Cancel
                else        -> Icons.Default.HourglassEmpty
            },
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = "Status: $status", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(
            text = value,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}