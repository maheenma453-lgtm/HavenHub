package com.example.havenhub.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.havenhub.data.Payment
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val paymentsCollection = firestore.collection("payments")

    suspend fun savePayment(payment: Payment): Resource<String> {
        return try {
            val docRef = paymentsCollection.document()
            val newPayment = payment.copy(
                paymentId = docRef.id
            )
            docRef.set(newPayment).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save payment")
        }
    }

    suspend fun getUserPayments(userId: String): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("payerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch user payments")
        }
    }

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

    suspend fun getLandlordPayments(landlordId: String): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("receiverId", landlordId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
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
}