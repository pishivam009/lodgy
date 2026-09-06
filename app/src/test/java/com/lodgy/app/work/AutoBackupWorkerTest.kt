package com.lodgy.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.lodgy.app.backup.AutoBackup
import com.lodgy.app.backup.AutoBackupOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoBackupWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val autoBackup: AutoBackup = mockk()

    private fun worker() = AutoBackupWorker(context, params, autoBackup)

    @Test
    fun `runs the backup and always reports success`() = runTest {
        coEvery { autoBackup.run() } returns AutoBackupOutcome.SUCCESS

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        coVerify(exactly = 1) { autoBackup.run() }
    }

    @Test
    fun `a failed backup does not fail the worker - the dashboard surfaces it instead`() = runTest {
        // Returning failure here would make WorkManager retry quietly; the ticket wants the failure
        // shown on the dashboard, not hidden behind a backoff (AC7).
        coEvery { autoBackup.run() } returns AutoBackupOutcome.FAILED

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
    }
}
