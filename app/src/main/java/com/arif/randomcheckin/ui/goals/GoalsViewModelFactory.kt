package com.arif.randomcheckin.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arif.randomcheckin.data.GoalRepository
import com.arif.randomcheckin.data.GoalStore

/**
 * Centralizes GoalsViewModel creation so every instance shares the same GoalRepository, keeping
 * business rules in the ViewModel/domain layers rather than the UI.
 */
class GoalsViewModelFactory(goalStore: GoalStore) : ViewModelProvider.Factory {

    private val repository = GoalRepository(goalStore)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
            return GoalsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
