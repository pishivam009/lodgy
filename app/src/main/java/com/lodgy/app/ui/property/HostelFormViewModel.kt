package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.ReconciliationRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.ui.common.UpdateChange
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HostelFormUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val address: String = "",
    val contactPhone: String = "",
    val propertyType: PropertyType = PropertyType.HOSTEL,
    /** Only asked for single-unit properties: the property IS the rentable unit, so its rent belongs
     *  on this form rather than behind floors and rooms the warden will never see (LODGY-79). */
    val monthlyRent: String = "",
    val saved: Boolean = false,
    /** Delete a property added in error (LODGY-64). Confirmed first (LODGY-57), and blocked while it
     *  still has a tenancy or an expense - the two things that don't cascade away with it. */
    val pendingDelete: Boolean = false,
    val blockedDelete: Boolean = false,
    val deleted: Boolean = false,
    /** What Save is about to change that this form does not show the consequence of
     *  (LODGY-65); empty means save straight through. */
    val pendingChanges: List<UpdateChange> = emptyList(),
) {
    val isSingleUnit: Boolean get() = propertyType.isSingleUnit

    /** A hostel is set up floor by floor afterwards, so it needs no rent here. A single-unit
     *  property is rentable the moment it is created, so it does. */
    val canSave: Boolean
        get() = name.isNotBlank() && (!isSingleUnit || monthlyRent.toDoubleOrNull() != null)
}

@HiltViewModel
class HostelFormViewModel @Inject constructor(
    private val hostelRepository: HostelRepository,
    private val wardenRepository: WardenRepository,
    private val floorRepository: FloorRepository,
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
    private val expenseRepository: ExpenseRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val reconciliationRepository: ReconciliationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val hostelId: String? = savedStateHandle["hostelId"]
    private var existingHostel: Hostel? = null

    private val _uiState = MutableStateFlow(HostelFormUiState(isEditing = hostelId != null))
    val uiState: StateFlow<HostelFormUiState> = _uiState.asStateFlow()

    init {
        val id = hostelId
        if (id != null) {
            viewModelScope.launch {
                val hostel = hostelRepository.getById(id) ?: return@launch
                existingHostel = hostel
                val rent = if (hostel.propertyType.isSingleUnit) {
                    roomRepository.getFirstRoomIdByHostel(hostel.id)
                        ?.let { roomId -> roomRepository.getById(roomId)?.pricePerBed?.toString() }
                        .orEmpty()
                } else {
                    ""
                }
                _uiState.update {
                    it.copy(
                        name = hostel.name,
                        address = hostel.address,
                        contactPhone = hostel.contactPhone,
                        propertyType = hostel.propertyType,
                        monthlyRent = rent,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onAddressChange(value: String) = _uiState.update { it.copy(address = value) }
    fun onContactPhoneChange(value: String) = _uiState.update { it.copy(contactPhone = value) }
    fun onMonthlyRentChange(value: String) = _uiState.update { it.copy(monthlyRent = value) }

    fun onPropertyTypeChange(value: PropertyType) = _uiState.update { it.copy(propertyType = value) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val changes = changesNeedingConfirmation(state)
            if (changes.isEmpty()) persist() else _uiState.update { it.copy(pendingChanges = changes) }
        }
    }

    /**
     * A rename is normally free and stays unconfirmed (LODGY-57 AC5) - but once the warden has
     * attested a month against this property, that paper trail and every PDF they exported still
     * carry the old name, and nothing on this form says so. The rent of a single-unit property is
     * the same invisible money as a room's price per bed: it re-rates every future invoice. The
     * address and the phone number confirm nothing (DESIGN.md 4.12).
     */
    private suspend fun changesNeedingConfirmation(state: HostelFormUiState): List<UpdateChange> {
        val existing = existingHostel ?: return emptyList()
        return buildList {
            if (state.name != existing.name) {
                val reconciled = reconciliationRepository.getByHostelId(existing.id).first().size
                if (reconciled > 0) add(UpdateChange.HostelRename(state.name, reconciled))
            }
            if (existing.propertyType.isSingleUnit) {
                val rent = state.monthlyRent.toDoubleOrNull()
                val current = currentUnitRent(existing.id)
                if (rent != null && current != null && rent != current) {
                    add(UpdateChange.UnitRent(current, rent))
                }
            }
        }
    }

    private suspend fun currentUnitRent(hostelId: String): Double? =
        roomRepository.getFirstRoomIdByHostel(hostelId)?.let { roomRepository.getById(it)?.pricePerBed }

    fun confirmChanges() {
        _uiState.update { it.copy(pendingChanges = emptyList()) }
        viewModelScope.launch { persist() }
    }

    fun dismissChanges() {
        _uiState.update { it.copy(pendingChanges = emptyList()) }
    }

    private suspend fun persist() {
        val state = _uiState.value
        val existing = existingHostel
        if (existing != null) {
            hostelRepository.update(existing, state.name, state.address, state.contactPhone)
            if (existing.propertyType.isSingleUnit) syncSingleUnit(existing.id, state)
        } else {
            val warden = wardenRepository.getWarden() ?: return
            val hostel = hostelRepository.create(
                warden.id, state.name, state.address, state.contactPhone, state.propertyType,
            )
            if (state.propertyType.isSingleUnit) createSingleUnit(hostel.id, state)
        }
        _uiState.update { it.copy(saved = true) }
    }

    /**
     * A shop, warehouse or flat is let as a whole, so the warden should never have to invent a floor
     * and a bed to rent it. They are created here instead, as REAL rows rather than nulls, so every
     * query, occupancy rollup and export keeps working exactly as it does for a hostel - the only
     * thing that changes is that the UI never shows these layers (LODGY-79).
     */
    private suspend fun createSingleUnit(hostelId: String, state: HostelFormUiState) {
        val floor = floorRepository.create(hostelId, IMPLICIT_FLOOR_LABEL)
        val room = roomRepository.create(
            floorId = floor.id,
            // Named for the property, so the row still reads sensibly anywhere the warden meets it
            // outside the property screens - the all-rooms tiles, a PDF packet, a CSV export.
            roomNumber = state.name,
            type = RoomType.SINGLE,
            pricePerBed = state.monthlyRent.toDoubleOrNull() ?: 0.0,
            amenities = "",
        )
        bedRepository.generateForRoom(room.id, 1)
    }

    /** Blocked while the property still has a tenancy or an expense - the two records that don't
     *  cascade away with the hostel and would fail the delete on a foreign key. Its floors, rooms and
     *  beds DO cascade (their foreign keys are ON DELETE CASCADE), so a mistakenly-added property -
     *  including a single-unit shop with its implicit floor/room/bed - deletes cleanly once it holds
     *  no tenancy or money (LODGY-64, AC1). */
    fun requestDelete() {
        val hostel = existingHostel ?: return
        viewModelScope.launch {
            val hasExpenses = expenseRepository.getByHostelId(hostel.id).first().isNotEmpty()
            val bedIds = floorRepository.getByHostelId(hostel.id).first()
                .flatMap { floor -> roomRepository.getByFloorId(floor.id).first() }
                .flatMap { room -> bedRepository.getByRoomId(room.id).first() }
                .map { it.id }
                .toSet()
            val hasTenancies = tenancyAgreementRepository.getAll().any { it.bedId in bedIds }
            _uiState.update {
                if (hasExpenses || hasTenancies) it.copy(blockedDelete = true) else it.copy(pendingDelete = true)
            }
        }
    }

    fun dismissDelete() = _uiState.update { it.copy(pendingDelete = false, blockedDelete = false) }

    fun confirmDelete() {
        val hostel = existingHostel ?: return
        viewModelScope.launch {
            hostelRepository.delete(hostel)
            _uiState.update { it.copy(pendingDelete = false, deleted = true) }
        }
    }

    /** Keeps the implicit room in step with the property, since the warden edits it here and can
     *  never reach the room form itself. */
    private suspend fun syncSingleUnit(hostelId: String, state: HostelFormUiState) {
        val roomId = roomRepository.getFirstRoomIdByHostel(hostelId) ?: return
        val room = roomRepository.getById(roomId) ?: return
        roomRepository.update(
            room = room,
            roomNumber = state.name,
            type = room.type,
            pricePerBed = state.monthlyRent.toDoubleOrNull() ?: room.pricePerBed,
            amenities = room.amenities,
        )
    }
}

/** The floor a single-unit property never shows. It exists only so the hierarchy stays intact. */
private const val IMPLICIT_FLOOR_LABEL = "-"
