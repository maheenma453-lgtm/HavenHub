package com.example.havenhub.remote

import android.util.Log
import com.example.havenhub.data.Booking
import com.example.havenhub.data.Message
import com.example.havenhub.data.Notification
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

// ══════════════════════════════════════════════════════════════════════════════
// FirebaseRealtimeListener
//
// This class handles two separate Firebase systems:
//   1. Firestore  → Messages, Notifications, Bookings (real-time listeners)
//   2. Realtime Database → User Online Presence (isOnline / lastSeen)
//
// Why Realtime DB for presence?
//   Firestore does NOT support .onDisconnect() — so if the app crashes or
//   loses connection, Firestore cannot automatically mark the user offline.
//   Firebase Realtime Database has built-in .onDisconnect() which runs
//   server-side even if the client disconnects abruptly.
// ══════════════════════════════════════════════════════════════════════════════

@Singleton
class FirebaseRealtimeListener @Inject constructor(
    private val firestore       : FirebaseFirestore,
    private val realtimeDatabase: FirebaseDatabase        // ✦ Injected via FirebaseModule
) {

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 1 — MESSAGING (Firestore)
    //
    // Listens to messages inside a conversation document in real-time.
    // Path: conversations/{conversationId}/messages
    // Ordered by timestamp ascending so newest message appears at bottom.
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 2 — USER PRESENCE (Firebase Realtime Database)
    //
    // Realtime DB path: /status/{userId}/
    //   - isOnline : Boolean  → true if user is currently in the app
    //   - lastSeen : Long     → epoch milliseconds of last seen time
    //
    // How it works:
    //   • When user opens the app → set isOnline=true via updateMyPresence()
    //   • When user closes/backgrounds app → set isOnline=false manually
    //   • If app CRASHES or network drops → .onDisconnect() runs server-side
    //     automatically and sets isOnline=false + lastSeen=ServerValue.TIMESTAMP
    //
    // This guarantees the user is never stuck as "Online" after a crash.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Data class representing another user's online presence state.
     * Used in MessagingUiState and displayed in ChatScreen header.
     */
    data class UserPresence(
        val isOnline: Boolean = false,
        val lastSeen: Long = 0L
    )

    /**
     * Observe another user's online/lastSeen status in real-time.
     * Call this when ChatScreen opens with the other user's ID.
     *
     * Returns a Flow<UserPresence> that emits whenever the other user's
     * presence changes (they come online, go offline, etc.).
     */
    fun listenToUserPresence(userId: String): Flow<UserPresence> = callbackFlow {
        // Guard: empty userId — emit default (offline) and close
        if (userId.isEmpty()) {
            trySend(UserPresence())
            awaitClose()
            return@callbackFlow
        }

        // Reference to /status/{userId} in Realtime Database
        val presenceRef: DatabaseReference = realtimeDatabase
            .getReference("status")
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Read isOnline field — default false if missing
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false

                // Read lastSeen field — default 0 if missing
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L

                Log.d("PRESENCE", "userId=$userId isOnline=$isOnline lastSeen=$lastSeen")
                trySend(UserPresence(isOnline = isOnline, lastSeen = lastSeen))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PRESENCE", "listenToUserPresence cancelled: ${error.message}")
                trySend(UserPresence())   // emit offline on error
            }
        }

        presenceRef.addValueEventListener(listener)

        // When the Flow collector cancels (ChatScreen leaves composition)
        // remove the listener to avoid memory leaks
        awaitClose { presenceRef.removeEventListener(listener) }
    }

    /**
     * Update the current logged-in user's presence in Realtime Database.
     *
     * Call with isOnline = true  → when user opens the app / returns to foreground
     * Call with isOnline = false → when user goes to background or logs out
     *
     * IMPORTANT: This also sets up .onDisconnect() so the server automatically
     * marks the user offline if the connection drops unexpectedly (crash, no internet).
     *
     * The .onDisconnect() is re-registered every time this function is called
     * with isOnline = true, which is the correct pattern.
     */
    fun updateMyPresence(userId: String, isOnline: Boolean) {
        if (userId.isEmpty()) return

        // Reference to /status/{userId} in Realtime Database
        val presenceRef: DatabaseReference = realtimeDatabase
            .getReference("status")
            .child(userId)

        if (isOnline) {
            // ── Going Online ──────────────────────────────────────────────────
            // Step 1: Register .onDisconnect() BEFORE setting online=true.
            //         This tells the Firebase server: "If this client disconnects
            //         for any reason (crash, network loss), run this update."
            //         ServerValue.TIMESTAMP = server fills in the exact time.
            presenceRef.onDisconnect().updateChildren(
                mapOf(
                    "isOnline" to false,
                    "lastSeen" to ServerValue.TIMESTAMP   // server-side timestamp
                )
            ).addOnSuccessListener {
                // Step 2: Only after .onDisconnect() is registered, set online=true.
                //         This ordering matters — if we set online first and then
                //         .onDisconnect() registration fails, user stays stuck as online.
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
            // ── Going Offline (manual) ────────────────────────────────────────
            // Called when user backgrounds the app or logs out gracefully.
            // Cancel any pending .onDisconnect() (not strictly needed but clean).
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

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 3 — NOTIFICATIONS (Firestore)
    //
    // BUG FIX NOTES (kept from original):
    //   • Bell badge uses listenToNotifications(userId) — recipientId filter
    //   • Admin activity feed uses listenToAdminNotifications() — targetRole filter
    //   • Both are separate flows to avoid count mismatch between badge & screen
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Listen to notifications for a specific user (recipientId == userId).
     * Used by: NotificationsScreen + DashboardViewModel unread badge.
     * orderBy removed to avoid composite index requirement — sorted client-side.
     */
    fun listenToNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }
        val reg: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("recipientId", userId)
            // orderBy intentionally removed — no composite index needed
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToNotifications error: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Sort client-side by createdAt descending
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

    /**
     * Admin activity feed — only notifications with targetRole == "admin".
     * Used ONLY for the dashboard recent activity section.
     * Do NOT use this for the notification badge count.
     */
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

    /**
     * Dashboard recent activity — last 15 admin-targeted notifications.
     * Same as listenToAdminNotifications but limited to 15 items client-side.
     */
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

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION 4 — BOOKINGS (Firestore)
    //
    // Real-time booking listeners for tenant, landlord, and admin views.
    // All use Firestore snapshots with orderBy createdAt descending.
    // ══════════════════════════════════════════════════════════════════════════

    /** Tenant's own bookings (tenantId == userId), newest first. */
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

    /** All bookings for a specific property — used by landlord and admin. */
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

    /** All bookings in the system — admin dashboard use only. */
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

    /** Landlord's incoming bookings (landlordId == landlordId), newest first. */
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
}