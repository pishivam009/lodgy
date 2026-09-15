package com.lodgy.app.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.backup.HistoryCsvReader
import com.lodgy.app.backup.StayRow
import com.lodgy.app.backup.StayRowError
import com.lodgy.app.backup.parseStayCsv
import com.lodgy.app.data.dao.BackfilledStayRow
import com.lodgy.app.data.dao.BedChoice
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.repository.BackfillOutcome
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.OccupancyPeriodRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StayBackfillUiState(
    val tenants: List<Tenant> = emptyList(),
    val beds: List<BedChoice> = emptyList(),
    val stays: List<BackfilledStayRow> = emptyList(),
    val selectedTenantId: String? = null,
    val selectedBedId: String? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    /** Non-null while correcting an existing entry rather than adding a new one. */
    val editingPeriodId: String? = null,
    val overlapError: Boolean = false,
    val saved: Boolean = false,
    val parsing: Boolean = false,
    val importing: Boolean = false,
    val csvRows: List<StayRow> = emptyList(),
    val csvErrors: List<StayRowError> = emptyList(),
    val unmatchedRows: List<StayRow> = emptyList(),
    val imported: Int? = null,
    val skippedOverlap: Int = 0,
    val readFailed: Boolean = false,
) {
    val selectedTenantName: String? get() = tenants.firstOrNull { it.id == selectedTenantId }?.name
    val selectedBed: BedChoice? get() = beds.firstOrNull { it.bedId == selectedBedId }
    val canSave: Boolean
        get() = selectedTenantId != null && selectedBedId != null &&
            startDateMillis != null && endDateMillis != null && startDateMillis <= endDateMillis
    val importableCount: Int get() = csvRows.size - unmatchedRows.size
    val canImportCsv: Boolean get() = !importing && importableCount > 0
}

/**
 * Lets a warden record occupancy that predates the app - a fast path here for a handful of stays,
 * and a bulk CSV path for a spreadsheet, matching the shape of the LODGY-44 history import this is
 * a sibling of. Skipping this entirely is fine: the app works from today onward regardless
 * (LODGY-94).
 */
@HiltViewModel
class StayBackfillViewModel @Inject constructor(
    private val occupancyPeriodRepository: OccupancyPeriodRepository,
    private val tenantRepository: TenantRepository,
    private val bedRepository: BedRepository,
    private val csvReader: HistoryCsvReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StayBackfillUiState())
    val uiState: StateFlow<StayBackfillUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    tenants = tenantRepository.getAll().first().sortedBy { t -> t.name },
                    beds = bedRepository.getAllChoices(),
                    stays = occupancyPeriodRepository.getBackfilledStays(),
                )
            }
        }
    }

    fun onTenantSelected(tenantId: String) = _uiState.update { it.copy(selectedTenantId = tenantId) }

    fun onBedSelected(bedId: String) = _uiState.update { it.copy(selectedBedId = bedId) }

    fun onStartDateChange(millis: Long) = _uiState.update { it.copy(startDateMillis = millis, overlapError = false) }

    fun onEndDateChange(millis: Long) = _uiState.update { it.copy(endDateMillis = millis, overlapError = false) }

    /** Loads an existing entry back into the form so it can be corrected in place, rather than
     *  deleted and re-typed (LODGY-94). */
    fun startEdit(row: BackfilledStayRow) {
        viewModelScope.launch {
            val period = occupancyPeriodRepository.getById(row.periodId) ?: return@launch
            _uiState.update {
                it.copy(
                    editingPeriodId = period.id,
                    selectedTenantId = period.tenantId,
                    selectedBedId = period.bedId,
                    startDateMillis = period.startDate,
                    endDateMillis = period.endDate,
                    overlapError = false,
                    saved = false,
                )
            }
        }
    }

    fun cancelEdit() = _uiState.update {
        it.copy(
            editingPeriodId = null, selectedTenantId = null, selectedBedId = null,
            startDateMillis = null, endDateMillis = null, overlapError = false,
        )
    }

    fun save() {
        val state = _uiState.value
        val tenantId = state.selectedTenantId ?: return
        val bedId = state.selectedBedId ?: return
        val start = state.startDateMillis ?: return
        val end = state.endDateMillis ?: return
        if (start > end) return

        viewModelScope.launch {
            val editingId = state.editingPeriodId
            val outcome = if (editingId != null) {
                val existing = occupancyPeriodRepository.getById(editingId) ?: return@launch
                occupancyPeriodRepository.editBackfilled(existing, bedId, start, end)
            } else {
                occupancyPeriodRepository.backfill(tenantId, bedId, start, end)
            }
            when (outcome) {
                is BackfillOutcome.Overlaps -> _uiState.update { it.copy(overlapError = true) }
                is BackfillOutcome.Saved -> _uiState.update {
                    it.copy(
                        editingPeriodId = null, selectedTenantId = null, selectedBedId = null,
                        startDateMillis = null, endDateMillis = null, overlapError = false,
                        saved = true, stays = occupancyPeriodRepository.getBackfilledStays(),
                    )
                }
            }
        }
    }

    fun delete(row: BackfilledStayRow) {
        viewModelScope.launch {
            val period = occupancyPeriodRepository.getById(row.periodId) ?: return@launch
            occupancyPeriodRepository.delete(period)
            _uiState.update { it.copy(stays = occupancyPeriodRepository.getBackfilledStays()) }
        }
    }

    fun onFilePicked(uri: Uri) {
        _uiState.update {
            it.copy(
                parsing = true, csvRows = emptyList(), csvErrors = emptyList(),
                unmatchedRows = emptyList(), imported = null, skippedOverlap = 0, readFailed = false,
            )
        }
        viewModelScope.launch {
            val text = csvReader.read(uri)
            if (text == null) {
                _uiState.update { it.copy(parsing = false, readFailed = true) }
                return@launch
            }
            val parsed = parseStayCsv(text)
            val state = _uiState.value
            val unmatched = parsed.rows.filter { row -> resolveBed(row, state.beds) == null || resolveTenant(row, state.tenants) == null }
            _uiState.update {
                it.copy(parsing = false, csvRows = parsed.rows, csvErrors = parsed.errors, unmatchedRows = unmatched)
            }
        }
    }

    fun importCsv() {
        val state = _uiState.value
        if (!state.canImportCsv) return
        _uiState.update { it.copy(importing = true) }
        viewModelScope.launch {
            var written = 0
            var overlapped = 0
            state.csvRows.forEach { row ->
                val tenant = resolveTenant(row, state.tenants) ?: return@forEach
                val bed = resolveBed(row, state.beds) ?: return@forEach
                when (occupancyPeriodRepository.backfill(tenant.id, bed.bedId, row.startDate, row.endDate)) {
                    is BackfillOutcome.Saved -> written++
                    is BackfillOutcome.Overlaps -> overlapped++
                }
            }
            _uiState.update {
                it.copy(
                    importing = false, imported = written, skippedOverlap = overlapped,
                    stays = occupancyPeriodRepository.getBackfilledStays(),
                )
            }
        }
    }

    private fun resolveTenant(row: StayRow, tenants: List<Tenant>): Tenant? =
        tenants.firstOrNull { it.phone == row.phone }

    private fun resolveBed(row: StayRow, beds: List<BedChoice>): BedChoice? = beds.firstOrNull {
        it.hostelName.equals(row.hostel, ignoreCase = true) &&
            it.roomNumber.equals(row.room, ignoreCase = true) &&
            it.bedLabel.equals(row.bed, ignoreCase = true)
    }
}
