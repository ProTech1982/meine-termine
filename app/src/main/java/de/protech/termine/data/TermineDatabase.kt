package de.protech.termine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Appointment::class,
        Category::class,
        Reminder::class,
        Marker::class,
        AppointmentMarker::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TermineDatabase : RoomDatabase() {

    abstract fun dao(): TermineDao

    companion object {
        @Volatile
        private var INSTANCE: TermineDatabase? = null

        fun get(context: Context): TermineDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TermineDatabase::class.java,
                    "termine.db"
                )
                    // Bestehende Termine muessen ein Update ueberleben,
                    // deshalb echte Migrationen statt Neuanlegen.
                    .addMigrations(*Migrations.ALL)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
