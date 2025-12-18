package com.arif.randomcheckin.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalScreen(
    initialTitle: String = "",
    initialDescription: String = "",
    initialEndDate: String = "",
    titleText: String = if (initialTitle.isBlank()) "Add Goal" else "Edit Goal",
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val today = remember { LocalDate.now() }
    val focusManager = LocalFocusManager.current

    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var endDate by rememberSaveable { mutableStateOf(initialEndDate) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val initialMillis = remember(initialEndDate) {
        initialEndDate.takeIf { it.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")) }?.let {
            runCatching {
                LocalDate.parse(it, dateFormatter)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                return !date.isBefore(today)
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selected = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            endDate = selected.format(dateFormatter)
                            dateError = null
                            showDatePicker = false
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(titleText, style = MaterialTheme.typography.headlineMedium)

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

        val interactionSource = remember { MutableInteractionSource() }
        OutlinedTextField(
            value = endDate,
            onValueChange = { },
            label = { Text("End date (dd.MM.yyyy)") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    focusManager.clearFocus()
                    showDatePicker = true
                },
            readOnly = true,
            enabled = true,
            trailingIcon = {
                IconButton(onClick = {
                    focusManager.clearFocus()
                    showDatePicker = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Pick date"
                    )
                }
            },
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
                if (endDate.isValidGoalDate(today)) {
                    onSave(title, description, endDate.trim())
                } else {
                    dateError = "Tarih bugün veya sonrası olmalı"
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

private fun String.isValidGoalDate(today: LocalDate = LocalDate.now()): Boolean {
    val parts = trim().split('.')
    if (parts.size != 3) return false
    val day = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    val year = parts[2].toIntOrNull() ?: return false
    if (day !in 1..31) return false
    if (month !in 1..12) return false
    if (year !in 2024..2066) return false
    val parsed = runCatching {
        LocalDate.of(year, month, day)
    }.getOrNull() ?: return false
    return !parsed.isBefore(today)
}
