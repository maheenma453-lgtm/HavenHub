package com.example.havenhub.repository

import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.NotificationType
import com.example.havenhub.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val dataManager      : FirebaseDataManager,
    private val realtimeListener : FirebaseRealtimeListener,
    private val firestore        : FirebaseFirestore        // ← add kiya
) {
    suspend fun createBooking(booking: Booking): Resource<String> {
        val pendingBooking = booking.copy(status = BookingStatus.PENDING.name)
        val result = dataManager.createBooking(pendingBooking)
        if (result is Resource.Success) {
            sendNotificationToAdmin(
                pendingBooking.copy(bookingId = result.data ?: "")
            )
        }
        return result
    }

    suspend fun getBookingById(bookingId: String): Resource<Booking> {
        return dataManager.getBookingById(bookingId)
    }

    suspend fun getAllBookingsForAdmin(): List<Booking> {
        return try {
            val resource = dataManager.getAllBookings()
            if (resource is Resource.Success) resource.data ?: emptyList()
            else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTenantBookings(tenantId: String): List<Booking> {
        return try {
            dataManager.getBookingsByTenantId(tenantId)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getLandlordBookings(landlordId: String): List<Booking> {
        return try {
            dataManager.getBookingsByLandlordId(landlordId)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun updateBookingStatus(
        bookingId : String,
        newStatus : BookingStatus
    ): Resource<Unit> {
        return dataManager.updateBookingStatus(bookingId, newStatus.name)
    }

    // ── NEW: payment hone ke baad booking mein paymentStatus + paymentId update karo ──
    suspend fun updatePaymentStatusOnBooking(
        bookingId    : String,
        paymentStatus: String,
        paymentId    : String
    ) {
        try {
            firestore.collection("bookings")
                .document(bookingId)
                .update(
                    mapOf(
                        "paymentStatus" to paymentStatus,
                        "paymentId"     to paymentId
                    )
                )
                .await()
        } catch (e: Exception) {
            // silently fail — booking status already updated
        }
    }

    fun getBookingsFlow(userId: String): Flow<List<Booking>> {
        return realtimeListener.getBookingsFlow(userId)
    }

    private suspend fun sendNotificationToAdmin(booking: Booking) {
        val notificationData = mapOf(
            "title"       to "New Booking Request",
            "body"        to "New request for ${booking.propertyTitle} by ${booking.tenantName}",
            "type"        to NotificationType.BOOKING_REQUESTED.name,
            "referenceId" to booking.bookingId,
            "targetRole"  to "admin",
            "isRead"      to false,
            "isActive"    to true
        )
        dataManager.sendNotification(notificationData)
    }
}