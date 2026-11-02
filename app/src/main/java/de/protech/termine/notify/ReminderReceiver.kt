package de.protech.termine.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.protech.termine.MainActivity
import de.protech.termine.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return

        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, 0L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty()
        val category = intent.getStringExtra(AlarmScheduler.EXTRA_CATEGORY).orEmpty()
        val location = intent.getStringExtra(AlarmScheduler.EXTRA_LOCATION).orEmpty()
        val startMillis = intent.getLongExtra(AlarmScheduler.EXTRA_START_MILLIS, 0L)
        val minutesBefore = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTES_BEFORE, 0)

        Notifications.ensureChannel(context)

        val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault())
        val timeText = start.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM, HH:mm", Locale.GERMAN))

        val headline = if (category.isNotBlank()) "$category: $title" else title
        val lead = leadText(minutesBefore)

        val body = buildString {
            append(lead)
            append(" – ")
            append(timeText)
            append(" Uhr")
            if (location.isNotBlank()) {
                append("\n")
                append(location)
            }
        }

        val tapIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_termin)
            .setContentTitle(headline)
            .setContentText(body.replace("\n", " · "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setWhen(startMillis)
            .setShowWhen(false)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(reminderId.toInt(), notification)
        } catch (_: SecurityException) {
            // Benachrichtigungsrecht fehlt – nichts zu tun.
        }
    }

    private fun leadText(minutesBefore: Int): String = when {
        minutesBefore <= 0 -> "Jetzt"
        minutesBefore < 60 -> "In $minutesBefore Minuten"
        minutesBefore == 60 -> "In einer Stunde"
        minutesBefore < 1440 && minutesBefore % 60 == 0 -> "In ${minutesBefore / 60} Stunden"
        minutesBefore == 1440 -> "Morgen"
        minutesBefore % 1440 == 0 -> "In ${minutesBefore / 1440} Tagen"
        else -> "In ${minutesBefore / 60} Stunden"
    }
}
