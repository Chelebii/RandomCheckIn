package com.arif.randomcheckin.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Converts stored strings into LocalDate objects while tolerating malformed entries. */
private fun Goal.startDateOrNull(): LocalDate? = parseGoalDateOrNull(startDate)

/** Converts stored strings into LocalDate objects while tolerating malformed entries. */
private fun Goal.endDateOrNull(): LocalDate? = parseGoalDateOrNull(endDate)

private const val GOAL_DATE_PATTERN = "dd.MM.yyyy"

/** Shared formatter keeps all goal dates aligned with the same display/parsing pattern. */
val goalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(GOAL_DATE_PATTERN)

/** Single parsing entry prevents drift in trimming/format rules across date fields. */
private fun parseGoalDateOrNull(raw: String): LocalDate? = runCatching {
    LocalDate.parse(raw.trim(), goalDateFormatter)
}.getOrNull()

/** Goals are active until the day before their end date; expired goals should move to Completed automatically. */
fun Goal.isActive(referenceDate: LocalDate = LocalDate.now()): Boolean {
    val goalEndDate = endDateOrNull()
    return goalEndDate?.isBefore(referenceDate) != true
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

/** Completed state is the inverse of [isActive] to keep a single source of truth. */
fun Goal.isCompleted(referenceDate: LocalDate = LocalDate.now()): Boolean = !isActive(referenceDate)
