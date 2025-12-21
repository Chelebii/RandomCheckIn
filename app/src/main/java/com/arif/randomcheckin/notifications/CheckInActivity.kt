package com.arif.randomcheckin.notifications

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arif.randomcheckin.data.GoalStore
import kotlinx.coroutines.launch

private const val DAILY_NOTE_MAX_CHAR = 160

class CheckInActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val goalStore = GoalStore(this)

        setContent {
            val scope = rememberCoroutineScope()
            MaterialTheme {
                CheckInScreen(
                    onSave = { note ->
                        scope.launch {
                            goalStore.saveDailyNote(note)
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

/**
 * Lightweight screen hosts the text input directly inside the notification activity so no ViewModel
 * is needed; note text stays local because business rules live elsewhere.
 */
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
            onValueChange = { updated ->
                if (updated.length <= DAILY_NOTE_MAX_CHAR) {
                    note = updated
                }
            },
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
