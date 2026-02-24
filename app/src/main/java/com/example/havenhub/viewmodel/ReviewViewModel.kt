package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.Review
import com.havenhub.data.repository.AuthRepository
import com.havenhub.data.repository.ReviewRepository
import com.havenhub.utils.Resource
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

    private val _reviews = MutableStateFlow<Resource<List<Review>>>(Resource.Idle())
    val reviews: StateFlow<Resource<List<Review>>> = _reviews.asStateFlow()

    private val _addReviewState = MutableStateFlow<Resource<Review>>(Resource.Idle())
    val addReviewState: StateFlow<Resource<Review>> = _addReviewState.asStateFlow()

    private val _averageRating = MutableStateFlow(0.0)
    val averageRating: StateFlow<Double> = _averageRating.asStateFlow()

    fun loadPropertyReviews(propertyId: String) {
        viewModelScope.launch {
            _reviews.value = Resource.Loading()
            val result = reviewRepository.getReviewsByProperty(propertyId)
            _reviews.value = result
            if (result is Resource.Success) {
                val list = result.data ?: emptyList()
                _averageRating.value = if (list.isNotEmpty())
                    list.sumOf { it.rating } / list.size.toDouble()
                else 0.0
            }
        }
    }

    fun addReview(
        propertyId: String,
        bookingId: String,
        rating: Double,
        comment: String
    ) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            val userName = authRepository.getCurrentUser()?.displayName ?: ""
            _addReviewState.value = Resource.Loading()
            _addReviewState.value = reviewRepository.addReview(
                userId = userId,
                userName = userName,
                propertyId = propertyId,
                bookingId = bookingId,
                rating = rating,
                comment = comment
            )
        }
    }

    fun resetState() {
        _addReviewState.value = Resource.Idle()
    }
}