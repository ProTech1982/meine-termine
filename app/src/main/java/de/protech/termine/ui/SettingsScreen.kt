@file:OptIn(ExperimentalMaterial3Api::class)

package de.protech.termine.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.protech.termine.BuildConfig
import de.protech.termine.update.UpdateInfo
import de.protech.termine.update.UpdateResult
import de.protech.termine.update.Updater
import kotlinx.coroutines.launch

/** Was Android der App gerade erlaubt. */
data class SetupStatus(
    val notificationsAllowed: Boolean = true,
    val exactAlarmsAllowed: Boolean = true,
    val batteryUnrestricted: Boolean = true
) {
    val allGood: Boolean
        get() = notificationsAllowed && exactAlarmsAllowed && batteryUnrestricted
}

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Failed(val message: String) : UpdateState
}

@Composable
fun SettingsScreen(
    status: SetupStatus,
    onBack: () -> Unit,
    onCategories: () -> Unit,
    onLegal: () -> Unit,
    onFixNotifications: () -> Unit,
    onFixExactAlarms: () -> Unit,
    onFixBattery: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            /* --- Erinnerungen --- */
            SettingsCard(
                title = "Erinnerungen",
                subtitle = if (status.allGood)
                    "Alles erlaubt – Erinnerungen kommen auf die Minute."
                else
                    "Android bremst die App noch. Die rot markierten Punkte bitte freigeben."
            ) {
                StatusRow(
                    icon = Icons.Default.Notifications,
                    label = "Benachrichtigungen",
                    detail = "Ohne diese Freigabe bleibt die Erinnerung stumm.",
                    ok = status.notificationsAllowed,
                    onFix = onFixNotifications
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                StatusRow(
                    icon = Icons.Default.Schedule,
                    label = "Wecker und Erinnerungen",
                    detail = "Erlaubt exakte Alarme – sonst verschiebt Android sie.",
                    ok = status.exactAlarmsAllowed,
                    onFix = onFixExactAlarms
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                StatusRow(
                    icon = Icons.Default.BatteryAlert,
                    label = "Akku-Optimierung aus",
                    detail = "Verhindert, dass die App im Schlaf übergangen wird.",
                    ok = status.batteryUnrestricted,
                    onFix = onFixBattery
                )
            }

            /* --- Kategorien --- */
            SettingsCard(
                title = "Kategorien",
                subtitle = "Namen und Farben der Arztarten – frei anpassbar."
            ) {
                OutlinedButton(
                    onClick = onCategories,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Label, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Kategorien verwalten")
                }
            }

            /* --- Update --- */
            SettingsCard(
                title = "Version und Update",
                subtitle = "Installiert: ${BuildConfig.VERSION_NAME}"
            ) {
                Text(
                    text = "Die App schaut auf GitHub nach, ob es eine neuere Fassung gibt. " +
                        "Heruntergeladen und installiert wird über den Browser – " +
                        "die App installiert selbst nichts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        updateState = UpdateState.Checking
                        scope.launch {
                            updateState = when (val result = Updater.check()) {
                                is UpdateResult.Available -> UpdateState.Available(result.info)
                                is UpdateResult.UpToDate -> UpdateState.UpToDate
                                is UpdateResult.Failed -> UpdateState.Failed(result.message)
                            }
                        }
                    },
                    enabled = updateState !is UpdateState.Checking,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (updateState is UpdateState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Suche läuft …")
                    } else {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Nach Update suchen")
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { Updater.openInBrowser(context, Updater.releasesPage) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Alle Versionen im Browser")
                }
            }

            SettingsCard(
                title = "Über & Rechtliches",
                subtitle = "Diese App wurde mit Hilfe von KI entwickelt."
            ) {
                Text(
                    text = "Konzept und Vorgaben stammen von ProTech1982, den Programmcode " +
                        "hat überwiegend Claude (Anthropic) geschrieben. Dort stehen auch " +
                        "Haftung, Datenschutz und die Lizenzen der verwendeten Bibliotheken.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onLegal,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Hinweise und Lizenzen")
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Meine Termine · ${Updater.repo}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    /* --- Dialoge zur Update-Suche --- */
    when (val state = updateState) {
        is UpdateState.UpToDate -> AlertDialog(
            onDismissRequest = { updateState = UpdateState.Idle },
            title = { Text("Alles aktuell") },
            text = { Text("Version ${BuildConfig.VERSION_NAME} ist die neueste.") },
            confirmButton = {
                TextButton(onClick = { updateState = UpdateState.Idle }) { Text("Alles klar") }
            }
        )

        is UpdateState.Available -> AlertDialog(
            onDismissRequest = { updateState = UpdateState.Idle },
            title = { Text("Version ${state.info.versionName} ist da") },
            text = {
                Column {
                    Text(
                        text = "Installiert ist ${BuildConfig.VERSION_NAME}." +
                            if (state.info.sizeBytes > 0)
                                " Der Download ist etwa ${state.info.sizeBytes / 1024 / 1024} MB groß."
                            else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.info.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.info.notes.lineSequence()
                                .filter { it.isNotBlank() }
                                .take(6)
                                .joinToString("\n"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Der Browser öffnet sich und lädt die Datei. " +
                            "Danach in den Downloads antippen, um sie einzuspielen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Updater.openInBrowser(context, state.info.apkUrl)
                    updateState = UpdateState.Idle
                }) { Text("Im Browser laden") }
            },
            dismissButton = {
                TextButton(onClick = { updateState = UpdateState.Idle }) { Text("Später") }
            }
        )

        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = { updateState = UpdateState.Idle },
            title = { Text("Nachsehen ging nicht") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = {
                    Updater.openInBrowser(context, Updater.releasesPage)
                    updateState = UpdateState.Idle
                }) { Text("Im Browser nachsehen") }
            },
            dismissButton = {
                TextButton(onClick = { updateState = UpdateState.Idle }) { Text("Schließen") }
            }
        )

        else -> Unit
    }
}

/* ---------- Bausteine ---------- */

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Spacer(Modifier.height(14.dp))
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
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
private fun StatusRow(
    icon: ImageVector,
    label: String,
    detail: String,
    ok: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = if (ok) "erlaubt" else "noch nicht erlaubt",
                    modifier = Modifier.size(17.dp),
                    tint = if (ok) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!ok) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onFix,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 4.dp, vertical = 0.dp
                    )
                ) { Text("Freigeben") }
            }
        }
    }
}
