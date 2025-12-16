package com.arif.randomcheckin.notifications


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arif.randomcheckin.data.model.GoalStore
import kotlinx.coroutines.launch

class CheckInActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = GoalStore(this)
        val activity = this

        setContent {
            val scope = rememberCoroutineScope()
            MaterialTheme {
                CheckInScreen(
                    onSave = { note ->
                        scope.launch {
                            store.saveDailyNote(note)
                            activity.finish()
                        }
                    },
                    onCancel = { activity.finish() }
                )
            }
        }
    }
}

@Composable
private fun CheckInScreen(
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bugün kısa not",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 160) note = it },
            label = { Text("Kısa yorum (isteğe bağlı)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = { onSave(note.trim()) }) {
                Text("Kaydet")
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(onClick = onCancel) {
                Text("İptal")
            }
        }
    }
}
