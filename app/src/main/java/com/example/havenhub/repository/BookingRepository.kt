package com.example.havenhub.repository

import android.util.Log
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.NotificationType
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.remote.FirebaseRealtimeListener
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val dataManager           : FirebaseDataManager,
    private val realtimeListener      : FirebaseRealtimeListener,
    private val firestore             : FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) {
    // ✅ FIXED: Sirf ek "bookings" collection — space wali hatdi
    private val bookingsCol = firestore.collection("bookings")
    private val usersCol = firestore.collection("users")
    private val notificationsCol = firestore.collection("notifications")
    private val propertiesCol = firestore.collection("properties")

    // ── Helper: safe parse ────────────────────────────────────────────────────
    private fun parseBookingSafe(doc: com.google.firebase.firestore.DocumentSnapshot): Booking? {
        return try {
            var b = doc.toObject(Booking::class.java) ?: return null
            if (b.bookingId.isBlank()) b = b.copy(bookingId = doc.id)
            b
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "Parse fail ${doc.id}: ${e.localizedMessage}")
            null
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    suspend fun createBooking(booking: Booking): Resource<String> {
        Log.d("BOOKING_REPO", "createBooking START tenantId='${booking.tenantId}'")

        val resolvedTenantName = resolveTenantName(booking)
        val pendingBooking = booking.copy(
            status = BookingStatus.PENDING.name,
            tenantName = resolvedTenantName
        )

        val result = dataManager.createBooking(pendingBooking)
        Log.d("BOOKING_REPO", "createBooking result = $result")

        if (result is Resource.Success) {
            val bookingId = result.data ?: ""
            val resolvedLandlordId = resolveLandlordId(booking)
            val finalBooking = pendingBooking.copy(
                bookingId = bookingId,
                landlordId = resolvedLandlordId
            )
            sendBookingNotificationToLandlord(finalBooking)
            sendBookingNotificationToAdmin(finalBooking)
        }
        return result
    }

    // ── READ ──────────────────────────────────────────────────────────────────
    suspend fun getBookingById(bookingId: String): Resource<Booking> {
        Log.d("BOOKING_REPO", "getBookingById: '$bookingId'")
        return dataManager.getBookingById(bookingId)
    }

    suspend fun getAllBookingsForAdmin(): List<Booking> {
        Log.d("BOOKING_REPO", "getAllBookingsForAdmin START")
        return try {
            val snap = bookingsCol.get().await()
            val all = snap.documents
                .mapNotNull { parseBookingSafe(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Log.d("BOOKING_REPO", "getAllBookingsForAdmin: ${all.size} total")
            all
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "getAllBookingsForAdmin EXCEPTION: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun getTenantBookings(tenantId: String): List<Booking> {
        Log.d("BOOKING_REPO", "getTenantBookings START — tenantId='$tenantId'")

        if (tenantId.isBlank()) {
            Log.e("BOOKING_REPO", "tenantId BLANK — aborting")
            return emptyList()
        }

        return try {
            val snap = bookingsCol
                .whereEqualTo("tenantId", tenantId)
                .get().await()

            Log.d("BOOKING_REPO", "snap size = ${snap.documents.size}")

            val all = snap.documents
                .mapNotNull { parseBookingSafe(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }

            Log.d("BOOKING_REPO", "getTenantBookings result: ${all.size} bookings")
            all

        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "getTenantBookings EXCEPTION: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun getLandlordBookings(landlordId: String): List<Booking> {
        Log.d("BOOKING_REPO", "getLandlordBookings START — landlordId='$landlordId'")

        if (landlordId.isBlank()) {
            Log.e("BOOKING_REPO", "landlordId BLANK — aborting")
            return emptyList()
        }

        return try {
            val snap = bookingsCol
                .whereEqualTo("landlordId", landlordId)
                .get().await()

            Log.d("BOOKING_REPO", "snap size = ${snap.documents.size}")

            val all = snap.documents
                .mapNotNull { parseBookingSafe(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }

            Log.d("BOOKING_REPO", "getLandlordBookings result: ${all.size} bookings")
            all

        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "getLandlordBookings EXCEPTION: ${e.localizedMessage}")
            emptyList()
        }
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────
    suspend fun updateBookingStatus(
        bookingId: String,
        newStatus: BookingStatus
    ): Resource<Unit> {
        Log.d("BOOKING_REPO", "updateBookingStatus: '$bookingId' → '${newStatus.name}'")

        return try {
            bookingsCol.document(bookingId).update("status", newStatus.name).await()

            // Notification bhejo
            try {
                val bookingResource = getBookingById(bookingId)
                if (bookingResource is Resource.Success) {
                    val booking = bookingResource.data
                    when (newStatus) {
                        BookingStatus.CONFIRMED ->
                            notificationRepository.sendBookingConfirmedToTenant(
                                tenantId = booking.tenantId,
                                bookingId = bookingId,
                                propertyTitle = booking.propertyTitle.ifBlank { "Property" }
                            )

                        BookingStatus.CANCELLED ->
                            notificationRepository.sendBookingCancelledToTenant(
                                tenantId = booking.tenantId,
                                bookingId = bookingId,
                                propertyTitle = booking.propertyTitle.ifBlank { "Property" }
                            )

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("BOOKING_REPO", "Status notification error: ${e.localizedMessage}")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "updateBookingStatus FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to update status")
        }
    }

    // ── PAYMENT UPDATE ────────────────────────────────────────────────────────
    suspend fun updatePaymentStatusOnBooking(
        bookingId: String,
        paymentStatus: String,
        paymentId: String
    ) {
        Log.d("BOOKING_REPO", "updatePaymentStatusOnBooking: '$bookingId'")
        val updateMap = mapOf(
            "paymentStatus" to paymentStatus,
            "paymentId" to paymentId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            bookingsCol.document(bookingId).update(updateMap).await()
            Log.d("BOOKING_REPO", "✅ Payment status updated on booking")
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "updatePaymentStatus error: ${e.localizedMessage}")
        }
    }

    // ── FLOW ──────────────────────────────────────────────────────────────────
    fun getBookingsFlow(userId: String): Flow<List<Booking>> {
        return realtimeListener.getBookingsFlow(userId)
    }

    // ── CANCEL ────────────────────────────────────────────────────────────────
    suspend fun cancelBooking(bookingId: String): Resource<Unit> {
        val updateMap = mapOf(
            "status" to BookingStatus.CANCELLED.name,
            "cancelledAt" to FieldValue.serverTimestamp()
        )
        return try {
            bookingsCol.document(bookingId).update(updateMap).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Cancel failed")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private suspend fun resolveTenantName(booking: Booking): String {
        if (booking.tenantName.isNotBlank()) return booking.tenantName
        if (booking.tenantId.isBlank()) return ""
        return try {
            val userDoc = usersCol.document(booking.tenantId).get().await()
            userDoc.getString("fullName")
                ?: userDoc.getString("name")
                ?: userDoc.getString("displayName")
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun resolveLandlordId(booking: Booking): String {
        if (booking.landlordId.isNotBlank()) return booking.landlordId
        if (booking.propertyId.isBlank()) return ""
        return try {
            val propDoc = propertiesCol.document(booking.propertyId).get().await()
            propDoc.getString("landlordId")
                ?: propDoc.getString("ownerId")
                ?: propDoc.getString("userId")
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun sendBookingNotificationToLandlord(booking: Booking) {
        try {
            if (booking.landlordId.isBlank()) return
            notificationRepository.sendBookingRequestToLandlord(
                landlordId = booking.landlordId,
                bookingId = booking.bookingId,
                propertyTitle = booking.propertyTitle.ifBlank { "Property" },
                tenantName = booking.tenantName.ifBlank { "Tenant" }
            )
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "landlord notification error: ${e.localizedMessage}")
        }
    }

    private suspend fun sendBookingNotificationToAdmin(booking: Booking) {
        try {
            var adminQuery = usersCol.whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = usersCol.whereEqualTo("role", "admin").get().await()
            }

            if (adminQuery.isEmpty) {
                val notifData = mapOf(
                    "recipientId" to "admin",
                    "targetRole" to "admin",
                    "title" to "New Booking Request",
                    "body" to "${booking.tenantName.ifBlank { "A tenant" }} ne \"${booking.propertyTitle.ifBlank { "a property" }}\" book kiya.",
                    "type" to NotificationType.BOOKING_REQUESTED.name,
                    "referenceId" to booking.bookingId,
                    "isRead" to false,
                    "isActive" to true,
                    "createdAt" to Timestamp.now()
                )
                notificationsCol.add(notifData).await()
                return
            }

            adminQuery.documents.forEach { adminDoc ->
                notificationRepository.sendBookingNotificationToAdmin(
                    adminId = adminDoc.id,
                    bookingId = booking.bookingId,
                    propertyTitle = booking.propertyTitle.ifBlank { "Property" },
                    tenantName = booking.tenantName.ifBlank { "Tenant" }
                )
            }
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "admin notification error: ${e.localizedMessage}")
        }
    }
}