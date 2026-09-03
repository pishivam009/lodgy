package com.lodgy.app.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.R
import com.lodgy.app.data.prefs.AuthPreferences
import com.lodgy.app.data.repository.WardenRepository
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
)

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val wardenRepository: WardenRepository,
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinLockUiState())
    val uiState: StateFlow<PinLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val biometricEnabled = authPreferences.biometricEnabled.first()
            val pinLength = authPreferences.pinLength.first()
            _uiState.update { it.copy(biometricEnabled = biometricEnabled, pinLength = pinLength) }
        }
    }

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.enteredDigits.length >= state.pinLength) return
        val next = state.enteredDigits + digit
        _uiState.update { it.copy(enteredDigits = next, error = null) }
        if (next.length == state.pinLength) verify(next)
    }

    fun onBackspace() {
        _uiState.update { it.copy(enteredDigits = it.enteredDigits.dropLast(1), error = null) }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(unlocked = true) }
    }

    private fun verify(pin: String) {
        viewModelScope.launch {
            val warden = wardenRepository.getWarden()
            val correct = warden != null && PinHasher.verify(pin, warden.pinHash)
            _uiState.update {
                if (correct) {
                    it.copy(unlocked = true)
                } else {
                    it.copy(enteredDigits = "", error = R.string.pin_lock_incorrect_error)
                }
            }
        }
    }
}
