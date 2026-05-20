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

    // ── Global Reviews (GlobalReviewsScreen) ──────────────────────
    val allReviews      : List<Review> = emptyList(),
    val isLoadingAll    : Boolean      = false,
    val allReviewsError : String?      = null,
    val selectedFilter  : String       = "All",
    val selectedSort    : String       = "Newest",

    // ── Delete Review State ────────────────────────────────────────
    // Landlord + Tenant dono k liye same state use hoti hai
    val isDeleteReviewLoading : Boolean = false,
    val deleteReviewError     : String? = null,
    val deleteReviewSuccess   : Boolean = false,

    // ── Property Search (AddReviewScreen dropdown) ─────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD: Property-specific reviews
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD: All reviews — Tenant ke liye (GlobalReviewsScreen)
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD: Sirf landlord ki properties k reviews (GlobalReviewsScreen)
    // ─────────────────────────────────────────────────────────────────────────
    fun loadLandlordReviews() {
        viewModelScope.launch {
            val landlordId = authRepository.currentUser?.uid ?: run {
                _uiState.update {
                    it.copy(isLoadingAll = false, allReviewsError = "User not logged in")
                }
                return@launch
            }
            _uiState.update { it.copy(isLoadingAll = true, allReviewsError = null) }
            when (val result = reviewRepository.getReviewsForLandlord(landlordId)) {
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

    // ─────────────────────────────────────────────────────────────────────────
    // FILTER + SORT setters
    // ─────────────────────────────────────────────────────────────────────────
    fun setFilter(filter: String) = _uiState.update { it.copy(selectedFilter = filter) }
    fun setSort(sort: String)     = _uiState.update { it.copy(selectedSort = sort) }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE: Landlord — vulgar review hatata hai (kisi bhi tenant ka)
    //
    // Flow:
    //   GlobalReviewsScreen → long press → bottom sheet → confirm
    //   → viewModel.deleteReview(reviewId, propertyId)
    //   → repository checks landlord owns the property → Firestore delete
    // ─────────────────────────────────────────────────────────────────────────
    fun deleteReview(reviewId: String, propertyId: String) {
        viewModelScope.launch {
            val landlordId = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isDeleteReviewLoading = true, deleteReviewError = null) }

            when (val result = reviewRepository.deleteReview(
                reviewId   = reviewId,
                propertyId = propertyId,
                landlordId = landlordId
            )) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isDeleteReviewLoading = false,
                        deleteReviewSuccess   = true,
                        // Dono lists se hatao taake UI instantly update ho
                        reviews    = it.reviews.filter    { r -> r.reviewId != reviewId },
                        allReviews = it.allReviews.filter { r -> r.reviewId != reviewId }
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isDeleteReviewLoading = false, deleteReviewError = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE: Tenant — sirf APNA review delete kar sakta hai
    //
    // Flow:
    //   GlobalReviewsScreen → "Delete" button (sirf apne card par dikhta hai)
    //   → AlertDialog confirm → viewModel.deleteOwnReview(reviewId)
    //   → repository verifies reviewerId == tenantId → Firestore delete
    //
    // Security layers:
    //   Layer 1 (UI)         → isOwnReview check: button sirf apne card par
    //   Layer 2 (ViewModel)  → tenantId auth se leta hai, UI se nahi
    //   Layer 3 (Repository) → Firestore fetch karke reviewerId verify karta hai
    //   Layer 4 (Firestore)  → Security rules: delete sirf owner kar sakta hai
    // ─────────────────────────────────────────────────────────────────────────
    fun deleteOwnReview(reviewId: String) {
        viewModelScope.launch {
            // Auth se UID lo — UI se trust mat karo
            val tenantId = authRepository.currentUser?.uid ?: run {
                _uiState.update {
                    it.copy(deleteReviewError = "Aap logged in nahi hain.")
                }
                return@launch
            }

            _uiState.update { it.copy(isDeleteReviewLoading = true, deleteReviewError = null) }

            when (val result = reviewRepository.deleteOwnReview(
                reviewId = reviewId,
                tenantId = tenantId   // Repository yahan verify karega k reviewerId match kare
            )) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isDeleteReviewLoading = false,
                        deleteReviewSuccess   = true,
                        // Local lists se bhi hatao — screen refresh nahi karni padegi
                        reviews    = it.reviews.filter    { r -> r.reviewId != reviewId },
                        allReviews = it.allReviews.filter { r -> r.reviewId != reviewId }
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isDeleteReviewLoading = false, deleteReviewError = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLEAR: Delete state flags reset (snackbar dikhane ke baad call karo)
    // ─────────────────────────────────────────────────────────────────────────
    fun clearDeleteReviewState() {
        _uiState.update { it.copy(deleteReviewSuccess = false, deleteReviewError = null) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEARCH: Properties for AddReviewScreen dropdown
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // ADD: Naya review submit karna
    // ─────────────────────────────────────────────────────────────────────────
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
                ?: authRepository.currentUser?.email?.substringBefore("@")
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

    // ─────────────────────────────────────────────────────────────────────────
    // REPLY: Landlord ka tenant review par reply
    // ─────────────────────────────────────────────────────────────────────────
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

    fun clearReplySuccess() = _uiState.update { it.copy(replySuccess = false, replyError = null) }
    fun clearMessages()     = _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
}