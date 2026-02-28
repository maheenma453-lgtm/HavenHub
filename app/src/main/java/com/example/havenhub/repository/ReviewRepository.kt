package com.example.havenhub.repository

import com.example.havenhub.data.Review
import com.example.havenhub.remote.FirebaseDataManager
import com.example.havenhub.utils.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val dataManager: FirebaseDataManager
) {

    suspend fun addReview(review: Review): Resource<String> {
        return dataManager.addReview(review)
    }

    suspend fun getPropertyReviews(propertyId: String): Resource<List<Review>> =
        dataManager.getReviewsByProperty(propertyId)

    suspend fun getAverageRating(propertyId: String): Resource<Double> {
        val reviewsResult = dataManager.getReviewsByProperty(propertyId)
        if (reviewsResult is Resource.Error) return Resource.Error(reviewsResult.message)

        val reviews = (reviewsResult as Resource.Success).data
        if (reviews.isEmpty()) return Resource.Success(0.0)

        val average = reviews.map { it.overallRating.toDouble() }.average()
        return Resource.Success(average)
    }
}