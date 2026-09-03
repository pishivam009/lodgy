package com.lodgy.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricAvailabilityTest {

    private val context: Context = mockk()
    private val biometricManager: BiometricManager = mockk()

    @After
    fun tearDown() {
        unmockkStatic(BiometricManager::class)
    }

    private fun stub(canAuthenticateResult: Int) {
        mockkStatic(BiometricManager::class)
        every { BiometricManager.from(context) } returns biometricManager
        every {
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK,
            )
        } returns canAuthenticateResult
    }

    @Test
    fun `available when the device reports biometric success`() {
        stub(BiometricManager.BIOMETRIC_SUCCESS)
        assertTrue(BiometricAvailability(context).isAvailable())
    }

    @Test
    fun `unavailable when no hardware is present`() {
        stub(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
        assertFalse(BiometricAvailability(context).isAvailable())
    }

    @Test
    fun `unavailable when nothing is enrolled`() {
        stub(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        assertFalse(BiometricAvailability(context).isAvailable())
    }
}
