package com.lodgy.app.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.repository.WardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppStartState {
    data object Loading : AppStartState
    data object NeedsPinSetup : AppStartState
    data object Locked : AppStartState
    data object Unlocked : AppStartState
}

@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val wardenRepository: WardenRepository,
) : ViewModel(), DefaultLifecycleObserver {

    private val _state = MutableStateFlow<AppStartState>(AppStartState.Loading)
    val state: StateFlow<AppStartState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = if (wardenRepository.getWarden() == null) {
                AppStartState.NeedsPinSetup
            } else {
                AppStartState.Locked
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // Re-lock when the whole app leaves the foreground (not per-Activity onPause, so a
    // biometric system dialog showing over us doesn't itself trigger a re-lock).
    override fun onStop(owner: LifecycleOwner) {
        if (_state.value == AppStartState.Unlocked) {
            _state.value = AppStartState.Locked
        }
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    fun onPinSetupComplete() {
        _state.value = AppStartState.Unlocked
    }

    fun onUnlocked() {
        _state.value = AppStartState.Unlocked
    }
}
