package com.lodgy.app.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.R
import com.lodgy.app.data.prefs.AuthPreferences
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.security.BiometricAvailability
import com.lodgy.app.security.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinSetupStep { ENTER, CONFIRM, BIOMETRIC, DONE }

data class PinSetupUiState(
    val step: PinSetupStep = PinSetupStep.ENTER,
    val pinLength: Int = AuthPreferences.DEFAULT_PIN_LENGTH,
    val enteredDigits: String = "",
    val firstPin: String? = null,
    @param:StringRes val error: Int? = null,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
) {
    /** Nothing advances on its own any more: with a chosen length there is no digit count that
     *  reliably means "done", so the warden confirms each step (AC 2). */
    val canSubmit: Boolean get() = enteredDigits.length == pinLength

    val lengthOptions: List<Int> get() = (AuthPreferences.MIN_PIN_LENGTH..AuthPreferences.MAX_PIN_LENGTH).toList()
}

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val wardenRepository: WardenRepository,
    private val authPreferences: AuthPreferences,
    biometricAvailability: BiometricAvailability,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PinSetupUiState(biometricAvailable = biometricAvailability.isAvailable()),
    )
    val uiState: StateFlow<PinSetupUiState> = _uiState.asStateFlow()

    /** Only selectable before any digits are typed, so the dots never disagree with the entry. */
    fun onLengthChange(length: Int) {
        val state = _uiState.value
        if (state.step != PinSetupStep.ENTER || state.enteredDigits.isNotEmpty()) return
        _uiState.update { it.copy(pinLength = length, error = null) }
    }

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.step != PinSetupStep.ENTER && state.step != PinSetupStep.CONFIRM) return
        if (state.enteredDigits.length >= state.pinLength) return

        _uiState.update { it.copy(enteredDigits = it.enteredDigits + digit, error = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        when (state.step) {
            PinSetupStep.ENTER -> _uiState.update {
                it.copy(enteredDigits = "", firstPin = state.enteredDigits, step = PinSetupStep.CONFIRM, error = null)
            }
            PinSetupStep.CONFIRM -> {
                if (state.enteredDigits == state.firstPin) {
                    advancePastConfirm()
                } else {
                    _uiState.update {
                        it.copy(
                            enteredDigits = "",
                            firstPin = null,
                            step = PinSetupStep.ENTER,
                            error = R.string.pin_setup_mismatch_error,
                        )
                    }
                }
            }
            else -> Unit
        }
    }

    fun onBackspace() {
        _uiState.update { it.copy(enteredDigits = it.enteredDigits.dropLast(1), error = null) }
    }

    fun onBiometricToggle(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
    }

    fun onFinishBiometricStep() {
        viewModelScope.launch { persistAndFinish() }
    }

    private fun advancePastConfirm() {
        if (_uiState.value.biometricAvailable) {
            _uiState.update { it.copy(enteredDigits = "", step = PinSetupStep.BIOMETRIC) }
        } else {
            viewModelScope.launch { persistAndFinish() }
        }
    }

    private suspend fun persistAndFinish() {
        val pin = _uiState.value.firstPin ?: return
        wardenRepository.createWarden(PinHasher.hash(pin))
        authPreferences.setPinLength(pin.length)
        authPreferences.setBiometricEnabled(_uiState.value.biometricEnabled)
        _uiState.update { it.copy(step = PinSetupStep.DONE) }
    }
}
