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

class DailyCheckInWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        // 1. Bildirimi göster
        NotificationHelper.showCheckInNotification(applicationContext)

        // 2. Bir sonraki GÜNLÜK random zamanı planla
        scheduleNextDaily()

        return Result.success()
    }

    private fun scheduleNextDaily() = runBlocking {

        val store = GoalStore(applicationContext)

        // Saat aralığını GoalStore'dan al
        val (startMin, endMin) = store.activeWindowFlow().first()

        // Bir sonraki random zamanın kaç ms sonra olduğunu hesapla
        val delayMillis = TimeUtils.nextRandomDelayMillis(startMin, endMin)

        val request = OneTimeWorkRequestBuilder<DailyCheckInWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        // Her zaman TEK job olsun diye unique work
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "daily_checkin_work",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
