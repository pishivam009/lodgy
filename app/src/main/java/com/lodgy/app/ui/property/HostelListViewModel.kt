package com.lodgy.app.ui.property

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.WardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HostelListUiState(
    val hostels: List<Hostel> = emptyList(),
    val selectedHostelId: String? = null,
)

@HiltViewModel
class HostelListViewModel @Inject constructor(
    private val wardenRepository: WardenRepository,
    private val hostelRepository: HostelRepository,
    private val hostelPreferences: HostelPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostelListUiState())
    val uiState: StateFlow<HostelListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val warden = wardenRepository.getWarden() ?: return@launch
            combine(
                hostelRepository.getByWardenId(warden.id),
                hostelPreferences.selectedHostelId,
            ) { hostels, selectedId ->
                HostelListUiState(
                    hostels = hostels,
                    selectedHostelId = selectedId ?: hostels.firstOrNull()?.id,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun selectHostel(id: String) {
        viewModelScope.launch { hostelPreferences.setSelectedHostelId(id) }
    }
}
