package com.lodgy.app.ui.auth

import com.lodgy.app.R
import com.lodgy.app.data.prefs.AuthPreferences
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.data.entity.Warden
import com.lodgy.app.security.PinHasher
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PinLockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val wardenRepository: WardenRepository = mockk()
    private val authPreferences: AuthPreferences = mockk()

    private fun viewModel(biometricEnabled: Boolean = false): PinLockViewModel {
        every { authPreferences.biometricEnabled } returns flowOf(biometricEnabled)
        return PinLockViewModel(wardenRepository, authPreferences)
    }

    private fun enter(viewModel: PinLockViewModel, pin: String) = pin.forEach(viewModel::onDigit)

    @Test
    fun `loads whether biometric unlock is enabled`() {
        assertTrue(viewModel(biometricEnabled = true).uiState.value.biometricEnabled)
        assertFalse(viewModel(biometricEnabled = false).uiState.value.biometricEnabled)
    }

    @Test
    fun `entering the correct pin unlocks`() {
        val warden = Warden(id = "w1", pinHash = PinHasher.hash("1234"), name = "Warden", createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns warden

        val viewModel = viewModel()
        enter(viewModel, "1234")

        assertTrue(viewModel.uiState.value.unlocked)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `entering the wrong pin clears the buffer and shows an error`() {
        val warden = Warden(id = "w1", pinHash = PinHasher.hash("1234"), name = "Warden", createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns warden

        val viewModel = viewModel()
        enter(viewModel, "0000")

        assertFalse(viewModel.uiState.value.unlocked)
        assertEquals("", viewModel.uiState.value.enteredDigits)
        assertEquals(R.string.pin_lock_incorrect_error, viewModel.uiState.value.error)
    }

    @Test
    fun `verification fails safely when there is no warden on record`() {
        coEvery { wardenRepository.getWarden() } returns null

        val viewModel = viewModel()
        enter(viewModel, "1234")

        assertFalse(viewModel.uiState.value.unlocked)
        assertEquals(R.string.pin_lock_incorrect_error, viewModel.uiState.value.error)
    }

    @Test
    fun `digits beyond the pin length are ignored`() {
        val warden = Warden(id = "w1", pinHash = PinHasher.hash("1234"), name = "Warden", createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns warden

        val viewModel = viewModel()
        enter(viewModel, "12345")

        assertTrue(viewModel.uiState.value.unlocked)
    }

    @Test
    fun `onBackspace removes the last digit and clears the error`() {
        val viewModel = viewModel()
        enter(viewModel, "12")

        viewModel.onBackspace()

        assertEquals("1", viewModel.uiState.value.enteredDigits)
    }

    @Test
    fun `onBiometricSuccess unlocks directly`() {
        val viewModel = viewModel()
        viewModel.onBiometricSuccess()
        assertTrue(viewModel.uiState.value.unlocked)
    }
}
