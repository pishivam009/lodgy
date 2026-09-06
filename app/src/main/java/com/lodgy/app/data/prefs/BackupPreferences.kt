package com.lodgy.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backupDataStore by preferencesDataStore(name = "backup_prefs")

/** State for the daily automatic backup (LODGY-68). The folder is picked once and its persisted
 *  SAF permission remembered here; the rest records what the last run did, so the dashboard can
 *  show staleness and failure as plainly as success rather than letting a broken backup pass for a
 *  working one. */
class BackupPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val folderUriKey = stringPreferencesKey("folder_uri")
    private val lastSuccessTimeKey = longPreferencesKey("last_success_time")
    private val lastFingerprintKey = stringPreferencesKey("last_fingerprint")
    private val lastAttemptFailedKey = booleanPreferencesKey("last_attempt_failed")

    /** The tree the daily job writes into, or null on an install that has never chosen one - which
     *  keeps working exactly as before, just never backing up automatically (AC8). */
    val folderUri: Flow<String?> = context.backupDataStore.data.map { it[folderUriKey] }

    /** When the last backup actually succeeded, or null if one never has. */
    val lastSuccessTime: Flow<Long?> = context.backupDataStore.data.map { it[lastSuccessTimeKey] }

    /** Content fingerprint of the last successful backup, so an unchanged day is skipped rather than
     *  churning an identical zip (AC5). */
    val lastFingerprint: Flow<String?> = context.backupDataStore.data.map { it[lastFingerprintKey] }

    /** True when the most recent attempt failed - the folder went unwritable, or the write did.
     *  Surfaced on the dashboard so the failure is visible rather than assumed fine (AC7). */
    val lastAttemptFailed: Flow<Boolean> =
        context.backupDataStore.data.map { it[lastAttemptFailedKey] ?: false }

    suspend fun setFolderUri(uri: String?) {
        context.backupDataStore.edit {
            if (uri == null) it.remove(folderUriKey) else it[folderUriKey] = uri
            // Choosing a folder is a fresh start: clear any prior failure so the dashboard stops
            // showing red the moment the warden re-picks, rather than staying failed until the next
            // successful run (LODGY-68, AC7).
            it[lastAttemptFailedKey] = false
        }
    }

    suspend fun recordSuccess(time: Long, fingerprint: String) {
        context.backupDataStore.edit {
            it[lastSuccessTimeKey] = time
            it[lastFingerprintKey] = fingerprint
            it[lastAttemptFailedKey] = false
        }
    }

    /** Records that a run failed without disturbing the last-success time - the warden still needs
     *  to see how old the last good backup is, next to the fact that today's did not work. */
    suspend fun recordFailure() {
        context.backupDataStore.edit { it[lastAttemptFailedKey] = true }
    }
}
