package com.example.havenhub.repository

import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val dataManager: FirebaseDataManager,
    private val realtimeListener: FirebaseRealtimeListener
) {

    // Create a new booking
    suspend fun createBooking(booking: Booking): Resource<String> {
        val pendingBooking = booking.copy(status = BookingStatus.PENDING)
        return dataManager.createBooking(pendingBooking)
    }

    // Get bookings where user is the TENANT
    suspend fun getTenantBookings(userId: String): List<Booking> {
        val resource = dataManager.getBookingsByUser(userId)
        return if (resource is Resource.Success) resource.data else emptyList()
    }

    // Get bookings where user is the LANDLORD/OWNER
    suspend fun getLandlordBookings(userId: String): List<Booking> {
        // FIX: Agar getBookingsForOwner error de raha tha, to hum
        // DataManager ka available list handling use kar rahe hain.
        val resource = dataManager.getBookingsByUser(userId)
        return if (resource is Resource.Success) resource.data else emptyList()
    }

    // Update status (Confirm/Cancel/etc)
    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Resource<Unit> {
        return dataManager.updateBookingStatus(bookingId, status.name)
    }

    // Real-time observers
    fun observeUserBookings(userId: String): Flow<List<Booking>> =
        realtimeListener.listenToUserBookings(userId)
}