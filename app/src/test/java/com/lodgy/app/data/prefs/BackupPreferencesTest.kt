package com.lodgy.app.data.prefs

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Like [AuthPreferencesTest], the DataStore delegate is a process-wide singleton pinned to the
 * first context that resolves it, so the whole lifecycle lives in one test method against one
 * temp dir.
 */
class BackupPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `folder, last success and failure round-trip and default sensibly`() = runTest {
        val context: Context = mockk()
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempFolder.newFolder("files")
        val prefs = BackupPreferences(context)

        // A fresh install: nothing configured, nothing ever backed up, no prior failure.
        assertNull(prefs.folderUri.first())
        assertNull(prefs.lastSuccessTime.first())
        assertNull(prefs.lastFingerprint.first())
        assertFalse(prefs.lastAttemptFailed.first())

        prefs.setFolderUri("content://tree/x")
        assertEquals("content://tree/x", prefs.folderUri.first())

        prefs.recordSuccess(time = 123L, fingerprint = "fp1")
        assertEquals(123L, prefs.lastSuccessTime.first())
        assertEquals("fp1", prefs.lastFingerprint.first())
        assertFalse(prefs.lastAttemptFailed.first())

        // A later failure flags itself but leaves the last good backup's time and fingerprint,
        // so the dashboard can show both "last worked then" and "today failed".
        prefs.recordFailure()
        assertTrue(prefs.lastAttemptFailed.first())
        assertEquals(123L, prefs.lastSuccessTime.first())
        assertEquals("fp1", prefs.lastFingerprint.first())

        // A success clears the failure flag again.
        prefs.recordSuccess(time = 456L, fingerprint = "fp2")
        assertFalse(prefs.lastAttemptFailed.first())
        assertEquals(456L, prefs.lastSuccessTime.first())

        // Re-picking a folder after a failure clears the failure, so the dashboard recovers at once
        // rather than staying red until the next successful run (AC7).
        prefs.recordFailure()
        assertTrue(prefs.lastAttemptFailed.first())
        prefs.setFolderUri("content://tree/y")
        assertFalse(prefs.lastAttemptFailed.first())

        // Clearing the folder returns to the never-configured state.
        prefs.setFolderUri(null)
        assertNull(prefs.folderUri.first())
    }
}
