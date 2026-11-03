package de.protech.termine.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.protech.termine.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val apkUrl: String,
    val pageUrl: String,
    val sizeBytes: Long,
    val notes: String
)

sealed interface UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult
    data object UpToDate : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

/**
 * Sucht auf GitHub nach einer neueren Version.
 *
 * Bewusst OHNE eigenen Download und ohne eigene Installation: die App fragt
 * nur nach, ob es etwas Neues gibt, und uebergibt die Adresse an den Browser.
 * Dadurch braucht sie weder die Berechtigung REQUEST_INSTALL_PACKAGES noch
 * einen FileProvider – und Android bzw. Play Protect stuft sie nicht als App
 * ein, die fremde Pakete installiert. Heruntergeladen und installiert wird
 * ueber den Browser, dem das System ohnehin vertraut.
 */
object Updater {

    val repo: String get() = BuildConfig.UPDATE_REPO
    private val apiUrl: String get() = "https://api.github.com/repos/$repo/releases/latest"
    val releasesPage: String get() = "https://github.com/$repo/releases/latest"

    private const val TIMEOUT_MS = 15_000

    suspend fun check(currentVersion: String = BuildConfig.VERSION_NAME): UpdateResult =
        withContext(Dispatchers.IO) {
            try {
                val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "MeineTermine-App")
                }

                val body = try {
                    when (connection.responseCode) {
                        in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
                        403 -> return@withContext UpdateResult.Failed(
                            "GitHub bremst die Abfrage gerade (Limit). Später noch einmal versuchen."
                        )
                        404 -> return@withContext UpdateResult.Failed(
                            "Es gibt noch kein Release zum Vergleichen."
                        )
                        else -> return@withContext UpdateResult.Failed(
                            "GitHub antwortete mit ${connection.responseCode}"
                        )
                    }
                } finally {
                    connection.disconnect()
                }

                val release = JSONObject(body)
                val tag = release.optString("tag_name")
                if (tag.isBlank()) {
                    return@withContext UpdateResult.Failed("Release ohne Versionsangabe")
                }
                val remoteVersion = tag.removePrefix("v").removePrefix("V")

                if (!isNewer(remoteVersion, currentVersion)) {
                    return@withContext UpdateResult.UpToDate
                }

                var apkUrl = releasesPage
                var size = 0L
                release.optJSONArray("assets")?.let { assets ->
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            size = asset.optLong("size")
                            break
                        }
                    }
                }

                UpdateResult.Available(
                    UpdateInfo(
                        versionName = remoteVersion,
                        apkUrl = apkUrl,
                        pageUrl = release.optString("html_url").ifBlank { releasesPage },
                        sizeBytes = size,
                        notes = release.optString("body").trim()
                    )
                )
            } catch (e: Exception) {
                UpdateResult.Failed(e.message ?: "Keine Verbindung zu GitHub")
            }
        }

    /** Uebergibt die Adresse an den Browser – mehr macht die App nicht. */
    fun openInBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Vergleicht Versionen wie "1.0.14" stellenweise und numerisch. */
    fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split('.', '-').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        val c = current.split('.', '-').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
