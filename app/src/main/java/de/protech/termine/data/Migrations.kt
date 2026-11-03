package de.protech.termine.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Datenbank-Aenderungen zwischen zwei Versionen.
 *
 * Wichtig: niemals destruktiv migrieren. Auf dem Geraet stehen echte Termine,
 * die beim Update erhalten bleiben muessen. Jede neue Version bekommt hier
 * eine eigene Migration, und die Versionsnummer in [TermineDatabase] wird
 * im selben Schritt hochgesetzt.
 */
object Migrations {

    /** Version 2: Kennzeichen neben der Kategorie, zum Beispiel "Labor". */
    val FROM_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS markers (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    colorArgb INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS appointment_markers (
                    appointmentId INTEGER NOT NULL,
                    markerId INTEGER NOT NULL,
                    PRIMARY KEY(appointmentId, markerId),
                    FOREIGN KEY(appointmentId) REFERENCES appointments(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(markerId) REFERENCES markers(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_appointment_markers_appointmentId " +
                    "ON appointment_markers(appointmentId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_appointment_markers_markerId " +
                    "ON appointment_markers(markerId)"
            )
        }
    }

    val ALL = arrayOf(FROM_1_TO_2)
}
