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
}
