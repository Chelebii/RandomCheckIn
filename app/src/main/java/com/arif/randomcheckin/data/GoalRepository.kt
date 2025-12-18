package com.arif.randomcheckin.data

import com.arif.randomcheckin.data.model.Goal
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalStore: GoalStore) {

    fun goalsFlow(): Flow<List<Goal>> = goalStore.goalListFlow()

    suspend fun addGoal(title: String, description: String, endDate: String) =
        goalStore.addGoal(title, description, endDate)

    suspend fun updateGoal(id: String, title: String, description: String, endDate: String) =
        goalStore.updateGoal(id, title, description, endDate)

    suspend fun deleteGoal(id: String) = goalStore.removeGoal(id)

    suspend fun completeGoal(id: String) = goalStore.completeGoal(id)
}
