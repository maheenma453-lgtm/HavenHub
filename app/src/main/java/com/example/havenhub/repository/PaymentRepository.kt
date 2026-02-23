package com.example.havenhub.repository
import com.google.firebase.firestore.FirebaseFirestore
import com.havenhub.data.model.Payment
import com.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PaymentRepository
 *
 * Manages payment records and transaction history for HavenHub.
 * Payments are stored in the `payments` Firestore collection.
 *
 * NOTE: Actual payment processing (e.g., Stripe, PayFast) is handled
 * server-side via Cloud Functions. This repository only manages the
 * resulting payment records in Firestore.
 *
 * Responsibilities:
 *  - Save a payment record after a successful transaction
 *  - Fetch payment history for a user (tenant or landlord)
 *  - Fetch payment records linked to a specific booking
 *  - Update payment status (pending → completed | failed | refunded)
 */
@Singleton
class PaymentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val paymentsCollection = firestore.collection("payments")

    // ─────────────────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves a new payment record to Firestore.
     * Called after a payment gateway confirms a transaction.
     *
     * @param payment The [Payment] object with transaction details.
     * @return [Resource.Success] with the payment document ID, or [Resource.Error].
     */
    suspend fun savePayment(payment: Payment): Resource<String> {
        return try {
            val docRef = paymentsCollection.document()
            val newPayment = payment.copy(
                id        = docRef.id,
                createdAt = System.currentTimeMillis()
            )
            docRef.set(newPayment).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save payment")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all payment records made by a specific user (tenant).
     * Ordered by most recent first.
     *
     * @param userId The tenant's Firebase Auth UID.
     * @return [Resource.Success] with a list of [Payment], or [Resource.Error].
     */
    suspend fun getUserPayments(userId: String): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("payerId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user payments")
        }
    }

    /**
     * Fetches the payment record associated with a specific booking.
     * Returns null if no payment has been made for the booking yet.
     *
     * @param bookingId The Firestore document ID of the booking.
     * @return [Resource.Success] with [Payment] or null, or [Resource.Error].
     */
    suspend fun getPaymentByBooking(bookingId: String): Resource<Payment?> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("bookingId", bookingId)
                .limit(1)
                .get()
                .await()
            val payment = snapshot.toObjects(Payment::class.java).firstOrNull()
            Resource.Success(payment)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch booking payment")
        }
    }

    /**
     * Fetches all payment records received by a landlord.
     * Used in the landlord's revenue dashboard and admin reports.
     *
     * @param landlordId The landlord's Firebase Auth UID.
     * @return [Resource.Success] with a list of [Payment], or [Resource.Error].
     */
    suspend fun getLandlordPayments(landlordId: String): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("receiverId", landlordId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch landlord payments")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the status of a payment record.
     * Valid statuses: "pending", "completed", "failed", "refunded"
     *
     * @param paymentId The payment document ID.
     * @param status    The new payment status.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
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
}


