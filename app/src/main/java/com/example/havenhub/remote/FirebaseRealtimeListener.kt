package com.example.havenhub.remote

import android.util.Log
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

    // ══════════════════════════════════════════════════════════════════════════
    // MESSAGING
    // ══════════════════════════════════════════════════════════════════════════

    fun listenToMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToMessages: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Message::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ✦ NEW — USER PRESENCE (Online / Last Seen)
    //
    // Firebase Realtime Database mein user presence track karta hai.
    // Structure:
    //   /status/{userId}/
    //     - isOnline: Boolean
    //     - lastSeen: Long (epoch ms)
    //
    // ChatScreen header mein "Online" ya "Last seen 10:30 AM" dikhane ke liye.
    // ══════════════════════════════════════════════════════════════════════════

    data class UserPresence(
        val isOnline: Boolean = false,
        val lastSeen: Long    = 0L
    )

    fun listenToUserPresence(userId: String): Flow<UserPresence> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(UserPresence())
            awaitClose()
            return@callbackFlow
        }

        val reg: ListenerRegistration = firestore
            .collection("status")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToUserPresence: ${error.localizedMessage}")
                    trySend(UserPresence())
                    return@addSnapshotListener
                }
                val isOnline = snapshot?.getBoolean("isOnline") ?: false
                val lastSeen = snapshot?.getLong("lastSeen") ?: 0L
                trySend(UserPresence(isOnline = isOnline, lastSeen = lastSeen))
            }
        awaitClose { reg.remove() }
    }

    /**
     * Apni presence Firestore mein update karo.
     * Call karo: app foreground mein aaye toh isOnline=true,
     * background/close hone pe isOnline=false + lastSeen=now.
     */
    fun updateMyPresence(userId: String, isOnline: Boolean) {
        if (userId.isEmpty()) return
        val data = if (isOnline) {
            mapOf("isOnline" to true, "lastSeen" to System.currentTimeMillis())
        } else {
            mapOf("isOnline" to false, "lastSeen" to System.currentTimeMillis())
        }
        firestore.collection("status").document(userId).set(data)
            .addOnFailureListener { Log.e("REALTIME", "updateMyPresence failed: ${it.localizedMessage}") }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATIONS — ✅ BUG FIX
    //
    // PROBLEM WAS:
    //   • Bell badge   → listenToAdminNotifications() → filter: targetRole == "admin"
    //   • Screen count → listenToNotifications(userId) → filter: recipientId == userId
    //   Dono ALAG queries the, isliye counts mismatch hote the (bell=5, screen=2).
    //
    // FIX:
    //   • Bell badge (DashboardViewModel) → unreadNotifCount ab
    //     listenToAdminNotifications() se aata hai (targetRole == "admin")
    //     lekin yeh SIRF admin dashboard activity feed ke liye hai.
    //
    //   • AdminTopBar notification badge ab DashboardViewModel.unreadNotifCount
    //     use karta hai jo sirf admin-targeted notifications count karta hai.
    //
    //   • NotificationsScreen → listenToNotifications(userId) use karta hai
    //     jo recipientId == userId filter karta hai — screen pe wahi dikhega
    //     jo is admin user ko bheja gaya hai.
    //
    //   SOLUTION: Admin ke liye dono same user ka userId use karein.
    //   DashboardViewModel mein unreadNotifCount ab userId-based count se
    //   aata hai, targetRole se nahi. Yeh fix DashboardViewModel mein hai.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * User-specific notifications (recipientId == userId).
     * NotificationsScreen + DashboardViewModel unread badge dono yahi use karein.
     * orderBy hata diya — composite index issue avoid karne ke liye, client-side sort.
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
            // ✅ orderBy removed — no composite index needed
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToNotifications: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot
                    ?.toObjects(Notification::class.java)
                    ?.sortedByDescending { it.createdAt?.seconds }  // ✅ client-side sort
                    ?: emptyList()
                Log.d("REALTIME", "✅ ${list.size} notifications | unread: ${list.count { !it.isRead }} | userId: $userId")
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Admin activity feed — targetRole == "admin" wali notifications.
     * ONLY dashboard "Recent Activity" section ke liye use karo.
     * Badge count ke liye mat use karo — userId-based listenToNotifications use karo.
     */
    fun listenToAdminNotifications(): Flow<List<Notification>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("targetRole", "admin")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToAdminNotifications: ${error.localizedMessage}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot
                    ?.toObjects(Notification::class.java)
                    ?.sortedByDescending { it.createdAt?.seconds }
                    ?: emptyList()
                Log.d("REALTIME", "✅ Admin activity feed: ${list.size}")
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Dashboard recent activity section — last 15 admin notifications.
     */
    fun listenToRecentActivities(): Flow<List<Notification>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("targetRole", "admin")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("REALTIME", "listenToRecentActivities: ${error.localizedMessage}")
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
    // BOOKINGS
    // ══════════════════════════════════════════════════════════════════════════

    fun getBookingsFlow(userId: String): Flow<List<Booking>> = callbackFlow {
        if (userId.isEmpty()) { trySend(emptyList()); awaitClose(); return@callbackFlow }
        val reg: ListenerRegistration = firestore
            .collection("bookings")
            .whereEqualTo("tenantId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("REALTIME", "getBookingsFlow: ${error.localizedMessage}"); trySend(emptyList()); return@addSnapshotListener }
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
                if (error != null) { Log.e("REALTIME", "listenToPropertyBookings: ${error.localizedMessage}"); trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Booking::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    fun listenToAllBookings(): Flow<List<Booking>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection("bookings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("REALTIME", "listenToAllBookings: ${error.localizedMessage}"); trySend(emptyList()); return@addSnapshotListener }
                val bookings = try { snapshot?.toObjects(Booking::class.java) ?: emptyList() }
                catch (e: Exception) { Log.e("REALTIME", "Booking parse: ${e.localizedMessage}"); emptyList() }
                Log.d("REALTIME", "✅ ${bookings.size} total bookings")
                trySend(bookings)
            }
        awaitClose { reg.remove() }
    }

    fun listenToLandlordBookings(landlordId: String): Flow<List<Booking>> = callbackFlow {
        if (landlordId.isEmpty()) { trySend(emptyList()); awaitClose(); return@callbackFlow }
        val reg: ListenerRegistration = firestore
            .collection("bookings")
            .whereEqualTo("landlordId", landlordId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("REALTIME", "listenToLandlordBookings: ${error.localizedMessage}"); trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Booking::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }
}











