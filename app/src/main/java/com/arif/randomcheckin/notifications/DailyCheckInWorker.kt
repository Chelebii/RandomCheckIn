package com.arif.randomcheckin.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.arif.randomcheckin.data.GoalStore
import com.arif.randomcheckin.utils.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

private const val DAILY_CHECK_IN_WORK_NAME = "daily_checkin_work"

/**
 * Shows the daily check-in notification and immediately schedules the next random reminder so the
 * one-per-day invariant cannot drift even if the app process dies.
 */
class DailyCheckInWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        NotificationHelper.showCheckInNotification(applicationContext)
        scheduleNextDaily()
        return Result.success()
    }

    /**
     * WorkManager executes on a background thread but exposes a blocking API, so runBlocking keeps
     * the contract synchronous while still letting us collect from the DataStore flow once.
     */
    private fun scheduleNextDaily() = runBlocking {
        val store = GoalStore(applicationContext)
        val (startMin, endMin) = store.activeWindowFlow().first()
        val delayMillis = TimeUtils.nextRandomDelayMillis(startMin, endMin)
        enqueueNextDaily(delayMillis)
    }

    private fun enqueueNextDaily(delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<DailyCheckInWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            DAILY_CHECK_IN_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
