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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// MainActivity: Uygulama açılınca çalışan ana Android Activity (ekranın giriş kapısı)
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

@Composable
fun GoalListScreen(
    goalStore: GoalStore,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = GoalsViewModelFactory(goalStore))
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var goalEditor by remember { mutableStateOf<GoalEditorState>(GoalEditorState.Hidden) }
    val state by viewModel.state.collectAsState()
    val isActiveTab = state.currentTab == GoalsTab.ACTIVE
    val visibleGoals by remember(state) { derivedStateOf { state.visibleGoals() } }
    val handleAddGoal: () -> Unit = {
        if (state.canAddMoreActive) {
            goalEditor = GoalEditorState.Creating
        } else {
            viewModel.showLimitInfo()
        }
    }
    BackHandler(enabled = state.showLimitInfo) { viewModel.hideLimitInfo() }

    val dismissEditor: () -> Unit = { goalEditor = GoalEditorState.Hidden }

    val editorState = goalEditor
    if (editorState !is GoalEditorState.Hidden) {
        AddGoalScreen(
            initialTitle = editorState.initialTitle(),
            initialDescription = editorState.initialDescription(),
            initialEndDate = editorState.initialEndDate(),
            titleText = if (editorState is GoalEditorState.Editing) "Edit Goal" else "Add Goal",
            onSave = { title, desc, endDate ->
                scope.launch {
                    try {
                        when (editorState) {
                            GoalEditorState.Creating -> viewModel.addGoal(title, desc, endDate)
                            is GoalEditorState.Editing -> viewModel.updateGoal(editorState.goal.id, title, desc, endDate)
                            GoalEditorState.Hidden -> Unit
                        }
                        goalEditor = GoalEditorState.Hidden
                    } catch (limit: IllegalStateException) {
                        viewModel.showLimitInfo()
                    }
                }
            },
            onCancel = dismissEditor
        )
        return
    }

    Surface(color = colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Goals",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = "Stay consistent every night",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onBackground.copy(alpha = 0.7f)
                        )
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
                            contentDescription = "Toggle theme"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TabRow(selectedTabIndex = state.currentTab.ordinal) {
                    GoalsTab.values().forEachIndexed { index, tab ->
                        Tab(
                            selected = state.currentTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = { Text(tab.name.lowercase().replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GoalsList(
                    modifier = Modifier.weight(1f),
                    goals = visibleGoals,
                    isActiveTab = isActiveTab,
                    colorScheme = colorScheme,
                    onCompleteGoal = viewModel::markCompleted,
                    onEditGoal = { goal -> goalEditor = GoalEditorState.Editing(goal) },
                    onDeleteGoal = viewModel::requestDelete,
                    onAddGoal = handleAddGoal
                )
                Spacer(modifier = Modifier.height(28.dp))

                if (isActiveTab) {
                    FilledTonalButton(
                        onClick = handleAddGoal,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (!state.canAddMoreActive) colorScheme.surfaceVariant.copy(alpha = 0.5f) else colorScheme.primaryContainer,
                            contentColor = if (!state.canAddMoreActive) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(if (!state.canAddMoreActive) "Goal limit reached" else "Add goal")
                    }
                }
            }

            if (state.pendingDelete != null) {
                DeleteConfirmationDialog(
                    colorScheme = colorScheme,
                    onDismiss = { viewModel.cancelDelete() },
                    onConfirm = { viewModel.confirmDelete() }
                )
            }

            AnimatedVisibility(
                visible = state.showLimitInfo,
                enter = fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = fadeOut(animationSpec = tween(durationMillis = 180))
            ) {
                GoalLimitOverlay(
                    colorScheme = colorScheme,
                    onDismiss = { viewModel.hideLimitInfo() }
                )
            }
        }
    }
}

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
                val barColor = if (progress <= 0.1f) colorScheme.error else colorScheme.primary
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.dp)
                            .background(barColor)
                    )
                }
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

@Composable
private fun DeleteConfirmationDialog(
    colorScheme: ColorScheme,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Delete this goal?", style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text("This action can’t be undone.", color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = colorScheme.errorContainer, contentColor = colorScheme.onErrorContainer)) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalLimitOverlay(
    colorScheme: ColorScheme,
    onDismiss: () -> Unit
) {
    val overlayColor = Color.Black.copy(alpha = 0.55f)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss
            )
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
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
                Text(
                    text = "Why only 3 goals?",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Limiting goals helps you stay focused and finish what you start. Three is enough.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoalsList(
    modifier: Modifier = Modifier,
    goals: List<GoalWithProgress>,
    isActiveTab: Boolean,
    colorScheme: ColorScheme,
    onCompleteGoal: (Goal) -> Unit,
    onEditGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit,
    onAddGoal: () -> Unit
) {
    if (goals.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isActiveTab) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = ACTIVE_EMPTY_TEXT,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onAddGoal) {
                        Text("Add goal")
                    }
                }
            } else {
                Text(
                    text = COMPLETED_EMPTY_TEXT,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(goals, key = { it.goal.id }) { goalWithProgress ->
                GoalCard(
                    goal = goalWithProgress.goal,
                    colorScheme = colorScheme,
                    showComplete = isActiveTab,
                    showEdit = isActiveTab,
                    progress = if (isActiveTab) goalWithProgress.remainingProgress else null,
                    onComplete = { onCompleteGoal(goalWithProgress.goal) },
                    onEdit = { onEditGoal(goalWithProgress.goal) },
                    onDelete = { onDeleteGoal(goalWithProgress.goal) }
                )
            }
        }
    }
}

private const val ACTIVE_EMPTY_TEXT = "Start with one clear goal."
private const val COMPLETED_EMPTY_TEXT = "No completed goals yet. Time will take care of that."

private sealed interface GoalEditorState {
    data object Hidden : GoalEditorState
    data object Creating : GoalEditorState
    data class Editing(val goal: Goal) : GoalEditorState
}

private fun GoalEditorState.initialTitle() = when (this) {
    GoalEditorState.Hidden, GoalEditorState.Creating -> ""
    is GoalEditorState.Editing -> goal.title
}

private fun GoalEditorState.initialDescription() = when (this) {
    GoalEditorState.Hidden, GoalEditorState.Creating -> ""
    is GoalEditorState.Editing -> goal.description
}

private fun GoalEditorState.initialEndDate() = when (this) {
    GoalEditorState.Hidden, GoalEditorState.Creating -> ""
    is GoalEditorState.Editing -> goal.endDate
}

private fun GoalsUiState.visibleGoals(): List<GoalWithProgress> = when (currentTab) {
    GoalsTab.ACTIVE -> activeGoals
    GoalsTab.COMPLETED -> completedGoals.map { GoalWithProgress(it, 0f) }
}
