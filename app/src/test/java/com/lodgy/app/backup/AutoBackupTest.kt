package com.lodgy.app.backup

import android.net.Uri
import com.lodgy.app.data.prefs.BackupPreferences
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AutoBackupTest {

    private val backupManager: BackupManager = mockk()
    private val backupPreferences: BackupPreferences = mockk(relaxed = true)
    private val safBackupStore: SafBackupStore = mockk(relaxed = true)

    private val folderUriString = "content://tree/folder"
    private val folderUri: Uri = mockk()
    private val targetUri: Uri = mockk()

    private fun autoBackup() = AutoBackup(backupManager, backupPreferences, safBackupStore)

    @Before
    fun setUp() {
        // Uri.parse is a static Android call; stub it to hand back a mock for the stored string.
        mockkStatic(Uri::class)
        every { Uri.parse(folderUriString) } returns folderUri
        every { backupPreferences.folderUri } returns flowOf(folderUriString)
        every { backupPreferences.lastFingerprint } returns flowOf(null)
    }

    @After
    fun tearDown() {
        // Uri.parse is a process-wide static mock; leaving it stubbed corrupts every later test in
        // the JVM that touches Uri.
        unmockkStatic(Uri::class)
    }

    @Test
    fun `no folder configured is a no-op`() = runTest {
        every { backupPreferences.folderUri } returns flowOf(null)

        assertEquals(AutoBackupOutcome.NOT_CONFIGURED, autoBackup().run())

        coVerify(exactly = 0) { backupManager.export(any()) }
        coVerify(exactly = 0) { backupPreferences.recordFailure() }
    }

    @Test
    fun `an unwritable folder fails and is recorded, without touching the export`() = runTest {
        every { safBackupStore.isWritable(folderUri) } returns false

        assertEquals(AutoBackupOutcome.FAILED, autoBackup().run())

        coVerify(exactly = 1) { backupPreferences.recordFailure() }
        coVerify(exactly = 0) { backupManager.export(any()) }
    }

    @Test
    fun `an unchanged day is skipped`() = runTest {
        every { safBackupStore.isWritable(folderUri) } returns true
        coEvery { backupManager.currentFingerprint() } returns "same"
        every { backupPreferences.lastFingerprint } returns flowOf("same")

        assertEquals(AutoBackupOutcome.SKIPPED_UNCHANGED, autoBackup().run())

        coVerify(exactly = 0) { backupManager.export(any()) }
        coVerify(exactly = 0) { backupPreferences.recordSuccess(any(), any()) }
    }

    @Test
    fun `force overrides an unchanged day and still backs up`() = runTest {
        every { safBackupStore.isWritable(folderUri) } returns true
        coEvery { backupManager.currentFingerprint() } returns "same"
        every { backupPreferences.lastFingerprint } returns flowOf("same")
        every { safBackupStore.createBackupFile(folderUri, any()) } returns targetUri
        coEvery { backupManager.export(targetUri) } returns true
        every { safBackupStore.listBackupNames(folderUri) } returns emptyList()

        assertEquals(AutoBackupOutcome.SUCCESS, autoBackup().run(force = true))

        coVerify(exactly = 1) { backupManager.export(targetUri) }
        coVerify(exactly = 1) { backupPreferences.recordSuccess(any(), "same") }
    }

    @Test
    fun `a null created file is a recorded failure`() = runTest {
        every { safBackupStore.isWritable(folderUri) } returns true
        coEvery { backupManager.currentFingerprint() } returns "fp"
        every { safBackupStore.createBackupFile(folderUri, any()) } returns null

        assertEquals(AutoBackupOutcome.FAILED, autoBackup().run())

        coVerify(exactly = 1) { backupPreferences.recordFailure() }
        coVerify(exactly = 0) { backupManager.export(any()) }
    }

    @Test
    fun `an export that returns false is a recorded failure`() = runTest {
        every { safBackupStore.isWritable(folderUri) } returns true
        coEvery { backupManager.currentFingerprint() } returns "fp"
        every { safBackupStore.createBackupFile(folderUri, any()) } returns targetUri
        coEvery { backupManager.export(targetUri) } returns false

        assertEquals(AutoBackupOutcome.FAILED, autoBackup().run())

        coVerify(exactly = 1) { backupPreferences.recordFailure() }
        coVerify(exactly = 0) { backupPreferences.recordSuccess(any(), any()) }
    }

    @Test
    fun `a successful run prunes the oldest beyond the keep window and records success`() = runTest {
        every { safBackupStore.isWritable(folderUri) } returns true
        coEvery { backupManager.currentFingerprint() } returns "fp"
        every { safBackupStore.createBackupFile(folderUri, any()) } returns targetUri
        coEvery { backupManager.export(targetUri) } returns true

        // KEEP_BACKUPS + 2 existing files, so the two oldest must be pruned.
        val existing = (1..(KEEP_BACKUPS + 2)).map { "lodgy-backup-2026-09-%02d-100000.zip".format(it) }
        every { safBackupStore.listBackupNames(folderUri) } returns existing.shuffled()
        every { safBackupStore.deleteBackups(folderUri, any()) } just Runs

        assertEquals(AutoBackupOutcome.SUCCESS, autoBackup().run())

        val expectedDeleted = setOf(
            "lodgy-backup-2026-09-01-100000.zip",
            "lodgy-backup-2026-09-02-100000.zip",
        )
        coVerify(exactly = 1) { safBackupStore.deleteBackups(folderUri, expectedDeleted) }
        coVerify(exactly = 1) { backupPreferences.recordSuccess(any(), "fp") }
    }
}
