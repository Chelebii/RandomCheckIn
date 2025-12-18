package com.arif.randomcheckin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arif.randomcheckin.data.GoalStore
import com.arif.randomcheckin.data.model.Goal
import com.arif.randomcheckin.data.model.ThemeMode
import com.arif.randomcheckin.ui.goals.GoalWithProgress
import com.arif.randomcheckin.ui.goals.GoalsTab
import com.arif.randomcheckin.ui.goals.GoalsUiState
import com.arif.randomcheckin.ui.goals.GoalsViewModel
import com.arif.randomcheckin.ui.goals.GoalsViewModelFactory
import com.arif.randomcheckin.ui.theme.AddGoalScreen
import com.arif.randomcheckin.ui.theme.RandomCheckInTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

/**
 * Hosts the RandomCheckIn experience. Keeps the activity lean by delegating all logic to Compose and the ViewModel.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Android 13+ için bildirim izni isteme
        // İzin verilmezse bildirimler görünmeyebilir
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        // Jetpack Compose ile UI çizimi burada başlar
        setContent {
            RandomCheckInApp()
        }
    }
}

@Composable
fun RandomCheckInApp() {
    val context = LocalContext.current
    val goalStore = remember { GoalStore(context) }
    val themeMode by goalStore.themeModeFlow().collectAsState(initial = ThemeMode.LIGHT)
    val themeScope = rememberCoroutineScope()

    RandomCheckInTheme(darkTheme = themeMode == ThemeMode.DARK) {
        val systemUiController = rememberSystemUiController()
        val darkIcons = !(themeMode == ThemeMode.DARK)
        val backgroundColor = MaterialTheme.colorScheme.background

        SideEffect {
            systemUiController.setStatusBarColor(color = backgroundColor, darkIcons = darkIcons)
            systemUiController.setNavigationBarColor(color = backgroundColor, darkIcons = darkIcons)
        }

        GoalListScreen(
            goalStore = goalStore,
            currentTheme = themeMode,
            onThemeChange = { mode ->
                themeScope.launch { goalStore.setThemeMode(mode) }
            }
        )
    }
}

/**
 * High-level goals UI. UI remains stateless by observing [GoalsViewModel] and emitting user intents back.
 */
@Composable
fun GoalListScreen(
    goalStore: GoalStore,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = GoalsViewModelFactory(goalStore))
) {
    val state by viewModel.state.collectAsState()
    var showAddScreen by rememberSaveable { mutableStateOf(false) }
    var editingGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val editingGoal = remember(state.activeGoals, state.completedGoals, editingGoalId) {
        editingGoalId?.let { id ->
            state.activeGoals.firstOrNull { it.goal.id == id }?.goal
                ?: state.completedGoals.firstOrNull { it.id == id }
        }
    }

    // Use derivedStateOf so recomposition only happens when tab data actually changes.
    val visibleGoals by remember(state.currentTab, state.activeGoals, state.completedGoals) {
        derivedStateOf {
            when (state.currentTab) {
                GoalsTab.ACTIVE -> state.activeGoals
                GoalsTab.COMPLETED -> state.completedGoals.map { GoalWithProgress(it, 0f) }
            }
        }
    }

    val isActiveTab = state.currentTab == GoalsTab.ACTIVE

    BackHandler(enabled = state.showLimitInfo) { viewModel.hideLimitInfo() }

    if (showAddScreen) {
        AddGoalScreen(
            initialTitle = editingGoal?.title.orEmpty(),
            initialDescription = editingGoal?.description.orEmpty(),
            initialEndDate = editingGoal?.endDate.orEmpty(),
            titleText = if (editingGoal == null) ADD_GOAL_LABEL else EDIT_GOAL_LABEL,
            onSave = { title, desc, endDate ->
                scope.launch {
                    runCatching {
                        if (editingGoal == null) {
                            viewModel.addGoal(title, desc, endDate)
                        } else {
                            viewModel.updateGoal(editingGoal.id, title, desc, endDate)
                        }
                    }.onSuccess {
                        showAddScreen = false
                        editingGoalId = null
                    }.onFailure {
                        viewModel.showLimitInfo()
                    }
                }
            },
            onCancel = {
                showAddScreen = false
                editingGoalId = null
            }
        )
        return
    }

    GoalListContent(
        state = state,
        currentTheme = currentTheme,
        onThemeChange = onThemeChange,
        visibleGoals = visibleGoals,
        isActiveTab = isActiveTab,
        onAddGoal = {
            if (state.canAddMoreActive) {
                editingGoalId = null
                showAddScreen = true
            } else {
                viewModel.showLimitInfo()
            }
        },
        onEditGoal = { goalId ->
            editingGoalId = goalId
            showAddScreen = true
        },
        onCompleteGoal = viewModel::markCompleted,
        onDeleteGoal = viewModel::requestDelete,
        onDismissDelete = viewModel::cancelDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onHideLimitInfo = viewModel::hideLimitInfo,
        onTabSelected = viewModel::setTab
    )
}

/**
 * Stateless container that wires UI controls to events while showing either the Active or Completed tab.
 */
@Composable
private fun GoalListContent(
    state: GoalsUiState,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    visibleGoals: List<GoalWithProgress>,
    isActiveTab: Boolean,
    onAddGoal: () -> Unit,
    onEditGoal: (String) -> Unit,
    onCompleteGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onHideLimitInfo: () -> Unit,
    onTabSelected: (GoalsTab) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(color = colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Header(currentTheme = currentTheme) { onThemeChange(it) }
                Spacer(modifier = Modifier.height(24.dp))
                GoalsTabRow(currentTab = state.currentTab, onTabSelected = onTabSelected)
                Spacer(modifier = Modifier.height(16.dp))
                GoalsList(
                    goals = visibleGoals,
                    isActiveTab = isActiveTab,
                    colorScheme = colorScheme,
                    onCompleteGoal = onCompleteGoal,
                    onEditGoal = onEditGoal,
                    onDeleteGoal = onDeleteGoal
                )
                Spacer(modifier = Modifier.height(28.dp))
                if (isActiveTab) {
                    AddGoalButton(
                        enabled = state.canAddMoreActive,
                        onClick = onAddGoal,
                        colorScheme = colorScheme
                    )
                }
            }

            if (state.pendingDelete != null) {
                DeleteConfirmationDialog(
                    colorScheme = colorScheme,
                    onDismiss = onDismissDelete,
                    onConfirm = onConfirmDelete
                )
            }

            AnimatedVisibility(
                visible = state.showLimitInfo,
                enter = fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = fadeOut(animationSpec = tween(durationMillis = 180))
            ) {
                LimitInfoOverlay(colorScheme = colorScheme, onDismiss = onHideLimitInfo)
            }
        }
    }
}

@Composable
private fun Header(currentTheme: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = GOALS_TITLE, style = MaterialTheme.typography.headlineMedium, color = colorScheme.onBackground)
            Text(text = GOALS_SUBTITLE, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onBackground.copy(alpha = 0.7f))
        }
        FilledIconButton(
            onClick = {
                val nextMode = if (currentTheme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                onThemeChange(nextMode)
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = if (currentTheme == ThemeMode.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = THEME_TOGGLE_DESCRIPTION
            )
        }
    }
}

/** Renders the Active/Completed tab switcher so each tab stays declarative. */
@Composable
private fun GoalsTabRow(currentTab: GoalsTab, onTabSelected: (GoalsTab) -> Unit) {
    TabRow(selectedTabIndex = currentTab.ordinal) {
        GoalsTab.values().forEach { tab ->
            Tab(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.title) }
            )
        }
    }
}

private val GoalsTab.title: String
    get() = name.lowercase().replaceFirstChar { it.titlecase() }

/** Displays either the progress cards or contextual empty state copy. */
@Composable
private fun GoalsList(
    goals: List<GoalWithProgress>,
    isActiveTab: Boolean,
    colorScheme: ColorScheme,
    onCompleteGoal: (Goal) -> Unit,
    onEditGoal: (String) -> Unit,
    onDeleteGoal: (Goal) -> Unit
) {
    if (goals.isEmpty()) {
        Text(
            text = if (isActiveTab) NO_ACTIVE_TEXT else NO_COMPLETED_TEXT,
            color = colorScheme.onBackground.copy(alpha = 0.75f)
        )
    } else {
        goals.forEach { goalWithProgress ->
            GoalCard(
                goal = goalWithProgress.goal,
                colorScheme = colorScheme,
                showComplete = isActiveTab,
                showEdit = isActiveTab,
                progress = if (isActiveTab) goalWithProgress.remainingProgress else null,
                onComplete = { onCompleteGoal(goalWithProgress.goal) },
                onEdit = { onEditGoal(goalWithProgress.goal.id) },
                onDelete = { onDeleteGoal(goalWithProgress.goal) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/** CTA lives in its own composable to keep GoalListContent compact and reusable. */
@Composable
private fun AddGoalButton(enabled: Boolean, onClick: () -> Unit, colorScheme: ColorScheme) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (!enabled) colorScheme.surfaceVariant.copy(alpha = 0.5f) else colorScheme.primaryContainer,
            contentColor = if (!enabled) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer
        )
    ) {
        Text(text = if (!enabled) LIMIT_REACHED_LABEL else ADD_GOAL_LABEL)
    }
}

/** Full-screen overlay explaining the "why only 3" rule without blocking accessibility focus. */
@Composable
private fun LimitInfoOverlay(colorScheme: ColorScheme, onDismiss: () -> Unit) {
    val overlayColor = Color.Black.copy(alpha = 0.55f)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onDismiss() }
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(24.dp)
            ) {
                Text(text = WHY_LIMIT_TITLE, style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = WHY_LIMIT_BODY, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Modal dialog used whenever the user deletes a goal, regardless of tab. */
@Composable
private fun DeleteConfirmationDialog(
    colorScheme: ColorScheme,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(DELETE_TITLE, style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text(DELETE_BODY, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text(DELETE_CANCEL) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.errorContainer,
                            contentColor = colorScheme.onErrorContainer
                        )
                    ) { Text(DELETE_CONFIRM) }
                }
            }
        }
    }
}

/** Card UI for a single goal. Receives all state via params so previews stay easy. */
@Composable
private fun GoalCard(
    goal: Goal,
    colorScheme: ColorScheme,
    showComplete: Boolean,
    showEdit: Boolean,
    progress: Float?,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurface)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (showEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit goal", tint = colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete goal", tint = colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(goal.description, color = colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Ends ${goal.endDate}", color = colorScheme.onSurfaceVariant)
            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    trackColor = colorScheme.surfaceVariant,
                    color = colorScheme.primary
                )
            }
            if (showComplete) {
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Mark as completed",
                        tint = colorScheme.primary
                    )
                }
            }
        }
    }
}

private const val GOALS_TITLE = "Goals"
private const val GOALS_SUBTITLE = "Stay consistent every night"
private const val NO_ACTIVE_TEXT = "No active goals."
private const val NO_COMPLETED_TEXT = "No completed goals yet."
private const val WHY_LIMIT_TITLE = "Why only 3 goals?"
private const val WHY_LIMIT_BODY = "Limiting goals helps you stay focused and finish what you start. Three is enough."
private const val DELETE_TITLE = "Delete this goal?"
private const val DELETE_BODY = "This action can’t be undone."
private const val DELETE_CANCEL = "Cancel"
private const val DELETE_CONFIRM = "Delete"
private const val ADD_GOAL_LABEL = "Add goal"
private const val EDIT_GOAL_LABEL = "Edit Goal"
private const val LIMIT_REACHED_LABEL = "Goal limit reached"
private const val THEME_TOGGLE_DESCRIPTION = "Toggle theme"
