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
    private val bookingsCol = firestore.collection("bookings")
    private val usersCol = firestore.collection("users")
    private val notificationsCol = firestore.collection("notifications")
    private val propertiesCol = firestore.collection("properties")

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: safely parse a Firestore document into a Booking object
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseBookingSafe(doc: com.google.firebase.firestore.DocumentSnapshot): Booking? {
        return try {
            var b = doc.toObject(Booking::class.java) ?: return null
            // If bookingId is blank, use the Firestore document ID
            if (b.bookingId.isBlank()) b = b.copy(bookingId = doc.id)
            b
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "Parse fail ${doc.id}: ${e.localizedMessage}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE BOOKING
    //
    // ROOT CAUSE FIX:
    //   OLD (broken) flow:
    //     1. Booking saved to Firestore WITHOUT landlordId
    //     2. landlordId resolved AFTER save
    //     3. Update sent — but if it fails, booking has no landlordId
    //     → getLandlordBookings() query finds nothing OR wrong landlord's data
    //       because landlordId field is empty/wrong in Firestore
    //
    //   NEW (fixed) flow:
    //     1. Resolve landlordId from Property document BEFORE saving anything
    //     2. Validate that landlordId is not blank — abort if property has no owner
    //     3. Build the complete booking object with correct landlordId
    //     4. Save to Firestore once with all fields correct
    //     → getLandlordBookings("ahmedId") only returns Ahmed's bookings
    //     → getLandlordBookings("saraId")  only returns Sara's bookings
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun createBooking(booking: Booking): Resource<String> {
        Log.d(
            "BOOKING_REPO",
            "createBooking START tenantId='${booking.tenantId}' propertyId='${booking.propertyId}'"
        )

        // ── Step 1: Resolve tenant name (if not already set) ──────────────────
        val resolvedTenantName = resolveTenantName(booking)

        // ── Step 2: Resolve landlordId from Property document BEFORE saving ───
        // This is the critical fix — we must know the correct owner BEFORE
        // writing to Firestore so the booking is always attached to the right landlord
        val resolvedLandlordId = resolveLandlordId(booking)

        if (resolvedLandlordId.isBlank()) {
            // Property has no owner — something is wrong with the data
            Log.e(
                "BOOKING_REPO",
                "createBooking ABORTED — landlordId could not be resolved for propertyId='${booking.propertyId}'"
            )
            return Resource.Error("Property owner could not be found. Please try again.")
        }

        Log.d(
            "BOOKING_REPO",
            "createBooking — resolved landlordId='$resolvedLandlordId' tenantName='$resolvedTenantName'"
        )

        // ── Step 3: Build the complete booking with all fields set correctly ───
        val completeBooking = booking.copy(
            status = BookingStatus.PENDING.name,
            tenantName = resolvedTenantName,
            landlordId = resolvedLandlordId   // ← correct landlord ID set BEFORE save
        )

        // ── Step 4: Save the complete booking to Firestore ────────────────────
        val result = dataManager.createBooking(completeBooking)
        Log.d("BOOKING_REPO", "createBooking result = $result")

        if (result is Resource.Success) {
            val bookingId = result.data ?: ""

            // ── Step 5: Save pre-booking extra fields if needed ───────────────
            // isPreBooking, depositAmount, remainingAmount need explicit update
            // because FirebaseDataManager may not handle them
            if (bookingId.isNotBlank()) {
                try {
                    val updateMap = mutableMapOf<String, Any>(
                        "isPreBooking" to booking.isPreBooking,
                        // Re-confirm landlordId in update as a safety net
                        "landlordId" to resolvedLandlordId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    if (booking.isPreBooking) {
                        // Calculate deposit (20%) and remaining (80%) if not already set
                        val deposit = if (booking.depositAmount > 0) booking.depositAmount
                        else booking.totalAmount * 0.2
                        val remaining = if (booking.remainingAmount > 0) booking.remainingAmount
                        else booking.totalAmount * 0.8
                        updateMap["depositAmount"] = deposit
                        updateMap["remainingAmount"] = remaining
                        Log.d(
                            "BOOKING_REPO",
                            "Pre-booking fields — deposit=$deposit remaining=$remaining"
                        )
                    }

                    bookingsCol.document(bookingId).update(updateMap).await()
                    Log.d("BOOKING_REPO", "Extra fields updated on booking '$bookingId'")
                } catch (e: Exception) {
                    Log.e("BOOKING_REPO", "Extra fields update error: ${e.localizedMessage}")
                    // Non-fatal — booking was already saved with correct landlordId
                }
            }

            // ── Step 6: Send notifications ────────────────────────────────────
            val finalBooking = completeBooking.copy(bookingId = bookingId)
            sendBookingNotificationToLandlord(finalBooking)
            sendBookingNotificationToAdmin(finalBooking)
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ: single booking by ID
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getBookingById(bookingId: String): Resource<Booking> {
        Log.d("BOOKING_REPO", "getBookingById: '$bookingId'")
        return dataManager.getBookingById(bookingId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ: all bookings (admin only — no filter)
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // READ: bookings for a specific tenant (tenant's "My Bookings" screen)
    // Filter: tenantId == current user's UID
    // ─────────────────────────────────────────────────────────────────────────
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
            Log.d("BOOKING_REPO", "getTenantBookings snap size = ${snap.documents.size}")
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

    // ─────────────────────────────────────────────────────────────────────────
    // READ: bookings for a specific landlord (landlord's dashboard)
    //
    // This query works correctly ONLY when every booking document in Firestore
    // has the landlordId field set to the property owner's UID.
    // The createBooking() fix above ensures this is always the case.
    //
    // Ahmed's dashboard  → getLandlordBookings("ahmed_uid") → only Ahmed's bookings
    // Sara's dashboard   → getLandlordBookings("sara_uid")  → only Sara's bookings
    // ─────────────────────────────────────────────────────────────────────────
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
            Log.d("BOOKING_REPO", "getLandlordBookings snap size = ${snap.documents.size}")
            val all = snap.documents
                .mapNotNull { parseBookingSafe(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Log.d(
                "BOOKING_REPO",
                "getLandlordBookings result: ${all.size} bookings for landlordId='$landlordId'"
            )
            all
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "getLandlordBookings EXCEPTION: ${e.localizedMessage}")
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE: booking status (CONFIRMED / CANCELLED / etc.)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun updateBookingStatus(
        bookingId: String,
        newStatus: BookingStatus
    ): Resource<Unit> {
        Log.d("BOOKING_REPO", "updateBookingStatus: '$bookingId' → '${newStatus.name}'")
        return try {
            bookingsCol.document(bookingId).update(
                mapOf(
                    "status" to newStatus.name,
                    "bookingStatus" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Send notification to tenant about status change
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

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE: payment status on a booking document
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
            Log.d("BOOKING_REPO", "Payment status updated on booking '$bookingId'")
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "updatePaymentStatus error: ${e.localizedMessage}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REALTIME FLOW: live booking updates for a user
    // ─────────────────────────────────────────────────────────────────────────
    fun getBookingsFlow(userId: String): Flow<List<Booking>> {
        return realtimeListener.getBookingsFlow(userId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL booking by ID
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // MAINTENANCE: mark past-checkout bookings as COMPLETED
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun markExpiredBookingsCompleted() {
        try {
            val now = Timestamp.now()
            val activeStatuses = listOf(
                BookingStatus.CONFIRMED.name,
                BookingStatus.CHECKED_IN.name,
                BookingStatus.PENDING_APPROVAL.name
            )

            val snap = bookingsCol
                .whereIn("status", activeStatuses)
                .get().await()

            snap.documents.forEach { doc ->
                val checkOutDate = doc.getTimestamp("checkOutDate") ?: return@forEach
                if (checkOutDate.seconds < now.seconds) {
                    doc.reference.update(
                        mapOf(
                            "status" to BookingStatus.COMPLETED.name,
                            "bookingStatus" to BookingStatus.COMPLETED.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    Log.d("BOOKING_REPO", "Expired booking marked COMPLETED: ${doc.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "markExpiredBookingsCompleted error: ${e.localizedMessage}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK: is a property currently booked (active booking exists)?
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun isPropertyBooked(propertyId: String): Boolean {
        if (propertyId.isBlank()) return false
        return try {
            val activeStatuses = listOf(
                BookingStatus.CONFIRMED.name,
                BookingStatus.CHECKED_IN.name,
                BookingStatus.PENDING_APPROVAL.name
            )
            val snap = bookingsCol
                .whereEqualTo("propertyId", propertyId)
                .whereIn("status", activeStatuses)
                .get().await()

            val booked = !snap.isEmpty
            Log.d("BOOKING_REPO", "isPropertyBooked '$propertyId' = $booked")
            booked
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "isPropertyBooked error: ${e.localizedMessage}")
            false
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    // Resolve the tenant's display name from Firestore users collection
    // Returns booking.tenantName as-is if already populated
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
            Log.w("BOOKING_REPO", "resolveTenantName failed: ${e.localizedMessage}")
            ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRITICAL HELPER: resolve landlordId from the Property document
    //
    // Priority:
    //   1. booking.landlordId if already set (e.g. passed from UI)
    //   2. property.landlordId field in Firestore
    //   3. property.ownerId field in Firestore (fallback field name)
    //   4. property.userId field in Firestore  (legacy fallback)
    //
    // This must be called BEFORE saving the booking so that landlordId
    // is always correctly set in the Firestore document from the start.
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun resolveLandlordId(booking: Booking): String {
        // If already set correctly, use it directly
        if (booking.landlordId.isNotBlank()) {
            Log.d("BOOKING_REPO", "resolveLandlordId — already set: '${booking.landlordId}'")
            return booking.landlordId
        }
        if (booking.propertyId.isBlank()) {
            Log.e("BOOKING_REPO", "resolveLandlordId — propertyId is BLANK, cannot resolve")
            return ""
        }
        return try {
            val propDoc = propertiesCol.document(booking.propertyId).get().await()
            val resolved = propDoc.getString("landlordId")
                ?: propDoc.getString("ownerId")
                ?: propDoc.getString("userId")
                ?: ""
            Log.d("BOOKING_REPO", "resolveLandlordId — fetched from property: '$resolved'")
            resolved
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "resolveLandlordId EXCEPTION: ${e.localizedMessage}")
            ""
        }
    }

    // Send booking request notification to the correct landlord
    private suspend fun sendBookingNotificationToLandlord(booking: Booking) {
        try {
            if (booking.landlordId.isBlank()) {
                Log.w(
                    "BOOKING_REPO",
                    "sendBookingNotificationToLandlord — landlordId blank, skipping"
                )
                return
            }
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

    // Send booking notification to all admin users
    private suspend fun sendBookingNotificationToAdmin(booking: Booking) {
        try {
            // Try both "ADMIN" and "admin" role values for compatibility
            var adminQuery = usersCol.whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = usersCol.whereEqualTo("role", "admin").get().await()
            }

            if (adminQuery.isEmpty) {
                // No admin user found — save a generic admin notification
                val notifData = mapOf(
                    "recipientId" to "admin",
                    "targetRole" to "admin",
                    "title" to "New Booking Request",
                    "body" to "${booking.tenantName.ifBlank { "A tenant" }} booked \"${booking.propertyTitle.ifBlank { "a property" }}\".",
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