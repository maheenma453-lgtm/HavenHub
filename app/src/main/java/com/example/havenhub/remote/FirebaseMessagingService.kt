package com.example.havenhub.remote

import android.util.Log
import com.example.havenhub.utils.Constants
import com.example.havenhub.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * HavenHub FCM push notification receiver.
 *
 * FIX 1 — Renamed from FirebaseMessagingService to HavenHubMessagingService.
 *          The old class name matched the parent class name exactly, causing
 *          a silent class resolution conflict. Android resolved the wrong class
 *          after the first install, so push notifications stopped arriving.
 *          Renaming the class permanently fixes this.
 *
 * FIX 2 — @AndroidEntryPoint removed. Hilt injects via onCreate which is called
 *          AFTER FCM delivers the message. Using lazy init avoids the null crash.
 */
class HavenHubMessagingService : FirebaseMessagingService() {

    private val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(applicationContext)
    }

    // ------------------------------------------------------------------
    // Called every time a push message arrives (foreground or background)
    // ------------------------------------------------------------------
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        val title       = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "HavenHub"
        val body        = remoteMessage.notification?.body  ?: remoteMessage.data["body"]  ?: ""
        val type        = remoteMessage.data["type"]        ?: Constants.NOTIF_SYSTEM
        val referenceId = remoteMessage.data["referenceId"] ?: ""

        if (body.isEmpty()) {
            Log.w("FCM", "Empty body — notification skipped")
            return
        }

        // Ensure channels exist before posting any notification
        notificationHelper.createNotificationChannels()

        when (type) {

            Constants.NOTIF_BOOKING -> {
                when (remoteMessage.data["subType"] ?: "") {
                    "CONFIRMED" -> notificationHelper.showBookingConfirmed(
                        propertyName = remoteMessage.data["propertyTitle"] ?: "",
                        bookingId    = referenceId
                    )
                    "CANCELLED" -> notificationHelper.showBookingCancelled(
                        propertyName = remoteMessage.data["propertyTitle"] ?: "",
                        bookingId    = referenceId
                    )
                    "REQUESTED" -> notificationHelper.showNewBookingRequest(
                        propertyName = remoteMessage.data["propertyTitle"] ?: "",
                        tenantName   = remoteMessage.data["tenantName"]    ?: "",
                        bookingId    = referenceId
                    )
                    "REMINDER"  -> notificationHelper.showCheckInReminder(
                        propertyName = remoteMessage.data["propertyTitle"] ?: "",
                        bookingId    = referenceId
                    )
                    else -> notificationHelper.showNotification(title, body, type, referenceId)
                }
            }

            Constants.NOTIF_PROPERTY -> {
                when (remoteMessage.data["subType"] ?: "") {
                    "APPROVED" -> notificationHelper.showPropertyApproved(
                        propertyTitle = remoteMessage.data["propertyTitle"] ?: "",
                        propertyId    = referenceId,
                        adminNote     = remoteMessage.data["adminNote"]     ?: ""
                    )
                    "REJECTED" -> notificationHelper.showPropertyRejected(
                        propertyTitle = remoteMessage.data["propertyTitle"] ?: "",
                        propertyId    = referenceId,
                        adminNote     = remoteMessage.data["adminNote"]     ?: ""
                    )
                    "PENDING"  -> notificationHelper.showNewPropertyPending(
                        propertyTitle = remoteMessage.data["propertyTitle"] ?: "",
                        landlordName  = remoteMessage.data["landlordName"]  ?: "",
                        propertyId    = referenceId
                    )
                    else -> notificationHelper.showNotification(title, body, type, referenceId)
                }
            }

            Constants.NOTIF_SYSTEM -> {
                when (remoteMessage.data["subType"] ?: "") {
                    "USER_VERIFIED" -> notificationHelper.showUserVerified(
                        userName = remoteMessage.data["userName"] ?: ""
                    )
                    "USER_REJECTED" -> notificationHelper.showUserRejected(
                        userName = remoteMessage.data["userName"] ?: "",
                        reason   = remoteMessage.data["reason"]   ?: ""
                    )
                    "USER_PENDING"  -> notificationHelper.showNewUserPending(
                        userName = remoteMessage.data["userName"] ?: "",
                        userId   = referenceId
                    )
                    else -> notificationHelper.showNotification(title, body, type, referenceId)
                }
            }

            Constants.NOTIF_MESSAGE -> notificationHelper.showNewMessage(
                senderName     = remoteMessage.data["senderName"] ?: title,
                preview        = body,
                conversationId = referenceId
            )

            else -> notificationHelper.showNotification(title, body, type, referenceId)
        }

        Log.d("FCM", "Notification posted — type=$type referenceId=$referenceId")
    }

    // ------------------------------------------------------------------
    // Called when FCM issues a new device token
    // ------------------------------------------------------------------
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token issued: $token")

        // Save token locally so it can be uploaded on next login if user is signed out
        getSharedPreferences("haven_prefs", MODE_PRIVATE)
            .edit().putString(Constants.PREF_FCM_TOKEN, token).apply()

        val prefs  = getSharedPreferences("haven_prefs", MODE_PRIVATE)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: prefs.getString(Constants.PREF_USER_ID, "") ?: ""
        val role   = prefs.getString(Constants.PREF_USER_ROLE, Constants.ROLE_TENANT)
            ?.uppercase() ?: Constants.ROLE_TENANT

        // Upload token to Firestore in a background thread (no coroutine needed here)
        if (userId.isNotEmpty()) {
            Thread {
                try {
                    FirebaseFirestore.getInstance()
                        .collection(Constants.COLLECTION_USERS)
                        .document(userId)
                        .update("fcmToken", token)
                    Log.d("FCM", "Token saved for userId=$userId")
                } catch (e: Exception) {
                    Log.e("FCM", "Token save failed: ${e.message}")
                }
            }.start()
        } else {
            Log.w("FCM", "No userId available — token will be saved on next login")
        }

        // Subscribe to role topic so server can send targeted push messages
        val roleTopic = when (role) {
            Constants.ROLE_ADMIN    -> Constants.TOPIC_ADMIN
            Constants.ROLE_LANDLORD -> Constants.TOPIC_LANDLORD
            else                    -> Constants.TOPIC_TENANT
        }

        FirebaseMessaging.getInstance().subscribeToTopic(roleTopic)
            .addOnSuccessListener { Log.d("FCM", "Subscribed to topic: $roleTopic") }
            .addOnFailureListener { Log.e("FCM", "Topic subscribe failed: ${it.message}") }

        FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ALL)
            .addOnSuccessListener { Log.d("FCM", "Subscribed to topic: ${Constants.TOPIC_ALL}") }
    }
}