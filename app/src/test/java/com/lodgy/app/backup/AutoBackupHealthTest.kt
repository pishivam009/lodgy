package com.lodgy.app.backup

import com.lodgy.app.ui.theme.StatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupHealthTest {

    private val now = 1_000_000_000_000L

    @Test
    fun `no folder chosen reads as not configured`() {
        assertEquals(
            BackupHealth.NOT_CONFIGURED,
            backupHealth(folderConfigured = false, lastSuccessTime = now, lastAttemptFailed = false, now = now),
        )
    }

    @Test
    fun `a failed attempt outranks a recent success`() {
        // The folder just went unwritable; last week's good backup must not make it look fine.
        val health = backupHealth(
            folderConfigured = true,
            lastSuccessTime = now - 1000,
            lastAttemptFailed = true,
            now = now,
        )
        assertEquals(BackupHealth.FAILED, health)
    }

    @Test
    fun `configured but never backed up reads as never`() {
        assertEquals(
            BackupHealth.NEVER,
            backupHealth(folderConfigured = true, lastSuccessTime = null, lastAttemptFailed = false, now = now),
        )
    }

    @Test
    fun `a fresh backup is recent and a day-old one still recent`() {
        assertEquals(
            BackupHealth.RECENT,
            backupHealth(folderConfigured = true, lastSuccessTime = now, lastAttemptFailed = false, now = now),
        )
        assertEquals(
            BackupHealth.RECENT,
            backupHealth(
                folderConfigured = true,
                lastSuccessTime = now - STALE_AFTER_MILLIS,
                lastAttemptFailed = false,
                now = now,
            ),
        )
    }

    @Test
    fun `past the stale window it reads as stale`() {
        assertEquals(
            BackupHealth.STALE,
            backupHealth(
                folderConfigured = true,
                lastSuccessTime = now - STALE_AFTER_MILLIS - 1,
                lastAttemptFailed = false,
                now = now,
            ),
        )
    }

    @Test
    fun `only a recent backup is green`() {
        assertEquals(StatusLevel.GOOD, BackupHealth.RECENT.level)
        assertEquals(StatusLevel.WARN, BackupHealth.STALE.level)
        assertEquals(StatusLevel.WARN, BackupHealth.NOT_CONFIGURED.level)
        assertEquals(StatusLevel.BAD, BackupHealth.NEVER.level)
        assertEquals(StatusLevel.BAD, BackupHealth.FAILED.level)
    }

    @Test
    fun `skip only when the fingerprint matches a real previous backup`() {
        assertTrue(shouldSkipBackup("abc", "abc"))
        assertFalse(shouldSkipBackup("abc", "def"))
        // A never-backed-up install has no last fingerprint and must never skip its first backup.
        assertFalse(shouldSkipBackup("abc", null))
    }

    @Test
    fun `prune keeps the newest N and returns the oldest to delete`() {
        val names = listOf(
            "lodgy-backup-2026-09-01-100000.zip",
            "lodgy-backup-2026-09-03-100000.zip",
            "lodgy-backup-2026-09-02-100000.zip",
            "lodgy-backup-2026-09-05-100000.zip",
            "lodgy-backup-2026-09-04-100000.zip",
        )
        val toDelete = backupsToDelete(names, keep = 3)
        assertEquals(
            listOf(
                "lodgy-backup-2026-09-01-100000.zip",
                "lodgy-backup-2026-09-02-100000.zip",
            ),
            toDelete,
        )
    }

    @Test
    fun `prune deletes nothing when at or under the limit`() {
        val names = listOf("lodgy-backup-2026-09-01-100000.zip", "lodgy-backup-2026-09-02-100000.zip")
        assertTrue(backupsToDelete(names, keep = 3).isEmpty())
        assertTrue(backupsToDelete(names, keep = 2).isEmpty())
    }

    @Test
    fun `a non-positive keep deletes everything`() {
        val names = listOf("lodgy-backup-2026-09-01-100000.zip")
        assertEquals(names, backupsToDelete(names, keep = 0))
    }
}
