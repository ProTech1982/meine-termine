@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package de.protech.termine.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import de.protech.termine.data.Category
import de.protech.termine.data.Doctor
import de.protech.termine.look.AppCard

/**
 * Die eigenen Ärzte und Praxen. Beim Eintragen eines Termins stehen sie als
 * Schnellauswahl unter der Bezeichnung: ein Tipp setzt Name, Kategorie und
 * Ort ein, damit man das nicht jedes Mal neu schreiben muss.
 */
@Composable
fun DoctorScreen(
    viewModel: TermineViewModel,
    onBack: () -> Unit
) {
    val doctors by viewModel.doctors.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Doctor?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Doctor?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Ärzte") },
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
                text = { Text("Arzt") }
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
                    "Beim Eintragen eines Termins erscheinen diese Namen unter der " +
                        "Bezeichnung. Ein Tipp setzt Name, Kategorie und Ort ein – " +
                        "ändern lässt sich danach alles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }

            if (doctors.isEmpty()) {
                item {
                    Text(
                        "Noch kein Arzt eingetragen. Unten rechts auf „Arzt\" tippen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            items(doctors, key = { it.id }) { doctor ->
                val category = categories.firstOrNull { it.id == doctor.categoryId }
                AppCard(
                    onClick = { editing = doctor },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    category?.let { Color(it.colorArgb) }
                                        ?: MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(doctor.name, style = MaterialTheme.typography.titleMedium)
                            val details = listOfNotNull(
                                category?.name,
                                doctor.location.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                            if (details.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { deleting = doctor }) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        DoctorDialog(
            initial = null,
            categories = categories,
            onDismiss = { creating = false },
            onSave = { name, categoryId, location ->
                viewModel.addDoctor(name, categoryId, location)
                creating = false
            }
        )
    }

    editing?.let { doctor ->
        DoctorDialog(
            initial = doctor,
            categories = categories,
            onDismiss = { editing = null },
            onSave = { name, categoryId, location ->
                viewModel.updateDoctor(
                    doctor.copy(name = name, categoryId = categoryId, location = location)
                )
                editing = null
            }
        )
    }

    deleting?.let { doctor ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Arzt löschen?") },
            text = {
                Text(
                    "\"${doctor.name}\" verschwindet aus der Schnellauswahl. " +
                        "Bereits eingetragene Termine bleiben unverändert."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDoctor(doctor.id)
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
private fun DoctorDialog(
    initial: Doctor?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, Long?, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var location by remember { mutableStateOf(initial?.location ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Neuer Arzt" else "Arzt bearbeiten") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z. B. Dr. Weber") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ort (freiwillig)") },
                    placeholder = { Text("z. B. Lindenstr. 4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (categories.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Kategorie (freiwillig)", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val chosen = categoryId == category.id
                            FilterChip(
                                selected = chosen,
                                onClick = { categoryId = if (chosen) null else category.id },
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
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), categoryId, location.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
