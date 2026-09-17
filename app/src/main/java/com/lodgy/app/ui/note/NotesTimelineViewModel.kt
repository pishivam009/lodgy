package com.lodgy.app.ui.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.TenantStayRow
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.OccupancyPeriodRepository
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

/** One tenancy's bed sequence - a returning tenant has more than one of these, shown as separate
 *  stays rather than merged (LODGY-92). A backfilled period with no agreement to group under
 *  ([TenantStayRow.tenancyAgreementId] null) is its own single-period stay. */
data class StayGroup(val tenancyAgreementId: String?, val periods: List<TenantStayRow>)

data class NotesTimelineUiState(
    val loading: Boolean = true,
    val entries: List<TimelineEntry> = emptyList(),
    val stayGroups: List<StayGroup> = emptyList(),
) {
    val notes: List<TenantNote> get() = entries.filterIsInstance<TimelineEntry.NoteEntry>().map { it.note }

    /** A tenant who has never moved has exactly one, app-recorded period ever; showing a one-row
     *  "history" section for them would read as broken rather than informative. A backfilled
     *  period is different even alone: the warden typed it in specifically because there is a
     *  story before the tenant's current, otherwise-untracked stay (LODGY-91's no-auto-backfill
     *  decision means that current stay has no period of its own to add to the count) - hiding it
     *  would bury the one thing they just went to the trouble of recording (LODGY-94's AC3). */
    val showStays: Boolean get() = stayGroups.sumOf { it.periods.size } > 1 || stayGroups.any { group -> group.periods.any { it.backfilled } }
}

@HiltViewModel
class NotesTimelineViewModel @Inject constructor(
    tenantNoteRepository: TenantNoteRepository,
    creditRepository: CreditRepository,
    occupancyPeriodRepository: OccupancyPeriodRepository,
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
                occupancyPeriodRepository.observeStaysByTenantId(tenantId),
            ) { notes, credits, stays ->
                val entries = (notes.map(TimelineEntry::NoteEntry) + credits.map(TimelineEntry::CreditEntry))
                    .sortedByDescending { it.occurredOn }
                val stayGroups = stays
                    .groupBy { it.tenancyAgreementId ?: it.periodId }
                    .values
                    .map { periods -> StayGroup(periods.first().tenancyAgreementId, periods) }
                    .sortedByDescending { group -> group.periods.minOf { it.startDate } }
                Pair(entries, stayGroups)
            }.collect { (entries, stayGroups) ->
                _uiState.update { it.copy(loading = false, entries = entries, stayGroups = stayGroups) }
            }
        }
    }
}
