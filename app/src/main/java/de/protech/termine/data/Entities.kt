package de.protech.termine.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Junction
import androidx.room.Relation
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("startMillis")]
)
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val categoryId: Long? = null,
    val startMillis: Long,
    val location: String = "",
    val notes: String = ""
) {
    val startDateTime: LocalDateTime
        get() = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
}

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Appointment::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("appointmentId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appointmentId: Long,
    val minutesBefore: Int
)

/**
 * Zusaetzliches Kennzeichen neben der Kategorie, zum Beispiel "Labor" oder
 * "Blutprobe". Ein Termin kann mehrere davon tragen; angezeigt werden sie
 * alphabetisch nach Name.
 */
@Entity(tableName = "markers")
data class Marker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int
)

/** Verbindet Termine und Kennzeichen; ein Termin kann mehrere tragen. */
@Entity(
    tableName = "appointment_markers",
    primaryKeys = ["appointmentId", "markerId"],
    foreignKeys = [
        ForeignKey(
            entity = Appointment::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Marker::class,
            parentColumns = ["id"],
            childColumns = ["markerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("appointmentId"), Index("markerId")]
)
data class AppointmentMarker(
    val appointmentId: Long,
    val markerId: Long
)

/**
 * Ein Arzt oder eine Praxis als Schnellauswahl beim Eintragen. Kategorie
 * und Adresse sind Vorschlaege: beim Antippen werden sie uebernommen,
 * lassen sich danach aber frei aendern.
 */
@Entity(
    tableName = "doctors",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class Doctor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val location: String = ""
)

data class AppointmentFull(
    @Embedded val appointment: Appointment,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category?,
    @Relation(parentColumn = "id", entityColumn = "appointmentId")
    val reminders: List<Reminder>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AppointmentMarker::class,
            parentColumn = "appointmentId",
            entityColumn = "markerId"
        )
    )
    val markers: List<Marker> = emptyList()
) {
    /** Immer alphabetisch - so steht "Arzttermin" vor "Labor". */
    val sortedMarkers: List<Marker>
        get() = markers.sortedBy { it.name.lowercase() }
}
