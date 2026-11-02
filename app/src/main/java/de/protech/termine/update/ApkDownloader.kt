package de.protech.termine.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Laedt die APK ueber den System-DownloadManager und uebergibt sie danach
 * an den Paket-Installer. Der Fortschritt wird gepollt, damit die App einen
 * Balken zeigen kann.
 */
object ApkDownloader {

    private const val FILE_NAME = "meine-termine-update.apk"
    private const val POLL_INTERVAL_MS = 300L

    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext<Result<File>>(Dispatchers.IO) {
        try {
            val manager = context.getSystemService<DownloadManager>()
                ?: return@withContext Result.failure(IllegalStateException("DownloadManager fehlt"))

            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return@withContext Result.failure(IllegalStateException("Kein Speicherort verfügbar"))

            val target = File(targetDir, FILE_NAME)
            if (target.exists()) target.delete()

            val request = DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle("Meine Termine ${info.versionName}")
                .setDescription("Update wird geladen")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
                .setAllowedOverRoaming(false)

            val id = manager.enqueue(request)
            var outcome: Result<File>? = null

            while (outcome == null) {
                outcome = pollOnce(manager, id, info, target, onProgress)
                if (outcome == null) delay(POLL_INTERVAL_MS)
            }

            outcome
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ein Blick in die Download-Warteschlange.
     * Rueckgabe null bedeutet: laeuft noch, bitte spaeter nochmal schauen.
     */
    private fun pollOnce(
        manager: DownloadManager,
        id: Long,
        info: UpdateInfo,
        target: File,
        onProgress: (Float) -> Unit
    ): Result<File>? {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id))
            ?: return Result.failure(IllegalStateException("Download nicht auffindbar"))

        cursor.use { c ->
            if (!c.moveToFirst()) {
                return Result.failure(IllegalStateException("Download verschwunden"))
            }

            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val soFar = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val reported = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val total = if (reported > 0) reported else info.sizeBytes

            if (total > 0) onProgress((soFar.toFloat() / total).coerceIn(0f, 1f))

            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    onProgress(1f)
                    Result.success(target)
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    Result.failure(IllegalStateException("Download fehlgeschlagen (Code $reason)"))
                }
                else -> null
            }
        }
    }

    /** Oeffnet den System-Installer fuer die geladene APK. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Ab Android 8 muss die App einmalig als Installationsquelle erlaubt werden. */
    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
