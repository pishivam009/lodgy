package com.lodgy.app.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class WorkSchedulerTest {

    @Test
    fun `schedules a unique daily periodic invoice-generation job that keeps any existing one`() {
        val workManager: WorkManager = mockk()
        val requestSlot = slot<PeriodicWorkRequest>()
        every {
            workManager.enqueueUniquePeriodicWork("invoice-generation", ExistingPeriodicWorkPolicy.KEEP, capture(requestSlot))
        } returns mockk(relaxed = true)

        workManager.scheduleInvoiceGeneration()

        verify {
            workManager.enqueueUniquePeriodicWork("invoice-generation", ExistingPeriodicWorkPolicy.KEEP, any<PeriodicWorkRequest>())
        }
        val intervalMillis = requestSlot.captured.workSpec.intervalDuration
        assertEquals(TimeUnit.DAYS.toMillis(1), intervalMillis)
    }
}
