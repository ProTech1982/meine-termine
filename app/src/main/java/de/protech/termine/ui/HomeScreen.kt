@file:OptIn(ExperimentalMaterial3Api::class)

package de.protech.termine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.protech.termine.look.AppCard
import de.protech.termine.look.LocalAppearance
import de.protech.termine.look.cardShape
import de.protech.termine.look.fieldShape
import de.protech.termine.data.AppointmentFull
import de.protech.termine.data.Category

@Composable
fun HomeScreen(
    viewModel: TermineViewModel,
    showSetupHint: Boolean,
    onDismissSetupHint: () -> Unit,
    onFixSetup: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPast by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Meine Termine",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Termin") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showSetupHint) {
                item(key = "hint") {
                    SetupHintCard(onFix = onFixSetup, onDismiss = onDismissSetupHint)
                }
            }

            if (state.categories.size > 1) {
                item(key = "filter") {
                    CategoryFilterRow(
                        categories = state.categories,
                        activeFilter = state.activeFilter,
                        onSelect = viewModel::setFilter
                    )
                }
            }

            val next = state.next
            if (next == null) {
                item(key = "empty") {
                    EmptyState(hasFilter = state.activeFilter != null, loaded = state.loaded)
                }
            } else {
                item(key = "next-${next.appointment.id}") {
                    NextCard(full = next, onClick = { onOpen(next.appointment.id) })
                }
            }

            items(
                items = state.agenda,
                key = { item ->
                    when (item) {
                        is AgendaItem.Header -> "h-${item.label}"
                        is AgendaItem.Entry -> "e-${item.full.appointment.id}"
                    }
                }
            ) { item ->
                when (item) {
                    is AgendaItem.Header -> GroupHeader(item.label, item.count)
                    is AgendaItem.Entry -> AppointmentRow(
                        full = item.full,
                        onClick = { onOpen(item.full.appointment.id) }
                    )
                }
            }

            if (state.past.isNotEmpty()) {
                item(key = "past-toggle") {
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = { showPast = !showPast },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (showPast) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (showPast) "Gewesene Termine ausblenden"
                            else "${state.past.size} gewesene Termine"
                        )
                    }
                }
                if (showPast) {
                    items(state.past, key = { "p-${it.appointment.id}" }) { full ->
                        AppointmentRow(
                            full = full,
                            onClick = { onOpen(full.appointment.id) },
                            dimmed = true
                        )
                    }
                }
            }
        }
    }
}

/* ---------- Bausteine ---------- */

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    activeFilter: Long?,
    onSelect: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = activeFilter == null,
            onClick = { onSelect(null) },
            label = { Text("Alle") }
        )
        categories.forEach { category ->
            val selected = activeFilter == category.id
            FilterChip(
                selected = selected,
                onClick = { onSelect(if (selected) null else category.id) },
                label = { Text(category.name) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(category.colorArgb))
                    )
                },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun GroupHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        if (count > 1) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NextCard(full: AppointmentFull, onClick: () -> Unit) {
    val accent = full.category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary
    val appointment = full.appointment

    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        container = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ALS NÄCHSTES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Der Termin selbst steht oben, der Countdown darunter in
            // kleinerer Schrift: beim Blick auf die Karte zaehlt zuerst,
            // wo es hingeht, danach wie viel Zeit noch bleibt.
            Text(
                text = appointment.title,
                style = MaterialTheme.typography.headlineMedium
            )

            full.category?.let { category ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(category.colorArgb).copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(category.colorArgb))
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = countdown(appointment.startMillis),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(14.dp))

            InfoLine(
                icon = Icons.Default.CalendarMonth,
                text = "${formatLongDate(appointment.startMillis)}\n" +
                    "${formatTime(appointment.startMillis)} Uhr · ${relativeDay(appointment.startMillis)}"
            )

            if (appointment.location.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                InfoLine(icon = Icons.Default.Place, text = appointment.location)
            }

            if (full.reminders.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                InfoLine(
                    icon = Icons.Default.NotificationsActive,
                    text = full.reminders.sortedBy { it.minutesBefore }
                        .joinToString(" · ") { reminderChipLabel(it.minutesBefore) } + " vorher"
                )
            }
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AppointmentRow(
    full: AppointmentFull,
    onClick: () -> Unit,
    dimmed: Boolean = false
) {
    val appointment = full.appointment
    val accent = full.category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.primary
    val alpha = if (dimmed) 0.55f else 1f

    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Datumsplakette links – macht die Liste auf einen Blick lesbar.
            Surface(
                shape = fieldShape(),
                color = accent.copy(alpha = if (dimmed) 0.10f else 0.16f),
                modifier = Modifier.size(52.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = weekdayShort(appointment.startMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                    )
                    Text(
                        text = dayOfMonth(appointment.startMillis),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "${formatTime(appointment.startMillis)} Uhr",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent.copy(alpha = alpha)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appointment.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                val subtitle = buildList {
                    full.category?.let { add(it.name) }
                    if (appointment.location.isNotBlank()) add(appointment.location)
                }.joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        maxLines = 1
                    )
                }
            }

            if (full.reminders.isNotEmpty() && !dimmed) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = "Erinnerung gesetzt",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(hasFilter: Boolean, loaded: Boolean) {
    if (!loaded) return

    AppCard(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (hasFilter) "Hier steht gerade nichts an"
                else "Keine Termine eingetragen",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (hasFilter) "In dieser Kategorie ist nichts offen."
                else "Unten rechts auf „Termin“ tippen, dann kann es losgehen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SetupHintCard(onFix: () -> Unit, onDismiss: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        container = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Erinnerungen noch nicht sicher",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hinweis ausblenden",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Android darf die App gerade noch schlafen legen. " +
                    "Zwei Schalter, dann kommen die Erinnerungen auf die Minute.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onFix) { Text("Jetzt einrichten") }
        }
    }
}
