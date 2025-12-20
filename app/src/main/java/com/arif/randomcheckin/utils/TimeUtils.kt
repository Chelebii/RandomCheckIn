package com.arif.randomcheckin.utils

import java.util.Calendar
import kotlin.random.Random

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
private const val LAST_MINUTE_OF_DAY = MINUTES_PER_DAY - 1
private const val FALLBACK_WINDOW_MINUTES = 60

object TimeUtils {

    /**
     * Returns the milliseconds until the next reminder window.
     *
     * If the caller provides an invalid range (end <= start), we auto-expand it by one hour
     * so the scheduler never stalls. Resulting time is always in the future.
     */
    fun nextRandomDelayMillis(startMin: Int, endMin: Int): Long {
        val now = Calendar.getInstance()

        val startMinute = startMin.coerceIn(0, LAST_MINUTE_OF_DAY)
        val endMinute = endMin.coerceIn(0, LAST_MINUTE_OF_DAY)
        val inclusiveEnd = if (endMinute <= startMinute) {
            (startMinute + FALLBACK_WINDOW_MINUTES).coerceAtMost(LAST_MINUTE_OF_DAY)
        } else {
            endMinute
        }

        val randomMinute = Random.nextInt(startMinute, inclusiveEnd + 1)

        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, randomMinute / MINUTES_PER_HOUR)
            set(Calendar.MINUTE, randomMinute % MINUTES_PER_HOUR)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return target.timeInMillis - now.timeInMillis
    }
}