package com.example.havenhub.remote
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.havenhub.data.model.Booking
import com.havenhub.data.model.Message
import com.havenhub.data.model.Notification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseRealtimeListener
 *
 * Provides real-time Firestore snapshot listeners exposed as Kotlin [Flow]s.
 * Used wherever the UI needs live updates without manual polling:
 *  - Chat messages
 *  - Booking status changes
 *  - Incoming notifications
 *
 * Each Flow automatically:
 *  - Attaches a Firestore listener on collection
 *  - Emits updates whenever the underlying data changes
 *  - Removes the listener when the Flow is cancelled (e.g., screen leaves composition)
 *
 * The [callbackFlow] builder bridges the Firestore callback API into structured coroutines.
 */
@Singleton
class FirebaseRealtimeListener @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Chat / Messaging
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Listens in real-time to messages in a specific conversation thread.
     *
     * Conversation ID convention: sorted concatenation of both user UIDs,
     * e.g., "uid1_uid2" (sorted alphabetically to ensure uniqueness).
     *
     * @param conversationId Unique ID of the conversation.
     * @return A [Flow] emitting the updated list of [Message] on every change.
     *         Emits an empty list on snapshot errors to avoid crashing the UI.
     */
    fun listenToMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        // Build path: conversations/{conversationId}/messages
        val messagesRef = firestore
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        // Attach snapshot listener — fires immediately with current data,
        // then again on every write to this collection.
        val registration: ListenerRegistration = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Emit empty list rather than crashing; log error if needed
                trySend(emptyList())
                return@addSnapshotListener
            }
            val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
            trySend(messages)
        }

        // awaitClose is called when the Flow collector is cancelled.
        // Remove the Firestore listener to avoid memory leaks.
        awaitClose { registration.remove() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notifications
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Listens in real-time to unread notifications for a specific user.
     * Ordered by timestamp (newest first) to surface recent alerts at the top.
     *
     * @param userId The Firebase Auth UID of the target user.
     * @return A [Flow] emitting a list of [Notification] on every change.
     */
    fun listenToNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val notificationsRef = firestore
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = notificationsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
            trySend(notifications)
        }

        awaitClose { registration.remove() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Booking Status
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Listens in real-time to all bookings associated with a specific property.
     * Useful for hosts to track incoming/pending booking requests without refresh.
     *
     * @param propertyId The Firestore document ID of the property.
     * @return A [Flow] emitting the list of [Booking] on every change.
     */
    fun listenToPropertyBookings(propertyId: String): Flow<List<Booking>> = callbackFlow {
        val bookingsRef = firestore
            .collection("bookings")
            .whereEqualTo("propertyId", propertyId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = bookingsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val bookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
            trySend(bookings)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Listens in real-time to all bookings made by a specific tenant/user.
     * Allows renters to see live status updates (confirmed, cancelled, etc.).
     *
     * @param userId The Firebase Auth UID of the tenant.
     * @return A [Flow] emitting the list of [Booking] on every change.
     */
    fun listenToUserBookings(userId: String): Flow<List<Booking>> = callbackFlow {
        val bookingsRef = firestore
            .collection("bookings")
            .whereEqualTo("tenantId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = bookingsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val bookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
            trySend(bookings)
        }

        awaitClose { registration.remove() }
    }
}


