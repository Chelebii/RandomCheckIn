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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arif.randomcheckin.data.GoalStore
import com.arif.randomcheckin.data.MAX_ACTIVE_GOALS
import com.arif.randomcheckin.data.model.ThemeMode
import com.arif.randomcheckin.data.model.Goal
import com.arif.randomcheckin.ui.theme.RandomCheckInTheme
import com.arif.randomcheckin.ui.theme.AddGoalScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog

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
    onThemeChange: (ThemeMode) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddScreen by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var showLimitInfo by remember { mutableStateOf(false) }
    var pendingDeleteGoal by remember { mutableStateOf<Goal?>(null) }
    BackHandler(enabled = showLimitInfo) { showLimitInfo = false }

    val goalList by goalStore.goalListFlow().collectAsState(initial = emptyList())
    val isLimitReached = goalList.size >= MAX_ACTIVE_GOALS

    val dismissAddScreen: () -> Unit = {
        showAddScreen = false
        editingGoal = null
    }

    if (showAddScreen) {
        AddGoalScreen(
            initialTitle = editingGoal?.title.orEmpty(),
            initialDescription = editingGoal?.description.orEmpty(),
            initialEndDate = editingGoal?.endDate.orEmpty(),
            titleText = if (editingGoal == null) "Add Goal" else "Edit Goal",
            onSave = { title, desc, endDate ->
                scope.launch {
                    try {
                        if (editingGoal == null) {
                            goalStore.addGoal(title, desc, endDate)
                        } else {
                            goalStore.updateGoal(editingGoal!!.id, title, desc, endDate)
                        }
                        showAddScreen = false
                        editingGoal = null
                    } catch (limit: IllegalStateException) {
                        showLimitInfo = true
                    }
                }
            },
            onCancel = dismissAddScreen
        )
        return
    }

    // Ana ekran UI düzeni (dikey kolon)
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

                // Hedef yoksa mesaj göster, varsa kart içinde göster
                if (goalList.isEmpty()) {
                    Text(
                        "No goals yet.",
                        color = colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                } else {
                    goalList.forEach { goal ->
                        GoalCard(
                            goal = goal,
                            colorScheme = colorScheme,
                            onEdit = {
                                editingGoal = goal
                                showAddScreen = true
                            },
                            onDelete = { pendingDeleteGoal = goal }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                FilledTonalButton(
                    onClick = {
                        if (isLimitReached) {
                            showLimitInfo = true
                        } else {
                            showAddScreen = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isLimitReached) colorScheme.surfaceVariant.copy(alpha = 0.5f) else colorScheme.primaryContainer,
                        contentColor = if (isLimitReached) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(if (isLimitReached) "Goal limit reached" else "Add goal")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        com.arif.randomcheckin.notifications.NotificationHelper
                            .showCheckInNotification(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondaryContainer)
                ) {
                    Text("Test notification", color = colorScheme.onSecondaryContainer)
                }
            }

            if (pendingDeleteGoal != null) {
                DeleteConfirmationDialog(
                    colorScheme = colorScheme,
                    onDismiss = { pendingDeleteGoal = null },
                    onConfirm = {
                        scope.launch {
                            pendingDeleteGoal?.let { goalStore.removeGoal(it.id) }
                            pendingDeleteGoal = null
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = showLimitInfo,
                enter = fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = fadeOut(animationSpec = tween(durationMillis = 180))
            ) {
                val overlayColor = Color.Black.copy(alpha = 0.55f)
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { showLimitInfo = false }
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
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    colorScheme: ColorScheme,
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
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit goal", tint = colorScheme.onSurfaceVariant)
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
