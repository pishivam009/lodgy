package com.lodgy.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.lodgy.app.media.OrphanPhotoCleaner
import com.lodgy.app.notify.LodgyNotifications
import com.lodgy.app.work.scheduleDuesReminder
import com.lodgy.app.work.scheduleInvoiceGeneration
import com.lodgy.app.work.scheduleVacancyCheck
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LodgyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var orphanPhotoCleaner: OrphanPhotoCleaner

    @Inject
    lateinit var notifications: LodgyNotifications

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(hiltWorkerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // AppLocale.applyDefaultIfUnset() deliberately does NOT run here - see
        // MainActivity.onCreate() for why (needs a registered AppCompatDelegate first).
        notifications.ensureChannels()
        with(WorkManager.getInstance(this)) {
            scheduleInvoiceGeneration()
            scheduleVacancyCheck()
            scheduleDuesReminder()
        }
        // Fire-and-forget on IO: startup must not wait on a directory listing, and a sweep that
        // loses a race with a fresh pick simply finds the file referenced on the next launch.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { orphanPhotoCleaner.clean() }
    }
}
