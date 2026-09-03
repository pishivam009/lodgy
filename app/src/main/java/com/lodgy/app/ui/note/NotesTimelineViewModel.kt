package com.lodgy.app.ui.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.repository.TenantNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesTimelineUiState(
    val loading: Boolean = true,
    val notes: List<TenantNote> = emptyList(),
)

@HiltViewModel
class NotesTimelineViewModel @Inject constructor(
    tenantNoteRepository: TenantNoteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val tenantId: String = checkNotNull(savedStateHandle["tenantId"])

    private val _uiState = MutableStateFlow(NotesTimelineUiState())
    val uiState: StateFlow<NotesTimelineUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tenantNoteRepository.getByTenantId(tenantId).collect { notes ->
                _uiState.update { it.copy(loading = false, notes = notes) }
            }
        }
    }
}
