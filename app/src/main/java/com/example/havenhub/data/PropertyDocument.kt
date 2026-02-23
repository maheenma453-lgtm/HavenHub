package com.example.havenhub.data
// ═══════════════════════════════════════════════════════════════════════════════
// PropertyDocument.kt
// Model: A single legal/ownership document uploaded for property verification.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * # PropertyDocument
 *
 * Represents a single document uploaded by a landlord as evidence of property
 * ownership or legal compliance during the verification process.
 *
 * Documents are stored in Firebase Storage and their metadata is saved in
 * Firestore as part of the property's verification submission.
 *
 * ## Supported Document Types
 * | Type Constant             | Description                              |
 * |---------------------------|------------------------------------------|
 * | [TYPE_TITLE_DEED]         | Original property title deed             |
 * | [TYPE_LEASE_AGREEMENT]    | Signed lease or rental agreement         |
 * | [TYPE_MUNICIPAL_RATES]    | Municipal rates clearance certificate    |
 * | [TYPE_PHOTO_ID]           | Owner's government-issued photo ID       |
 * | [TYPE_OTHER]              | Any other supporting document            |
 *
 * ## Firebase Storage Path
 * ```
 * property_documents/{propertyId}/{fileName}
 * ```
 *
 * ## Firestore Path
 * Stored as an array or sub-collection within the verification document:
 * ```
 * property_verifications/{verificationId}/documents/{documentId}
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * val doc = PropertyDocument(
 *     propertyId = "prop_abc123",
 *     type       = PropertyDocument.TYPE_TITLE_DEED,
 *     url        = "https://storage.googleapis.com/.../deed.pdf",
 *     fileName   = "title_deed_2024.pdf"
 * )
 * ```
 *
 * @property id          Auto-generated Firestore document ID.
 * @property propertyId  Firestore document ID of the associated property.
 * @property type        Document type label. Use the [TYPE_*] constants.
 * @property url         Firebase Storage public download URL of the uploaded file.
 * @property fileName    Original file name as provided during upload.
 * @property fileSize    File size in bytes. Used to display size info in the UI.
 * @property uploadedAt  Epoch millis when the document was uploaded.
 * @property uploadedBy  Firebase Auth UID of the user who uploaded this document.
 * @property isVerified  Whether an admin has individually verified this document.
 * @property adminNote   Optional admin note about this specific document.
 */
data class PropertyDocument(

    /** Auto-generated Firestore document ID. Populated after saving. */
    val id: String = "",

    /** Firestore document ID of the property this document belongs to. */
    val propertyId: String = "",

    /**
     * Document category/type.
     * Use the companion [TYPE_*] constants rather than raw strings.
     * Example: PropertyDocument.TYPE_TITLE_DEED
     */
    val type: String = TYPE_OTHER,

    /**
     * Firebase Storage public download URL.
     * This URL is stored after a successful upload via [FirebaseStorageManager].
     */
    val url: String = "",

    /** Original filename as provided during upload (e.g., "deed_signed_2024.pdf"). */
    val fileName: String = "",

    /**
     * File size in bytes.
     * Used to display file size information (e.g., "2.4 MB") in the document list UI.
     * Defaults to 0 if not provided.
     */
    val fileSize: Long = 0L,

    /** Epoch millis when this document was uploaded to Firebase Storage. */
    val uploadedAt: Long = System.currentTimeMillis(),

    /** Firebase Auth UID of the user (landlord) who uploaded this document. */
    val uploadedBy: String = "",

    /**
     * Indicates whether an admin has individually reviewed and verified this document.
     * A [PropertyVerification] may be approved even if not every document is verified
     * individually, but this flag helps track document-level review granularity.
     */
    val isVerified: Boolean = false,

    /**
     * Optional note from an admin regarding this specific document.
     * For example: "Deed appears expired — please re-submit."
     * Not shown to the landlord unless the admin chooses to share it.
     */
    val adminNote: String = ""

) {

    // ─────────────────────────────────────────────────────────────────────────
    // Companion Object — Document Type Constants
    // Use these instead of raw strings to avoid inconsistency across the codebase.
    // ─────────────────────────────────────────────────────────────────────────

    companion object {

        /** Original property title deed proving ownership. */
        const val TYPE_TITLE_DEED = "title_deed"

        /** Signed lease or rental agreement for the property. */
        const val TYPE_LEASE_AGREEMENT = "lease_agreement"

        /** Municipal rates clearance certificate showing no outstanding rates. */
        const val TYPE_MUNICIPAL_RATES = "municipal_rates"

        /** Owner's government-issued photo identification document. */
        const val TYPE_PHOTO_ID = "photo_id"

        /** Any other supporting document that doesn't fit the above categories. */
        const val TYPE_OTHER = "other"

        /**
         * Returns a human-readable label for a given document type constant.
         * Used for displaying document type names in the UI.
         *
         * @param type One of the [TYPE_*] constants.
         * @return A user-friendly display label string.
         */
        fun getDisplayLabel(type: String): String = when (type) {
            TYPE_TITLE_DEED      -> "Title Deed"
            TYPE_LEASE_AGREEMENT -> "Lease Agreement"
            TYPE_MUNICIPAL_RATES -> "Municipal Rates Clearance"
            TYPE_PHOTO_ID        -> "Photo ID"
            else                 -> "Other Document"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Computed Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the human-readable label for this document's type.
     * Delegates to [getDisplayLabel] for consistent formatting.
     *
     * Example: "Title Deed", "Lease Agreement"
     */
    val displayLabel: String
        get() = getDisplayLabel(type)

    /**
     * Returns the file size as a human-readable string.
     * Converts raw bytes into KB or MB format for display in the UI.
     *
     * Examples: "845 KB", "2.4 MB"
     */
    val fileSizeDisplay: String
        get() = when {
            fileSize <= 0              -> "Unknown size"
            fileSize < 1024 * 1024     -> "${fileSize / 1024} KB"
            else                       -> "${"%.1f".format(fileSize / (1024.0 * 1024.0))} MB"
        }

    /**
     * Returns the file extension extracted from [fileName].
     * Useful for determining file type icons in the document list UI.
     *
     * Example: "pdf", "jpg", "png"
     * Returns empty string if no extension is found.
     */
    val fileExtension: String
        get() = fileName.substringAfterLast('.', "").lowercase()

    /**
     * Returns true if this document is a PDF file.
     * Used to decide whether to render a PDF viewer or an image viewer.
     */
    val isPdf: Boolean
        get() = fileExtension == "pdf"
}