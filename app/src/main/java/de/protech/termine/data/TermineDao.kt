package de.protech.termine.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TermineDao {

    @Transaction
    @Query("SELECT * FROM appointments ORDER BY startMillis ASC")
    fun observeAll(): Flow<List<AppointmentFull>>

    @Transaction
    @Query("SELECT * FROM appointments WHERE id = :id")
    fun observeOne(id: Long): Flow<AppointmentFull?>

    @Transaction
    @Query("SELECT * FROM appointments ORDER BY startMillis ASC")
    suspend fun getAll(): List<AppointmentFull>

    @Transaction
    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getOne(id: Long): AppointmentFull?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Long)

    @Query("DELETE FROM reminders WHERE appointmentId = :appointmentId")
    suspend fun deleteRemindersFor(appointmentId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<Reminder>): List<Long>

    @Query("SELECT * FROM reminders WHERE appointmentId = :appointmentId")
    suspend fun remindersFor(appointmentId: Long): List<Reminder>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    /* --- Kennzeichen --- */

    @Query("SELECT * FROM markers ORDER BY name COLLATE NOCASE ASC")
    fun observeMarkers(): Flow<List<Marker>>

    @Query("SELECT COUNT(*) FROM markers")
    suspend fun markerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: Marker): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarkers(markers: List<Marker>)

    @Update
    suspend fun updateMarker(marker: Marker)

    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun deleteMarkerById(id: Long)

    @Query("DELETE FROM appointment_markers WHERE appointmentId = :appointmentId")
    suspend fun clearMarkersFor(appointmentId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun linkMarkers(links: List<AppointmentMarker>)

    /* --- Aerzte --- */

    @Query("SELECT * FROM doctors ORDER BY name COLLATE NOCASE ASC")
    fun observeDoctors(): Flow<List<Doctor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: Doctor): Long

    @Update
    suspend fun updateDoctor(doctor: Doctor)

    @Query("DELETE FROM doctors WHERE id = :id")
    suspend fun deleteDoctorById(id: Long)
}
