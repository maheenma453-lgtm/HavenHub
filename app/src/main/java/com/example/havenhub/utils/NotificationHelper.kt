package com.example.havenhub.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.havenhub.MainActivity
import com.example.havenhub.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_BOOKINGS = "channel_bookings"
        const val CHANNEL_PAYMENTS = "channel_payments"
        const val CHANNEL_MESSAGES = "channel_messages"
        const val CHANNEL_SYSTEM   = "channel_system"
        const val CHANNEL_PROPERTY = "channel_property"

        private val notificationIdCounter = AtomicInteger(1000)
        fun nextId() = notificationIdCounter.getAndIncrement()
    }

    // ── Create all channels ───────────────────────────────────────────────────
    // FIX: lockscreenVisibility = VISIBILITY_PUBLIC added so notifications show
    //      on lock screen (like Snapchat). Without this they were hidden on lock screen.
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_BOOKINGS,
                        "Bookings",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description         = "Booking confirmations, reminders and cancellations"
                        enableVibration(true)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC   // ✅ lock screen fix
                    },

                    NotificationChannel(
                        CHANNEL_PAYMENTS,
                        "Payments",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description          = "Payment receipts and confirmations"
                        enableVibration(true)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC   // ✅
                    },

                    NotificationChannel(
                        CHANNEL_MESSAGES,
                        "Messages",
                        NotificationManager.IMPORTANCE_HIGH                           // ✅ DEFAULT → HIGH (popup fix)
                    ).apply {
                        description          = "New messages from hosts and tenants"
                        enableVibration(true)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC   // ✅
                    },

                    NotificationChannel(
                        CHANNEL_SYSTEM,
                        "System",
                        NotificationManager.IMPORTANCE_HIGH                           // ✅ LOW → HIGH
                    ).apply {
                        description          = "App updates and announcements"
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC   // ✅
                    },

                    NotificationChannel(
                        CHANNEL_PROPERTY,
                        "Property Updates",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description          = "Property approval and rejection alerts"
                        enableVibration(true)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC   // ✅
                    }
                )
            )
        }
    }

    // ── Core show notification ────────────────────────────────────────────────
    fun showNotification(
        title      : String,
        message    : String,
        type       : String = Constants.NOTIF_SYSTEM,
        referenceId: String = ""
    ) {
        // Android 13+ POST_NOTIFICATIONS permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                android.util.Log.w("NotificationHelper", "POST_NOTIFICATIONS not granted — skipping")
                return
            }
        }

        val channelId = when (type) {
            Constants.NOTIF_BOOKING  -> CHANNEL_BOOKINGS
            Constants.NOTIF_PAYMENT  -> CHANNEL_PAYMENTS
            Constants.NOTIF_MESSAGE  -> CHANNEL_MESSAGES
            Constants.NOTIF_PROPERTY -> CHANNEL_PROPERTY
            else                     -> CHANNEL_SYSTEM
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", type)
            putExtra("referenceId", referenceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            nextId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)          // ✅ ensures heads-up popup
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)    // ✅ lock screen visibility
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(nextId(), notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "SecurityException: ${e.message}")
        }
    }

    // ── Booking notifications — ALL ENGLISH ──────────────────────────────────

    fun showBookingConfirmed(propertyName: String, bookingId: String) =
        showNotification(
            title       = "Booking Confirmed ✓",
            message     = "Your booking for \"$propertyName\" has been confirmed.",
            type        = Constants.NOTIF_BOOKING,
            referenceId = bookingId
        )

    fun showBookingCancelled(propertyName: String, bookingId: String) =
        showNotification(
            title       = "Booking Cancelled",
            message     = "Your booking for \"$propertyName\" has been cancelled.",
            type        = Constants.NOTIF_BOOKING,
            referenceId = bookingId
        )

    fun showNewBookingRequest(propertyName: String, tenantName: String, bookingId: String) =
        showNotification(
            title       = "New Booking Request 📋",
            message     = "$tenantName has requested to book \"$propertyName\".",
            type        = Constants.NOTIF_BOOKING,
            referenceId = bookingId
        )

    fun showCheckInReminder(propertyName: String, bookingId: String) =
        showNotification(
            title       = "Check-In Tomorrow! 🏠",
            message     = "Your check-in at \"$propertyName\" is scheduled for tomorrow.",
            type        = Constants.NOTIF_BOOKING,
            referenceId = bookingId
        )

    // ── Payment notifications ─────────────────────────────────────────────────

    fun showPaymentSuccess(amount: Double, transactionId: String) =
        showNotification(
            title       = "Payment Successful 💚",
            message     = "Payment of Rs. ${amount.toInt()} has been confirmed. Transaction ID: $transactionId",
            type        = Constants.NOTIF_PAYMENT,
            referenceId = transactionId
        )

    // ── Message notifications ─────────────────────────────────────────────────

    fun showNewMessage(senderName: String, preview: String, conversationId: String) =
        showNotification(
            title       = "New Message from $senderName 💬",
            message     = preview,
            type        = Constants.NOTIF_MESSAGE,
            referenceId = conversationId
        )

    // ── Property notifications ────────────────────────────────────────────────

    fun showPropertyApproved(
        propertyTitle: String,
        propertyId   : String,
        adminNote    : String = ""
    ) {
        val body = if (adminNote.isNotEmpty())
            "Your property \"$propertyTitle\" has been approved! Admin note: $adminNote"
        else
            "Congratulations! Your property \"$propertyTitle\" has been approved. ✅"
        showNotification(
            title       = "Property Approved ✓",
            message     = body,
            type        = Constants.NOTIF_PROPERTY,
            referenceId = propertyId
        )
    }

    fun showPropertyRejected(
        propertyTitle: String,
        propertyId   : String,
        adminNote    : String = ""
    ) {
        val body = if (adminNote.isNotEmpty())
            "Your property \"$propertyTitle\" was not approved. Reason: $adminNote"
        else
            "Your property \"$propertyTitle\" was not approved. Please contact support."
        showNotification(
            title       = "Property Rejected ✗",
            message     = body,
            type        = Constants.NOTIF_PROPERTY,
            referenceId = propertyId
        )
    }

    // ── User verification notifications ──────────────────────────────────────

    fun showUserVerified(userName: String) =
        showNotification(
            title   = "Account Verified ✓",
            message = "Congratulations $userName! Your account has been successfully verified.",
            type    = Constants.NOTIF_SYSTEM
        )

    fun showUserRejected(userName: String, reason: String = "") {
        val body = if (reason.isNotEmpty())
            "Your account verification was rejected. Reason: $reason"
        else
            "Your account verification was rejected. Please contact support."
        showNotification(
            title   = "Verification Rejected",
            message = body,
            type    = Constants.NOTIF_SYSTEM
        )
    }

    // ── Admin notifications ───────────────────────────────────────────────────

    fun showNewPropertyPending(
        propertyTitle: String,
        landlordName : String,
        propertyId   : String
    ) = showNotification(
        title       = "New Property Pending Review ⏳",
        message     = "$landlordName has submitted \"$propertyTitle\" for review.",
        type        = Constants.NOTIF_PROPERTY,
        referenceId = propertyId
    )

    fun showNewUserPending(userName: String, userId: String) =
        showNotification(
            title       = "New User Verification Request 👤",
            message     = "$userName has applied for account verification.",
            type        = Constants.NOTIF_SYSTEM,
            referenceId = userId
        )

    // ── Cancel ────────────────────────────────────────────────────────────────
    fun cancelNotification(id: Int) = NotificationManagerCompat.from(context).cancel(id)
    fun cancelAll() = NotificationManagerCompat.from(context).cancelAll()
}



















