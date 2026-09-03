package com.lodgy.app.data.prefs

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** See [AuthPreferencesTest] for why this stays a single test method. */
class HostelPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `selectedHostelId defaults to null then reflects what was last set`() = runTest {
        val context: Context = mockk()
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempFolder.newFolder("files")
        val prefs = HostelPreferences(context)

        assertNull(prefs.selectedHostelId.first())

        prefs.setSelectedHostelId("hostel-1")
        assertEquals("hostel-1", prefs.selectedHostelId.first())

        prefs.setSelectedHostelId("hostel-2")
        assertEquals("hostel-2", prefs.selectedHostelId.first())
    }
}
