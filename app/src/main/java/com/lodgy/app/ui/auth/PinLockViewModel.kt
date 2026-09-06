package com.lodgy.app.ui.auth

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.R
import com.lodgy.app.backup.BackupManager
import com.lodgy.app.data.prefs.AuthPreferences
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.security.PinBackoff
import com.lodgy.app.security.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PinLockUiState(
    val pinLength: Int = AuthPreferences.DEFAULT_PIN_LENGTH,
    val enteredDigits: String = "",
    @param:StringRes val error: Int? = null,
    val biometricEnabled: Boolean = false,
    val unlocked: Boolean = false,
    /** The forgotten-PIN recovery flow, reached from a link on the lock screen (LODGY-76). */
    val showForgot: Boolean = false,
    val exporting: Boolean = false,
    /** True once a backup has been saved, which is the only thing that unlocks the reset step - a
     *  warden must have a file in hand before anything is cleared (AC3). */
    val backupExported: Boolean = false,
    @param:StringRes val forgotMessage: Int? = null,
    /** Set when the PIN has been cleared, so the host can return to first-launch setup. */
    val pinReset: Boolean = false,
    /** While set and still in the future, PIN entry is refused after too many wrong attempts
     *  (LODGY-77). The Screen counts it down; the forgot-PIN route stays reachable throughout. */
    val lockedUntilMillis: Long? = null,
) {
    val isLockedOut: Boolean get() = lockedUntilMillis?.let { System.currentTimeMillis() < it } == true
}

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val wardenRepository: WardenRepository,
    private val authPreferences: AuthPreferences,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinLockUiState())
    val uiState: StateFlow<PinLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val biometricEnabled = authPreferences.biometricEnabled.first()
            val pinLength = authPreferences.pinLength.first()
            // Restore any wait already in force, so force-stopping or rebooting is not a way around
            // the delay (LODGY-77, AC3).
            val until = PinBackoff.lockedUntil(
                authPreferences.failedPinAttempts.first(),
                authPreferences.lastFailedPinAt.first(),
            )
            _uiState.update {
                it.copy(
                    biometricEnabled = biometricEnabled,
                    pinLength = pinLength,
                    lockedUntilMillis = until.takeIf { end -> System.currentTimeMillis() < end },
                )
            }
        }
    }

    fun onDigit(digit: Char) {
        val state = _uiState.value
        // Frozen while the backoff delay is in force; the Screen shows the countdown (LODGY-77).
        if (state.isLockedOut) return
        if (state.enteredDigits.length >= state.pinLength) return
        val next = state.enteredDigits + digit
        _uiState.update { it.copy(enteredDigits = next, error = null) }
        if (next.length == state.pinLength) verify(next)
    }

    fun onBackspace() {
        if (_uiState.value.isLockedOut) return
        _uiState.update { it.copy(enteredDigits = it.enteredDigits.dropLast(1), error = null) }
    }

    fun onBiometricSuccess() {
        // Biometric is never penalised by wrong PINs, and getting in clears the count (AC4, AC7).
        viewModelScope.launch { authPreferences.resetFailedPinAttempts() }
        _uiState.update { it.copy(unlocked = true) }
    }

    /** Called by the Screen's countdown when the wait has elapsed, to re-enable entry. */
    fun onLockoutElapsed() {
        val until = _uiState.value.lockedUntilMillis ?: return
        if (System.currentTimeMillis() >= until) {
            _uiState.update { it.copy(lockedUntilMillis = null, error = null) }
        }
    }

    private fun verify(pin: String) {
        viewModelScope.launch {
            val warden = wardenRepository.getWarden()
            val correct = warden != null && PinHasher.verify(pin, warden.pinHash)
            if (correct) {
                authPreferences.resetFailedPinAttempts()
                _uiState.update { it.copy(unlocked = true) }
            } else {
                val now = System.currentTimeMillis()
                val failures = authPreferences.recordFailedPinAttempt(now)
                val delay = PinBackoff.requiredDelayMillis(failures)
                _uiState.update {
                    it.copy(
                        enteredDigits = "",
                        error = R.string.pin_lock_incorrect_error,
                        lockedUntilMillis = if (delay > 0) now + delay else null,
                    )
                }
            }
        }
    }

    fun onForgotPinClicked() =
        _uiState.update { it.copy(showForgot = true, backupExported = false, forgotMessage = null) }

    fun onForgotDismissed() =
        _uiState.update {
            it.copy(showForgot = false, exporting = false, backupExported = false, forgotMessage = null)
        }

    /** Exports a full backup straight to the picked SAF location without unlocking, reusing the
     *  same export the app uses everywhere (LODGY-28). Only a successful export opens the reset
     *  step (AC2, AC3); a failure leaves the warden exactly where they were, nothing lost (AC5). */
    fun onBackupExportPicked(destination: Uri) {
        _uiState.update { it.copy(exporting = true, forgotMessage = null) }
        viewModelScope.launch {
            val ok = backupManager.export(destination)
            _uiState.update {
                it.copy(
                    exporting = false,
                    backupExported = it.backupExported || ok,
                    forgotMessage = if (ok) R.string.forgot_pin_backup_saved else R.string.forgot_pin_backup_failed,
                )
            }
        }
    }

    /** Clears the stored PIN after the warden has confirmed and has a backup in hand. Their data is
     *  left intact; the host takes them to set a new PIN. */
    fun onResetConfirmed() {
        viewModelScope.launch {
            wardenRepository.clearWarden()
            _uiState.update { it.copy(pinReset = true) }
        }
    }
}
