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

class GoalsViewModel(private val repository: GoalRepository) : ViewModel() {

    private val backingState: StateFlow<GoalsUiState> = repository.goalsFlow()
        .map { goals ->
            val today = LocalDate.now()
            val (active, completed) = goals.partition { it.isActive(today) }
            // ensure expired goals move to completed via date logic only
            GoalsUiState(
                activeGoals = active.map { GoalWithProgress(it, calculateRemainingProgress(it, today)) },
                completedGoals = completed,
                canAddMoreActive = active.size < MAX_ACTIVE_GOALS,
                currentTab = _uiState.value.currentTab,
                pendingDelete = _uiState.value.pendingDelete,
                showLimitInfo = _uiState.value.showLimitInfo
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GoalsUiState()
        )

    private val _uiState = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            backingState.collect { derived ->
                _uiState.update { current ->
                    derived.copy(
                        currentTab = current.currentTab,
                        pendingDelete = current.pendingDelete,
                        showLimitInfo = current.showLimitInfo
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

    fun calculateRemainingProgress(goal: Goal, today: LocalDate = LocalDate.now()): Float = goal.remainingProgress(today)

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
}
