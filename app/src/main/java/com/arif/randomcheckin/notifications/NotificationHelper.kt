package com.arif.randomcheckin.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arif.randomcheckin.R

object NotificationHelper {

    private const val CHANNEL_ID = "checkin_channel"
    private const val CHANNEL_NAME = "Check-in reminders"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE_CHECK_IN = 0
    private const val TITLE_CHECK_IN = "Günlük check-in"
    private const val MESSAGE_CHECK_IN = "Bugün için kısa bir not ekle."

    /**
     * Surfaces the reminder only when the user can actually receive notifications, preventing
     * confusing silent failures while keeping UI behavior unchanged.
     */
    fun showCheckInNotification(context: Context) {
        if (!notificationsEnabled(context)) {
            return
        }
        ensureChannelExists(context)

        val contentIntent = createCheckInPendingIntent(context)
        val notification = buildCheckInNotification(context, contentIntent)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * Creates or updates the high-importance channel on Android O+ so reminders remain prominent.
     */
    private fun ensureChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    /**
     * Rejects notification attempts when the runtime permission (on 13+) or global toggle is off.
     */
    private fun notificationsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun createCheckInPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CheckInActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_CHECK_IN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildCheckInNotification(
        context: Context,
        contentIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(TITLE_CHECK_IN)
            .setContentText(MESSAGE_CHECK_IN)
            .setStyle(NotificationCompat.BigTextStyle().bigText(MESSAGE_CHECK_IN))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }
}
