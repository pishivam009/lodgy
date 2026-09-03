package com.lodgy.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.lodgy.app.work.scheduleInvoiceGeneration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LodgyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(hiltWorkerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // AppLocale.applyDefaultIfUnset() deliberately does NOT run here - see
        // MainActivity.onCreate() for why (needs a registered AppCompatDelegate first).
        WorkManager.getInstance(this).scheduleInvoiceGeneration()
    }
}
