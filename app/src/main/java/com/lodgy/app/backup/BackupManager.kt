package com.lodgy.app.backup

import android.content.Context
import android.net.Uri
import com.lodgy.app.data.LodgyDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DB_ENTRY_NAME = "lodgy.db"
private const val PHOTOS_ENTRY_PREFIX = "photos/"

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

    suspend fun export(destination: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            // Flush the WAL into the main db file so the copy below is complete and self-contained.
            database.query("PRAGMA wal_checkpoint(FULL)", null).close()

            context.contentResolver.openOutputStream(destination)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.putNextEntry(ZipEntry(DB_ENTRY_NAME))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()

                    photosDir.listFiles()?.forEach { photo ->
                        zip.putNextEntry(ZipEntry(PHOTOS_ENTRY_PREFIX + photo.name))
                        photo.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
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

        stagingDir.deleteRecursively()
    }
}
