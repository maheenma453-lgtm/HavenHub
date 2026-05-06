package com.example.havenhub.data

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val propertyId: String = "",        // Track which property is being discussed
    val propertyTitle: String = "",     // Display title in chat list
    val content: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val messageType: String = TYPE_TEXT,
    val mediaUrl: String? = null,
    val mediaFileName: String? = null,

    // ✦ NEW — UI-only field, Firestore mein save NAHI hota
    // Long press selection mode ke liye use hota hai
    @field:com.google.firebase.firestore.Exclude
    @get:com.google.firebase.firestore.Exclude
    val isSelected: Boolean = false
) {
    // Message list mein preview dikhane ke liye
    val preview: String
        get() = when (messageType) {
            TYPE_IMAGE    -> "📷 Photo"
            TYPE_DOCUMENT -> "📄 Document"
            else          -> content
        }

    companion object {
        const val TYPE_TEXT     = "text"
        const val TYPE_IMAGE    = "image"
        const val TYPE_DOCUMENT = "document"

        // Unique ID generator: Tenant + Landlord + Property combo
        fun buildConversationId(u1: String, u2: String, pId: String): String {
            val sortedIds = listOf(u1, u2).sorted()
            return "${sortedIds[0]}_${sortedIds[1]}_$pId"
        }
    }
}