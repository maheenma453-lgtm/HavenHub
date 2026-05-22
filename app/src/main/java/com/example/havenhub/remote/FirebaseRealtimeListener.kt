package com.example.havenhub.remote

import android.util.Log
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Message
import com.example.havenhub.data.Notification
import com.example.havenhub.data.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
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
    private val firestore       : FirebaseFirestore,
    private val realtimeDatabase: FirebaseDatabase
) {

    fun listenToMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToMessages error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Message::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    data class UserPresence(
        val isOnline: Boolean = false,
        val lastSeen: Long = 0L
    )

    fun listenToUserPresence(userId: String): Flow<UserPresence> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(UserPresence())
            awaitClose()
            return@callbackFlow
        }

        val presenceRef: DatabaseReference = realtimeDatabase
            .getReference("status")
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                Log.d("PRESENCE", "userId=$userId isOnline=$isOnline lastSeen=$lastSeen")
                trySend(UserPresence(isOnline = isOnline, lastSeen = lastSeen))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PRESENCE", "listenToUserPresence cancelled: ${error.message}")
                trySend(UserPresence())
            }
        }

        presenceRef.addValueEventListener(listener)
        awaitClose { presenceRef.removeEventListener(listener) }
    }

    fun updateMyPresence(userId: String, isOnline: Boolean) {
        if (userId.isEmpty()) return

        val presenceRef: DatabaseReference = realtimeDatabase
            .getReference("status")
            .child(userId)

        if (isOnline) {
            presenceRef.onDisconnect().updateChildren(
                mapOf(
                    "isOnline" to false,
                    "lastSeen" to ServerValue.TIMESTAMP
                )
            ).addOnSuccessListener {
                presenceRef.updateChildren(
                    mapOf(
                        "isOnline" to true,
                        "lastSeen" to ServerValue.TIMESTAMP
                    )
                ).addOnFailureListener {
                    Log.e("PRESENCE", "updateMyPresence online=true failed: ${it.localizedMessage}")
                }
            }.addOnFailureListener {
                Log.e("PRESENCE", "onDisconnect registration failed: ${it.localizedMessage}")
            }
        } else {
            presenceRef.onDisconnect().cancel()
            presenceRef.updateChildren(
                mapOf(
                    "isOnline" to false,
                    "lastSeen" to ServerValue.TIMESTAMP
                )
            ).addOnFailureListener {
                Log.e("PRESENCE", "updateMyPresence online=false failed: ${it.localizedMessage}")
            }
        }
    }

    fun listenToNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }
        val reg: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("recipientId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToNotifications error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot
                    ?.toObjects(Notification::class.java)
                    ?.sortedByDescending { it.createdAt?.seconds }
                    ?: emptyList()

                Log.d(
                    "REALTIME",
                    "Notifications: ${list.size} total | unread: ${list.count { !it.isRead }}"
                )
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun listenToAdminNotifications(): Flow<List<Notification>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("targetRole", "admin")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToAdminNotifications error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot
                    ?.toObjects(Notification::class.java)
                    ?.sortedByDescending { it.createdAt?.seconds }
                    ?: emptyList()

                Log.d("REALTIME", "Admin activity feed: ${list.size} notifications")
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun listenToRecentActivities(): Flow<List<Notification>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("targetRole", "admin")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToRecentActivities error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot
                    ?.toObjects(Notification::class.java)
                    ?.sortedByDescending { it.createdAt?.seconds }
                    ?.take(15)
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun getBookingsFlow(userId: String): Flow<List<Booking>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList()); awaitClose(); return@callbackFlow
        }
        val reg: ListenerRegistration = firestore
            .collection("bookings")
            .whereEqualTo("tenantId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "getBookingsFlow error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Booking::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    fun listenToPropertyBookings(propertyId: String): Flow<List<Booking>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("bookings")
            .whereEqualTo("propertyId", propertyId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToPropertyBookings error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Booking::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    fun listenToAllBookings(): Flow<List<Booking>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("bookings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToAllBookings error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val bookings = try {
                    snapshot?.toObjects(Booking::class.java) ?: emptyList()
                } catch (e: Exception) {
                    Log.e("REALTIME", "Booking parse error: ${e.localizedMessage}")
                    emptyList()
                }
                Log.d("REALTIME", "All bookings: ${bookings.size}")
                trySend(bookings)
            }
        awaitClose { reg.remove() }
    }

    fun listenToLandlordBookings(landlordId: String): Flow<List<Booking>> = callbackFlow {
        if (landlordId.isEmpty()) {
            trySend(emptyList()); awaitClose(); return@callbackFlow
        }
        val reg: ListenerRegistration = firestore
            .collection("bookings")
            .whereEqualTo("landlordId", landlordId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToLandlordBookings error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Booking::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    // ── FIX: orderBy hata diya — createdAt missing documents bhi count honge ──
    fun listenToAllUsers(): Flow<List<User>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToAllUsers error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = try {
                    snapshot?.toObjects(User::class.java) ?: emptyList()
                } catch (e: Exception) {
                    Log.e("REALTIME", "User parse error: ${e.localizedMessage}")
                    emptyList()
                }
                Log.d("REALTIME", "All users realtime: ${users.size}")
                trySend(users)
            }
        awaitClose { reg.remove() }
    }
}