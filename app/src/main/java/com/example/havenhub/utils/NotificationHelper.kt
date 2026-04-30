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
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_BOOKINGS, "Bookings",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Booking confirmations, reminders and cancellations"
                        enableVibration(true)
                    },
                    NotificationChannel(
                        CHANNEL_PAYMENTS, "Payments",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Payment receipts and confirmations"
                        enableVibration(true)
                    },
                    NotificationChannel(
                        CHANNEL_MESSAGES, "Messages",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "New messages from hosts and tenants" },

                    NotificationChannel(
                        CHANNEL_SYSTEM, "System",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "App updates and announcements" },

                    NotificationChannel(
                        CHANNEL_PROPERTY, "Property Updates",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Property approval and rejection alerts"
                        enableVibration(true)
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
        // ✅ FIX: Android 13+ POST_NOTIFICATIONS permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                android.util.Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted — skipping")
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

        // ✅ FIX: ic_notifications exist nahi karta silently fail hoti thi.
        // ic_launcher_foreground hamesha exist karta hai — safe fallback.
        val smallIcon = R.drawable.ic_notification

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(nextId(), notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "SecurityException — permission missing: ${e.message}")
        }
    }

    // ── Booking notifications ─────────────────────────────────────────────────
    fun showBookingConfirmed(propertyName: String, bookingId: String) =
        showNotification(
            title       = "Booking Confirmed! ✓",
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
            message     = "$tenantName ne \"$propertyName\" ke liye booking request ki hai.",
            type        = Constants.NOTIF_BOOKING,
            referenceId = bookingId
        )

    fun showCheckInReminder(propertyName: String, bookingId: String) =
        showNotification(
            title       = "Check-In Tomorrow! 🏠",
            message     = "Your check-in at \"$propertyName\" is tomorrow.",
            type        = Constants.NOTIF_BOOKING,
            referenceId = bookingId
        )

    // ── Payment notifications ─────────────────────────────────────────────────
    fun showPaymentSuccess(amount: Double, transactionId: String) =
        showNotification(
            title       = "Payment Successful! 💚",
            message     = "Payment of Rs. ${amount.toInt()} confirmed. Txn: $transactionId",
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
            "\"$propertyTitle\" approve ho gayi! Admin note: $adminNote"
        else
            "Mubarak! Aapki property \"$propertyTitle\" approve ho gayi hai. ✅"
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
            "\"$propertyTitle\" reject hui. Reason: $adminNote"
        else
            "Aapki property \"$propertyTitle\" approve nahi hui."
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
            title   = "Account Verified! ✓",
            message = "Mubarak $userName! Aapka account verify ho gaya hai.",
            type    = Constants.NOTIF_SYSTEM
        )

    fun showUserRejected(userName: String, reason: String = "") {
        val body = if (reason.isNotEmpty())
            "Aapka account verify nahi hua. Reason: $reason"
        else
            "Aapka account verification reject ho gaya."
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
        title       = "New Property Pending ⏳",
        message     = "$landlordName ne \"$propertyTitle\" submit ki hai — review karen.",
        type        = Constants.NOTIF_PROPERTY,
        referenceId = propertyId
    )

    fun showNewUserPending(userName: String, userId: String) =
        showNotification(
            title       = "New User Verification Pending 👤",
            message     = "$userName ne verification ke liye apply kiya hai.",
            type        = Constants.NOTIF_SYSTEM,
            referenceId = userId
        )

    // ── Cancel ────────────────────────────────────────────────────────────────
    fun cancelNotification(id: Int) = NotificationManagerCompat.from(context).cancel(id)
    fun cancelAll() = NotificationManagerCompat.from(context).cancelAll()
}













