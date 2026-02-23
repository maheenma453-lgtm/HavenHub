package com.example.havenhub.repository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.havenhub.data.model.Booking
import com.havenhub.data.model.Payment
import com.havenhub.data.model.Property
import com.havenhub.data.model.PropertyVerification
import com.havenhub.data.model.User
import com.havenhub.utils.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdminRepository
 *
 * Provides admin-only data access operations for HavenHub's admin dashboard.
 * This repository should only be accessible to users with the "admin" role.
 *
 * Access Control:
 *  - Firestore Security Rules must restrict these collections to admin UIDs.
 *  - ViewModels should additionally guard admin functions using role checks.
 *
 * Responsibilities:
 *  - Manage user accounts (fetch all, ban, verify)
 *  - Manage property listings (fetch all, approve, reject)
 *  - Manage bookings platform-wide
 *  - View payment reports and revenue data
 *  - Handle property and user verification workflows
 */
@Singleton
class AdminRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersCollection        = firestore.collection("users")
    private val propertiesCollection   = firestore.collection("properties")
    private val bookingsCollection     = firestore.collection("bookings")
    private val paymentsCollection     = firestore.collection("payments")
    private val verificationsCollection = firestore.collection("property_verifications")

    // ─────────────────────────────────────────────────────────────────────────
    // User Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all registered users across the platform.
     * Used in the admin Manage Users screen.
     *
     * @return [Resource.Success] with a list of all [User]s, or [Resource.Error].
     */
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

    /**
     * Bans a user by setting their `isBanned` flag to true.
     * Banned users cannot log in (enforced by Firestore rules + app logic).
     *
     * @param userId The UID of the user to ban.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun banUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).update("isBanned", true).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to ban user")
        }
    }

    /**
     * Reinstates a previously banned user.
     *
     * @param userId The UID of the user to unban.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun unbanUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).update("isBanned", false).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to unban user")
        }
    }

    /**
     * Marks a user as verified (KYC/identity confirmed by admin).
     *
     * @param userId The UID of the verified user.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun verifyUser(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId).update(
                mapOf(
                    "isVerified"   to true,
                    "verifiedAt"   to System.currentTimeMillis()
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to verify user")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all property listings regardless of approval status.
     * Used in admin's Manage Properties and Verify Properties screens.
     *
     * @return [Resource.Success] with a list of all [Property]s, or [Resource.Error].
     */
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

    /**
     * Fetches all properties currently awaiting admin verification.
     *
     * @return [Resource.Success] with pending [Property] list, or [Resource.Error].
     */
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

    /**
     * Approves a property listing, making it visible to tenants.
     *
     * @param propertyId The property's Firestore document ID.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun approveProperty(propertyId: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(
                mapOf(
                    "isApproved"          to true,
                    "verificationStatus"  to "approved",
                    "approvedAt"          to System.currentTimeMillis()
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to approve property")
        }
    }

    /**
     * Rejects a property listing with a reason communicated to the owner.
     *
     * @param propertyId     The property's Firestore document ID.
     * @param rejectionReason A brief explanation to show the property owner.
     * @return [Resource.Success] with Unit, or [Resource.Error].
     */
    suspend fun rejectProperty(propertyId: String, rejectionReason: String): Resource<Unit> {
        return try {
            propertiesCollection.document(propertyId).update(
                mapOf(
                    "isApproved"          to false,
                    "verificationStatus"  to "rejected",
                    "rejectionReason"     to rejectionReason,
                    "rejectedAt"          to System.currentTimeMillis()
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to reject property")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Booking Management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all bookings across the platform.
     * Used in the admin Manage Bookings screen.
     *
     * @return [Resource.Success] with all [Booking]s, or [Resource.Error].
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // Reports & Revenue
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all payment records for admin reporting and revenue analysis.
     *
     * @return [Resource.Success] with all [Payment]s, or [Resource.Error].
     */
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

    /**
     * Fetches property verification records awaiting admin review.
     *
     * @return [Resource.Success] with [PropertyVerification] list, or [Resource.Error].
     */
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


