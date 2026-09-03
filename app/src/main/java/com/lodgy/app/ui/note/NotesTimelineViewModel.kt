package com.lodgy.app.ui.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.TenantNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Notes are editable records the warden types; credits are money already recorded elsewhere and
 *  only shown here, so the timeline distinguishes them rather than flattening both into text. */
sealed interface TimelineEntry {
    val occurredOn: Long

    data class NoteEntry(val note: TenantNote) : TimelineEntry {
        override val occurredOn: Long get() = note.occurredOn
    }

    data class CreditEntry(val credit: Credit) : TimelineEntry {
        override val occurredOn: Long get() = credit.createdAt
    }
}

data class NotesTimelineUiState(
    val loading: Boolean = true,
    val entries: List<TimelineEntry> = emptyList(),
) {
    val notes: List<TenantNote> get() = entries.filterIsInstance<TimelineEntry.NoteEntry>().map { it.note }
}

@HiltViewModel
class NotesTimelineViewModel @Inject constructor(
    tenantNoteRepository: TenantNoteRepository,
    creditRepository: CreditRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val tenantId: String = checkNotNull(savedStateHandle["tenantId"])

    private val _uiState = MutableStateFlow(NotesTimelineUiState())
    val uiState: StateFlow<NotesTimelineUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tenantNoteRepository.getByTenantId(tenantId),
                creditRepository.getByTenantId(tenantId),
            ) { notes, credits ->
                (notes.map(TimelineEntry::NoteEntry) + credits.map(TimelineEntry::CreditEntry))
                    .sortedByDescending { it.occurredOn }
            }.collect { entries -> _uiState.update { it.copy(loading = false, entries = entries) } }
        }
    }
}
