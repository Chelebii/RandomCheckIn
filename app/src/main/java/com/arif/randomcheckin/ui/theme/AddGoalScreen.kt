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

private const val END_DATE_PATTERN = "\\d{2}\\.\\d{2}\\.\\d{4}"
private val ALLOWED_GOAL_YEARS = 2024..2066

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
        parseInitialDateMillis(initialEndDate, dateFormatter)
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

    val openDatePicker: () -> Unit = {
        focusManager.clearFocus()
        showDatePicker = true
    }

    val applySelectedDate: () -> Unit = {
        datePickerState.selectedDateMillis?.let { millis ->
            val selected = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            endDate = selected.format(dateFormatter)
            dateError = null
            showDatePicker = false
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = applySelectedDate,
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
        // Field stays read-only to force usage of the picker and avoid invalid manual input.
        OutlinedTextField(
            value = endDate,
            onValueChange = { },
            label = { Text("End date (dd.MM.yyyy)") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = openDatePicker
                ),
            readOnly = true,
            enabled = true,
            trailingIcon = {
                IconButton(onClick = openDatePicker) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Pick date"
                    )
                }
            },
            isError = dateError != null,
            supportingText = {
                dateError?.let { Text(it) }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(onClick = {
                val trimmedEndDate = endDate.trim()
                if (trimmedEndDate.isValidGoalDate(today)) {
                    onSave(title, description, trimmedEndDate)
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

private fun parseInitialDateMillis(
    initialEndDate: String,
    formatter: DateTimeFormatter
): Long? {
    if (!initialEndDate.matches(Regex(END_DATE_PATTERN))) {
        return null
    }
    return runCatching {
        LocalDate.parse(initialEndDate, formatter)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private fun String.isValidGoalDate(today: LocalDate = LocalDate.now()): Boolean {
    val parts = trim().split('.')
    if (parts.size != 3) return false
    val day = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    val year = parts[2].toIntOrNull() ?: return false
    if (day !in 1..31) return false
    if (month !in 1..12) return false
    if (year !in ALLOWED_GOAL_YEARS) return false
    val parsed = runCatching {
        LocalDate.of(year, month, day)
    }.getOrNull() ?: return false
    return !parsed.isBefore(today)
}
