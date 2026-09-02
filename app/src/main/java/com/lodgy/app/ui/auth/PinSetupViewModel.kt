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

private const val PIN_LENGTH = 4

enum class PinSetupStep { ENTER, CONFIRM, BIOMETRIC, DONE }

data class PinSetupUiState(
    val step: PinSetupStep = PinSetupStep.ENTER,
    val enteredDigits: String = "",
    val firstPin: String? = null,
    @param:StringRes val error: Int? = null,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
)

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

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.step != PinSetupStep.ENTER && state.step != PinSetupStep.CONFIRM) return
        if (state.enteredDigits.length >= PIN_LENGTH) return

        val next = state.enteredDigits + digit
        if (next.length < PIN_LENGTH) {
            _uiState.update { it.copy(enteredDigits = next, error = null) }
            return
        }

        when (state.step) {
            PinSetupStep.ENTER -> _uiState.update {
                it.copy(enteredDigits = "", firstPin = next, step = PinSetupStep.CONFIRM, error = null)
            }
            PinSetupStep.CONFIRM -> {
                if (next == state.firstPin) {
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
        authPreferences.setBiometricEnabled(_uiState.value.biometricEnabled)
        _uiState.update { it.copy(step = PinSetupStep.DONE) }
    }
}
