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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val isLoading: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val averageRating: Double = 0.0,
    val errorMessage: String? = null,
    val actionSuccess: Boolean = false
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    // Load Property Reviews
    fun loadPropertyReviews(propertyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = reviewRepository.getPropertyReviews(propertyId)) {
                is Resource.Success -> {
                    val list = result.data
                    val avg  = if (list.isNotEmpty())
                        list.sumOf { it.overallRating.toDouble() } / list.size
                    else 0.0
                    _uiState.update {
                        it.copy(isLoading = false, reviews = list, averageRating = avg)
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    // Add Review
    fun addReview(
        propertyId: String,
        bookingId: String,
        rating: Float,
        comment: String
    ) {
        viewModelScope.launch {
            val userId   = authRepository.currentUser?.uid ?: return@launch
            val userName = authRepository.currentUser?.displayName ?: ""

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val review = Review(
                reviewerId    = userId,
                reviewerName  = userName,
                propertyId    = propertyId,
                bookingId     = bookingId,
                overallRating = rating,
                comment       = comment
            )

            when (val result = reviewRepository.addReview(review)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, actionSuccess = true)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    // Clear Messages — navigation ke baad call karo
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}