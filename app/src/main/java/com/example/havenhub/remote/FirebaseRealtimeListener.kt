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

    fun listenToNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val ref = firestore
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(Notification::class.java) ?: emptyList())
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
            val bookings = snapshot?.documents?.mapNotNull {
                try { it.toObject(Booking::class.java) } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(bookings)
        }

        awaitClose { registration.remove() }
    }

    fun listenToUserBookings(userId: String): Flow<List<Booking>> = callbackFlow {
        val ref = firestore
            .collection("bookings")
            .whereEqualTo("tenantId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val bookings = snapshot?.documents?.mapNotNull {
                try { it.toObject(Booking::class.java) } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(bookings)
        }

        awaitClose { registration.remove() }
    }
}