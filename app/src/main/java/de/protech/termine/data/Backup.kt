package de.protech.termine.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import de.protech.termine.TermineApp
import de.protech.termine.notify.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sicherung und Wiederherstellung aller Daten als JSON-Datei: Termine,
 * Kategorien, Kennzeichen, Erinnerungen, Aerzte und die Darstellung.
 *
 * Die Datei liegt dort, wo du sie im Android-Dateidialog hinlegst, und
 * ueberlebt deshalb eine Neuinstallation. Die Tabellen werden spaltenweise
 * gelesen und geschrieben; Ids bleiben erhalten, damit alle Verknuepfungen
 * (Kategorie, Kennzeichen, Erinnerungen) wieder stimmen.
 */
object Backup {

    private const val FORMAT = "meine-termine-backup"
    private const val VERSION = 1
    private const val PREFS_NAME = "darstellung"

    /** Eltern zuerst - beim Loeschen wird die Reihenfolge umgedreht. */
    private val TABLES = listOf(
        "categories",
        "markers",
        "appointments",
        "doctors",
        "reminders",
        "appointment_markers"
    )

    fun suggestedName(): String =
        "meine-termine-sicherung-${java.time.LocalDate.now()}.json"

    suspend fun write(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val app = context.applicationContext
            val db = TermineDatabase.get(app)
            val sql = db.openHelper.readableDatabase

            val tables = JSONObject()
            var rows = 0
            for (table in TABLES) {
                val arr = JSONArray()
                sql.query("SELECT * FROM $table").use { c ->
                    while (c.moveToNext()) {
                        arr.put(rowToJson(c))
                        rows++
                    }
                }
                tables.put(table, arr)
            }

            val root = JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("dbVersion", sql.version)
                .put("erstellt", java.time.LocalDateTime.now().toString())
                .put("tabellen", tables)
                .put("darstellung", prefsToJson(app))

            app.contentResolver.openOutputStream(uri, "wt").use { out ->
                requireNotNull(out) { "Datei konnte nicht geschrieben werden" }
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }
            rows
        }
    }

    /** Ersetzt alle vorhandenen Daten durch die der Sicherung. Gibt die Anzahl der Zeilen zurueck. */
    suspend fun restore(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val app = context.applicationContext
            val text = app.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Datei konnte nicht gelesen werden" }
                input.readBytes().toString(Charsets.UTF_8)
            }
            val root = JSONObject(text)
            require(root.optString("format") == FORMAT) { "Das ist keine Sicherung von Meine Termine." }
            require(root.optInt("version", 0) <= VERSION) { "Die Sicherung stammt von einer neueren App-Version." }

            val db = TermineDatabase.get(app)
            val sql = db.openHelper.writableDatabase
            require(root.optInt("dbVersion", -1) == sql.version) {
                "Die Sicherung hat ein anderes Datenbankformat (${root.optInt("dbVersion", -1)} statt ${sql.version})."
            }
            val tables = root.getJSONObject("tabellen")

            // Alte Alarme merken, damit keine verwaisten Erinnerungen uebrig bleiben.
            val alteErinnerungen = db.dao().getAll().flatMap { it.reminders }

            var rows = 0
            // Alles in einer Transaktion: bei einem Fehler bleibt der alte Stand erhalten.
            db.runInTransaction(Runnable {
                for (table in TABLES.reversed()) sql.execSQL("DELETE FROM $table")
                for (table in TABLES) {
                    val arr = tables.optJSONArray(table) ?: continue
                    val spalten = columnsOf(sql, table)
                    for (i in 0 until arr.length()) {
                        val values = jsonToValues(arr.getJSONObject(i), spalten)
                        val id = sql.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT, values)
                        check(id != -1L) { "Zeile in '$table' konnte nicht geschrieben werden" }
                        rows++
                    }
                }
            })

            // Darstellung zuruecksetzen und neu laden.
            jsonToPrefs(app, root.optJSONArray("darstellung"))
            (app as? TermineApp)?.appearance?.reload()

            // Alarme: alte weg, neue passend zu den wiederhergestellten Terminen.
            AlarmScheduler.cancelFor(app, alteErinnerungen)
            (app as? TermineApp)?.repository?.rescheduleAll()
            rows
        }
    }

    // ---------- Datenbank ----------

    private fun rowToJson(c: Cursor): JSONObject {
        val o = JSONObject()
        for (i in 0 until c.columnCount) {
            val name = c.getColumnName(i)
            when (c.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> o.put(name, JSONObject.NULL)
                Cursor.FIELD_TYPE_INTEGER -> o.put(name, c.getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> o.put(name, c.getDouble(i))
                Cursor.FIELD_TYPE_STRING -> o.put(name, c.getString(i))
                else -> Unit // BLOBs kommen in dieser App nicht vor
            }
        }
        return o
    }

    private fun columnsOf(sql: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Set<String> {
        val result = mutableSetOf<String>()
        sql.query("PRAGMA table_info($table)").use { c ->
            val nameIdx = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) result += c.getString(nameIdx)
        }
        return result
    }

    private fun jsonToValues(o: JSONObject, spalten: Set<String>): ContentValues {
        val v = ContentValues()
        for (key in o.keys()) {
            if (key !in spalten) continue
            when (val x = o.get(key)) {
                JSONObject.NULL -> v.putNull(key)
                is Int -> v.put(key, x.toLong())
                is Long -> v.put(key, x)
                is Double -> v.put(key, x)
                is Boolean -> v.put(key, if (x) 1L else 0L)
                else -> v.put(key, x.toString())
            }
        }
        return v
    }

    // ---------- Darstellung (SharedPreferences) ----------

    private fun prefsToJson(context: Context): JSONArray {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val items = JSONArray()
        for ((key, value) in sp.all) {
            val (type, json) = when (value) {
                is Boolean -> "boolean" to value
                is Int -> "int" to value
                is Long -> "long" to value
                is Float -> "float" to value.toDouble()
                is String -> "string" to value
                else -> continue
            }
            items.put(JSONObject().put("k", key).put("t", type).put("v", json))
        }
        return items
    }

    private fun jsonToPrefs(context: Context, items: JSONArray?) {
        if (items == null) return
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.clear()
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val key = o.getString("k")
            when (o.getString("t")) {
                "boolean" -> editor.putBoolean(key, o.getBoolean("v"))
                "int" -> editor.putInt(key, o.getInt("v"))
                "long" -> editor.putLong(key, o.getLong("v"))
                "float" -> editor.putFloat(key, o.getDouble("v").toFloat())
                "string" -> editor.putString(key, o.getString("v"))
            }
        }
        editor.commit()
    }
}
