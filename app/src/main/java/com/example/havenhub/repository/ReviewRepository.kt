package com.example.havenhub.repository
import com.havenhub.data.model.Review
import com.havenhub.data.remote.FirebaseDataManager
import com.havenhub.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReviewRepository
 *
 * Manages review creation and retrieval for HavenHub property listings.
 * Reviews are submitted by tenants after a completed booking.
 *
 * Responsibilities:
 *  - Submit a new review for a property
 *  - Fetch all reviews for a given property
 *  - Calculate and return the average rating for a property
 */
@Singleton
class ReviewRepository @Inject constructor(
    private val dataManager: FirebaseDataManager
) {

    /**
     * Submits a new review for a property.
     * Automatically timestamps the review before saving.
     *
     * @param review The [Review] object containing rating, comment, and metadata.
     * @return [Resource.Success] with the new review ID, or [Resource.Error].
     */
    suspend fun addReview(review: Review): Resource<String> {
        val timestampedReview = review.copy(createdAt = System.currentTimeMillis())
        return dataManager.addReview(timestampedReview)
    }

    /**
     * Fetches all reviews for a given property, sorted by newest first.
     *
     * @param propertyId The Firestore document ID of the property.
     * @return [Resource.Success] with a list of [Review], or [Resource.Error].
     */
    suspend fun getPropertyReviews(propertyId: String): Resource<List<Review>> {
        return dataManager.getReviewsByProperty(propertyId)
    }

    /**
     * Calculates the average star rating for a property based on all reviews.
     * Returns 0.0 if no reviews exist yet.
     *
     * @param propertyId The Firestore document ID of the property.
     * @return [Resource.Success] with the average rating (0.0–5.0), or [Resource.Error].
     */
    suspend fun getAverageRating(propertyId: String): Resource<Double> {
        val reviewsResult = dataManager.getReviewsByProperty(propertyId)
        if (reviewsResult is Resource.Error) return Resource.Error(reviewsResult.message)

        val reviews = (reviewsResult as Resource.Success).data
        if (reviews.isEmpty()) return Resource.Success(0.0)

        val average = reviews.map { it.rating }.average()
        return Resource.Success(average)
    }
}


