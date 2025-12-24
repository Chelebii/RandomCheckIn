package com.arif.randomcheckin.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arif.randomcheckin.R
import com.arif.randomcheckin.data.model.Goal

object NotificationHelper {

    private const val CHANNEL_ID = "checkin_channel"
    private const val CHANNEL_NAME = "Check-in reminders"
    private const val NOTIFICATION_ID = 1001
    private const val TITLE_CHECK_IN = "Günlük check-in"
    private const val MESSAGE_CHECK_IN = "Bugün için kısa bir not ekle."
    private const val TEST_MESSAGE = "Are you on track?"
    private const val TEST_REQUEST_CODE_BASE = 2000

    /**
     * Surfaces the reminder only when the user can actually receive notifications, preventing
     * confusing silent failures while keeping UI behavior unchanged.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showCheckInNotification(context: Context) {
        if (!notificationsEnabled(context)) {
            return
        }
        ensureChannelExists(context)

        val notification = buildCheckInNotification(context)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showGoalTestNotification(context: Context, goal: Goal) {
        if (!notificationsEnabled(context)) return
        ensureChannelExists(context)

        val title = goal.title.ifBlank { context.getString(R.string.app_name) }
        val notificationId = TEST_REQUEST_CODE_BASE + goal.id.hashCode()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(TEST_MESSAGE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(TEST_MESSAGE))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
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

    private fun buildCheckInNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(TITLE_CHECK_IN)
            .setContentText(MESSAGE_CHECK_IN)
            .setStyle(NotificationCompat.BigTextStyle().bigText(MESSAGE_CHECK_IN))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()
    }
}
