@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package de.protech.termine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.protech.termine.look.AppCard
import de.protech.termine.look.fieldShape
import de.protech.termine.data.Appointment
import de.protech.termine.data.Category
import de.protech.termine.data.Marker
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
    val isNew = appointmentId == 0L
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    val existing by viewModel.appointment(appointmentId)
        .collectAsStateWithLifecycle(initialValue = null)

    var loaded by remember { mutableStateOf(isNew) }
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var dateTime by remember {
        mutableStateOf(LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0))
    }
    val reminders = remember { mutableStateListOf<Int>() }
    val chosenMarkers = remember { mutableStateListOf<Long>() }

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
            chosenMarkers.clear()
            chosenMarkers.addAll(full.markers.map { it.id })
            loaded = true
        }
    }

    LaunchedEffect(Unit) {
        if (isNew && reminders.isEmpty()) {
            reminders.add(1440)
            reminders.add(60)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomReminder by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Neuer Termin" else "Termin ändern") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Termin löschen")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        val millis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        viewModel.save(
                            Appointment(
                                id = appointmentId,
                                title = title.trim().ifBlank { "Termin" },
                                categoryId = categoryId,
                                startMillis = millis,
                                location = location.trim(),
                                notes = notes.trim()
                            ),
                            reminders.toList(),
                            chosenMarkers.toList(),
                            onDone = onBack
                        )
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (isNew) "Termin speichern" else "Änderung speichern")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Section(title = "Worum geht es?") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bezeichnung") },
                    placeholder = { Text("z. B. Kontrolle oder Blutabnahme") },
                    singleLine = true,
                    shape = fieldShape(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (categories.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Kategorie",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            CategoryChip(
                                category = category,
                                selected = categoryId == category.id,
                                onClick = {
                                    categoryId = if (categoryId == category.id) null else category.id
                                }
                            )
                        }
                    }
                }

                if (markers.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Kennzeichen",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Mehrere moeglich; sie stehen alphabetisch neben der Kategorie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        markers.forEach { marker ->
                            MarkerChip(
                                marker = marker,
                                selected = chosenMarkers.contains(marker.id),
                                onClick = {
                                    if (chosenMarkers.contains(marker.id)) {
                                        chosenMarkers.remove(marker.id)
                                    } else {
                                        chosenMarkers.add(marker.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Section(title = "Wann?") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickDate("Morgen", 1, dateTime) { dateTime = it }
                    QuickDate("Übermorgen", 2, dateTime) { dateTime = it }
                    QuickDate("In 1 Woche", 7, dateTime) { dateTime = it }
                    QuickDate("In 4 Wochen", 28, dateTime) { dateTime = it }
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = fieldShape(),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(formatNumericDate(dateTime.toEpochMillisHere()))
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        shape = fieldShape(),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(formatTime(dateTime.toEpochMillisHere()))
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "${formatLongDate(dateTime.toEpochMillisHere())} · " +
                        relativeDay(dateTime.toEpochMillisHere()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Section(
                title = "Erinnerung",
                subtitle = "Mehrere sind möglich – etwa am Vortag und noch mal kurz vorher."
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    REMINDER_PRESETS.forEach { minutes ->
                        FilterChip(
                            selected = reminders.contains(minutes),
                            onClick = {
                                if (reminders.contains(minutes)) reminders.remove(minutes)
                                else reminders.add(minutes)
                            },
                            label = { Text(reminderChipLabel(minutes)) }
                        )
                    }
                    reminders.filter { it !in REMINDER_PRESETS }.sorted().forEach { minutes ->
                        FilterChip(
                            selected = true,
                            onClick = { reminders.remove(minutes) },
                            label = { Text(reminderChipLabel(minutes)) }
                        )
                    }
                    AssistChip(
                        onClick = { showCustomReminder = true },
                        label = { Text("eigene Zeit") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (reminders.isEmpty()) "Keine Erinnerung – der Termin steht nur in der Liste."
                    else reminders.sorted().joinToString(", ") { reminderLabel(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reminders.isEmpty()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Section(title = "Noch etwas?") {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ort oder Praxis") },
                    singleLine = true,
                    shape = fieldShape(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen") },
                    placeholder = { Text("Überweisung, Nüchtern kommen, Karte mitnehmen …") },
                    minLines = 3,
                    shape = fieldShape(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))
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
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Behalten") }
            }
        )
    }
}

/* ---------- Bausteine ---------- */

private fun LocalDateTime.toEpochMillisHere(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
private fun Section(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Spacer(Modifier.height(14.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category.name) },
        leadingIcon = {
            Box(
                Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Color(category.colorArgb))
            )
        }
    )
}

@Composable
private fun MarkerChip(marker: Marker, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(marker.name) },
        leadingIcon = {
            Box(
                Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Color(marker.colorArgb))
            )
        }
    )
}

@Composable
private fun QuickDate(
    label: String,
    daysFromToday: Long,
    current: LocalDateTime,
    onPick: (LocalDateTime) -> Unit
) {
    val target = LocalDate.now().plusDays(daysFromToday)
    AssistChip(
        onClick = { onPick(LocalDateTime.of(target, current.toLocalTime())) },
        label = { Text(label) }
    )
}

@Composable
private fun CustomReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(ReminderUnit.MINUTEN) }
    val minutes = (amount.toIntOrNull() ?: 0) * unit.factor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eigene Vorlaufzeit") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { new -> amount = new.filter { it.isDigit() }.take(4) },
                    label = { Text("Wie viel vorher?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = fieldShape(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderUnit.entries.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unit = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                if (minutes > 0) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Erinnerung ${reminderLabel(minutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes) }, enabled = minutes > 0) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

private enum class ReminderUnit(val label: String, val factor: Int) {
    MINUTEN("Minuten", 1),
    STUNDEN("Stunden", 60),
    TAGE("Tage", 1440)
}
