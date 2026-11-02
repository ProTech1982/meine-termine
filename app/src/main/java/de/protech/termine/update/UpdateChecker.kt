package de.protech.termine.update

import de.protech.termine.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val notes: String
)

sealed interface UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult
    data object UpToDate : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

/**
 * Fragt das neueste GitHub-Release ab und vergleicht es mit der installierten Version.
 * Bewusst ohne zusaetzliche Bibliothek – HttpURLConnection und org.json reichen.
 */
object UpdateChecker {

    private const val TIMEOUT_MS = 15_000

    suspend fun check(currentVersion: String = BuildConfig.VERSION_NAME): UpdateResult =
        withContext(Dispatchers.IO) {
            try {
                val json = fetch("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
                    ?: return@withContext UpdateResult.Failed("Keine Antwort von GitHub")

                val release = JSONObject(json)
                val tag = release.optString("tag_name").ifBlank {
                    return@withContext UpdateResult.Failed("Release ohne Versionsangabe")
                }
                val remoteVersion = tag.removePrefix("v").removePrefix("V")

                val assets = release.optJSONArray("assets")
                var apkUrl = ""
                var size = 0L
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            size = asset.optLong("size")
                            break
                        }
                    }
                }

                if (apkUrl.isBlank()) {
                    return@withContext UpdateResult.Failed("Im Release liegt keine APK")
                }

                if (isNewer(remoteVersion, currentVersion)) {
                    UpdateResult.Available(
                        UpdateInfo(
                            versionName = remoteVersion,
                            apkUrl = apkUrl,
                            sizeBytes = size,
                            notes = release.optString("body").trim()
                        )
                    )
                } else {
                    UpdateResult.UpToDate
                }
            } catch (e: Exception) {
                UpdateResult.Failed(e.message ?: "Netzwerkfehler")
            }
        }

    private fun fetch(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MeineTermine-Updater")
        }
        return try {
            if (connection.responseCode !in 200..299) null
            else connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Vergleicht Versionen wie "1.0.14" stellenweise und numerisch. */
    fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split('.', '-').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        val c = current.split('.', '-').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        val length = maxOf(r.size, c.size)
        for (i in 0 until length) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
