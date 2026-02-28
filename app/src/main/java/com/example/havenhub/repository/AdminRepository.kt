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

    suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snapshot = usersCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(User::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch users")
        }
    }

    suspend fun banUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).update("isBanned", true).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to ban user")
        }
    }

    suspend fun unbanUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).update("isBanned", false).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unban user")
        }
    }

    suspend fun verifyUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).update(
                mapOf(
                    "isVerified" to true,
                    "verifiedAt" to System.currentTimeMillis()
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to verify user")
        }
    }

    suspend fun getAllProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Property::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch properties")
        }
    }

    suspend fun getPendingProperties(): Resource<List<Property>> {
        return try {
            val snapshot = propertiesCollection
                .whereEqualTo("verificationStatus", "pending")
                .orderBy("submittedForVerificationAt", Query.Direction.ASCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Property::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch pending properties")
        }
    }

    suspend fun approveProperty(propertyId: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(
                mapOf(
                    "isApproved"         to true,
                    "verificationStatus" to "approved",
                    "approvedAt"         to System.currentTimeMillis()
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to approve property")
        }
    }

    suspend fun rejectProperty(propertyId: String, rejectionReason: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(
                mapOf(
                    "isApproved"         to false,
                    "verificationStatus" to "rejected",
                    "rejectionReason"    to rejectionReason,
                    "rejectedAt"         to System.currentTimeMillis()
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to reject property")
        }
    }

    suspend fun getAllBookings(): Resource<List<Booking>> {
        return try {
            val snapshot = bookingsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Booking::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch all bookings")
        }
    }

    suspend fun getAllPayments(): Resource<List<Payment>> {
        return try {
            val snapshot = paymentsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(Payment::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch payment reports")
        }
    }

    suspend fun getPendingVerifications(): Resource<List<PropertyVerification>> {
        return try {
            val snapshot = verificationsCollection
                .whereEqualTo("status", "pending")
                .orderBy("submittedAt", Query.Direction.ASCENDING)
                .get()
                .await()
            Resource.Success(snapshot.toObjects(PropertyVerification::class.java))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch verifications")
        }
    }
}