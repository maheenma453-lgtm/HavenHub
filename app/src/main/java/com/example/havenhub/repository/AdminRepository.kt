package com.example.havenhub.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Payment
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyVerification
import com.example.havenhub.data.User
import com.example.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersCollection         = firestore.collection("users")
    private val propertiesCollection    = firestore.collection("properties")
    private val bookingsCollection      = firestore.collection("bookings")
    private val paymentsCollection      = firestore.collection("payments")
    private val verificationsCollection = firestore.collection("property_verifications")

    // --- User Management ---
    suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snapshot = usersCollection.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            Resource.Success(snapshot.toObjects(User::class.java))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch users") }
    }

    suspend fun banUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).update("isBanned", true).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to ban user") }
    }

    suspend fun unbanUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).update("isBanned", false).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to unban user") }
    }

    // --- Property Management ---
    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            Resource.Success(snapshot.toObjects(Property::class.java))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch properties") }
    }

    suspend fun rejectProperty(propertyId: String, reason: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(
                mapOf("status" to "REJECTED", "adminNote" to reason)
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to reject property") }
    }

    // --- Booking Management ---
    suspend fun getAllBookings(): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            Resource.Success(snapshot.toObjects(Booking::class.java))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch bookings") }
    }

    // ✅ FIXED: Missing function added
    suspend fun cancelBooking(bookingId: String): Resource<Unit> {
        return try {
            bookingsCollection.document(bookingId).update(
                mapOf(
                    "status" to "CANCELLED",
                    "cancelledAt" to com.google.firebase.Timestamp.now(),
                    "cancelledBy" to "ADMIN"
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to cancel booking") }
    }

    // --- Payment Management ---
    suspend fun getAllPayments(): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed to fetch payments") }
    }
}