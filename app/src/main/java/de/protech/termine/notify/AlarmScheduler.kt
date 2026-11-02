package de.protech.termine.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import de.protech.termine.MainActivity
import de.protech.termine.data.AppointmentFull
import de.protech.termine.data.Reminder

/**
 * Setzt exakte Alarme fuer Termin-Erinnerungen.
 *
 * Es wird bewusst setAlarmClock() verwendet: diese Alarme sind von Doze und
 * Akku-Optimierung ausgenommen und feuern daher auch dann puenktlich, wenn das
 * Geraet laenger im Standby war. Genau daran scheitern die meisten Kalender-Apps.
 */
object AlarmScheduler {

    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_CATEGORY = "category"
    const val EXTRA_LOCATION = "location"
    const val EXTRA_START_MILLIS = "start_millis"
    const val EXTRA_MINUTES_BEFORE = "minutes_before"

    fun scheduleFor(context: Context, full: AppointmentFull) {
        val manager = context.getSystemService<AlarmManager>() ?: return
        val now = System.currentTimeMillis()

        full.reminders.forEach { reminder ->
            val triggerAt = full.appointment.startMillis - reminder.minutesBefore * 60_000L
            val pending = pendingIntent(context, reminder, full, PendingIntent.FLAG_UPDATE_CURRENT)

            // Vergangene Erinnerungen nicht einplanen.
            if (triggerAt <= now) {
                manager.cancel(pending)
                return@forEach
            }

            if (canScheduleExact(manager)) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    reminder.id.toInt(),
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                manager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                    pending
                )
            } else {
                // Fallback, falls der Nutzer exakte Alarme entzogen hat.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }
    }

    fun cancelFor(context: Context, reminders: List<Reminder>) {
        val manager = context.getSystemService<AlarmManager>() ?: return
        reminders.forEach { reminder ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_FIRE
                data = android.net.Uri.parse("termine://reminder/${reminder.id}")
            }
            val pending = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pending != null) {
                manager.cancel(pending)
                pending.cancel()
            }
        }
    }

    fun canScheduleExact(manager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) manager.canScheduleExactAlarms() else true

    private fun pendingIntent(
        context: Context,
        reminder: Reminder,
        full: AppointmentFull,
        flags: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            // Eindeutige Data-URI, damit Intents sich nicht gegenseitig ueberschreiben.
            data = android.net.Uri.parse("termine://reminder/${reminder.id}")
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_TITLE, full.appointment.title)
            putExtra(EXTRA_CATEGORY, full.category?.name ?: "")
            putExtra(EXTRA_LOCATION, full.appointment.location)
            putExtra(EXTRA_START_MILLIS, full.appointment.startMillis)
            putExtra(EXTRA_MINUTES_BEFORE, reminder.minutesBefore)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    const val ACTION_FIRE = "de.protech.termine.REMINDER_FIRE"
}
