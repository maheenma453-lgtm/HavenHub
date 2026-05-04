package com.example.havenhub.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a tenant review stored in Firestore → `reviews/{reviewId}`.
 */
data class Review(

    @DocumentId
    val reviewId: String = "",

    // ── References ────────────────────────────────────────────────────────────
    val bookingId: String = "",
    val propertyId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val reviewerAvatarUrl: String = "",
    val landlordId: String = "",

    // ── Ratings ───────────────────────────────────────────────────────────────
    val overallRating: Float = 0f,
    val cleanlinessRating: Float = 0f,
    val accuracyRating: Float = 0f,
    val communicationRating: Float = 0f,
    val checkInRating: Float = 0f,
    val valueRating: Float = 0f,
    val locationRating: Float = 0f,

    // ── Content ───────────────────────────────────────────────────────────────
    val comment: String = "",
    val photoUrls: List<String> = emptyList(),

    // ── Landlord Response ─────────────────────────────────────────────────────
    val landlordReply: String = "",
    val landlordRepliedAt: Timestamp? = null,

    // ── Moderation ────────────────────────────────────────────────────────────
    val isVisible: Boolean = true,
    val moderationNote: String = "",

    // ── Timestamps ────────────────────────────────────────────────────────────
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null

    // ✅ FIX: constructor() : this(reviewId = "") REMOVED
    // Reason: Jab saare fields mein default values hain, Firestore
    // automatically no-arg constructor generate karta hai.
    // Manual constructor @DocumentId ke saath conflict karta tha.

) {
    /** True when the landlord has publicly replied to this review. */
    val hasLandlordReply: Boolean get() = landlordReply.isNotEmpty()

    /** True when the review includes attached photos. */
    val hasPhotos: Boolean get() = photoUrls.isNotEmpty()

    /**
     * Average of all sub-ratings for display purposes.
     * Falls back to overallRating when sub-ratings are not filled.
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