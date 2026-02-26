package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Review
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.ReviewRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _reviews = MutableStateFlow<Resource<List<Review>>>(Resource.Loading)
    val reviews: StateFlow<Resource<List<Review>>> = _reviews.asStateFlow()

    // FIX: addReview() returns Resource<String> not Resource<Review>
    private val _addReviewState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val addReviewState: StateFlow<Resource<String>> = _addReviewState.asStateFlow()

    private val _averageRating = MutableStateFlow(0.0)
    val averageRating: StateFlow<Double> = _averageRating.asStateFlow()

    // FIX: getReviewsByProperty() → getPropertyReviews()
    fun loadPropertyReviews(propertyId: String) {
        viewModelScope.launch {
            _reviews.value = Resource.Loading
            val result = reviewRepository.getPropertyReviews(propertyId)
            _reviews.value = result
            if (result is Resource.Success) {
                // FIX: result.data is non-null in Resource.Success — no ?: needed
                // FIX: rating field is overallRating not rating
                val list = result.data
                _averageRating.value = if (list.isNotEmpty())
                    list.sumOf { it.overallRating.toDouble() } / list.size
                else 0.0
            }
        }
    }

    // FIX: addReview() takes a Review object, not separate params
    // FIX: getCurrentUser() → currentUser (property)
    fun addReview(
        propertyId: String,
        bookingId: String,
        rating: Float,
        comment: String
    ) {
        viewModelScope.launch {
            val userId   = authRepository.currentUser?.uid ?: return@launch
            val userName = authRepository.currentUser?.displayName ?: ""
            _addReviewState.value = Resource.Loading

            val review = Review(
                reviewerId   = userId,
                reviewerName = userName,
                propertyId   = propertyId,
                bookingId    = bookingId,
                overallRating = rating,
                comment      = comment
            )
            _addReviewState.value = reviewRepository.addReview(review)
        }
    }

    fun resetState() {
        _addReviewState.value = Resource.Loading
    }
}