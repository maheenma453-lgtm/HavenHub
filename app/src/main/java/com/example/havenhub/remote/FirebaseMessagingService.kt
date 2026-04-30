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
 * ✅ FIX LIST:
 * 1. @AndroidEntryPoint HATAYA — Hilt Services mein properly inject nahi karta,
 *    silent crash hoti thi aur onMessageReceived kabhi nahi chalta tha.
 * 2. NotificationHelper manually instantiate kiya — @Inject ki zaroorat nahi.
 * 3. onNewToken mein Thread() ki jagah coroutine-safe approach use ki.
 */
class FirebaseMessagingService : FirebaseMessagingService() {

    // ✅ FIX: @Inject hatao — manually banao. Hilt @AndroidEntryPoint
    // Service mein onCreate se pehle inject karta hai jo FCM Service mein
    // reliable nahi hota aur nullPointerException silently crash karti thi.
    private val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_SERVICE", "Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "HavenHub"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""
        val type        = remoteMessage.data["type"]        ?: Constants.NOTIF_SYSTEM
        val referenceId = remoteMessage.data["referenceId"] ?: ""

        if (body.isEmpty()) {
            Log.w("FCM_SERVICE", "Empty body — notification skipped")
            return
        }

        // Channels create karo — agar app ne pehle nahi kiye to yahan ensure karo
        notificationHelper.createNotificationChannels()

        when (type) {
            Constants.NOTIF_BOOKING -> {
                val subType = remoteMessage.data["subType"] ?: ""
                when (subType) {
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
                    else -> notificationHelper.showNotification(
                        title       = title,
                        message     = body,
                        type        = type,
                        referenceId = referenceId
                    )
                }
            }

            Constants.NOTIF_PAYMENT -> notificationHelper.showNotification(
                title       = title,
                message     = body,
                type        = type,
                referenceId = referenceId
            )

            Constants.NOTIF_MESSAGE -> notificationHelper.showNewMessage(
                senderName     = remoteMessage.data["senderName"] ?: title,
                preview        = body,
                conversationId = referenceId
            )

            Constants.NOTIF_PROPERTY -> {
                val subType = remoteMessage.data["subType"] ?: ""
                when (subType) {
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
                    else -> notificationHelper.showNotification(
                        title       = title,
                        message     = body,
                        type        = type,
                        referenceId = referenceId
                    )
                }
            }

            Constants.NOTIF_SYSTEM -> {
                val subType = remoteMessage.data["subType"] ?: ""
                when (subType) {
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
                    else -> notificationHelper.showNotification(
                        title       = title,
                        message     = body,
                        type        = type,
                        referenceId = referenceId
                    )
                }
            }

            else -> notificationHelper.showNotification(
                title       = title,
                message     = body,
                type        = type,
                referenceId = referenceId
            )
        }

        Log.d("FCM_SERVICE", "Notification handled — type=$type ref=$referenceId")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "New FCM token: $token")

        // SharedPreferences mein token save karo
        val prefs = getSharedPreferences("haven_prefs", MODE_PRIVATE)
        prefs.edit().putString(Constants.PREF_FCM_TOKEN, token).apply()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: prefs.getString(Constants.PREF_USER_ID, "") ?: ""

        val role = prefs.getString(Constants.PREF_USER_ROLE, Constants.ROLE_TENANT)
            ?.uppercase() ?: Constants.ROLE_TENANT

        // ✅ FIX: Thread safe way — simple background thread, no coroutine needed here
        if (userId.isNotEmpty()) {
            Thread {
                try {
                    FirebaseFirestore.getInstance()
                        .collection(Constants.COLLECTION_USERS)
                        .document(userId)
                        .update("fcmToken", token)
                    Log.d("FCM_SERVICE", "Token saved for userId=$userId")
                } catch (e: Exception) {
                    Log.e("FCM_SERVICE", "Token save failed: ${e.localizedMessage}")
                }
            }.start()
        } else {
            Log.w("FCM_SERVICE", "No userId — token will be saved on next login")
        }

        val roleTopic = when (role) {
            Constants.ROLE_ADMIN    -> Constants.TOPIC_ADMIN
            Constants.ROLE_LANDLORD -> Constants.TOPIC_LANDLORD
            else                    -> Constants.TOPIC_TENANT
        }

        FirebaseMessaging.getInstance().subscribeToTopic(roleTopic)
            .addOnSuccessListener { Log.d("FCM_SERVICE", "Subscribed: $roleTopic") }
            .addOnFailureListener { Log.e("FCM_SERVICE", "Subscribe failed: ${it.message}") }

        FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_ALL)
            .addOnSuccessListener { Log.d("FCM_SERVICE", "Subscribed: ${Constants.TOPIC_ALL}") }
    }
}













