package de.protech.termine.data

import android.content.Context
import de.protech.termine.notify.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class TermineRepository(
    private val context: Context,
    private val dao: TermineDao
) {

    val appointments: Flow<List<AppointmentFull>> = dao.observeAll()
    val categories: Flow<List<Category>> = dao.observeCategories()

    fun appointment(id: Long): Flow<AppointmentFull?> = dao.observeOne(id)

    suspend fun seedCategoriesIfEmpty() {
        if (dao.categoryCount() > 0) return
        dao.insertCategories(DEFAULT_CATEGORIES)
    }

    /**
     * Speichert einen Termin samt Erinnerungen und setzt die Alarme neu.
     * Gibt die ID des Termins zurueck.
     */
    suspend fun save(appointment: Appointment, minutesBefore: List<Int>): Long {
        val id: Long
        if (appointment.id == 0L) {
            id = dao.insertAppointment(appointment)
        } else {
            id = appointment.id
            dao.updateAppointment(appointment)
            AlarmScheduler.cancelFor(context, dao.remindersFor(id))
            dao.deleteRemindersFor(id)
        }

        val distinct = minutesBefore.distinct().sorted()
        if (distinct.isNotEmpty()) {
            dao.insertReminders(distinct.map { Reminder(appointmentId = id, minutesBefore = it) })
        }

        val saved = dao.getOne(id)
        if (saved != null) AlarmScheduler.scheduleFor(context, saved)
        return id
    }

    suspend fun delete(id: Long) {
        AlarmScheduler.cancelFor(context, dao.remindersFor(id))
        dao.deleteAppointmentById(id)
    }

    /** Nach Neustart, Zeitumstellung oder App-Update alle Alarme neu setzen. */
    suspend fun rescheduleAll() {
        dao.getAll().forEach { AlarmScheduler.scheduleFor(context, it) }
    }

    suspend fun addCategory(name: String, colorArgb: Int) {
        dao.insertCategory(Category(name = name, colorArgb = colorArgb, sortOrder = 100))
    }

    suspend fun updateCategory(category: Category) = dao.updateCategory(category)

    suspend fun deleteCategory(id: Long) = dao.deleteCategoryById(id)

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category(name = "Hausarzt", colorArgb = 0xFF2E7D32.toInt(), sortOrder = 1),
            Category(name = "Zahnarzt", colorArgb = 0xFF0277BD.toInt(), sortOrder = 2),
            Category(name = "Diabetologe", colorArgb = 0xFFC62828.toInt(), sortOrder = 3),
            Category(name = "Augenarzt", colorArgb = 0xFF6A1B9A.toInt(), sortOrder = 4),
            Category(name = "Orthopäde", colorArgb = 0xFFEF6C00.toInt(), sortOrder = 5),
            Category(name = "Physiotherapie", colorArgb = 0xFF00838F.toInt(), sortOrder = 6),
            Category(name = "Labor / Blutabnahme", colorArgb = 0xFFAD1457.toInt(), sortOrder = 7),
            Category(name = "Sonstiges", colorArgb = 0xFF455A64.toInt(), sortOrder = 8)
        )
    }
}
