package com.arif.randomcheckin.data.model

import androidx.compose.runtime.Immutable

/** Identifier wrapper clarifies intent without changing the underlying storage type. */
typealias GoalId = String

/**
 * Immutable snapshot of a goal flowing between UI and domain layers.
 * Dates remain raw strings so validation (max 3 active goals, non-past end dates, etc.) stays centralized
 * in the ViewModel/domain layer instead of being duplicated in UI components.
 */
@Immutable
data class Goal(
    val id: GoalId,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String
)
