package de.protech.termine.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.protech.termine.data.TermineDatabase
import de.protech.termine.data.TermineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Setzt alle Alarme nach Neustart, App-Update oder Zeitumstellung neu –
 * ohne das gehen geplante Alarme beim Reboot verloren.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Notifications.ensureChannel(appContext)
                val dao = TermineDatabase.get(appContext).dao()
                TermineRepository(appContext, dao).rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
