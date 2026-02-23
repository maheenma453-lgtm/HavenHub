package com.example.havenhub.data
// ═══════════════════════════════════════════════════════════════════════════════
// Message.kt
// Model: A single chat message in a tenant ↔ landlord conversation.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * # Message
 *
 * Represents a single chat message exchanged between two users (typically
 * a tenant and a landlord) within HavenHub's in-app messaging system.
 *
 * Messages are stored as a sub-collection beneath a parent conversation document.
 * The parent conversation is identified by a deterministic [conversationId]
 * derived from both participants' UIDs.
 *
 * ## Firestore Path
 * ```
 * conversations/{conversationId}/messages/{messageId}
 * ```
 *
 * ## Conversation ID Convention
 * To guarantee a single shared conversation document regardless of who
 * initiates the chat, the ID is built by sorting both UIDs lexicographically:
 * ```kotlin
 * val conversationId = listOf(uid1, uid2).sorted().joinToString("_")
 * // Example: "uid_alice_uid_bob"
 * ```
 *
 * ## Message Types
 * | Type Constant    | Description                              |
 * |------------------|------------------------------------------|
 * | [TYPE_TEXT]      | Plain text message (default)             |
 * | [TYPE_IMAGE]     | Image shared via [mediaUrl]              |
 * | [TYPE_DOCUMENT]  | PDF or file shared via [mediaUrl]        |
 *
 * ## Usage Example
 * ```kotlin
 * val message = Message(
 *     conversationId = "uid_alice_uid_bob",
 *     senderId       = "uid_alice",
 *     receiverId     = "uid_bob",
 *     content        = "Is the apartment still available?",
 *     messageType    = Message.TYPE_TEXT
 * )
 * ```
 *
 * @property id             Auto-generated Firestore message document ID.
 * @property conversationId The parent conversation document ID.
 * @property senderId       Firebase Auth UID of the user who sent this message.
 * @property receiverId     Firebase Auth UID of the intended recipient.
 * @property content        The text body of the message. Empty for media-only messages.
 * @property timestamp      Epoch millis when the message was sent. Used for ordering.
 * @property isRead         Whether the recipient has read this message.
 * @property messageType    Content type of the message. Use the [TYPE_*] constants.
 * @property mediaUrl       Firebase Storage URL for image or document messages. Null for text.
 * @property mediaFileName  Original filename for document messages. Null for text/image.
 */
data class Message(

    /** Auto-generated Firestore document ID. Populated after saving. */
    val id: String = "",

    /**
     * The parent conversation document ID.
     * Derived by sorting and joining both participant UIDs:
     * min(uid1, uid2) + "_" + max(uid1, uid2)
     */
    val conversationId: String = "",

    /** Firebase Auth UID of the user who sent this message. */
    val senderId: String = "",

    /** Firebase Auth UID of the intended message recipient. */
    val receiverId: String = "",

    /**
     * The text content of the message.
     * For [TYPE_IMAGE] or [TYPE_DOCUMENT] messages this may contain
     * an optional caption or be left empty.
     */
    val content: String = "",

    /**
     * Epoch millis representing when this message was sent.
     * Used to order messages chronologically in the chat UI.
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Indicates whether the recipient has read this message.
     * Updated to true by [MessagingRepository.markMessagesAsRead]
     * when the recipient opens the chat screen.
     */
    val isRead: Boolean = false,

    /**
     * The type of content this message carries.
     * Use the companion [TYPE_*] constants.
     * Defaults to [TYPE_TEXT] for plain text messages.
     */
    val messageType: String = TYPE_TEXT,

    /**
     * Firebase Storage download URL for media messages.
     * Populated for [TYPE_IMAGE] and [TYPE_DOCUMENT] messages.
     * Null for plain [TYPE_TEXT] messages.
     */
    val mediaUrl: String? = null,

    /**
     * Original filename for document-type messages.
     * Displayed beneath the file icon in the chat bubble.
     * Null for text and image messages.
     */
    val mediaFileName: String? = null

) {

    // ─────────────────────────────────────────────────────────────────────────
    // Companion Object — Message Type Constants
    // ─────────────────────────────────────────────────────────────────────────

    companion object {

        /** Plain text message. The most common message type. */
        const val TYPE_TEXT = "text"

        /** Image message. The image file URL is stored in [mediaUrl]. */
        const val TYPE_IMAGE = "image"

        /** Document/file message (e.g., lease agreement PDF). URL in [mediaUrl]. */
        const val TYPE_DOCUMENT = "document"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Computed Helpers
    // Convenience properties for UI rendering logic.
    // ─────────────────────────────────────────────────────────────────────────

    /** true if this is a plain text message. */
    val isTextMessage: Boolean
        get() = messageType == TYPE_TEXT

    /** true if this message contains an image attachment. */
    val isImageMessage: Boolean
        get() = messageType == TYPE_IMAGE

    /** true if this message contains a document/file attachment. */
    val isDocumentMessage: Boolean
        get() = messageType == TYPE_DOCUMENT

    /** true if this message has not yet been read by the recipient. */
    val isUnread: Boolean
        get() = !isRead

    /**
     * Returns a short preview string for use in conversation list items.
     * Shows "📷 Photo" for images, "📄 Document" for files, or the text content.
     * Truncates long text to 50 characters.
     */
    val preview: String
        get() = when (messageType) {
            TYPE_IMAGE    -> "📷 Photo"
            TYPE_DOCUMENT -> "📄 ${mediaFileName ?: "Document"}"
            else          -> if (content.length > 50) "${content.take(50)}…" else content
        }

    /**
     * Checks whether this message was sent by the given user.
     * Used in the chat UI to determine message bubble alignment (left/right).
     *
     * @param userId The UID of the currently authenticated user.
     * @return true if this message was sent by [userId].
     */
    fun isSentBy(userId: String): Boolean = senderId == userId
}

