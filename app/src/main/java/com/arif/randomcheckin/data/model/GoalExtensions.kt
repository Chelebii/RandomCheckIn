package com.arif.randomcheckin.data.model

import java.time.LocalDate
import java.time.LocalDateTime
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
 * Remaining ratio = (end - now) / (end - start). We treat the day after the stored end date’s start
 * as the exclusive boundary so users enjoy the full final day. Zero/negative durations render as 1f
 * (full line) to avoid divide-by-zero flicker when goals are created with identical start/end.
 */
fun Goal.remainingProgress(now: LocalDateTime = LocalDateTime.now()): Float {
    val start = startDateOrNull()?.atStartOfDay() ?: now
    val endExclusive = endDateOrNull()
        ?.plusDays(1)
        ?.atStartOfDay()
        ?: start

    val totalMinutes = ChronoUnit.MINUTES.between(start, endExclusive).coerceAtLeast(0)
    if (totalMinutes == 0L) return 1f

    val remainingMinutes = ChronoUnit.MINUTES.between(now, endExclusive).coerceAtLeast(0)
    val ratio = remainingMinutes / totalMinutes.toFloat()
    return ratio.coerceIn(0f, 1f)
}

/** Completed state is the inverse of [isActive] to keep a single source of truth. */
fun Goal.isCompleted(referenceDate: LocalDate = LocalDate.now()): Boolean = !isActive(referenceDate)
