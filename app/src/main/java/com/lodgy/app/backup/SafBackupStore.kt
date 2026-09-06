package com.lodgy.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_PREFIX = "lodgy-backup-"
private const val BACKUP_SUFFIX = ".zip"
private const val ZIP_MIME = "application/zip"

/**
 * The thin Storage Access Framework layer for the automatic backup: everything that has to touch a
 * persisted tree Uri and DocumentFile lives here and nowhere else, so the orchestration in
 * [AutoBackup] stays plain Kotlin that can be unit-tested. No branching of its own worth covering -
 * the same reasoning that keeps PhotoStorage and LodgyNotifications out of the JVM coverage set.
 */
@Singleton
class SafBackupStore @Inject constructor(@ApplicationContext private val context: Context) {

    /** Take the persistable read/write grant so the daily job can write here unattended, across
     *  reboots and app updates, long after the picker Activity is gone (AC1). */
    fun persistPermission(treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    /** True when the folder still exists and we can write to it - false once the card is pulled,
     *  the folder deleted, or the permission revoked, which is what the dashboard must surface. */
    fun isWritable(treeUri: Uri): Boolean {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        return tree.exists() && tree.canWrite()
    }

    /** Creates an empty timestamped backup document in the folder, returning its Uri to write into,
     *  or null if the folder is gone or the create failed. */
    fun createBackupFile(treeUri: Uri, name: String): Uri? {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        return tree.createFile(ZIP_MIME, name)?.uri
    }

    /** The names of the backup zips already in the folder, so old ones can be pruned. */
    fun listBackupNames(treeUri: Uri): List<String> = backupFiles(treeUri).mapNotNull { it.name }

    /** Deletes the named backup zips from the folder. */
    fun deleteBackups(treeUri: Uri, names: Set<String>) {
        if (names.isEmpty()) return
        backupFiles(treeUri).filter { it.name in names }.forEach { it.delete() }
    }

    private fun backupFiles(treeUri: Uri): List<DocumentFile> {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return tree.listFiles().filter { file ->
            file.isFile && file.name?.let { it.startsWith(BACKUP_PREFIX) && it.endsWith(BACKUP_SUFFIX) } == true
        }
    }
}
