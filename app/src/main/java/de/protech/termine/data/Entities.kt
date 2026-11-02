package de.protech.termine.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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

data class AppointmentFull(
    @Embedded val appointment: Appointment,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category?,
    @Relation(parentColumn = "id", entityColumn = "appointmentId")
    val reminders: List<Reminder>
)
