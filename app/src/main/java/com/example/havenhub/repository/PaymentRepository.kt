package com.example.havenhub.repository

import android.util.Log
import com.example.havenhub.data.Booking
import com.google.firebase.firestore.FirebaseFirestore
import com.example.havenhub.data.NotificationType
import com.example.havenhub.data.Payment
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.List

@Singleton
class PaymentRepository @Inject constructor(
    private val firestore             : FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) {

    private val paymentsCollection = firestore.collection("payments")
    private val usersCol           = firestore.collection("users")

    // ── SAVE ──────────────────────────────────────────────────────────────────
    suspend fun savePayment(payment: Payment): Resource<String> {
        return try {
            val docRef     = paymentsCollection.document()
            val newPayment = payment.copy(paymentId = docRef.id)
            docRef.set(newPayment).await()
            sendPaymentNotifications(newPayment)
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save payment")
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    // ✅ FIX: orderBy hata diya — payments index abhi build ho raha hai
    // Client-side sort se same result milega
    suspend fun getUserPayments(userId: String): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("payerId", userId)
                // orderBy removed — composite index required tha jo nahi tha
                .get().await()
            Resource.Success(
                snapshot.toObjects(Payment::class.java)
                    .sortedByDescending { it.createdAt }  // client-side sort
            )
        } catch (e: Exception) {
            Log.e("PAYMENT_REPO", "getUserPayments FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch user payments")
        }
    }

    suspend fun getPaymentByBooking(bookingId: String): Resource<Payment?> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("bookingId", bookingId)
                .limit(1)
                .get().await()
            Resource.Success(snapshot.toObjects(Payment::class.java).firstOrNull())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch booking payment")
        }
    }

    // ✅ FIX: orderBy hata diya — landlord payments bhi same issue tha
    suspend fun getLandlordPayments(landlordId: String): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("payeeId", landlordId)
                // orderBy removed — composite index required tha jo nahi tha
                .get().await()
            Resource.Success(
                snapshot.toObjects(Payment::class.java)
                    .sortedByDescending { it.createdAt }  // client-side sort
            )
        } catch (e: Exception) {
            Log.e("PAYMENT_REPO", "getLandlordPayments FAIL: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch landlord payments")
        }
    }

    suspend fun updatePaymentStatus(paymentId: String, status: String): Resource<Unit> {
        return try {
            paymentsCollection.document(paymentId)
                .update("status", status)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update payment status")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private suspend fun sendPaymentNotifications(payment: Payment) {
        try {
            val amountInt     = payment.amountDouble.toInt()
            val propertyTitle = fetchPropertyTitle(payment.bookingId)
            val bookingId     = payment.bookingId

            // 1. Tenant ko confirm karo
            if (payment.payerId.isNotBlank()) {
                notificationRepository.sendNotification(
                    recipientId = payment.payerId,
                    type        = NotificationType.PAYMENT_RECEIVED,
                    title       = "Payment Successful! 💚",
                    body        = "Rs. $amountInt \"$propertyTitle\" ke liye payment confirm ho gayi.",
                    referenceId = bookingId,
                    targetRole  = "tenant"
                )
                Log.d("PAYMENT_REPO", "✅ Tenant payment notification sent to ${payment.payerId}")
            }

            // 2. Landlord ko batao
            val landlordId = resolvePayeeId(payment)
            if (landlordId.isNotBlank()) {
                notificationRepository.sendNotification(
                    recipientId = landlordId,
                    type        = NotificationType.PAYMENT_RECEIVED,
                    title       = "Payment Received! 💰",
                    body        = "Rs. $amountInt \"$propertyTitle\" ke liye receive hua.",
                    referenceId = bookingId,
                    targetRole  = "landlord"
                )
                Log.d("PAYMENT_REPO", "✅ Landlord payment notification sent to $landlordId")
            }

            // 3. Admin ko inform karo
            sendPaymentNotificationToAdmins(amountInt, propertyTitle, bookingId)

        } catch (e: Exception) {
            Log.e("PAYMENT_REPO", "sendPaymentNotifications error: ${e.localizedMessage}")
        }
    }

    private suspend fun fetchPropertyTitle(bookingId: String): String {
        if (bookingId.isBlank()) return "Property"
        return try {
            val bookingDoc = firestore.collection("bookings").document(bookingId).get().await()
            bookingDoc.getString("propertyTitle") ?: "Property"
        } catch (e: Exception) {
            Log.e("PAYMENT_REPO", "fetchPropertyTitle error: ${e.localizedMessage}")
            "Property"
        }
    }

    private suspend fun resolvePayeeId(payment: Payment): String {
        if (payment.payeeId.isNotBlank()) return payment.payeeId
        if (payment.bookingId.isBlank())  return ""
        return try {
            val bookingDoc = firestore.collection("bookings").document(payment.bookingId).get().await()
            bookingDoc.getString("landlordId") ?: ""
        } catch (e: Exception) {
            Log.e("PAYMENT_REPO", "resolvePayeeId error: ${e.localizedMessage}")
            ""
        }
    }

    private suspend fun sendPaymentNotificationToAdmins(
        amountInt    : Int,
        propertyTitle: String,
        bookingId    : String
    ) {
        try {
            var adminQuery = usersCol.whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = usersCol.whereEqualTo("role", "admin").get().await()
            }
            adminQuery.documents.forEach { doc ->
                notificationRepository.sendNotification(
                    recipientId = doc.id,
                    type        = NotificationType.PAYMENT_RECEIVED,
                    title       = "Payment Received (Admin)",
                    body        = "Rs. $amountInt \"$propertyTitle\" ke liye receive hua.",
                    referenceId = bookingId,
                    targetRole  = "admin"
                )
            }
        } catch (e: Exception) {
            Log.e("PAYMENT_REPO", "sendPaymentNotificationToAdmins error: ${e.localizedMessage}")
        }
    }
}















