package com.lodgy.app.data.prefs

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * `Context.authDataStore` is a top-level delegate, so its underlying DataStore instance is a
 * process-wide singleton pinned to whichever [Context] first resolves it. Everything this test
 * needs must therefore happen in a single test method against a single mocked context/temp dir -
 * splitting it across methods would leak state (or a deleted directory) from one into the next.
 */
class AuthPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `biometricEnabled defaults to false then reflects what was last set`() = runTest {
        val context: Context = mockk()
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempFolder.newFolder("files")
        val prefs = AuthPreferences(context)

        assertFalse(prefs.biometricEnabled.first())

        prefs.setBiometricEnabled(true)
        assertTrue(prefs.biometricEnabled.first())

        prefs.setBiometricEnabled(false)
        assertFalse(prefs.biometricEnabled.first())
    }
}
