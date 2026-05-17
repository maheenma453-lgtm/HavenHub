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

    // ── WRITE ─────────────────────────────────────────────────────────────────
    suspend fun addReview(review: Review): Resource<String> {
        val result = dataManager.addReview(review)
        if (result is Resource.Success) {
            sendReviewNotifications(review)
        }
        return result
    }

    // ── LANDLORD REPLY ────────────────────────────────────────────────────────
    suspend fun replyToReview(
        reviewId     : String,
        propertyId   : String,
        reply        : String,
        tenantId     : String,
        reviewerName : String
    ): Resource<Unit> {
        return try {
            // Firestore mein review document update karo
            reviewsCol.document(reviewId).update(
                mapOf(
                    "landlordReply"    to reply,
                    "hasLandlordReply" to true,
                    "landlordRepliedAt" to Timestamp.now()
                )
            ).await()

            // Tenant ko notification bhejo
            val propertyTitle = fetchPropertyTitle(propertyId)
            if (tenantId.isNotBlank()) {
                notificationRepository.sendNotification(
                    recipientId = tenantId,
                    type        = NotificationType.NEW_REVIEW,
                    title       = "Landlord ne aapke review ka jawab diya 💬",
                    body        = "\"$propertyTitle\" ke landlord ne aapke review ka reply kiya: \"$reply\"",
                    referenceId = propertyId,
                    targetRole  = "tenant"
                )
                Log.d("REVIEW_REPO", "✅ Reply notification sent to tenant $tenantId")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "replyToReview error: ${e.localizedMessage}")
            Resource.Error(e.localizedMessage ?: "Reply submit karne mein error aayi")
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────
    suspend fun getPropertyReviews(propertyId: String): Resource<List<Review>> =
        dataManager.getReviewsByProperty(propertyId)

    suspend fun getAllReviews(): Resource<List<Review>> =
        dataManager.getAllReviews()

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

            // 1. Landlord ko NEW_REVIEW batao
            val landlordId = resolveLandlordId(propertyId, review)
            if (landlordId.isNotBlank()) {
                notificationRepository.sendNotification(
                    recipientId = landlordId,
                    type        = NotificationType.NEW_REVIEW,
                    title       = "New Review Received ⭐",
                    body        = "$reviewerName ne \"$propertyTitle\" ko $rating star diye.",
                    referenceId = propertyId,
                    targetRole  = "landlord"
                )
                Log.d("REVIEW_REPO", "✅ Review notification sent to landlord $landlordId")
            }

            // 2. Admin ko bhi inform karo
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
            Log.e("REVIEW_REPO", "fetchPropertyTitle error: ${e.localizedMessage}")
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
            Log.e("REVIEW_REPO", "resolveLandlordId error: ${e.localizedMessage}")
            ""
        }
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
                    body        = "$reviewerName ne \"$propertyTitle\" ko $rating star diye.",
                    referenceId = propertyId,
                    targetRole  = "admin"
                )
            }
        } catch (e: Exception) {
            Log.e("REVIEW_REPO", "sendReviewNotificationToAdmins error: ${e.localizedMessage}")
        }
    }
}