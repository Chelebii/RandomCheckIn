package com.arif.randomcheckin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RandomCheckInApp()
        }
    }
}

@Composable
fun RandomCheckInApp() {
    MaterialTheme {
        GoalListScreen()
    }
}

@Composable
fun GoalListScreen() {

    val context = LocalContext.current
    val goalStore = com.arif.randomcheckin.data.model.GoalStore(context)
    val scope = rememberCoroutineScope()

    var showAddScreen by remember { mutableStateOf(false) }

    val goalState = goalStore.goalFlow()
        .collectAsState(initial = null).value

    if (showAddScreen) {
        com.arif.randomcheckin.ui.theme.AddGoalScreen(
            onSave = { title, desc, endDate ->
                scope.launch {
                    goalStore.saveGoal(title, desc, endDate)
                    showAddScreen = false
                }
            },
            onCancel = {
                showAddScreen = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Goals",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (goalState == null) {
            Text("No goal yet.")
        } else {
            val (title, desc, endDate) = goalState

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Bitiş: $endDate")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { showAddScreen = true }) {
            Text("Add goal")
        }
    }
}
