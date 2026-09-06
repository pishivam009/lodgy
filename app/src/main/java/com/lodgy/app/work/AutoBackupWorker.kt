package com.lodgy.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lodgy.app.backup.AutoBackup
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The daily automatic backup (LODGY-68), same periodic-worker pattern as invoice generation and the
 * notification checks. It always reports success: a folder that has gone unwritable is surfaced on
 * the dashboard through [AutoBackup]'s recorded failure, not by having WorkManager retry quietly -
 * a warden who believes they are covered and is not must be told, not left to a silent backoff
 * (AC7). An install with no folder chosen is a no-op (AC8).
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val autoBackup: AutoBackup,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        autoBackup.run()
        return Result.success()
    }
}
