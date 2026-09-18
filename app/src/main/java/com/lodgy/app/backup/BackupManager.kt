package com.lodgy.app.backup

import android.content.Context
import android.net.Uri
import com.lodgy.app.data.LodgyDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DB_ENTRY_NAME = "lodgy.db"
private const val PHOTOS_ENTRY_PREFIX = "photos/"
private const val PREFS_ENTRY_PREFIX = "prefs/"

/**
 * DataStore files deliberately left out of a backup, named rather than filtered by an allowlist
 * so a preference file added later is included by default - an allowlist here would silently
 * repeat the exact gap this ticket was filed over (LODGY-103).
 *
 * - `auth_prefs`: the PIN hash and biometric opt-in. A new phone getting a fresh PIN setup is the
 *   safer default (LODGY-76 already treats "PIN gone, data intact" as ordinary recovery, not a
 *   failure state) - carrying a PIN hash across silently would be the stranger choice.
 * - `backup_prefs`: the auto-backup folder's SAF URI and last-run state are tied to a permission
 *   grant on THIS phone. Restoring them onto a new phone wouldn't resolve to anything real, or
 *   worse would resolve to an unrelated folder that happens to share a URI - actively wrong to
 *   carry over, not merely omittable.
 */
private val PREFS_EXCLUDED_FROM_BACKUP = setOf("auth_prefs.preferences_pb", "backup_prefs.preferences_pb")

sealed interface ImportResult {
    data object Success : ImportResult
    data object NotALodgyBackup : ImportResult
    data class Failed(val message: String) : ImportResult
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LodgyDatabase,
) {
    private val photosDir: File get() = File(context.filesDir, "photos")
    private val dbFile: File get() = context.getDatabasePath(LodgyDatabase.DATABASE_NAME)
    private val datastoreDir: File get() = File(context.filesDir, "datastore")

    suspend fun export(destination: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            // Flush the WAL into the main db file so the copy below is complete and self-contained.
            database.query("PRAGMA wal_checkpoint(FULL)", null).close()

            // Held in a val and null-checked on its own. Folding this into an elvis after `use`
            // also catches the block's own last expression being null - which it is whenever
            // filesDir/photos does not exist yet - and reports a good backup as a failure.
            val out = context.contentResolver.openOutputStream(destination)
                ?: return@runCatching false

            out.use {
                ZipOutputStream(it).use { zip ->
                    zip.putNextEntry(ZipEntry(DB_ENTRY_NAME))
                    dbFile.inputStream().use { db -> db.copyTo(zip) }
                    zip.closeEntry()

                    // Absent on any install that has never saved a photo, which is normal.
                    photosDir.listFiles().orEmpty().forEach { photo ->
                        zip.putNextEntry(ZipEntry(PHOTOS_ENTRY_PREFIX + photo.name))
                        photo.inputStream().use { image -> image.copyTo(zip) }
                        zip.closeEntry()
                    }

                    // hostel_prefs, notification_prefs, theme_prefs and any future addition -
                    // everything except the two named exclusions above (LODGY-103).
                    datastoreDir.listFiles().orEmpty()
                        .filter { it.name !in PREFS_EXCLUDED_FROM_BACKUP }
                        .forEach { prefs ->
                            zip.putNextEntry(ZipEntry(PREFS_ENTRY_PREFIX + prefs.name))
                            prefs.inputStream().use { data -> data.copyTo(zip) }
                            zip.closeEntry()
                        }
                }
            }
            true
        }.getOrDefault(false)
    }

    /**
     * A content fingerprint of everything a backup would capture, so the daily job can skip a day
     * on which nothing changed rather than writing an identical zip (LODGY-68, AC5). The WAL is
     * checkpointed first so pending writes are folded into the db file being hashed - otherwise a
     * day whose changes still sat in the WAL would look unchanged and be skipped. Identical data
     * yields an identical fingerprint; any write since the last backup changes it.
     */
    suspend fun currentFingerprint(): String = withContext(Dispatchers.IO) {
        database.query("PRAGMA wal_checkpoint(FULL)", null).close()
        val digest = MessageDigest.getInstance("SHA-256")
        dbFile.inputStream().use { db ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = db.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        // Photos ride along in the zip, so a photo added or removed must count as a change even when
        // the db is byte-identical. Name and size are enough; the bytes of a picked photo never
        // change in place. Sorted so ordering from the filesystem cannot perturb the hash.
        photosDir.listFiles().orEmpty().sortedBy { it.name }.forEach { photo ->
            digest.update("${photo.name}:${photo.length()}".toByteArray())
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Extracts and validates [source] without touching existing data. Returns the staging
     * directory on success so the caller can confirm before [applyStaged] commits it.
     */
    suspend fun stageImport(source: Uri): Pair<ImportResult, File?> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "restore_staging").apply {
            deleteRecursively()
            mkdirs()
        }
        runCatching {
            var sawDb = false
            context.contentResolver.openInputStream(source)?.use { input ->
                ZipInputStream(input).use { zip ->
                    generateSequence { zip.nextEntry }.forEach { entry ->
                        when {
                            entry.name == DB_ENTRY_NAME -> {
                                File(stagingDir, DB_ENTRY_NAME).outputStream().use { zip.copyTo(it) }
                                sawDb = true
                            }
                            entry.name.startsWith(PHOTOS_ENTRY_PREFIX) && !entry.isDirectory -> {
                                val name = entry.name.removePrefix(PHOTOS_ENTRY_PREFIX)
                                if (name.isNotBlank() && !name.contains("..") && !name.contains('/')) {
                                    val stagedPhotosDir = File(stagingDir, "photos").apply { mkdirs() }
                                    File(stagedPhotosDir, name).outputStream().use { zip.copyTo(it) }
                                }
                            }
                            entry.name.startsWith(PREFS_ENTRY_PREFIX) && !entry.isDirectory -> {
                                val name = entry.name.removePrefix(PREFS_ENTRY_PREFIX)
                                // Absent from a backup taken before LODGY-103, or from one where the
                                // warden had turned that category off - stageImport just won't find
                                // the entry, and applyStaged leaves the current phone's own file alone.
                                if (name.isNotBlank() && !name.contains("..") && !name.contains('/')) {
                                    val stagedPrefsDir = File(stagingDir, "prefs").apply { mkdirs() }
                                    File(stagedPrefsDir, name).outputStream().use { zip.copyTo(it) }
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: return@withContext ImportResult.Failed("Could not open the selected file") to null

            if (!sawDb) {
                stagingDir.deleteRecursively()
                return@withContext ImportResult.NotALodgyBackup to null
            }
            ImportResult.Success to stagingDir
        }.getOrElse { error ->
            stagingDir.deleteRecursively()
            ImportResult.Failed(error.message ?: "Import failed") to null
        }
    }

    /** Overwrites the live DB and photos with what [stagingDir] holds (from [stageImport]). */
    suspend fun applyStaged(stagingDir: File): Unit = withContext(Dispatchers.IO) {
        database.close()

        dbFile.parentFile?.listFiles { file -> file.name.startsWith(LodgyDatabase.DATABASE_NAME) }
            ?.forEach { it.delete() }
        File(stagingDir, DB_ENTRY_NAME).copyTo(dbFile, overwrite = true)

        photosDir.deleteRecursively()
        photosDir.mkdirs()
        File(stagingDir, "photos").takeIf { it.isDirectory }?.listFiles()?.forEach { photo ->
            photo.copyTo(File(photosDir, photo.name), overwrite = true)
        }

        // Only overwrites a prefs file the backup actually captured - auth_prefs/backup_prefs were
        // never in the zip by design, and an older backup predating LODGY-103 has no prefs/ entries
        // at all, so either way the current phone's own settings for anything not present are left
        // untouched rather than wiped.
        File(stagingDir, "prefs").takeIf { it.isDirectory }?.listFiles()?.forEach { prefs ->
            File(datastoreDir, prefs.name).also { it.parentFile?.mkdirs() }
                .let { dest -> prefs.copyTo(dest, overwrite = true) }
        }

        stagingDir.deleteRecursively()
    }
}
