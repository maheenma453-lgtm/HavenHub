package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.Property
import com.example.havenhub.data.Review
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.PropertyRepository
import com.example.havenhub.repository.ReviewRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    // ── Property-specific reviews (ViewReviewsScreen) ─────────────
    val isLoading     : Boolean      = false,
    val reviews       : List<Review> = emptyList(),
    val averageRating : Double       = 0.0,
    val errorMessage  : String?      = null,
    val actionSuccess : Boolean      = false,

    // ── Landlord Reply ─────────────────────────────────────────────
    val isReplyLoading : Boolean = false,
    val replySuccess   : Boolean = false,
    val replyError     : String? = null,

    // ── Global reviews (GlobalReviewsScreen) ──────────────────────
    val allReviews      : List<Review> = emptyList(),
    val isLoadingAll    : Boolean      = false,
    val allReviewsError : String?      = null,
    val selectedFilter  : String       = "All",    // "All", "5★", "4★", "3★", "2★", "1★"
    val selectedSort    : String       = "Newest", // "Newest", "Highest", "Lowest"

    // ── Property search state (AddReviewScreen ke liye) ────────────
    val propertySearchResults : List<Property> = emptyList(),
    val isSearchingProperties : Boolean        = false,
    val propertySearchError   : String?        = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository   : ReviewRepository,
    private val authRepository     : AuthRepository,
    private val propertyRepository : PropertyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    // ── Load Property-Specific Reviews ────────────────────────────
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

    // ── Load All Reviews (Global Reviews Tab) ─────────────────────
    fun loadAllReviews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAll = true, allReviewsError = null) }
            when (val result = reviewRepository.getAllReviews()) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoadingAll = false, allReviews = result.data)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoadingAll = false, allReviewsError = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    // ── Filter / Sort (Global Reviews Tab) ───────────────────────
    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSort(sort: String) {
        _uiState.update { it.copy(selectedSort = sort) }
    }

    fun getFilteredReviews(state: ReviewUiState): List<Review> {
        val filtered = when (state.selectedFilter) {
            "All" -> state.allReviews
            else  -> {
                val star = state.selectedFilter.first().digitToIntOrNull() ?: 0
                state.allReviews.filter { it.overallRating.toInt() == star }
            }
        }
        return when (state.selectedSort) {
            "Highest" -> filtered.sortedByDescending { it.overallRating }
            "Lowest"  -> filtered.sortedBy { it.overallRating }
            else      -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    // ── Property Search (AddReviewScreen dropdown ke liye) ────────
    @OptIn(FlowPreview::class)
    fun searchProperties(query: String) {
        if (query.length < 2) {
            _uiState.update {
                it.copy(propertySearchResults = emptyList(), isSearchingProperties = false)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingProperties = true, propertySearchError = null) }
            when (val result = propertyRepository.searchProperties(query)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isSearchingProperties = false,
                        propertySearchResults = result.data
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(
                        isSearchingProperties = false,
                        propertySearchError   = result.message,
                        propertySearchResults = emptyList()
                    )
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun clearPropertySearch() {
        _uiState.update {
            it.copy(
                propertySearchResults = emptyList(),
                isSearchingProperties = false,
                propertySearchError   = null
            )
        }
    }

    // ── Add Review ────────────────────────────────────────────────
    fun addReview(
        propertyId        : String,
        bookingId         : String,
        rating            : Float,
        comment           : String,
        cleanlinessRating : Float = 0f,
        locationRating    : Float = 0f,
        valueRating       : Float = 0f
    ) {
        viewModelScope.launch {
            val userId   = authRepository.currentUser?.uid ?: return@launch
            val userName = authRepository.currentUser?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: authRepository.currentUser?.email
                    ?.substringBefore("@")
                ?: "User"

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val review = Review(
                reviewerId        = userId,
                reviewerName      = userName,
                reviewerAvatarUrl = authRepository.currentUser?.photoUrl?.toString() ?: "",
                propertyId        = propertyId,
                bookingId         = bookingId,
                overallRating     = rating,
                comment           = comment,
                cleanlinessRating = cleanlinessRating,
                locationRating    = locationRating,
                valueRating       = valueRating
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

    // ── Landlord Reply to Review ───────────────────────────────────
    fun replyToReview(
        reviewId     : String,
        propertyId   : String,
        reply        : String,
        tenantId     : String,
        reviewerName : String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReplyLoading = true, replyError = null) }
            when (val result = reviewRepository.replyToReview(
                reviewId     = reviewId,
                propertyId   = propertyId,
                reply        = reply,
                tenantId     = tenantId,
                reviewerName = reviewerName
            )) {
                is Resource.Success -> {
                    // hasLandlordReply ek computed property hai: landlordReply.isNotEmpty()
                    // isliye sirf landlordReply update karo — copy() mein
                    // hasLandlordReply pass karne ki zaroorat nahi hai
                    val updatedReviews = _uiState.value.reviews.map { r ->
                        if (r.reviewId == reviewId) r.copy(landlordReply = reply) else r
                    }
                    _uiState.update {
                        it.copy(
                            isReplyLoading = false,
                            replySuccess   = true,
                            reviews        = updatedReviews
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isReplyLoading = false, replyError = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    // ── Clear Reply Success Flag ───────────────────────────────────
    fun clearReplySuccess() {
        _uiState.update { it.copy(replySuccess = false, replyError = null) }
    }

    // ── Clear Messages ────────────────────────────────────────────
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}
