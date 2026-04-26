package com.example.havenhub.remote

import com.example.havenhub.data.Booking
import com.example.havenhub.data.Message
import com.example.havenhub.data.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRealtimeListener @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ── Messaging Listener ──────────────────────────────────────────────────

    fun listenToMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val ref = firestore
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(Message::class.java) ?: emptyList())
        }

        awaitClose { registration.remove() }
    }

    // ── Notifications Listeners ──────────────────────────────────────────────

    fun listenToAdminNotifications(): Flow<List<Notification>> = callbackFlow {
        val ref = firestore
            .collection("notifications")
            .whereEqualTo("targetRole", "admin")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
            trySend(notifications)
        }

        awaitClose { registration.remove() }
    }

    fun listenToNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val ref = firestore
            .collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(Notification::class.java) ?: emptyList())
        }

        awaitClose { registration.remove() }
    }

    // ── Booking Listeners ───────────────────────────────────────────────────

    fun getBookingsFlow(userId: String): Flow<List<Booking>> = callbackFlow {
        val ref = firestore
            .collection("bookings")
            .whereIn("tenantId", listOf(userId))
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val bookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
            trySend(bookings)
        }

        awaitClose { registration.remove() }
    }

    fun listenToPropertyBookings(propertyId: String): Flow<List<Booking>> = callbackFlow {
        val ref = firestore
            .collection("bookings")
            .whereEqualTo("propertyId", propertyId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val bookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
            trySend(bookings)
        }

        awaitClose { registration.remove() }
    }

    // ── ✅ NEW: Admin — All Bookings Real-time ──────────────────────────────
    // Admin ko saare bookings chahiye bina kisi filter ke

    fun listenToAllBookings(): Flow<List<Booking>> = callbackFlow {
        val ref = firestore
            .collection("bookings")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val bookings = try {
                snapshot?.toObjects(Booking::class.java) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            trySend(bookings)
        }

        awaitClose { registration.remove() }
    }

    // ── ✅ NEW: Admin — Recent Activities Real-time ─────────────────────────
    // Notifications collection ko hi activities ki tarah use karenge (targetRole = "admin")
    // Naya log karne ke liye NotificationRepository mein add karna hoga

    fun listenToRecentActivities(): Flow<List<Notification>> = callbackFlow {
        val ref = firestore
            .collection("notifications")
            .whereEqualTo("targetRole", "admin")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(15)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val activities = snapshot?.toObjects(Notification::class.java) ?: emptyList()
            trySend(activities)
        }

        awaitClose { registration.remove() }
    }
}











