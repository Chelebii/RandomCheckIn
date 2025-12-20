package com.arif.randomcheckin.data.model

/**
 * Represents the only two states a goal can have in the app. Keeping this limited ensures the
 * "max 3 active" rule and the separate completed tab never drift out of sync with the domain model.
 */
enum class GoalStatus {
    /** Active goals still consume one of the three allowed slots and require progress tracking. */
    ACTIVE,

    /** Completed goals move to their dedicated tab and stop counting toward the active limit. */
    COMPLETED
}
