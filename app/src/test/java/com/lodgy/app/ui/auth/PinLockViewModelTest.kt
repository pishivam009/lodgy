package com.lodgy.app.ui.auth

import android.net.Uri
import com.lodgy.app.R
import com.lodgy.app.backup.BackupManager
import com.lodgy.app.data.prefs.AuthPreferences
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.data.entity.Warden
import com.lodgy.app.security.PinHasher
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PinLockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val wardenRepository: WardenRepository = mockk()
    private val authPreferences: AuthPreferences = mockk()
    private val backupManager: BackupManager = mockk()
    private var failCount = 0

    private fun viewModel(
        biometricEnabled: Boolean = false,
        pinLength: Int = 4,
        startingFailures: Int = 0,
        lastFailedAt: Long = 0L,
    ): PinLockViewModel {
        every { authPreferences.biometricEnabled } returns flowOf(biometricEnabled)
        every { authPreferences.pinLength } returns flowOf(pinLength)
        failCount = startingFailures
        every { authPreferences.failedPinAttempts } returns flowOf(startingFailures)
        every { authPreferences.lastFailedPinAt } returns flowOf(lastFailedAt)
        coEvery { authPreferences.recordFailedPinAttempt(any()) } answers { ++failCount }
        coEvery { authPreferences.resetFailedPinAttempts() } answers { failCount = 0 }
        return PinLockViewModel(wardenRepository, authPreferences, backupManager)
    }

    private val wrongWarden get() = Warden(id = "w1", pinHash = PinHasher.hash("1234"), name = "Warden", createdAt = 0L, updatedAt = 0L)

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

    @Test
    fun `a six-digit pin is only verified once all six digits are in`() {
        val warden = Warden(id = "w1", pinHash = PinHasher.hash("123456"), name = "Warden", createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns warden

        val viewModel = viewModel(pinLength = 6)
        enter(viewModel, "1234")

        assertFalse(viewModel.uiState.value.unlocked)
        assertNull(viewModel.uiState.value.error)

        enter(viewModel, "56")
        assertTrue(viewModel.uiState.value.unlocked)
    }

    @Test
    fun `an install from before configurable length still unlocks on four digits`() {
        val warden = Warden(id = "w1", pinHash = PinHasher.hash("1234"), name = "Warden", createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns warden

        val viewModel = viewModel(pinLength = AuthPreferences.DEFAULT_PIN_LENGTH)
        enter(viewModel, "1234")

        assertTrue(viewModel.uiState.value.unlocked)
    }

    @Test
    fun `the forgot flow only opens the reset step after a backup is actually saved`() {
        val destination: Uri = mockk()
        coEvery { backupManager.export(destination) } returns true
        val viewModel = viewModel()

        viewModel.onForgotPinClicked()
        assertTrue(viewModel.uiState.value.showForgot)
        // Reset must not be available before a backup exists (AC3).
        assertFalse(viewModel.uiState.value.backupExported)

        viewModel.onBackupExportPicked(destination)

        val state = viewModel.uiState.value
        assertFalse(state.exporting)
        assertTrue(state.backupExported)
        assertEquals(R.string.forgot_pin_backup_saved, state.forgotMessage)
    }

    @Test
    fun `a failed export does not open the reset step and clears nothing`() {
        val destination: Uri = mockk()
        coEvery { backupManager.export(destination) } returns false
        val viewModel = viewModel()

        viewModel.onForgotPinClicked()
        viewModel.onBackupExportPicked(destination)

        val state = viewModel.uiState.value
        assertFalse(state.backupExported)
        assertEquals(R.string.forgot_pin_backup_failed, state.forgotMessage)
        assertFalse(state.pinReset)
        // Nothing destructive can have happened on a failed export (AC5).
        coVerify(exactly = 0) { wardenRepository.clearWarden() }
    }

    @Test
    fun `confirming reset clears the warden and signals the host to go to setup`() {
        coEvery { wardenRepository.clearWarden() } returns Unit
        val viewModel = viewModel()

        viewModel.onResetConfirmed()

        assertTrue(viewModel.uiState.value.pinReset)
        coVerify(exactly = 1) { wardenRepository.clearWarden() }
    }

    @Test
    fun `the first five wrong attempts impose no wait`() {
        coEvery { wardenRepository.getWarden() } returns wrongWarden
        val viewModel = viewModel()

        repeat(5) { enter(viewModel, "0000") }

        assertNull(viewModel.uiState.value.lockedUntilMillis)
        assertFalse(viewModel.uiState.value.isLockedOut)
    }

    @Test
    fun `the sixth wrong attempt locks entry, and further taps are ignored until it clears`() {
        coEvery { wardenRepository.getWarden() } returns wrongWarden
        val viewModel = viewModel()

        repeat(6) { enter(viewModel, "0000") }

        val state = viewModel.uiState.value
        assertTrue(state.isLockedOut)
        assertNotNull(state.lockedUntilMillis)
        // Locked: typing does nothing, so a scripted hammering can't keep guessing.
        enter(viewModel, "1234")
        assertEquals("", viewModel.uiState.value.enteredDigits)
        assertFalse(viewModel.uiState.value.unlocked)
    }

    @Test
    fun `a correct pin clears the failure count`() {
        coEvery { wardenRepository.getWarden() } returns wrongWarden
        val viewModel = viewModel()

        repeat(2) { enter(viewModel, "0000") }
        enter(viewModel, "1234")

        assertTrue(viewModel.uiState.value.unlocked)
        coVerify { authPreferences.resetFailedPinAttempts() }
    }

    @Test
    fun `a biometric unlock clears the failure count and is never penalised`() {
        val viewModel = viewModel()

        viewModel.onBiometricSuccess()

        assertTrue(viewModel.uiState.value.unlocked)
        coVerify { authPreferences.resetFailedPinAttempts() }
    }

    @Test
    fun `an active lockout is restored on launch, so a restart is not a way around it`() {
        // Six prior failures a moment ago -> still inside the 30s wait when the app reopens.
        val viewModel = viewModel(startingFailures = 6, lastFailedAt = System.currentTimeMillis())

        assertTrue(viewModel.uiState.value.isLockedOut)
    }

    @Test
    fun `an expired lockout does not carry over on launch`() {
        // Six failures, but long enough ago that the wait has passed.
        val viewModel = viewModel(startingFailures = 6, lastFailedAt = System.currentTimeMillis() - 60_000)

        assertNull(viewModel.uiState.value.lockedUntilMillis)
        assertFalse(viewModel.uiState.value.isLockedOut)
    }

    @Test
    fun `the forgot-PIN route is reachable even while locked out`() {
        coEvery { wardenRepository.getWarden() } returns wrongWarden
        val viewModel = viewModel()

        repeat(6) { enter(viewModel, "0000") }
        assertTrue(viewModel.uiState.value.isLockedOut)

        // A warden who has genuinely forgotten must not be made to wait out the penalty to escape.
        viewModel.onForgotPinClicked()
        assertTrue(viewModel.uiState.value.showForgot)
    }

    @Test
    fun `dismissing the forgot flow resets its state`() {
        val destination: Uri = mockk()
        coEvery { backupManager.export(destination) } returns true
        val viewModel = viewModel()

        viewModel.onForgotPinClicked()
        viewModel.onBackupExportPicked(destination)
        viewModel.onForgotDismissed()

        val state = viewModel.uiState.value
        assertFalse(state.showForgot)
        assertFalse(state.backupExported)
        assertNull(state.forgotMessage)
    }
}
