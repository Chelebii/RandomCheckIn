package com.arif.randomcheckin.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddGoalScreen(
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Add Goal", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = endDate,
            onValueChange = {
                endDate = it
                dateError = null
            },
            label = { Text("End date (örn: 31.12.2025)") },
            modifier = Modifier.fillMaxWidth(),
            isError = dateError != null,
            supportingText = {
                if (dateError != null) {
                    Text(dateError!!)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(onClick = {
                if (endDate.isValidGoalDate()) {
                    onSave(title, description, endDate.trim())
                } else {
                    dateError = "Tarih dd.MM.yyyy ve yıl ≤ 2066 olmalı"
                }
            }) {
                Text("Save")
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

private fun String.isValidGoalDate(): Boolean {
    val parts = trim().split('.')
    if (parts.size != 3) return false
    val day = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    val year = parts[2].toIntOrNull() ?: return false
    if (day !in 1..31) return false
    if (month !in 1..12) return false
    if (year !in 1..2066) return false
    return true
}
