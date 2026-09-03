package com.lodgy.app.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.notify.LodgyNotifications
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val vacancyEnabled: Boolean = true,
    val duesEnabled: Boolean = true,
    val vacancyThresholdDays: Int = NotificationPreferences.DEFAULT_VACANCY_THRESHOLD_DAYS,
    /** False when Android itself is blocking notifications, whatever these switches say. */
    val systemPermissionGranted: Boolean = true,
) {
    val thresholdOptions: List<Int> get() = listOf(3, 7, 14, 30)
}

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val preferences: NotificationPreferences,
    private val notifications: LodgyNotifications,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.vacancyEnabled,
                preferences.duesEnabled,
                preferences.vacancyThresholdDays,
            ) { vacancy, dues, threshold ->
                NotificationSettingsUiState(
                    vacancyEnabled = vacancy,
                    duesEnabled = dues,
                    vacancyThresholdDays = threshold,
                    systemPermissionGranted = notifications.canPost(),
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun refreshPermissionState() {
        _uiState.update { it.copy(systemPermissionGranted = notifications.canPost()) }
    }

    fun onVacancyEnabledChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setVacancyEnabled(enabled) }
    }

    fun onDuesEnabledChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setDuesEnabled(enabled) }
    }

    fun onThresholdChange(days: Int) {
        viewModelScope.launch { preferences.setVacancyThresholdDays(days) }
    }
}
