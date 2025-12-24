package com.arif.randomcheckin.data

import com.arif.randomcheckin.data.model.Goal
import com.arif.randomcheckin.data.model.GoalId
import kotlinx.coroutines.flow.Flow

/**
 * Thin repository facade keeps the rest of the app decoupled from the underlying [GoalStore].
 * Business rules (max three active goals, automatic completion, etc.) remain in the ViewModel/domain,
 * so this layer simply centralizes data access without altering behavior.
 */
class GoalRepository(private val goalStore: GoalStore) {

    /** Emits the canonical goal list so UI progress bars stay in sync with persistence. */
    fun goalsFlow(): Flow<List<Goal>> = goalStore.goalListFlow()

    /** Delegates creation to the store; validation occurs before calling this method. */
    suspend fun addGoal(title: String, description: String, endDate: String) =
        goalStore.addGoal(title, endDate)

    /** Keeps update logic consistent by funneling all edits through the same data source. */
    suspend fun updateGoal(id: GoalId, title: String, description: String, endDate: String) =
        goalStore.updateGoal(id, title, endDate)

    /** Repository owns deletes so observers receive a single source of truth. */
    suspend fun deleteGoal(id: GoalId) = goalStore.removeGoal(id)

    /** Completing a goal still flows through the store to trigger downstream flows. */
    suspend fun completeGoal(id: GoalId) = goalStore.completeGoal(id)

    fun sendTestNotification(goal: Goal) = goalStore.sendTestNotification(goal)
}
