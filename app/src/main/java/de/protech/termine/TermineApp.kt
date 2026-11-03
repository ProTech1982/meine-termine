package de.protech.termine

import android.app.Application
import de.protech.termine.data.TermineDatabase
import de.protech.termine.look.AppearanceStore
import de.protech.termine.data.TermineRepository
import de.protech.termine.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TermineApp : Application() {

    lateinit var repository: TermineRepository
        private set

    lateinit var appearance: AppearanceStore
        private set

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        appearance = AppearanceStore(this)
        repository = TermineRepository(this, TermineDatabase.get(this).dao())

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.seedCategoriesIfEmpty()
            repository.seedMarkersIfEmpty()
            repository.rescheduleAll()
        }
    }
}
