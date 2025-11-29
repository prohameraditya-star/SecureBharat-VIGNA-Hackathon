package com.example.paisacheck360

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {

    private const val CHANNEL_ID = "scam_alert_channel"
    private const val CHANNEL_NAME = "Scam Alerts"
    private const val CHANNEL_DESC = "Alerts for suspected scam or risky SMS messages"

    /** 🔹 Ensures the channel exists (for Android 8.0+) */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager?.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    enableLights(true)
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }

    /**
     * 🔔 Old behavior — shows a simple scam notification
     */
    fun showScamNotification(context: Context, title: String, message: String) {
        ensureChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /**
     * 🔔 New behavior — shows alert for suspicious/dangerous URLs in SMS
     */
    fun showUrlAlertNotification(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        url: String,
        reasons: List<String>
    ) {
        ensureChannel(context)

        // Intent → open LinkScannerActivity for details
        val detailIntent = Intent(context, LinkScannerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("scanned_url", url)
            putStringArrayListExtra("scanned_reasons", ArrayList(reasons))
        }

        val detailPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            detailIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent → open URL in browser (user choice only)
        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$text\n\nReasons: ${reasons.joinToString("; ")}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "View Details", detailPendingIntent)
            .addAction(android.R.drawable.ic_menu_info_details, "Open Link", openPendingIntent)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
