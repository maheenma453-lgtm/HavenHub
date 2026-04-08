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
    suspend fun createBooking(booking: Booking): Resource<String> {
        // ✅ FIX — BookingStatus.PENDING.name (String hai Booking.status)
        val pendingBooking = booking.copy(status = BookingStatus.PENDING.name)
        return dataManager.createBooking(pendingBooking)
    }

    suspend fun getTenantBookings(userId: String): List<Booking> {
        val resource = dataManager.getBookingsByUser(userId)
        // ✅ FIX — type argument remove, simple is check kaafi hai
        return if (resource is Resource.Success) resource.data ?: emptyList() else emptyList()
    }

    suspend fun getLandlordBookings(userId: String): List<Booking> {
        val resource = dataManager.getBookingsByLandlord(userId)
        // ✅ FIX — same fix
        return if (resource is Resource.Success) resource.data ?: emptyList() else emptyList()
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Resource<Unit> {
        return dataManager.updateBookingStatus(bookingId, status.name)
    }

    fun observeUserBookings(userId: String): Flow<List<Booking>> =
        realtimeListener.listenToUserBookings(userId)
}