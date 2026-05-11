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

    // ── CREATE ────────────────────────────────────────────────────────────────
    suspend fun createBooking(booking: Booking): Resource<String> {

        // ✅ FIX: tenantName blank hai toh Firestore se fetch karo BEFORE saving
        val resolvedTenantName = resolveTenantName(booking)

        val pendingBooking = booking.copy(
            status     = BookingStatus.PENDING.name,
            tenantName = resolvedTenantName   // ✅ guaranteed non-blank naam
        )

        val result = dataManager.createBooking(pendingBooking)

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
    suspend fun getBookingById(bookingId: String): Resource<Booking> =
        dataManager.getBookingById(bookingId)

    suspend fun getAllBookingsForAdmin(): List<Booking> {
        return try {
            val resource = dataManager.getAllBookings()
            if (resource is Resource.Success) resource.data ?: emptyList()
            else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTenantBookings(tenantId: String): List<Booking> {
        return try { dataManager.getBookingsByTenantId(tenantId) }
        catch (e: Exception) { emptyList() }
    }

    suspend fun getLandlordBookings(landlordId: String): List<Booking> {
        return try { dataManager.getBookingsByLandlordId(landlordId) }
        catch (e: Exception) { emptyList() }
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────
    suspend fun updateBookingStatus(
        bookingId : String,
        newStatus : BookingStatus
    ): Resource<Unit> {
        val result = dataManager.updateBookingStatus(bookingId, newStatus.name)

        if (result is Resource.Success) {
            try {
                val bookingResource = dataManager.getBookingById(bookingId)
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
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("BOOKING_REPO", "Status notification error: ${e.localizedMessage}")
            }
        }
        return result
    }

    // ── PAYMENT UPDATE ────────────────────────────────────────────────────────
    suspend fun updatePaymentStatusOnBooking(
        bookingId    : String,
        paymentStatus: String,
        paymentId    : String
    ) {
        try {
            bookingsCol.document(bookingId)
                .update(
                    mapOf(
                        "paymentStatus" to paymentStatus,
                        "paymentId"     to paymentId,
                        "updatedAt"     to FieldValue.serverTimestamp()
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "updatePaymentStatus error: ${e.localizedMessage}")
        }
    }

    // ── FLOW ──────────────────────────────────────────────────────────────────
    fun getBookingsFlow(userId: String): Flow<List<Booking>> =
        realtimeListener.getBookingsFlow(userId)

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    // ✅ NEW: tenantName Firestore se resolve karo
    private suspend fun resolveTenantName(booking: Booking): String {
        // Pehle check: booking object mein naam already hai
        if (booking.tenantName.isNotBlank()) {
            Log.d("BOOKING_REPO", "tenantName already present: ${booking.tenantName}")
            return booking.tenantName
        }

        // Nahi hai toh tenantId se Firestore users collection se fetch karo
        if (booking.tenantId.isBlank()) {
            Log.e("BOOKING_REPO", "tenantId bhi blank — cannot resolve tenantName")
            return ""
        }

        return try {
            val userDoc = usersCol.document(booking.tenantId).get().await()
            val name = userDoc.getString("fullName")      // ✅ Firestore mein "fullName" field hai
                ?: userDoc.getString("name")
                ?: userDoc.getString("displayName")
                ?: ""
            if (name.isNotBlank()) {
                Log.d("BOOKING_REPO", "✅ tenantName resolved: $name")
            } else {
                Log.w("BOOKING_REPO", "tenantName empty even after Firestore fetch for ${booking.tenantId}")
            }
            name
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "resolveTenantName error: ${e.localizedMessage}")
            ""
        }
    }

    private suspend fun resolveLandlordId(booking: Booking): String {
        if (booking.landlordId.isNotBlank()) {
            Log.d("BOOKING_REPO", "landlordId from booking object: ${booking.landlordId}")
            return booking.landlordId
        }
        return try {
            if (booking.propertyId.isBlank()) {
                Log.e("BOOKING_REPO", "propertyId bhi empty — cannot resolve landlordId")
                return ""
            }
            val propDoc    = propertiesCol.document(booking.propertyId).get().await()
            val landlordId = propDoc.getString("landlordId")
                ?: propDoc.getString("ownerId")
                ?: propDoc.getString("userId")
                ?: ""
            if (landlordId.isBlank()) {
                Log.e("BOOKING_REPO", "landlordId not found in property ${booking.propertyId}")
            } else {
                Log.d("BOOKING_REPO", "landlordId fetched from property: $landlordId")
            }
            landlordId
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "resolveLandlordId error: ${e.localizedMessage}")
            ""
        }
    }

    private suspend fun sendBookingNotificationToLandlord(booking: Booking) {
        try {
            val landlordId = booking.landlordId
            if (landlordId.isBlank()) {
                Log.e("BOOKING_REPO", "landlordId empty — skipping landlord notification")
                return
            }
            notificationRepository.sendBookingRequestToLandlord(
                landlordId    = landlordId,
                bookingId     = booking.bookingId,
                propertyTitle = booking.propertyTitle.ifBlank { "Property" },
                tenantName    = booking.tenantName.ifBlank    { "Tenant"   }
            )
            Log.d("BOOKING_REPO", "✅ Landlord notification sent to $landlordId")
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "sendBookingNotificationToLandlord error: ${e.localizedMessage}")
        }
    }

    private suspend fun sendBookingNotificationToAdmin(booking: Booking) {
        try {
            var adminQuery = usersCol.whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = usersCol.whereEqualTo("role", "admin").get().await()
            }

            if (adminQuery.isEmpty) {
                Log.w("BOOKING_REPO", "No admin found — sending to generic admin recipientId")
                val notifData = mapOf(
                    "recipientId" to "admin",
                    "targetRole"  to "admin",
                    "title"       to "New Booking Request",
                    "body"        to "${booking.tenantName.ifBlank { "A tenant" }} ne \"${booking.propertyTitle.ifBlank { "a property" }}\" book kiya.",
                    "type"        to NotificationType.BOOKING_REQUESTED.name,
                    "referenceId" to booking.bookingId,
                    "isRead"      to false,
                    "isActive"    to true,
                    "createdAt"   to Timestamp.now()
                )
                notificationsCol.add(notifData).await()
                return
            }

            adminQuery.documents.forEach { adminDoc ->
                val adminId = adminDoc.id
                notificationRepository.sendBookingNotificationToAdmin(
                    adminId       = adminId,
                    bookingId     = booking.bookingId,
                    propertyTitle = booking.propertyTitle.ifBlank { "Property" },
                    tenantName    = booking.tenantName.ifBlank    { "Tenant"   }
                )
                Log.d("BOOKING_REPO", "✅ Admin notification sent to $adminId")
            }
        } catch (e: Exception) {
            Log.e("BOOKING_REPO", "sendBookingNotificationToAdmin error: ${e.localizedMessage}")
        }
    }
}