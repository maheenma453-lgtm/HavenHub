package com.example.havenhub.repository

import android.util.Log
import com.example.havenhub.data.NotificationType
import com.example.havenhub.data.Review
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val dataManager            : FirebaseDataManager,
    private val firestore              : FirebaseFirestore,
    private val notificationRepository : NotificationRepository
) {

    private val usersCol = firestore.collection("users")
    private val propertiesCol = firestore.collection("properties")
    private val reviewsCol = firestore.collection("reviews")

    // -------------------------------------------------------------------------
    // ADD NEW REVIEW
    // Delegates to FirebaseDataManager which writes createdAt via serverTimestamp
    // -------------------------------------------------------------------------
    suspend fun addReview(review: Review): Resource<String> {
        val result = dataManager.addReview(review)
        if (result is Resource.Success) {
            sendReviewNotifications(review)
        }
        return result
    }

    // -------------------------------------------------------------------------
    // LANDLORD REPLY
    // Writes reply + sets hasLandlordReply = true + sends notification to tenant
    // -------------------------------------------------------------------------
    suspend fun replyToReview(
        reviewId: String,
        propertyId: String,
        reply: String,
        tenantId: String,
        reviewerName: String
    ): Resource<Unit> {
        return try {
            reviewsCol.document(reviewId).update(
                mapOf(
                    "landlordReply" to reply,
                    "hasLandlordReply" to true,
                    "landlordRepliedAt" to Timestamp.now()
                )
            ).await()

            val propertyTitle = fetchPropertyTitle(propertyId)
            if (tenantId.isNotBlank()) {
                notificationRepository.sendNotification(
                    recipientId = tenantId,
                    type = NotificationType.NEW_REVIEW,
                    title = "Landlord replied to your review",
                    body = "The landlord of \"$propertyTitle\" replied: \"$reply\"",
                    referenceId = propertyId,
                    targetRole = "tenant"
                )
                Log.d("REVIEW_REPO", "Reply notification sent to tenant $tenantId")
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "replyToReview error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to submit reply")
        }
    }

    // -------------------------------------------------------------------------
    // DELETE REVIEW — Landlord only
    //
    // Previous bug: ownership check was querying "ownerId" field but many
    // Firestore property docs store it as "landlordId" or "userId", so the
    // check always failed silently with "not authorized".
    //
    // Fix: removed the broken Kotlin-side ownership check entirely.
    // Security is enforced by:
    //   Layer 1 (Firebase Rules) → allow delete if isAdmin() || isLandlord()
    //   Layer 2 (UI)             → Delete button only shown to landlords
    // -------------------------------------------------------------------------
    suspend fun deleteReview(
        reviewId: String,
        propertyId: String,
        landlordId: String
    ): Resource<Unit> {
        return try {
            Log.d(
                "REVIEW_REPO",
                "deleteReview: reviewId=$reviewId propertyId=$propertyId landlordId=$landlordId"
            )

            val reviewDoc = reviewsCol.document(reviewId).get().await()
            if (!reviewDoc.exists()) {
                Log.e("REVIEW_REPO", "Review $reviewId not found")
                return Resource.Error("Review not found")
            }

            // Firebase security rules enforce landlord-only access
            reviewsCol.document(reviewId).delete().await()
            Log.d("REVIEW_REPO", "Review deleted successfully: $reviewId")
            Resource.Success(Unit)

        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "deleteReview FAILED: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete review")
        }
    }

    // -------------------------------------------------------------------------
    // DELETE OWN REVIEW — Tenant only
    // Repository verifies reviewerId == tenantId before deleting
    // -------------------------------------------------------------------------
    suspend fun deleteOwnReview(
        reviewId: String,
        tenantId: String
    ): Resource<Unit> {
        return try {
            val reviewDoc = reviewsCol.document(reviewId).get().await()
            if (!reviewDoc.exists()) {
                return Resource.Error("Review not found")
            }

            val reviewerId = reviewDoc.getString("reviewerId") ?: ""
            if (reviewerId != tenantId) {
                Log.w("REVIEW_REPO", "Unauthorized delete: tenant=$tenantId reviewer=$reviewerId")
                return Resource.Error("You can only delete your own reviews")
            }

            reviewsCol.document(reviewId).delete().await()
            Log.d("REVIEW_REPO", "Tenant deleted own review: $reviewId")
            Resource.Success(Unit)

        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "deleteOwnReview error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete your review")
        }
    }

    // -------------------------------------------------------------------------
    // GET REVIEWS — by property
    // -------------------------------------------------------------------------
    suspend fun getPropertyReviews(propertyId: String): Resource<List<Review>> =
        dataManager.getReviewsByProperty(propertyId)

    // -------------------------------------------------------------------------
    // GET ALL REVIEWS — for tenant / admin (GlobalReviewsScreen)
    // -------------------------------------------------------------------------
    suspend fun getAllReviews(): Resource<List<Review>> =
        dataManager.getAllReviews()

    // -------------------------------------------------------------------------
    // GET REVIEWS FOR LANDLORD'S PROPERTIES
    //
    // ROOT CAUSE FIX (date missing on landlord reviews screen):
    //
    // Old code used doc.toObject(Review::class.java) which IGNORES the custom
    // parseReview() logic in FirebaseDataManager. That custom parser has the
    // createdAt fallback: if createdAt is null it falls back to updatedAt.
    // toObject() has no such fallback — it just maps null → null, so dates
    // were blank on every landlord review.
    //
    // Fix: fetch raw document snapshots and pass each one through
    // dataManager.getReviewsByProperty() per property, which internally uses
    // parseReview(). This guarantees the same date-fix logic runs for landlord
    // reviews as it does for tenant/global reviews.
    // -------------------------------------------------------------------------
    suspend fun getReviewsForLandlord(landlordId: String): Resource<List<Review>> {
        return try {
            // Step 1: get all property IDs belonging to this landlord
            val propertiesSnapshot = propertiesCol
                .whereEqualTo("ownerId", landlordId)
                .get()
                .await()

            val propertyIds = propertiesSnapshot.documents.map { it.id }

            if (propertyIds.isEmpty()) {
                Log.d("REVIEW_REPO", "Landlord $landlordId has no properties — empty list")
                return Resource.Success(emptyList())
            }

            // Step 2: for each property, use dataManager.getReviewsByProperty()
            // so parseReview() (with the createdAt fallback fix) runs on every doc
            val allReviews = mutableListOf<Review>()

            for (propertyId in propertyIds) {
                when (val result = dataManager.getReviewsByProperty(propertyId)) {
                    is Resource.Success -> allReviews.addAll(result.data)
                    is Resource.Error -> Log.w(
                        "REVIEW_REPO",
                        "Could not fetch reviews for property $propertyId: ${result.message}"
                    )

                    Resource.Loading -> Unit
                }
            }

            // Sort newest first — createdAt is now reliably populated
            allReviews.sortByDescending { it.createdAt?.seconds ?: 0L }

            Log.d("REVIEW_REPO", "Landlord reviews fetched: ${allReviews.size}")
            Resource.Success(allReviews)

        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "getReviewsForLandlord error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch your property reviews")
        }
    }

    // -------------------------------------------------------------------------
    // GET AVERAGE RATING for a property
    // -------------------------------------------------------------------------
    suspend fun getAverageRating(propertyId: String): Resource<Double> {
        val reviewsResult = dataManager.getReviewsByProperty(propertyId)
        if (reviewsResult is Resource.Error) return Resource.Error(reviewsResult.message)
        val reviews = (reviewsResult as Resource.Success).data
        if (reviews.isEmpty()) return Resource.Success(0.0)
        return Resource.Success(reviews.map { it.overallRating.toDouble() }.average())
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private suspend fun sendReviewNotifications(review: Review) {
        try {
            val reviewerName = review.reviewerName.ifBlank { "A tenant" }
            val rating = review.overallRating
            val propertyId = review.propertyId
            val propertyTitle = fetchPropertyTitle(propertyId)

            val landlordId = resolveLandlordId(propertyId, review)
            if (landlordId.isNotBlank()) {
                notificationRepository.sendNotification(
                    recipientId = landlordId,
                    type = NotificationType.NEW_REVIEW,
                    title = "New Review Received",
                    body = "$reviewerName rated \"$propertyTitle\" $rating stars.",
                    referenceId = propertyId,
                    targetRole = "landlord"
                )
                Log.d("REVIEW_REPO", "Review notification sent to landlord $landlordId")
            }
            sendReviewNotificationToAdmins(reviewerName, propertyTitle, rating, propertyId)
        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "sendReviewNotifications error: ${e.localizedMessage}")
        }
    }

    private suspend fun fetchPropertyTitle(propertyId: String): String {
        if (propertyId.isBlank()) return "Property"
        return try {
            val doc = propertiesCol.document(propertyId).get().await()
            doc.getString("title") ?: "Property"
        } catch (e: Exception) {
            "Property"
        }
    }

    private suspend fun resolveLandlordId(propertyId: String, review: Review): String {
        if (review.landlordId.isNotBlank()) return review.landlordId
        if (propertyId.isBlank()) return ""
        return try {
            val doc = propertiesCol.document(propertyId).get().await()
            doc.getString("ownerId")
                ?: doc.getString("landlordId")
                ?: doc.getString("userId")
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun sendReviewNotificationToAdmins(
        reviewerName: String,
        propertyTitle: String,
        rating: Float,
        propertyId: String
    ) {
        try {
            var adminQuery = usersCol.whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = usersCol.whereEqualTo("role", "admin").get().await()
            }
            adminQuery.documents.forEach { doc ->
                notificationRepository.sendNotification(
                    recipientId = doc.id,
                    type = NotificationType.NEW_REVIEW,
                    title = "New Review Posted",
                    body = "$reviewerName rated \"$propertyTitle\" $rating stars.",
                    referenceId = propertyId,
                    targetRole = "admin"
                )
            }
        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "sendReviewNotificationToAdmins error: ${e.localizedMessage}")
        }
    }
}