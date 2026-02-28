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
        val pendingBooking = booking.copy(
            status = BookingStatus.PENDING
        )
        return dataManager.createBooking(pendingBooking)
    }

    suspend fun getUserBookings(userId: String): Resource<List<Booking>> {
        return dataManager.getBookingsByUser(userId)
    }

    fun observeUserBookings(userId: String): Flow<List<Booking>> {
        return realtimeListener.listenToUserBookings(userId)
    }

    fun observePropertyBookings(propertyId: String): Flow<List<Booking>> {
        return realtimeListener.listenToPropertyBookings(propertyId)
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Resource<Unit> {
        return dataManager.updateBookingStatus(bookingId, status.name)
    }
}