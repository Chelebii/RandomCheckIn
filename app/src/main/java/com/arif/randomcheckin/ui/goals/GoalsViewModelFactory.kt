package com.arif.randomcheckin.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arif.randomcheckin.data.GoalRepository
import com.arif.randomcheckin.data.GoalStore

class GoalsViewModelFactory(private val goalStore: GoalStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
            val repository = GoalRepository(goalStore)
            return GoalsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

