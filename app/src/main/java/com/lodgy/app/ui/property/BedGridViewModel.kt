package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedOccupancyRow
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.OccupancyPeriodRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.ui.common.BedFilter
import com.lodgy.app.ui.common.matches
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
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
    val propertyName: String = "",
    val propertyType: PropertyType = PropertyType.HOSTEL,
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
    /** Non-null while the warden is naming the occupant of their own or a caretaker's room,
     *  pre-filled with the warden's name (LODGY-87). */
    val markingOwnRoom: String? = null,
    /** Every tenancy this bed has ever had, current one included, newest first (LODGY-93). */
    val bedHistory: List<BedOccupancyRow> = emptyList(),
    /** True from the moment a sheet opens until its history has actually loaded, so "never
     *  occupied" cannot flash on a bed that turns out to have history (LODGY-93). */
    val bedHistoryLoading: Boolean = false,
) {
    val filteredBeds: List<Bed> get() = beds.filter { filter.matches(it.status) }

    /** A shop, warehouse or flat is one unit. There is no grid to filter and no bed to name, so the
     *  screen shows the property and its letting status instead (LODGY-79). */
    val isSingleUnit: Boolean get() = propertyType.isSingleUnit
}

@HiltViewModel
class BedGridViewModel @Inject constructor(
    private val bedRepository: BedRepository,
    roomRepository: RoomRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val wardenRepository: WardenRepository,
    private val occupancyPeriodRepository: OccupancyPeriodRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private var historyJob: Job? = null

    private val _uiState = MutableStateFlow(BedGridUiState())
    val uiState: StateFlow<BedGridUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val room = roomRepository.getById(roomId)
            val property = roomRepository.getPropertyForRoom(roomId)
            _uiState.update {
                it.copy(
                    roomNumber = room?.roomNumber.orEmpty(),
                    propertyName = property?.hostelName.orEmpty(),
                    propertyType = property?.propertyType ?: PropertyType.HOSTEL,
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
                it.copy(
                    selectedBed = SelectedBed(bed, tenant?.id, tenant?.name.orEmpty()),
                    bedHistory = emptyList(),
                    bedHistoryLoading = true,
                )
            }
        }
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            occupancyPeriodRepository.observeByBedId(bed.id).collect { rows ->
                _uiState.update { it.copy(bedHistory = rows, bedHistoryLoading = false) }
            }
        }
    }

    fun onBedSheetDismissed() {
        historyJob?.cancel()
        historyJob = null
        _uiState.update { it.copy(selectedBed = null, bedHistory = emptyList(), bedHistoryLoading = false) }
    }

    /**
     * The shortcut past onboarding for a room the warden or a caretaker lives in (LODGY-87).
     * Reaching LODGY-82's switch used to mean typing yourself in as a tenant with a phone number
     * first, because the switch is on the last screen of the onboarding chain. The name is
     * pre-filled with the warden's own, so marking your own room is a confirm and marking a
     * caretaker's is one field.
     */
    fun onMarkOwnRoomRequested() {
        viewModelScope.launch {
            _uiState.update { it.copy(markingOwnRoom = wardenRepository.getWarden()?.name.orEmpty()) }
        }
    }

    fun onMarkOwnRoomNameChange(value: String) = _uiState.update { it.copy(markingOwnRoom = value) }

    fun onMarkOwnRoomDismissed() = _uiState.update { it.copy(markingOwnRoom = null) }

    /**
     * Creates a real tenant and a real tenancy carrying the non-revenue flag, so the room behaves
     * exactly as it does when the same thing is done the long way: occupied, billed to nobody,
     * out of dues, and with a history that reads like any other room's (LODGY-82). A tenant row
     * per marked room rather than one shared "self" record, because one tenant holding several
     * active agreements would make getActiveByTenantId pick the wrong one for transfer, checkout
     * and invoicing. Billing day 1: it must be valid, and it is where LODGY-84's forgone-rent
     * expense lands.
     */
    fun onMarkOwnRoomConfirmed() {
        val bed = _uiState.value.selectedBed?.bed ?: return
        val name = _uiState.value.markingOwnRoom?.trim().orEmpty()
        if (name.isEmpty()) return
        viewModelScope.launch {
            val tenant = tenantRepository.create(
                name = name,
                phone = "",
                photoPath = null,
                idProofPhotoPath = null,
                emergencyContactName = "",
                emergencyContactPhone = "",
            )
            tenancyAgreementRepository.create(
                tenantId = tenant.id,
                bedId = bed.id,
                agreedRent = 0.0,
                advanceDeposit = 0.0,
                billingCycleDay = 1,
                moveInDate = System.currentTimeMillis(),
                nonRevenue = true,
            )
            bedRepository.setOccupied(bed.id)
            _uiState.update { it.copy(markingOwnRoom = null, selectedBed = null) }
        }
    }
}
