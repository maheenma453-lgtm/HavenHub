package com.example.havenhub.data
/ ═══════════════════════════════════════════════════════════════════════════════
// PropertyVerification.kt
// Model: Admin verification record for a submitted property listing.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * # PropertyVerification
 *
 * Represents the full admin verification lifecycle for a property listing.
 * When a landlord submits a property for approval, a [PropertyVerification]
 * document is created in Firestore to track the review process from
 * submission through to approval or rejection.
 *
 * ## Firestore Path
 * ```
 * property_verifications/{verificationId}
 * ```
 *
 * ## Verification Status Flow
 * ```
 *  Landlord submits property
 *         │
 *         ▼
 *     [ pending ]  ──── Admin reviews ────▶  [ approved ]  →  Property goes live
 *         │
 *         └──────────────────────────────▶  [ rejected ]  →  Owner notified with reason
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * val verification = PropertyVerification(
 *     propertyId   = "prop_abc123",
 *     ownerId      = "user_xyz789",
 *     status       = PropertyVerification.STATUS_PENDING,
 *     documentUrls = listOf("https://storage.../deed.pdf")
 * )
 * ```
 *
 * @property id              Auto-generated Firestore document ID.
 * @property propertyId      Firestore document ID of the property being verified.
 * @property ownerId         Firebase Auth UID of the landlord who owns the property.
 * @property status          Current review status: "pending" | "approved" | "rejected".
 * @property submittedAt     Epoch millis when the landlord submitted for review.
 * @property reviewedAt      Epoch millis when the admin completed their review. Null if pending.
 * @property reviewedBy      Firebase Auth UID of the admin reviewer. Null if pending.
 * @property rejectionReason Human-readable explanation for rejection shown to the landlord.
 *                           Null if approved or still pending.
 * @property documentUrls    Firebase Storage URLs for uploaded verification documents.
 * @property notes           Internal admin notes. Never shown to the property owner.
 */
data class PropertyVerification(

    /** Auto-generated Firestore document ID. Populated after saving. */
    val id: String = "",

    /** Firestore document ID of the property under admin review. */
    val propertyId: String = "",

    /** Firebase Auth UID of the landlord who owns the property. */
    val ownerId: String = "",

    /**
     * Current verification status.
     * Use the companion constants [STATUS_PENDING], [STATUS_APPROVED], [STATUS_REJECTED]
     * rather than raw strings to avoid typos.
     */
    val status: String = STATUS_PENDING,

    /** Epoch millis when the landlord first submitted the property for verification. */
    val submittedAt: Long = System.currentTimeMillis(),

    /**
     * Epoch millis when the admin completed their review action.
     * Remains null until an admin either approves or rejects the submission.
     */
    val reviewedAt: Long? = null,

    /**
     * Firebase Auth UID of the admin who reviewed this submission.
     * Remains null until an admin takes action.
     */
    val reviewedBy: String? = null,

    /**
     * Brief explanation for the rejection decision.
     * Displayed to the property owner on their dashboard.
     * Only populated when [status] == [STATUS_REJECTED].
     */
    val rejectionReason: String? = null,

    /**
     * Firebase Storage download URLs for all documents uploaded by the landlord.
     * Examples: title deeds, ownership certificates, municipal rates clearance.
     */
    val documentUrls: List<String> = emptyList(),

    /**
     * Internal admin notes about this verification request.
     * These are private and are never surfaced to the property owner.
     */
    val notes: String = ""

) {

    // ─────────────────────────────────────────────────────────────────────────
    // Companion Object — Status Constants
    // Use these constants instead of raw strings to prevent typo bugs.
    // ─────────────────────────────────────────────────────────────────────────

    companion object {

        /** Property is submitted and awaiting admin review. */
        const val STATUS_PENDING = "pending"

        /** Property has been approved — visible to tenants on the platform. */
        const val STATUS_APPROVED = "approved"

        /** Property has been rejected — owner notified with [rejectionReason]. */
        const val STATUS_REJECTED = "rejected"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Computed State Helpers
    // Convenience booleans for conditional UI rendering and business logic.
    // ─────────────────────────────────────────────────────────────────────────

    /** true if this verification is awaiting admin action. */
    val isPending: Boolean
        get() = status == STATUS_PENDING

    /** true if the property listing has been approved and is live. */
    val isApproved: Boolean
        get() = status == STATUS_APPROVED

    /** true if the property was rejected by admin. */
    val isRejected: Boolean
        get() = status == STATUS_REJECTED

    /** true if the landlord has uploaded at least one verification document. */
    val hasDocuments: Boolean
        get() = documentUrls.isNotEmpty()

    /** Number of verification documents attached to this submission. */
    val documentCount: Int
        get() = documentUrls.size
}
