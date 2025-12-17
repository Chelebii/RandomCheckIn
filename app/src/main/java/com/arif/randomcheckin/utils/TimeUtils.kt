package com.arif.randomcheckin.utils

import java.util.Calendar
import kotlin.random.Random

object TimeUtils {

    fun nextRandomDelayMillis(startMin: Int, endMin: Int): Long {
        val now = Calendar.getInstance()

        val start = startMin.coerceIn(0, 1439)
        val endFixed = endMin.coerceIn(0, 1439)
        val end = if (endFixed <= start) (start + 60).coerceAtMost(1439) else endFixed

        val randomMinute = Random.nextInt(start, end + 1)

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, randomMinute / 60)
            set(Calendar.MINUTE, randomMinute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}