package com.arif.randomcheckin.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Shared formatter for dd.MM.yyyy goal dates. */
val goalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun Goal.startDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(startDate.trim(), goalDateFormatter)
}.getOrNull()

fun Goal.endDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(endDate.trim(), goalDateFormatter)
}.getOrNull()

/** Goals are active until the day before their end date; expired goals should move to Completed automatically. */
fun Goal.isActive(referenceDate: LocalDate = LocalDate.now()): Boolean {
    val goalDate = endDateOrNull()
    return goalDate?.isBefore(referenceDate) != true
}

/**
 * Calculates remaining progress as the share of days left. The result is clamped 0..1 so the UI bar never overflows,
 * and zero-length goals safely report 0.
 */
fun Goal.remainingProgress(today: LocalDate = LocalDate.now()): Float {
    val start = startDateOrNull() ?: today
    val end = endDateOrNull() ?: start
    val totalDays = (java.time.temporal.ChronoUnit.DAYS.between(start, end)).coerceAtLeast(0)
    if (totalDays == 0L) return 0f
    val elapsed = java.time.temporal.ChronoUnit.DAYS.between(start, today).coerceAtLeast(0)
    val usedRatio = elapsed / totalDays.toFloat()
    val remaining = 1f - usedRatio
    return remaining.coerceIn(0f, 1f)
}

fun Goal.isCompleted(referenceDate: LocalDate = LocalDate.now()): Boolean = !isActive(referenceDate)
