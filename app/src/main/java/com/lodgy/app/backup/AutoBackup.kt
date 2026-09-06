package com.lodgy.app.backup

import android.net.Uri
import com.lodgy.app.data.prefs.BackupPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** How many daily zips to keep before pruning the oldest, so the folder does not grow without
 *  bound (LODGY-68, AC6). A week of recovery points is plenty for a phone-loss or fat-finger. */
const val KEEP_BACKUPS = 7

enum class AutoBackupOutcome {
    /** No folder chosen - the install keeps working, just never backs up automatically (AC8). */
    NOT_CONFIGURED,

    /** Nothing changed since the last successful backup, so no zip was written (AC5). */
    SKIPPED_UNCHANGED,

    /** A fresh backup was written and old ones pruned. */
    SUCCESS,

    /** The folder is gone or unwritable, or the write failed - recorded so the dashboard shows it
     *  rather than the app retrying quietly (AC7). */
    FAILED,
}

/**
 * Orchestrates one automatic-backup run (LODGY-68). Deliberately plain Kotlin over injected
 * collaborators so every branch here is unit-tested; the Android SAF calls it makes all live behind
 * [SafBackupStore], and the zip writing reuses the existing, tested [BackupManager.export].
 *
 * Called both by the daily [com.lodgy.app.work.AutoBackupWorker] and by the dashboard's one-tap
 * backup, which passes [force] so a warden asking for a backup now always gets one even if nothing
 * changed.
 */
@Singleton
class AutoBackup @Inject constructor(
    private val backupManager: BackupManager,
    private val backupPreferences: BackupPreferences,
    private val safBackupStore: SafBackupStore,
) {

    suspend fun run(force: Boolean = false): AutoBackupOutcome {
        val folder = backupPreferences.folderUri.first()?.let(Uri::parse)
            ?: return AutoBackupOutcome.NOT_CONFIGURED

        if (!safBackupStore.isWritable(folder)) {
            backupPreferences.recordFailure()
            return AutoBackupOutcome.FAILED
        }

        val fingerprint = backupManager.currentFingerprint()
        if (!force && shouldSkipBackup(fingerprint, backupPreferences.lastFingerprint.first())) {
            return AutoBackupOutcome.SKIPPED_UNCHANGED
        }

        val target = safBackupStore.createBackupFile(folder, backupFileName())
            ?: run {
                backupPreferences.recordFailure()
                return AutoBackupOutcome.FAILED
            }

        if (!backupManager.export(target)) {
            backupPreferences.recordFailure()
            return AutoBackupOutcome.FAILED
        }

        val stale = backupsToDelete(safBackupStore.listBackupNames(folder), KEEP_BACKUPS)
        safBackupStore.deleteBackups(folder, stale.toSet())

        backupPreferences.recordSuccess(System.currentTimeMillis(), fingerprint)
        return AutoBackupOutcome.SUCCESS
    }

    /** Timestamped to the second and zero-padded so the names sort in time order, which is what lets
     *  [backupsToDelete] pick the oldest by a plain lexicographic sort. */
    private fun backupFileName(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
        return "lodgy-backup-$stamp.zip"
    }
}
