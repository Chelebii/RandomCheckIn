package com.arif.randomcheckin.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

val goalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun Goal.startDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(startDate.trim(), goalDateFormatter)
}.getOrNull()

fun Goal.endDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(endDate.trim(), goalDateFormatter)
}.getOrNull()

fun Goal.isActive(referenceDate: LocalDate = LocalDate.now()): Boolean {
    val goalDate = endDateOrNull()
    return goalDate?.isBefore(referenceDate) != true
}

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
