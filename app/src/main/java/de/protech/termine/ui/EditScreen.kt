@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package de.protech.termine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.protech.termine.data.Appointment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun EditScreen(
    viewModel: TermineViewModel,
    appointmentId: Long,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val existing by viewModel.appointment(appointmentId)
        .collectAsStateWithLifecycle(initialValue = null)

    var loaded by remember { mutableStateOf(appointmentId == 0L) }
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var dateTime by remember {
        mutableStateOf(
            LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0)
        )
    }
    val reminders = remember { mutableStateListOf<Int>() }

    // Bestehenden Termin einmalig in die Felder uebernehmen.
    LaunchedEffect(existing) {
        val full = existing
        if (!loaded && full != null) {
            title = full.appointment.title
            location = full.appointment.location
            notes = full.appointment.notes
            categoryId = full.appointment.categoryId
            dateTime = Instant.ofEpochMilli(full.appointment.startMillis)
                .atZone(ZoneId.systemDefault()).toLocalDateTime()
            reminders.clear()
            reminders.addAll(full.reminders.map { it.minutesBefore }.sorted())
            loaded = true
        }
    }

    // Standard-Erinnerung fuer neue Termine.
    LaunchedEffect(Unit) {
        if (appointmentId == 0L && reminders.isEmpty()) {
            reminders.add(60)
            reminders.add(1440)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomReminder by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (appointmentId == 0L) "Neuer Termin" else "Termin bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (appointmentId != 0L) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Bezeichnung") },
                placeholder = { Text("z. B. Kontrolle, Blutabnahme") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            CategoryPicker(
                categories = categories,
                selectedId = categoryId,
                onSelect = { categoryId = it }
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dateTime.toLocalDate()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dateTime.toLocalTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + " Uhr"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Erinnerungen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                "Mehrere sind möglich – zum Beispiel am Vortag und noch mal kurz vorher.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            ReminderChips(
                selected = reminders,
                onToggle = { minutes ->
                    if (reminders.contains(minutes)) reminders.remove(minutes)
                    else reminders.add(minutes)
                },
                onCustom = { showCustomReminder = true }
            )

            if (reminders.any { it !in REMINDER_PRESETS }) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Eigene: " + reminders.filter { it !in REMINDER_PRESETS }
                        .sorted().joinToString(", ") { reminderLabel(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Ort / Praxis") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notizen") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val millis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    viewModel.save(
                        Appointment(
                            id = appointmentId,
                            title = title.ifBlank { "Termin" },
                            categoryId = categoryId,
                            startMillis = millis,
                            location = location.trim(),
                            notes = notes.trim()
                        ),
                        reminders.toList(),
                        onDone = onBack
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toLocalDate()
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        dateTime = LocalDateTime.of(picked, dateTime.toLocalTime())
                    }
                    showDatePicker = false
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Uhrzeit") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dateTime = LocalDateTime.of(
                        dateTime.toLocalDate(),
                        LocalTime.of(timeState.hour, timeState.minute)
                    )
                    showTimePicker = false
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Abbrechen") }
            }
        )
    }

    if (showCustomReminder) {
        CustomReminderDialog(
            onDismiss = { showCustomReminder = false },
            onConfirm = { minutes ->
                if (minutes > 0 && !reminders.contains(minutes)) reminders.add(minutes)
                showCustomReminder = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Termin löschen?") },
            text = { Text("Der Termin und seine Erinnerungen werden entfernt.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(appointmentId, onDone = onBack)
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun CategoryPicker(
    categories: List<de.protech.termine.data.Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected?.name ?: "Keine Kategorie",
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategorie") },
            leadingIcon = selected?.let {
                {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(it.colorArgb))
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Keine Kategorie") },
                onClick = { onSelect(null); expanded = false }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(category.colorArgb))
                        )
                    },
                    onClick = { onSelect(category.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun ReminderChips(
    selected: List<Int>,
    onToggle: (Int) -> Unit,
    onCustom: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        REMINDER_PRESETS.forEach { minutes ->
            FilterChip(
                selected = selected.contains(minutes),
                onClick = { onToggle(minutes) },
                label = { Text(shortReminderLabel(minutes)) }
            )
        }
        AssistChip(onClick = onCustom, label = { Text("+ eigene") })
    }
}

private fun shortReminderLabel(minutes: Int): String = when {
    minutes <= 0 -> "pünktlich"
    minutes < 60 -> "$minutes Min"
    minutes == 60 -> "1 Std"
    minutes < 1440 -> "${minutes / 60} Std"
    minutes == 1440 -> "1 Tag"
    minutes < 10080 -> "${minutes / 1440} Tage"
    else -> "1 Woche"
}

@Composable
private fun CustomReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(Unit2.MINUTEN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eigene Erinnerung") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { new -> amount = new.filter { it.isDigit() }.take(4) },
                    label = { Text("Vorlaufzeit") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Unit2.entries.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unit = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val minutes = (amount.toIntOrNull() ?: 0) * unit.factor
                if (minutes > 0) {
                    Text(
                        "Erinnerung ${reminderLabel(minutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm((amount.toIntOrNull() ?: 0) * unit.factor) },
                enabled = (amount.toIntOrNull() ?: 0) > 0
            ) { Text("Hinzufügen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

private enum class Unit2(val label: String, val factor: Int) {
    MINUTEN("Minuten", 1),
    STUNDEN("Stunden", 60),
    TAGE("Tage", 1440)
}
