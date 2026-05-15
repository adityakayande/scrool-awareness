package com.example.scrolltracker.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.scrolltracker.ui.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "scroll_tracker_channel"
        const val HEADS_UP_CHANNEL_ID = "scroll_tracker_heads_up_channel_v2"
        const val HEADS_UP_CHANNEL_1_ID = "scroll_tracker_heads_up_channel_custom_1_v2"
        const val HEADS_UP_CHANNEL_2_ID = "scroll_tracker_heads_up_channel_custom_2_v2"
        const val HEADS_UP_CHANNEL_3_ID = "scroll_tracker_heads_up_channel_custom_3_v2"
        const val NOTIFICATION_ID = 1001
        const val HEADS_UP_NOTIFICATION_ID_BASE = 2000

        /** Format milliseconds into a concise string like "1h 4m" or "45s". */
        fun formatWatchTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scroll Tracker Service"
            val descriptionText = "Displays real-time scroll count and watch time"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val headsUpChannel = NotificationChannel(HEADS_UP_CHANNEL_ID, "Scroll Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shows popup alerts when scroll limits are reached"
            }
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(headsUpChannel)

            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val sound1Uri = android.net.Uri.parse("android.resource://${context.packageName}/${com.example.scrolltracker.R.raw.sound1}")
            val channel1 = NotificationChannel(HEADS_UP_CHANNEL_1_ID, "Scroll Reminders (Sound 1)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Custom reminder sound 1"
                setSound(sound1Uri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel1)

            val sound2Uri = android.net.Uri.parse("android.resource://${context.packageName}/${com.example.scrolltracker.R.raw.sound2}")
            val channel2 = NotificationChannel(HEADS_UP_CHANNEL_2_ID, "Scroll Reminders (Sound 2)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Custom reminder sound 2"
                setSound(sound2Uri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel2)

            val sound3Uri = android.net.Uri.parse("android.resource://${context.packageName}/${com.example.scrolltracker.R.raw.sound3}")
            val channel3 = NotificationChannel(HEADS_UP_CHANNEL_3_ID, "Scroll Reminders (Sound 3)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Custom reminder sound 3"
                setSound(sound3Uri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel3)
        }
    }

    fun getNotification(scrollCount: Int, watchTimeMs: Long = 0L): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val watchText = if (watchTimeMs > 0L) " | Watch: ${formatWatchTime(watchTimeMs)}" else ""

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Scroll Tracker")
            .setContentText("Scrolls: $scrollCount$watchText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun updateNotification(scrollCount: Int, watchTimeMs: Long = 0L) {
        notificationManager.notify(NOTIFICATION_ID, getNotification(scrollCount, watchTimeMs))
    }

    fun sendHeadsUpReminder(title: String, message: String, idTag: Int, soundIndex: Int? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, idTag, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (soundIndex) {
            0 -> HEADS_UP_CHANNEL_1_ID
            1 -> HEADS_UP_CHANNEL_2_ID
            2 -> HEADS_UP_CHANNEL_3_ID
            else -> HEADS_UP_CHANNEL_ID
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(30000) // Auto-clean notification after 30 seconds

        notificationManager.notify(HEADS_UP_NOTIFICATION_ID_BASE, builder.build())
    }
}
