package de.protech.termine.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.content.getSystemService

object Notifications {

    const val CHANNEL_ID = "termin_erinnerungen"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Termin-Erinnerungen",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Erinnerungen an anstehende Termine"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 250, 400)
            enableLights(true)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
            )
        }
        manager.createNotificationChannel(channel)
    }
}
