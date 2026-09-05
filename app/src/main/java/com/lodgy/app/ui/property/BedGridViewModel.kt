package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.ui.common.BedFilter
import com.lodgy.app.ui.common.matches
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [tenantId] is null for a vacant bed, and also for the rare case of an OCCUPIED bed whose
 *  tenancy has gone missing - which falls back to the vacant behaviour rather than opening a
 *  profile that is not there. */
data class SelectedBed(val bed: Bed, val tenantId: String?, val tenantName: String)

data class BedGridUiState(
    val roomNumber: String = "",
    val roomType: RoomType? = null,
    val pricePerBed: Double = 0.0,
    /** Free text as the warden typed it. Captured since LODGY-8 and, until LODGY-71, readable
     *  nowhere except the edit form - so checking what a room had meant opening a screen whose
     *  purpose is changing it. */
    val amenities: String = "",
    val beds: List<Bed> = emptyList(),
    val filter: BedFilter = BedFilter.ALL,
    /** The bed whose sheet is open. Every tap opens a sheet rather than acting immediately, so
     *  nothing navigates on a stray touch of a dense grid (LODGY-69). */
    val selectedBed: SelectedBed? = null,
) {
    val filteredBeds: List<Bed> get() = beds.filter { filter.matches(it.status) }
}

@HiltViewModel
class BedGridViewModel @Inject constructor(
    bedRepository: BedRepository,
    roomRepository: RoomRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])

    private val _uiState = MutableStateFlow(BedGridUiState())
    val uiState: StateFlow<BedGridUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val room = roomRepository.getById(roomId)
            _uiState.update {
                it.copy(
                    roomNumber = room?.roomNumber.orEmpty(),
                    roomType = room?.type,
                    pricePerBed = room?.pricePerBed ?: 0.0,
                    amenities = room?.amenities.orEmpty(),
                )
            }
        }
        viewModelScope.launch {
            bedRepository.getByRoomId(roomId).collect { beds ->
                _uiState.update { it.copy(beds = beds.sortedBy(Bed::label)) }
            }
        }
    }

    fun onFilterChange(filter: BedFilter) = _uiState.update { it.copy(filter = filter) }

    fun onBedSelected(bed: Bed) {
        viewModelScope.launch {
            val agreement = tenancyAgreementRepository.getActiveByBedId(bed.id)
            val tenant = agreement?.let { tenantRepository.getById(it.tenantId) }
            _uiState.update {
                it.copy(selectedBed = SelectedBed(bed, tenant?.id, tenant?.name.orEmpty()))
            }
        }
    }

    fun onBedSheetDismissed() = _uiState.update { it.copy(selectedBed = null) }
}
