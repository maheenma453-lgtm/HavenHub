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
    private val bookingsCol      = firestore.collection("bookings")
    private val usersCol         = firestore.collection("users")
    private val notificationsCol = firestore.collection("notifications")
    private val propertiesCol    = firestore.collection("properties")

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

    // ══════════════════════════════════════════════════════════════════════════
    // isPropertyBooked — DATE-AWARE CHECK
    //
    // Returns true only if the property has an ACTIVE booking that has NOT
    // yet ended (i.e. checkOutDate is in the future or today).
    //
    // OLD behaviour (broken):
    //   - Only checked status == CONFIRMED or PENDING
    //   - Did NOT look at dates → a booking from last month still showed "Booked"
    //
    // NEW behaviour (fixed):
    //   Step 1: Fetch all CONFIRMED + PENDING bookings for this property
    //   Step 2: For each booking, check if checkOutDate > now
    //           - If yes  → property is still occupied  → return true
    //           - If no   → booking has expired, auto-mark it COMPLETED in background
    //   Step 3: If no active (non-expired) booking found → return false
    //           → property is available for new tenants
    //
    // Why auto-complete here?
    //   Calling markExpiredBookingsCompleted() inside this check means the
    //   cleanup happens silently every time a tenant opens PropertyDetailScreen
    //   — no cron job or Cloud Function needed.
    // ══════════════════════════════════════════════════════════════════════════
    suspend fun isPropertyBooked(propertyId: String): Boolean {
        if (propertyId.isBlank()) return false
        return try {
            val now = Timestamp.now()

            // Fetch bookings that are in an "active" status
            val snap = bookingsCol
                .whereEqualTo("propertyId", propertyId)
                .whereIn("status", listOf(
                    BookingStatus.CONFIRMED.name,
                    BookingStatus.PENDING.name,
                    BookingStatus.CHECKED_IN.name      // also block during active stay
                ))
                .get()
                .await()

            if (snap.isEmpty) {
                Log.d("BOOKING_REPO", "isPropertyBooked('$propertyId') = false (no active bookings)")
                return false
            }

            var hasActiveBooking = false

            for (doc in snap.documents) {
                val booking = parseBookingSafe(doc) ?: continue

                val checkOut = booking.checkOutDate

                // If checkOutDate is missing, treat as still active (safe default)
                if (checkOut == null) {
                    hasActiveBooking = true
                    Log.d("BOOKING_REPO", "Booking ${booking.bookingId} has no checkOutDate — treating as active")
                    continue
                }

                if (checkOut.toDate().after(now.toDate())) {
                    // Booking is still ongoing — property is occupied
                    hasActiveBooking = true
                    Log.d("BOOKING_REPO", "Booking ${booking.bookingId} is active until ${checkOut.toDate()}")
                } else {
                    // checkOutDate has passed — auto-complete this booking in background
                    // This frees the property for new customers automatically
                    Log.d("BOOKING_REPO", "Booking ${booking.bookingId} expired on ${checkOut.toDate()} — auto-completing")
                    autoCompleteExpiredBooking(booking)
                }
            }

            Log.d("BOOKING_REPO", "isPropertyBooked('$propertyId') = $hasActiveBooking")
            hasActiveBooking

        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "isPropertyBooked error: ${e.localizedMessage}")
            false
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // autoCompleteExpiredBooking — PRIVATE HELPER
    //
    // Called internally when a booking's checkOutDate has passed.
    // Marks the booking as COMPLETED so it no longer blocks the property.
    //
    // What it does:
    //   1. Updates booking status → COMPLETED in Firestore
    //   2. Sends a "stay completed" notification to the tenant
    //      (optional but good UX — tenant can now leave a review)
    //
    // This runs silently — any failure is logged but does NOT crash the UI.
    // ══════════════════════════════════════════════════════════════════════════
    private suspend fun autoCompleteExpiredBooking(booking: Booking) {
        try {
            // Only auto-complete CONFIRMED or CHECKED_IN bookings
            // Do NOT auto-complete PENDING — those haven't started yet
            if (booking.status != BookingStatus.CONFIRMED.name &&
                booking.status != BookingStatus.CHECKED_IN.name) {
                Log.d("BOOKING_REPO", "Skipping auto-complete for status=${booking.status}")
                return
            }

            bookingsCol.document(booking.bookingId).update(
                mapOf(
                    "status"    to BookingStatus.COMPLETED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Log.d("BOOKING_REPO", "✅ Auto-completed booking ${booking.bookingId} — property is now free")

            // Send notification to tenant so they can leave a review
            try {
                if (booking.tenantId.isNotBlank()) {
                    notificationRepository.sendBookingCompletedToTenant(
                        tenantId      = booking.tenantId,
                        bookingId     = booking.bookingId,
                        propertyTitle = booking.propertyTitle.ifBlank { "Property" }
                    )
                }
            } catch (notifEx: Exception) {
                // Notification failure should never block the main flow
                Log.e("BOOKING_REPO", "Auto-complete notification error: ${notifEx.localizedMessage}")
            }

        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "autoCompleteExpiredBooking error for ${booking.bookingId}: ${e.localizedMessage}")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // markExpiredBookingsCompleted — PUBLIC (call from ViewModel/WorkManager)
    //
    // Scans ALL active bookings across ALL properties and completes expired ones.
    // Can be called:
    //   - On app launch (from a ViewModel init block)
    //   - From a scheduled WorkManager job (recommended for production)
    //   - Manually from admin dashboard
    //
    // Useful for bulk cleanup when the app hasn't been opened for a few days.
    // ══════════════════════════════════════════════════════════════════════════
    suspend fun markExpiredBookingsCompleted() {
        Log.d("BOOKING_REPO", "markExpiredBookingsCompleted — scanning all active bookings")
        try {
            val now = Timestamp.now()

            // Fetch all CONFIRMED + CHECKED_IN bookings across all properties
            val snap = bookingsCol
                .whereIn("status", listOf(
                    BookingStatus.CONFIRMED.name,
                    BookingStatus.CHECKED_IN.name
                ))
                .get()
                .await()

            if (snap.isEmpty) {
                Log.d("BOOKING_REPO", "No active bookings found — nothing to complete")
                return
            }

            var completedCount = 0

            for (doc in snap.documents) {
                val booking  = parseBookingSafe(doc) ?: continue
                val checkOut = booking.checkOutDate ?: continue  // skip if no date

                if (!checkOut.toDate().after(now.toDate())) {
                    // Booking has expired — mark it complete
                    autoCompleteExpiredBooking(booking)
                    completedCount++
                }
            }

            Log.d("BOOKING_REPO", "markExpiredBookingsCompleted done — completed $completedCount bookings")

        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "markExpiredBookingsCompleted error: ${e.localizedMessage}")
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    suspend fun createBooking(booking: Booking): Resource<String> {
        Log.d("BOOKING_REPO", "createBooking START tenantId='${booking.tenantId}'")

        val resolvedTenantName = resolveTenantName(booking)
        val pendingBooking = booking.copy(
            status     = BookingStatus.PENDING.name,
            tenantName = resolvedTenantName
        )

        val result = dataManager.createBooking(pendingBooking)
        Log.d("BOOKING_REPO", "createBooking result = $result")

        if (result is Resource.Success) {
            val bookingId          = result.data ?: ""
            val resolvedLandlordId = resolveLandlordId(booking)
            val finalBooking       = pendingBooking.copy(
                bookingId  = bookingId,
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

            try {
                val bookingResource = getBookingById(bookingId)
                if (bookingResource is Resource.Success) {
                    val booking = bookingResource.data
                    when (newStatus) {
                        BookingStatus.CONFIRMED ->
                            notificationRepository.sendBookingConfirmedToTenant(
                                tenantId      = booking.tenantId,
                                bookingId     = bookingId,
                                propertyTitle = booking.propertyTitle.ifBlank { "Property" }
                            )
                        BookingStatus.CANCELLED ->
                            notificationRepository.sendBookingCancelledToTenant(
                                tenantId      = booking.tenantId,
                                bookingId     = bookingId,
                                propertyTitle = booking.propertyTitle.ifBlank { "Property" }
                            )
                        BookingStatus.COMPLETED ->
                            notificationRepository.sendBookingCompletedToTenant(
                                tenantId      = booking.tenantId,
                                bookingId     = bookingId,
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
        bookingId    : String,
        paymentStatus: String,
        paymentId    : String
    ) {
        Log.d("BOOKING_REPO", "updatePaymentStatusOnBooking: '$bookingId'")
        val updateMap = mapOf(
            "paymentStatus" to paymentStatus,
            "paymentId"     to paymentId,
            "updatedAt"     to FieldValue.serverTimestamp()
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
            "status"      to BookingStatus.CANCELLED.name,
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
        } catch (e: Exception) { "" }
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
        } catch (e: Exception) { "" }
    }

    private suspend fun sendBookingNotificationToLandlord(booking: Booking) {
        try {
            if (booking.landlordId.isBlank()) return
            notificationRepository.sendBookingRequestToLandlord(
                landlordId    = booking.landlordId,
                bookingId     = booking.bookingId,
                propertyTitle = booking.propertyTitle.ifBlank { "Property" },
                tenantName    = booking.tenantName.ifBlank { "Tenant" }
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
                    "recipientId"  to "admin",
                    "targetRole"   to "admin",
                    "title"        to "New Booking Request",
                    "body"         to "${booking.tenantName.ifBlank { "A tenant" }} ne \"${booking.propertyTitle.ifBlank { "a property" }}\" book kiya.",
                    "type"         to NotificationType.BOOKING_REQUESTED.name,
                    "referenceId"  to booking.bookingId,
                    "isRead"       to false,
                    "isActive"     to true,
                    "createdAt"    to Timestamp.now()
                )
                notificationsCol.add(notifData).await()
                return
            }

            adminQuery.documents.forEach { adminDoc ->
                notificationRepository.sendBookingNotificationToAdmin(
                    adminId       = adminDoc.id,
                    bookingId     = booking.bookingId,
                    propertyTitle = booking.propertyTitle.ifBlank { "Property" },
                    tenantName    = booking.tenantName.ifBlank { "Tenant" }
                )
            }
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "admin notification error: ${e.localizedMessage}")
        }
    }
}