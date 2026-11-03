@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package de.protech.termine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.protech.termine.data.Marker
import de.protech.termine.look.AppCard

private val MARKER_PALETTE = listOf(
    0xFF1565C0, 0xFF00695C, 0xFFB71C1C, 0xFF4527A0,
    0xFF2E7D32, 0xFFEF6C00, 0xFFAD1457, 0xFF455A64,
    0xFF00838F, 0xFF6A1B9A, 0xFFD84315, 0xFF558B2F
).map { it.toInt() }

/**
 * Kennzeichen sind das Zusatzschild neben der Kategorie, zum Beispiel
 * "Labor" oder "Blutprobe". Verwaltung wie bei den Kategorien, nur dass
 * ein Termin hier mehrere tragen darf.
 */
@Composable
fun MarkerScreen(
    viewModel: TermineViewModel,
    onBack: () -> Unit
) {
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Marker?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Marker?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Kennzeichen") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Kennzeichen") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Kennzeichen stehen zusätzlich neben der Kategorie, zum Beispiel " +
                        "\"Labor\" neben \"Diabetologe\". Ein Termin kann mehrere tragen; " +
                        "sie erscheinen alphabetisch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
            items(markers, key = { it.id }) { marker ->
                AppCard(
                    onClick = { editing = marker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(marker.colorArgb))
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(marker.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { deleting = marker }) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        MarkerDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { name, color ->
                viewModel.addMarker(name, color)
                creating = false
            }
        )
    }

    editing?.let { marker ->
        MarkerDialog(
            initial = marker,
            onDismiss = { editing = null },
            onSave = { name, color ->
                viewModel.updateMarker(marker.copy(name = name, colorArgb = color))
                editing = null
            }
        )
    }

    deleting?.let { marker ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Kennzeichen löschen?") },
            text = {
                Text(
                    "\"${marker.name}\" wird entfernt und verschwindet bei allen Terminen, " +
                        "die es tragen. Die Termine selbst bleiben erhalten."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMarker(marker.id)
                    deleting = null
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun MarkerDialog(
    initial: Marker?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.colorArgb ?: MARKER_PALETTE.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Neues Kennzeichen" else "Kennzeichen bearbeiten") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z. B. Labor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Farbe", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MARKER_PALETTE.forEach { option ->
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(option))
                                .border(
                                    width = if (option == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { color = option }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), color) },
                enabled = name.isNotBlank()
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
