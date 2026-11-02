@file:OptIn(ExperimentalMaterial3Api::class)

package de.protech.termine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.protech.termine.BuildConfig
import de.protech.termine.data.AppointmentFull

@Composable
fun ListScreen(
    viewModel: TermineViewModel,
    showSetupHint: Boolean,
    onDismissSetupHint: () -> Unit,
    onFixSetup: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onCategories: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPast by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Termine") },
                actions = {
                    IconButton(onClick = onCheckUpdate) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = "Nach Update suchen")
                    }
                    IconButton(onClick = onCategories) {
                        Icon(Icons.Default.Label, contentDescription = "Kategorien verwalten")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Termin hinzufügen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showSetupHint) {
                item { SetupHintCard(onFix = onFixSetup, onDismiss = onDismissSetupHint) }
            }

            if (state.categories.isNotEmpty()) {
                item {
                    CategoryFilterRow(
                        state = state,
                        onSelect = { viewModel.setFilter(it) }
                    )
                }
            }

            if (state.upcoming.isEmpty()) {
                item { EmptyHint(hasFilter = state.activeFilter != null) }
            } else {
                item {
                    SectionLabel("Als Nächstes")
                }
                item {
                    NextAppointmentCard(
                        full = state.upcoming.first(),
                        onClick = { onOpen(state.upcoming.first().appointment.id) }
                    )
                }

                val rest = state.upcoming.drop(1)
                if (rest.isNotEmpty()) {
                    item { SectionLabel("Danach") }
                    items(rest, key = { it.appointment.id }) { full ->
                        AppointmentCard(full = full, onClick = { onOpen(full.appointment.id) })
                    }
                }
            }

            if (state.past.isNotEmpty()) {
                item {
                    TextButton(onClick = { showPast = !showPast }) {
                        Text(
                            if (showPast) "Vergangene Termine ausblenden"
                            else "Vergangene Termine anzeigen (${state.past.size})"
                        )
                    }
                }
                if (showPast) {
                    items(state.past, key = { "past-" + it.appointment.id }) { full ->
                        AppointmentCard(
                            full = full,
                            onClick = { onOpen(full.appointment.id) },
                            dimmed = true
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun CategoryFilterRow(state: ListUiState, onSelect: (Long?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.activeFilter == null,
            onClick = { onSelect(null) },
            label = { Text("Alle") }
        )
        state.categories.forEach { category ->
            FilterChip(
                selected = state.activeFilter == category.id,
                onClick = { onSelect(if (state.activeFilter == category.id) null else category.id) },
                label = { Text(category.name) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(category.colorArgb))
                    )
                }
            )
        }
    }
}

@Composable
private fun NextAppointmentCard(full: AppointmentFull, onClick: () -> Unit) {
    val accent = full.category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(6.dp)
                    .height(140.dp)
                    .background(accent)
            )
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = countdown(full.appointment.startMillis),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = full.appointment.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${formatDate(full.appointment.startMillis)} · ${formatTime(full.appointment.startMillis)} Uhr",
                    style = MaterialTheme.typography.bodyMedium
                )
                full.category?.let {
                    Spacer(Modifier.height(4.dp))
                    CategoryPill(name = it.name, color = Color(it.colorArgb))
                }
                if (full.appointment.location.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    IconLine(Icons.Default.Place, full.appointment.location)
                }
                if (full.reminders.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    IconLine(
                        Icons.Default.NotificationsActive,
                        full.reminders.sortedBy { it.minutesBefore }
                            .joinToString(" · ") { reminderLabel(it.minutesBefore) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    full: AppointmentFull,
    onClick: () -> Unit,
    dimmed: Boolean = false
) {
    val accent = full.category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(6.dp)
                    .height(88.dp)
                    .background(if (dimmed) accent.copy(alpha = 0.35f) else accent)
            )
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = full.appointment.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${formatShortDate(full.appointment.startMillis)} · " +
                        "${formatTime(full.appointment.startMillis)} Uhr · " +
                        relativeDay(full.appointment.startMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                full.category?.let {
                    Spacer(Modifier.height(6.dp))
                    CategoryPill(name = it.name, color = Color(it.colorArgb))
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IconLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyHint(hasFilter: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = if (hasFilter) "Keine Termine in dieser Kategorie"
                else "Noch keine Termine eingetragen",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Mit dem Plus unten rechts legst du einen Termin an.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SetupHintCard(onFix: () -> Unit, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Erinnerungen noch nicht zuverlässig",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hinweis schließen",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Android darf die App gerade noch schlafen legen. " +
                    "Damit Erinnerungen auf die Minute kommen, bitte exakte Alarme erlauben " +
                    "und die Akku-Optimierung für diese App ausschalten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onFix) { Text("Jetzt einstellen") }
        }
    }
}
