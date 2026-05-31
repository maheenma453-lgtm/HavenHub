package com.example.havenhub.repository

import android.util.Log
import com.example.havenhub.data.Booking
import com.example.havenhub.data.BookingStatus
import com.example.havenhub.data.NotificationType
import com.example.havenhub.data.PaymentStatus
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
    private val bookingsCol = firestore.collection("bookings")
    private val usersCol = firestore.collection("users")
    private val notificationsCol = firestore.collection("notifications")
    private val propertiesCol = firestore.collection("properties")

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Safe Firestore document → Booking parse
    // Returns null instead of crashing on malformed documents.
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseBookingSafe(doc: com.google.firebase.firestore.DocumentSnapshot): Booking? {
        return try {
            var b = doc.toObject(Booking::class.java) ?: return null
            // Fallback: if bookingId is empty, use the Firestore document ID
            if (b.bookingId.isBlank()) b = b.copy(bookingId = doc.id)
            b
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "Parse fail ${doc.id}: ${e.localizedMessage}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Resolve paymentStatus from a given BookingStatus
    //
    // Single source of truth — keeps paymentStatus in sync with bookingStatus
    // whenever a status change happens.
    //
    //   PENDING              → PENDING      (booking created, not yet paid)
    //   PENDING_APPROVAL     → PAID         (full payment done, awaiting landlord)
    //   CONFIRMED            → PAID         (landlord confirmed, payment complete)
    //   DEPOSIT_PAID         → DEPOSIT_PAID (20% advance paid for pre-booking)
    //   CHECKED_IN           → DEPOSIT_PAID (guest arrived, still owes 80%)
    //   AWAITING_FINAL_PAY   → DEPOSIT_PAID (80% not yet received)
    //   COMPLETED            → PAID         (stay finished, all settled)
    //   CANCELLED            → PENDING      (no payment or already refunded)
    // ─────────────────────────────────────────────────────────────────────────
    private fun resolvePaymentStatus(newStatus: BookingStatus): String {
        return when (newStatus) {
            BookingStatus.CONFIRMED -> PaymentStatus.PAID.name
            BookingStatus.PENDING_APPROVAL -> PaymentStatus.PAID.name
            BookingStatus.COMPLETED -> PaymentStatus.PAID.name
            BookingStatus.DEPOSIT_PAID -> PaymentStatus.DEPOSIT_PAID.name
            BookingStatus.CHECKED_IN -> PaymentStatus.DEPOSIT_PAID.name
            BookingStatus.AWAITING_FINAL_PAYMENT -> PaymentStatus.DEPOSIT_PAID.name
            BookingStatus.PENDING -> PaymentStatus.PENDING.name
            BookingStatus.CANCELLED -> PaymentStatus.PENDING.name
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // isPropertyBooked — DATE-AWARE AVAILABILITY CHECK
    //
    // Returns true only if the property has an ACTIVE booking whose
    // checkOutDate is still in the future (i.e. the stay is ongoing).
    //
    // Logic:
    //   1. Fetch all CONFIRMED / PENDING / CHECKED_IN bookings for this property
    //   2. For each booking, check if checkOutDate > now
    //      - Future  → property is occupied  → return true
    //      - Expired → auto-complete booking silently (frees up the property)
    //   3. No active booking found → return false → property is available
    //
    // Auto-complete runs here so no cron job or Cloud Function is needed.
    // ═════════════════════════════════════════════════════════════════════════
    suspend fun isPropertyBooked(propertyId: String): Boolean {
        if (propertyId.isBlank()) return false
        return try {
            val now = Timestamp.now()

            val snap = bookingsCol
                .whereEqualTo("propertyId", propertyId)
                .whereIn(
                    "status", listOf(
                        BookingStatus.CONFIRMED.name,
                        BookingStatus.PENDING.name,
                        BookingStatus.CHECKED_IN.name
                    )
                )
                .get()
                .await()

            if (snap.isEmpty) {
                Log.d(
                    "BOOKING_REPO",
                    "isPropertyBooked('$propertyId') = false (no active bookings)"
                )
                return false
            }

            var hasActiveBooking = false

            for (doc in snap.documents) {
                val booking = parseBookingSafe(doc) ?: continue
                val checkOut = booking.checkOutDate

                // No checkOutDate means we cannot tell when it ends → treat as active (safe default)
                if (checkOut == null) {
                    hasActiveBooking = true
                    Log.d(
                        "BOOKING_REPO",
                        "Booking ${booking.bookingId} has no checkOutDate — treating as active"
                    )
                    continue
                }

                if (checkOut.toDate().after(now.toDate())) {
                    // Stay is still ongoing → property is occupied
                    hasActiveBooking = true
                    Log.d(
                        "BOOKING_REPO",
                        "Booking ${booking.bookingId} active until ${checkOut.toDate()}"
                    )
                } else {
                    // checkOutDate has passed → auto-complete and free the property
                    Log.d(
                        "BOOKING_REPO",
                        "Booking ${booking.bookingId} expired on ${checkOut.toDate()} — auto-completing"
                    )
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

    // ═════════════════════════════════════════════════════════════════════════
    // autoCompleteExpiredBooking — PRIVATE HELPER
    //
    // Called when a booking's checkOutDate has passed.
    // Marks it COMPLETED so it no longer blocks the property.
    // Also sends a "stay completed" notification to the tenant
    // so they know they can leave a review.
    // ═════════════════════════════════════════════════════════════════════════
    private suspend fun autoCompleteExpiredBooking(booking: Booking) {
        try {
            // Only auto-complete bookings that were actually in progress.
            // PENDING bookings have not started yet — do not touch them.
            if (booking.status != BookingStatus.CONFIRMED.name &&
                booking.status != BookingStatus.CHECKED_IN.name
            ) {
                Log.d("BOOKING_REPO", "Skipping auto-complete for status=${booking.status}")
                return
            }

            bookingsCol.document(booking.bookingId).update(
                mapOf(
                    "status" to BookingStatus.COMPLETED.name,
                    "bookingStatus" to BookingStatus.COMPLETED.name,
                    // Sync paymentStatus to PAID when auto-completing a finished stay
                    "paymentStatus" to PaymentStatus.PAID.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Log.d(
                "BOOKING_REPO",
                "✅ Auto-completed booking ${booking.bookingId} — property is now free"
            )

            // Notify tenant so they can leave a review
            try {
                if (booking.tenantId.isNotBlank()) {
                    notificationRepository.sendBookingCompletedToTenant(
                        tenantId = booking.tenantId,
                        bookingId = booking.bookingId,
                        propertyTitle = booking.propertyTitle.ifBlank { "Property" }
                    )
                }
            } catch (notifEx: Exception) {
                Log.e(
                    "BOOKING_REPO",
                    "Auto-complete notification error: ${notifEx.localizedMessage}"
                )
            }

        } catch (e: Exception) {
            Log.e(
                "BOOKING_REPO",
                "autoCompleteExpiredBooking error for ${booking.bookingId}: ${e.localizedMessage}"
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // markExpiredBookingsCompleted — PUBLIC
    //
    // Scans ALL active bookings across ALL properties and completes expired ones.
    // Call this:
    //   - On app launch (from a ViewModel init block)
    //   - From a scheduled WorkManager job (recommended for production)
    //   - From admin dashboard for manual bulk cleanup
    // ═════════════════════════════════════════════════════════════════════════
    suspend fun markExpiredBookingsCompleted() {
        Log.d("BOOKING_REPO", "markExpiredBookingsCompleted — scanning all active bookings")
        try {
            val now = Timestamp.now()

            // Only look at statuses that represent an in-progress stay
            val snap = bookingsCol
                .whereIn(
                    "status", listOf(
                        BookingStatus.CONFIRMED.name,
                        BookingStatus.CHECKED_IN.name
                    )
                )
                .get()
                .await()

            if (snap.isEmpty) {
                Log.d("BOOKING_REPO", "No active bookings found — nothing to complete")
                return
            }

            var completedCount = 0

            for (doc in snap.documents) {
                val booking = parseBookingSafe(doc) ?: continue
                val checkOut = booking.checkOutDate ?: continue  // skip if no checkout date

                if (!checkOut.toDate().after(now.toDate())) {
                    autoCompleteExpiredBooking(booking)
                    completedCount++
                }
            }

            Log.d(
                "BOOKING_REPO",
                "markExpiredBookingsCompleted done — completed $completedCount bookings"
            )

        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "markExpiredBookingsCompleted error: ${e.localizedMessage}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun createBooking(booking: Booking): Resource<String> {
        Log.d(
            "BOOKING_REPO",
            "createBooking START tenantId='${booking.tenantId}' isPreBooking='${booking.isPreBooking}'"
        )

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

            // FIX: Explicitly save isPreBooking + deposit fields to Firestore.
            // This ensures the fields persist even if FirebaseDataManager missed them.
            if (bookingId.isNotBlank()) {
                try {
                    val updateMap = mutableMapOf<String, Any>(
                        "isPreBooking" to booking.isPreBooking,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    // For pre-bookings, also save depositAmount and remainingAmount
                    if (booking.isPreBooking) {
                        val deposit =
                            if (booking.depositAmount > 0) booking.depositAmount else booking.totalAmount * 0.2
                        val remaining =
                            if (booking.remainingAmount > 0) booking.remainingAmount else booking.totalAmount * 0.8
                        updateMap["depositAmount"] = deposit
                        updateMap["remainingAmount"] = remaining
                        Log.d(
                            "BOOKING_REPO",
                            "✅ Pre-booking fields saved — deposit: $deposit, remaining: $remaining"
                        )
                    }
                    bookingsCol.document(bookingId).update(updateMap).await()
                    Log.d(
                        "BOOKING_REPO",
                        "✅ isPreBooking=${booking.isPreBooking} saved to Firestore"
                    )
                } catch (e: Exception) {
                    Log.e("BOOKING_REPO", "isPreBooking save error: ${e.localizedMessage}")
                }
            }

            sendBookingNotificationToLandlord(finalBooking)
            sendBookingNotificationToAdmin(finalBooking)
        }
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────
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
            val snap = bookingsCol.whereEqualTo("tenantId", tenantId).get().await()
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
            val snap = bookingsCol.whereEqualTo("landlordId", landlordId).get().await()
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

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE STATUS
    //
    // KEY FIX: Previously only updated "status" and "bookingStatus".
    // Now also updates "paymentStatus" using resolvePaymentStatus() so both
    // fields always stay in sync after any status change.
    //
    // Example: CONFIRMED now also sets paymentStatus → PAID
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun updateBookingStatus(
        bookingId: String,
        newStatus: BookingStatus
    ): Resource<Unit> {
        Log.d("BOOKING_REPO", "updateBookingStatus: '$bookingId' → '${newStatus.name}'")
        return try {
            // Derive the correct paymentStatus for this booking status
            val newPaymentStatus = resolvePaymentStatus(newStatus)

            bookingsCol.document(bookingId).update(
                mapOf(
                    "status" to newStatus.name,
                    "bookingStatus" to newStatus.name,
                    // FIX: sync paymentStatus whenever booking status changes
                    "paymentStatus" to newPaymentStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Log.d(
                "BOOKING_REPO",
                "✅ Status updated: bookingStatus=${newStatus.name}, paymentStatus=$newPaymentStatus"
            )

            // Send a notification to the tenant based on the new status
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

                        BookingStatus.COMPLETED ->
                            notificationRepository.sendBookingCompletedToTenant(
                                tenantId = booking.tenantId,
                                bookingId = bookingId,
                                propertyTitle = booking.propertyTitle.ifBlank { "Property" }
                            )

                        else -> { /* No notification needed for other statuses */
                        }
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

    // ─────────────────────────────────────────────────────────────────────────
    // PAYMENT UPDATE
    // Called directly after a payment transaction completes.
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // FLOW — Real-time listener for bookings (used by UI layers)
    // ─────────────────────────────────────────────────────────────────────────
    fun getBookingsFlow(userId: String): Flow<List<Booking>> {
        return realtimeListener.getBookingsFlow(userId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL
    // FIX: Also resets paymentStatus to PENDING on cancel for consistency.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun cancelBooking(bookingId: String): Resource<Unit> {
        val updateMap = mapOf(
            "status" to BookingStatus.CANCELLED.name,
            "bookingStatus" to BookingStatus.CANCELLED.name,
            // Reset paymentStatus to PENDING when booking is cancelled
            "paymentStatus" to PaymentStatus.PENDING.name,
            "cancelledAt" to FieldValue.serverTimestamp()
        )
        return try {
            bookingsCol.document(bookingId).update(updateMap).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Cancel failed")
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns the tenant's display name.
     * Uses the name already on the booking if present,
     * otherwise falls back to a Firestore users lookup.
     */
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

    /**
     * Returns the landlord's UID.
     * Uses the ID already on the booking if present,
     * otherwise looks it up from the property document.
     */
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

    /** Notifies the landlord that a new booking request has been made. */
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

    /**
     * Notifies the admin that a new booking has been created.
     * Tries ADMIN role first, then admin (lowercase).
     * If no admin user document is found, saves a generic notification
     * targeting the "admin" role so it's still visible in the admin panel.
     */
    private suspend fun sendBookingNotificationToAdmin(booking: Booking) {
        try {
            var adminQuery = usersCol.whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = usersCol.whereEqualTo("role", "admin").get().await()
            }

            // No admin user found → save a fallback notification targeting the admin role
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

            // Send to every admin user found
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