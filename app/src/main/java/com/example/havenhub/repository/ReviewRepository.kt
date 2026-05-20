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

    private val usersCol      = firestore.collection("users")
    private val propertiesCol = firestore.collection("properties")
    private val reviewsCol    = firestore.collection("reviews")

    // ── Add New Review ────────────────────────────────────────────────────────
    // Tenant submits a review — saves to Firestore + notifies landlord & admin
    suspend fun addReview(review: Review): Resource<String> {
        val result = dataManager.addReview(review)
        if (result is Resource.Success) {
            sendReviewNotifications(review)
        }
        return result
    }

    // ── Landlord Reply ────────────────────────────────────────────────────────
    // Landlord replies to a tenant's review — saves reply + notifies tenant
    suspend fun replyToReview(
        reviewId     : String,
        propertyId   : String,
        reply        : String,
        tenantId     : String,
        reviewerName : String
    ): Resource<Unit> {
        return try {
            reviewsCol.document(reviewId).update(
                mapOf(
                    "landlordReply"     to reply,
                    "hasLandlordReply"  to true,
                    "landlordRepliedAt" to Timestamp.now()
                )
            ).await()

            val propertyTitle = fetchPropertyTitle(propertyId)
            if (tenantId.isNotBlank()) {
                notificationRepository.sendNotification(
                    recipientId = tenantId,
                    type        = NotificationType.NEW_REVIEW,
                    title       = "Landlord replied to your review 💬",
                    body        = "The landlord of \"$propertyTitle\" replied: \"$reply\"",
                    referenceId = propertyId,
                    targetRole  = "tenant"
                )
                Log.d("REVIEW_REPO", "✅ Reply notification sent to tenant $tenantId")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "replyToReview error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to submit reply")
        }
    }

    // ── Delete Vulgar Review (Landlord Only) ──────────────────────────────────
    // Landlord can delete a vulgar/abusive tenant review from their own property
    // Security: landlord can only delete reviews where propertyId matches their property
    suspend fun deleteReview(
        reviewId   : String,
        propertyId : String,
        landlordId : String
    ): Resource<Unit> {
        return try {
            // Step 1: Verify this review actually belongs to the property
            val reviewDoc        = reviewsCol.document(reviewId).get().await()
            val reviewPropertyId = reviewDoc.getString("propertyId") ?: ""

            if (reviewPropertyId != propertyId) {
                return Resource.Error("You can only delete reviews on your own properties")
            }

            // Step 2: Verify the property belongs to this landlord
            val propertyDoc = propertiesCol.document(propertyId).get().await()
            val ownerId = propertyDoc.getString("ownerId")
                ?: propertyDoc.getString("landlordId")
                ?: propertyDoc.getString("userId")
                ?: ""

            if (ownerId != landlordId) {
                return Resource.Error("You are not authorized to delete this review")
            }

            // Step 3: All checks passed — delete the review
            reviewsCol.document(reviewId).delete().await()
            Log.d("REVIEW_REPO", "✅ Vulgar review deleted by landlord: $reviewId")
            Resource.Success(Unit)

        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "deleteReview error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete review")
        }
    }

    // ── Delete Own Review (Tenant Only) ──────────────────────────────────────
    // Tenant apna khud ka review delete kar sakta hai
    //
    // Security layers:
    //   1. reviewerId == tenantId verify hota hai Firestore se (UI pe trust nahi)
    //   2. Firestore rules bhi ensure karti hain k sirf owner delete kare
    //
    // Agar koi dusre ka reviewId bheje → "You can only delete your own reviews" error
    suspend fun deleteOwnReview(
        reviewId : String,
        tenantId : String
    ): Resource<Unit> {
        return try {
            // Step 1: Review fetch karo Firestore se
            val reviewDoc = reviewsCol.document(reviewId).get().await()

            if (!reviewDoc.exists()) {
                return Resource.Error("Review not found")
            }

            // Step 2: Verify karo k yeh review is tenant ka apna hai
            // tenantId FirebaseAuth.uid se aata hai — UI se nahi
            val reviewerId = reviewDoc.getString("reviewerId") ?: ""
            if (reviewerId != tenantId) {
                Log.w("REVIEW_REPO", "⛔ Unauthorized delete attempt: tenant=$tenantId, reviewer=$reviewerId")
                return Resource.Error("You can only delete your own reviews")
            }

            // Step 3: Ownership confirmed — Firestore se delete karo
            reviewsCol.document(reviewId).delete().await()
            Log.d("REVIEW_REPO", "✅ Tenant deleted own review: $reviewId")
            Resource.Success(Unit)

        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "deleteOwnReview error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to delete your review")
        }
    }

    // ── Get Reviews by Property ───────────────────────────────────────────────
    // Returns all reviews for a specific property
    suspend fun getPropertyReviews(propertyId: String): Resource<List<Review>> =
        dataManager.getReviewsByProperty(propertyId)

    // ── Get All Reviews ───────────────────────────────────────────────────────
    // Returns every review across the platform (GlobalReviewsScreen for tenant/admin)
    suspend fun getAllReviews(): Resource<List<Review>> =
        dataManager.getAllReviews()

    // ── Get Reviews for Landlord's Own Properties ─────────────────────────────
    // Fetches only the reviews that belong to properties owned by this landlord
    // Used in GlobalReviewsScreen when user role is LANDLORD
    suspend fun getReviewsForLandlord(landlordId: String): Resource<List<Review>> {
        return try {
            // Step 1: Is landlord ki saari properties ka ID nikalo
            val propertiesSnapshot = propertiesCol
                .whereEqualTo("ownerId", landlordId)
                .get()
                .await()

            val propertyIds = propertiesSnapshot.documents.map { it.id }

            if (propertyIds.isEmpty()) {
                Log.d("REVIEW_REPO", "Landlord $landlordId has no properties — returning empty")
                return Resource.Success(emptyList())
            }

            // Step 2: Firestore 'whereIn' max 10 items support karta hai
            // 10 se zyada properties hain to chunked queries use hoti hain
            val allReviews = mutableListOf<Review>()
            val batches    = propertyIds.chunked(10)

            for (batch in batches) {
                val snapshot = reviewsCol
                    .whereIn("propertyId", batch)
                    .get()
                    .await()

                val reviews = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Review::class.java)
                    } catch (e: Exception) {
                        Log.e("REVIEW_REPO", "Parse fail for review ${doc.id}: ${e.localizedMessage}")
                        null
                    }
                }
                allReviews.addAll(reviews)
            }

            Log.d("REVIEW_REPO", "Landlord reviews fetched: ${allReviews.size}")
            Resource.Success(allReviews)

        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "getReviewsForLandlord error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Failed to fetch your property reviews")
        }
    }

    // ── Get Average Rating ────────────────────────────────────────────────────
    suspend fun getAverageRating(propertyId: String): Resource<Double> {
        val reviewsResult = dataManager.getReviewsByProperty(propertyId)
        if (reviewsResult is Resource.Error) return Resource.Error(reviewsResult.message)
        val reviews = (reviewsResult as Resource.Success).data
        if (reviews.isEmpty()) return Resource.Success(0.0)
        return Resource.Success(reviews.map { it.overallRating.toDouble() }.average())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private suspend fun sendReviewNotifications(review: Review) {
        try {
            val reviewerName  = review.reviewerName.ifBlank { "A tenant" }
            val rating        = review.overallRating
            val propertyId    = review.propertyId
            val propertyTitle = fetchPropertyTitle(propertyId)

            val landlordId = resolveLandlordId(propertyId, review)
            if (landlordId.isNotBlank()) {
                notificationRepository.sendNotification(
                    recipientId = landlordId,
                    type        = NotificationType.NEW_REVIEW,
                    title       = "New Review Received ⭐",
                    body        = "$reviewerName rated \"$propertyTitle\" $rating stars.",
                    referenceId = propertyId,
                    targetRole  = "landlord"
                )
                Log.d("REVIEW_REPO", "✅ Review notification sent to landlord $landlordId")
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
        } catch (e: Exception) { "Property" }
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
        } catch (e: Exception) { "" }
    }

    private suspend fun sendReviewNotificationToAdmins(
        reviewerName  : String,
        propertyTitle : String,
        rating        : Float,
        propertyId    : String
    ) {
        try {
            var adminQuery = usersCol.whereEqualTo("role", "ADMIN").get().await()
            if (adminQuery.isEmpty) {
                adminQuery = usersCol.whereEqualTo("role", "admin").get().await()
            }
            adminQuery.documents.forEach { doc ->
                notificationRepository.sendNotification(
                    recipientId = doc.id,
                    type        = NotificationType.NEW_REVIEW,
                    title       = "New Review Posted",
                    body        = "$reviewerName rated \"$propertyTitle\" $rating stars.",
                    referenceId = propertyId,
                    targetRole  = "admin"
                )
            }
        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "sendReviewNotificationToAdmins error: ${e.localizedMessage}")
        }
    }
}