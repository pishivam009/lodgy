package com.lodgy.app.data.prefs

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun `biometric and pin length default sensibly then reflect what was last set`() = runTest {
        val context: Context = mockk()
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempFolder.newFolder("files")
        val prefs = AuthPreferences(context)

        assertFalse(prefs.biometricEnabled.first())

        prefs.setBiometricEnabled(true)
        assertTrue(prefs.biometricEnabled.first())

        prefs.setBiometricEnabled(false)
        assertFalse(prefs.biometricEnabled.first())

        // An install from before LODGY-50 has no stored length and must keep unlocking on four.
        assertEquals(AuthPreferences.DEFAULT_PIN_LENGTH, prefs.pinLength.first())

        prefs.setPinLength(6)
        assertEquals(6, prefs.pinLength.first())

        // Clamped both ways, so a stray value cannot lock the warden out.
        prefs.setPinLength(2)
        assertEquals(AuthPreferences.MIN_PIN_LENGTH, prefs.pinLength.first())
        prefs.setPinLength(99)
        assertEquals(AuthPreferences.MAX_PIN_LENGTH, prefs.pinLength.first())

        // Failed-attempt count starts clean, increments and persists (so a restart can't dodge the
        // backoff), and a reset clears both the count and the timestamp (LODGY-77).
        assertEquals(0, prefs.failedPinAttempts.first())
        assertEquals(0L, prefs.lastFailedPinAt.first())

        assertEquals(1, prefs.recordFailedPinAttempt(1_000L))
        assertEquals(2, prefs.recordFailedPinAttempt(2_000L))
        assertEquals(2, prefs.failedPinAttempts.first())
        assertEquals(2_000L, prefs.lastFailedPinAt.first())

        prefs.resetFailedPinAttempts()
        assertEquals(0, prefs.failedPinAttempts.first())
        assertEquals(0L, prefs.lastFailedPinAt.first())
    }
}
