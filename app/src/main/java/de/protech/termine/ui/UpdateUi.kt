package de.protech.termine.ui

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.protech.termine.BuildConfig
import de.protech.termine.update.ApkDownloader
import de.protech.termine.update.UpdateChecker
import de.protech.termine.update.UpdateInfo
import de.protech.termine.update.UpdateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class NeedsPermission(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateUiState
    data class Ready(val file: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var state by mutableStateOf<UpdateUiState>(UpdateUiState.Idle)
        private set

    fun dismiss() {
        state = UpdateUiState.Idle
    }

    fun check() {
        state = UpdateUiState.Checking
        scope.launch {
            state = when (val result = UpdateChecker.check()) {
                is UpdateResult.Available -> UpdateUiState.Available(result.info)
                is UpdateResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateResult.Failed -> UpdateUiState.Error(result.message)
            }
        }
    }

    fun startDownload(info: UpdateInfo) {
        if (!ApkDownloader.canInstall(context)) {
            state = UpdateUiState.NeedsPermission(info)
            return
        }
        state = UpdateUiState.Downloading(info, 0f)
        scope.launch {
            val result = ApkDownloader.download(context, info) { progress ->
                val current = state
                if (current is UpdateUiState.Downloading) {
                    state = current.copy(progress = progress)
                }
            }
            state = result.fold(
                onSuccess = { UpdateUiState.Ready(it) },
                onFailure = { UpdateUiState.Error(it.message ?: "Download fehlgeschlagen") }
            )
        }
    }

    fun grantInstallPermission() {
        ApkDownloader.openInstallPermissionSettings(context)
    }

    fun install(file: File) {
        runCatching { ApkDownloader.install(context, file) }
            .onFailure { state = UpdateUiState.Error(it.message ?: "Installer ließ sich nicht öffnen") }
    }
}

@Composable
fun rememberUpdateController(): UpdateController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context) { UpdateController(context, scope) }
}

@Composable
fun UpdateDialogs(controller: UpdateController) {
    when (val state = controller.state) {
        UpdateUiState.Idle -> Unit

        UpdateUiState.Checking -> AlertDialog(
            onDismissRequest = { controller.dismiss() },
            title = { Text("Suche nach Update") },
            text = {
                Column {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("GitHub wird abgefragt …")
                }
            },
            confirmButton = {
                TextButton(onClick = { controller.dismiss() }) { Text("Abbrechen") }
            }
        )

        UpdateUiState.UpToDate -> AlertDialog(
            onDismissRequest = { controller.dismiss() },
            title = { Text("Alles aktuell") },
            text = { Text("Du hast bereits Version ${BuildConfig.VERSION_NAME}.") },
            confirmButton = {
                TextButton(onClick = { controller.dismiss() }) { Text("Alles klar") }
            }
        )

        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = { controller.dismiss() },
            title = { Text("Version ${state.info.versionName} ist da") },
            text = {
                Column {
                    Text(
                        "Installiert: ${BuildConfig.VERSION_NAME}" +
                            if (state.info.sizeBytes > 0)
                                " · Download: ${state.info.sizeBytes / 1024 / 1024} MB" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.info.notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            state.info.notes.lineSequence().take(8).joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { controller.startDownload(state.info) }) {
                    Text("Herunterladen")
                }
            },
            dismissButton = {
                TextButton(onClick = { controller.dismiss() }) { Text("Später") }
            }
        )

        is UpdateUiState.NeedsPermission -> AlertDialog(
            onDismissRequest = { controller.dismiss() },
            title = { Text("Installation erlauben") },
            text = {
                Text(
                    "Android muss dieser App einmalig erlauben, Updates zu installieren. " +
                        "Danach einfach noch einmal auf Herunterladen tippen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    controller.grantInstallPermission()
                    controller.dismiss()
                }) { Text("Einstellung öffnen") }
            },
            dismissButton = {
                TextButton(onClick = { controller.dismiss() }) { Text("Abbrechen") }
            }
        )

        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = { },
            title = { Text("Update wird geladen") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${(state.progress * 100).toInt()} % von Version ${state.info.versionName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = { }
        )

        is UpdateUiState.Ready -> AlertDialog(
            onDismissRequest = { controller.dismiss() },
            title = { Text("Bereit zum Installieren") },
            text = { Text("Die neue Version ist geladen. Android fragt gleich nach der Bestätigung.") },
            confirmButton = {
                TextButton(onClick = {
                    controller.install(state.file)
                    controller.dismiss()
                }) { Text("Jetzt installieren") }
            },
            dismissButton = {
                TextButton(onClick = { controller.dismiss() }) { Text("Später") }
            }
        )

        is UpdateUiState.Error -> AlertDialog(
            onDismissRequest = { controller.dismiss() },
            title = { Text("Hat nicht geklappt") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = { controller.check() }) { Text("Nochmal") }
            },
            dismissButton = {
                TextButton(onClick = { controller.dismiss() }) { Text("Schließen") }
            }
        )
    }
}
