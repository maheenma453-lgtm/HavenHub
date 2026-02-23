package com.example.havenhub.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a tenant review stored in Firestore → `reviews/{reviewId}`.
 *
 * Reviews are linked to a completed [Booking] and can only be submitted
 * once per booking. They contribute to the property's [Property.averageRating]
 * and the landlord's [User.landlordRating] (updated via Cloud Function).
 *
 * Supports category-level sub-ratings for granular feedback.
 */
data class Review(

    @DocumentId
    val reviewId: String = "",

    // ── References ────────────────────────────────────────────────────────────

    /** The completed booking this review is for. */
    val bookingId: String = "",

    val propertyId: String = "",

    /** UID of the tenant who wrote the review. */
    val reviewerId: String = "",

    /** Reviewer name (denormalized for list display). */
    val reviewerName: String = "",

    /** Reviewer avatar URL (denormalized). */
    val reviewerAvatarUrl: String = "",

    /** UID of the property's landlord. */
    val landlordId: String = "",

    // ── Ratings ───────────────────────────────────────────────────────────────

    /** Overall star rating (1–5). */
    val overallRating: Float = 0f,

    /** Sub-rating: cleanliness of the property (1–5). */
    val cleanlinessRating: Float = 0f,

    /** Sub-rating: accuracy vs. listing description (1–5). */
    val accuracyRating: Float = 0f,

    /** Sub-rating: landlord responsiveness (1–5). */
    val communicationRating: Float = 0f,

    /** Sub-rating: check-in experience (1–5). */
    val checkInRating: Float = 0f,

    /** Sub-rating: value for money (1–5). */
    val valueRating: Float = 0f,

    /** Sub-rating: property location and surroundings (1–5). */
    val locationRating: Float = 0f,

    // ── Content ───────────────────────────────────────────────────────────────

    /** Written review text (optional). */
    val comment: String = "",

    /** URLs of photos attached to the review. */
    val photoUrls: List<String> = emptyList(),

    // ── Landlord Response ─────────────────────────────────────────────────────

    /** Optional public reply from the landlord. */
    val landlordReply: String = "",

    val landlordRepliedAt: Timestamp? = null,

    // ── Moderation ────────────────────────────────────────────────────────────

    /** Whether this review is publicly visible. */
    val isVisible: Boolean = true,

    /** Admin moderation note if the review was hidden. */
    val moderationNote: String = "",

    // ── Timestamps ────────────────────────────────────────────────────────────

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    val updatedAt: Timestamp? = null

) {
    constructor() : this(reviewId = "")

    /** True when the landlord has publicly replied to this review. */
    val hasLandlordReply: Boolean get() = landlordReply.isNotEmpty()

    /** True when the review includes attached photos. */
    val hasPhotos: Boolean get() = photoUrls.isNotEmpty()

    /**
     * Average of all sub-ratings for display purposes.
     * Falls back to [overallRating] when sub-ratings are not filled.
     */
    val calculatedAverage: Float
        get() {
            val subs = listOf(
                cleanlinessRating,
                accuracyRating,
                communicationRating,
                checkInRating,
                valueRating,
                locationRating
            ).filter { it > 0f }
            return if (subs.isEmpty()) overallRating else subs.average().toFloat()
        }
}

