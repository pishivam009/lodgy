package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.dao.VacantBedRow
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantNoteRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferUiState(
    val loading: Boolean = true,
    val hasActiveAgreement: Boolean = false,
    val tenantName: String = "",
    val currentLocation: BedLocation? = null,
    val options: List<VacantBedRow> = emptyList(),
    val selectedBedId: String? = null,
    val rent: String = "",
    val saved: Boolean = false,
) {
    val selectedOption: VacantBedRow? get() = options.firstOrNull { it.bedId == selectedBedId }
    val canSave: Boolean get() = selectedBedId != null && (rent.toDoubleOrNull() ?: -1.0) >= 0.0
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val bedRepository: BedRepository,
    private val tenantNoteRepository: TenantNoteRepository,
    private val hostelPreferences: HostelPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])
    private var agreement: TenancyAgreement? = null

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val active = tenancyAgreementRepository.getActiveByTenantId(tenantId)
            agreement = active
            if (active == null) {
                _uiState.update { it.copy(loading = false, hasActiveAgreement = false) }
                return@launch
            }
            val hostelId = hostelPreferences.selectedHostelId.first()
            _uiState.update {
                it.copy(
                    loading = false,
                    hasActiveAgreement = true,
                    tenantName = tenantRepository.getById(tenantId)?.name.orEmpty(),
                    currentLocation = bedRepository.getLocation(active.bedId),
                    options = hostelId?.let { id -> bedRepository.getVacantBedsByHostel(id) }.orEmpty(),
                    rent = active.agreedRent.toString(),
                )
            }
        }
    }

    /** Picking a bed re-prices to that room's rate; the warden can still overwrite it, and past
     *  invoices are immutable snapshots either way, so only future ones move. */
    fun onBedSelected(bedId: String) {
        _uiState.update { state ->
            val option = state.options.firstOrNull { it.bedId == bedId }
            state.copy(selectedBedId = bedId, rent = option?.pricePerBed?.toString() ?: state.rent)
        }
    }

    fun onRentChange(value: String) = _uiState.update { it.copy(rent = value) }

    /** [noteText] is built by the caller so the timeline entry lands in the warden's language. */
    fun confirmTransfer(noteText: String) {
        val current = agreement ?: return
        val state = _uiState.value
        val newBedId = state.selectedBedId ?: return
        val rent = state.rent.toDoubleOrNull() ?: return
        if (newBedId == current.bedId) return
        viewModelScope.launch {
            tenancyAgreementRepository.transferBed(current, newBedId, rent)
            bedRepository.setOccupied(newBedId)
            bedRepository.setVacant(current.bedId)
            tenantNoteRepository.create(
                tenantId = tenantId,
                type = NoteType.GENERAL,
                text = noteText,
                photoPath = null,
                occurredOn = System.currentTimeMillis(),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
