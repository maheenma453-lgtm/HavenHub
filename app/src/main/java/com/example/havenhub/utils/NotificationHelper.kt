package com.example.havenhub.utils
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.havenhub.MainActivity
import com.havenhub.R
import dagger.hilt.android.qualifiers.ApplicationContext
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
        private var notificationId = 1000
    }

    // ── Create all channels (call once in Application) ────────────────
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(CHANNEL_BOOKINGS, "Bookings", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Booking confirmations, reminders and cancellations"
                    },
                    NotificationChannel(CHANNEL_PAYMENTS, "Payments", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Payment receipts and confirmations"
                    },
                    NotificationChannel(CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "New messages from hosts and tenants"
                    },
                    NotificationChannel(CHANNEL_SYSTEM, "System", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "App updates and announcements"
                    }
                )
            )
        }
    }

    // ── Show a notification ───────────────────────────────────────────
    fun showNotification(
        title: String,
        message: String,
        type: String = Constants.NOTIF_SYSTEM,
        referenceId: String = ""
    ) {
        val channelId = when (type) {
            Constants.NOTIF_BOOKING -> CHANNEL_BOOKINGS
            Constants.NOTIF_PAYMENT -> CHANNEL_PAYMENTS
            Constants.NOTIF_MESSAGE -> CHANNEL_MESSAGES
            else                    -> CHANNEL_SYSTEM
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("type", type)
            putExtra("referenceId", referenceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId++, notification)
    }

    // ── Booking notifications ─────────────────────────────────────────
    fun showBookingConfirmed(propertyName: String, bookingId: String) {
        showNotification("Booking Confirmed!", "Your booking for $propertyName has been confirmed.", Constants.NOTIF_BOOKING, bookingId)
    }

    fun showBookingCancelled(propertyName: String, bookingId: String) {
        showNotification("Booking Cancelled", "Your booking for $propertyName has been cancelled.", Constants.NOTIF_BOOKING, bookingId)
    }

    fun showCheckInReminder(propertyName: String, bookingId: String) {
        showNotification("Check-In Tomorrow!", "Your check-in at $propertyName is tomorrow.", Constants.NOTIF_BOOKING, bookingId)
    }

    // ── Payment notifications ─────────────────────────────────────────
    fun showPaymentSuccess(amount: Double, transactionId: String) {
        showNotification("Payment Successful!", "Payment of Rs. ${amount.toInt()} confirmed. Txn: $transactionId", Constants.NOTIF_PAYMENT, transactionId)
    }

    // ── Message notifications ─────────────────────────────────────────
    fun showNewMessage(senderName: String, preview: String, conversationId: String) {
        showNotification("New Message from $senderName", preview, Constants.NOTIF_MESSAGE, conversationId)
    }

    // ── Cancel ────────────────────────────────────────────────────────
    fun cancelNotification(id: Int) = NotificationManagerCompat.from(context).cancel(id)
    fun cancelAll() = NotificationManagerCompat.from(context).cancelAll()
}