package com.arif.randomcheckin.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val GOAL_DATE_PATTERN = "dd.MM.yyyy"

/** Shared formatter keeps all goal dates aligned with the same display/parsing pattern. */
val goalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(GOAL_DATE_PATTERN)

/** Single parsing entry prevents drift in trimming/format rules across date fields. */
private fun parseGoalDateOrNull(raw: String): LocalDate? = runCatching {
    LocalDate.parse(raw.trim(), goalDateFormatter)
}.getOrNull()

/** Null-safe access ensures callers never crash on malformed or empty start dates. */
fun Goal.startDateOrNull(): LocalDate? = parseGoalDateOrNull(startDate)

/** Matches [startDateOrNull] semantics so start/end comparisons stay symmetric. */
fun Goal.endDateOrNull(): LocalDate? = parseGoalDateOrNull(endDate)

/**
 * Goals remain "active" through the end date (inclusive) to respect the business cap of 3 active goals.
 * Null end dates are treated as ongoing goals so users never lose progress bars prematurely.
 */
fun Goal.isActive(referenceDate: LocalDate = LocalDate.now()): Boolean {
    val goalEndDate = endDateOrNull()
    return goalEndDate?.isBefore(referenceDate) != true
}

/**
 * Returns the remaining fraction (0f-1f) of the current goal timeline for the UI progress bar.
 * Short or inverted ranges collapse to zero so the UI never divides by zero or overflows beyond bounds.
 */
fun Goal.remainingProgress(today: LocalDate = LocalDate.now()): Float {
    val start = startDateOrNull() ?: today
    val end = endDateOrNull() ?: start
    val totalDays = ChronoUnit.DAYS.between(start, end).coerceAtLeast(0)
    if (totalDays == 0L) return 0f
    val elapsedDays = ChronoUnit.DAYS.between(start, today).coerceAtLeast(0)
    val usedRatio = elapsedDays / totalDays.toFloat()
    return (1f - usedRatio).coerceIn(0f, 1f)
}

/** Completed state is the inverse of [isActive] to keep a single source of truth. */
fun Goal.isCompleted(referenceDate: LocalDate = LocalDate.now()): Boolean = !isActive(referenceDate)
