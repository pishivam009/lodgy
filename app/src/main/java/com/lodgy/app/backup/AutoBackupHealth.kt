package com.lodgy.app.backup

import com.lodgy.app.ui.theme.StatusLevel
import java.util.concurrent.TimeUnit

/**
 * How the daily automatic backup is doing, as the warden needs to read it off the dashboard
 * (LODGY-68). The whole point of the tile is that a silently broken backup must look broken, so
 * these map onto the LODGY-36 RAG tokens: only a recent, successful backup is green.
 */
enum class BackupHealth {
    /** No folder chosen yet - prompted, never blocked (AC8). */
    NOT_CONFIGURED,

    /** A folder is set but no backup has ever succeeded. */
    NEVER,

    /** The last attempt failed - folder unwritable, or the write did. */
    FAILED,

    /** Succeeded, but long enough ago that the warden should know it has gone quiet. */
    STALE,

    /** Succeeded recently. */
    RECENT,
    ;

    val level: StatusLevel
        get() = when (this) {
            RECENT -> StatusLevel.GOOD
            STALE -> StatusLevel.WARN
            NOT_CONFIGURED -> StatusLevel.WARN
            NEVER, FAILED -> StatusLevel.BAD
        }
}

/** A backup older than this reads as stale rather than fresh. */
val STALE_AFTER_MILLIS: Long = TimeUnit.DAYS.toMillis(2)

/**
 * Classifies the backup state for the dashboard. A failed attempt outranks age: a warden whose
 * folder just became unwritable needs to see FAILED even if last week's backup is still recent.
 */
fun backupHealth(
    folderConfigured: Boolean,
    lastSuccessTime: Long?,
    lastAttemptFailed: Boolean,
    now: Long,
): BackupHealth = when {
    !folderConfigured -> BackupHealth.NOT_CONFIGURED
    lastAttemptFailed -> BackupHealth.FAILED
    lastSuccessTime == null -> BackupHealth.NEVER
    now - lastSuccessTime <= STALE_AFTER_MILLIS -> BackupHealth.RECENT
    else -> BackupHealth.STALE
}

/** True when the data is byte-identical to the last successful backup, so the run is skipped rather
 *  than writing another copy of the same zip (AC5). A never-backed-up install ([last] null) never
 *  skips. */
fun shouldSkipBackup(current: String, last: String?): Boolean = last != null && current == last

/**
 * Of [names] (the backup files already in the folder), the ones to delete so only the newest [keep]
 * remain (AC6). Names are the timestamped `lodgy-backup-YYYY-MM-DD-HHmmss.zip`, which sort
 * lexicographically in time order, so the oldest are simply the front of the sorted list.
 */
fun backupsToDelete(names: List<String>, keep: Int): List<String> {
    if (keep <= 0) return names
    val sorted = names.sorted()
    val excess = sorted.size - keep
    return if (excess <= 0) emptyList() else sorted.take(excess)
}
