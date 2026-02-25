package com.example.havenhub.repository
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.havenhub.data.model.Booking
import com.havenhub.data.remote.FirebaseDataManager
import com.havenhub.data.remote.FirebaseRealtimeListener
import com.havenhub.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BookingRepository
 *
 * Handles all rental booking operations for HavenHub.
 * Supports both one-time requests (suspend functions) and real-time
 * live updates (Flows via [FirebaseRealtimeListener]).
 *
 * Responsibilities:
 *  - Create new booking requests
 *  - Fetch bookings by tenant or by property
 *  - Update booking status (confirm, cancel, complete)
 *  - Listen to live booking changes for hosts and tenants
 */
@Singleton
class BookingRepository @Inject constructor(
    private val dataManager: FirebaseDataManager,
    private val realtimeListener: FirebaseRealtimeListener
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new booking request in Firestore.
     * Initial status is set to "pending" until the host confirms.
     *
     * @param booking The [Booking] data model to persist.
     * @return [Resource.Success] with the new booking ID, or [Resource.Error].
     */
    suspend fun createBooking(booking: Booking): Resource<String> {
        val pendingBooking = booking.copy(
            status    = "pending",
            createdAt = System.currentTimeMillis()
        )
        return dataManager.createBooking(pendingBooking)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all bookings made by a specific tenant (one-time fetch).
     *
     * @param userId The tenant's Firebase Auth UID.
     * @return [Resource.Success] with a list of [Booking], or [Resource.Error].
     */
    suspend fun getUserBookings(userId: String): Resource<List<Booking>> {
        return dataManager.getBookingsByUser(userId)
    }

    /**
     * Provides a real-time [Flow] of bookings for a specific tenant.
     * Emits updates whenever any of the user's bookings change.
     *
     * @param userId The tenant's Firebase Auth UID.
     * @return [Flow] of [List<Booking>] that auto-updates on changes.
     */
    fun observeUserBookings(userId: String): Flow<List<Booking>> {
        return realtimeListener.listenToUserBookings(userId)
    }

    /**
     * Provides a real-time [Flow] of all bookings for a specific property.
     * Used by hosts to monitor incoming and active booking requests.
     *
     * @param propertyId The property's Firestore document ID.
     * @return [Flow] of [List<Booking>] that auto-updates on changes.
     */
    fun observePropertyBookings(propertyId: String): Flow<List<Booking>> {
        return realtimeListener.listenToPropertyBookings(propertyId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the status of a booking.
     * Valid status transitions:
     *  pending → confirmed | cancelled
     *  confirmed → completed | cancelled
     *
     * @param bookingId The booking's Firestore document ID.
     * @param status    New status: "confirmed", "cancelled", or "completed".
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun updateBookingStatus(bookingId: String, status: String): Resource<Unit> {
        return dataManager.updateBookingStatus(bookingId, status)
    }
}


