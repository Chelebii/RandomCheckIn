package com.arif.randomcheckin.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arif.randomcheckin.data.GoalRepository
import com.arif.randomcheckin.data.MAX_ACTIVE_GOALS
import com.arif.randomcheckin.data.model.Goal
import com.arif.randomcheckin.data.model.isActive
import com.arif.randomcheckin.data.model.remainingProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalsUiState(
    val activeGoals: List<GoalWithProgress> = emptyList(),
    val completedGoals: List<Goal> = emptyList(),
    val canAddMoreActive: Boolean = true,
    val currentTab: GoalsTab = GoalsTab.ACTIVE,
    val pendingDelete: Goal? = null,
    val showLimitInfo: Boolean = false
)

data class GoalWithProgress(val goal: Goal, val remainingProgress: Float)

enum class GoalsTab { ACTIVE, COMPLETED }

private data class GoalsSnapshot(
    val active: List<GoalWithProgress> = emptyList(),
    val completed: List<Goal> = emptyList(),
    val canAddMoreActive: Boolean = true
)

class GoalsViewModel(private val repository: GoalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        observeGoals()
    }

    /**
     * Repository is the single source of truth for goal status; we merge its snapshot with
     * UI-specific flags (tab, dialogs) to honor the max-active rule without UI shortcuts.
     */
    private fun observeGoals() {
        viewModelScope.launch {
            repository.goalsFlow()
                .map { goals -> goals.toSnapshot() }
                .collect { snapshot ->
                    _uiState.update { current ->
                        current.copy(
                            activeGoals = snapshot.active,
                            completedGoals = snapshot.completed,
                            canAddMoreActive = snapshot.canAddMoreActive
                        )
                    }
                }
        }
    }

    fun setTab(tab: GoalsTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun showLimitInfo() = _uiState.update { it.copy(showLimitInfo = true) }
    fun hideLimitInfo() = _uiState.update { it.copy(showLimitInfo = false) }

    fun requestDelete(goal: Goal) {
        _uiState.update { it.copy(pendingDelete = goal) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val goal = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            repository.deleteGoal(goal.id)
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    fun calculateRemainingProgress(goal: Goal, today: LocalDate = LocalDate.now()): Float =
        goal.remainingProgress(today)

    fun markCompleted(goal: Goal) {
        viewModelScope.launch {
            repository.completeGoal(goal.id)
            _uiState.update { it.copy(currentTab = GoalsTab.COMPLETED) }
        }
    }

    suspend fun addGoal(title: String, description: String, endDate: String) {
        repository.addGoal(title, description, endDate)
    }

    suspend fun updateGoal(goalId: String, title: String, description: String, endDate: String) {
        repository.updateGoal(goalId, title, description, endDate)
    }

    private fun List<Goal>.toSnapshot(): GoalsSnapshot {
        val today = LocalDate.now()
        val (active, completed) = partition { it.isActive(today) }
        return GoalsSnapshot(
            active = active.map { GoalWithProgress(it, it.remainingProgress(today)) },
            completed = completed,
            canAddMoreActive = active.size < MAX_ACTIVE_GOALS
        )
    }
}
