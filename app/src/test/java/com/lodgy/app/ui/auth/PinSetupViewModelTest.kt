package com.lodgy.app.ui.auth

import com.lodgy.app.data.prefs.AuthPreferences
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.security.BiometricAvailability
import com.lodgy.app.security.PinHasher
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PinSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val wardenRepository: WardenRepository = mockk(relaxed = true)
    private val authPreferences: AuthPreferences = mockk(relaxed = true)
    private val biometricAvailability: BiometricAvailability = mockk()

    private fun viewModel(biometricAvailable: Boolean = false): PinSetupViewModel {
        every { biometricAvailability.isAvailable() } returns biometricAvailable
        return PinSetupViewModel(wardenRepository, authPreferences, biometricAvailability)
    }

    private fun enter(viewModel: PinSetupViewModel, pin: String) = pin.forEach(viewModel::onDigit)

    @Test
    fun `entering a full pin moves from ENTER to CONFIRM and clears the buffer`() {
        val viewModel = viewModel()

        enter(viewModel, "1234")

        val state = viewModel.uiState.value
        assertEquals(PinSetupStep.CONFIRM, state.step)
        assertEquals("", state.enteredDigits)
        assertNull(state.error)
    }

    @Test
    fun `digits beyond the pin length are ignored`() {
        val viewModel = viewModel()

        enter(viewModel, "12345")

        assertEquals(PinSetupStep.CONFIRM, viewModel.uiState.value.step)
    }

    @Test
    fun `matching confirmation without biometric hardware persists the warden and finishes`() {
        val viewModel = viewModel(biometricAvailable = false)
        val pinHashSlot = slot<String>()
        coEvery { wardenRepository.createWarden(capture(pinHashSlot)) } returns Unit

        enter(viewModel, "1234")
        enter(viewModel, "1234")

        val state = viewModel.uiState.value
        assertEquals(PinSetupStep.DONE, state.step)
        assertTrue(PinHasher.verify("1234", pinHashSlot.captured))
        coVerify { authPreferences.setBiometricEnabled(false) }
    }

    @Test
    fun `mismatched confirmation resets to ENTER with an error and forgets the first pin`() {
        val viewModel = viewModel()

        enter(viewModel, "1234")
        enter(viewModel, "9999")

        val state = viewModel.uiState.value
        assertEquals(PinSetupStep.ENTER, state.step)
        assertEquals("", state.enteredDigits)
        assertNull(state.firstPin)
        assertEquals(com.lodgy.app.R.string.pin_setup_mismatch_error, state.error)
        coVerify(exactly = 0) { wardenRepository.createWarden(any()) }
    }

    @Test
    fun `matching confirmation with biometric hardware available stops at the BIOMETRIC step`() {
        val viewModel = viewModel(biometricAvailable = true)

        enter(viewModel, "1234")
        enter(viewModel, "1234")

        assertEquals(PinSetupStep.BIOMETRIC, viewModel.uiState.value.step)
        coVerify(exactly = 0) { wardenRepository.createWarden(any()) }
    }

    @Test
    fun `finishing the biometric step persists the chosen toggle and the original pin`() {
        val viewModel = viewModel(biometricAvailable = true)
        val pinHashSlot = slot<String>()
        coEvery { wardenRepository.createWarden(capture(pinHashSlot)) } returns Unit

        enter(viewModel, "1234")
        enter(viewModel, "1234")
        viewModel.onBiometricToggle(true)
        viewModel.onFinishBiometricStep()

        assertEquals(PinSetupStep.DONE, viewModel.uiState.value.step)
        assertTrue(PinHasher.verify("1234", pinHashSlot.captured))
        coVerify { authPreferences.setBiometricEnabled(true) }
    }

    @Test
    fun `digit entry is ignored once past the CONFIRM step`() {
        val viewModel = viewModel(biometricAvailable = true)
        enter(viewModel, "1234")
        enter(viewModel, "1234")
        assertEquals(PinSetupStep.BIOMETRIC, viewModel.uiState.value.step)

        viewModel.onDigit('5')

        assertEquals(PinSetupStep.BIOMETRIC, viewModel.uiState.value.step)
        assertEquals("", viewModel.uiState.value.enteredDigits)
    }

    @Test
    fun `onBackspace removes the last entered digit and clears any error`() {
        val viewModel = viewModel()
        enter(viewModel, "12")

        viewModel.onBackspace()

        assertEquals("1", viewModel.uiState.value.enteredDigits)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `biometricAvailable reflects hardware availability at construction`() {
        assertFalse(viewModel(biometricAvailable = false).uiState.value.biometricAvailable)
        assertTrue(viewModel(biometricAvailable = true).uiState.value.biometricAvailable)
    }
}
